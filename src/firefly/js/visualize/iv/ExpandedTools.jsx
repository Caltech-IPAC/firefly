/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {ExpandType, dispatchChangeExpandedMode,
         dispatchExpandedAutoPlay} from '../ImagePlotCntlr.js';
import {primePlot, getActivePlotView} from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {showExpandedOptionsPopup} from '../ui/ExpandedOptionsPopup.jsx';
import { dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {VisToolbar} from '../ui/VisToolbar.jsx';
import {getMultiViewRoot, getExpandedViewerItemIds} from '../MultiViewCntlr.js';
import {WcsMatchOptions} from '../ui/WcsMatchOptions.jsx';

import './ExpandedTools.css';

import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import LIST from 'html/images/icons-2014/ListOptions.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import ACTIVE_DOT from 'html/images/green-dot-10x10.png';
import INACTIVE_DOT from 'html/images/blue-dot-10x10.png';

const tStyle= {
    display:'inline-block',
    whiteSpace: 'nowrap',
    minWidth: '3em',
    paddingLeft : 5
};


function createOptions(expandedMode, singleAutoPlay, visRoot, plotIdAry) {
    var autoPlay= false;
    var wcsMatch= false;
    if (expandedMode===ExpandType.SINGLE && plotIdAry.length>1) {
        autoPlay= (
            <div style={{paddingLeft:25}}>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}} type='checkbox' checked={singleAutoPlay}
                           onChange={() => dispatchExpandedAutoPlay(!singleAutoPlay) }
                    />
                </div>
                <div style={tStyle}>Auto Play</div>
            </div>
        );
    }

    if (plotIdAry.length>1) {
        wcsMatch= (
                <WcsMatchOptions activePlotId={visRoot.activePlotId} wcsMatchType={visRoot.wcsMatchType} />
        );
    }

    return (
        <div style={{display:'inline-block', paddingLeft:15}}>
            {wcsMatch}
            {autoPlay}
        </div>
    );
}


const closeButtonStyle= {
    display: 'inline-block',
    padding: '1px 12px 0 1px'
};

const singlePlotTitleStyle= {
    paddingLeft: 2,
    alignSelf : 'center'
};

const gridPlotTitleStyle= {
    paddingLeft: 3,
    lineHeight: '2em',
    fontSize: '10pt',
    fontWeight: 'bold',
    alignSelf : 'center'
};


export function ExpandedTools({visRoot,closeFunc}) {
    const {expandedMode,activePlotId, singleAutoPlay}= visRoot;
    const plotIdAry= getExpandedViewerItemIds(getMultiViewRoot());
    const single= expandedMode===ExpandType.SINGLE || plotIdAry.length===1;
    const pv= getActivePlotView(visRoot);
    const plot= primePlot(pv);

    let plotTitle;
    if (plot) {
        if (single) {
            plotTitle= (
                <div style={singlePlotTitleStyle}>
                    <PlotTitle brief={false} inline={false} titleType={TitleType.EXPANDED} plotView={pv} />
                </div>
            );
        }
        else {
            plotTitle= (<div style={gridPlotTitleStyle}>Tiled View</div>);
        }
    }
    const getPlotTitle = (plotId) => {
        var plot= primePlot(visRoot,plotId);
        return plot ? plot.title : '';
    };

    return (
        <div>
            <div style={{display: 'flex', flexWrap:'wrap',paddingBottom: 2, borderBottom: '1px solid rgba(0,0,0,.2)' }}>
                {closeFunc && <CloseButton style={closeButtonStyle} onClick={closeFunc}/>}
                <div style={{'flex': '1 1 auto'}}>
                    <VisToolbar messageUnder={Boolean(closeFunc)}/>
                </div>
            </div>
            <div style={{width:'100%', minHeight:25, margin: '7px 0 5px 0',
                         display: 'flex', justifyContent:'space-between'}} className='disable-select'>
                {plotTitle}
                <div style={{paddingBottom:5, alignSelf:'flex-end', whiteSpace:'nowrap'}}>
                    <WhichView  visRoot={visRoot}/>
                    {createOptions(expandedMode,singleAutoPlay, visRoot, plotIdAry)}
                    <PagingControl
                        viewerItemIds={getExpandedViewerItemIds(getMultiViewRoot())}
                        activeItemId={activePlotId}
                        isPagingMode={expandedMode===ExpandType.SINGLE}
                        getItemTitle={getPlotTitle}
                        onActiveItemChange={dispatchChangeActivePlotView}
                    />
                </div>
            </div>
        </div>
    );
}

//<div style={{ display: 'inline-block', paddingLeft: 10}}></div>
//<div style={s}>checkboxes: wcs target match, wcs match, auto play (single only)</div>
//{makeInlineTitle(visRoot,pv)}

