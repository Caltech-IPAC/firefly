/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {debounce} from 'lodash';
import React, {forwardRef, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {SINGLE, GRID} from '../MultiViewCntlr.js';
import {Divider, Stack} from '@mui/joy';

const flexContainerStyle= {
    display:'flex',
    flexDirection:'column',
    flexWrap:'nowrap',
    alignItems: 'stretch',
    flexGrow: 1
};

const defDecStyle= {
    boxSizing: 'border-box'
};


export const MultiItemViewerView=forwardRef( (props, ref) =>  {

    const {current:gridContainerElement}= useRef({element:undefined});
    const [,setWindowWidth]= useState(window?.innerWidth??1000);
    const {layoutType, activeItemId,
        viewerItemIds, forceRowSize, forceColSize, makeCustomLayout, gridDefFunc,
        style, insideFlex=false, defaultDecoration=true, sparseGridTitleLocation= 'top',
        scrollGrid=false,
        makeToolbar, makeItemViewer, makeItemViewerFull, autoRowOriented=true}= props;
    let wrapperStyle;

    useEffect(() => {
        if (!scrollGrid) return;
        const doResize= () => setWindowWidth(window.innerWidth);
        const browserResizeCallback= debounce(doResize,20);
        window.addEventListener('resize', browserResizeCallback);
        return () => {
            window.removeEventListener('resize', browserResizeCallback);
        };
    },[]);

    if (insideFlex) {
        wrapperStyle= Object.assign({}, flexContainerStyle, {flex:'1 1 auto'});
    }
    else {
        wrapperStyle= Object.assign({}, flexContainerStyle, {width:'100%', height:'100%'});
    }
    let container;
    if (!viewerItemIds.length && !gridDefFunc) {
        container= false;
    }
    else if (makeCustomLayout) {  // CUSTOM layout defined by a function
        container = makeCustomLayout(viewerItemIds, makeItemViewer);
    }
    else if (layoutType==='single' || viewerItemIds.length===1) {  // SINGLE VIEW
        const id= viewerItemIds.includes(activeItemId) ? activeItemId : viewerItemIds[0];
        container= renderItemViewerFull(makeItemViewerFull,id);
    }
    else if (gridDefFunc) {  // GRID computed by a function
        const gridDef= gridDefFunc(viewerItemIds);
        container=  gridDef.length ?
            makeSparseGrid(viewerItemIds, gridDef, makeItemViewer, sparseGridTitleLocation) : false;
    }
    else if (forceRowSize) {  // GRID with row size
        const col = viewerItemIds.length / forceRowSize + (viewerItemIds.length % forceRowSize);
        container= makePackedGrid(viewerItemIds,forceRowSize,col,false,makeItemViewer);
    }
    else if (forceColSize) { // GRID with column size
        const rows = viewerItemIds.length / forceColSize + (viewerItemIds.length % forceColSize);
        container= makePackedGrid(viewerItemIds,rows,forceColSize,true,makeItemViewer);
    }
    else if (scrollGrid) {
        let cols;
        const {width:containerWidth}= gridContainerElement?.element ?
            gridContainerElement.element.getBoundingClientRect() : {width:0,height:0};
        if (viewerItemIds.length>16) cols=4;
        else if (viewerItemIds.length>5) cols=3;
        else cols=2;
        container= makeScrollGrid(viewerItemIds,cols,containerWidth, makeItemViewer);
    }
    else {                   // GRID automatic
        const dim= findAutoGridDim(viewerItemIds.length, autoRowOriented);
        container= makePackedGrid(viewerItemIds,dim.rows,dim.cols,true,makeItemViewer);
    }

    const s= Object.assign({}, wrapperStyle, defaultDecoration? defDecStyle: {}, style);

    return (
        <div style={s} ref={ref}>
            {makeToolbar &&
                <Stack key='toolbar'>
                    {makeToolbar()}
                    <Divider orientation={'horizontal'}/>
                </Stack>}

            <div key='container' ref={(e) => gridContainerElement.element= e}
                 style={{position:'relative', width:'100%', height:'100%', flex:'1 1 auto',
                         overflow: (gridDefFunc||scrollGrid) ? 'auto' : 'hidden' }}>
                {container}
            </div>
        </div>
    );
});

MultiItemViewerView.propTypes= {
    viewerId : PropTypes.string.isRequired,
    style : PropTypes.object,
    defaultDecoration : PropTypes.bool,
    layoutType : PropTypes.oneOf([GRID,SINGLE]),
    forceRowSize : PropTypes.number,   //optional - force a certain number of rows
    forceColSize : PropTypes.number,  //optional - force a certain number of columns
    makeCustomLayout : PropTypes.func,  //optional - a function to present the items in a custom layout
    gridDefFunc : PropTypes.func,  // optional - a function to return the grid definition
    gridComponent : PropTypes.object,  // an element to define the grid - not implemented, just an idea
    scrollGrid: PropTypes.bool,
    insideFlex :  PropTypes.bool,
    autoRowOriented: PropTypes.bool,

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

function makeScrollGrid(viewerItemIds,cols,containerWidth, makeItemViewer) {
    const size= 100/cols;
    const sizePx= containerWidth ? Math.trunc((size/100)*containerWidth-2) : 0;
    const width= `calc(${size}% - 2px)`;
    const height= containerWidth ? `${sizePx-2}px` : `calc(${size}% - 2px)`;
    return columnBasedIvAry(viewerItemIds,cols,size,size,width,height,makeItemViewer,sizePx);
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


function columnBasedIvAry(viewerItemIds,cols,percentWidth,percentHeight,width,height,makeItemViewer,sizePx=0) {
    let col = 0;
    let row = 0;
    return viewerItemIds.map( (plotId) => {
        const left= `calc(${col*percentWidth}% + 1px)`;
        const top= sizePx ? `${row*sizePx+1}px`  : `calc(${row*percentHeight}% + 1px)`;
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

function findAutoGridDim(size, rowOriented=true) {
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
            rows = rowOriented ? 2 : 3;
            cols = rowOriented ? 3 : 2;
        } else if (size === 4) {
            rows = 2;
            cols = 2;
        } else if (size === 3) {
            rows = rowOriented ? 1 : 3;
            cols = rowOriented ? 3 : 1;
        } else if (size === 2) {
            rows = rowOriented ? 1 : 2;
            cols = rowOriented ? 2 : 1;
        }
    }
    return {rows,cols};
}


/**
 *  gridDef parameter is an array of objects that contain an optional title and an array of plotIds
 *  each element of the array should represent a row and each plotId a plot in that row,
 *  an empty element will act as a placeholder in the row.
 *
 * @param {Array} viewerItemIds
 * @param {Array.<{string,string[]}>} gridDef
 * @param {Function} makeItemViewer
 * @param sparseGridTitleLocation
 */
function makeSparseGrid(viewerItemIds,gridDef,makeItemViewer, sparseGridTitleLocation) {


    const sortedGridDef= gridDef.sort( (g1,g2) => {
        if (!g1.plotIdAry.length && !g2.plotIdAry.length) return 0;
        else if (g1.plotIdAry.length && g2.plotIdAry.length) return 0;
        else if (g1.plotIdAry.length && !g2.plotIdAry.length) return -1;
        else if (!g1.plotIdAry.length && g2.plotIdAry.length) return 1;
    } );


    const itemPosAry= viewerItemIds.map( (plotId) => {
        return sortedGridDef.reduce( (obj, gridRow, idx) => {
            const colIdx= gridRow.plotIdAry.findIndex( (id) => id===plotId);
            if (colIdx>-1) {
                obj.row= idx;
                obj.col= colIdx;
                obj.size= sortedGridDef[idx].size;
                obj.plotId= plotId;
            }
            return obj;
        },{row:-1,col:-1});
    });

    const itemPosTitle= sortedGridDef.map( (gd,idx) => ({title:gd.title, noDataMessage: gd.noDataMessage,
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
    let titleOffset= sparseGridTitleLocation==='left' && itemPosTitle[rowIdx].hasData ? itemPosTitle[0].dataSize/2 : 0;
    for(let i= 0; (i<rowIdx); i++) {
        if (sparseGridTitleLocation==='top' || !itemPosTitle[i].hasData) {
            titleOffset+= itemPosTitle[i].hasData ? itemPosTitle[i].dataSize+titleHeight : titleHeight;
        }
        else if (sparseGridTitleLocation==='left') {
            titleOffset+= itemPosTitle[i].dataSize;
        }
    }
    return titleOffset;
}
function computeDataYOffset(sparseGridTitleLocation, itemPosTitle,rowIdx) {
    let titleOffset= sparseGridTitleLocation==='left' && itemPosTitle?.[0]?.hasData ? 0 : 30;
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


