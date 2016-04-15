/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import ReactDOM from 'react-dom';

import {flux, firefly} from 'firefly/Firefly.js';
import AppDataCntlr from 'firefly/core/AppDataCntlr.js';
import {LO_EXPANDED, LAYOUT_PATH, getActiveTableId, getExpandedMode, SHOW_DROPDOWN_UI} from 'firefly/core/LayoutCntlr.js';
import Menu from 'firefly/ui/Menu.jsx';
import Banner from 'firefly/ui/Banner.jsx';
import {SearchPanel} from 'firefly/ui/SearchPanel.jsx';
import {TestQueriesPanel} from 'firefly/ui/TestQueriesPanel.jsx';
import {ImageSelectDropdown} from 'firefly/ui/ImageSelectDropdown.jsx';
import {DropDownContainer} from 'firefly/ui/DropDownContainer.jsx';
import {ResultsPanel} from 'firefly/ui/ResultsPanel.jsx';
import {ExpandedModeDisplay} from 'firefly/visualize/iv/ExpandedModeDisplay.jsx';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import * as TblUtil from 'firefly/tables/TableUtil.js';
import {ChartsContainer} from 'firefly/visualize/ChartsContainer.jsx';
import {VisHeader} from 'firefly/visualize/ui/VisHeader.jsx';
import {VisToolbar} from 'firefly/visualize/ui/VisToolbar.jsx';
import {getActionFromUrl} from 'firefly/core/History.js';
import {MultiImageViewer} from 'firefly/visualize/ui/MultiImageViewer.jsx';
import {MultiViewStandardToolbar} from 'firefly/visualize/ui/MultiViewStandardToolbar.jsx';
import {TriViewImageSection,launchImageMetaDataSega} from    'firefly/visualize/ui/TriViewImageSection.jsx';


const HOME = {type: SHOW_DROPDOWN_UI, payload: {view:'AnyDataSetSearch'}};

firefly.bootstrap();
const goto = getActionFromUrl();
firefly.process(goto || HOME);
launchImageMetaDataSega();

const resultId = TblUtil.uniqueTblUiGid();

const App = React.createClass({

    propTypes: {
        appData : React.PropTypes.object.isRequired,
        title   : React.PropTypes.string,
        expandedMode : React.PropTypes.string,
        activeTblId : React.PropTypes.string
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
                            <TestQueriesPanel resultId={resultId} />
                            <ImageSelectDropdown />
                        </DropDownContainer>
                    </header>
                    <main>
                        <ResultsPanel title={title}
                            imagePlot ={<TriViewImageSection showCoverage={true} showFits={true}
                                                             showImageMetaData={true}
                                                             imageExpandedMode={expandedMode===LO_EXPANDED.images.view} />}
                            visToolbar = {<VisToolbar/>}
                            xyPlot = {<ChartsContainer key='results-xyplots' tblId={activeTblId} />}
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



// == Save this it is anternative to react-redux

// function getNextState() {
//     const storeState= flux.getState();
//     const appData= storeState[AppDataCntlr.APP_DATA_PATH];
//     const layout= storeState[LAYOUT_PATH];
//     const  title= 'FFTools entry point';
//     const  activeTblId= getActiveTableId();
//     const  expandedMode= getExpandedMode();
//     return {appData,layout,title,activeTblId,expandedMode};
//
// }
//
// class AppWrapper extends Component {
//
//     constructor(props,context) {
//         super(props, context);
//         this.state= getNextState();
//     }
//
//     getChildContext() {
//         return {store: flux.getRedux()};
//     }
//
//     shouldComponentUpdate(np, ns) { return sCompare(this, np, ns); }
//
//     componentDidMount() {
//         this.removeListener= flux.addListener(() => this.storeUpdate());
//         this.storeUpdate();
//     }
//
//     componentWillUnmount() { if (this.removeListener) this.removeListener(); }
//
//     storeUpdate() {
//         const {state}= this;
//         const ns= getNextState();
//         if (ns.appData!==state.appData ||
//             ns.layout!==state.layout ||
//             ns.activeTblId!==state.activeTblId ||
//             ns.title!==state.title ||
//             ns.expandedMode!==state.expandedMode) {
//             this.setState(ns);
//         }
//     }
//
//
//     render() { return ( <App {...this.state}/> ); }
// }
//
// AppWrapper.childContextTypes= {
//     store: PropTypes.object
// };
//
// ReactDOM.render(<AppWrapper/>,
//     document.getElementById('app')
// );

