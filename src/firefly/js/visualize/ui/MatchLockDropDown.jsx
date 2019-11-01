import React from 'react';
import {get} from 'lodash';
import {dispatchWcsMatch, WcsMatchType} from '../ImagePlotCntlr.js';
import {getPlotViewAry, hasWCSProjection, primePlot} from '../PlotViewUtil.js';
import {PlotAttribute} from '../PlotAttribute';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';

import MATCH_LOCKED from 'html/images/28x28_Match_Locked.png';
import MATCH_UNLOCKED from 'html/images/28x28_Match_Unlocked.png';
import {isImage} from '../WebPlot';

function changeMatchType(vr, matchType, lockMatch) {
    const plot= primePlot(vr);
    if (!plot) return;
    dispatchWcsMatch({matchType, plotId:plot.plotId, lockMatch});
}

/**
 * @param {VisRoot} vr
 * @return {number}
 */
const getCountWithWCS= (vr) => getPlotViewAry(vr).filter( (pv) => hasWCSProjection(pv)).length;

const getCountWithImages= (vr) => getPlotViewAry(vr).filter( (pv) => isImage(primePlot(pv))).length;

/**
 * @param {VisRoot} vr
 * @return {number}
 */
const getCountWithTarget= (vr) =>
    getPlotViewAry(vr).filter( (pv) => get(primePlot(pv), ['attributes',PlotAttribute.FIXED_TARGET]) ).length;



export function MatchLockDropDown({visRoot:vr, enabled, visible}) {
    const {wcsMatchType}= vr;
    const wcsCnt= getCountWithWCS(vr);
    const tCnt= getCountWithTarget(vr);
    const imageCnt= getCountWithImages(vr);
    const p= primePlot(vr);
    const hasWcs= p && hasWCSProjection(p);
    const hasTarget= Boolean(p && get(p, ['attributes',PlotAttribute.FIXED_TARGET]));
    const additionalStyle = {marginLeft: 15};
    const titleDiv= {fontSize:'10pt', fontWeight: 'bold', padding: '0 0 3px 0'};

    const dropDown= (
        <SingleColumnMenu>
            <div style={titleDiv} title='Align all images to the selected image, but leave unlocked'>
                Align-only Options
            </div>
            <ToolbarButton text='by WCS' tip='Align by WCS (no locking)'
                           enabled={hasWcs && wcsCnt>1}
                           horizontal={false} key={'by wcs'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Standard, false)}/>

            <ToolbarButton text='by Target' tip='Align by Target (no locking)'
                           enabled={hasTarget && tCnt>1}
                           horizontal={false} key={'by target'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Target, false)}/>

            <ToolbarButton text='by Pixel Origins' tip='Align by Pixel Origins (no locking)'
                           enabled={isImage(p) && imageCnt>1}
                           horizontal={false} key={'by pixel'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Pixel, false)}/>

            <ToolbarButton text='by Pixel at Image Centers' tip='Align by Pixel at Image Centers (no locking)'
                           enabled={isImage(p) && imageCnt>1}
                           horizontal={false} key={'by pixel/center'}
                           additionalStyle={additionalStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.PixelCenter, false)}/>

            <DropDownVerticalSeparator useLine={true}/>
            <div style={titleDiv} title='Align all images to the selected image, and lock this mode'>
                Align and Lock Options
            </div>

            <ToolbarButton text='Unlock' tip='Unlock the alignment of all images' hasCheckBox={true} checkBoxOn={!wcsMatchType}
                                     enabled={true} horizontal={false} key={'unlock'}
                                     additionalStyle={additionalStyle}
                                     onClick={() => changeMatchType(vr, false)}/>

            <ToolbarButton text='by WCS' tip='Align by WCS & Lock'
                           enabled={hasWcs}
                           horizontal={false} key={'by wcs & Lock'}
                           hasCheckBox={true} checkBoxOn={wcsMatchType===WcsMatchType.Standard}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Standard, true)}/>

            <ToolbarButton text='by Target' tip='Align by Target & Lock'
                           enabled={hasTarget}
                           horizontal={false} key={'by target & Lock'}
                           hasCheckBox={true} checkBoxOn={wcsMatchType===WcsMatchType.Target}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Target, true)}/>

            <ToolbarButton text='by Pixel Origin' tip='Align by Pixel Origin & Lock'
                           enabled={isImage(p)}
                           hasCheckBox={true}
                           checkBoxOn={wcsMatchType===WcsMatchType.Pixel}
                           horizontal={false} key={'by pixel & Lock'}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Pixel, true)}/>

            <ToolbarButton text='by Pixel at Image Centers' tip='Align by Pixel at Image Centers & Lock'
                           enabled={isImage(p)}
                           hasCheckBox={true}
                           checkBoxOn={wcsMatchType===WcsMatchType.PixelCenter}
                           horizontal={false} key={'by pixel/center & Lock'}
                           additionalStyle={additionalStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.PixelCenter, true)}/>
        </SingleColumnMenu>
    );

    return (
        <DropDownToolbarButton icon={wcsMatchType?MATCH_LOCKED:MATCH_UNLOCKED }
                               tip='Determine how to align images'
                               enabled={enabled} horizontal={true}
                               visible={visible}
                               hasHorizontalLayoutSep={false}
                               useDropDownIndicator={true}
                               dropDown={dropDown}/>

    );

}
