/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {VisHeaderView, VisPreview} from './VisHeaderView.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {VisToolbar} from './VisToolbar.jsx';
import {lastMouseCtx, lastMouseImageReadout} from '../VisMouseSync.js';
import {readoutRoot} from '../MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {primePlot} from '../PlotViewUtil';
import {isImage} from '../WebPlot';
import {useMouseStoreConnector} from './MouseStoreConnector.jsx';


function makeState() {
    return {vr:visRoot(), currMouseState:lastMouseCtx(), readoutData:lastMouseImageReadout(), readout:readoutRoot()};
}

export const ApiFullImageDisplay= memo(({closeFunc, viewerId, renderTreeId, showHealpixPixel}) => {
    const showHP= showHealpixPixel ??getAppOptions()?.hips?.readoutShowsPixel;
    const {vr,currMouseState, readout, readoutData}= useMouseStoreConnector(makeState);
    return (
        <RenderTreeIdCtx.Provider value={{renderTreeId}}>
            <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
                alignItems:'stretch', flexDirection:'column', position: 'relative'}}>
                <div style={{position: 'relative', marginBottom:'6px',
                    display:'flex', flexWrap:'nowrap', flexDirection:'row', justifyContent: 'center'}}
                     className='banner-background'>
                    <div style={{display:'flex', flexDirection:'row', alignItems:'flex-end'}}>
                        <VisHeaderView visRoot={vr} readoutData={readoutData}
                                       currMouseState={currMouseState} readout={readout}
                                       style={{
                                           height: 34,
                                           minHeight: 34,
                                           padding: '2px 0 1px 0'
                                       }}
                                       showHealpixPixel={showHP}/>
                    </div>
                </div>
                <div>
                    <VisToolbar messageUnder={Boolean(closeFunc)}/>
                </div>
                <div style={{flex: '1 1 auto', display:'flex'}}>
                    <MultiImageViewer viewerId= {viewerId}
                                      insideFlex={true}
                                      canReceiveNewPlots={NewPlotMode.create_replace.key}
                                      Toolbar={MultiViewStandardToolbar}/>
                </div>
                {isImage(primePlot(vr)) &&
                <div style={{display:'flex', flexDirection:'row', alignItems:'flex-end', position: 'absolute',
                    bottom: 3, right: 4, borderTop: '2px ridge', borderLeft: '2px ridge'
                }} >
                    <VisPreview {...{showPreview:true, visRoot:vr, currMouseState, readout}}/>
                </div>}
            </div>
        </RenderTreeIdCtx.Provider>
    );
});

ApiFullImageDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    viewerId: PropTypes.string,
    renderTreeId : PropTypes.string,
    showHealpixPixel: PropTypes.string // getAppOptions()?.hips?.readoutShowsPixel
};

ApiFullImageDisplay.defaultProps= {
    closeFunc:null
};

