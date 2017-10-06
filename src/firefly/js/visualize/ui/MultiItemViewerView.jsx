/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {SINGLE, GRID} from '../MultiViewCntlr.js';
import {get} from  'lodash';
const flexContainerStyle= {
    display:'flex',
    flexDirection:'column',
    flexWrap:'nowrap',
    alignItems: 'stretch',
    flexGrow: 1
};

const defDecStyle= {
    background: 'rgb(200,200,200)',
    border: '1px solid rgb(200,200,200)',
    boxSizing: 'border-box'
};

const flexToolbarStyle= {
    flex: '0 0 auto'
};


export function MultiItemViewerView(props) {

    const {layoutType, activeItemId,
        viewerItemIds, forceRowSize, forceColSize, gridDefFunc,
        style, insideFlex=false, defaultDecoration=true, sparseGridTitleLocation= 'top',
        makeToolbar, makeItemViewer, makeItemViewerFull}= props;
    let wrapperStyle;
    if (insideFlex) {
        wrapperStyle= Object.assign({}, flexContainerStyle, {flex:'1 1 auto'});
    }
    else {
        wrapperStyle= Object.assign({}, flexContainerStyle, {width:'100%', height:'100%'});
    }
    let container;
    if (viewerItemIds.length===0) {
        container= false;
    }
    else if (layoutType==='single' || viewerItemIds.length===1) {  // SINGLE VIEW
        const id= viewerItemIds.includes(activeItemId) ? activeItemId : viewerItemIds[0];
        container= renderItemViewerFull(makeItemViewerFull,id);
    }
    else if (gridDefFunc) {  // GRID computed by a function
        container= makeSparseGrid(viewerItemIds, gridDefFunc(viewerItemIds), makeItemViewer, sparseGridTitleLocation);
    }
    else if (forceRowSize) {  // GRID with row size
        const col = viewerItemIds.length / forceRowSize + (viewerItemIds.length % forceRowSize);
        container= makePackedGrid(viewerItemIds,forceRowSize,col,false,makeItemViewer);
    }
    else if (forceColSize) { // GRID with column size
        const rows = viewerItemIds.length / forceColSize + (viewerItemIds.length % forceColSize);
        container= makePackedGrid(viewerItemIds,rows,forceColSize,true,makeItemViewer);
    }
    else {                   // GRID automatic
        const dim= findAutoGridDim(viewerItemIds.length);
        container= makePackedGrid(viewerItemIds,dim.rows,dim.cols,true,makeItemViewer);
    }

    const s= Object.assign({}, style, wrapperStyle, defaultDecoration? defDecStyle: {});

    return (
        <div style={s}>
            {makeToolbar &&
                <div key='toolbar' style={flexToolbarStyle}>
                    {makeToolbar()}
                </div>}
            <div key='container' style={{position:'relative', width:'100%', height:'100%', flex:'1 1 auto',
                         overflow: gridDefFunc ? 'auto' : 'hidden' }}>
                {container}
            </div>
        </div>
    );
}

//{Toolbar ? <div style={flexContainerStyle}><Toolbar/> </div> : ''}

MultiItemViewerView.propTypes= {
    viewerId : PropTypes.string.isRequired,
    style : PropTypes.object,
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
    makeItemViewerFull : PropTypes.func,
    eventCallback: PropTypes.object,
    sparseGridTitleLocation : PropTypes.oneOf(['top', 'left', 'off', ''])
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
    let col = 0;
    let row = 0;
    return viewerItemIds.map( (plotId) => {
        const left= `calc(${col*percentWidth}% + 1px)`;
        const top= `calc(${row*percentHeight}% + 1px)`;
        row = (row < rows - 1) ? row + 1 : 0;
        if (row===0) col++;
        return renderItemViewer(makeItemViewer,plotId,top,left,width,height);
    });

}


