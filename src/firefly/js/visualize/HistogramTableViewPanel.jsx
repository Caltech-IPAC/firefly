import React from 'react';

import {dispatchLoadColData} from './HistogramCntlr.js';
import HistogramOptions from './HistogramOptions.jsx';
import Histogram from './Histogram.jsx';


var HistogramTablePanel = React.createClass({

        propTypes: {
            tblHistogramData : React.PropTypes.object.isRequired
        },

        shouldComponentUpdate(nextProps) {
            return nextProps.tblHistogramData !== this.props.tblHistogramData;
        },

        renderOptions() {
            const { searchRequest, isColStatsReady, colStats } = this.props.tblHistogramData;
            const formName = 'HistogramOptionsForm_'+searchRequest.tbl_id;

            if (isColStatsReady) {
                return (
                    <HistogramOptions groupKey = {formName}
                                      colValStats={colStats}
                                      onOptionsSelected={(histogramParams) => {
                                            console.log(histogramParams);
                                            dispatchLoadColData(histogramParams, searchRequest);
                                        }
                                      }/>
                );
            } else {
                return 'Loading Options...';
            }

        },

        renderHistogram() {
            const { isColDataReady, histogramData, histogramParams } = this.props.tblHistogramData;

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
                               height={250}
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
            var {tblHistogramData} = this.props;
            if (!tblHistogramData) {
                return (<div>.....</div>);
            } else if (!tblHistogramData.isTblLoaded) {
                return (<div>Loading Table...</div>);
            } else if (!tblHistogramData.isColStatsReady) {
                return (<div>Loading Table Statistics...</div>);
            } else {
                return (
                    <div>
                        <div style={{display:'inline-block', verticalAlign:'top', whiteSpace: 'nowrap'}}>
                            <div
                                style={{display:'inline-block',overflow:'auto',width:380,height:250,border:'1px solid black', marginLeft:10}}>
                                {this.renderOptions()}
                            </div>
                            <div
                                style={{display:'inline-block',overflow:'auto', width:520,height:250,border:'1px solid black', marginLeft:10}}>
                                {this.renderHistogram()}
                            </div>
                        </div>
                    </div>
                );
            }
        } }
);

export default HistogramTablePanel;