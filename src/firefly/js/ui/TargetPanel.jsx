/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {get, isString} from 'lodash';
import {parseTarget, getFeedback, formatPosForTextField} from './TargetPanelWorker.js';
import TargetFeedback from './TargetFeedback.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import FieldGroupUtils, {getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import {isValidPoint, parseWorldPt} from '../visualize/Point.js';



class TargetPanelView extends Component {

    componentWillUnmount() {
        const {onUnmountCB, fieldKey, groupKey}= this.props;
        if (onUnmountCB) onUnmountCB(fieldKey,groupKey);
    }

    render() {
        const {showHelp, feedback, valid, message, onChange, value, labelWidth}= this.props;
        return (
            <div>
                <InputFieldView
                    valid={valid}
                    visible= {true}
                    message={message}
                    onChange={onChange}
                    label='Name or Position:'
                    value={value}
                    tooltip='Enter a target'
                    labelWidth={labelWidth}
                />
                <TargetFeedback showHelp={showHelp} feedback={feedback}/>
            </div>
        );
    }
}


TargetPanelView.propTypes = {
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number,
    onUnmountCB : PropTypes.func,
};


function didUnmount(fieldKey,groupKey) {
   // console.log(`did unmount: ${fieldKey}, ${groupKey}`);
   //  console.log(`value: ${FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey)}`);

    const wp= parseWorldPt(FieldGroupUtils.getFldValue(FieldGroupUtils.getGroupFields(groupKey),fieldKey));

    if (isValidPoint(wp)) {
        if (wp) dispatchActiveTarget(wp);
    }
}



function getProps(params, fireValueChange) {

    var feedback= params.feedback|| '';
    var value= params.displayValue;
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
            onChange: (ev) => handleOnChange(ev,params, fireValueChange),
            label: 'Name or Position:',
            tooltip: 'Enter a target',
            value,
            feedback,
            showHelp,
            onUnmountCB: didUnmount
        });
}




function handleOnChange(ev, params, fireValueChange) {
    var displayValue= ev.target.value;
    var {parseResults={}}= params;


    parseResults= parseTarget(displayValue, parseResults);
    var {resolvePromise}= parseResults;

    const targetResolve= (asyncParseResults) => {
        return asyncParseResults ? makePayloadAndUpdateActive(displayValue, asyncParseResults) : null;
    };

    resolvePromise= resolvePromise ? resolvePromise.then(targetResolve) : null;

    fireValueChange(makePayloadAndUpdateActive(displayValue,parseResults, resolvePromise));
}

/**
 * make a payload and update the active target
 * Note- this function has as side effect to fires an action to update the active target
 * @param displayValue
 * @param parseResults
 * @param resolvePromise
 * @return {{message: string, displayValue: *, wpt: (*|null), value: null, valid: *, showHelp: (*|boolean), feedback: (string|*|string), parseResults: *}}
 */
function makePayloadAndUpdateActive(displayValue, parseResults, resolvePromise) {
    const {wpt}= parseResults;
    const wpStr= parseResults && wpt ? wpt.toString() : null;

    return {
        message : 'Could not resolve object: Enter valid object',
        displayValue,
        wpt,
        value : resolvePromise ? resolvePromise  : wpStr,
        valid : parseResults.valid,
        showHelp : parseResults.showHelp,
        feedback : parseResults.feedback,
        parseResults
    };
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
       // console.log(`value: ${v}, but I could use: ${t.worldPt}`);
       if (get(t,'worldPt')) retVal= t.worldPt.toString();
    }
    return retVal;
}


export const TargetPanel= fieldGroupConnector(TargetPanelView,getProps,null,connectorDefaultProps, replaceValue);

