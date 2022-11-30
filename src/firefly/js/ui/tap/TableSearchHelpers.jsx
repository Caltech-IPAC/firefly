import HelpIcon from 'firefly/ui/HelpIcon';
import {isEqual} from 'lodash';
import Prism from 'prismjs';
import PropTypes from 'prop-types';
import React, {useEffect, useRef} from 'react';
import {getAppOptions} from '../../api/ApiUtil.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';

export const HeaderFont = {fontSize: 12, fontWeight: 'bold', alignItems: 'center'};

// Style Helpers
export const LeftInSearch = 24;
export const LabelWidth = 110;
export const LableSaptail = 110;
export const SpatialWidth = 440;
export const Width_Column = 175;
export const SmallFloatNumericWidth = 12;
export const Width_Time_Wrapper = Width_Column + 30;
export const SpatialPanelWidth = Math.max(Width_Time_Wrapper * 2, SpatialWidth) + LabelWidth + 10;

const DEF_ERR_MSG= 'Constraints Error';


/**
 * make a FieldErrorList object
 * @returns {FieldErrorList}
 */
export const makeFieldErrorList = () => {
    const errors= [];
    const checkForError= ({valid=true, message=''}= {}) => !valid && errors.push(message);
    const addError= (message) => errors.push(message);
    const getErrors= () => [...errors];
    return {checkForError,addError,getErrors};
};

export const getPanelPrefix = (panelTitle) => panelTitle[0].toLowerCase() + panelTitle.substr(1);


function getPanelAdqlConstraint(panelActive, panelTitle,constraintsValid,adqlConstraintsAry,firstMessage, defErrorMessage=DEF_ERR_MSG) {
    if (!panelActive) return {adqlConstraint:'',adqlConstraintErrors:[]};

    if (constraintsValid && adqlConstraintsAry?.length) {
        return {adqlConstraint:adqlConstraintsAry.join(' AND '),adqlConstraintErrors:[]};
    } else {
        const msg= (!constraintsValid && firstMessage) ?
            `Error processing ${panelTitle} constraints: ${firstMessage}` : defErrorMessage;
        return {adqlConstraint:'',adqlConstraintErrors:[msg]};
    }
}

/**
 *
 * @param {boolean} panelActive
 * @param {String} panelTitle
 * @param {String} [defErrorMessage]
 * @returns {Function}
 */
export function makePanelStatusUpdater(panelActive,panelTitle,defErrorMessage) {
    /**
     * @Function
     * @param {InputConstraints} constraints
     * @param {ConstraintResult} lastConstraintResult
     * @param {Function} setConstraintResult - a function to set the constraint result setConstraintResult(ConstraintResult)
     * @String string - panel message
     */
    return (constraints, lastConstraintResult, setConstraintResult) => {
        const {valid:constraintsValid,errAry, adqlConstraintsAry, siaConstraints, siaConstraintErrors}= constraints;

        const simpleError= constraintsValid ? '' : (errAry[0]|| defErrorMessage || '');

        const {adqlConstraint, adqlConstraintErrors}=
            getPanelAdqlConstraint(panelActive,panelTitle, constraintsValid,adqlConstraintsAry,errAry[0], defErrorMessage);
        const cr = { adqlConstraint, adqlConstraintErrors, siaConstraints, siaConstraintErrors, simpleError};
        if (constrainResultDiffer(cr, lastConstraintResult)) setConstraintResult(cr);

        return simpleError;
    };
}


function constrainResultDiffer(c1, c2) {
    return (c1?.adqlConstraint !== c2?.adqlConstraint ||
        (c1.simpleError!==c2.simpleError) ||
        !isEqual(c1.adqlConstraintErrors, c2.adqlConstraintErrors) ||
        !isEqual(c1.siaConstraints, c2.siaConstraints) ||
        !isEqual(c1.siaConstraintErrors, c2.siaConstraintErrors));
}



function Header({title, helpID='', checkID, message, enabled=false, panelValue=undefined}) {
    const tooltip = title + ' search is included in the query if checked';
    return (
        <div style={{display: 'inline-flex', alignItems: 'center'}} title={title + ' search'}>
            <div onClick={(e) => e.stopPropagation()} title={tooltip}>
                <CheckboxGroupInputField key={checkID} fieldKey={checkID}
                    initialState={{ value: enabled ? panelValue || title:'', label: '' }}
                    options={[{label:'', value: panelValue || title}]}
                    alignment='horizontal' wrapperStyle={{whiteSpace: 'norma'}} />
            </div>
            <div style={{...HeaderFont, marginRight: 5}}>{title}</div>
            <HelpIcon helpId={helpID}/>
            <div style={{marginLeft: 10, color: 'saddlebrown', fontStyle: 'italic', fontWeight: 'normal'}}>{message}</div>
        </div>
    );
}