ExpandedTools.propTypes= {
    visRoot : PropTypes.object.isRequired,
    closeable : PropTypes.bool,
    closeFunc : PropTypes.func
};




function WhichView({visRoot}) {
    var {plotViewAry}= visRoot;
    const showViewButtons= getExpandedViewerItemIds(getMultiViewRoot()).length>1;
    return (
        <div style={{display: 'inline-block', verticalAlign:'top'}}>
            {showViewButtons &&
                   <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                                  imageStyle={{width:24,height:24}}
                                  enabled={true} visible={true}
                                  horizontal={true}
                                  onClick={() => dispatchChangeExpandedMode(ExpandType.SINGLE)}/>}
            {showViewButtons &&
                   <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                                  enabled={true} visible={true} horizontal={true}
                                  imageStyle={{width:24,height:24}}
                                  onClick={() => dispatchChangeExpandedMode(ExpandType.GRID)}/>
            }
            {plotViewAry.length>1 &&
                   <ToolbarButton icon={LIST} tip={'Choose which plots to show'}
                                  imageStyle={{width:24,height:24}}
                                  enabled={true} visible={true} horizontal={true}
                                  onClick={() =>showExpandedOptionsPopup(visRoot.plotViewAry) }/>
            }
        </div>
    );
}

WhichView.propTypes= {
    visRoot : PropTypes.object.isRequired
};


const rightTitleStyle= {
    // display:'inline-block',
    paddingLeft : 5,
    cursor : 'pointer',
    float : 'right'
};


const controlStyle= {
    display: 'inline-block',
    paddingLeft: 10,
    width: 300
};


function pTitle(begin,title) {
    return title ? begin+title : '';
}



export function PagingControl({viewerItemIds,activeItemId,isPagingMode,getItemTitle,onActiveItemChange}) {

    if (!activeItemId || viewerItemIds.length<2 || !isPagingMode) return <div style={controlStyle}/>;

    const cIdx= viewerItemIds.indexOf(activeItemId);
    if (cIdx<0) return <div style={controlStyle}/>;

    const nextIdx= cIdx===viewerItemIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerItemIds.length-1;

    const dots= viewerItemIds.map( (plotId,idx) =>
        idx===cIdx ?
            <img src={ACTIVE_DOT} className='control-dots'
                 title={pTitle('Active Plot: ', getItemTitle(plotId))}
                 key={idx}/>  :
            <img src={INACTIVE_DOT} className='control-dots'
                 title={pTitle('Display: ', getItemTitle(plotId))}
                 key={idx}
                  onClick={() => onActiveItemChange(plotId)}/>);

    const leftTitleStyle= {
        // display:'inline-block',
        cursor : 'pointer',
        textAlign : 'left'
    };
    const leftImageStyle= {
        verticalAlign:'bottom',
        cursor:'pointer'
    };
    if (viewerItemIds.length===2) {
        leftTitleStyle.visibility='hidden';
        leftImageStyle.visibility='hidden';
    }


    return (
        <div style={controlStyle} >
            <div style= {{display: 'flex', flexDirection: 'row', alignItems:'center'}}>
                <img style={leftImageStyle} src={PAGE_LEFT}
                     title={pTitle('Previous: ',getItemTitle(viewerItemIds[prevIdx]))}
                     onClick={() => onActiveItemChange(viewerItemIds[prevIdx])}
                />
                <a style={leftTitleStyle} className='ff-href text-nav-controls'
                   title={pTitle('Previous: ',getItemTitle(viewerItemIds[prevIdx]))}
                     onClick={() => onActiveItemChange(viewerItemIds[prevIdx])} >
                    {getItemTitle(viewerItemIds[prevIdx])}
                </a>
                <div style={{flex: '1 1 auto'}}/>

                <a style={rightTitleStyle} className='ff-href text-nav-controls'
                   title={pTitle('Next: ', getItemTitle(viewerItemIds[nextIdx]))}
                   onClick={() => onActiveItemChange(viewerItemIds[nextIdx])} >
                    {getItemTitle(viewerItemIds[nextIdx])}
                </a>
                <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right'}}
                     title={pTitle('Next: ', getItemTitle(viewerItemIds[nextIdx]))}
                     src={PAGE_RIGHT}
                     onClick={() => onActiveItemChange(viewerItemIds[nextIdx])}
                />
            </div>
            <div style={{textAlign:'center'}}>
                {dots}
            </div>
        </div>

    );


}

PagingControl.propTypes= {
    viewerItemIds: PropTypes.arrayOf(PropTypes.string).isRequired,
    activeItemId: PropTypes.string,
    isPagingMode: PropTypes.bool.isRequired,
    getItemTitle : PropTypes.func.isRequired,
    onActiveItemChange : PropTypes.func.isRequired
};
