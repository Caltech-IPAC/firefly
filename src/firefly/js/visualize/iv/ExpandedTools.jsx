/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {ExpandType, dispatchChangeExpandedMode, dispatchExpandedAutoPlay} from '../ImagePlotCntlr.js';
import {primePlot, expandedPlotViewAry} from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {showExpandedOptionsPopup} from '../ui/ExpandedOptionsPopup.jsx';
import { dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';

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




function createOptions(expandedMode,singleAutoPlay) {
    var autoPlay= false;
    if (expandedMode===ExpandType.SINGLE) {
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

    return (
        <div style={{display:'inline-block', paddingLeft:15}}>
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
            {autoPlay}
        </div>
    );
}



const s= {
    display: 'inline-block',
    paddingLeft: 10
};

const singlePlotTitleStyle= {
    display: 'inline-block',
    paddingLeft: 10,
    position: 'relative',
    top: -3
};

const gridPlotTitleStyle= {
    display: 'inline-block',
    paddingLeft: 10,
    position: 'relative',
    top: -3,
    lineHeight: '2em',
    fontSize: '10pt',
    fontWeight: 'bold',
    verticalAlign: 'middle'
};


export function ExpandedTools({visRoot}) {
    var {expandedMode,plotViewAry,activePlotId, singleAutoPlay}= visRoot;
    var single= expandedMode===ExpandType.SINGLE;
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
        <div style={{width:'100%', height:70}} className='disable-select'>
            <CloseButton style={s} onClick={() => console.log('ExpandedTools: back button')}/>
            {plotTitle}
            <div style={s}></div>
            <div style={{display: 'inline-block', float:'right'}}>
                <WhichView  visRoot={visRoot}/>
                {createOptions(expandedMode,singleAutoPlay)}
                <PagingControl plotViewAry={plotViewAry}
                                         activePlotId={activePlotId}
                                         expandedMode={expandedMode} />
            </div>
        </div>
    );
}

//<div style={s}>checkboxes: wcs target match, wcs match, auto play (single only)</div>
//{makeInlineTitle(visRoot,pv)}

ExpandedTools.propTypes= {
    visRoot : PropTypes.object.isRequired
};




function WhichView({visRoot}) {
    return (
        <div style={{display: 'inline-block', verticalAlign:'top'}}>
            <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                           enabled={true} visible={true}
                           horizontal={true}
                           onClick={() => dispatchChangeExpandedMode(ExpandType.SINGLE)}/>
            <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                           enabled={true} visible={true} horizontal={true}
                           onClick={() => dispatchChangeExpandedMode(ExpandType.GRID)}/>
            <ToolbarButton icon={LIST} tip={'Choose which plots to show'}
                           enabled={true} visible={true} horizontal={true}
                           onClick={() =>showExpandedOptionsPopup(visRoot.plotViewAry) }/>
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


function pTitle(begin,pv) {
    var plot= primePlot(pv);
    return plot ? begin+plot.title : '';
}



function PagingControl({plotViewAry, activePlotId, expandedMode}) {



    const pvAry= expandedPlotViewAry(plotViewAry,activePlotId);

    if (pvAry.length<2 || expandedMode!==ExpandType.SINGLE) return <div style={controlStyle}></div>;

    const cIdx= pvAry.findIndex( (pv) => pv.plotId===activePlotId);
    const nextIdx= cIdx===pvAry.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : pvAry.length-1;



    const dots= pvAry.map( (pv,idx) =>
        idx===cIdx ?
            <img src={ACTIVE_DOT} className='control-dots'
                 title={pTitle('Active Plot: ',pvAry[idx])}
                 key={idx}/>  :
            <img src={INACTIVE_DOT} className='control-dots'
                 title={pTitle('Display: ', pvAry[idx])}
                 key={idx}
                  onClick={() => dispatchChangeActivePlotView(pvAry[idx].plotId)}/>);

    const leftTitleStyle= {
        display:'inline-block',
        cursor : 'pointer',
        textAlign : 'left'
    };
    const leftImageStyle= {
        verticalAlign:'bottom',
        cursor:'pointer'
    };
    if (pvAry.length===2) {
        leftTitleStyle.visibility='hidden';
        leftImageStyle.visibility='hidden';
    }


    return (
        <div style={controlStyle} >
            <div>
                <img style={leftImageStyle} src={PAGE_LEFT}
                     title={pTitle('Previous: ',pvAry[prevIdx])}
                     onClick={() => dispatchChangeActivePlotView(pvAry[prevIdx].plotId)}
                />
                <a style={leftTitleStyle} className='ff-href text-nav-controls'
                   title={pTitle('Previous: ',pvAry[prevIdx])}
                     onClick={() => dispatchChangeActivePlotView(pvAry[prevIdx].plotId)} >
                    {pTitle('',pvAry[prevIdx])}
                </a>

                <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right'}}
                     title={pTitle('Next: ', pvAry[nextIdx])}
                     src={PAGE_RIGHT}
                     onClick={() => dispatchChangeActivePlotView(pvAry[nextIdx].plotId)}
                />
                <a style={rightTitleStyle} className='ff-href text-nav-controls'
                   title={pTitle('Next: ', pvAry[nextIdx])}
                   onClick={() => dispatchChangeActivePlotView(pvAry[nextIdx].plotId)} >
                    {pTitle('',pvAry[nextIdx])}
                </a>
            </div>
            <div style={{textAlign:'center'}}>
                {dots}
            </div>
        </div>

    );


}

PagingControl.propTypes= {
    plotViewAry : PropTypes.array.isRequired,
    activePlotId: PropTypes.string.isRequired,
    expandedMode: PropTypes.object.isRequired
};
