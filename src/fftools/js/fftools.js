/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import ReactDOM from 'react-dom';

import {flux, firefly} from 'firefly/Firefly.js';
import AppDataCntlr from 'firefly/core/AppDataCntlr.js';
import {LO_EXPANDED, LAYOUT_PATH, dispatchUpdateLayout, getExpandedMode} from 'firefly/core/LayoutCntlr.js';
import Menu from 'firefly/ui/Menu.jsx';
import Banner from 'firefly/ui/Banner.jsx';
import SearchPanel from 'firefly/ui/SearchPanel.jsx';
import {ResultsPanel} from 'firefly/ui/ResultsPanel.jsx';
import FormPanel from 'firefly/ui/FormPanel.jsx';
import TestImagePanel from 'firefly/visualize/ui/TestImagePanel.jsx';
import {ExpandedModeDisplay} from 'firefly/visualize/iv/ExpandedModeDisplay.jsx';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import Validate from 'firefly/util/Validate.js';
import * as TblUtil from 'firefly/tables/TableUtil.js';
import {ChartsTableViewPanel} from 'firefly/visualize/ChartsTableViewPanel.jsx';
import {VisHeader} from 'firefly/visualize/ui/VisHeader.jsx';
import {VisToolbar} from 'firefly/visualize/ui/VisToolbar.jsx';

import FieldGroup from 'firefly/ui/FieldGroup.jsx';
import ValidationField from 'firefly/ui/ValidationField.jsx';

import {TableRequest} from 'firefly/tables/TableRequest.js';

import * as TableStatsCntlr from 'firefly/visualize/TableStatsCntlr.js';
import * as TablesCntlr from 'firefly/tables/TablesCntlr.js';
import {dispatchTableAdded} from 'firefly/tables/TablesUiCntlr.js';
import {getRootURL} from 'firefly/util/BrowserUtil.js';
import {download} from 'firefly/util/WebUtil.js';


firefly.bootstrap();
firefly.process( {type : AppDataCntlr.APP_LOAD} );

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
    dispatchUpdateLayout( {search: false});
}

function doFileDownload() {
    download(getRootURL() + 'samplehistdata.csv');
}

const resultGroupId = TblUtil.uniqueTblUiGid();

const App = React.createClass({

    propTypes: {
        appData : React.PropTypes.object.isRequired,
        layout : React.PropTypes.object,
        title   : React.PropTypes.string,
        table   : React.PropTypes.object,
        expandedMode : React.PropTypes.string,
        activeTbl : React.PropTypes.object,
        tblStatsData : React.PropTypes.object,
        xyPlotData : React.PropTypes.object,
        histogramData : React.PropTypes.object
    },

    onSearchSubmit(request) {
        if (request.srcTable) {
            var treq = TableRequest.newInstance({
                id:'IpacTableFromSource',
                source: request.srcTable,
                tbl_id:  newActiveTblId(),
                filters: request.filters
            });

            TableStatsCntlr.dispatchSetupTblTracking(getCurrentActiveTblId());
            TablesCntlr.dispatchTableFetch(treq);
            dispatchTableAdded(resultGroupId, treq.tbl_id);
            hideSearchPanel();
        }
    },

    showError() {
        alert('Invalid input');

    },


    render() {
        var {appData, title, table, expandedMode, layout} = this.props;

        const tblId = table ? table.tbl_id : undefined;

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
                        <SearchPanel show={layout && layout.search}>
                            <FormPanel
                                width='640px' height='300px'
                                groupKey='TBL_BY_URL_PANEL'
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
                                    <ValidationField style={{width:500}}
                                                     fieldKey='filters'
                                                     groupKey='TBL_BY_URL_PANEL'
                                                     initialState= {{ 
                                                            value: '',
                                                            label : 'Filters:',
                                                            labelWidth : 120 
                                                         }}
                                                     />

                                </FieldGroup>
                            </FormPanel>
                        </SearchPanel>
                    </header>
                    <main>
                        <ResultsPanel title={title}
                            imagePlot = {expandedMode===LO_EXPANDED.images.view ?
                                             <ExpandedModeDisplay   key='results-plots-expanded' forceExpandedMode={true}/> :
                                             <TestImagePanel key='results-plots'/> }
                            visToolbar = {<VisToolbar/>}
                            xyPlot = {<ChartsTableViewPanel key='results-xyplots' tblId={tblId}/> }
                            tables = {tblId && <TablesContainer key='results-tables' tbl_ui_gid={resultGroupId} /> }
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
        appData: state[AppDataCntlr.APP_DATA_PATH],
        layout: state[LAYOUT_PATH],
        title: 'FFTools entry point',
        table : TblUtil.findTblById(activeTblId),
        expandedMode: getExpandedMode()
    };
}
const container = flux.createSmartComponent(connector, App);

ReactDOM.render(container,
    document.getElementById('app')
);