import React from 'react';

import {throttle} from 'lodash';
import Resizable from 'react-component-resizable';

import XYPlotCntlr from '../visualize/XYPlotCntlr.js';
import XYPlotOptions from '../visualize/XYPlotOptions.jsx';
import XYPlot from '../visualize/XYPlot.jsx';


var XYPlotTablePanel = React.createClass({


    throttledResize: throttle( (size) => {
                if (size) {
                    this.setState({ widthPx: (size.width-20), heightPx: (size.height-10) });
                }
            }, 500, {'leading':false}),

    propTypes: {
        tblStatsData: React.PropTypes.object.isRequired,
        tblPlotData : React.PropTypes.object.isRequired,
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
        nextProps.tblPlotData !== this.props.tblPlotData ||
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
        const formName = 'XYPlotOptionsForm_'+searchRequest.tbl_id;

        if (isColStatsReady) {
            return (
                <XYPlotOptions groupKey = {formName}
                                  colValStats={colStats}
                                  onOptionsSelected={(xyPlotParams) => {
                                            console.log(xyPlotParams);
                                            XYPlotCntlr.dispatchLoadPlotData(xyPlotParams, searchRequest);
                                        }
                                      }/>
            );
        } else {
            return 'Loading Options...';
        }

    },

    renderHistogram() {
        if (!this.props.tblPlotData) {
            return 'Select XY plot parameters...';
        }
        const { isPlotDataReady, xyPlotData, xyPlotParams } = this.props.tblPlotData;
        var {heightPx} = this.state;

        if (isPlotDataReady) {
            var logs = undefined;
            var reversed = undefined;
            if (xyPlotParams) {
                var logvals = '';
                var rvals = '';
                if (xyPlotParams.x.options) {
                    if (xyPlotParams.x.options.includes('log')) { logvals += 'x'; }
                    if (xyPlotParams.x.includes('flip')) { rvals += 'x';}
                }
                if (xyPlotParams.y.options)  {
                    if (xyPlotParams.y.options.includes('log')) { logvals += 'y';}
                    if (xyPlotParams.y.includes('flip')) { rvals += 'y';}
                }
                if (logvals.length>0) { logs = logvals;}
                if (rvals.length>0) { reversed = rvals;}

            }
            return (
                <XYPlot data={xyPlotData}
                           desc={xyPlotParams.x.columnOrExpr+' vs. '+xyPlotParams.y.columnOrExpr}
                           height={heightPx}
                           params={xyPlotParams}
                           logs={logs}
                           reversed={reversed}
                />
            );
        } else {
            if (xyPlotParams) {
                return 'Loading XY plot...';
            } else {
                return 'Select XY plot parameters';
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
                <Resizable id='xyplot-resizer' style={{width, height}} onResize={this.state.throttledResize} {...this.props} >

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

export default XYPlotTablePanel;