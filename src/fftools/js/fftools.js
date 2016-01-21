/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import get from 'lodash/object/get';

import React from 'react';
import ReactDOM from 'react-dom';
import {flux, firefly} from 'firefly/Firefly.js';
import * as appDataCntlr from 'firefly/core/AppDataCntlr.js';
import Menu from 'firefly/ui/Menu.jsx';
import Banner from 'firefly/ui/Banner.jsx';
import SearchPanel from 'firefly/ui/SearchPanel.jsx';
import ResultsPanel from 'firefly/ui/ResultsPanel.jsx';
import FormPanel from 'firefly/ui/FormPanel.jsx';
import TestImagePanel from 'firefly/visualize/ui/TestImagePanel.jsx';
import TablePanel from 'firefly/tables/ui/TablePanel.jsx';
import Validate from 'firefly/util/Validate.js';
import TblUtil from 'firefly/tables/TableUtil.js';
import HistogramTableViewPanel from 'firefly/visualize/HistogramTableViewPanel.jsx';
import {VisHeader} from 'firefly/visualize/ui/VisHeader.jsx';
import {VisToolbar} from 'firefly/visualize/ui/VisToolbar.jsx';

import FieldGroup from 'firefly/ui/FieldGroup.jsx';
import CompleteButton from 'firefly/ui/CompleteButton.jsx';
import ValidationField from 'firefly/ui/ValidationField.jsx';

import {TableRequest} from 'firefly/tables/TableRequest.js';

import TableStatsCntlr from 'firefly/visualize/TableStatsCntlr.js';
import HistogramCntlr from 'firefly/visualize/HistogramCntlr.js';
import TablesCntlr from 'firefly/tables/TablesCntlr.js';
import {getRootURL} from 'firefly/util/BrowserUtil.js';
import {download} from 'firefly/util/WebUtil.js';


firefly.bootstrap();
firefly.process( {type : appDataCntlr.APP_LOAD} );

var currentUniqueId = 0;
var activeTblId = newActiveTblId();

function newActiveTblId() {
    activeTblId = 'id-'+currentUniqueId++;
    return activeTblId;
}

function getCurrentActiveTblId() {
    return activeTblId;
}

function hideSearchPanel() {
    appDataCntlr.dispatchUpdateLayout( {search: false});
}

function doFileDownload() {
    download(getRootURL() + 'samplehistdata.csv');
}

const App = React.createClass({

    propTypes: {
        appData : React.PropTypes.object.isRequired,
        title   : React.PropTypes.string,
        table   : React.PropTypes.object,
        activeTbl : React.PropTypes.object,
        tblStatsData : React.PropTypes.object,
        histogramData : React.PropTypes.object
    },

    onSearchSubmit(request) {
        console.log(request);
        if (request.srcTable) {
            var treq = TableRequest.newInstance({
                id:'IpacTableFromSource',
                source: request.srcTable,
                tbl_id:  newActiveTblId()
            });

            TableStatsCntlr.dispatchSetupTblTracking(getCurrentActiveTblId());
            TablesCntlr.dispatchFetchTable(treq);
            hideSearchPanel();
        }
    },

    showError() {
        alert('Invalid input');

    },


    render() {
        var {appData, title, table, tblStatsData, histogramData} = this.props;


        const v = get(this.props, 'appData.props.version') || 'unknown';
        if (!appData.isReady) {
            return (
                <div>
                    <p>Loading... </p>
                </div>
            );
        } else {
            return (
                <div id='App'>
                    <header>
                        <Banner
                            menu={<Menu menu={appData.menu} /> }
                            visPreview={<VisHeader/> }
                            appTitle='Firefly'
                        />
                        <SearchPanel show={appData.layoutInfo && appData.layoutInfo.search}>
                            <FormPanel
                                width='640px' height='300px'
                                action={TablesCntlr.FETCH_TABLE}
                                groupKey='TBL_BY_URL_PANEL'
                                params={ {id: 'IpacTableFromSource'} }
                                onSubmit={this.onSearchSubmit}
                                onCancel={hideSearchPanel}>
                                <p>
                                    <input type='button' name='dowload' value='Download Sample File' onClick={doFileDownload} />
                                </p>
                                <FieldGroup groupKey='TBL_BY_URL_PANEL' validatorFunc={null} keepState={true}>
                                    <ValidationField style={{width:500}}
                                                     fieldKey='srcTable'
                                                     groupKey='TBL_BY_URL_PANEL'
                                                     initialState= {{ 
                                                            value: 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
                                                            validator: Validate.validateUrl.bind(null, 'Source Table'),
                                                            tooltip: 'The URL to the source table',
                                                            label : 'Source Table:',
                                                            labelWidth : 120 
                                                         }}
                                    />
                                </FieldGroup>
                            </FormPanel>
                        </SearchPanel>
                        </header>
                    <main>
                        <ResultsPanel title={title}
                            imagePlot = {<TestImagePanel />}
                            visToolbar = {<VisToolbar/>}
                            xyPlot = {<HistogramTableViewPanel tblStatsData={tblStatsData} tblHistogramData={histogramData}/> }
                            tables = { <TablePanel tableModel={table} selectable={true}/> }
                            layoutInfo = { appData.layoutInfo }
                        />
                    </main>
                </div>
            );
        }
    }
});

function connector(state) {
    const activeTblId = getCurrentActiveTblId();
    return {
        appData: state[appDataCntlr.APP_DATA_PATH],
        title: 'FFTools entry point',
        table : TblUtil.findById(activeTblId),
        tblStatsData: get(state[TableStatsCntlr.TBLSTATS_DATA_KEY], activeTblId),
        histogramData: get(state[HistogramCntlr.HISTOGRAM_DATA_KEY], activeTblId)
    };
}
const container = flux.createSmartComponent(connector, App);

ReactDOM.render(container,
    document.getElementById('app')
);