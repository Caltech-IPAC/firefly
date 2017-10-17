/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';

import {get, debounce, isEmpty, set, omit} from 'lodash';
import {getPlotLy} from '../PlotlyConfig.js';
import {getChartData, useChartRedraw, useScatterGL} from '../ChartsCntlr.js';
import {logError, deltas, flattenObject} from '../../util/WebUtil.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import Enum from 'enum';

const PLOTLY_BASE_ID= 'plotly-plot';
const MASKING_DELAY= 400;
let counter= 0;

const isDebug = () => get(window, 'firefly.debug', false);

export const RenderType= new Enum([ 'RESIZE', 'UPDATE', 'RESTYLE', 'RELAYOUT',
                                    'RESTYLE_AND_RELAYOUT', 'NEW_PLOT', 'PAUSE_DRAWING' ],
             { ignoreCase: true });


const defaultConfig= {
    displaylogo: false,
    displayModeBar: false,
    modeBarButtonsToRemove :[
        'sendDataToCloud',
        'hoverClosestCartesian',
        'hoverCompareCartesian'
    ]
};

const maskWrapper= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    height:'100%'
};

function isSlowResize() {
    return BrowserInfo.isFirefox();
}


export function downloadChart(chartId) {
    getPlotLy().then( (Plotly) => {
        // in API we can have same chart unexpanded and expanded
        const chartDivAll = document.querySelectorAll(`#${chartId}`);
        if (chartId && chartDivAll && chartDivAll.length > 0) {
            const chartDiv = chartDivAll[chartDivAll.length-1];
            const filename = get(chartDiv, 'layout.title') || chartId;
            Plotly.downloadImage(chartDiv, {
                format: 'png',
                filename
            });
        } else {
            logError(`Image download has failed for chart id ${chartId}`);
        }
    });
}

export class PlotlyWrapper extends Component {

    constructor(props) {
        super(props);
        counter++;
        this.id= PLOTLY_BASE_ID+counter;
        this.div= null;
        this.refUpdate= this.refUpdate.bind(this);
        this.renderType= RenderType.NEW_PLOT;
        this.state= {preMask: false, showMask:false};
        this.lastWidth= 0;
        this.lastHeight= 0;
        this.preMask= false;


        this.resizeUpdate= (postResizeRenderType) => {
            this.renderType= postResizeRenderType;
            this.forceUpdate();
        };

        this.resizeUpdateDebouncedLong= debounce(this.resizeUpdate,800);
        this.resizeUpdateDebouncedShort= debounce(this.resizeUpdate,100);


    }


    showMask(show) {
        this.preMask= show;
        if (show) {
            setTimeout( () => {
                if (!this.state.showMask && this.preMask) {
                    this.setState(() => ({showMask:true}));
                }
            }, MASKING_DELAY);
        }
        else {
            setTimeout( () => {
                if (this.state.showMask) {
                    this.setState(() => ({showMask:false}));
                }
            },0);
        }
    }

    /**
     *
     * @param useMask
     * @param treatAsResize
     * @param renderType
     */
    updateRenderType(useMask, treatAsResize, renderType) {
        if (treatAsResize) {
            this.renderType= RenderType.PAUSE_DRAWING;
            isSlowResize() ? this.resizeUpdateDebouncedLong(renderType) : this.resizeUpdateDebouncedShort(renderType);
        }
        else {
            this.renderType= renderType;
        }
        if (useMask || treatAsResize) this.showMask(true);
    }

    shouldComponentUpdate(np, ns) {

        let detectedResize= false;
        const rec= this.div.getBoundingClientRect();
        if (this.lastWidth!==rec.width || this.lastHeight!==rec.height) {
            this.lastWidth= rec.width;
            this.lastHeight=rec.height;
            detectedResize= true;
        }

        const {data,layout, config, dataUpdate, layoutUpdate}= this.props;
        const { maskOnLayout, maskOnRestyle, maskOnResize, maskOnNewPlot,
                autoSizePlot, doingResize, autoDetectResizing }= np;

        const treatAsResize= doingResize || (autoDetectResizing && detectedResize);

        if (shallowequal(data, np.data) && shallowequal(layout, np.layout) && shallowequal(config, np.config)) {  // these are the update cases

            const doRestyle= np.dataUpdate && dataUpdate!==np.dataUpdate;
            const doRelayout=np.layoutUpdate && layoutUpdate!==np.layoutUpdate;

            if (doRestyle && doRelayout) {
                this.updateRenderType(maskOnRestyle, treatAsResize, RenderType.RESTYLE_AND_RELAYOUT);
            }
            else if (doRestyle) {
                this.updateRenderType(maskOnRestyle, treatAsResize, RenderType.RESTYLE);
            }
            else if (doRelayout) {
                this.updateRenderType(maskOnLayout, treatAsResize, RenderType.RELAYOUT);
            }
            else if (detectedResize && autoSizePlot) {
                this.updateRenderType(maskOnResize, true, RenderType.RESIZE);
            }
            else { // in this case- the important props have not changed, therefore return on state (showMask) change
                   // everywhere else the props have changed so we return true
                this.renderType= RenderType.PAUSE_DRAWING;
                return this.state.showMask!==ns.showMask;
            }
        }
        else { // new plot case
            this.updateRenderType(maskOnNewPlot, treatAsResize, RenderType.NEW_PLOT);
        }
        return true;
    }

