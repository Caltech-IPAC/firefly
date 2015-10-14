/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import _ from 'lodash';

import React from 'react';
import {flux, firefly} from 'firefly/Firefly.js';
import * as appData from 'firefly/core/AppData.js';

const App = React.createClass({

    render() {
        let v = _.get(this.props, 'appData.props.version') || 'unknown';
        if (!this.props.appData.isReady) {
            return (
                <div>
                    <p>Loading... </p>
                </div>
            );
        } else {
            return (
                <div>
                    <h2>{this.props.title}</h2>
                    <i>Version: {v}</i>
                </div>
            );
        }
    }
});

function connector(state) {
    return {
        appData: state.appData,
        title: 'FFTools entry point'
    };
}

firefly.bootstrap();
firefly.process( {type : appData.APP_LOAD} );

const container = flux.createSmartComponent(connector, App);

React.render(container,
    document.getElementById('app')
);