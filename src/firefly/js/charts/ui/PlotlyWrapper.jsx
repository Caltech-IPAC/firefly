/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {get} from 'lodash';
import {getPlotLy} from '../PlotlyConfig.js';
import {logError} from '../../util/WebUtil.js';
import Enum from 'enum';

const PLOTLY_BASE_ID= 'plotly-plot';
var counter= 0;

export const RenderType= new Enum([ 'RESIZE', 'UPDATE', 'RESTYLE', 'RELAYOUT', 'RESTYLE_AND_RELAYOUT', 'NEW_PLOT'],
             { ignoreCase: true });


const defaultConfig= {
    displaylogo: false,
    modeBarButtonsToRemove :[
        'sendDataToCloud',
        'hoverClosestCartesian',
        'hoverCompareCartesian'
    ]
};

export function downloadChart(chartId) {
    getPlotLy().then( (Plotly) => {
        const chartDiv = document.getElementById(chartId);
        if (chartId && chartDiv) {
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
    }

    shouldComponentUpdate(np, ns) {
        const {data,layout, config, dataUpdate, layoutUpdate}= this.props;
        if (data===np.data && layout===np.layout && config===np.config) {

            const doRestyle= np.dataUpdate && dataUpdate!==np.dataUpdate;
            const doRelayout=np.layoutUpdate && layoutUpdate!==np.layoutUpdate;

            if (doRestyle && doRelayout) this.renderType= RenderType.RESTYLE_AND_RELAYOUT;
            else if (doRestyle)          this.renderType= RenderType.RESTYLE;
            else if (doRelayout)         this.renderType= RenderType.RELAYOUT;
            else                         this.renderType= RenderType.RESIZE;
        }
        else if (data!==np.data || layout!==np.layout && config===np.config) {
            // this.renderType= RenderType.UPDATE;
            this.renderType= RenderType.NEW_PLOT;
        }
        else {
            this.renderType= RenderType.NEW_PLOT;
        }
        return true;
    }

    draw() {
        const {data,layout, config= defaultConfig, newPlotCB, dataUpdate, layoutUpdate, dataUpdateTraces}= this.props;
        const renderType = this.renderType;
        getPlotLy().then( (Plotly) => {
            if (this.div) { // make sure the div is still there
                switch (renderType) {
                    case RenderType.RESTYLE:
                        Plotly.restyle(this.div, dataUpdate, dataUpdateTraces);
                        break;
                    case RenderType.RELAYOUT:
                        Plotly.relayout(this.div, layoutUpdate);
                        break;
                    case RenderType.RESTYLE_AND_RELAYOUT:
                        Plotly.restyle(this.div, dataUpdate, dataUpdateTraces);
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
                        }
                        if (newPlotCB) {
                            newPlotCB(this.div, Plotly);
                        }
                        break;
                }
            }
        } ).catch( (e) => {
            console.log('Plotly not loaded',e);
        });

    }

    refUpdate(ref) {
        const {divUpdateCB}= this.props;
        this.div= ref;
        if (divUpdateCB) {
            getPlotLy().then( (Plotly) => {
                divUpdateCB(this.div,Plotly);
            } );
        }
    }

    componentDidMount() {
        this.draw();
    }

    componentDidUpdate() {
        this.draw();
    }

    render() {
        const {chartId, style}= this.props;
        // note: wrapper div is the target for the simulated click event
        // when the original click event is lost and plotly_click is emitted instead
        // chart image download relies on div id matching chartId
        return (
            <div>
                <div id={chartId || this.id} style={style} ref={this.refUpdate}/>
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
    newPlotCB: PropTypes.func
};
