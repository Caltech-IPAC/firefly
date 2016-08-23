/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {ExpandType, dispatchChangeExpandedMode, dispatchExpandedAutoPlay} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {showExpandedOptionsPopup} from '../ui/ExpandedOptionsPopup.jsx';
import { dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {VisToolbar} from '../ui/VisToolbar.jsx';
import {getMultiViewRoot, getExpandedViewerPlotIds} from '../MultiViewCntlr.js';

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


function createOptions(expandedMode,singleAutoPlay, plotIdAry) {
    var autoPlay= false;
    var wcsSTMatch= false;
    var wcsMatch= false;
    if (expandedMode===ExpandType.SINGLE && plotIdAry.length>1) {
        autoPlay= (
            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                          checked={singleAutoPlay}
                           onChange={() => dispatchExpandedAutoPlay(!singleAutoPlay) }
                    />
                </div>
                <div style={tStyle}>Auto Play</div>
            </div>
        );
    }

    if (plotIdAry.length>1) {
        wcsSTMatch= (
            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={false}
                           onChange={() => console.log('WCS Search Target Match') }
                    />
                </div>
                <div style={tStyle}>TODO: WCS Search Target Match</div>
            </div>
        );
        wcsMatch= (
            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={false}
                           onChange={() => console.log('WCS Match') }
                    />
                </div>
                <div style={tStyle}>TODO: WCS Match</div>
            </div>
        );
    }

    return (
        <div style={{display:'inline-block', paddingLeft:15}}>
            {wcsSTMatch}
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
    var {expandedMode,activePlotId, singleAutoPlay}= visRoot;
    const plotIdAry= getExpandedViewerPlotIds(getMultiViewRoot());
    var single= expandedMode===ExpandType.SINGLE || plotIdAry.length===1;
    var plot= primePlot(visRoot);

    var plotTitle;
    if (plot) {
        var {title, zoomFactor, plotState}=plot;
        if (single) {
            plotTitle= (
                <div style={singlePlotTitleStyle}>
                    <PlotTitle brief={false} titleStr={title} inline={false}
                               titleType={TitleType.EXPANDED}
                               zoomFactor={zoomFactor}
                               plotState={plotState} plotId={plot.plotId}
                    />
                </div>
            );
        }
        else {
            plotTitle= (<div style={gridPlotTitleStyle}>Tiled View</div>);
        }
    }
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
                <div style={{paddingBottom:5, alignSelf:'flex-end'}}>
                    <WhichView  visRoot={visRoot}/>
                    {createOptions(expandedMode,singleAutoPlay, plotIdAry)}
                    <PagingControl activePlotId={activePlotId}
                                   visRoot={visRoot}
                                   expandedMode={expandedMode} />
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
    closeable : PropTypes.bool
};




function WhichView({visRoot}) {
    var {plotViewAry}= visRoot;
    const showViewButtons= getExpandedViewerPlotIds(getMultiViewRoot()).length>1;
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
    display:'inline-block',
    paddingLeft : 5,
    cursor : 'pointer',
    float : 'right'
};


const controlStyle= {
    display: 'inline-block',
    paddingLeft: 10,
    width: 300
};


function pTitle(begin,visRoot,plotId) {
    var plot= primePlot(visRoot,plotId);
    return plot ? begin+plot.title : '';
}



function PagingControl({visRoot,activePlotId, expandedMode}) {

    const plotIdAry= getExpandedViewerPlotIds(getMultiViewRoot());

    if (!activePlotId || plotIdAry.length<2 || expandedMode!==ExpandType.SINGLE) return <div style={controlStyle}></div>;

    const cIdx= plotIdAry.indexOf(activePlotId);
    const nextIdx= cIdx===plotIdAry.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : plotIdAry.length-1;



    const dots= plotIdAry.map( (plotId,idx) =>
        idx===cIdx ?
            <img src={ACTIVE_DOT} className='control-dots'
                 title={pTitle('Active Plot: ', visRoot,plotId)}
                 key={idx}/>  :
            <img src={INACTIVE_DOT} className='control-dots'
                 title={pTitle('Display: ', visRoot,plotId)}
                 key={idx}
                  onClick={() => dispatchChangeActivePlotView(plotId)}/>);

    const leftTitleStyle= {
        display:'inline-block',
        cursor : 'pointer',
        textAlign : 'left'
    };
    const leftImageStyle= {
        verticalAlign:'bottom',
        cursor:'pointer'
    };
    if (plotIdAry.length===2) {
        leftTitleStyle.visibility='hidden';
        leftImageStyle.visibility='hidden';
    }


    return (
        <div style={controlStyle} >
            <div>
                <img style={leftImageStyle} src={PAGE_LEFT}
                     title={pTitle('Previous: ',visRoot,plotIdAry[prevIdx])}
                     onClick={() => dispatchChangeActivePlotView(plotIdAry[prevIdx])}
                />
                <a style={leftTitleStyle} className='ff-href text-nav-controls'
                   title={pTitle('Previous: ',visRoot,plotIdAry[prevIdx])}
                     onClick={() => dispatchChangeActivePlotView(plotIdAry[prevIdx])} >
                    {pTitle('',visRoot,plotIdAry[prevIdx])}
                </a>

                <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right'}}
                     title={pTitle('Next: ', visRoot,plotIdAry[nextIdx])}
                     src={PAGE_RIGHT}
                     onClick={() => dispatchChangeActivePlotView(plotIdAry[nextIdx])}
                />
                <a style={rightTitleStyle} className='ff-href text-nav-controls'
                   title={pTitle('Next: ', visRoot,plotIdAry[nextIdx])}
                   onClick={() => dispatchChangeActivePlotView(plotIdAry[nextIdx])} >
                    {pTitle('',visRoot,plotIdAry[nextIdx])}
                </a>
            </div>
            <div style={{textAlign:'center'}}>
                {dots}
            </div>
        </div>

    );


}

PagingControl.propTypes= {
    activePlotId: PropTypes.string,
    expandedMode: PropTypes.object.isRequired,
    visRoot : PropTypes.object.isRequired
};
