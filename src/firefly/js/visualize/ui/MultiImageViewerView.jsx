/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {SINGLE, GRID} from '../MultiViewCntlr.js';
import {ImageViewer} from './../iv/ImageViewer.jsx';




const flexContainerStyle= {
    display:'flex',
    flexDirection:'column',
    flexWrap:'nowrap',
    alignItems: 'stretch'
};

const defDecStyle= {
    background: 'rgb(200,200,200)',
    border: '1px solid rgb(200,200,200)',
    // borderRadius: '5px',
    boxSizing: 'border-box' 
};

const flexToolbarStyle= {
    flex: '0 0 auto'
};


export function MultiImageViewerView(props) {

    const {Toolbar, layoutType, visRoot,
           viewerPlotIds, forceRowSize, forceColSize, gridDefFunc,
           showWhenExpanded=false, additionalStyle,
           defaultDecoration=true, insideFlex=false}= props;
    var wrapperStyle; 
    if (insideFlex) {
        wrapperStyle= Object.assign({}, flexContainerStyle, {flex:'1 1 auto'});
    }
    else {
        wrapperStyle= Object.assign({}, flexContainerStyle, {width:'100%', height:'100%'});
    }
    var container;
    if (viewerPlotIds.length===0) {
        container= false;
    }
    else if (layoutType==='single' || viewerPlotIds.length===1) {  // SINGLE VIEW
        var id= viewerPlotIds.includes(visRoot.activePlotId) ? visRoot.activePlotId : viewerPlotIds[0];
        container= makeImageViewerFull(id,showWhenExpanded,viewerPlotIds.length===1);
    }
    else if (gridDefFunc) {  // GRID computed by a function
        container= makeSparseGrid(viewerPlotIds, gridDefFunc(viewerPlotIds),showWhenExpanded);
    }
    else if (forceRowSize) {  // GRID with row size
        var col = viewerPlotIds.length / forceRowSize + (viewerPlotIds.length % forceRowSize);
        container= makePackedGrid(viewerPlotIds,forceRowSize,col,false,showWhenExpanded);
    }
    else if (forceColSize) { // GRID with column size
        var rows = viewerPlotIds.length / forceColSize + (viewerPlotIds.length % forceColSize);
        container= makePackedGrid(viewerPlotIds,rows,forceColSize,true,showWhenExpanded);
    }
    else {                   // GRID automatic
        const dim= findAutoGridDim(viewerPlotIds.length);
        container= makePackedGrid(viewerPlotIds,dim.rows,dim.cols,true,showWhenExpanded);
    }
    
    var s= Object.assign({}, additionalStyle, wrapperStyle, defaultDecoration? defDecStyle: {});

    return (
        <div style={s}>
            {Toolbar &&
                <div style={flexToolbarStyle}>
                    <Toolbar {...props} /> 
                </div>}
            <div style={{position:'relative', width:'100%', height:'100%', flex:'1 1 auto'}}>
                {container}
            </div>
        </div>
    );
}

//{Toolbar ? <div style={flexContainerStyle}><Toolbar/> </div> : ''}

MultiImageViewerView.propTypes= {
    Toolbar : PropTypes.func,
    viewerId : PropTypes.string.isRequired,
    additionalStyle : PropTypes.object,    
    defaultDecoration : PropTypes.bool,
    visRoot : PropTypes.object,
    layoutType : PropTypes.oneOf([GRID,SINGLE]),
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    forceRowSize : PropTypes.number,   //optional - force a certain number of rows
    forceColSize : PropTypes.number,  //optional - force a certain number of columns
    gridDefFunc : PropTypes.func,  // optional - a function to return the grid definition
    gridComponent : PropTypes.object,  // a react element to define the grid - not implemented, just an idea
    showWhenExpanded : PropTypes.bool,
    insideFlex :  PropTypes.bool
};



function makePackedGrid(viewerPlotIds,rows,cols, columnBased,showWhenExpanded) {
    const percentWidth= 100/cols;
    const percentHeight= 100/rows;

    const width= `calc(${percentWidth}% - 2px)`;
    const height= `calc(${percentHeight}% - 2px)`;

    return columnBased ?
        columnBasedIvAry(viewerPlotIds,cols,percentWidth,percentHeight,width,height, showWhenExpanded)  :
        rowBasedIvAry(viewerPlotIds,rows,percentWidth,percentHeight,width,height,showWhenExpanded);
}


function rowBasedIvAry(viewerPlotIds,rows,percentWidth,percentHeight,width,height,showWhenExpanded) {
    var col = 0;
    var row = 0;
    return viewerPlotIds.map( (plotId) => {
        var left= `calc(${col*percentWidth}% + 1px)`;
        var top= `calc(${row*percentHeight}% + 1px)`;
        row = (row < rows - 1) ? row + 1 : 0;
        if (row===0) col++;
        return makeImageViewer(plotId,top,left,width,height, showWhenExpanded);
    });

}


function columnBasedIvAry(viewerPlotIds,cols,percentWidth,percentHeight,width,height,showWhenExpanded) {
    var col = 0;
    var row = 0;
    return viewerPlotIds.map( (plotId) => {
        var left= `calc(${col*percentWidth}% + 1px)`;
        var top= `calc(${row*percentHeight}% + 1px)`;
        col = (col < cols - 1) ? col + 1 : 0;
        if (col===0) row++;
        return makeImageViewer(plotId,top,left,width,height, showWhenExpanded);
    });
}


const makeImageViewer = (plotId,top,left,width,height,showWhenExpanded) => (
                      <div style={{position:'absolute', top,left,width,height}} key={plotId}>
                          <ImageViewer plotId={plotId} key={plotId} 
                                       showWhenExpanded={showWhenExpanded}
                                       handleInlineTools={false}/>
                      </div>
                                        );

/**
 * 
 * @param plotId
 * @param showWhenExpanded
 * @param handleInlineTools
 */
const makeImageViewerFull = (plotId, showWhenExpanded, handleInlineTools) => (
    <div style={{position:'absolute', top:0,left:0,bottom:0,right:0}} key={plotId}>
        <ImageViewer plotId={plotId} key={plotId}  showWhenExpanded={showWhenExpanded}
                      handleInlineTools={handleInlineTools} />
    </div>
);

function findAutoGridDim(size) {
    var rows=0 ,cols=0;
    if (size) {
        rows = 1;
        cols = 1;
        if (size >= 7) {
            // rows = size / 4 + (size % 4);
            rows = Math.floor(size / 4);
            if (size/4 > rows) rows++;
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




/**
 *  gridDef parameter is an array of objects that contain an optional title and an array of plotIds
 *  each element of the array should represent a row and each plotId a plot in that row,
 *  an empty element will act as a place holder in the row.
 *
 * @param {Array} viewerPlotIds
 * @param {Array.<{string,string[]}>} gridDef
 */
function makeSparseGrid(viewerPlotIds,gridDef) {
    //todo
}