Header.propTypes = {
    title: PropTypes.string,
    helpID: PropTypes.string,
    checkID: PropTypes.string,
    message: PropTypes.string,
    panelValue: PropTypes.string,
    enabled: PropTypes.bool
};

function InternalCollapsibleCheckHeader({title, helpID, children, fieldKey, checkKey, message, initialState, initialStateChecked, panelValue}) {

    return (
        <FieldGroupCollapsible header={<Header title={title} helpID={helpID}
                                               enabled={initialStateChecked}
                                               checkID={checkKey} message={message} panelValue={panelValue}/>}
                               initialState={initialState} fieldKey={fieldKey} headerStyle={HeaderFont}>
            {children}
        </FieldGroupCollapsible>

    );
}



export function makeCollapsibleCheckHeader(base) {
    const panelKey= base+'-panelKey';
    const panelCheckKey= base+'-panelCheckKey';
    const panelValue= base+'-panelEnabled';

    const retObj= {
            isPanelActive: () => false,
            setPanelActive: () => undefined,
            collapsibleCheckHeaderKeys:  [panelKey,panelCheckKey],
        };

    retObj.CollapsibleCheckHeader= ({title,helpID,message,initialStateOpen, initialStateChecked,children}) => {
        const [getPanelActive, setPanelActive] = useFieldGroupValue(panelCheckKey);// eslint-disable-line react-hooks/rules-of-hooks
        const isActive= getPanelActive() === panelValue;
        retObj.isPanelActive= () => getPanelActive() === panelValue;
        retObj.setPanelActive= (active) => setPanelActive(active ? panelValue : '');
        return (
            <InternalCollapsibleCheckHeader {...{title, helpID, checkKey:panelCheckKey, fieldKey:panelKey,
                                            message: isActive ? message:'', initialStateChecked, panelValue,
                                            initialState:{value: initialStateOpen ? 'open' : 'close'}}} >
                {children}
            </InternalCollapsibleCheckHeader>
        );
    };
    return retObj;
}






export function DebugObsCore({constraintResult, includeSia=false}) {

    const {current:divElementRef}= useRef({divElement:undefined});

    useEffect(() => {
        divElementRef.divElement&& Prism.highlightAllUnder(divElementRef.divElement);// highlight help text/code snippets
    });
    if (!getAppOptions().tapObsCore?.debug) return false;

    const siaFrag= (
        <span>
            sia: {constraintResult?.siaConstraintErrors?.length ?
            `Error: ${constraintResult?.siaConstraintErrors?.join(' ')}` :
            constraintResult?.siaConstraints?.join('&')}
        </span>

    );

    const adqlFrag= (
        <code className='language-sql' style={{   background: 'none' }}>
            {constraintResult?.adqlConstraint}
        </code>
    );

    return (
        <div ref={(c) => divElementRef.divElement= c} style={{marginTop:5}}>
            <span style={{fontStyle:'italic', fontSize:'smaller'}}>adql: </span>
            <span>
                {constraintResult?.adqlConstraint ? adqlFrag : <span>&#8709;</span>}
            </span> <br/>
            {includeSia && siaFrag}
        </div> );
}





/**
 * @typedef {Object} FieldErrorList
 * @prop {Function} addError - add a string error message, addError(string)
 * @prop {Function} checkForError - check and FieldGroupField for errors and add if found, checkForError(field)
 * @prop {Function} getErrors - return the arrays of errors, const errAry= errList.getErrors()
 */

/**
 * @typedef {Object} InputConstraints
 *
 * @prop {boolean} valid
 * @prop {Array.<String>} errAry
 * @props {Array.<String>} adqlConstraintsAry
 * @props {Array.<String>} siaConstraints
 * @props {Array.<String>} siaConstraintErrors
 */

/**
 * @typedef {Object} ConstraintResult
 *
 * @prop {String} adqlConstraint
 * @props {Array.<String>} adqlConstraintErrors
 * @props {Array.<String>} siaConstraints
 * @props {Array.<String>} siaConstraintErrors
 * @prop {String} simpleError
 *
 */