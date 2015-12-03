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
import TestHistogramPanel from 'firefly/visualize/TestHistogramPanel.jsx';

firefly.bootstrap();
firefly.process( {type : appDataCntlr.APP_LOAD} );

const menu = flux.createSmartComponent((state) => {
                return {menu: (get(state, `${appDataCntlr.APP_DATA_PATH}.menu`) || [])};
            }, Menu);

const App = React.createClass({

    propTypes: {
        appData : React.PropTypes.object.isRequired,
        title   : React.PropTypes.string
    },

    render() {

        if (!this.props.appData.isReady) {
            return (
                <div>
                    <p>Loading... </p>
                </div>
            );
        } else {

            return (
                <div>
                    <Banner
                        menu={menu}
                        appTitle='Histogram'
                    />
                    <h2>{this.props.title}</h2>
                    <div style={{paddingLeft:10}}>
                        <TestHistogramPanel/>
                    </div>
                </div>
            );
        }
    }
});

function connector(state) {
    return {
        appData: state[appDataCntlr.APP_DATA_PATH],
        title: 'Histogram Test Entry Point'
    };
}
const container = flux.createSmartComponent(connector, App);

ReactDOM.render(container,
    document.getElementById('app')
);