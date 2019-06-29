/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {parseTarget, getFeedback, formatPosForTextField} from './TargetPanelWorker.js';
import {TargetFeedback} from './TargetFeedback.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import {isValidPoint, parseWorldPt} from '../visualize/Point.js';


const TARGET= 'targetSource';
const RESOLVER= 'resolverSource';
const LABEL_DEFAULT='Name or Position:';

const nedThenSimbad= 'nedthensimbad';
const simbadThenNed= 'simbadthenned';

class TargetPanelView extends PureComponent {

    componentWillUnmount() {
        const {onUnmountCB}= this.props;
        if (onUnmountCB) onUnmountCB(this.props);
    }

    render() {
        const {showHelp, feedback, valid, message, onChange, value,
            labelWidth, children, resolver, feedbackStyle, showResolveSourceOp= true, showExample= true,
            examples, label= LABEL_DEFAULT}= this.props;
        let positionField = (<InputFieldView
                                valid={valid}
                                visible= {true}
                                message={message}
                                onChange={(ev) => onChange(ev.target.value, TARGET)}
                                label={label}
                                value={value}
                                tooltip='Enter a target'
                                labelWidth={labelWidth}
                            />);
        positionField = children ? (<div style={{display: 'flex'}}>{positionField} {children}</div>) : positionField;

        return (
            <div>
                <div style= {{display: 'flex'}}>
                    {positionField}
                    {showResolveSourceOp && <ListBoxInputFieldView
                        options={[{label: 'Try NED then Simbad', value: nedThenSimbad},
                               {label: 'Try Simbad then NED', value: simbadThenNed}
                              ]}
                        value={resolver}
                        onChange={(ev) => onChange(ev.target.value, RESOLVER)}
                        multiple={false}
                        tooltip='Select which name resolver'
                        label=''
                        labelWidth={3}
                        wrapperStyle={{}}
                    />}
                </div>
                {(showExample || !showHelp) &&
                        <TargetFeedback {...{showHelp, feedback, style:feedbackStyle, examples}}/>
                }
            </div>
        );
    }
}


TargetPanelView.propTypes = {
    label : PropTypes.string,
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    examples: PropTypes.object,
    resolver: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    onUnmountCB : PropTypes.func,
    feedbackStyle: PropTypes.object,
    nullAllowed: PropTypes.bool,
    showResolveSourceOp: PropTypes.bool,
    showExample: PropTypes.bool
};


function didUnmount(fieldKey,groupKey, props) {
    const wp= parseWorldPt(FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey));

    if (props.nullAllowed && !wp) {
        dispatchActiveTarget(null);
    }
    else if (isValidPoint(wp)) {
        dispatchActiveTarget(wp);
    }
}


function handleOnChange(value, source, params, fireValueChange) {
    let {parseResults={}}= params;

    let displayValue;
    let resolver;

    if (source===TARGET) {
        resolver= params.resolver || nedThenSimbad;
        displayValue= value;
    }
    else if (source===RESOLVER) {
        resolver= value;
        displayValue= params.displayValue || '';
    }
    else {
        console.error('should never be here');
    }

    parseResults= parseTarget(displayValue, parseResults, resolver);
    let {resolvePromise}= parseResults;

    const targetResolve= (asyncParseResults) => {
        return asyncParseResults ? makePayloadAndUpdateActive(displayValue, asyncParseResults, null, resolver) : null;
    };

    if (!displayValue && params.nullAllowed) {
        parseResults.valid= true;
        parseResults.feedback= 'valid: true';
    }



    resolvePromise= resolvePromise ? resolvePromise.then(targetResolve) : null;

    fireValueChange(makePayloadAndUpdateActive(displayValue,parseResults, resolvePromise, resolver));

}

/**
 * make a payload and update the active target
 * Note- this function has as side effect to fires an action to update the active target
 * @param displayValue
 * @param parseResults
 * @param resolvePromise
 * @param {string} resolver the key to specify the resolver
 * @return {{message: string, displayValue: *, wpt: (*|null), value: null, valid: *, showHelp: (*|boolean), feedback: (string|*|string), parseResults: *}}
 */
function makePayloadAndUpdateActive(displayValue, parseResults, resolvePromise, resolver) {
    const {wpt}= parseResults;
    const wpStr= parseResults && wpt ? wpt.toString() : null;

    const payload= {
        message : parseResults.parseError || 'Could not resolve object: Enter valid object',
        displayValue,
        wpt,
        value : resolvePromise ? resolvePromise  : wpStr,
        valid : parseResults.valid,
        showHelp : parseResults.showHelp,
        feedback : parseResults.feedback,
        parseResults
    };
    if (resolver) payload.resolver= resolver;
    return payload;
}


function replaceValue(v,defaultToActiveTarget) {
    if (!defaultToActiveTarget) return v;
    const t= getActiveTarget();
    let retVal= v;
    if (t && t.worldPt) {
       if (get(t,'worldPt')) retVal= t.worldPt.toString();
    }
    return retVal;
}




export const TargetPanel = memo( ({fieldKey= 'UserTargetWorldPt',initialState= {},
                                       defaultToActiveTarget= true, ...restOfProps}) => {
    const {viewProps, fireValueChange, groupKey}=  useFieldGroupConnector({
                                fieldKey, initialState,
                                confirmValueOnInit: (v) => replaceValue(v,defaultToActiveTarget)});
    const newProps= computeProps(viewProps, restOfProps, fieldKey, groupKey);
    return ( <TargetPanelView {...newProps}
                              onChange={(value,source) => handleOnChange(value,source,newProps, fireValueChange)}/>);
});



TargetPanel.propTypes = {
    fieldKey: PropTypes.string,
    groupKey: PropTypes.string,
    examples: PropTypes.object,
    labelWidth : PropTypes.number,
    nullAllowed: PropTypes.bool,
    initialState: PropTypes.object,
    showResolveSourceOp: PropTypes.bool,
    showExample: PropTypes.bool,
    defaultToActiveTarget: PropTypes.bool,
};


function computeProps(viewProps, componentProps, fieldKey, groupKey) {

    let feedback= viewProps.feedback|| '';
    let value= viewProps.displayValue;
    let showHelp= get(viewProps,'showHelp', true);
    const resolver= viewProps.resolver || nedThenSimbad;
    const wpStr= viewProps.value;
    const wp= parseWorldPt(wpStr);

    if (isValidPoint(wp) && !value) {
        feedback= getFeedback(wp);
        value= wp.objName || formatPosForTextField(wp);
        showHelp= false;
    }

    return {
        ...viewProps,
        visible: true,
        label: 'Name or Position:',
        tooltip: 'Enter a target',
        value,
        feedback,
        resolver,
        showHelp,
        onUnmountCB: (props) => didUnmount(fieldKey,groupKey,props),
        ...componentProps};
}
