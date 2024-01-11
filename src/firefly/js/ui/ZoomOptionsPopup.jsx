/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Stack} from '@mui/joy';
import React, {useEffect} from 'react';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {getActivePlotView, getFoV, primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchZoom} from '../visualize/ImagePlotCntlr.js';
import {imageLevels, UserZoomTypes, makeFoVString} from '../visualize/ZoomUtil';
import {ToolbarButton} from './ToolbarButton.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';

export function showZoomOptionsPopup() {
    const popup = ( <PopupPanel title='Choose Field of View'> <ZoomOptionsPopup/> </PopupPanel> );
    DialogRootContainer.defineDialog('zoomOptionsDialog', popup);
    dispatchShowDialog('zoomOptionsDialog');
}

const ZoomOptionsPopup = () => {
    const pv= useStoreConnector( () => getActivePlotView(visRoot()));
    useEffect(() => void (!primePlot(pv) && dispatchHideDialog('zoomOptionsDialog')), [pv]);
    return primePlot(pv) ? <ZoomOptionsPopupForm pv={pv}/> : <div/>;
};

const ZoomOptionsPopupForm= ({pv}) => (
    <Stack {...{ minWidth:150, minHeight: 300, direction:'row', spacing:2} }>
        <Box minWidth='8rem'> {makeZoomItems(pv, imageLevels.slice(0,imageLevels.length/2))} </Box>
        <Box minWidth='8rem'> {makeZoomItems(pv, imageLevels.slice(imageLevels.length/2, imageLevels.length))} </Box>
    </Stack>
);

const makeZoomItems= (pv,levels) =>
    levels
    .map((zl) => ({label: makeFoVString(getFoV(pv,zl)), level: zl}))
    .map( ({label,level},opId) =>
        (level===primePlot(pv).zoomFactor) ?
            (<u key={opId}>{`${label} : Current`}</u> ) :
            (<ToolbarButton text={label} tip={label} horizontal={false} key={opId}
                            onClick={() => dispatchZoom({plotId:pv.plotId, userZoomType:UserZoomTypes.LEVEL, level})}/>)
    );