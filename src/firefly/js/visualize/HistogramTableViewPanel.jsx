import React from 'react';

import {throttle} from 'lodash';
import Resizable from 'react-component-resizable';

import HistogramCntlr from '../visualize/HistogramCntlr.js';
import HistogramOptions from './HistogramOptions.jsx';
import Histogram from './Histogram.jsx';


var HistogramTablePanel = React.createClass({


    throttledResize: throttle( (size) => {
                if (size) {
                    this.setState({ widthPx: (size.width-20), heightPx: (size.height-10) });
                }
            }, 500, {'leading':false}),

    propTypes: {
        tblStatsData: React.PropTypes.object.isRequired,
        tblHistogramData : React.PropTypes.object.isRequired,
        width : React.PropTypes.string,
        height : React.PropTypes.string
    },

    getInitialState() {
        return {
            widthPx: 700,
            heightPx: 300,
            throttledResize: throttle(this.onResize, 500, {'leading':false, 'trailing':true})
        };
    },

    shouldComponentUpdate(nextProps, nextState) {
        return nextProps.tblStatsData !== this.props.tblStatsData ||
        nextProps.tblHistogramData !== this.props.tblHistogramData ||
            nextState !== this.state;
    },

    onResize(size) {
        if (size) {
            this.setState({ widthPx: (size.width-20), heightPx: (size.height-10), throttledResize: this.state.throttledResize });
        }
    },

    componentDidMount() {
        this.onResize();
    },

    renderOptions() {
        const { searchRequest, isColStatsReady, colStats } = this.props.tblStatsData;
        const formName = 'HistogramOptionsForm_'+searchRequest.tbl_id;

        if (isColStatsReady) {
            return (
                <HistogramOptions groupKey = {formName}
                                  colValStats={colStats}
                                  onOptionsSelected={(histogramParams) => {
                                            console.log(histogramParams);
                                            HistogramCntlr.dispatchLoadColData(histogramParams, searchRequest);
                                        }
                                      }/>
            );
        } else {
            return 'Loading Options...';
        }

    },

    renderHistogram() {
        if (!this.props.tblHistogramData) {
            return 'Select Histogram Parameters...';
        }
        const { isColDataReady, histogramData, histogramParams } = this.props.tblHistogramData;
        var {heightPx} = this.state;

        if (isColDataReady) {
            var logs = undefined;
            var reversed = undefined;
            if (histogramParams) {
                var logvals = '';
                if (histogramParams.x.includes('log')) { logvals += 'x';}
                if (histogramParams.y.includes('log')) { logvals += 'y';}
                if (logvals.length>0) { logs = logvals;}

                var rvals = '';
                if (histogramParams.x.includes('flip')) { rvals += 'x';}
                if (histogramParams.y.includes('flip')) { rvals += 'y';}
                if (rvals.length>0) { reversed = rvals;}

            }
            return (
                <Histogram data={histogramData}
                           desc={histogramParams.columnOrExpr}
                           binColor='#8c8c8c'
                           height={heightPx}
                           logs={logs}
                           reversed={reversed}
                />
            );
        } else {
            if (histogramParams) {
                return 'Loading Histogram...';
            } else {
                return 'Select Histogram Parameters';
            }

        }

    },

    render() {
        var {tblStatsData, width, height} = this.props;
        if (!tblStatsData) {
            return (<div>.....</div>);
        } else if (!tblStatsData.isTblLoaded) {
            return (<div>Loading Table...</div>);
        } else if (!tblStatsData.isColStatsReady) {
            return (<div>Loading Table Statistics...</div>);
        } else {
            var { widthPx, heightPx} = this.state;
            width = width || '100%';

            return (
                <Resizable id='histogram-resizer' style={{width, height}} onResize={this.state.throttledResize} {...this.props} >

                    <div style={{display:'inline-block', verticalAlign:'top', whiteSpace: 'nowrap'}}>
                        <div
                            style={{display:'inline-block',overflow:'auto',width:380,height:heightPx,border:'1px solid black', marginLeft:10}}>
                            {this.renderOptions()}
                        </div>
                        <div
                            style={{display:'inline-block',overflow:'auto', width:(widthPx-400),height:heightPx,border:'1px solid black', marginLeft:10}}>
                            {this.renderHistogram()}
                        </div>

                    </div>
                </Resizable>
            );
        }
    } }
);

export default HistogramTablePanel;