/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {ExpandedTools} from './ExpandedTools.jsx';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {primePlot, expandedPlotViewAry} from '../PlotViewUtil.js';
import {ImageViewer} from './../iv/ImageViewer.jsx';

export function ExpandedGridView({visRoot}) {
    if (visRoot.expandedMode===ExpandType.COLLAPSE) return <div></div>;
    return (
        <div className='ExpandedSingleGrid' style={{flex:'auto', position:'relative'}}>
            <ExpandedTools  visRoot={visRoot}/>
            <div style={{position:'absolute', top:60,left:0,right:0,bottom:0}}>
                <ImageViewAutoGrid visRoot={visRoot} />
            </div>
        </div>
    );
}

ExpandedGridView.propTypes= {
    visRoot : PropTypes.object.isRequired
};


//==============================================================
//==============================================================
//==============================================================
//==============================================================
//==============================================================


export function ImageViewAutoGrid({visRoot}) {

    const {plotViewAry,activePlotId}= visRoot;
    const pvAry= expandedPlotViewAry(plotViewAry,activePlotId);
    return (
        <div style={{position:'absolute', top:0,left:0,right:0,bottom:0}}>
            {makeAutoGrid(pvAry)}
        </div>
    );
}



ImageViewAutoGrid.propTypes= {
    visRoot : PropTypes.object.isRequired
};


function makeAutoGrid(pvAry) {

    const dim= findGridDim(pvAry.length);
    const percentWidth= 100/dim.cols;
    const percentHeight= 100/dim.rows;

    const width= `calc(${percentWidth}% - 2px)`;
    const height= `calc(${percentHeight}% - 2px)`;


    var col = 0;
    var row = 0;

    return pvAry.map( (pv) => {
        var left= `calc(${col*percentWidth}% + 1px)`;
        var top= `calc(${row*percentHeight}% + 1px)`;
        col = (col < dim.cols - 1) ? col + 1 : 0;
        if (col == 0) row++;
        return (
            <div style={{position:'absolute', top,left,width,height}} key={pv.plotId}>
                <ImageViewer plotId={pv.plotId}/>
            </div>
        );
    });


}



function findGridDim(size) {
    var rows=0 ,cols=0;
    if (size) {
        rows = 1;
        cols = 1;
        if (size >= 7) {
            rows = size / 4 + (size % 4);
            cols = 4;
        } else if (size === 5 || size === 6) {
            rows = 2;
            cols = 3;
        } else if (size === 4) {
            rows = 2;
            cols = 2;
        } else if (size === 3) {
            rows = 1;
            cols = 3;
        } else if (size === 2) {
            rows = 1;
            cols = 2;
        }
    }
    return {rows,cols};
}
