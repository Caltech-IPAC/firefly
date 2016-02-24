/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import FixedDataTable from 'fixed-data-table';
import Resizable from 'react-component-resizable';
import {debounce, get, isEmpty} from 'lodash';

import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo, FILTER_TTIPS} from '../FilterInfo.js';
import {InputField} from '../../ui/InputField.jsx';
import {SORT_ASC, SORT_DESC, UNSORTED, SortInfo} from '../SortInfo';

import './TablePanel.css';
import ASC_ICO from 'html/images/sort_asc.gif';
import DESC_ICO from 'html/images/sort_desc.gif';

const {Table, Column, Cell} = FixedDataTable;

export class BasicTable extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
                widthPx: 0,
                heightPx: 0,
                columnWidths: makeColWidth(props.columns, props.data, props.showUnits)
        };

        this.onResize = this.onResize.bind(this);
        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
    }

    onResize() {
        return debounce((size) => {
            if (size) {
                var widthPx = size.width;
                var heightPx = size.height;
                this.setState({widthPx, heightPx});
            }
        }, 200);
    }

    onColumnResizeEndCallback(newColumnWidth, columnKey) {
        var columnWidths = Object.assign(this.state.columnWidths, {[columnKey]: newColumnWidth});
        this.setState({columnWidths});
    }

    rowClassName(index) {
        const {hlRowIdx} = this.props;
        return (hlRowIdx === index) ? 'tablePanel__Row_highlighted' : '';
    }

    componentWillReceiveProps(nProps) {
        if (isEmpty(this.state.columnWidths) && !isEmpty(nProps.columns)) {
            this.setState({columnWidths: makeColWidth(nProps.columns, nProps.data, nProps.showUnits)});
        }
    }

    render() {
        const {columns, data, hlRowIdx, selectable, showUnits, showFilters, selectInfo, filterInfo, sortInfo, tableStore, width, height} = this.props;
        const {widthPx, heightPx, columnWidths} = this.state;
        if (isEmpty(columns)) return false;

        var style = {width, height};
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const sortInfoCls = SortInfo.parse(sortInfo);

        const headerHeight = 22 + (showUnits && 12) + (showFilters && 20);
        return (
            <Resizable id='table-resizer' style={style} onResize={this.onResize()}>
                <Table
                    rowHeight={20}
                    headerHeight={headerHeight}
                    rowsCount={data.length}
                    isColumnResizing={false}
                    onColumnResizeEndCallback={this.onColumnResizeEndCallback}
                    onRowClick={(e, index) => tableStore.onRowHighlight && tableStore.onRowHighlight(index)}
                    rowClassNameGetter={this.rowClassName}
                    scrollToRow={hlRowIdx}
                    width={widthPx}
                    height={heightPx}>
                    {makeColumns(columns, columnWidths, data, selectable, showUnits, showFilters, selectInfo, filterInfoCls, sortInfoCls, tableStore)}
                </Table>
                {isEmpty(data) && <div className='tablePanel_NoData'> No Data Found </div>}
            </Resizable>
        );
    }
}

BasicTable.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.object),
    data: PropTypes.arrayOf(PropTypes.array),
    hlRowIdx: PropTypes.number,
    selectInfo: PropTypes.instanceOf(SelectInfo),
    filterInfo: PropTypes.string,
    sortInfo: PropTypes.string,
    selectable: PropTypes.bool,
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    width: PropTypes.string,
    height: PropTypes.string,
    tableStore: PropTypes.shape({
        onRowHighlight: PropTypes.func,
        onRowSelect: PropTypes.func,
        onSelectAll: PropTypes.func,
        onSort: PropTypes.func,
        onFilter: PropTypes.func
    })
};

BasicTable.defaultProps = {
    selectable: false,
    showUnits: false,
    showFilters: false,
    width: '100%',
    height: '100%'
};


const SortSymbol = ({sortDir}) => {
    return <img style={{marginLeft: 2}} src={sortDir === SORT_ASC ? ASC_ICO : DESC_ICO}/>;
};

const TextCell = ({rowIndex, data, col}) => {
    return (
        <Cell>
            {get(data, [rowIndex, col],'undef')}
        </Cell>
    );
};

const HeaderCell = ({col, showUnits, showFilters, filterInfoCls, sortInfoCls, tableStore}) => {

    const cname = col.name;
    const sortDir = sortInfoCls.getDirection(cname);

    const onfilterChange = ({fieldKey, valid, value}) => {
        if (valid && !filterInfoCls.isEqual(fieldKey, value)) {
            filterInfoCls.setFilter(fieldKey, value);
            tableStore.onFilter && tableStore.onFilter(filterInfoCls.serialize());
        }
    };

    const onSortChange = () => {
        tableStore.onSort && tableStore.onSort(sortInfoCls.toggle(cname).serialize());
    };

    return (
        <div title={col.title || cname} className='TablePanel__header'>
            <div onClick={onSortChange} >{cname}
                { sortDir!==UNSORTED && <SortSymbol sortDir={sortDir}/> }
            </div>
            {showUnits && col.units && <div style={{fontWeight: 'normal'}}>({col.units})</div>}
            {showFilters && <InputField
                validator={FilterInfo.validator}
                fieldKey={cname}
                tooltip = {FILTER_TTIPS}
                value = {filterInfoCls.getFilter(cname)}
                onChange = {onfilterChange}
                actOn={['blur','enter']}
                showWarning={false}
                width={'100%'}
            />
            }
        </div>
    );
};

function makeColWidth(columns, data, showUnits) {
    return !columns ? {} : columns.reduce((widths, col, cidx) => {
        const label = col.name;
        var nchar = col.prefWidth;
        const unitLength = showUnits ? get(col, 'units.length', 0) : 0;
        if (!nchar) {
            nchar = Math.max(label.length, unitLength, get(data, `0.${cidx}.length`, 0));
        }
        widths[col.name] = nchar * 8 + 20;    // 20 is for the padding and sort symbol
        return widths;
    }, {});
}

function makeColumns(columns, columnWidths, data, selectable, showUnits, showFilters, selectInfo, filterInfoCls, sortInfoCls, tableStore) {
    if (!columns) return false;

    var colsEl = columns.map((col, idx) => {
        if (col.visibility !== 'show') return false;
        return (
            <Column
                key={col.name}
                columnKey={col.name}
                header={<HeaderCell {...{col, showUnits, showFilters, filterInfoCls, sortInfoCls, tableStore}} />}
                cell={<TextCell data={data} col={idx} />}
                fixed={false}
                width={columnWidths[col.name]}
                isResizable={true}
                allowCellsRecycling={true}
            />
        );
    });
    if (selectable) {
        const headerCB = () => {
            const onSelectAll = (e) => tableStore.onSelectAll && tableStore.onSelectAll(e.target.checked);
            return (
                <div className='tablePanel__checkbox'>
                    <input type='checkbox' checked={selectInfo.isSelectAll()} onChange ={onSelectAll}/>
                </div>
            );
        };

        const cellCB = ({rowIndex}) => {
            const onRowSelect = (e) => tableStore.onRowSelect && tableStore.onRowSelect(e.target.checked, rowIndex);
            return (
                <div className='tablePanel__checkbox' style={{backgroundColor: 'whitesmoke'}}>
                    <input type='checkbox' checked={selectInfo.isSelected(rowIndex)} onChange={onRowSelect}/>
                </div>
            );
        };

        var cbox = <Column
            key="selectable-checkbox"
            columnKey='selectable-checkbox'
            header={headerCB}
            cell={cellCB}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />;
        colsEl.splice(0, 0, cbox);
    }
    return colsEl;
}

