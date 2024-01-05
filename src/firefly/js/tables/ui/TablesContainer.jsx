/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, get} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import {TablePanel} from './TablePanel.jsx';
import {Tab, TabPanel} from '../../ui/panel/TabPanel.jsx';
import {dispatchTableRemove, dispatchActiveTableChanged} from '../TablesCntlr.js';
import {hashCode} from '../../util/WebUtil.js';


import {LO_VIEW, LO_MODE, dispatchSetLayoutMode, getExpandedMode} from '../../core/LayoutCntlr.js';
import {CloseButton} from '../../ui/CloseButton.jsx';

import {Logger} from '../../util/Logger.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';

const logger = Logger('Tables').tag('TablesContainer');


export function TablesContainer(props) {
    const {mode, closeable, tableOptions, style, expandedMode:xMode} = props;
    let {tbl_group} = props;

    const tables = useStoreConnector(() => TblUtil.getTableGroup(tbl_group)?.tables);
    const active = useStoreConnector(() => TblUtil.getTableGroup(tbl_group)?.active);
    const expandedMode = useStoreConnector(() => xMode || getExpandedMode() === LO_VIEW.tables);

    useEffect(() => {
        if (expandedMode && mode !== 'standard') {
            tbl_group = TblUtil.getTblExpandedInfo().tbl_group;
        }
    }, [expandedMode, mode]);

    logger.debug('render... tbl_group: ' + tbl_group);

    if (expandedMode) {
        return <ExpandedView {...{active, tables, tableOptions, expandedMode, closeable, tbl_group}} />;
    } else {
        return isEmpty(tables) ? <div/> : <StandardView {...{active, tables, tableOptions, expandedMode, tbl_group, style}} />;
    }
}

TablesContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    tbl_group: PropTypes.string,
    style: PropTypes.object,
    tableOptions: PropTypes.object,
    mode: PropTypes.oneOf(['expanded', 'standard', 'both'])
};
TablesContainer.defaultProps = {
    expandedMode: false,
    closeable: true,
    mode: 'both'
};



function ExpandedView(props) {
    const {tables, closeable} = props;
    return (
        <div style={{ display: 'flex', height: '100%', flexGrow: 1, flexDirection: 'column', overflow: 'hidden'}}>
            <div style={{marginBottom: 3}}>
                {closeable && <CloseButton style={{paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
            {!isEmpty(tables) &&
                <div style={{position: 'relative', flexGrow: 1}}>
                    <div style={{position: 'absolute', top:0, right:0, bottom:0, left:0}}>
                         <StandardView expandedMode={true} {...props} />
                    </div>
                </div>
            }
        </div>
    );

}


function StandardView(props) {
    const {tables, tableOptions, expandedMode, active, tbl_group, style={}} = props;

    var activeIdx = Object.keys(tables).findIndex( (tbl_ui_id) => get(tables,[tbl_ui_id,'tbl_id']) === active);
    activeIdx = activeIdx === -1 ? 0 : activeIdx;
    const onTabSelect = (idx) => {
        const tbl_id = get(tables, [Object.keys(tables)[idx], 'tbl_id']);
        tbl_id && dispatchActiveTableChanged(tbl_id, tbl_group);
    };
    const keys = Object.keys(tables);
    if (keys.length === 1) {
        return <SingleTable table={get(tables, [keys[0]])} expandedMode={expandedMode} tableOptions={tableOptions}/>;
    } else {
        const uid = hashCode(keys.join());
        return (
            <TabPanel key={uid} sx={style} value={activeIdx}
                      slotProps={{ tabPanel:{sx:{p:0}} }}
                      onTabSelect={onTabSelect} resizable={true} showOpenTabs={true} tabId={'TableContainers-' + (tbl_group||'main')}>
                {tablesAsTab(tables, tableOptions, expandedMode)}
            </TabPanel>
        );
    }
}

function SingleTable({table, tableOptions, expandedMode}) {
    var {tbl_id, title, removable, tbl_ui_id, options={}} = table;

    options = Object.assign({}, options, tableOptions);

    return  (
        <TablePanel key={tbl_id} {...{title, removable, tbl_id, tbl_ui_id, ...options, expandedMode}} />
    );
}

function tablesAsTab(tables, tableOptions, expandedMode) {

    return tables &&
        Object.keys(tables).map( (key) => {
            var {tbl_id, title, removable, tbl_ui_id, options={}} = tables[key];
            options = Object.assign({}, options, tableOptions);
            const onTabRemove = () => {
                dispatchTableRemove(tbl_id);
            };
            return  (
                <Tab key={tbl_ui_id} label={title} removable={removable} onTabRemove={onTabRemove}>
                    <TablePanel key={tbl_id}
                                slotProps={{ toolbar:{variant:'plain'}, tablePanel:{variant: 'plain'} }}
                                {...{tbl_id, tbl_ui_id, ...options, expandedMode, showTitle: false}} />
                </Tab>
            );
        } );
}

