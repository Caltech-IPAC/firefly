/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import {arrayOf, func, object, string} from 'prop-types';
import React, {memo} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {computeCentralPtRadiusAverage} from '../../visualize/VisUtil.js';
import CompleteButton from '../CompleteButton.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {convertRequest, DynLayoutPanelTypes} from './DynamicUISearchPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {getStandardIdType, hasAnySpacial, makeFieldDefs} from './ServiceDefTools.js';
import ShapeDataObj from '../../visualize/draw/ShapeDataObj.js';

/**
 * @typedef {Object} SearchAreaInfo
 *
 * @prop {String} examples
 * @prop {String} moc
 * @prop {String} mocDesc
 * @prop {String} HiPS
 * @prop {String} hips_initial_fov
 * @prop {String} standardID
 * @prop {WorldPt} centerWp
 * @prop {CoordinateSys} coordinateSys
 */

const GROUP_KEY= 'ActivateMenu';

export const ServiceDescriptorPanel= memo(({ serviceDefRef='none', serDef, setSearchParams,
                                               title, makeDropDown, sRegion, standardID,
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

    const fieldDefAry= makeFieldDefs(serDef?.serDefParams, sRegion, undefined, true,
        getAppOptions()?.coverage?.hipsSourceURL, fovSize);

    const submitSearch= (r) => {
        const convertedR= convertRequest(r,fieldDefAry,getStandardIdType(standardID));
        setSearchParams(convertedR);
    };


    return (
        <Stack {...{direction:'row', key:serviceDefRef, width:1, height:1}}>
            <Stack {...{direction:'column', p:.25, width:1}}>
                <Stack {...{direction:'row', alignItems:'center'}}>
                    {makeDropDown?.()}
                    <Typography level= {title?.length>30 ? 'body-xs' : 'body-sm'}>{title}</Typography>
                </Stack>

                <FieldGroup groupKey={GROUP_KEY} keepState={false} style={{display:'flex', flexGrow:1}}>
                    <DynLayoutPanelTypes.Inset fieldDefAry={fieldDefAry} style={{width: '100%'}}
                                               slotProps={{
                                                   FormPanel: {
                                                      onSuccess: (r) => submitSearch(r),
                                                       onError: () => showInfoPopup('Some field are not valid')
                                                   }
                                               }}
                                               plotId={plotId}>
                        <Stack direction='column' alignItems='flex-start' p={.25}>
                            <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'flex-start',
                                pt:.5, pr:.5, width:'100%', alignSelf:'flex-start' }}>
                                {/*<CompleteButton onSuccess={(r) => submitSearch(r)}*/}
                                {/*                onFail={() => showInfoPopup('Some field are not valid')}*/}
                                {/*                text={'Submit'} groupKey={GROUP_KEY} />*/}
                                {hasAnySpacial(fieldDefAry) &&
                                    <Typography level='body-xs'>Enter search position or click on background HiPS</Typography>}
                            </Stack>
                        </Stack>
                    </DynLayoutPanelTypes.Inset>
                </FieldGroup>
            </Stack>
        </Stack>
    );
});

ServiceDescriptorPanel.propTypes= {
    serDefParams: arrayOf(object),
    title: string,
    serviceDefRef: string,
    setSearchParams: func,
    makeDropDown: func,
    sRegion: string,
    standardID: string,
    plotId: string,
};


