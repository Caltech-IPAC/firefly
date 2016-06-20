/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, cloneDeep, get} from 'lodash';

import {BasicTableView} from './BasicTableView.jsx';
import {SelectInfo} from '../SelectInfo.js';
import {createInputCell} from './TableRenderer.js';
import {FILTER_CONDITION_TTIPS, FILTER_TTIPS, FilterInfo} from '../FilterInfo.js';
import {sortTableData} from  '../TableUtil.js';
import {InputAreaField} from '../../ui/InputAreaField.jsx';
// import {deepDiff} from '../../util/WebUtil.js';

const wrapperStyle = {display: 'block', flexGrow: 0};
const style = {display: 'block', width: '100%', resize: 'none', boxSizing: 'border-box', backgroundColor: 'white'};

export class FilterEditor extends React.Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         'FilterEditor');
    // }

    render() {
        const {columns, origColumns, onChange, sortInfo, filterInfo= ''} = this.props;
        if (isEmpty(columns)) return false;

        const {cols, data, selectInfoCls} = prepareOptionData(columns, sortInfo, filterInfo);
        const callbacks = makeCallbacks(onChange, columns, origColumns, data, filterInfo);
        const renderers = makeRenderers(callbacks.onFilter);
        return (
            <div style={{height: '100%', display: 'flex', flexDirection: 'column'}}>
                <div style={{flexGrow: 1, position: 'relative'}}>
                    <div style={{position: 'absolute', top:0, bottom:5, left:0, right:0}}>
                        <BasicTableView
                            bgColor='beige'
                            columns={cols}
                            rowHeight={24}
                            selectable={true}
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
};

FilterEditor.propTypes = {
    columns: PropTypes.arrayOf(React.PropTypes.object),
    origColumns: PropTypes.arrayOf(PropTypes.object),
    sortInfo: PropTypes.string,
    filterInfo: PropTypes.string,
    onChange: PropTypes.func
};

function prepareOptionData(columns, sortInfo, filterInfo) {

    var cols = [
        {name: 'Column', visibility: 'show', prefWidth: 12, fixed: true},
        {name: 'Filter', visibility: 'show', prefWidth: 12},
        {name: 'Units', visibility: 'show', prefWidth: 6},
        {name: 'Description', visibility: 'show', prefWidth: 60},
        {name: 'Selected', visibility: 'hidden'},
    ];

    const filterInfoCls = FilterInfo.parse(filterInfo);
    columns = columns.filter((c) => c.visibility != 'hidden');
    var data = columns.map( (v) => {
        const filter = filterInfoCls.getFilter(v.name) || '';
        return [v.name||'', filter, v.units||'', v.desc||'', v.visibility !== 'hide'];
    } );
    sortTableData(data, cols, sortInfo);

    var selectInfoCls = SelectInfo.newInstance({rowCount: data.length});
    selectInfoCls.data.rowCount = data.length;
    data.forEach( (v, idx) => {
        selectInfoCls.setRowSelect(idx, get(v, '4', true));
    } );
    var tableRowCount = data.length;

    return {cols, data, tableRowCount, selectInfoCls};
}

function makeCallbacks(onChange, columns, origColumns, data, orgFilterInfo='') {
    var onSelectAll = (checked) => {
        const nColumns = cloneDeep(columns).filter((c) => c.visibility != 'hidden');
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