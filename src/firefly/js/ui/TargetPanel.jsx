/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Divider, Stack} from '@mui/joy';
import {isString} from 'lodash';
import React, {memo, useContext, useEffect} from 'react';
import PropTypes, {arrayOf, object, bool, string, shape, func} from 'prop-types';
import {parsePosition, PositionParsedInputType} from '../util/PositionParser';
import {ConnectionCtx} from './ConnectionCtx.js';
import {parseTarget} from './TargetPanelWorker.js';
import {formatPosForHelp, formatPosForTextField, formatTargetForHelp} from './PositionFieldDef.js';
import {TargetFeedback} from './TargetFeedback.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import {isValidPoint, makeWorldPt, parseWorldPt, pointEquals} from '../visualize/Point.js';


const TARGET= 'targetSource';
const RESOLVER= 'resolverSource';
const LABEL_DEFAULT='Coordinates or Object Name';

const nedThenSimbad= 'nedthensimbad';
const simbadThenNed= 'simbadthenned';

const TargetPanelView = (props) =>{
    const {showHelp, feedback, valid, message, onChange, value, button, slotProps, fieldKey,
        children, resolver, showResolveSourceOp= true, showExample= true,
        label= LABEL_DEFAULT,
        targetPanelExampleRow1, targetPanelExampleRow2,
        connectedMarker=false, placeholderHighlight=true,
        examples, onUnmountCB, sx}= props;

    useEffect(() => () => onUnmountCB(props),[]);
    const connectContext= useContext(ConnectionCtx);

    const endDecorator= makeEndDecorator(showResolveSourceOp,onChange,resolver,button);

    const positionField = (
        <InputFieldView {...{valid, visible:true, message,
            placeholder:label,
            onChange: (ev) => onChange(ev.target.value, TARGET),
            endDecorator,
            sx : makeSx(showResolveSourceOp, Boolean(button),sx),
            value,
            tooltip:'Enter a target',
            slotProps: {
                input: {
                    sx: {
                        '--Input-placeholderColor': placeholderHighlight ?
                            'var(--joy-palette-warning-plainColor)' : 'inherit'
                    }
                }
            },
            connectedMarker:connectedMarker||connectContext.controlConnected,
            }}
        />);
    const positionInput = children ? (<div style={{display: 'flex'}}>{positionField} {children}</div>) : positionField;



    return (
        <Stack direction='column'>
            {positionInput}
            {(showExample || !showHelp) && <TargetFeedback {...{showHelp, feedback, fieldKey,
                targetPanelExampleRow1, targetPanelExampleRow2, examples, ...slotProps?.feedback}}/> }
        </Stack>
    );
};


TargetPanelView.propTypes = {
    label : string,
    sx: object,
    valid   : bool.isRequired,
    showHelp   : bool.isRequired,
    feedback: string.isRequired,
    examples: object,
    resolver: string.isRequired,
    message: string.isRequired,
    onChange: func.isRequired,
    value : string.isRequired,
    onUnmountCB : func,
    nullAllowed: bool,
    showResolveSourceOp: bool,
    targetPanelExampleRow1: arrayOf(string),
    targetPanelExampleRow2: arrayOf(string),
    connectedMarker: bool,
    showExample: bool,
    fieldKey: string,
    slotProps: shape({
        feedback: object,
    })
};

function makeEndDecorator(showResolveSourceOp, onChange, resolver, button) {
    const resolverOp=
        showResolveSourceOp?
            (<ListBoxInputFieldView
                options={[
                    {label: 'Try NED then Simbad', value: nedThenSimbad},
                    {label: 'Try Simbad then NED', value: simbadThenNed}
                ]}
                slotProps={{
                    input: {
                        variant:'plain',
                        sx:{minHeight:'unset'}
                        // sx:{'&:hover': { bgcolor: 'transparent' } }
                    }
                }}
                onChange={(ev,newValue) => onChange(newValue, RESOLVER)}
                value={resolver} multiple={false}
                tooltip='Select which name resolver' label=''  />) : undefined;

    if  (resolverOp || button) {
        return (
            <Stack direction='row' alignItems='center'>
                {Boolean(resolverOp) && <Divider orientation='vertical'/>}
                {resolverOp}
                {Boolean(button) && <Divider orientation='vertical' />}
                {Boolean(button) && <Box sx={{ml:1/2}}>{button}</Box>}
            </Stack>);
    }
    return undefined;
}

function makeSx(useResolver, useButton, sx) {
    const minWidth='32rem';
    return (useResolver && !useButton) ?
        { minWidth, '& .MuiInput-root':{ 'paddingInlineEnd': 0}, ...sx} :
        {minWidth,...sx};
}

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



