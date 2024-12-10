import KeyboardDoubleArrowDown from '@mui/icons-material/KeyboardDoubleArrowDown';

import KeyboardDoubleArrowUp from '@mui/icons-material/KeyboardDoubleArrowUp';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import {Box, Button, IconButton, Stack, Typography} from '@mui/joy';
import HelpIcon from 'firefly/ui/HelpIcon';
import {SwitchInputFieldView} from 'firefly/ui/SwitchInputField';
import {isEqual, isObject} from 'lodash';
import Prism from 'prismjs';
import PropTypes from 'prop-types';
import React, {useEffect, useRef} from 'react';
import {getAppOptions} from '../../api/ApiUtil.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FieldGroupAccordionPanel} from '../panel/AccordionPanel.jsx';
import {RadioGroupInputFieldView} from '../RadioGroupInputFieldView.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {showResultTitleDialog} from './ResultTitleDialog';
import {ADQL_QUERY_KEY, makeTapSearchTitle, USER_ENTERED_TITLE} from './TapUtil';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';

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

/**
 * @param key
 * @param [serviceLabel]
 * @return {*}
 */
export function getObsCoreOption(key,serviceLabel=undefined) {
    const slOps= serviceLabel ? getAppOptions().tapObsCore?.[serviceLabel] ?? {} : {};
    const ops= getAppOptions().tapObsCore ?? {};
    return slOps[key] ?? ops[key];
}


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


