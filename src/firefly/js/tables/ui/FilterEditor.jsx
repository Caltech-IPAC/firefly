/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, cloneDeep, get} from 'lodash';

import {BasicTableView} from './BasicTableView.jsx';
import {SelectInfo} from '../SelectInfo.js';
import {createInputCell} from './TableRenderer.js';
import {FILTER_CONDITION_TTIPS, FILTER_TTIPS, FilterInfo} from '../FilterInfo.js';
import {sortTableData, calcColumnWidths} from  '../TableUtil.js';
import {InputAreaField} from '../../ui/InputAreaField.jsx';
import {toBoolean} from '../../util/WebUtil.js';
import {NOT_CELL_DATA} from './TableRenderer.js';

const wrapperStyle = {display: 'block', flexGrow: 0};
const style = {display: 'block', width: '100%', resize: 'none', boxSizing: 'border-box', backgroundColor: 'white'};

export class FilterEditor extends PureComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {columns, selectable, onChange, sortInfo, filterInfo= ''} = this.props;
        if (isEmpty(columns)) return false;

        const {cols, data, selectInfoCls} = prepareOptionData(columns, sortInfo, filterInfo, selectable);
        const callbacks = makeCallbacks(onChange, columns, data, filterInfo);
        const renderers = makeRenderers(callbacks.onFilter);
        return (
            <div style={{height: '100%', display: 'flex', flexDirection: 'column'}}>
                <div style={{flexGrow: 1, position: 'relative'}}>
                    <div style={{position: 'absolute', top:0, bottom:5, left:0, right:0}}>
                        <BasicTableView
                            bgColor='beige'
                            columns={cols}
                            rowHeight={24}
                            selectable={selectable}
                            {...{data, selectInfoCls, sortInfo, callbacks, renderers}}
                        />
                    </div>
                </div>
                <InputAreaField
                    value={filterInfo}
                    label='Filters:'
                    validator={FilterInfo.validator.bind(null,columns)}
                    tooltip={FILTER_TTIPS}
                    inline={false}
                    rows={3}
                    onChange={callbacks.onAllFilter}
                    actOn={['blur', 'enter']}
                    wrapperStyle={wrapperStyle}
                    style={style}
                    showWarning={false}
                />
            </div>
        );
    }
}

FilterEditor.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.object),
    selectable: PropTypes.bool,
    sortInfo: PropTypes.string,
    filterInfo: PropTypes.string,
    onChange: PropTypes.func
};

FilterEditor.defaultProps = {
    selectable: true
};

function prepareOptionData(columns, sortInfo, filterInfo, selectable) {

    var cols = [
        {name: 'Column', fixed: true},
        {name: 'Filter'},
        {name: 'Units'},
        {name: '', visibility: 'hidden'},
        {name: 'Selected', visibility: 'hidden'}
    ];

    const filterInfoCls = FilterInfo.parse(filterInfo);
    columns = columns.filter((c) => c.visibility !== 'hidden');
    var data = columns.map( (v) => {
        const filter = toBoolean(v.filterable, true) ? filterInfoCls.getFilter(v.name) || '' : NOT_CELL_DATA;
        return [v.label||v.name||'', filter, v.units||'', v.desc||'', v.visibility !== 'hide'];
    } );
    sortTableData(data, cols, sortInfo);

    const widths = calcColumnWidths(cols, data);
    cols[0].prefWidth = Math.min(widths[0], 20);  // adjust width of column for optimum display.
    cols[1].prefWidth = Math.max(46 - widths[0] - widths[2] - widths[3] - (selectable? 3 : 0), 12);  // expand filter field to fill in empty space.
    cols[2].prefWidth = Math.min(widths[2], 12);
    if (widths[3]) {
        cols[3] = {name: 'Description', prefWidth: widths[3], visibility: 'show'};
    }

    var selectInfoCls = SelectInfo.newInstance({rowCount: data.length});
    selectInfoCls.data.rowCount = data.length;
    data.forEach( (v, idx) => {
        selectInfoCls.setRowSelect(idx, get(v, '4', true));
    } );
    var tableRowCount = data.length;

    return {cols, data, tableRowCount, selectInfoCls};
}

function makeCallbacks(onChange, columns, data, orgFilterInfo='') {
    var onSelectAll = (checked) => {
        const nColumns = columns.map((c) => {
            var col = cloneDeep(c);
            if (col.visibility !== 'hidden') {
                col.visibility = checked ? 'show' : 'hide';
            }
            return col;
        });
        onChange && onChange({columns: nColumns});
    };

    var onRowSelect = (checked, rowIdx) => {
        const selColName = get(data, [rowIdx, 0]);
        const nColumns = cloneDeep(columns);
        const selCol = nColumns.find((col) => col.name === selColName || col.label === selColName);
        selCol && (selCol.visibility = checked ? 'show' : 'hide');
        onChange && onChange({columns: nColumns});
    };

    var onSort = (sortInfoString) => {
        const sortInfo = sortInfoString;
        onChange && onChange({sortInfo});

    };

    const onAllFilter = (fieldVal) => {
        if (fieldVal.valid) {
            if (fieldVal.value !== orgFilterInfo) {
                onChange && onChange({filterInfo: fieldVal.value});
            }
        }
    };

    const onFilter = (fieldVal) => {
        if (fieldVal.valid) {
            const filterInfo = collectFilterInfo(data);
            if (filterInfo !== orgFilterInfo) {
                onChange && onChange({filterInfo});
            }
        }
    };

    return {onSelectAll, onRowSelect, onSort, onFilter, onAllFilter};
}

function collectFilterInfo(data) {
    const filterCls = FilterInfo.parse('');
    data.filter( (row) => row[1]).forEach( (row) => {
        filterCls.addFilter(row[0], row[1]);
    });
    return filterCls.serialize();
}

function makeRenderers(onFilter) {
    const style = {width: '100%', boxSizing: 'border-box'};
    const filterCellRenderer = createInputCell(
        FILTER_CONDITION_TTIPS,
        undefined,
        FilterInfo.conditionValidator,
        onFilter,
        style
    );
    return {Filter: {cellRenderer: filterCellRenderer}};
}
