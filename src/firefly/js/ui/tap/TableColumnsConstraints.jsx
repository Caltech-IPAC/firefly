import React, {Fragment, useState, useEffect} from 'react';
import {cloneDeep, get, isEmpty,} from 'lodash';

import {calcColumnWidths, getCellValue, getColumn, getColumns, getColumnValues, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchTableFilter, dispatchTableAddLocal, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {ColumnConstraintsPanel, resetConstraints} from './ColumnConstraintsPanel.jsx';

const COLS_TO_DISPLAY = ['column_name','description','unit','datatype','ucd','utype','principal'];


export function TableColumnsConstraints({fieldKey, columnsModel}) {

    const tbl_id = get(columnsModel, 'tbl_id');
    const [tableModel, setTableModel] = useState(getTblById(tbl_id));

    useEffect( () => tableEffect(columnsModel, setTableModel, tbl_id), [columnsModel]);

    if (isEmpty(tableModel)) {
        return <div/>;
    } else {
        return <ColumnConstraintsPanel style={{height: '100%'}} {...{tableModel, fieldKey}} />;
    }

}

export function TableColumnsConstraintsToolbar({groupKey, fieldKey, columnsModel}) {
    const tbl_id = get(columnsModel, 'tbl_id');
    const [tableModel, setTableModel] = useState(getTblById(tbl_id));
    const filters = get(tableModel, 'request.filters', '');
    const [filterCount, setFilterCount] = useState(filters ? filters.split(';').length : 0);

    useEffect( () => tableEffect(columnsModel, setTableModel, tbl_id, setFilterCount), [columnsModel]);

    const {error} = tableModel || {};

    if (isEmpty(tableModel)) {
        return <div/>;
    }

    const resetButton = () => {
        return (
            <button style={{padding: '0 5px 0 5px'}}
                    title='Reset table below to the default'
                    onClick={ () => {
                        resetConstraints(groupKey, fieldKey);
                        const tblModel = reorganizeTableModel(columnsModel, COLS_TO_DISPLAY, true);
                        dispatchTableAddLocal(tblModel, {}, false);
                    }}>Reset
            </button>
        );
    };

    return (
        <div style={{display:'inline-flex', padding:'0 0 2px', height: 20}}>
            {!error && resetButton()}
            {!error && filterCount > 0 &&
                <Fragment>
                    <button style={{padding: '0 5px 0 5px', margin: '0 2px 0 10px'}}
                           onClick={() => dispatchTableFilter({tbl_id: tableModel.tbl_id, filters: ''})}>Remove column filters
                    </button>
                    <span style={{color: 'blue', alignSelf: 'center'}}>{`(${filterCount} filter${filterCount>1?'s':''})`}</span>
                </Fragment>
            }
        </div>
    );
}

function tableEffect(columnsModel, setTableModel, tbl_id, setFilterCount) {

    if (!columnsModel || columnsModel.error) {
        setTableModel(columnsModel);
    } else {
        setTableModel(reorganizeTableModel(columnsModel, COLS_TO_DISPLAY));

        if (setFilterCount) {
            return watchTableChanges(tbl_id,
                [TABLE_LOADED],
                (action) => {
                    const {tableModel} = action.payload;
                    // sync filter count with table model
                    if (get(tableModel, 'tbl_id') === tbl_id) {
                        const filterInfo = get(tableModel, 'request.filters', '');
                        setFilterCount && setFilterCount(filterInfo ? filterInfo.split(';').length : 0);
                    }
                }, `ucd-${tbl_id}-filterCnt`); // watcher id for debugging
        }
    }
}


/**
 * Returns true, if table model has 'principal' column and at least 3 values in it are not 0
 * @param tableModel
 * @returns {boolean}
 */
function isPrincipalSet(tableModel) {
    // 3 or more principle columns (coordinates and value) might be useful
    return getColumnValues(tableModel, 'principal').filter((v)=>v>0).length >= 3;
}

/**
 * Returns new table model with only the columns we are interested in
 * @param tableModel
 * @param columnNames - columns to show first
 * @param reset - if true, recreate table model
 */
function reorganizeTableModel(tableModel, columnNames, reset) {

    const tbl_id = get(tableModel,'tbl_id');
    let modifiedTableModel = getTblById(tbl_id);
    if (modifiedTableModel && !reset) return modifiedTableModel;

    // keep only the columns that are present in tableModel
    const colNames = columnNames.filter((c) => getColumn(tableModel, c));

    // make sure all original table columns are included
    colNames.push(...getColumns(tableModel).map((c)=>c.name).filter((name)=>!colNames.includes(name)));

    const columns = colNames.map((name) => cloneDeep(getColumn(tableModel, name)));

    const data = [];
    const origData = get(tableModel, 'tableData.data', []);
    origData.forEach((r, rIdx) => {
        data.push(colNames.reduce((rval, c) => {
            rval.push(getCellValue(tableModel, rIdx, c));
            return rval;
        }, []));
    });

    // set column widths
    const widths = calcColumnWidths(columns, data);
    columns.forEach((col, idx) => {
        col.prefWidth = Math.min(widths[idx]+2, 55);
    });

    // add constraints column
    const constraintsColIdx = 1;
    const constraintsCol = {name: 'constraints', idx: constraintsColIdx, type: 'char', width: 10};
    columns.splice(constraintsColIdx, 0, cloneDeep(constraintsCol));
    data.map((e) => {
        e.splice(constraintsColIdx, 0, '');
    });

    // set selections
    const selectInfoCls = SelectInfo.newInstance({rowCount: data.length});
    if (selectInfoCls.getSelectedCount() === 0) {
        let defaultSelected = [];
        // default selected are the principal rows
        if (isPrincipalSet(tableModel)) {
            defaultSelected = getColumnValues(tableModel, 'principal').reduce((sels, v, i) => {
                if (parseInt(v) === 1) sels.push(i);
                return sels;
            }, []);
        }
        defaultSelected.forEach((idx)=>selectInfoCls.setRowSelect(idx, true));
    }

    modifiedTableModel = {tbl_id, totalRows: data.length, tableData: {columns, data},
        selectInfo: selectInfoCls.data, request: {tbl_id}};

    return modifiedTableModel;
}