    optimize(graphDiv, renderType, {chartId, data, layout, dataUpdate=[], layoutUpdate, dataUpdateTraces, ...rest}) {
        const {lastUpdated} = getChartData(chartId);

        if (!useChartRedraw && lastUpdated && renderType ===  RenderType.NEW_PLOT && graphDiv.data) {
            // omitting 'firefly' from data[*] for now
            const ndata = data.map((d) => omit(d, 'firefly'));
            const nlayout = omit(layout, 'lastInputTime');

            const dataDelta = deltas(ndata, graphDiv.data || {});
            const layoutDelta = flattenObject(deltas(nlayout, graphDiv.layout || {}, false));

            const hasLayout = !isEmpty(layoutDelta);
            const hasData = !isEmpty(dataDelta);
            if(hasData) {
                dataUpdate = Object.values(dataDelta).map((d) => flattenObject(d));
                dataUpdateTraces = Object.keys(dataDelta).map((k) => parseInt(k));
                renderType = RenderType.RESTYLE;
            }
            if (hasLayout) {
                layoutUpdate = layoutDelta;
                renderType = RenderType.RELAYOUT;
            }
            if (hasData && hasLayout) {
                renderType = RenderType.RESTYLE_AND_RELAYOUT;
            }

            if (!useScatterGL) {
                // when using SVG, it's actually faster to redraw then to do multiple updates
                // if renderType is restyle, plotly render the inactive trace on top of the active trace
                // for chart with type histogram2d or histogram2dcontour
                if (renderType === RenderType.RESTYLE_AND_RELAYOUT || dataUpdate.length > 1 ||
                    (data.find((d) => get(d, 'type', '').includes('histogram2d')))) {
                    renderType = RenderType.NEW_PLOT;
                }
            }
        }

        return {renderType, chartId, data, layout, dataUpdate, layoutUpdate, dataUpdateTraces, ...rest};
    }

    draw() {
        let renderType = this.renderType;
        if (renderType===RenderType.PAUSE_DRAWING) return;

        getPlotLy().then( (Plotly) => {

            const optimized = this.optimize(this.div || {}, renderType, this.props);

            const {chartId, data,layout, config= defaultConfig, newPlotCB, dataUpdate, layoutUpdate, dataUpdateTraces} = optimized;
            renderType = optimized.renderType;

            if (this.div) { // make sure the div is still there
const now = Date.now();
                switch (renderType) {
                    case RenderType.RESTYLE:
                        this.restyle(this.div, Plotly, data, dataUpdate, dataUpdateTraces);
                        break;
                    case RenderType.RELAYOUT:
                        Plotly.relayout(this.div, layoutUpdate);
                        break;
                    case RenderType.RESTYLE_AND_RELAYOUT:
                        this.restyle(this.div, Plotly, data, dataUpdate, dataUpdateTraces);
                        Plotly.relayout(this.div, layoutUpdate);
                        break;
                    case RenderType.RESIZE:
                        Plotly.Plots.resize(this.div);
                        break;
                    case RenderType.UPDATE:
                        Plotly.update(this.div, data, layout);
                        break;
                    case RenderType.NEW_PLOT:
                        Plotly.newPlot(this.div, data, layout, config);
                        if (this.div.on) {
                            const chart = this.div;
                            chart.on('plotly_click', () => chart.parentElement.click());
                            chart.on('plotly_afterplot', () => this.showMask(false));
                            chart.on('plotly_autosize', () => this.showMask(false));
                            chart.on('plotly_relayout', () => this.showMask(false));
                            chart.on('plotly_restyle', () => this.showMask(false));
                            chart.on('plotly_redraw', () => this.showMask(false));
                            chart.on('plotly_relayout', (changes) => this.syncLayout(chartId, changes));
                        }
                        else {
                            this.showMask(false);
                        }
                        if (newPlotCB) newPlotCB(this.div, Plotly);

                        break;
                }
                set(getChartData(chartId), 'lastUpdated', Date.now());
                isDebug() && console.log(`${renderType.toString()} ${dataUpdateTraces} elapsed: ${Date.now() - now}`);
            }
        } ).catch( (e) => {
            console.log('Plotly not loaded',e);
        });
    }

