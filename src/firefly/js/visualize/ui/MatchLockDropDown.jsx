import {Box, Tooltip, Typography} from '@mui/joy';
import React from 'react';
import {get} from 'lodash';
import {dispatchWcsMatch, WcsMatchType} from '../ImagePlotCntlr.js';
import {getPlotViewAry, hasWCSProjection, primePlot} from '../PlotViewUtil.js';
import {PlotAttribute} from '../PlotAttribute';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {isImage} from '../WebPlot';
import {showInfoPopup} from '../../ui/PopupUtil';
import {LockImages} from './Buttons.jsx';

function changeMatchType(vr, matchType, lockMatch) {
    const plot= primePlot(vr);
    if (!plot) return;
    //check for images without WCS when aligning by WCS or Target, and display appropriate warning
    if(matchType.key === 'Standard' || matchType.key === 'Target') {
        const warningTitles= getPlotViewAry(vr)
                .filter( (pv) => !hasWCSProjection(primePlot(pv)) )
                .map ( (pv) => primePlot(pv)?.title ?? '');
        if (warningTitles.length !== 0) { //at least one image without WCS
            const msgTitle = 'The following image(s) do not have celestial WCS:';
            const msgDesc = warningTitles.join(', ');
            const renderContent = (
                <Box>
                    <Typography level='title-md'>{msgTitle} </Typography>
                    <br/> <br/>
                    <Typography style={{padding: '5', whiteSpace: 'normal', letterSpacing: '1', lineHeight: '1.5'}}>
                        {msgDesc}
                    </Typography>
                </Box>);
            showInfoPopup(renderContent, 'Warning');
        }
    }
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



export function MatchLockDropDown({visRoot:vr, enabled, visible, inDropDown=false, imageStyle}) {
    const {wcsMatchType}= vr;
    const wcsCnt= getCountWithWCS(vr);
    const tCnt= getCountWithTarget(vr);
    const imageCnt= getCountWithImages(vr);
    const p= primePlot(vr);
    const hasWcs= p && hasWCSProjection(p);
    const hasTarget= Boolean(p && get(p, ['attributes',PlotAttribute.FIXED_TARGET]));
    const buttonIndentStyle = {marginLeft: 15};

    const dropDown= (
        <SingleColumnMenu>
            <Tooltip title={'Align all images to the selected image, but leave unlocked'}>
                <Typography> Align-only Options </Typography>
            </Tooltip>
            <ToolbarButton text='by WCS' tip='Align by WCS (no locking)'
                           enabled={hasWcs && wcsCnt>1}
                           horizontal={false} key={'by wcs'}
                           style={buttonIndentStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Standard, false)}/>

            <ToolbarButton text='by Target' tip='Align by Target (no locking)'
                           enabled={hasTarget && tCnt>1}
                           horizontal={false} key={'by target'}
                           style={buttonIndentStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Target, false)}/>

            <ToolbarButton text='by Pixel Origins' tip='Align by Pixel Origins (no locking)'
                           enabled={isImage(p) && imageCnt>1}
                           horizontal={false} key={'by pixel'}
                           style={buttonIndentStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.Pixel, false)}/>

            <ToolbarButton text='by Pixel at Image Centers' tip='Align by Pixel at Image Centers (no locking)'
                           enabled={isImage(p) && imageCnt>1}
                           horizontal={false} key={'by pixel/center'}
                           style={buttonIndentStyle}
                           hasCheckBox={true}
                           onClick={() => changeMatchType(vr, WcsMatchType.PixelCenter, false)}/>

            <DropDownVerticalSeparator useLine={true}/>
            <Tooltip title={'Align all images to the selected image, and lock this mode'}>
                <Typography>Align and Lock Options</Typography>
            </Tooltip>

            <ToolbarButton text='Unlock' tip='Unlock the alignment of all images' hasCheckBox={true} checkBoxOn={!wcsMatchType}
                                     enabled={true} horizontal={false} key={'unlock'}
                                     style={buttonIndentStyle}
                                     onClick={() => changeMatchType(vr, false)}/>

            <ToolbarButton text='by WCS' tip='Align by WCS & Lock'
                           enabled={hasWcs}
                           horizontal={false} key={'by wcs & Lock'}
                           hasCheckBox={true} checkBoxOn={wcsMatchType===WcsMatchType.Standard}
                           style={buttonIndentStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Standard, true)}/>

            <ToolbarButton text='by Target' tip='Align by Target & Lock'
                           enabled={hasTarget}
                           horizontal={false} key={'by target & Lock'}
                           hasCheckBox={true} checkBoxOn={wcsMatchType===WcsMatchType.Target}
                           style={buttonIndentStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Target, true)}/>

            <ToolbarButton text='by Pixel Origin' tip='Align by Pixel Origin & Lock'
                           enabled={isImage(p)}
                           hasCheckBox={true}
                           checkBoxOn={wcsMatchType===WcsMatchType.Pixel}
                           horizontal={false} key={'by pixel & Lock'}
                           style={buttonIndentStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.Pixel, true)}/>

            <ToolbarButton text='by Pixel at Image Centers' tip='Align by Pixel at Image Centers & Lock'
                           enabled={isImage(p)}
                           hasCheckBox={true}
                           checkBoxOn={wcsMatchType===WcsMatchType.PixelCenter}
                           horizontal={false} key={'by pixel/center & Lock'}
                           style={buttonIndentStyle}
                           onClick={() => changeMatchType(vr, WcsMatchType.PixelCenter, true)}/>
        </SingleColumnMenu>
    );

    return (
        <LockImages locked={Boolean(wcsMatchType)}
                               tip='Image alignment drop down: determine how to align images'
                               enabled={enabled} horizontal={true}
                               visible={visible}
                               disableHiding={inDropDown}
                               dropDownKey={inDropDown? 'matchLock' : undefined}
                               useDropDownIndicator={true}
                               imageStyle={imageStyle}
                               dropDown={dropDown}/>

    );
}
