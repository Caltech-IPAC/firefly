/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {SINGLE, GRID} from '../MultiViewCntlr.js';

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


export function MultiItemViewerView(props) {

    const {layoutType, activeItemId,
        viewerItemIds, forceRowSize, forceColSize, gridDefFunc,
        additionalStyle, insideFlex=false, defaultDecoration=true,
        makeToolbar, makeItemViewer, makeItemViewerFull}= props;
    var wrapperStyle;
    if (insideFlex) {
        wrapperStyle= Object.assign({}, flexContainerStyle, {flex:'1 1 auto'});
    }
    else {
        wrapperStyle= Object.assign({}, flexContainerStyle, {width:'100%', height:'100%'});
    }
    var container;
    if (viewerItemIds.length===0) {
        container= false;
    }
    else if (layoutType==='single' || viewerItemIds.length===1) {  // SINGLE VIEW
        var id= viewerItemIds.includes(activeItemId) ? activeItemId : viewerItemIds[0];
        container= renderItemViewerFull(makeItemViewerFull,id);
    }
    else if (gridDefFunc) {  // GRID computed by a function
        container= makeSparseGrid(viewerItemIds, gridDefFunc(viewerItemIds), makeItemViewer);
    }
    else if (forceRowSize) {  // GRID with row size
        var col = viewerItemIds.length / forceRowSize + (viewerItemIds.length % forceRowSize);
        container= makePackedGrid(viewerItemIds,forceRowSize,col,false,makeItemViewer);
    }
    else if (forceColSize) { // GRID with column size
        var rows = viewerItemIds.length / forceColSize + (viewerItemIds.length % forceColSize);
        container= makePackedGrid(viewerItemIds,rows,forceColSize,true,makeItemViewer);
    }
    else {                   // GRID automatic
        const dim= findAutoGridDim(viewerItemIds.length);
        container= makePackedGrid(viewerItemIds,dim.rows,dim.cols,true,makeItemViewer);
    }

    var s= Object.assign({}, additionalStyle, wrapperStyle, defaultDecoration? defDecStyle: {});

    return (
        <div style={s}>
            {makeToolbar &&
                <div style={flexToolbarStyle}>
                    {makeToolbar()}
                </div>}
            <div style={{position:'relative', width:'100%', height:'100%', flex:'1 1 auto'}}>
                {container}
            </div>
        </div>
    );
}

//{Toolbar ? <div style={flexContainerStyle}><Toolbar/> </div> : ''}

MultiItemViewerView.propTypes= {
    viewerId : PropTypes.string.isRequired,
    additionalStyle : PropTypes.object,
    defaultDecoration : PropTypes.bool,
    layoutType : PropTypes.oneOf([GRID,SINGLE]),
    forceRowSize : PropTypes.number,   //optional - force a certain number of rows
    forceColSize : PropTypes.number,  //optional - force a certain number of columns
    gridDefFunc : PropTypes.func,  // optional - a function to return the grid definition
    gridComponent : PropTypes.object,  // a react element to define the grid - not implemented, just an idea
    insideFlex :  PropTypes.bool,

    viewerItemIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    activeItemId : PropTypes.string,
    makeToolbar : PropTypes.func,
    makeItemViewer : PropTypes.func,
    makeItemViewerFull : PropTypes.func
};



function makePackedGrid(viewerItemIds,rows,cols, columnBased,makeItemViewer) {
    const percentWidth= 100/cols;
    const percentHeight= 100/rows;

    const width= `calc(${percentWidth}% - 2px)`;
    const height= `calc(${percentHeight}% - 2px)`;

    return columnBased ?
        columnBasedIvAry(viewerItemIds,cols,percentWidth,percentHeight,width,height,makeItemViewer)  :
        rowBasedIvAry(viewerItemIds,rows,percentWidth,percentHeight,width,height,makeItemViewer);
}


function rowBasedIvAry(viewerItemIds,rows,percentWidth,percentHeight,width,height,makeItemViewer) {
    var col = 0;
    var row = 0;
    return viewerItemIds.map( (plotId) => {
        var left= `calc(${col*percentWidth}% + 1px)`;
        var top= `calc(${row*percentHeight}% + 1px)`;
        row = (row < rows - 1) ? row + 1 : 0;
        if (row===0) col++;
        return renderItemViewer(makeItemViewer,plotId,top,left,width,height);
    });

}


function columnBasedIvAry(viewerItemIds,cols,percentWidth,percentHeight,width,height,makeItemViewer) {
    var col = 0;
    var row = 0;
    return viewerItemIds.map( (plotId) => {
        var left= `calc(${col*percentWidth}% + 1px)`;
        var top= `calc(${row*percentHeight}% + 1px)`;
        col = (col < cols - 1) ? col + 1 : 0;
        if (col===0) row++;
        return renderItemViewer(makeItemViewer,plotId,top,left,width,height);
    });
}

const renderItemViewer = (makeItemViewer,itemId,top,left,width,height) => (
        <div style={{position:'absolute', top,left,width,height}} key={itemId}>
            {makeItemViewer(itemId)}
        </div>
    );


const renderItemViewerFull = (makeItemViewerFull, itemId) => (
        <div style={{position:'absolute', top:0,left:0,bottom:0,right:0}} key={itemId}>
            {makeItemViewerFull(itemId)}
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
 * @param {Array} viewerItemIds
 * @param {Array.<{string,string[]}>} gridDef
 * @param {Function} makeItemViewer
 */
function makeSparseGrid(viewerItemIds,gridDef,makeItemViewer) {
    //todo
}

