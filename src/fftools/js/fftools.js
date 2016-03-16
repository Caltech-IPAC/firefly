/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import ReactDOM from 'react-dom';

import {flux, firefly} from 'firefly/Firefly.js';
import AppDataCntlr from 'firefly/core/AppDataCntlr.js';
import {LO_EXPANDED, LAYOUT_PATH, getActiveTableId, getExpandedMode} from 'firefly/core/LayoutCntlr.js';
import Menu from 'firefly/ui/Menu.jsx';
import Banner from 'firefly/ui/Banner.jsx';
import {SearchPanel} from 'firefly/ui/SearchPanel.jsx';
import {DropDownContainer} from 'firefly/ui/DropDownContainer.jsx';
import {ResultsPanel} from 'firefly/ui/ResultsPanel.jsx';
import TestImagePanel from 'firefly/visualize/ui/TestImagePanel.jsx';
import {ExpandedModeDisplay} from 'firefly/visualize/iv/ExpandedModeDisplay.jsx';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import * as TblUtil from 'firefly/tables/TableUtil.js';
import {ChartsTableViewPanel} from 'firefly/visualize/ChartsTableViewPanel.jsx';
import {VisHeader} from 'firefly/visualize/ui/VisHeader.jsx';
import {VisToolbar} from 'firefly/visualize/ui/VisToolbar.jsx';


firefly.bootstrap();
firefly.process( {type : AppDataCntlr.APP_LOAD} );

const resultId = TblUtil.uniqueTblUiGid();

const App = React.createClass({

    propTypes: {
        appData : React.PropTypes.object.isRequired,
        title   : React.PropTypes.string,
        expandedMode : React.PropTypes.string,
        activeTblId : React.PropTypes.string,
    },

    showError() {
        alert('Invalid input');

    },


    render() {
        var {appData, title, expandedMode, activeTblId} = this.props;

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
                        <DropDownContainer>
                            <SearchPanel resultId={resultId} />
                        </DropDownContainer>
                    </header>
                    <main>
                        <ResultsPanel title={title}
                            imagePlot = {expandedMode===LO_EXPANDED.images.view ?
                                             <ExpandedModeDisplay   key='results-plots-expanded' forceExpandedMode={true}/> :
                                             <TestImagePanel key='results-plots'/> }
                            visToolbar = {<VisToolbar/>}
                            xyPlot = {<ChartsTableViewPanel key='results-xyplots' tblId={activeTblId} /> }
                            tables = {<TablesContainer key='results-tables' tbl_ui_gid={resultId} /> }
                        />
                    </main>
                </div>
            );
        }
    }
});

function connector(state) {
    return {
        appData: state[AppDataCntlr.APP_DATA_PATH],
        layout: state[LAYOUT_PATH],
        title: 'FFTools entry point',
        activeTblId: getActiveTableId(),
        expandedMode: getExpandedMode()
    };
}
const container = flux.createSmartComponent(connector, App);

ReactDOM.render(container,
    document.getElementById('app')
);