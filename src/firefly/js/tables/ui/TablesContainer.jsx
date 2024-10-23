/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, get, union} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import {TablePanel} from './TablePanel.jsx';
import {Tab, TabPanel} from '../../ui/panel/TabPanel.jsx';
import {dispatchTableRemove, dispatchActiveTableChanged} from '../TablesCntlr.js';
import {hashCode} from '../../util/WebUtil.js';


import {LO_VIEW, LO_MODE, dispatchSetLayoutMode, getExpandedMode} from '../../core/LayoutCntlr.js';
import {CloseButton} from '../../ui/CloseButton.jsx';

import {Logger} from '../../util/Logger.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getTableUiById, getTableUiByTblId, getTblIdsByGroup} from '../TableUtil.js';

const logger = Logger('Tables').tag('TablesContainer');

export function TablesContainer(props) {
    const {mode='both', closeable=true, tableOptions, style, expandedMode:xMode=false} = props;
    const expandedMode = useStoreConnector(() => xMode || getExpandedMode() === LO_VIEW.tables);
    const tbl_group = expandedMode && mode !== 'standard' ? TblUtil.getTblExpandedInfo().tbl_group : props.tbl_group;

    const tables = useStoreConnector(() => TblUtil.getTableGroup(tbl_group)?.tables);
    const active = useStoreConnector(() => TblUtil.getTableGroup(tbl_group)?.active);
    useStoreConnector((lastTitles) => {// force a rerender if any title ui changes
        const titles= getTblIdsByGroup().map( (tbl_id) => getTableUiByTblId(tbl_id)?.title);
        if (!lastTitles) return titles;
        return (union(titles,lastTitles).length === titles?.length) ? lastTitles : titles;
    });

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

    const onTabSelect = (tbl_id) => {
        tbl_id && dispatchActiveTableChanged(tbl_id, tbl_group);
    };
    const keys = Object.keys(tables);
    if (keys.length === 1) {
        return <SingleTable table={get(tables, [keys[0]])} expandedMode={expandedMode} tableOptions={tableOptions}/>;
    } else {
        const uid = hashCode(keys.join());
        return (
            <TabPanel key={uid} sx={style} value={active}
                      onTabSelect={onTabSelect} resizable={true} showOpenTabs={true}>
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
            const {tbl_id, removable, tbl_ui_id, options:inOptions={}, title:titleStr} = tables[key];
            const {title:titleUI, color} = getTableUiById(tbl_ui_id) || {};
            const options = {...inOptions, tableOptions};
            const onTabRemove = () => {
                dispatchTableRemove(tbl_id);
            };
            return  (
                <Tab key={tbl_id} id={tbl_id} label={titleUI} name={titleStr} colorSwatch={color} removable={removable} onTabRemove={onTabRemove}>
                    <TablePanel key={tbl_id}
                                slotProps={{ toolbar:{variant:'plain'}, root:{variant: 'plain'} }}
                                {...{tbl_id, tbl_ui_id, ...options, expandedMode, showTitle: false}} />
                </Tab>
            );
        } );
}

