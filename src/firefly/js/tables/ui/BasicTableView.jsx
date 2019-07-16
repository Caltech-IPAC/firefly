/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useEffect} from 'react';
import PropTypes from 'prop-types';
import FixedDataTable from 'fixed-data-table-2';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {get, isEmpty} from 'lodash';

import {tableTextView, getTableUiById, getProprietaryInfo, getTblById, hasRowAccess, calcColumnWidths, uniqueTblUiId} from '../TableUtil.js';
import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
import {TextCell, HeaderCell, SelectableHeader, SelectableCell} from './TableRenderer.js';

import './TablePanel.css';
import {LinkCell} from './TableRenderer';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {dispatchTableUiUpdate} from '../TablesCntlr';
import * as Cntlr from '../TablesCntlr';

const {Table, Column} = FixedDataTable;
const noDataMsg = 'No Data Found';
const noDataFromFilter = 'No data match these criteria';

const BY_SCROLL = 'byScroll';


const BasicTableViewInternal = React.memo((props) => {

    const {width, height} = props.size;
    const {columns, data, hlRowIdx, showUnits, showTypes, showFilters, filterInfo, renderers,
            bgColor, selectable, selectInfoCls, sortInfo, callbacks, textView, rowHeight,
            showMask, error, tbl_ui_id=uniqueTblUiId(), currentPage, startIdx=0, highlightedRowHandler} = props;

    const uiStates = getTableUiById(tbl_ui_id) || {};
    const {tbl_id, columnWidths, scrollLeft=0, scrollTop=0, triggeredBy} = uiStates;

    const onScrollEnd    = useCallback( doScrollEnd.bind({tbl_ui_id}), [tbl_ui_id]);
    const onColumnResize = useCallback( doColumnResize.bind({columnWidths, tbl_ui_id}), [columnWidths]);
    const onKeyDown      = useCallback( doKeyDown.bind({callbacks, hlRowIdx, currentPage}), [callbacks, hlRowIdx, currentPage]);
    const onRowSelect    = useCallback( doRowSelect.bind({callbacks}), [callbacks]);
    const onSelectAll    = useCallback( doSelectAll.bind({callbacks}), [callbacks]);
    const onSort         = useCallback( doSort.bind({callbacks, sortInfo}), [callbacks, sortInfo]);
    const onFilter       = useCallback( doFilter.bind({callbacks, filterInfo}), [callbacks, filterInfo]);
    const onFilterSelected = useCallback( doFilterSelected.bind({callbacks, selectInfoCls}), [callbacks, selectInfoCls]);

    const headerHeight = 22 + (showUnits && 8) + (showTypes && 8) + (showFilters && 22);
    let totalColWidths = 0;
    if (!isEmpty(columns) && !isEmpty(columnWidths)) {
        totalColWidths = columns.reduce((pv, c, idx) => {
            const delta =  get(c, ['visibility'], 'show') === 'show' ?  columnWidths[idx] : 0;
            return pv + delta;
        }, 0);
    }

    const adjScrollTop = correctScrollTopIfNeeded(totalColWidths, scrollTop, width, height-headerHeight, rowHeight, hlRowIdx, triggeredBy);
    const adjScrollLeft = correctScrollLeftIfNeeded(totalColWidths, scrollLeft, width, triggeredBy);

    useEffect( () => {
        const changes = {};
        if (!columnWidths)                  changes.columnWidths = columnWidthsInPixel(columns, data);
        if (adjScrollTop !== scrollTop)     changes.scrollTop = adjScrollTop;
        if (adjScrollLeft !== scrollLeft)   changes.scrollLeft = adjScrollLeft;

        if (!isEmpty(changes)) {
            dispatchTableUiUpdate({tbl_ui_id, ...changes});
        }
    });

    if (!error && (isEmpty(columns) || isEmpty(columnWidths))) return (<div style={{top: 0}} className='loading-mask'/>);

    const makeColumnsProps = {columns, data, selectable, selectInfoCls, renderers, bgColor,
        columnWidths, filterInfo, sortInfo, showUnits, showTypes, showFilters,
        onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected, tbl_id};

    const rowClassNameGetter = highlightedRowHandler || defHighlightedRowHandler(tbl_id, hlRowIdx, startIdx);

    const content = () => {
        if (error) {
            return <div style={{padding: 10}}>{error}</div>;
        } else if (width === 0 || showMask || isEmpty(columns)) {
            return <div/>;
        } else if (textView) {
            return <TextView { ...{columns, data, showUnits, width, height} }/>;
        } else {
            return (
                <Table  rowHeight={rowHeight}
                        headerHeight={headerHeight}
                        rowsCount={data.length}
                        isColumnResizing={false}
                        onColumnResizeEndCallback={onColumnResize}
                        onRowClick={(e, index) => callbacks.onRowHighlight && callbacks.onRowHighlight(index)}
                        rowClassNameGetter={rowClassNameGetter}
                        onScrollEnd={onScrollEnd}
                        scrollTop = {adjScrollTop}
                        scrollLeft={adjScrollLeft}
                        width={width}
                        height={height}>

                    { makeColumns(makeColumnsProps) }
                </Table>
            );
        }
    };

    const status = () => {
        if (!error) {
            if (showMask) {
                return <div style={{top: 0}} className='loading-mask'/>;
            } else if (isEmpty(data)) {
                return <div className='TablePanel_NoData'> {filterInfo ? noDataFromFilter : noDataMsg} </div>;
            }
        }
        return null;
    };

    return (
        <div tabIndex='-1' onKeyDown={onKeyDown} className='TablePanel__frame'>
            {content()}
            {status()}
        </div>
    );
});


