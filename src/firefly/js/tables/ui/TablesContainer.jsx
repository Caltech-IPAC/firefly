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
import {dispatchTableUiRemoved} from '../TablesUiCntlr.js';
import {dispatchTableRemove} from '../TablesCntlr.js';


import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_EXPANDED, dispatchSetLayoutMode, getExpandedMode, dispatchActiveTableChanged} from '../../core/LayoutCntlr.js';
import {CloseButton} from '../../ui/CloseButton.jsx';

import LOADING from 'html/images/gxt/loading.gif';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';

export class TablesContainer extends Component {
    constructor(props) {
        super(props);
        const uiGroup = TblUtil.findUiGroupById(props.tbl_ui_gid);
        this.state = uiGroup ||
            {
                layout: 'tabs',
                tables: {}
            };
        this.state.expandedMode= getExpandedMode() === LO_EXPANDED.tables.view;
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
        const tblUiGroup = TblUtil.findUiGroupById(this.props.tbl_ui_gid);
        const expandedMode = getExpandedMode() === LO_EXPANDED.tables.view;
        this.setState({expandedMode,...tblUiGroup});
    }

    render() {
        const {expandedMode, tables, layout} = this.state;
        const {tbl_ui_gid} = this.props;

        if (isEmpty(tables)) return false;
        else {
            return expandedMode ? <ExpandedView {...{tbl_ui_gid, tables, layout}} /> : <TabsView {...{tbl_ui_gid, tables}} />;
        }
    }
}

TablesContainer.propTypes = {
    tbl_ui_gid: PropTypes.string,
    expandedMode: PropTypes.bool
};
TablesContainer.defaultProps = {
    tbl_ui_gid: TblUtil.uniqueTblUiGid(),
    expandedMode: false
};



function ExpandedView(props) {
    return (
        <div style={{ display: 'flex', flex: 'auto', flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}><CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_EXPANDED.none)}/></div>
            <TabsView expandedMode={true} {...props} />
        </div>
    );

}
ExpandedView.propTypes = TabsView.propTypes;
ExpandedView.defaultProps = TabsView.defaultProps;


function TabsView(props) {
    const {tables, tbl_ui_gid, expandedMode} = props;
    const onTabSelect = (idx) => {
        const tbl_id = get(tables, [Object.keys(tables)[idx], 'tbl_id']);
        tbl_id && dispatchActiveTableChanged(tbl_id)
    };

    return (
        <Tabs defaultSelected={0} onTabSelect={onTabSelect}>
            {tablesAsTab(tables, tbl_ui_gid, expandedMode)}
        </Tabs>
    );
}
TabsView.propTypes = {
    tbl_ui_gid: PropTypes.string,
    expandedMode: PropTypes.bool,
    mode: PropTypes.oneOf(['tabs', 'grid'])
};

TabsView.defaultProps = {
    tbl_gid: TblUtil.uniqueTblUiGid(),
    expandedMode: false,
    mode: 'tabs'
};


function tablesAsTab(tables, tbl_ui_gid, expandedMode) {

    return tables &&
        Object.keys(tables).map( (key) => {
            var {tbl_id, removable, tbl_ui_id} = tables[key];
            tbl_ui_id = tbl_ui_id || TblUtil.uniqueTblUiId();
            const onTabRemove = (tbl_ui_id) => {
                dispatchTableUiRemoved(tbl_ui_gid, tbl_ui_id);
                dispatchTableRemove(tbl_id);
            };

            return  (
                <Tab key={tbl_ui_id} name={tbl_ui_id} removable={removable} onTabRemove={onTabRemove}>
                    <TablePanel key={tbl_ui_id} {...{tbl_id, tbl_ui_id, expandedMode}} />
                </Tab>
            );
        } );
}