export function ingestNewTargetValue(value, setter, params, ) {
    const {resolver= nedThenSimbad}= params ?? {};

    const displayValue= value;

    const parseResults= parseTarget(displayValue, undefined, resolver);
    let {resolvePromise}= parseResults;

    const targetResolve= (asyncParseResults) => {
        return asyncParseResults ? setter(makePayloadAndUpdateActive(displayValue, asyncParseResults, null, resolver)) : null;
    };

    resolvePromise= resolvePromise ? resolvePromise.then(targetResolve) : null;


    setter(makePayloadAndUpdateActive(displayValue,parseResults, resolvePromise, resolver));

}



const prepareResult= (v) => isString(v) ? v : v?.toString();

/**
 * Make a payload and update the active target, Note: this function has as side effect to fires an action to update the active target
 * @param displayValue
 * @param parseResults
 * @param resolvePromise
 * @param {string} resolver the key to specify the resolver
 * @return {{message: string, displayValue: *, wpt: (*|null), value: null, valid: *, showHelp: (*|boolean), feedback: (string|*|string), parseResults: *}}
 */
function makePayloadAndUpdateActive(displayValue, parseResults, resolvePromise, resolver) {
    const {wpt}= parseResults;
    const wpStr= parseResults && wpt ? wpt.toString() : null;


    if (isValidPoint(wpt) && !pointEquals(wpt, getActiveTarget()?.worldPt)) {
        setTimeout(() => dispatchActiveTarget(wpt), 0);
    }

    const message= parseResults.parseError || (displayValue ? 'Could not resolve object: Enter valid object' : '');

    const payload= {
        message,
        displayValue,
        wpt,
        value : resolvePromise ? resolvePromise  : wpStr,
        valid : parseResults.valid,
        showHelp : parseResults.showHelp,
        feedback : parseResults.feedback,
        prepareResult,
        parseResults,
    };
    if (resolver) payload.resolver= resolver;
    return payload;
}


function replaceValue(v,defaultToActiveTarget, computedState) {
    if (!defaultToActiveTarget) return v;
    if ((computedState.displayValue || computedState.message) && !computedState.valid) return '';
    if (isString(v)) v= v.trim();
    if (v && v===computedState.value && computedState.valid && isValidPoint(parseWorldPt(v))) return v;
    return getActiveTarget()?.worldPt?.toString() ?? v;
}


export const DEF_TARGET_PANEL_KEY= 'UserTargetWorldPt';


export const TargetPanel = memo( ({fieldKey= DEF_TARGET_PANEL_KEY,initialState= {},
                                       defaultToActiveTarget= true, ...restOfProps}) => {
    const {viewProps, fireValueChange, groupKey}=  useFieldGroupConnector({
                                fieldKey, initialState: {...initialState, prepareResult},
                                confirmValueOnInit: (v, props,initialState,computedState) => replaceValue(v,defaultToActiveTarget,computedState)});
    const newProps= computePropsForView(viewProps, restOfProps, fieldKey, groupKey);
    return ( <TargetPanelView {...{...newProps}}
                              onChange={(value,source) => handleOnChange(value,source,newProps, fireValueChange)}/>);
});

TargetPanel.propTypes = {
    sx: object,
    fieldKey: string,
    groupKey: string,
    examples: object,
    nullAllowed: bool,
    initialState: object,
    showResolveSourceOp: bool,
    targetPanelExampleRow1: arrayOf(PropTypes.string),
    targetPanelExampleRow2: arrayOf(PropTypes.string),
    showExample: bool,
    connectedMarker: bool,
    placeholderHighlight: bool,
    defaultToActiveTarget: bool,
};


function computePropsForView(viewProps, componentProps, fieldKey, groupKey) {

    let feedback;
    let value;
    let showHelp;
    const wp= parseWorldPt(viewProps.value);
    let displayValue;

    // note that the value returned is the display value since it is going to the view. This is hard to understand
    // and should be clean up at some point in the future

    if (isValidPoint(wp)) {
        showHelp= false;
        if (!viewProps.displayValue) {
            feedback = formatTargetForHelp(wp);
            value = wp.objName || formatPosForTextField(wp);
        }
        else if (wp.objName) {
            value= wp.objName;
            feedback= (wp.objName===viewProps.displayValue) ? viewProps.feedback : formatTargetForHelp(wp);
        }
        else {
            const {valid, inputType, ra, dec, coordSys} = parsePosition(viewProps.displayValue);
            const dWp= (valid && inputType===PositionParsedInputType.Position) ?  makeWorldPt(ra,dec,coordSys) : undefined;
            value= pointEquals(wp,dWp) ? viewProps.displayValue : formatPosForTextField(wp);
            feedback= formatPosForHelp(wp);
        }
        displayValue= value;
    }
    else {
        value= viewProps.displayValue;
        feedback= viewProps.feedback;
        showHelp= viewProps?.showHelp ?? true;
    }

    return {
        ...viewProps,
        displayValue,
        visible: true,
        label: 'Coordinates or Object Name',
        tooltip: 'Enter a target',
        value,
        feedback: feedback||'',
        resolver: viewProps.resolver ?? nedThenSimbad,
        showHelp,
        fieldKey,
        onUnmountCB: (props) => didUnmount(fieldKey,groupKey,props),
        ...componentProps};
}