BasicTableViewInternal.propTypes = {
    tbl_ui_id: PropTypes.string,
    columns: PropTypes.arrayOf(PropTypes.object),
    data: PropTypes.arrayOf(PropTypes.array),
    hlRowIdx: PropTypes.number,
    selectInfoCls: PropTypes.instanceOf(SelectInfo),
    filterInfo: PropTypes.string,
    sortInfo: PropTypes.string,
    selectable: PropTypes.bool,
    showUnits: PropTypes.bool,
    showTypes: PropTypes.bool,
    showFilters: PropTypes.bool,
    textView: PropTypes.bool,
    rowHeight: PropTypes.number,
    showMask: PropTypes.bool,
    currentPage: PropTypes.number,
    startIdx: PropTypes.number,
    bgColor: PropTypes.string,
    error:  PropTypes.string,
    size: PropTypes.object.isRequired,
    highlightedRowClsName: PropTypes.func,
    renderers: PropTypes.objectOf(
        PropTypes.shape({
            cellRenderer: PropTypes.func,
            headRenderer: PropTypes.func
        })
    ),
    callbacks: PropTypes.shape({
        onRowHighlight: PropTypes.func,
        onRowSelect: PropTypes.func,
        onSelectAll: PropTypes.func,
        onSort: PropTypes.func,
        onFilter: PropTypes.func,
        onGotoPage: PropTypes.func
    })

};

BasicTableViewInternal.defaultProps = {
    selectable: false,
    showUnits: false,
    showTypes: false,
    showFilters: false,
    showMask: false,
    rowHeight: 20,
    currentPage: -1
};

export const BasicTableView = wrapResizer(BasicTableViewInternal);

export const BasicTableViewWithConnector = React.memo((props) => {
    const {tbl_ui_id=uniqueTblUiId()} = props;
    useStoreConnector(() => getTableUiById(tbl_ui_id));
    return <BasicTableView {...{tbl_ui_id, ...props}}/>;
});

/*---------------------------------------------------------------------------*/

function doScrollEnd(scrollLeft, scrollTop) {
    const {tbl_ui_id} = this;
    const {scrollLeft:cScrollLeft, scrollTop:cScrollTop} =  getTableUiById(tbl_ui_id);
    if (cScrollLeft !== scrollLeft || cScrollTop !== scrollTop) {
        dispatchTableUiUpdate({ tbl_ui_id, scrollLeft, scrollTop, triggeredBy: BY_SCROLL});
    }
}

function doColumnResize(newColumnWidth, columnKey) {
    const {columnWidths={}, tbl_ui_id} = this;
    dispatchTableUiUpdate({
        tbl_ui_id,
        columnWidths: Object.assign({}, columnWidths, {[columnKey]: newColumnWidth})
    });
}

function doKeyDown(e) {
    const {callbacks, hlRowIdx, currentPage} = this;
    const key = get(e, 'key');
    if (key === 'ArrowDown') {
        callbacks.onRowHighlight && callbacks.onRowHighlight(hlRowIdx + 1);
        e.preventDefault && e.preventDefault();
    } else if (key === 'ArrowUp') {
        callbacks.onRowHighlight && callbacks.onRowHighlight(hlRowIdx - 1);
        e.preventDefault && e.preventDefault();
    } else if (key === 'PageDown') {
        callbacks.onGotoPage && callbacks.onGotoPage(currentPage + 1);
        e.preventDefault && e.preventDefault();
    } else if (key === 'PageUp') {
        callbacks.onGotoPage && callbacks.onGotoPage(currentPage - 1);
        e.preventDefault && e.preventDefault();
    }
}

function doFilterSelected() {
    const {callbacks, selectInfoCls} = this;
    if (callbacks.onFilterSelected) {
        const selected = [...selectInfoCls.getSelected()];
        callbacks.onFilterSelected(selected);
    }
}

function doFilter({fieldKey, valid, value}) {
    const {callbacks, filterInfo} = this;
    if (callbacks.onFilter) {
        const filterInfoCls = FilterInfo.parse(filterInfo);
        if (valid && !filterInfoCls.isEqual(fieldKey, value)) {
            filterInfoCls.setFilter(fieldKey, value);
            callbacks.onFilter(filterInfoCls.serialize());
        }
    }
};