function columnBasedIvAry(viewerItemIds,cols,percentWidth,percentHeight,width,height,makeItemViewer) {
    let col = 0;
    let row = 0;
    return viewerItemIds.map( (plotId) => {
        const left= `calc(${col*percentWidth}% + 1px)`;
        const top= `calc(${row*percentHeight}% + 1px)`;
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
    let rows=0 ,cols=0;
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
 * @param sparseGridTitleLocation
 */
function makeSparseGrid(viewerItemIds,gridDef,makeItemViewer, sparseGridTitleLocation) {
    const itemPosAry= viewerItemIds.map( (plotId) => {
        return gridDef.reduce( (obj, gridRow, idx) => {
            const colIdx= gridRow.plotIdAry.findIndex( (id) => id===plotId);
            if (colIdx>-1) {
                obj.row= idx;
                obj.col= colIdx;
                obj.size= gridDef[idx].size;
                obj.plotId= plotId;
            }
            return obj;
        },{row:-1,col:-1});
    });

    const itemPosTitle= gridDef.map( (gd,idx) => ({title:gd.title, noDataMessage: gd.noDataMessage,
                                                   row:idx, col:0, hasData:gd.plotIdAry.length>0,
                                                   dataSize: gd.size}));


    const itemRender= itemPosAry.map ( (pos,idx) => {
        const left= (sparseGridTitleLocation==='left') ?
                                `${pos.col*pos.size+ 30}px` : `${pos.col*pos.size+1}px`;

        const top= computeDataYOffset(sparseGridTitleLocation,itemPosTitle,pos.row);
        return renderItemViewer(makeItemViewer,pos.plotId || 'plot-'+idx,top,left,pos.size,pos.size);
    });
    let itemTitleRender= [];

    if (sparseGridTitleLocation==='top' || sparseGridTitleLocation==='left') {
        itemTitleRender= itemPosTitle.map ( (pos,idx) => {
            const top= computeTitleYOffset(sparseGridTitleLocation, itemPosTitle,pos.row);
            if (sparseGridTitleLocation==='top' || !pos.hasData) {
                const left= '5px';
                return (
                    <div key={`title-${idx}`}
                         style={{
                             position:'absolute',
                             left,top,
                         }}><div style={{
                        fontSize:  pos.hasData ? '12pt' :  '10pt',
                        fontWeight: 'bold',
                        lineHeight: '27px'
                    }}>{pos.hasData ? pos.title : pos.noDataMessage}</div>
                    </div>
                );
            }
            else {
                const left= '0';
                return (
                    <div key={`title-${idx}`}
                         style={{
                             position:'absolute',
                             left,top,
                             fontSize:  pos.hasData ? '12pt' :  '10pt',
                             fontWeight: 'bold',
                             lineHeight: '27px',
                             width: pos.dataSize,
                             marginLeft : pos.dataSize* -0.45,
                             textAlign : 'center',
                             transform : 'rotate(-90deg)',
                             whiteSpace : 'nowrap',
                             overflow : 'hidden',
                             textOverflow : 'ellipsis'
                         }}> {pos.hasData ? pos.title : pos.noDataMessage} </div>
                );
            }
        });
    }

    return [...itemTitleRender, ...itemRender];
}

const titleHeight= 30;

function computeTitleYOffset(sparseGridTitleLocation, itemPosTitle,rowIdx) {
    let titleOffset= sparseGridTitleLocation==='left' && itemPosTitle[0].hasData ? itemPosTitle[0].dataSize/2 : 0;
    for(let i= 0; (i<rowIdx); i++) {
        if (sparseGridTitleLocation==='top') {
            titleOffset+= itemPosTitle[i].hasData ? itemPosTitle[i].dataSize+titleHeight : titleHeight;
        }
        else if (sparseGridTitleLocation==='left') {
            titleOffset+= itemPosTitle[i].hasData ? itemPosTitle[i].dataSize  : titleHeight;
        }
    }
    if (!itemPosTitle[rowIdx].hasData && sparseGridTitleLocation==='left') {
        titleOffset-= itemPosTitle[rowIdx].dataSize/2;
    }
    return titleOffset;
}
function computeDataYOffset(sparseGridTitleLocation, itemPosTitle,rowIdx) {
    let titleOffset= sparseGridTitleLocation==='left' && get(itemPosTitle, '0.hasData') ? 0 : 30;
    for(let i= 0; (i<rowIdx); i++) {
        if (sparseGridTitleLocation==='top') {
            titleOffset+= itemPosTitle[i].hasData ? itemPosTitle[i].dataSize+titleHeight : titleHeight;
        }
        else if (sparseGridTitleLocation==='left') {
            titleOffset+= itemPosTitle[i].hasData ? itemPosTitle[i].dataSize : titleHeight;
        }
    }
    return titleOffset;
}