    /**
     * This function sync the div.layout with chart's layout.
     * Will use direct object update instead of dispatch chart update to avoid
     * unneeded render/comparison.
     * @param chartId
     * @param changes
     */
    syncLayout(chartId, changes) {
        const {layout} = getChartData(chartId);
        if (layout) {
            Object.entries(changes).forEach( ([k, v]) => {
                if (k === 'xaxis' && Array.isArray(v)) {
                    set(layout, 'xaxis.range', v);
                    set(layout, 'xaxis.autorange', false);
                } else if (k === 'yaxis' && Array.isArray(v)) {
                    set(layout, 'yaxis.range', v);
                    set(layout, 'yaxis.autorange', false);
                } else if (k.includes('xaxis.range')) {
                    set(layout, k, v);
                    set(layout, 'xaxis.autorange', false);
                } else if (k.includes('yaxis.range')) {
                    set(layout, k, v);
                    set(layout, 'yaxis.autorange', false);
                } else {
                    set(layout, k, v);
                }
            });
        }
    }

    restyle(div, Plotly, data, dataUpdate, dataUpdateTraces) {
        if (Array.isArray(dataUpdate)) {
            dataUpdate.forEach((v,idx) => this.restyle(div, Plotly, data, v, dataUpdateTraces[idx]));
        } else {
            if (dataUpdateTraces >= get(div, 'data.length', 0)) {
                Plotly.addTraces(div, data[dataUpdateTraces]);      // addTraces structure is similar to newplot.. not updates
            } else {
                Plotly.restyle(div, dataUpdate, dataUpdateTraces);
            }
        }
    }

    refUpdate(ref) {
        const {divUpdateCB}= this.props;
        this.div= ref;
        if (this.div) {
            const rec= this.div.getBoundingClientRect();
            this.lastWidth= rec.width;
            this.lastHeight= rec.height;
        }
        if (divUpdateCB) {
            getPlotLy().then( (Plotly) => {
                divUpdateCB(this.div,Plotly);
            } );
        }
    }

    componentDidMount() { this.draw(); }

    componentDidUpdate() { this.draw(); }

    render() {
        const {chartId, style}= this.props;
        const {showMask}= this.state;
        // note: wrapper div is the target for the simulated click event
        // when the original click event is lost and plotly_click is emitted instead
        // chart image download relies on div id matching chartId
        const nstyle = Object.assign({position:'relative', height: '100%', width: '100%'}, style);
        return (
            <div style={nstyle} >
                <div id={chartId || this.id} style={{height: '100%', width: '100%'}} ref={this.refUpdate}/>
                {showMask && <div style={maskWrapper}> <div className='loading-mask'/> </div>}
            </div>
        );
    }
}

PlotlyWrapper.propTypes = {
    chartId: PropTypes.string,
    data: PropTypes.arrayOf(PropTypes.object),
    style :PropTypes.object,
    layout: PropTypes.object,
    config: PropTypes.object,
    dataUpdate: PropTypes.object,
    dataUpdateTraces: PropTypes.number,
    layoutUpdate: PropTypes.object,
    divUpdateCB: PropTypes.func,
    newPlotCB: PropTypes.func,
    maskOnRelayout :PropTypes.bool,
    maskOnRestyle :PropTypes.bool,
    maskOnResize : PropTypes.bool,
    maskOnNewPlot : PropTypes.bool,

    autoSizePlot : PropTypes.bool,
    autoDetectResizing : PropTypes.bool,
    doingResize: PropTypes.bool
};

PlotlyWrapper.defaultProps = {
    maskOnLayout : true,
    maskOnRestyle : false,
    maskOnResize : true,
    maskOnNewPlot : true,

    autoSizePlot : false,
    autoDetectResizing : false,
    doingResize: false
};
