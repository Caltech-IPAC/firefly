import React, {useEffect} from 'react';
import {get, isEmpty} from 'lodash';
import {createInputCell} from '../../tables/ui/TableRenderer.js';
import {FILTER_TTIPS, FilterInfo} from '../../tables/FilterInfo.js';

import {getColumnIdx, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {fieldGroupConnector} from '../FieldGroupConnector.jsx';
import * as TblCntlr from '../../tables/TablesCntlr.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';

/**
 * @summary component to display the data restrictions into a tabular format
 * @param {Object} props
 * @returns {Object} constraint table
 */
export function ColConstraintsPanel({tableModel, onTableChanged, style}) {
    useEffect( () => {
        // when table changes, we need to sync the new table with the field group
        // currently doing it in TableColumnsConstraints
        onTableChanged && onTableChanged();

        return watchTableChanges(tableModel.tbl_id,
                [TblCntlr.TABLE_SELECT, TblCntlr.TABLE_LOADED],
                (action) => onTableChanged && onTableChanged(),
                `ucd-${tableModel.tbl_id}-select`); // watcher id for debugging
    }, [tableModel]);


    const newInputCell = createInputCell(FILTER_TTIPS,
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
                                key={tableModel.tbl_id}
                                tbl_ui_id={tbl_ui_id}
                                tableModel={tableModel}
                                showToolbar={false}
                                showFilters={true}
                                selectable={true}
                                showOptionButton={true}
                                border= {false}
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

export const ColumnConstraintsPanel = fieldGroupConnector(ColConstraintsPanel, getProps, null, null);

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onTableChanged: () => handleOnTableChanged(params, fireValueChange)
        });
}

/**
 * @summary update the state based on the change of column search constraints or column selection
 * @param {Object} params
 * @param {Function} fireValueChange
 */
function handleOnTableChanged(params, fireValueChange) {
    const {tbl_id} = params.tableModel;
    const tbl = getTblById(tbl_id);

    if (!tbl) return;

    const tbl_data = tbl.tableData.data;
    const sel_info  =  tbl.selectInfo;
    const filters = {};
    let sqlTxt = '';
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
                    errors += (errors.length > 0 ? ` ${value}` : `Invalid constraints: ${value}`);
                } else if (v) {
                    const oneSql = `${colName} ${v}`;
                    sqlTxt += (sqlTxt.length > 0 ? ` AND ${oneSql}` : oneSql);
                }
            });
        }
    });

    // collect the names of all selected columns
    let allSelected = true;

    let selCols = tbl_data.reduce((prev, d, idx) => {
            if ((sel_info.selectAll && (!sel_info.exceptions.has(idx))) ||
                (!sel_info.selectAll && (sel_info.exceptions.has(idx)))) {
                prev += d[0] + ',';
            } else {
                allSelected = false;
            }
            return prev;
        }, '');

    if (isEmpty(selCols)) {
        selCols = '';
    }

    // the value of this input field is a string
    const val = getFieldVal(params.groupKey, params.fieldKey); //get(params, 'value', '');
    // ++++++++++++++++++++++++++++
    // ++++++ WARNING!!!!!+++++++++
    // ++++++++++++++++++++++++++++
    // YOU ONLY CARE ABOUT CHANGES ON SELECTED COLUMNS AND CONSTRAINTS  INPUT FIELD
    //
    // CHECK IF FIREVALUE IS REQUIRED HERE EVERYTIME IF PREVIOUS VALUE HAS CHANGED
    // BECAUSE THIS WILL GET CALLED TWICE (TABLE AND FIELDGROUP) AND
    // CAN BECOME AN ENDLESS LOOP IF IT FIRES AGAIN WITHOUT CHECKING
    // BASICALLY IMPLEMENTING HERE THE 'ONCHANGE' OF THE TABLEVIEW USED IN A FORM

    if (val.constraints !== sqlTxt || val.selcols !== selCols || errors !== val.errorConstraints) {
        mergeConstraintsIntoOrig(tbl);
        fireValueChange({
            value:  {constraints: sqlTxt, selcols: selCols, errorConstraints: errors, filters}
        });
    }
}

/**
 * @summary reset table constraints state
 * @param {string} groupKey
 * @param {string} fieldKey
 */
export function resetConstraints(groupKey, fieldKey) {
    const value = {constraints: '', selcols: '', filters: {}, errorConstraints:''};
    dispatchValueChange({value, fieldKey, groupKey, valid: true});
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
    // if ORIG_IDX column is present in the table, its row indexes differ from the original table
    const idxCol = getColumnIdx(tbl, 'ORIG_IDX');
    const origTblData = get(origTableModel, 'tableData.data');
    tblData.forEach((row, i) => {
        const origIdx = idxCol < 0 ? i : parseInt(row[idxCol]);
        //set column constraint
        origTblData[origIdx][1] = tblData[i][1];
    });
}