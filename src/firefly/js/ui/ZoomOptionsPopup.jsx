/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
    const popup = ( <PopupPanel title={'Choose Field of View'}> <ZoomOptionsPopup/> </PopupPanel> );
    DialogRootContainer.defineDialog('zoomOptionsDialog', popup);
    dispatchShowDialog('zoomOptionsDialog');
}

const ZoomOptionsPopup = () => {
    const [pv]= useStoreConnector( () => getActivePlotView(visRoot()));
    useEffect(() => void (!primePlot(pv) && dispatchHideDialog('zoomOptionsDialog')), [pv]);
    return primePlot(pv) ? <ZoomOptionsPopupForm pv={pv}/> : <div/>;
};

const ZoomOptionsPopupForm= ({pv}) => (
    <div style={{ minWidth:150, minHeight: 300} }>
        <div style={{display: 'inline-block', padding: '10px 10px 20px 25px'}}> {makeZoomItems(pv)} </div>
    </div>
);

const makeZoomItems= (pv) => imageLevels
    .map((zl) => ({label: makeFoVString(getFoV(pv,zl)), level: zl}))
    .map( ({label,level},opId) =>
        (level===primePlot(pv).zoomFactor) ?
            (<u key={opId}>{`${label} : Current`}</u> ) :
            (<ToolbarButton text={label} tip={label} horizontal={false} key={opId}
                            onClick={() => dispatchZoom({plotId:pv.plotId, userZoomType:UserZoomTypes.LEVEL, level})}/>)
    );