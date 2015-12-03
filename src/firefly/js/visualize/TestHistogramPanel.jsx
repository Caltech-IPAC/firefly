import React from 'react/addons';
import {flux} from '../Firefly.js';

import {dispatchLoadTblStats, dispatchLoadColData, HISTOGRAM_DATA_KEY} from './HistogramCntlr.js';
import HistogramOptions from './HistogramOptions.jsx';
import Histogram from './Histogram.jsx';
import CompleteButton from '../ui/CompleteButton.jsx';
import ValidationField from '../ui/ValidationField.jsx';
import Validate from '../util/Validate.js';
import FieldGroup from '../ui/FieldGroup.jsx';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';

import {ServerRequest} from '../data/ServerRequest.js';



var TestHistogramPanel = React.createClass({

        storeListenerRemove : null,

        getInitialState() {
            return {
                histogramStore: flux.getState()[HISTOGRAM_DATA_KEY],
                fldStore: flux.getState()[FieldGroupCntlr.FIELD_GROUP_KEY]
            };


            /*
             isColStatsReady: histogramStore.isColStatsReady,
             colStats: histogramStore.colStats,
             searchReq: histogramStore.searchReq
             */
        },

        componentWillUnmount() {
            if (this.storeListenerRemove) this.storeListenerRemove();
        },


        componentDidMount() {
            this.storeListenerRemove= flux.addListener(this.storeUpdate);
        },

        storeUpdate() {
            var updateState = false;
            var histogramStore = flux.getState()[HISTOGRAM_DATA_KEY];
            if (histogramStore !== this.state.histogramStore) {
                updateState = true;
            }
            var fldStore = flux.getState()[FieldGroupCntlr.FIELD_GROUP_KEY];
            if (fldStore !== this.state.fldStore) {
                updateState = true;
            }
            if (updateState) {
                this.setState({histogramStore, fldStore});
            }
        },


        showResults(success, request) {
            console.log(request);
            if (request.srcTable) {
                const sreq = new ServerRequest('IpacTableFromSource');
                sreq.setParam({name : 'source', value : request.srcTable});
                sreq.setParam({name : 'startIdx', value : '0'});
                sreq.setParam({name : 'pageSize', value : '10000'});
                dispatchLoadTblStats(sreq);
            }
        },
        resultsFail(request) {
            this.showResults(false,request);
        },

        resultsSuccess(request) {
            this.showResults(true,request); 
        },

        renderOptions() {
            const { searchReq, isColStatsReady, colStats } = this.state.histogramStore;
            var optionsForm = this.state.fldStore.fieldGroupMap['HISTOGRAM_OPTIONS_FORM'];
            var algorithm =  (optionsForm) ? optionsForm.fields.algorithm.value : 'fixedSizeBins';

            if (isColStatsReady) {
                return (
                    <HistogramOptions groupKey = 'HISTOGRAM_OPTIONS_FORM'
                                      algorithm= {algorithm}
                                      colValStats={colStats}
                                      onOptionsSelected={(histogramParams) => {
                                            console.log(histogramParams);
                                            dispatchLoadColData(histogramParams, searchReq);
                                        }
                                      }/>
                );
            } else {
                return 'Loading Options...';
            }

        },

        renderHistogram() {
            const { isColDataReady, histogramData, histogramParams } = this.state.histogramStore;

            if (isColDataReady) {
                return (
                    <Histogram data={histogramData}
                               desc={histogramParams.columnOrExpr}
                               binColor='#c8c8c8'
                               height={250}
                    />
                );
            } else {
                return 'Loading Histogram...';
            }

        },

        render() {
            return (
                <div>
                    <div style={{display:'inline-block', verticalAlign:'top'}}>
                        <FieldGroup groupKey='TEST_HISTOGRAM_PANEL' validatorFunc={null} keepState={true}>
                            <ValidationField style={{width:500}}
                                             fieldKey='srcTable'
                                             groupKey='TEST_HISTOGRAM_PANEL'
                                             initialState= {{ 
                                                value: 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
                                                validator: null,
                                                tooltip: 'The URL to the source table',
                                                label : 'Source Table:',
                                                labelWidth : 120 
                                             }}
                                />
                            <div style={{height:10}}/>
                            <CompleteButton groupKey='TEST_HISTOGRAM_PANEL'
                                            onSuccess={this.resultsSuccess}
                                            onFail={this.resultsFail}
                                />
                        </FieldGroup>
                    </div>
                    <br/><br/>
                    <div style={{display:'inline-block', verticalAlign:'top'}}>
                        <div style={{display:'inline-block',overflow:'auto',width:400,height:300,border:'1px solid black', marginLeft:10}}>
                            {this.renderOptions()}
                        </div>
                        <div style={{display:'inline-block',overflow:'auto', width:600,height:300,border:'1px solid black', marginLeft:10}}>
                            {this.renderHistogram()}
                        </div>
                    </div>
                </div>
            );
        } }
);

export default TestHistogramPanel;