function doSort(cname) {
    const {callbacks, sortInfo} = this;
    if (callbacks.onSort) {
        const sortInfoCls = SortInfo.parse(sortInfo);
        callbacks.onSort(sortInfoCls.toggle(cname));
    }
};

function doSelectAll(checked) {
    const {callbacks} = this;
    callbacks.onSelectAll && callbacks.onSelectAll(checked);
}

function doRowSelect(checked, rowIndex) {
    const {callbacks} = this;
    callbacks.onRowSelect && callbacks.onRowSelect(checked, rowIndex);
}


const TextView = ({columns, data, showUnits, width, height}) => {
    const text = tableTextView(columns, data, showUnits);
    return (
        <div style={{height, width,overflow: 'hidden'}}>
            <div style={{height: '100%',overflow: 'auto'}}>
                <pre>{text}</pre>
            </div>
        </div>
    );
};

function correctScrollTopIfNeeded(totalColWidths, scrollTop, width, height, rowHeight, hlRowIdx, triggeredBy) {
    const rowHpos = hlRowIdx * rowHeight;
    if (triggeredBy !== BY_SCROLL) {
        // delta is a workaround for the horizontal scrollbar hiding part of the last row when visible
        const delta = totalColWidths > width ? (.5*rowHeight) : 0;

        if (rowHpos < scrollTop) {
            return rowHpos - (height * .3);
        } else if(rowHpos + rowHeight + delta > scrollTop + height) {
            return rowHpos - height + (height * .3);
        }
    }
    return scrollTop;
}

function correctScrollLeftIfNeeded(totalColWidths, scrollLeft, width, triggeredBy) {

    if (scrollLeft < 0) {
        return undefined;
    } else if (scrollLeft > 0 && triggeredBy === Cntlr.TBL_UI_UPDATE) {
        if (totalColWidths < width) {
            // if the total widths of the columns is less than the view's width, don't apply scrollLeft
            return undefined;
        }
    }
    return scrollLeft;
}

function columnWidthsInPixel(columns, data) {
    return calcColumnWidths(columns, data)
            .map( (w) =>  (w + 2) * 7);
}

function defHighlightedRowHandler(tbl_id, hlRowIdx, startIdx) {

    const tableModel = getTblById(tbl_id);
    const hasProprietaryInfo = !isEmpty(getProprietaryInfo(tableModel));

    return (rowIndex) => {
        const absRowIndex = startIdx + rowIndex;
        if (hasProprietaryInfo && !hasRowAccess(tableModel, absRowIndex)) {
            return hlRowIdx === rowIndex ? 'TablePanel__no-access--highlighted' : 'TablePanel__no-access';
        }
        if (hlRowIdx === rowIndex) return 'tablePanel__Row_highlighted';
    };
}

function makeColumns (props) {
    const {columns} = props;

    if (isEmpty(columns)) return false;

    var colsEl = columns.map((col, idx) => makeColumnTag(props, col, idx));
    const selBoxCol = makeSelColTag(props);
    return [selBoxCol, ...colsEl].filter((c) => c);
}


function makeColumnTag(props, col, idx) {
    const {data, columnWidths, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, tbl_id, renderers, bgColor='white'} = props;

    if (col.visibility && col.visibility !== 'show') return false;
    const HeadRenderer = get(renderers, [col.name, 'headRenderer'], HeaderCell);
    const CellRenderer = get(renderers, [col.name, 'cellRenderer'], getDefaultRenderer(col, tbl_id));
    const fixed = col.fixed || false;
    const style = col.fixed && {backgroundColor: bgColor};

    return (
        <Column
            key={col.name}
            columnKey={idx}
            header={<HeadRenderer {...{col, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, tbl_id}} />}
            cell={<CellRenderer {...{style, data, tbl_id, col, colIdx:idx}} />}
            fixed={fixed}
            width={columnWidths[idx]}
            isResizable={true}
            allowCellsRecycling={true}
        />
    );
}

function makeSelColTag({selectable, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected, bgColor='white', selectInfoCls, onRowSelect}) {

    if (!selectable) return false;

    const checked = selectInfoCls.isSelectAll();
    return (
        <Column
            key='selectable-checkbox'
            columnKey='selectable-checkbox'
            header={<SelectableHeader {...{checked, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected}} />}
            cell={<SelectableCell style={{backgroundColor: bgColor}} selectInfoCls={selectInfoCls} onRowSelect={onRowSelect} />}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />
    );
}

function getDefaultRenderer(col={}, tbl_id) {
    if (col.type === 'location' || !isEmpty(col.links)) {
        return ((props) => <LinkCell {...props} {...{tbl_id, col}}/>);
    }

    return TextCell;
}

