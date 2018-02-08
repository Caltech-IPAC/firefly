
import React from 'react';
import PropTypes from 'prop-types';
import {WcsMatchType, dispatchWcsMatch} from '../ImagePlotCntlr.js';
import {getPlotViewById, primePlot} from '../PlotViewUtil.js';
import {isImage, isHiPS} from '../WebPlot.js';
import {addImageOutlineDrawingLayer} from '../task/PlotHipsTask.js';


const tStyle= {
    display:'inline-block',
    whiteSpace: 'nowrap',
    minWidth: '3em',
    paddingLeft : 5
};

export function WcsMatchOptions({wcsMatchType, activePlotId}) {

    return (
        <div style={{alignSelf:'center', paddingLeft:25}}>
            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={wcsMatchType===WcsMatchType.Standard}
                           onChange={(ev) => wcsMatchStandard(ev.target.checked, activePlotId) }
                    />
                </div>
                <div style={tStyle}>WCS Match</div>
            </div>

            <div>
                <div style={{display:'inline-block'}}>
                    <input style={{margin: 0}}
                           type='checkbox'
                           checked={wcsMatchType===WcsMatchType.Target}
                           onChange={(ev) => wcsMatchTarget(ev.target.checked, activePlotId) }
                    />
                </div>
                <div style={tStyle}>Target Match</div>
            </div>
        </div>

    );
}

WcsMatchOptions.propTypes= {
    wcsMatchType : PropTypes.any,
    activePlotId: PropTypes.string,
};

function wcsMatchStandard(doWcsStandard, plotId) {
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Standard:false, plotId});
}


function wcsMatchTarget(doWcsTarget, plotId) {
    dispatchWcsMatch({matchType:doWcsTarget?WcsMatchType.Target:false, plotId});
}



export function HiPSMatchingOptions({visRoot, plotIdAry}) {
    const imageDataViewers= plotIdAry.filter( (plotId) => isImage(primePlot(visRoot,plotId)));
    const hipsViewers= plotIdAry.filter( (plotId) => isHiPS(primePlot(visRoot,plotId)));
    const activePV= getPlotViewById(visRoot, visRoot.activePlotId);
    const matchPV= isImage(primePlot(activePV)) ? activePV : getPlotViewById(visRoot, imageDataViewers[0]);
    if (!imageDataViewers.length || !hipsViewers.length ) return false;

    return (
        <div style={{marginLeft: 20, marginRight: 5}}>
            <input  type='button'
                    value={'Match Image to HiPS'}
                    onClick={()=>addImageOutlineDrawingLayer(matchPV, hipsViewers) } />
        </div>
    );

}

