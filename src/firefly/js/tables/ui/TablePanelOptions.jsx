/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {set, isEmpty, get} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import {BasicTable} from './BasicTable.jsx';
import {SelectInfo} from '../SelectInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';


function prepareOptionData(columns) {

    var data = columns.map( (v, idx) => {
        return [v.name];
    } );
    var cols = [{name: 'Column', visibility: 'show', prefWidth: 20}];
    var selectInfoCls = SelectInfo.newInstance({});
    selectInfoCls.data.rowCount = data.length;
    columns.forEach( (v, idx) => {
        selectInfoCls.setRowSelect(idx, v.visibility === 'show');
    } );
    var tableRowCount = data.length;

    return {cols, data, tableRowCount, selectInfoCls };
}

export const TablePanelOptions = (props) => {
    const {columns, pageSize, showUnits, showFilters, onChange} = props;
    if (isEmpty(columns)) return false;

    var onSelectAll = (checked) => {
        const nColumns = columns.slice();
        nColumns.forEach((v) => {
            v.visibility = checked ? 'show' : 'hide';
        });
        onChange && onChange({columns: nColumns});
    };

    var onRowSelect = (checked, rowIdx) => {
        const nColumns = columns.slice();
        nColumns[rowIdx].visibility = checked ? 'show' : 'hide';
        onChange && onChange({columns: nColumns});
    };

    var onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };

    var onPropChanged = (v, prop) => {
        onChange && onChange({[prop]: v});
    };

    var onReset = () => {
        onChange && onChange({pageSize: 50, showUnits: false, showFilters: false, columns: []});
    };

    const {cols, data, selectInfoCls} = prepareOptionData(columns);
    return (
        <div className='TablePanelOptions'>
            <div>
                <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '2px'}}>
                    <div style={{float: 'left'}}>
                        <InputField
                            validator = {intValidator(1,10000)}
                            tooltip = {'Set page size'}
                            label = {'Page Size:'}
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
            <BasicTable
                columns={cols}
                data={data}
                height={'calc(100% - 42px)'}
                selectable={true}
                selectInfoCls={selectInfoCls}
                tableStore={{onSelectAll, onRowSelect}}
            />
        </div>
    );
};

TablePanelOptions.propTypes = {
    columns: React.PropTypes.arrayOf(React.PropTypes.object),
    pageSize: React.PropTypes.number,
    showUnits: React.PropTypes.bool,
    showFilters: React.PropTypes.bool,
    onChange: React.PropTypes.func
};


