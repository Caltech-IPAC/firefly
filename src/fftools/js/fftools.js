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
import TestImagePanel from 'firefly/visualize/ui/TestImagePanel.jsx';
import TablePanel from 'firefly/tables/ui/TablePanel.jsx';
import TblUtil from 'firefly/tables/TableUtil.js';

import TablesCntlr from 'firefly/tables/TablesCntlr.js';
import TableRequest from 'firefly/tables/TableRequest.js';
import {REQ_PRM} from 'firefly/tables/TableRequest.js';
import {getRootURL} from 'firefly/util/BrowserUtil.js';


firefly.bootstrap();
firefly.process( {type : appDataCntlr.APP_LOAD} );

const menu = flux.createSmartComponent((state) => {
                return {menu: (get(state, `${appDataCntlr.APP_DATA_PATH}.menu`) || [])};
            }, Menu);

function loadTestTable() {
    var request = TableRequest.newInstance('IpacTableFromSource');
    request.setParam('source', getRootURL() + 'WiseQuery.tbl');
    request.setParam(REQ_PRM.TBL_ID, 'id-101');
    TablesCntlr.dispatchFetchTable(request);
}

const App = React.createClass({

    propTypes: {
        appData : React.PropTypes.object.isRequired,
        title   : React.PropTypes.string,
        table   : React.PropTypes.object
    },

    render() {
        const v = get(this.props, 'appData.props.version') || 'unknown';
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
                        appTitle='Firefly'
                    />
                    <h2>{this.props.title}</h2>
                    <div style={{paddingLeft:10}}>
                        <TestImagePanel/>
                    </div>
                    <div style={{padding:'10', height: '500px'}}>
                        <input type="button" name="test" value="Load Table Below" onClick={loadTestTable} />
                        <TablePanel
                            tableModel={this.props.table}
                            //width={1250}
                            //height={400}
                            selectable={true}
                        />
                    </div>
                </div>
            );
        }
    }
});

function connector(state) {
    return {
        appData: state[appDataCntlr.APP_DATA_PATH],
        title: 'FFTools entry point',
        table : TblUtil.findById('id-101')
    };
}
const container = flux.createSmartComponent(connector, App);

ReactDOM.render(container,
    document.getElementById('app')
);