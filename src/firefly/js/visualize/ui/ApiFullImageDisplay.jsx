/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';
import {VisHeaderView, VisPreview} from './VisHeaderView.jsx';
// import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {VisToolbar} from './VisToolbar.jsx';
import {addImageReadoutUpdateListener, lastMouseCtx, lastMouseImageReadout} from '../VisMouseSync.js';
import {readoutRoot} from '../../visualize/MouseReadoutCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {primePlot} from '../PlotViewUtil';
import {isImage} from '../WebPlot';

// import {ExpandedTools} from '../iv/ExpandedTools.jsx';



export class ApiFullImageDisplay extends PureComponent {
    constructor(props) {
        super(props);
        const showHealpixPixel= get(getAppOptions(), 'hips.readoutShowsPixel');
        this.state= {visRoot:visRoot(), currMouseState:lastMouseCtx(), readout:readoutRoot(), showHealpixPixel};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addImageReadoutUpdateListener(() => this.storeUpdate());
    }

    storeUpdate() {
        const {currMouseState,readoutData}= this.state;
        if (visRoot()!==this.state.visRoot ||
            lastMouseCtx() !==currMouseState ||
            lastMouseImageReadout()!== readoutData ||
            readoutRoot()!==this.state.readout) {
            this.setState({visRoot:visRoot(), currMouseState:lastMouseCtx(),
                readoutData:lastMouseImageReadout(), readout:readoutRoot()});
        }
    }

    /**
     *
     * @return {XML}
     */
    render() {
        const {closeFunc, viewerId}= this.props;
        const {visRoot,currMouseState, readout, readoutData, showHealpixPixel}= this.state;
        const plot= primePlot(visRoot);
        return (
            <RenderTreeIdCtx.Provider value={{renderTreeId : this.props.renderTreeId}}>
                <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
                    alignItems:'stretch', flexDirection:'column', position: 'relative'}}>
                    <div style={{position: 'relative', marginBottom:'6px',
                        display:'flex', flexWrap:'nowrap', flexDirection:'row', justifyContent: 'center'}}
                         className='banner-background'>
                        <div style={{display:'flex', flexDirection:'row', alignItems:'flex-end'}}>
                            <VisHeaderView visRoot={visRoot} readoutData={readoutData}
                                           currMouseState={currMouseState} readout={readout}
                                           style={{
                                               height: 34,
                                               minHeight: 34,
                                               padding: '2px 0 1px 0'
                                           }}
                                           showHealpixPixel={showHealpixPixel}/>
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
                    {isImage(plot) &&
                       <div style={{display:'flex', flexDirection:'row', alignItems:'flex-end', position: 'absolute',
                           bottom: 3, right: 4, borderTop: '2px ridge', borderLeft: '2px ridge'
                       }} >
                           <VisPreview {...{showPreview:true, visRoot, currMouseState, readout}}/>
                       </div>}
                </div>
            </RenderTreeIdCtx.Provider>
            );
    }
}

ApiFullImageDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    viewerId: PropTypes.string,
    renderTreeId : PropTypes.string
};

ApiFullImageDisplay.defaultProps= {
    closeFunc:null
};

