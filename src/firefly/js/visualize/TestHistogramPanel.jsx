import React from 'react';

import {set} from 'lodash';

import FieldGroup from '../ui/FieldGroup.jsx';
import CompleteButton from '../ui/CompleteButton.jsx';
import ValidationField from '../ui/ValidationField.jsx';

import HistogramCntlr from '../visualize/HistogramCntlr.js';
import HistogramTableViewPanel from '../visualize/HistogramTableViewPanel.jsx';

import TablePanel from '../tables/ui/TablePanel.jsx';
import TableUtil from '../tables/TableUtil.js';

import TablesCntlr from '../tables/TablesCntlr.js';
import TableRequest from '../tables/TableRequest.js';
import {REQ_PRM} from '../tables/TableRequest.js';

const TEST_TABLE_ID = 'activeTable';

const TestHistogramPanel = React.createClass({

    propTypes: {
        title : React.PropTypes.string.isRequired,
        activeTbl : React.PropTypes.object.isRequired,
        histogramData: React.PropTypes.object.isRequired
    },

    loadViewer(request) {
        console.log(request);
        if (request.srcTable) {
            var treq = TableRequest.newInstance({
                                id:'IpacTableFromSource',
                                source: request.srcTable
            });
            set(treq, REQ_PRM.TBL_ID, TEST_TABLE_ID);

            HistogramCntlr.dispatchSetupTblTracking(TEST_TABLE_ID);
            TablesCntlr.dispatchFetchTable(treq);
        }
    },

    showError() {
        alert('Invalid input');

    },

    getResults() {
        var {activeTbl, histogramData} = this.props;
        if (activeTbl) {
            return (
                    <div>
                        <TablePanel style={{height: '400px'}}
                            tableModel={activeTbl}
                            selectable={true}
                        />
                        <br/>
                        <div style={{height: '300px'}}>
                            <HistogramTableViewPanel tblHistogramData={histogramData}/>
                        </div>
                    </div>
                );
        } else {
            return (
                <div></div>
            );
        }

    },

    render() {
        var {title} = this.props;
        return (
            <div>
                <h2>{title}</h2>
                <div style={{paddingLeft:10}}>
                    <div style={{display:'inline-block', verticalAlign:'top'}}>
                        <FieldGroup groupKey='TBL_BY_URL_PANEL' validatorFunc={null} keepState={true}>
                            <ValidationField style={{width:500}}
                                             fieldKey='srcTable'
                                             groupKey='TBL_BY_URL_PANEL'
                                             initialState= {{ 
                                                    value: 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
                                                    validator: null,
                                                    tooltip: 'The URL to the source table',
                                                    label : 'Source Table:',
                                                    labelWidth : 120 
                                                 }}
                            />
                            <br/><br/><br/>
                            <CompleteButton groupKey='TBL_BY_URL_PANEL'
                                            onSuccess={this.loadViewer}
                                            onFail={this.showError}
                            />
                        </FieldGroup>
                    </div>
                    <br/>
                    <br/>
                    {this.getResults()}
                </div>
            </div>
        );
    }
});


export default TestHistogramPanel;