function getPanelAdqlConstraint(panelActive, panelTitle,constraintsValid,adqlConstraintsAry,siaConstraints, firstMessage, defErrorMessage=DEF_ERR_MSG, useSIAv2=false) {
    if (!panelActive) return {adqlConstraint:'',constraintErrors:[], siaConstraints:[]};

    if (useSIAv2 && constraintsValid && siaConstraints?.length) {
        return {adqlConstraint: adqlConstraintsAry.join(' AND '), constraintErrors: [], siaConstraints};
    }
    else if (!useSIAv2 && constraintsValid && adqlConstraintsAry?.length) {
        return {adqlConstraint:adqlConstraintsAry.join(' AND '),constraintErrors:[], siaConstraints};
    }
    else {
        const msg= (!constraintsValid && firstMessage) ?
            `Error processing ${panelTitle} constraints: ${firstMessage}` : defErrorMessage;
        return {adqlConstraint:'',constraintErrors:[msg], siaConstraints:[]};
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
    return (constraints, lastConstraintResult, setConstraintResult, useSIAv2= false) => {
        const {valid:constraintsValid,errAry, adqlConstraintsAry,
            uploadFile, TAP_UPLOAD}= constraints;

        const simpleError= constraintsValid ? '' : (errAry[0]|| defErrorMessage || '');

        const {adqlConstraint, constraintErrors, siaConstraints}=
            getPanelAdqlConstraint(panelActive,panelTitle, constraintsValid,adqlConstraintsAry,constraints.siaConstraints, errAry[0], defErrorMessage, useSIAv2);
        const cr = { adqlConstraint, constraintErrors, siaConstraints, simpleError,
            uploadFile, TAP_UPLOAD};
        if (constraintResultDiffer(cr, lastConstraintResult)) setConstraintResult(cr);

        return simpleError;
    };
}


function constraintResultDiffer(c1, c2) {
    return (c1?.adqlConstraint !== c2?.adqlConstraint ||
        (c1.simpleError!==c2.simpleError) ||
        !isEqual(c1.constraintErrors, c2.constraintErrors) ||
        !isEqual(c1.siaConstraints, c2.siaConstraints) ||
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
        <Stack spacing={1} alignItems='center' direction='row'>
            <div onClick={(e) => e.stopPropagation()} title={tooltip}>
                <CheckboxGroupInputField key={checkID} fieldKey={checkID}
                                         initialState={{ value: enabled ? panelValue || title:'', label: '' }}
                                         options={[{label:'', value: panelValue || title}]}
                                         orientation='horizontal'  />
            </div>
            <Typography {...{color:'primary'}}>{title}</Typography>
            <HelpIcon helpId={helpID} component='div' />
            <Typography {...{level:'body-sm', color:'warning'}}>{message}</Typography>
        </Stack>
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

function InternalCollapsibleCheckHeader({sx, title, helpID, children, fieldKey, checkKey, message, initialState, initialStateChecked, panelValue}) {

    return (
        <FieldGroupAccordionPanel header={<Header title={title} helpID={helpID}
                                               enabled={initialStateChecked}
                                               checkID={checkKey} message={message} panelValue={panelValue}/>}
                                  sx={{'& .MuiAccordionDetails-content>.check-header-content': { ml: 3, }, ...sx}}
                                          initialState={initialState} fieldKey={fieldKey} headerStyle={HeaderFont}>
            <Box className='check-header-content'>
                {children}
            </Box>
        </FieldGroupAccordionPanel>

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

    retObj.CollapsibleCheckHeader= ({sx, title,helpID,message,initialStateOpen, initialStateChecked,children}) => {
        const [getPanelActive, setPanelActive] = useFieldGroupValue(panelCheckKey);// eslint-disable-line react-hooks/rules-of-hooks
        const [getPanelOpenStatus, setPanelOpenStatus] = useFieldGroupValue(panelKey);// eslint-disable-line react-hooks/rules-of-hooks
        const isActive= getPanelActive() === panelValue;
        retObj.isPanelActive= () => getPanelActive() === panelValue;
        retObj.setPanelActive= (active) => setPanelActive(active ? panelValue : '');
        retObj.isPanelOpen= () => getPanelOpenStatus();
        retObj.setPanelOpen= (open) => setPanelOpenStatus(open);
        return (
            <InternalCollapsibleCheckHeader {...{sx, title, helpID, checkKey:panelCheckKey, fieldKey:panelKey,
                                            message: isActive ? message:'', initialStateChecked, panelValue,
                                            initialState:{value: initialStateOpen}}} >
                {children}
            </InternalCollapsibleCheckHeader>
        );
    };
    return retObj;
}


export function NavButtons({setServicesShowing, servicesShowing, currentPanel, setNextPanel, lockService=false}) {

    return (
        <Stack {...{direction:'column', justifyContent:'center', spacing:.5, alignItems:'flex-end', mr:.2,
            sx:{ 'button': {width:'80px', maxHeight:22 } }
        }}>
            {!lockService && <ShowServicesButton {...{setServicesShowing,servicesShowing}}/>}
            <GotoPanelButton {...{setNextPanel,currentPanel}}/>
        </Stack>

        // <div style={{display:'flex', flexDirection:'column', margin:'5px 5px 0 60px'}}>
        //     <ShowServicesButton {...{setServicesShowing,servicesShowing}/>
        //     <GotoPanelButton {...{sx:{mt:1/2}, setNextPanel,currentPanel}}/>
        // </div>
    );
}

export const ADQL= 'adql';
export const SINGLE= 'basic';
export const OBSCORE= 'obscore';
export const ANY= 'any';
const HIDE='HIDE';
const SHOW='SHOW';

function ShowServicesButton({setServicesShowing, servicesShowing }) {
    const currState= servicesShowing ? SHOW : HIDE;
    const options= [
        {label:'Show', startDecorator: <KeyboardDoubleArrowUp/>, value:SHOW, tooltip:'Show other TAP services'},
        {label:'Hide', startDecorator: <KeyboardDoubleArrowDown/>, value:HIDE, tooltip:'Hide other TAP services'}
    ];

    return (
            <RadioGroupInputFieldView {...{
                options, value:currState, label:'TAP Services: ', orientation:'horizontal', buttonGroup:true,
                slotProps: {button: {sx: {'--Button-minHeight' : 22, '--Button-gap': '.2rem', }}},
                onChange:() => setServicesShowing(!servicesShowing)
            }}/>
    );
}

function GotoPanelButton({currentPanel, setNextPanel}) {
    const options= [ {label:'UI assisted', value:SINGLE}, {label:'Edit ADQL', value:ADQL}];
    const tooltip= 'Please select an interface type to use';

    return (
            <RadioGroupInputFieldView {...{
                options, value:currentPanel,
                label:'View: ', orientation:'horizontal', tooltip, buttonGroup:true, inline:true,
                onChange: () => setNextPanel(currentPanel===SINGLE ? ADQL : SINGLE)
            }}/>
    );
}

export function TableTypeButton({sx, lockToObsCore, setLockToObsCore}) {
    const tooltip= lockToObsCore ? 'Selected an image Search' : 'Selected search for catalog or other tables';

    return (
        <SwitchInputFieldView  {...{
            sx,
            slotProps: {
                input: {
                    sx:{ '--Switch-trackWidth': '20px', '--Switch-trackHeight': '12px', }
                }
            },
            size:'sm',
            endDecorator: 'Use Image Search (ObsTAP)',
            tooltip, value: lockToObsCore,
            onChange: () => setLockToObsCore(!lockToObsCore)
        }}/>
    );
}

export function DebugObsCore({constraintResult, includeSia=true}) {

    const {current:divElementRef}= useRef({divElement:undefined});

    useEffect(() => {
        divElementRef.divElement&& Prism.highlightAllUnder(divElementRef.divElement);// highlight help text/code snippets
    });
    if (!getAppOptions().tapObsCore?.debug) return false;

    const siaFrag= (
        <>
            <span style={{fontStyle: 'italic', fontSize: 'smaller'}}>sia: </span>
            <code style={{fontSize: 'smaller'}}>
                { constraintResult?.siaConstraints?.join('&')}
            </code>
        </>
    );

    const adqlFrag = (
        <code className='language-sql' style={{background: 'none'}}>
            {constraintResult?.adqlConstraint}
        </code>
    );

    return (
        <div ref={(c) => divElementRef.divElement = c} style={{marginTop: 5}}>
            <span style={{fontStyle: 'italic', fontSize: 'smaller'}}>adql: </span>
            <span>
                {constraintResult?.adqlConstraint ? adqlFrag : <span>&#8709;</span>}
            </span> <br/>
            {includeSia && siaFrag}
        </div> );
}



export function TitleCustomizeButton({groupKey, tapBrowserState, selectBy}) {

    const [getUserTitle,setUserTitle]= useFieldGroupValue(USER_ENTERED_TITLE,groupKey);
    const [getADQL,]= useFieldGroupValue(ADQL_QUERY_KEY,groupKey);
    const getDefTitle= () =>
        selectBy === 'adql' ?
            makeTapSearchTitle(getADQL(), tapBrowserState.serviceUrl) :
            makeTapSearchTitle(undefined, tapBrowserState.serviceUrl, tapBrowserState.tableName);

    const onClick= () => {
        const defTitle= getDefTitle();
        showResultTitleDialog(getUserTitle(), defTitle, (newTitle) => setUserTitle(newTitle===defTitle ? undefined : newTitle) );
    };

    const title= getUserTitle() || getDefTitle();
    if (!title) return undefined;

    return (
            <Stack direction='row' alignItems='center' sx={{pl:3}}>
                <IconButton onClick={onClick} sx={{minWidth:0}}>
                    <EditOutlinedIcon/>
                </IconButton>
                <Typography
                    {...{
                        level: 'body-sm',
                        sx: {
                            textAlign:'left',
                            width: '13rem',
                            textWrap: 'nowrap',
                            textOverflow: 'ellipsis',
                            overflow: 'hidden'
                        }
                    }}
                >
                    <Typography level='title-md'>Title: </Typography>
                    {`${getUserTitle() || getDefTitle() || ''}`}
                </Typography>

            </Stack>
    );
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
 * @props {Array.<String>} constraintErrors
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