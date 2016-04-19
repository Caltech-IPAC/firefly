/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isEmpty, cloneDeep, get} from 'lodash';

import {BasicTableView} from './BasicTableView.jsx';
import {SelectInfo} from '../SelectInfo.js';
import {SortInfo, SORT_ASC} from '../SortInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';


export const TablePanelOptions = (props) => {
    const {columns, origColumns, pageSize, showUnits, showFilters, onChange, colSortDir} = props;
    if (isEmpty(columns)) return false;

    const {cols, data, sortInfo, selectInfoCls} = prepareOptionData(columns, colSortDir);
    const callbacks = makeCallbacks(onChange, columns, origColumns, data);
    const {onPageSize, onPropChanged, onReset, ...tableCallbacks} = callbacks;
    return (
        <div className='TablePanelOptions'>
            <div style={{flexGrow: 0}}>
                <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '2px'}}>
                    <div style={{float: 'left'}}>
                        <InputField
                            validator = {intValidator(1,10000)}
                            tooltip = 'Set page size'
                            label = 'Page Size:'
                            size = {3}
                            value = {pageSize+''}
                            onChange = {onPageSize}
                            actOn={['blur','enter']}
                        />
                    </div>
                    <span style={{float: 'right'}}>
                        <button className='TablePanelOptions__button' onClick={onReset} title='Reset all options to defauls'>Reset</button>
                    </span>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '2px'}}>
                    <span>Show Units: <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showUnits')} checked={showUnits}/></span>
                    <span>Show Filters: <input type='checkbox' onChange={(e) => onPropChanged(e.target.checked, 'showFilters')} checked={showFilters}/></span>
                </div>
            </div>
            <div style={{height: 'calc(100% - 40px)'}}>
                <BasicTableView 
                    columns={cols}
                    data={data}
                    height='calc(100% - 42px)'
                    selectable={true}
                    selectInfoCls={selectInfoCls}
                    sortInfo={sortInfo}
                    callbacks={tableCallbacks}
                />
            </div>
        </div>
    );
};

TablePanelOptions.propTypes = {
    columns: React.PropTypes.arrayOf(React.PropTypes.object),
    origColumns: React.PropTypes.arrayOf(React.PropTypes.object),
    colSortDir: React.PropTypes.oneOf(['ASC', 'DESC', '']),
    pageSize: React.PropTypes.number,
    showUnits: React.PropTypes.bool,
    showFilters: React.PropTypes.bool,
    onChange: React.PropTypes.func
};

function prepareOptionData(columns, colSortDir) {

    if (colSortDir) {
        // sort the columns according to colSortDir
        const multiplier = colSortDir === SORT_ASC ? 1 : -1;
        const comparator = (r1, r2) => {
            const [s1, s2] = [r1.name, r2.name];
            return multiplier * (s1 > s2 ? 1 : -1);
        };
        columns = columns.slice();      // shallow clone
        columns.sort(comparator);
    }

    var data = columns.map( (v, idx) => {
        return [v.name];
    } );

    var cols = [{name: 'Column', visibility: 'show', prefWidth: 20}];
    const sortInfo = SortInfo.newInstance(colSortDir, 'Column').serialize();

    var selectInfoCls = SelectInfo.newInstance({});
    selectInfoCls.data.rowCount = data.length;
    columns.forEach( (v, idx) => {
        selectInfoCls.setRowSelect(idx, v.visibility === 'show');
    } );
    var tableRowCount = data.length;

    return {cols, data, tableRowCount, selectInfoCls, sortInfo};
}

function makeCallbacks(onChange, columns, origColumns, data) {
    var onSelectAll = (checked) => {
        const nColumns = cloneDeep(columns);
        nColumns.forEach((v) => {
            v.visibility = checked ? 'show' : 'hide';
        });
        onChange && onChange({columns: nColumns});
    };

    var onRowSelect = (checked, rowIdx) => {
        const selColName = get(data, [rowIdx, 0]);
        const nColumns = cloneDeep(columns);
        const selCol = nColumns.find((col) => col.name === selColName);
        selCol && (selCol.visibility = checked ? 'show' : 'hide');
        onChange && onChange({columns: nColumns});
    };

    var onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    var onSort = (sortInfoString) => {
        const colSortDir = SortInfo.parse(sortInfoString).direction;
        onChange && onChange({colSortDir});
    };

    var onPropChanged = (v, prop) => {
        onChange && onChange({[prop]: v});
    };

    var onReset = () => {
        onChange && onChange({pageSize: 50, showUnits: false, showFilters: false, columns: cloneDeep(origColumns)});
    };

    return {onSelectAll, onRowSelect, onPageSize, onSort, onPropChanged, onReset};
}
