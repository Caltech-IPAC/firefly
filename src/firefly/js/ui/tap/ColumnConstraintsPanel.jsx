import React from 'react';
import {get, isEmpty, uniqueId} from 'lodash';
import {createInputCell} from '../../tables/ui/TableRenderer.js';
import {FILTER_CONDITION_TTIPS, FilterInfo} from '../../tables/FilterInfo.js';

import {getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {maybeQuote} from 'firefly/ui/tap/TapUtil.js';

/**
 * @summary component to display column constraints and selections using a table
 * Constraints table is represented by a client table.
 * Its original table model holds all constraints and selections.
 * @param {Object} props
 * @returns {Object} constraint table
 */
export function ColumnConstraintsPanel({tableModel, style}) {
    const onTableChanged = () => {
        const tbl = getTblById(tableModel.tbl_id);
        if (!tbl) return;
        mergeConstraintsIntoOrig(tbl);
    };

    const newInputCell = createInputCell(FILTER_CONDITION_TTIPS,
                15,
                FilterInfo.conditionValidatorNoAutoCorrect,
                onTableChanged, {width: '100%', boxSizing: 'border-box'});

    const tbl_ui_id = tableModel.tbl_id + '-ui';

    return (
        <div style={style}>
            <div style={{ position: 'relative', width: '100%', height: '100%'}}>
                <div className='TablePanel'>
                    <div className={'TablePanel__wrapper--border'}>
                        <div className='TablePanel__table' style={{top: 0}}>
                            <TablePanel
                                key={uniqueId()}
                                tbl_ui_id={tbl_ui_id}
                                tableModel={tableModel}
                                showToolbar={false}
                                showFilters={true}
                                selectable={true}
                                showOptionButton={true}
                                border= {false}
                                rowHeight={24}
                                renderers={{
                                    //name: { cellRenderer: createLinkCell({hrefColIdx: totalCol})},
                                    constraints: { cellRenderer: newInputCell}
                                }}
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}


export function getTableConstraints(tbl_id) {
    const tbl = get(getTblById(tbl_id), 'origTableModel');
    if (!tbl) {
        return;
    }
    const tbl_data = tbl.tableData.data;
    const sel_info  =  tbl.selectInfo;
    const filters = {};
    let whereFragment = '';
    let errors = '';

    if (!tbl_data) return;

    tbl_data.forEach((d) => {
        const filterStrings = d[1].trim();
        const colName = d[0];

        if (filterStrings && filterStrings.length > 0) {
            filters[colName] = filterStrings;
            const parts = filterStrings && filterStrings.split(';');

            parts.forEach((v) => {
                const {valid, value} = FilterInfo.conditionValidatorNoAutoCorrect(v);

                if (!valid) {
                    errors += (errors.length > 0 ? `, "${value}"` : `Invalid constraints: "${value}"`);
                } else if (v) {
                    const oneConstraint = `${maybeQuote(colName)} ${v}`;
                    whereFragment += (whereFragment.length > 0 ? ` AND ${oneConstraint}` : oneConstraint);
                }
            });
        }
    });

    // collect the names of all selected columns
    let allSelected = true;
    let selcolsArray = [];
    let selcolsFragment = tbl_data.reduce((prev, d, idx) => {
            if ((sel_info.selectAll && (!sel_info.exceptions.has(idx))) ||
                (!sel_info.selectAll && (sel_info.exceptions.has(idx)))) {
                prev += maybeQuote(d[0]) + ',';
                selcolsArray.push(d[0]);
            } else {
                allSelected = false;
            }
            return prev;
        }, '');

    if (isEmpty(selcolsFragment) || allSelected) {
        selcolsFragment = '';
    }

    return {whereFragment, selcolsFragment, errors, filters, selcolsArray};
}

/**
 * Merge constraints into original table model to preserve them through sorts and filters
 * @param tbl client side table model
 */
function mergeConstraintsIntoOrig(tbl) {
    const {origTableModel} = tbl;
    const tblData = get(tbl, 'tableData.data');

    if (!origTableModel || !tblData) {
        return;
    }
    // if ROW_IDX column is present in the table, its row indexes differ from the original table
    const idxCol = getColumnIdx(tbl, 'ROW_IDX');
    const origTblData = get(origTableModel, 'tableData.data');
    tblData.forEach((row, i) => {
        const origIdx = idxCol < 0 ? i : parseInt(row[idxCol]);
        //set column constraint
        origTblData[origIdx][1] = tblData[i][1];
    });
}