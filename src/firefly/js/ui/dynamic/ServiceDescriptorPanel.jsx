/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

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

const titleStyle= {textAlign:'center', padding:'0 0 0 50px', fontSize:'larger', fontWeight:'bold'};


export const ServiceDescriptorPanel= memo(({ serviceDefRef='none', serDefParams, setSearchParams,
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

    const fieldDefAry= makeFieldDefs(serDefParams, sRegion, undefined, true,
        getAppOptions()?.coverage?.hipsSourceURL, fovSize);

    const submitSearch= (r) => {
        const convertedR= convertRequest(r,fieldDefAry,getStandardIdType(standardID));
        setSearchParams(convertedR);
    };





    const Wrapper= ({children}) => (
        <div style={{ display:'flex', flexDirection:'column', alignItems:'center', paddingBottom:2}}>
            {children}
            <div style={{display:'flex', flexDirection:'row', padding: '5px 5px 0 0', width:'100%',
                alignSelf:'flex-start', justifyContent:'space-between', alignItems:'center'}}>
                <CompleteButton onSuccess={(r) => submitSearch(r)}
                                onFail={() => showInfoPopup('Some field are not valid')}
                                text={'Submit'} groupKey={GROUP_KEY} />
                {hasAnySpacial(fieldDefAry) &&
                    <div style={{fontStyle:'italic'}}>
                        Enter search position or click on background HiPS
                    </div>}
            </div>
        </div>
    );




    return (
        <div key={serviceDefRef} style={{display:'flex', width:'100%'}}>
            <div style={{padding: '1px 1px 1px 1px', display:'flex', flexDirection:'column', width:'100%'}}>
                <div className='ComponentBackground' style={{display:'flex', flexDirection:'row', justifyContent:'flex-start', alignItems:'center'}}>
                    {makeDropDown?.()}
                    <div style= {titleStyle}> {title} </div>
                </div>

                <FieldGroup groupKey={GROUP_KEY} keepState={false} style={{display:'flex', flexGrow:1}}>
                    <DynLayoutPanelTypes.Inset fieldDefAry={fieldDefAry} style={{width: '100%'}}
                                               WrapperComponent={Wrapper}
                                               plotId={plotId}/>
                </FieldGroup>
            </div>
        </div>
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


