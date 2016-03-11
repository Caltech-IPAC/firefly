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
import {dispatchTableRemoved} from '../TablesUiCntlr.js';


import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_EXPANDED, dispatchSetLayoutMode, getExpandedMode} from '../../core/LayoutCntlr.js';
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
        const {expandedMode, ...others} = this.state;
        const {tbl_ui_gid} = this.props;

        return expandedMode ? <ExpandedView tbl_ui_gid={tbl_ui_gid} {...others} /> : <StandardView tbl_ui_gid={tbl_ui_gid} {...others} />;

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
            <StandardView {...props} />
        </div>
    );

}
ExpandedView.propTypes = StandardView.propTypes;
ExpandedView.defaultProps = StandardView.defaultProps;


function StandardView(props) {
    const {tables, tbl_ui_gid} = props;

    return (
        <Tabs defaultSelected={0}>
            {layoutTables(tables, tbl_ui_gid)}
        </Tabs>
    );
}
StandardView.propTypes = {
    tbl_ui_gid: PropTypes.string,
    expandedMode: PropTypes.bool,
    mode: PropTypes.oneOf(['tabs', 'grid'])
};

StandardView.defaultProps = {
    tbl_gid: TblUtil.uniqueTblUiGid(),
    expandedMode: false,
    mode: 'tabs'
};


function layoutTables(tables, tbl_ui_gid) {
    const onTabRemove = (tbl_ui_id) => {
        dispatchTableRemoved(tbl_ui_gid, tbl_ui_id);
    };

    return tables &&
        Object.keys(tables).map( (key) => {
            var {tbl_id, removable, tbl_ui_id, expandedMode} = tables[key];
            tbl_ui_id = tbl_ui_id || TblUtil.uniqueTblUiId();

            return  (
                <Tab key={tbl_ui_id} name={tbl_ui_id} removable={removable} onTabRemove={onTabRemove}>
                    <TablePanel key={tbl_ui_id} {...{tbl_id, tbl_ui_id, expandedMode}} />
                </Tab>
            );
        } );
}

