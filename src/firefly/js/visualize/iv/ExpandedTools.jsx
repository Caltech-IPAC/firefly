/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import numeral from 'numeral';
import {getPlotGroupById}  from '../PlotGroup.js';
import {visRoot, ExpandType, dispatchChangeExpandedMode} from '../ImagePlotCntlr.js';
import {convertZoomToString} from '../ZoomUtil.js';
import {getActivePlotView} from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {PlotTitle, TitleType} from './PlotTitle.jsx';
import {CloseButton} from '../../ui/CloseButton.jsx';


import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import LIST from 'html/images/icons-2014/ListOptions.png';

const tStyle= {
    display:'inline-block',
    whiteSpace: 'nowrap',
    minWidth: '3em',
    paddingLeft : 5
};




function createOptions(expandedMode) {
    var autoPlay= false;
    if (expandedMode===ExpandType.SINGLE) {
        autoPlay= (
            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={false}
                           onChange={() => console.log('auto play') }
                    />
                </div>
                <div style={tStyle}>TODO: auto</div>
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

export function ExpandedTools({allPlots}) {
    var {expandedMode}= allPlots;
    var single= expandedMode===ExpandType.SINGLE;
    var pv= getActivePlotView(allPlots);
    var plotTitle;
    if (pv && pv.primaryPlot) {
        var {title, zoomFactor, plotState}=pv.primaryPlot;
        plotTitle= (
            <PlotTitle brief={false} titleStr={title} inline={false}
                       titleType={TitleType.EXPANDED}
                       zoomFactor={zoomFactor}
                       plotState={plotState} plotId={pv.plotId}
            />
        );

    }
    return (
        <div style={{width:'100%', height:70}}>
            <CloseButton style={s} onClick={() => console.log('ExpandedTools: back button')}/>
            <div style={s}>{single ? plotTitle : 'Tiled View'} </div>
            <div style={s}></div>
            <WhichView  allPlots={allPlots}/>
            {createOptions(expandedMode)}
            {single ? <div style={s}>paging(single only)</div> : false }
        </div>
    );
}

//<div style={s}>checkboxes: wcs target match, wcs match, auto play (single only)</div>
//{makeInlineTitle(visRoot,pv)}

ExpandedTools.propTypes= {
    allPlots : PropTypes.object.isRequired
};




function WhichView({allPlots}) {
    return (
        <div style={{display: 'inline-block'}}>
            <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                           enabled={true} visible={true}
                           horizontal={true}
                           onClick={() => dispatchChangeExpandedMode(ExpandType.SINGLE)}/>
            <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                           enabled={true} visible={true} horizontal={true}
                           onClick={() => dispatchChangeExpandedMode(ExpandType.GRID)}/>
            <ToolbarButton icon={LIST} tip={'Choose which plots to show'}
                           enabled={true} visible={true} horizontal={true}
                           todo={true}
                           onClick={() => console.log('Choose which plots to show')}/>
        </div>
    );
}

//{makeInlineTitle(visRoot,pv)}

WhichView.propTypes= {
    allPlots : PropTypes.object.isRequired
};
