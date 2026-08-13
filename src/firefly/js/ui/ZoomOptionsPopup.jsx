/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Stack} from '@mui/joy';
import React, {useEffect} from 'react';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {getFoV, primePlot, currentP} from '../visualize/PlotViewUtil.js';
import {dispatchZoom} from '../visualize/ImagePlotDispatch';
import {UserZoomTypes} from '../visualize/VisConst';
import {imageLevels, makeFoVString} from '../visualize/ZoomUtil';
import {ToolbarButton} from './ToolbarButton.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';

export function showZoomOptionsPopup() {
    const popup = ( <PopupPanel title='Choose Field of View'> <ZoomOptionsPopup/> </PopupPanel> );
    DialogRootContainer.defineDialog('zoomOptionsDialog', popup);
    dispatchShowDialog('zoomOptionsDialog');
}

const ZoomOptionsPopup = () => {
    const {pv,plot}= useStoreConnector( () => currentP());
    useEffect(() => void (!plot && dispatchHideDialog('zoomOptionsDialog')), [pv]);
    return plot ? <ZoomOptionsPopupForm pv={pv}/> : <div/>;
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