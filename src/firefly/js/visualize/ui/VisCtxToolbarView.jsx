/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {primePlot} from '../PlotViewUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {PlotAttribute} from '../WebPlot.js';
import {makeImagePt} from '../Point.js';
import {callGetAreaStatistics} from '../../rpc/PlotServicesJson.js';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {logError} from '../../util/WebUtil.js';


import {makeExtActivateData} from '../PlotCmdExtension.js';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';


import CROP from 'html/images/icons-2014/24x24_Crop.png';
import STATISTICS from 'html/images/icons-2014/24x24_Statistics.png';
import SELECTED from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECTED from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER from 'html/images/icons-2014/24x24_FilterAdd.png';




function crop(pv) {
    console.log('todo- crop:' + primePlot(pv).title);
}



//todo move the statistics constants to where they are needed
const Metrics= {MAX:'MAX', MIN:'MIN', CENTROID:'CENTROID', FW_CENTROID:'FW_CENTROID', MEAN:'MEAN',
    STDEV:'STDEV', INTEGRATED_FLUX:'INTEGRATED_FLUX', NUM_PIXELS:'NUM_PIXELS', PIXEL_AREA:'PIXEL_AREA'};



//TODO: call the dialog with the retrieved status
/**
 *
 * @param pv
 */
function stats(pv) {
    console.log('Stats getting: ' + primePlot(pv).title);
    console.log(Metrics);
    var p= primePlot(pv);
    var cc= CysConverter.make(p);
    var sel= p.attributes[PlotAttribute.SELECTION];

    var ip0=  cc.getImageCoords(sel.pt0);
    var ip2=  cc.getImageCoords(sel.pt1);
    var ip1=  makeImagePt(ip2.x,ip0.y);
    var ip3=  makeImagePt(ip1.x,ip2.y);


    callGetAreaStatistics(p.plotState, ip0,ip1,ip2,ip3)
        .then( (wpResult) => {
            console.log(wpResult);
            console.log('TODO: Stats: show the dialog here and add overlays: ' + primePlot(pv).title);
        })
        .catch ( (e) => {
            logError(`error, stat , plotId: ${p.plotId}`, e);
        });

}

function select(pv,dlAry) {
    console.log('todo- select:' + primePlot(pv).title);
}

function unselect(pv,dlAry) {
    console.log('todo- unselect:' + primePlot(pv).title);
}

function filter(pv,dlAry) {
    console.log('todo- filter:' + primePlot(pv).title);
}



function makeExtensionButtons(extensionAry,pv,dlAry) {
    if (!extensionAry) return false;
    return extensionAry.map( (ext,idx) => (
            <ToolbarButton icon={ext.imageUrl} text={ext.title}
                           tip={ext.toolTip} key={ext.id}
                           horizontal={true} enabled={true}
                           visible={true}
                           lastTextItem={idx===(extensionAry.length-1)}
                           onClick={() => dispatchExtensionActivate(ext,makeExtActivateData(ext,pv,dlAry))}/>
        )
    );
}


/**
 *
 * @param pv
 * @param dlAry
 * @param extensionAry
 * @param showCrop
 * @param showStats
 * @param showSelect
 * @param showUnSelect
 * @param showFilter
 * @param extensionAry
 * @return {XML}
 * @constructor
 */
export function VisCtxToolbarView({plotView:pv, dlAry, extensionAry, showCrop, showStats,
                                   showSelect, showUnSelect, showFilter}) {

    var rS= {
        width: '100% - 2px',
        height: 34,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap'

    };
    var showSeparator= (showCrop|| showStats|| showSelect|| showUnSelect|| showFilter)
                        && extensionAry && extensionAry.length;

    return (
        <div style={rS}>
            <div
                style={{display: 'inline-block', padding: '8px 7px 0 5px', float : 'left', fontStyle: 'italic'}}>
                Options:</div>
            <ToolbarButton icon={CROP}
                           tip='Crop the image to the selected area'
                           horizontal={true}
                           visible={showCrop}
                           todo={true}
                           onClick={() => crop(pv)}/>

            <ToolbarButton icon={STATISTICS}
                           tip='Show statistics for the selected area'
                           horizontal={true}
                           visible={showStats}
                           todo={true}
                           onClick={() => stats(pv)}/>

            <ToolbarButton icon={SELECTED}
                           tip='Mark data in area as selected'
                           horizontal={true}
                           visible={showSelect}
                           todo={true}
                           onClick={() => select(pv,dlAry)}/>

            <ToolbarButton icon={UNSELECTED}
                           tip='Mark all data unselected'
                           horizontal={true}
                           visible={showUnSelect}
                           todo={true}
                           onClick={() => unselect(pv,dlAry)}/>

            <ToolbarButton icon={FILTER}
                           tip='Filter in the selected area'
                           horizontal={true}
                           visible={showFilter}
                           todo={true}
                           onClick={() => filter(pv,dlAry)}/>


            {makeExtensionButtons(extensionAry,pv,dlAry)}
        </div>
    );
}


VisCtxToolbarView.propTypes= {
    plotView : PropTypes.object.isRequired,
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    extensionAry : PropTypes.arrayOf(React.PropTypes.object),
    showCrop : PropTypes.bool,
    showStats : PropTypes.bool,
    showSelect : PropTypes.bool,
    showUnSelect : PropTypes.bool,
    showFilter : PropTypes.bool
};



ToolbarButton.defaultProps= {
    showCrop : false,
    showStats : false,
    showSelect : false,
    showUnSelect : false,
    showFilter : false,
    extensionAry : null
};




