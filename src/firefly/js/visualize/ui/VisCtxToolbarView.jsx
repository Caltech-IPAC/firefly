/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {primePlot,isMultiImageFitsWithSameArea,getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {PlotAttribute} from '../WebPlot.js';
import {makeImagePt} from '../Point.js';
import {callGetAreaStatistics} from '../../rpc/PlotServicesJson.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {logError} from '../../util/WebUtil.js';
import SelectArea from '../../drawingLayers/SelectArea.js';
import {showImageAreaStatsPopup} from './ImageStatsPopup.jsx';

import {dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import {dispatchCrop} from '../ImagePlotCntlr.js';
import {makeExtActivateData} from '../PlotCmdExtension.js';
import {dispatchExtensionActivate} from '../../core/ExternalAccessCntlr.js';
import {selectCatalog,unselectCatalog,filterCatalog,clearFilterCatalog} from '../../drawingLayers/Catalog.js';

import CROP from 'html/images/icons-2014/24x24_Crop.png';
import STATISTICS from 'html/images/icons-2014/24x24_Statistics.png';
import SELECTED from 'html/images/icons-2014/24x24_Checkmark.png';
import UNSELECTED from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import FILTER from 'html/images/icons-2014/24x24_FilterAdd.png';
import CLEAR_FILTER from 'html/images/icons-2014/24x24_FilterOff_Circle.png';

import CoordUtil from '../CoordUtil.js';
import { parseImagePt } from '../Point.js';


//todo move the statistics constants to where they are needed
const Metrics= {MAX:'MAX', MIN:'MIN', CENTROID:'CENTROID', FW_CENTROID:'FW_CENTROID', MEAN:'MEAN',
    STDEV:'STDEV', INTEGRATED_FLUX:'INTEGRATED_FLUX', NUM_PIXELS:'NUM_PIXELS', PIXEL_AREA:'PIXEL_AREA'};


/**
 * representing the number into decimal or exponential format
 * @param {number} [num]
 * @param {number} [fractionDigits=7] fractional digits after the decimal point
 * @return {string}
 */

function formatNumber(num, fractionDigits=7)
{
    let numStr, thred;
    const SigDig = 7;


    if (num === null || num === undefined) {
        return '';
    }
    thred = Math.min(Math.pow(10, SigDig - fractionDigits), 1.0);

    if (Math.abs(num) >= thred) {
        numStr = num.toFixed(fractionDigits);
    } else {
        numStr = num.toExponential(fractionDigits);
    }
    return numStr;
}

/**
 * tabularize the area statistics into summary and stats two sections for popup display
 * @param {object} wpResult
 * @param {object} cc
 * @returns { object } {statsSummary: array (for summary items), statsTable: array (for table rows)}}
 */

function tabulateStatics(wpResult, cc) {

    const SSummary = 'statsSummary';
    const STable   = 'statsTable';
    const ipMetrics = [Metrics.MIN, Metrics.MAX, Metrics.CENTROID, Metrics.FW_CENTROID];
    const bands  = wpResult.Band_Info;
    const noBand = 'NO_BAND';

    var bandData = {};

    function getOneStatSet(b) {

        let tblData = {};

        tblData[SSummary] = [
            [b.MEAN.desc, formatNumber(b.MEAN.value) + ' ' + b.MEAN.units],
            [b.STDEV.desc, formatNumber(b.STDEV.value) + ' ' + b.STDEV.units],
            [b.INTEGRATED_FLUX.desc,
                formatNumber(b.INTEGRATED_FLUX.value) + ' ' + b.INTEGRATED_FLUX.units]
        ];

        tblData[STable] = [{'cells': ['', 'Position', 'Value']}];

        for (let i = 0; i < ipMetrics.length; i++) {
            if (!b.hasOwnProperty(ipMetrics[i])) {
                continue;
            }

            const item = b[ipMetrics[i]];
            let ipt, wpt;
            let hmsRA, hmsDec;
            let statsRow = [];

            // item title
            statsRow.push(item.desc);

            // item location
            ipt = parseImagePt(item.ip);
            wpt = cc.getWorldCoords(ipt);
            hmsRA = CoordUtil.convertLonToString(wpt.getLon(), wpt.getCoordSys());
            hmsDec = CoordUtil.convertLatToString(wpt.getLat(), wpt.getCoordSys());
            statsRow.push(`RA: ${hmsRA}\nDEC: ${hmsDec}`);

            // item value
            if (item.value === null || item.value === undefined) {
                statsRow.push('');
            } else {
                statsRow.push(`${formatNumber(item.value)} ${item.units}`);
            }

            tblData[STable].push({'cells': statsRow, 'worldPt': wpt});
        }
        return tblData;
    }

    if (bands.hasOwnProperty(noBand)) {
        bandData[noBand] = getOneStatSet(bands[noBand]);
    } else {
        const bandName = ['Blue', 'Red', 'Green'];

        bandName.forEach((value) => {
            if (bands[value]) {
                bandData[value] = getOneStatSet(bands[value]);
            }
        });
    }

    return bandData;
}

/**
 *
 * @param pv
 */
function stats(pv) {
    //console.log('Stats getting: ' + primePlot(pv).title);
    //console.log(Metrics);
    var p= primePlot(pv);
    var cc= CysConverter.make(p);
    var sel= p.attributes[PlotAttribute.SELECTION];

    var ip0=  cc.getImageCoords(sel.pt0);
    var ip2=  cc.getImageCoords(sel.pt1);
    var ip1=  makeImagePt(ip2.x,ip0.y);
    var ip3=  makeImagePt(ip1.x,ip2.y);


    callGetAreaStatistics(p.plotState, ip0,ip1,ip2,ip3)
        .then( (wpResult) => {      // result of area stats

            // tabaularize stats data
            var tblData = tabulateStatics(wpResult, cc);

            //console.log(wpResult);
            showImageAreaStatsPopup(p.title, tblData, pv.plotId);
        })
        .catch ( (e) => {
            logError(`error, stat , plotId: ${p.plotId}`, e);
        });

}


function crop(pv) {
    var p= primePlot(pv);
    var cc= CysConverter.make(p);
    var sel= p.attributes[PlotAttribute.SELECTION];

    var ip0=  cc.getImageCoords(sel.pt0);
    var ip1=  cc.getImageCoords(sel.pt1);


    var cropMultiAll= p.plotState.isMultiImageFile() && isMultiImageFitsWithSameArea(pv);

    dispatchDetachLayerFromPlot(SelectArea.TYPE_ID,pv.plotId,true);
    dispatchCrop(pv.plotId, ip0,ip1,cropMultiAll);
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
 * @param showClearFilter
 * @param extensionAry
 * @return {XML}
 * @constructor
 */
export function VisCtxToolbarView({plotView:pv, dlAry, extensionAry, showCrop=false, 
                                   showStats=false, showSelect=false, showUnSelect=false, 
                                   showFilter=false, showClearFilter=false }) {

    var rS= {
        width: '100% - 2px',
        height: 34,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top',
        whiteSpace: 'nowrap'

    };
    var showSeparator= (showCrop|| showStats|| showSelect|| showUnSelect|| showFilter || showClearFilter)
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
                           onClick={() => crop(pv)}/>

            <ToolbarButton icon={STATISTICS}
                           tip='Show statistics for the selected area'
                           horizontal={true}
                           visible={showStats}
                           onClick={() => stats(pv)}/>

            <ToolbarButton icon={SELECTED}
                           tip='Mark data in area as selected'
                           horizontal={true}
                           visible={showSelect}
                           onClick={() => selectCatalog(pv,dlAry)}/>

            <ToolbarButton icon={UNSELECTED}
                           tip='Mark all data unselected'
                           horizontal={true}
                           visible={showUnSelect}
                           onClick={() => unselectCatalog(pv,dlAry)}/>

            <ToolbarButton icon={FILTER}
                           tip='Filter in the selected area'
                           horizontal={true}
                           visible={showFilter}
                           onClick={() => filterCatalog(pv,dlAry)}/>

            <ToolbarButton icon={CLEAR_FILTER}
                           tip='Clear all the Filters'
                           horizontal={true}
                           visible={showClearFilter}
                           onClick={() => clearFilterCatalog(pv,dlAry)}/>


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
    showFilter : PropTypes.bool,
    showClearFilter : PropTypes.bool
};



ToolbarButton.defaultProps= {
    showCrop : false,
    showStats : false,
    showSelect : false,
    showUnSelect : false,
    showFilter : false,
    extensionAry : null
};




