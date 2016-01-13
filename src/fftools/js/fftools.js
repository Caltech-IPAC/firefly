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
import TblUtil from 'firefly/tables/TableUtil.js';
import TestHistogramPanel from 'firefly/visualize/TestHistogramPanel.jsx';
import {VisHeader} from 'firefly/visualize/ui/VisHeader.jsx';
import {VisToolbar} from 'firefly/visualize/ui/VisToolbar.jsx';

import HistogramCntlr from 'firefly/visualize/HistogramCntlr.js';
import TablesCntlr from 'firefly/tables/TablesCntlr.js';
import {getRootURL} from 'firefly/util/BrowserUtil.js';
import {download} from 'firefly/util/WebUtil.js';


firefly.bootstrap();
firefly.process( {type : appDataCntlr.APP_LOAD} );

const loadTestData = {
    id: 'IpacTableFromSource',
    source: getRootURL() + 'WiseQuery.tbl',
    tbl_id: 'id-101'
};

function hideSearchPanel() {
    flux.process({type:appDataCntlr.SEARCH_HIDE});
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
        histogramData : React.PropTypes.object
    },

    render() {
        var {appData, title, table, activeTbl, histogramData} = this.props;

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
                                width='500px' height='300px'
                                action={TablesCntlr.FETCH_TABLE}
                                params={ loadTestData }
                                onSubmit={hideSearchPanel}
                                onCancel={hideSearchPanel}>
                                <b>Click Search to load a test table</b>
                                <p>
                                    <input type='button' name='dowload' value='Download Sample File' onClick={doFileDownload} />
                                </p>
                            </FormPanel>
                        </SearchPanel>
                        </header>
                    <main>
                        <VisToolbar/>
                        <ResultsPanel title={title}
                            imagePlot = {<TestImagePanel />}
                            xyPlot = {<TestHistogramPanel title='Table with a histogram view' activeTbl={activeTbl} histogramData={histogramData}/> }
                            tables = { <TablePanel tableModel={table} selectable={true}/> }
                        />
                    </main>
                </div>
            );
        }
    }
});

function connector(state) {
    return {
        appData: state[appDataCntlr.APP_DATA_PATH],
        title: 'FFTools entry point',
        table : TblUtil.findById('id-101'),
        activeTbl : TblUtil.findById('activeTable'),
        histogramData: get(state[HistogramCntlr.HISTOGRAM_DATA_KEY], 'activeTable')

    };
}
const container = flux.createSmartComponent(connector, App);

ReactDOM.render(container,
    document.getElementById('app')
);