import HelpIcon from 'firefly/ui/HelpIcon';
import {isEqual, isObject} from 'lodash';
import Prism from 'prismjs';
import PropTypes from 'prop-types';
import React, {useEffect, useRef} from 'react';
import {getAppOptions} from '../../api/ApiUtil.js';
import {CheckboxGroupInputField, CheckboxGroupInputFieldView} from '../CheckboxGroupInputField.jsx';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {RadioGroupInputFieldView} from '../RadioGroupInputFieldView.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';

import HIDE_ICON from 'images/show-up-3.png';
import SHOW_ICON from 'images/hide-down-3.png';

export const HeaderFont = {fontSize: 12, fontWeight: 'bold', alignItems: 'center'};

// Style Helpers
export const LeftInSearch = 24;
export const LabelWidth = 110;
export const LableSaptail = 65;
export const SpatialWidth = 520;
export const Width_Column = 175;
export const SmallFloatNumericWidth = 12;
export const Width_Time_Wrapper = Width_Column + 30;
export const SpatialPanelWidth = Math.max(Width_Time_Wrapper * 2, SpatialWidth) + LabelWidth + 10;

const DEF_ERR_MSG= 'Constraints Error';


export const getTapObsCoreOptions= (serviceLabel) =>
    getAppOptions().tapObsCore?.[serviceLabel] ?? getAppOptions().tapObsCore ?? {};



export function getTapObsCoreOptionsGuess(serviceLabelGuess) {
    const {tapObsCore={}}=  getAppOptions();
    if (!serviceLabelGuess) return tapObsCore;
    const guessKey= Object.entries(tapObsCore)
        .find( ([key,value]) => isObject(value) && serviceLabelGuess.includes(key))?.[0];
    return getTapObsCoreOptions(guessKey);
}


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
        const {valid:constraintsValid,errAry, adqlConstraintsAry, siaConstraints, siaConstraintErrors,
            uploadFile, TAP_UPLOAD}= constraints;

        const simpleError= constraintsValid ? '' : (errAry[0]|| defErrorMessage || '');

        const {adqlConstraint, adqlConstraintErrors}=
            getPanelAdqlConstraint(panelActive,panelTitle, constraintsValid,adqlConstraintsAry,errAry[0], defErrorMessage);
        const cr = { adqlConstraint, adqlConstraintErrors, siaConstraints, siaConstraintErrors, simpleError,
            uploadFile, TAP_UPLOAD};
        if (constraintResultDiffer(cr, lastConstraintResult)) setConstraintResult(cr);

        return simpleError;
    };
}


function constraintResultDiffer(c1, c2) {
    return (c1?.adqlConstraint !== c2?.adqlConstraint ||
        (c1.simpleError!==c2.simpleError) ||
        !isEqual(c1.adqlConstraintErrors, c2.adqlConstraintErrors) ||
        !isEqual(c1.siaConstraints, c2.siaConstraints) ||
        !isEqual(c1.siaConstraintErrors, c2.siaConstraintErrors) ||
        c1.upload!==c2.upload ||
        c1.uploadFrom!==c2.uploadFrom ||
        c1.serverFile!==c2.serverFile ||
        c1.uploadFileName!==c2.uploadFileName||
        !isEqual(c1.uploadColumns, c2.uploadColumns) ||
        !isEqual(c1.TAP_UPLOAD, c2.TAP_UPLOAD)
    );
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
        const [getPanelOpenStatus, setPanelOpenStatus] = useFieldGroupValue(panelKey);// eslint-disable-line react-hooks/rules-of-hooks
        const isActive= getPanelActive() === panelValue;
        retObj.isPanelActive= () => getPanelActive() === panelValue;
        retObj.setPanelActive= (active) => setPanelActive(active ? panelValue : '');
        retObj.isPanelOpen= () => getPanelOpenStatus() === 'open';
        retObj.setPanelOpen= (open) => setPanelOpenStatus(open?'open':'close');
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


export function NavButtons({setServicesShowing, servicesShowing, currentPanel, setNextPanel}) {

    return (
        <div style={{display:'flex', flexDirection:'column', margin:'5px 5px 0 60px'}}>
            <ShowServicesButton {...{setServicesShowing,servicesShowing, labelWidth:0}}/>
            <GotoPanelButton {...{style:{marginTop:5}, setNextPanel,currentPanel, labelWidth:0}}/>
        </div>
    );
}

export const ADQL= 'adql';
export const SINGLE= 'basic';
export const OBSCORE= 'obscore';
export const ANY= 'any';
const HIDE='HIDE';
const SHOW='SHOW';

const ServLabel= ({text,icon}) => (
    <div style={{display:'flex', flexDirection:'row', justifyContent:'flex-start', alignItems:'center'}}>
        <img src={icon} alt={text} height={10}/>
        <div style={{paddingLeft:12}}>{text}</div>
    </div>
);


function ShowServicesButton({style={}, labelWidth, setServicesShowing, servicesShowing }) {
    const currState= servicesShowing ? SHOW : HIDE;
    const tooltip= servicesShowing ? 'Showing TAP services selection' : 'TAP Services selection is hidden';
    const showLabel= <ServLabel icon={SHOW_ICON} text='Show'/>;
    const hideLabel= <ServLabel icon={HIDE_ICON} text='Hide'/>;
    const options= [ {label:showLabel, value:SHOW}, {label:hideLabel, value:HIDE} ];

    return (
            <RadioGroupInputFieldView {...{
                wrapperStyle:{display:'inline-flex', alignItems:'center', justifyContent:'flex-end'},
                buttonGroupButtonStyle:{width:80},
                options, value:currState, labelWidth, tooltip,
                label:'TAP Services: ',
                buttonGroup:true, inline:true,
                onChange:() => setServicesShowing(!servicesShowing)
            }}/>
    );
}

function GotoPanelButton({style={}, currentPanel, setNextPanel, labelWidth}) {
    const options= [ {label:'UI assisted', value:SINGLE}, {label:'Edit ADQL', value:ADQL}];
    const tooltip= 'Please select an interface type to use';

    return (
            <RadioGroupInputFieldView {...{
                wrapperStyle:{...style, display:'inline-flex', alignItems:'center', justifyContent:'flex-end'},
                options, value:currentPanel, labelWidth,
                buttonGroupButtonStyle:{width:80},
                label:'View: ',
                tooltip,
                buttonGroup:true, inline:true,
                onChange: () => setNextPanel(currentPanel===SINGLE ? ADQL : SINGLE)
            }}/>
    );
}

export function TableTypeButton({style={}, lockToObsCore, setLockToObsCore}) {
    const options= [ {label:'Use Image Search (ObsTAP)', value:OBSCORE}];
    const tooltip= lockToObsCore ? 'Selected anb image Search' : 'Selected search for catalog or other tables';

    return (
        <CheckboxGroupInputFieldView  {...{
            wrapperStyle:{...style, display:'inline-flex', alignItems:'center', justifyContent:'flex-start'},
            options,
            tooltip,value:lockToObsCore?OBSCORE:'', labelWidth:1,
            onChange: () => setLockToObsCore(!lockToObsCore)
        }}/>
    );
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
 * @prop {boolean } upload
 * @prop {Array.<String>} uploadColumns
 * @prop {Object} TAP_UPLOAD
 * @prop {String} uploadFile
 */