/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Card, Stack, Typography} from '@mui/joy';
import {func, object, string} from 'prop-types';
import React, {memo, useContext, useEffect, useState} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {computeCentralPtRadiusAverage} from '../../visualize/VisUtil.js';
import {FieldGroup, FieldGroupCtx} from '../FieldGroup.jsx';
import {getServiceMetaOptions, loadSiaV2Meta, makeObsCoreMetadataModel} from '../tap/SiaUtil';
import {CONTEXT_PARAMS_STR, convertRequest, DEFER_TO_CONTEXT, DynLayoutPanelTypes} from './DynamicUISearchPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {isSimpleTargetPanel} from './DynComponents';
import {getStandardIdType, hasAnySpacial, isSIAStandardID, sdToFieldDefAry} from './ServiceDefTools.js';
import {ConstraintContext} from '../tap/Constraints';
import ShapeDataObj from '../../visualize/draw/ShapeDataObj.js';

/**
 * @typedef {Object} SearchAreaInfo
 *
 * @prop {String} examples
 * @prop {String} moc
 * @prop {String} mocDesc
 * @prop {number} maxFetchDepth
 * @prop {String} HiPS
 * @prop {String} hips_initial_fov
 * @prop {String} standardID
 * @prop {WorldPt} centerWp
 * @prop {CoordinateSys} coordinateSys
 */

const GROUP_KEY= 'ActivateMenu';

export const ServiceDescriptorPanel= memo(({ serviceDefRef='none', serDef, setSearchParams,
                                               title, makeDropDown, sRegion,
                                               plotId= 'defaultHiPSTargetSearch'}) => {

    const {valid,drawObj}= parseObsCoreRegion(sRegion) ?? {valid:false};
    let fovSize;
    if (valid) {
        const wp= ShapeDataObj.draw.getCenterPt(drawObj);
        const validPts = drawObj.pts.filter( (pt) => pt);
        const results= computeCentralPtRadiusAverage([validPts],.11);
        fovSize= results.fovSize * 2.4;
        console.log(wp);
    }

    const fieldDefAry= sdToFieldDefAry({
        serviceDef:serDef, sRegion, hipsUrl:getAppOptions()?.coverage?.hipsSourceURL, fovSize });
    const {hasSpacial, simpleTargetUI}= analyzeSpecial(fieldDefAry);
    let formSx={};
    if (hasSpacial) {
        if (simpleTargetUI) formSx= { ml: 1/2, mr: 2, mt: 2 };
    }
    else {
        formSx= { ml: 1/2, mr: 2, mt: 2 };
    }


    return (
        <Stack {...{direction:'row', key:serviceDefRef, width:1, height:(hasSpacial&&!simpleTargetUI) ? 1 : undefined}}>
            <Stack {...{component:hasSpacial?undefined:Card,
                direction:'column', p:hasSpacial?.25: undefined, width:1, ...formSx,
                variant: hasSpacial ? undefined : 'outlined', }}>
                <Stack {...{direction:'row', alignItems:'center'}}>
                    {makeDropDown?.()}
                    <Typography level= {title?.length>80 ? 'title-sm' : 'title-lg'}>{title}</Typography>
                </Stack>

                <FieldGroup groupKey={GROUP_KEY} keepState={false} style={{display:'flex', flexGrow:1}}>
                    <SDPanelContent {...{fieldDefAry, plotId, setSearchParams, serDef}}/>
                </FieldGroup>
            </Stack>
        </Stack>
    );
});

function SDPanelContent({fieldDefAry, plotId, serDef={}, setSearchParams, dataServiceId= undefined}) {
    const {getVal,setVal} = useContext(FieldGroupCtx);
    const [obsCoreMetadataModel, setObsCoreMetadataModel]= useState(undefined);
    const {accessURL,standardID}= serDef;

    const constraintCtx= {
        setConstraintFragment: (key,value) => {
            const fragments= getVal('SIA_CTX') ?? new Map();
            value ? fragments.set(key,value) : fragments.delete(key);
            setVal('SIA_CTX',fragments);
        }
    };

    useEffect( () => {
        if (accessURL && isSIAStandardID(standardID)) {
            loadSiaV2Meta(accessURL).then( (siaMeta) =>{
                const fallbackMetaOptions= getServiceMetaOptions(dataServiceId) ?? [];
                setObsCoreMetadataModel(makeObsCoreMetadataModel(siaMeta, fallbackMetaOptions));
            });
        }
    },[standardID,accessURL]);

    const submitSearch= (r) => {
        let convertedR;
        const step1 = convertRequest(r, fieldDefAry, getStandardIdType(standardID));
        if (isSIAStandardID(standardID)) {
            const step2 = Object.fromEntries(Object.entries(step1).filter(([k, v]) => v !== DEFER_TO_CONTEXT));
            const siaCtx= getVal('SIA_CTX') ?? new Map();
            const cAry= [...siaCtx.values()];
            const anyErrorStr= cAry.find( (c) => c.constraintErrors.length)?.constraintErrors[0];
            if (anyErrorStr) {
                showInfoPopup(anyErrorStr);
                return false;
            }
            const siaConstraints= cAry.map( (f) =>  f.siaConstraints).filter( (c) => c?.length).flat();
            convertedR= {...step2, [CONTEXT_PARAMS_STR]:siaConstraints.join('&')};
        }
        else {
            convertedR= step1;
        }
        setSearchParams(convertedR);
    };

    const {hasSpacial, simpleTargetUI}= analyzeSpecial(fieldDefAry);

    return (
        <ConstraintContext.Provider value={constraintCtx}>
            <DynLayoutPanelTypes.Inset fieldDefAry={fieldDefAry} style={{width: '100%'}}
                                       slotProps={{
                                           FormPanel: {
                                               onSuccess: (r) => submitSearch(r),
                                               onError: () => showInfoPopup('Some field are not valid'),
                                               sx: hasSpacial && simpleTargetUI ? {alignSelf:'flex-start', mt:3} : undefined,
                                           },
                                       }}
                                       obsCoreMetadataModel={obsCoreMetadataModel}
                                       plotId={plotId}>
                <Stack direction='column' alignItems='flex-start' p={.25}>
                    <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'flex-start',
                        pt:.5, pr:.5, width:'100%', alignSelf:'flex-start' }}>
                        {hasSpacial && !simpleTargetUI &&
                            <Typography level='body-xs'>Enter search position or click on background HiPS</Typography>}
                    </Stack>
                </Stack>
            </DynLayoutPanelTypes.Inset>
        </ConstraintContext.Provider>
    );

}




ServiceDescriptorPanel.propTypes= {
    serDef: object,
    title: string,
    serviceDefRef: string,
    setSearchParams: func,
    makeDropDown: func,
    sRegion: string,
    standardID: string,
    plotId: string,
};




function analyzeSpecial(fieldDefAry) {
    if (hasAnySpacial(fieldDefAry)) {
        return {hasSpacial: true, simpleTargetUI: isSimpleTargetPanel(fieldDefAry)};
    }
    else {
        return {hasSpacial:false, simpleTargetUI:false};
    }
}