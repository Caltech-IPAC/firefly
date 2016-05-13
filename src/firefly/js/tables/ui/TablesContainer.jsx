/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, isUndefined} from 'lodash';

import {flux} from '../../Firefly.js';
import * as TblUtil from '../TableUtil.js';
import {TablePanel} from './TablePanel.jsx';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {dispatchTableRemove, dispatchActiveTableChanged} from '../TablesCntlr.js';


import {LO_EXPANDED, dispatchSetLayoutMode, getExpandedMode} from '../../core/LayoutCntlr.js';
import {CloseButton} from '../../ui/CloseButton.jsx';


export class TablesContainer extends Component {
    constructor(props) {
        super(props);
        const tblResults = TblUtil.getTableGroup();
        this.state = Object.assign({layout: 'tabs', tables: {}}, tblResults, {
                        expandedMode: props.expandedMode,
                        closeable: props.closeable
                    });
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    storeUpdate() {
        const tblResults = TblUtil.getTableGroup();
        const expandedMode = getExpandedMode() === LO_EXPANDED.tables.view;
        this.setState({expandedMode,...tblResults});
    }

    render() {
        const {expandedMode, tables, layout, active, closeable} = this.state;

        if (expandedMode) {
            return <ExpandedView {...{active, tables, layout, expandedMode, closeable}} />;
        } else {
            return isEmpty(tables) ? <div></div> : <TabsView {...{active, tables, expandedMode}} />;
        }
    }
}

TablesContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool
};
TablesContainer.defaultProps = {
    expandedMode: false,
    closeable: true
};



function ExpandedView(props) {
    const {tables, closeable} = props;
    return (
        <div style={{ display: 'flex', height: '100%', flexGrow: 1, flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                {closeable && <CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_EXPANDED.none)}/>}
            </div>
            {!isEmpty(tables) && <TabsView expandedMode={true} {...props} />}
        </div>
    );

}


function TabsView(props) {
    const {tables, expandedMode, active} = props;

    var activeIdx = Object.keys(tables).findIndex( (tbl_ui_id) => get(tables,[tbl_ui_id,'tbl_id']) === active);
    activeIdx = activeIdx === -1 ? 0 : activeIdx;
    const onTabSelect = (idx) => {
        const tbl_id = get(tables, [Object.keys(tables)[idx], 'tbl_id']);
        tbl_id && dispatchActiveTableChanged(tbl_id);
    };

    return (
        <Tabs defaultSelected={activeIdx} onTabSelect={onTabSelect}>
            {tablesAsTab(tables, expandedMode)}
        </Tabs>
    );
}
TabsView.propTypes = {
    expandedMode: PropTypes.bool,
    mode: PropTypes.oneOf(['tabs', 'grid'])
};

TabsView.defaultProps = {
    expandedMode: false,
    mode: 'tabs'
};


function tablesAsTab(tables, expandedMode) {

    return tables &&
        Object.keys(tables).map( (key) => {
            var {tbl_id, title, removable, tbl_ui_id} = tables[key];
            const onTabRemove = (tbl_id) => {
                dispatchTableRemove(tbl_id);
            };
            return  (
                <Tab key={tbl_ui_id} name={title} removable={removable} onTabRemove={onTabRemove}>
                    <TablePanel key={tbl_id} border={false} {...{tbl_id, tbl_ui_id, expandedMode}} />
                </Tab>
            );
        } );
}

