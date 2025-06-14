/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Stack} from '@mui/joy';
import React from 'react';
import {CoordinateSys, parseWorldPt} from '../../api/ApiUtilImage.jsx';
import {isDefined} from '../../util/WebUtil';
import {standardIDs} from '../../voAnalyzer/VoConst.js';

import {CONE_CHOICE_KEY, POLY_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {convertCelestial} from '../../visualize/VisUtil.js';
import CompleteButton from '../CompleteButton.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {FormPanel} from '../FormPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {Slot} from '../SimpleComponent';
import {setIf as setIfUndefined} from 'firefly/util/WebUtil.js';
import {
    AREA, CHECKBOX, CIRCLE, CONE_AREA_KEY, ENUM, FLOAT, INT, POINT, POLYGON, POSITION, SIA_OBSCORE_OPS, UNKNOWN,
    WAVELENGTH
} from './DynamicDef.js';
import { getSpacialSearchType, hasValidSpacialSearch, makeAllFields, makeUnitsStr } from './DynComponents.jsx';
import {findFieldDefType} from './ServiceDefTools.js';

export const DEFER_TO_CONTEXT= 'DEFER_TO_CONTEXT';
export const CONTEXT_PARAMS_STR= 'CONTEXT_PARAMS_STR';

const defaultOnError= () => showInfoPopup('One or more fields are not valid', 'Invalid Data');

export const DynamicFieldGroupPanel = ({DynLayoutPanel, groupKey, fieldDefAry, style,
                                           keepState = true, plotId='defaultHiPSTargetSearch'}) => (
    <FieldGroup groupKey={groupKey} keepState={keepState} style={style}>
        <DynLayoutPanel fieldDefAry={fieldDefAry} style={{margin: '8px 3px 15px 3px', width: '100%'}} plotId={plotId}/>
    </FieldGroup>
);


export const DynCompleteButton= ({fieldDefAry, onSuccess, ...restOfProps}) => (
    <CompleteButton {...{...restOfProps,
        onSuccess:(r) => onSuccess(convertRequest(r,fieldDefAry))
    }}/> );

export function DynamicForm({DynLayoutPanel, groupKey,fieldDefAry, onSubmit, onError=defaultOnError, onCancel, help_id, style={}}) {

    const onSearchSubmit= (request) => {
        if (!hasValidSpacialSearch(request,fieldDefAry)) {
            showInfoPopup(
                getSpacialSearchType(request,fieldDefAry)===CONE_CHOICE_KEY ?
                    'Target is required' : 'Search Area is require and must have a least 3 points');
            return false;
        }
        return onSubmit?.(convertRequest(request, fieldDefAry));
    };

    return (
        <Box style={style}>
            <FormPanel  {...{groupKey, onSuccess:onSearchSubmit, onError, onCancel, help_id}}
                slotProps={{
                    searchBar: {px:1, py:1/2, justifyContent: 'left'},
                }}>

                <DynamicFieldGroupPanel {...{DynLayoutPanel, groupKey,fieldDefAry}}/>
            </FormPanel>
        </Box>
    );
}

function cleanupPolygonString(inStr='') {
   return inStr.replaceAll(',', ' ')
       .split(' ')
       .filter( (s) => s)
       .join(' ');
}

export function convertRequest(request, fieldDefAry, standardIDType) {
    const retReq= fieldDefAry.reduce( (out, {key,type, targetDetails:{raKey,decKey, targetKey,polygonKey, sizeKey}={} }) => {
        const supportsPoly= findFieldDefType(fieldDefAry, POLYGON);
        const coneOrArea= request[CONE_AREA_KEY] ?? (targetKey ? CONE_CHOICE_KEY : supportsPoly ? POLY_CHOICE_KEY : CONE_CHOICE_KEY);
        if (coneOrArea) {
            if (type===POLYGON && coneOrArea !== POLY_CHOICE_KEY) return out;
            if (type===CIRCLE && coneOrArea !== CONE_CHOICE_KEY) return out;
            if (type===POSITION && coneOrArea !== CONE_CHOICE_KEY) return out;
            if (type===AREA && coneOrArea !== CONE_CHOICE_KEY) return out;
        }
        switch (type) {
            case FLOAT: case INT: case AREA: case ENUM:
                out[key]= request[key];
                break;
            case UNKNOWN:
                if (isDefined(request[key])) out[key]= request[key];
                break;
            case POLYGON:
                if (standardIDType===standardIDs.sia) out[key]= 'POLYGON ' +cleanupPolygonString(request[polygonKey]);
                else if (standardIDType===standardIDs.soda) out[key]= cleanupPolygonString(request[polygonKey]);
                else out[key]=request[polygonKey];
                break;
            case CHECKBOX:
                const value= Object.entries(request)
                    .find( ([k]) => k.includes(key))?.[1]?.includes(key) ?? false;
                out[key]= value;
                break;
            case WAVELENGTH:
                if (standardIDType===standardIDs.sia) out[WAVELENGTH]= DEFER_TO_CONTEXT;
                break;
            case SIA_OBSCORE_OPS:
                if (standardIDType===standardIDs.sia) out[SIA_OBSCORE_OPS]= DEFER_TO_CONTEXT;
                break;
            case POSITION:
                if (raKey && decKey) {
                    const wp= convertCelestial(parseWorldPt(request[key]), CoordinateSys.EQ_J2000);
                    out[raKey]= wp?.x;
                    out[decKey]= wp?.y;
                }
                else {
                    out[key]= request[targetKey];
                }
                break;
            case POINT:
                if (targetKey) {
                    const wp= convertCelestial(parseWorldPt(request[targetKey]), CoordinateSys.EQ_J2000);
                    if (wp) out[key]= makePointString(wp.x, wp.y,standardIDType);
                }
                break;
            case CIRCLE:
                if (targetKey) {
                    const radius= request[sizeKey];
                    const wp= convertCelestial(parseWorldPt(request[targetKey]), CoordinateSys.EQ_J2000);
                    if (radius && wp) out[key]= makeCircleString(wp.x,wp.y,radius,standardIDType);
                }
                break;
        }
        return out;
    }, {});
    const hiddenFields= fieldDefAry
        .reduce( (obj,{hide, key,initValue}) => {
            if (hide && key && initValue) obj[key]= initValue;
            return obj;
        },{});
    return {...retReq,...hiddenFields};
}



export function makeCircleString(ra,dec,radius,standardID) {
     return `${standardID?.toLowerCase()?.startsWith(standardIDs.sia)?'CIRCLE ':''}${ra} ${dec} ${radius}`;
}
function makePointString(ra,dec,standardID) {
    const sep= standardID?.toLowerCase().startsWith(standardIDs.ssa) ? ',' : ' ';
    return `${ra}${sep}${dec}`;
}

export function isCircleSearch(primaryFdAry) {
    return findFieldDefType(primaryFdAry,CIRCLE);
}

export function isPolySearch(primaryFdAry) {
    return findFieldDefType(primaryFdAry,POLYGON);
}

export function isPointAreaSearch(primaryFdAry) {
    return findFieldDefType(primaryFdAry,POINT) && findFieldDefType(primaryFdAry,POINT) ;
}

function getUnknownsConst(fdAry) {
    const unknownValues = fdAry
        .filter( (fd) => fd.type===UNKNOWN && fd.initValue)
        .map( (fd) => [fd.key,fd.initValue]);
    return Object.fromEntries(unknownValues);
}

export function convertCircleToPointArea(request, primaryFdAry, secondaryFdAry, primStandardID, secondStandardID) {
    const cKey= findFieldDefType(primaryFdAry,CIRCLE)?.key;
    if (!cKey) return;
    const cStr= request[cKey];
    if (!cStr) return;
    const stringToSplit= primStandardID!==standardIDs.sia ? cStr : 'circle ' + cStr;
    const [,ra,dec,radius]= stringToSplit.split(' ');
    if (!ra || !dec || !radius) return;
    const pKey= findFieldDefType(secondaryFdAry,POINT)?.key;
    const aKey= findFieldDefType(secondaryFdAry,AREA)?.key;
    if (!pKey || !aKey) return;
    return {[pKey]: makePointString(ra,dec,secondStandardID), [aKey]:radius, ...getUnknownsConst(secondaryFdAry)};
}

export function convertPointAreaToCircle(request, primaryFdAry, secondaryFdAry, primStandardID, secondStandardID) {
    const pKey= findFieldDefType(primaryFdAry,POINT)?.key;
    const aKey= findFieldDefType(primaryFdAry,AREA)?.key;
    if (!pKey || !aKey || !request[aKey]) return;
    const pStr= request[pKey];
    if (!pStr) return;
    const [ra,dec]= pStr.split(primStandardID?.toLowerCase().startsWith(standardIDs.ssa)?',':' ');
    if (!ra || !dec) return;
    const cKey= findFieldDefType(secondaryFdAry,CIRCLE)?.key;
    return {[cKey]: makeCircleString(ra,dec,request[aKey],secondStandardID), ...getUnknownsConst(secondaryFdAry)};
}



export function findTargetFromRequest(request, fieldDefAry) {
    let wp;
    fieldDefAry?.forEach( ({key,type, targetDetails:{raKey,decKey, targetKey}={} }) => {
        switch (type) {
            case POSITION:
                if (raKey && decKey) wp= convertCelestial(parseWorldPt(request[key]), CoordinateSys.EQ_J2000);
                return;
            case CIRCLE:
                wp= convertCelestial(parseWorldPt(request[targetKey]), CoordinateSys.EQ_J2000);
                return;
        }
    });
    return wp;
}

function InsetDynSearchPanel({style={}, fieldDefAry, popupHiPS= false, plotId='defaultHiPSTargetSearch', toolbarHelpId,
                                 obsCoreMetadataModel,
                                 childComponents, slotProps={}, submitSearch, dataServiceId, children}) {
    const { DynSpacialPanel, areaFields, polyPanel, checkBoxFields, fieldsInputAry, opsInputAry, SiaWLPanel,
        SiaObsCorePanel,
        useSpacial}= makeAllFields({ fieldDefAry,popupHiPS, plotId, toolbarHelpId, insetSpacial:true, submitSearch});

    let iFieldLayout= undefined;
    let nonSpacialComponents= undefined;
    if (fieldsInputAry.length || opsInputAry.length) {
        iFieldLayout= (
            <Stack>
                <Box key='top' sx={{pt:1/2}}/>
                <Stack {...{alignItems:'flex-start', alignSelf:'flex-start'}}>
                    {fieldsInputAry}
                </Stack>
                {Boolean(fieldsInputAry.length) && <div key='pad' style={{paddingTop:5}}/>}
                {opsInputAry}
            </Stack>);
    }

    const nonSpacialList= [polyPanel, areaFields?.length, iFieldLayout, checkBoxFields, SiaWLPanel,
        SiaObsCorePanel, childComponents];
    const hasNonSpacial= nonSpacialList.some(Boolean);

    if (hasNonSpacial) {
        nonSpacialComponents= (
            <Stack className='non-spatial'>
                {Boolean(polyPanel) && <Stack {...{direction:'column', alignItems:'center'}}> {polyPanel} </Stack>}
                <div style={{paddingLeft:5, display:'flex', flexDirection:'column', alignSelf:'flex-start'}}>
                    {Boolean(areaFields?.length) &&
                        <div key='a' style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                            {areaFields}
                        </div>}
                    <Stack spacing={1}>
                        {Boolean(SiaObsCorePanel) && <SiaObsCorePanel {...{dataServiceId,obsCoreMetadataModel}} />}
                        {Boolean(SiaWLPanel) && <SiaWLPanel {...{dataServiceId,obsCoreMetadataModel}} />}
                    </Stack>
                    <Stack {...{direction:'row', alignItems:'center'}}>
                        {Boolean(iFieldLayout) && <div>{iFieldLayout}</div>}
                        {Boolean(checkBoxFields) &&
                            <div style={{padding: '5px 0 0 45px', display:'flex', flexDirection:'column', alignSelf:'center'}}>
                                {checkBoxFields}
                            </div> }
                    </Stack>
                </div>
                {childComponents}
            </Stack>
        );
    }


    if (!useSpacial) {
        return (
            <div style={style}>
                <Slot {...{ component: FormPanel,
                    slotProps: slotProps?.FormPanel, help_id: 'dynDefaultSearchPanelHelp',
                    onError:() => showInfoPopup('Fix errors and search again', 'Error'),
                    cancelText:'', completeText:'Submit'}} >
                    {nonSpacialComponents}
                    {children}
                </Slot>
            </div>
        );
    }

    const spacialSlotProps= {...slotProps};
    setIfUndefined(spacialSlotProps, 'spatialSearch.sx.alignItems', 'stretch');
    setIfUndefined(spacialSlotProps, 'spatialSearch.sx.ml', 'auto');
    setIfUndefined(spacialSlotProps, 'spatialSearch.sx.mr', 'auto');


    return (
        <div style={style}>
            <Stack {...{alignItems:'center', height:'100%'}}>
                <DynSpacialPanel slotProps={spacialSlotProps}>
                    <Stack {...{spacing:1, my:1}}>
                        {nonSpacialComponents}
                        {children}
                    </Stack>
                </DynSpacialPanel>
            </Stack>
        </div>
    );
}

// this panel is experimental
function GridDynSearchPanel({style={}, fieldDefAry, popupHiPS= true, plotId='defaultHiPSTargetSearch', toolbarHelpId}) {
    const { DynSpacialPanel, areaFields, checkBoxFields, fieldsInputAry, opsInputAry, SiaWLPanel, dataServiceId,
        SiaObsCorePanel, useSpacial}= makeAllFields({ fieldDefAry,noLabels:true, plotId, popupHiPS, toolbarHelpId});

const labelAry= fieldDefAry
        .filter( ({type}) => type===INT || type===FLOAT || type===ENUM || type===UNKNOWN)
        .map( ({desc,units}) => `${desc}${makeUnitsStr(units)}` );

    let gridFieldLayout;
    if (fieldsInputAry.length || opsInputAry.length) {
        const combinedAry= [];
        [...fieldsInputAry,...opsInputAry].forEach( (f,idx) => {
            combinedAry.push((<div key={labelAry[idx]} style={{justifySelf:'end'}}>{labelAry[idx]}</div>));
            combinedAry.push(f);
        } );

        gridFieldLayout= (
            <div style={{
                paddingTop: 20,
                display: 'grid',
                columnGap: '3px',
                rowGap: '2px',
                gridTemplateRows : 'auto',
                gridTemplateColumns : 'auto auto auto auto',
                alignItems: 'center',
            }}>
                {combinedAry}
            </div>);
    }
    return (
        <Stack {...{alignItems: 'center', alignSelf: 'stretch', ...style}}>
            {useSpacial &&
                <Stack {...{alignItems:'center', alignSelf:'stretch'}}>
                    {<DynSpacialPanel/>}
                    {Boolean(SiaWLPanel) && <SiaWLPanel {...{dataServiceId}}/>}
                    {Boolean(areaFields?.length) && <> {areaFields} </>}
                </Stack>
            }
            {Boolean(checkBoxFields) && <div key='b' style={{paddingTop:5, display:'flex', flexDirection:'column'}}>
                {checkBoxFields}
            </div> }
            {Boolean(gridFieldLayout) && gridFieldLayout}
        </Stack>
    );
}

// this panel is experimental
function SimpleDynSearchPanel({style={}, fieldDefAry, popupHiPS= true, plotId='defaultHiPSTargetSearch', dataServiceId, toolbarHelpId}) {
    const { DynSpacialPanel, areaFields, polyPanel, checkBoxFields, fieldsInputAry, opsInputAry, SiaWLPanel,
        SiaObsCorePanel, useSpacial}= makeAllFields({ fieldDefAry,popupHiPS, plotId, toolbarHelpId});

    let iFieldLayout;
    if (fieldsInputAry.length || opsInputAry.length) {
        iFieldLayout= (
            <>
                <div key='top' style={{paddingTop:5}}/>
                {fieldsInputAry}
                {Boolean(fieldsInputAry.length) && <div key='pad' style={{paddingTop:5}}/>}
                {opsInputAry}
            </>);
    }


    return (
        <div style={style}>
            {useSpacial &&
                <Stack {...{alignItems:'center', height:'100%'}}>
                    <DynSpacialPanel/>
                </Stack>}
            {Boolean(polyPanel) &&
                <Stack {...{alignItems:'center'}}> {polyPanel} </Stack>}
            <Stack {...{pl:1/2, spacing:1}}>
                {Boolean(areaFields?.length) &&
                    <Stack key='a' {...{pt:1/2}}>
                        {areaFields}
                    </Stack>}
                <Stack {...{spacing:1, alignItems:'center'}}>
                    {Boolean(SiaWLPanel) && <SiaWLPanel {...{dataServiceId}} /> }
                    {Boolean(iFieldLayout) && <div>{iFieldLayout}</div>}
                    {Boolean(checkBoxFields) &&
                        <Stack {...{pt:1/2, pl:8, alignSelf:'center'}}>
                            {checkBoxFields}
                        </Stack> }
                </Stack>
            </Stack>
        </div>
    );
}


export const DynLayoutPanelTypes= {
    Simple: SimpleDynSearchPanel, //experimental
    Inset: InsetDynSearchPanel,
    Grid: GridDynSearchPanel, //experimental
};
