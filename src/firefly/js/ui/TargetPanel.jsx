/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {get} from 'lodash';
import {parseTarget, getFeedback, formatPosForTextField} from './TargetPanelWorker.js';
import {TargetFeedback} from './TargetFeedback.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import {isValidPoint, parseWorldPt} from '../visualize/Point.js';


const TARGET= 'targetSource';
const RESOLVER= 'resolverSource';

const nedThenSimbad= 'nedthensimbad';
const simbadThenNed= 'simbadthenned';

class TargetPanelView extends Component {

    componentWillUnmount() {
        const {onUnmountCB, fieldKey, groupKey}= this.props;
        if (onUnmountCB) onUnmountCB(fieldKey,groupKey);
    }

    render() {
        const {showHelp, feedback, valid, message, onChange, value, labelWidth, children, resolver}= this.props;
        var positionField = (<InputFieldView
                                valid={valid}
                                visible= {true}
                                message={message}
                                onChange={(ev) => onChange(ev.target.value, TARGET)}
                                label='Name or Position:'
                                value={value}
                                tooltip='Enter a target'
                                labelWidth={labelWidth}
                            />);
        positionField = children ? (<div style={{display: 'flex'}}>{positionField} {children}</div>) : positionField;

        return (
            <div>
                <div style= {{display: 'flex'}}>
                    {positionField}
                    <ListBoxInputFieldView
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
                    />
                </div>
                <TargetFeedback showHelp={showHelp} feedback={feedback}/>
            </div>
        );
    }
}


TargetPanelView.propTypes = {
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    resolver: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    onUnmountCB : PropTypes.func,
};


function didUnmount(fieldKey,groupKey) {
    const wp= parseWorldPt(FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey));

    if (isValidPoint(wp)) {
        if (wp) dispatchActiveTarget(wp);
    }
}



function getProps(params, fireValueChange) {

    var feedback= params.feedback|| '';
    var value= params.displayValue;
    var resolver= params.resolver || nedThenSimbad;
    var showHelp= get(params,'showHelp', true);
    const wpStr= params.value;
    const wp= parseWorldPt(wpStr);

    if (isValidPoint(wp) && !value) {
        feedback= getFeedback(wp);
        value= wp.objName || formatPosForTextField(wp);
        showHelp= false;
    }

    return Object.assign({}, params,
        {
            visible: true,
            onChange: (value,source) => handleOnChange(value,source,params, fireValueChange),
            label: 'Name or Position:',
            tooltip: 'Enter a target',
            value,
            feedback,
            resolver,
            showHelp,
            onUnmountCB: didUnmount
        });
}




function handleOnChange(value, source, params, fireValueChange) {
    var {parseResults={}}= params;

    var displayValue;
    var resolver;

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
    var {resolvePromise}= parseResults;

    const targetResolve= (asyncParseResults) => {
        return asyncParseResults ? makePayloadAndUpdateActive(displayValue, asyncParseResults, null, resolver) : null;
    };

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
        message : 'Could not resolve object: Enter valid object',
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

const connectorDefaultProps = {
    fieldKey : 'UserTargetWorldPt',
    initialState  : {
        fieldKey : 'UserTargetWorldPt'
    }
};

function replaceValue(v,props) {
    const t= getActiveTarget();
    var retVal= v;
    if (t && t.worldPt) {
       if (get(t,'worldPt')) retVal= t.worldPt.toString();
    }
    return retVal;
}


export const TargetPanel= fieldGroupConnector(TargetPanelView,getProps,null,connectorDefaultProps, replaceValue);

