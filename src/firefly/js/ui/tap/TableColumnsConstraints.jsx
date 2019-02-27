import React, {Fragment, useState, useEffect} from 'react';
import {cloneDeep, get, isEmpty,} from 'lodash';

import {calcColumnWidths, getCellValue, getColumn, getColumns, getColumnValues, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchTableFilter, dispatchTableAddLocal, TABLE_FETCH, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {ColumnConstraintsPanel, resetConstraints} from './ColumnConstraintsPanel.jsx';
import {FilterInfo} from '../../tables/FilterInfo.js';

const COLS_TO_DISPLAY = ['column_name','description','unit','datatype','ucd','utype','principal'];

export function TableColumnsConstraints({groupKey, fieldKey, columnsModel}) {
    const tbl_id = get(columnsModel, 'tbl_id');
    const [tableModel, setTableModel] = useState(getTblById(tbl_id));
    
    const filters = get(tableModel, 'request.filters', '');
    const [filterCount, setFilterCount] = useState(filters ? filters.split(';').length : 0);

    const filterInfoCls =  FilterInfo.parse(filters);
    const principal = filterInfoCls.getFilter('principal') === '= 1';
    const showPrincipalSelection = isPrincipalSet(tableModel);

    useEffect( () => {
        if (!columnsModel || columnsModel.error) {
            setTableModel(columnsModel);
        } else {
            setTableModel(reorganizeTableModel(columnsModel, COLS_TO_DISPLAY));

            return watchTableChanges(tbl_id,
                [TABLE_LOADED],
                (action) => {
                    const {tableModel, invokedBy} = action.payload;
                    // sync filter count with table model
                    if (get(tableModel, 'tbl_id') === tbl_id) {
                        if (invokedBy === TABLE_FETCH && isPrincipalSet(tableModel)) {
                            // make Principal the default
                            updatePrincipalFilter(tbl_id, true);
                        } else {
                            const filterInfo = get(tableModel, 'request.filters', '');
                            const filterInfoCls = FilterInfo.parse(filterInfo);
                            const isPrincipal = filterInfoCls.getFilter('principal') === '= 1';
                            // sync principal column filter with the principal state value
                            dispatchValueChange({
                                fieldKey: 'columnsType',
                                groupKey,
                                value: isPrincipal ? 'principal' : 'all',
                                valid: true
                            });
                            setFilterCount(filterInfo ? filterInfo.split(';').length : 0);
                        }
                    }
                },
                `ucd-${tbl_id}-filterCnt`); // watcher id for debugging
        }
    }, [columnsModel]);

    const {error} = tableModel || {};



    if (isEmpty(tableModel)) {
        return <div/>;
    }


    const resetButton = () => {
        return (
            <button style={{padding: '0 5px 0 5px', margin: showPrincipalSelection ? '0 0 0 10px' : '0'}}
                    title='Reset table below to the default'
                    onClick={ () => {
                        resetConstraints(groupKey, fieldKey);
                        const tblModel = reorganizeTableModel(columnsModel, COLS_TO_DISPLAY, true);
                        dispatchTableAddLocal(tblModel, {}, false);
                        setTableModel(tblModel);
                    }}>Reset
            </button>
        );
    };

    const columnsType =
        (<ListBoxInputField fieldKey={'columnsType'} inline={true}
                            wrapperStyle={{alignSelf: 'center'}}
                            initialState={{
                                tooltip: 'Select which columns should be displayed',
                                value: principal ? 'principal' : 'all'
                            }}
                            options={[
                                {label: 'Principal Only', value: 'principal'},
                                {label: 'All Columns', value: 'all'}
                            ]}
                            value={principal ? 'principal' : 'all'}
                            labelWidth={0}
                            label=''
                            multiple={false}
                            onChange = {(ev)=>updatePrincipalFilter(tbl_id, ev.target.value === 'principal')}

        />);

    return (
        <div style={{padding:'0 0 5px'}}>
            <div
                style={{display:'flex', flexDirection:'column',
                        margin:'0px 10px 5px 5px', padding:'0px 5px',
                        border:'1px solid #a3aeb9'}}>
                <div style={{display:'flex', flexDirection:'row', padding:'5px 5px 0', height: 20}}>
                    {!error && showPrincipalSelection && columnsType}
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
                <div>
                    <ColumnConstraintsPanel {...{tableModel, fieldKey}} />
                </div>
            </div>
        </div>
    );

}

/**
 * Update table filter to be in sync with principal value
 * @param tbl_id
 * @param principal
 */
function updatePrincipalFilter(tbl_id, principal) {
    const {request={}} = getTblById(tbl_id);
    const filterInfoCls = FilterInfo.parse(get(request,'filters'));
    let prevPrincipalFilter = filterInfoCls.getFilter('principal');
    if (!prevPrincipalFilter) prevPrincipalFilter = '';
    const newPrincipalFilter = principal ? '= 1' : '';
    if (prevPrincipalFilter !== newPrincipalFilter) {
        filterInfoCls.setFilter('principal', principal ? '= 1' : '');
        dispatchTableFilter({tbl_id, filters: filterInfoCls.serialize()});
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
        if (isPrincipalSet(tableModel)) {
            // default selected are the principal rows
            defaultSelected = getColumnValues(tableModel, 'principal').reduce((sels, v, i) => {
                if (parseInt(v) === 1) sels.push(i);
                return sels;
            }, []);
        }
        defaultSelected.forEach((idx)=>selectInfoCls.setRowSelect(idx, true));
    }

    modifiedTableModel = {tbl_id, totalRows: data.length, tableData: {columns, data}, selectInfo: selectInfoCls.data};

    return modifiedTableModel;
}