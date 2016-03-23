/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {get} from 'lodash';
import {parseTarget} from './TargetPanelWorker.js';
import TargetFeedback from './TargetFeedback.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';



function TargetPanelView({showHelp, feedback, valid, message, onChange, value, labelWidth}) {
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


TargetPanelView.propTypes = {
    valid   : PropTypes.bool.isRequired,
    showHelp   : PropTypes.bool.isRequired,
    feedback: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    labelWidth : PropTypes.number
};





function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            visible: true,
            onChange: (ev) => handleOnChange(ev,params, fireValueChange),
            label: 'Name or Position:',
            tooltip: 'Enter a target',
            value: params.displayValue,
            showHelp : get(params,'showHelp', true),
            feedback : params.feedback|| ''
        });
}




function handleOnChange(ev, params, fireValueChange) {
    var displayValue= ev.target.value;
    var {parseResults={}}= params;


    parseResults= parseTarget(displayValue, parseResults);
    var {resolvePromise}= parseResults;

    const targetResolve= (asyncParseResults) => {
        return asyncParseResults ? makePayload(displayValue, asyncParseResults) : null;
    };

    resolvePromise= resolvePromise ? resolvePromise.then(targetResolve) : null;

    fireValueChange(makePayload(displayValue,parseResults, resolvePromise));
}

function makePayload(displayValue, parseResults, resolvePromise) {
    const wpStr= parseResults && parseResults.wpt ? parseResults.wpt.toString() : null;
    return {
        message : 'Could not resolve object: Enter valid object',
        displayValue,
        wpt : parseResults.wpt,
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


export const TargetPanel= fieldGroupConnector(TargetPanelView,getProps,null,connectorDefaultProps);

