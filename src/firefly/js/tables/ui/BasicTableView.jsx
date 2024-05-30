/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useEffect} from 'react';
import {Box, Typography} from '@mui/joy';
import PropTypes, {func} from 'prop-types';
import {Column, Table} from 'fixed-data-table-2';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {get, set, isEmpty, isUndefined, omitBy, pick} from 'lodash';

import {calcColumnWidths, getCellValue, getColMaxValues, getColumns, getProprietaryInfo, getTableState, getTableUiById, getTblById, hasRowAccess, isClientTable, tableTextView, TBL_STATE, uniqueTblUiId} from '../TableUtil.js';
import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
import {CellWrapper, getPxWidth, HeaderCell, headerLevel, headerStyle, makeDefaultRenderer, SelectableCell, SelectableHeader} from './TableRenderer.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchTableUiUpdate, TBL_UI_UPDATE} from '../TablesCntlr.js';
import {Logger} from '../../util/Logger.js';

import 'fixed-data-table-2/dist/fixed-data-table.css';
import './TablePanel.css';

const logger = Logger('Tables').tag('BasicTable');
const noDataMsg = 'No Data Found';
const noDataFromFilter = 'No data match these criteria';

export const BY_SCROLL = 'byScroll';


const BasicTableViewInternal = React.memo((props) => {

    const {width, height} = props.size;
    const {columns, data, hlRowIdx, renderers, selectInfoCls, callbacks, rowHeight, rowHeightGetter, showHeader=true,
            error, tbl_ui_id=uniqueTblUiId(), currentPage, startIdx=0, highlightedRowHandler, cellRenderers, onRowDoubleClick} = props;

    const uiStates = getTableUiById(tbl_ui_id) || {};
    const {tbl_id, columnWidths, scrollLeft=0, scrollTop=0, triggeredBy, showTypes, showFilters, showUnits, filterInfo,
            selectable, sortInfo, textView} = uiStates;

    useEffect( () => {
        if (!isEmpty(columns)) {
            const changes = omitBy(pick(props, 'showTypes', 'showFilters', 'showUnits', 'filterInfo','selectable', 'sortInfo', 'textView'), isUndefined);

            const showUnits = !!columns.find?.((col) => col?.units);
            if (isUndefined(changes.showUnits)) changes.showUnits = showUnits;
            if (isUndefined(changes.options?.showUnits)) set(changes,'options.showUnits', showUnits);

            const showFilters = !(!tbl_id || isClientTable(tbl_id));    // false if no tbl_id or is a client table
            if (isUndefined(changes.showFilters)) changes.showFilters = showFilters;
            if (isUndefined(changes.options?.showFilters)) set(changes,'options.showFilters', showFilters);

            if (!isEmpty(changes)) {
                dispatchTableUiUpdate({tbl_ui_id, ...changes});
            }
        }
    }, [columns]);

    const onScrollEnd    = useCallback( doScrollEnd.bind({tbl_ui_id}), [tbl_ui_id]);
    const onColumnResize = useCallback( doColumnResize.bind({columnWidths, tbl_ui_id}), [columnWidths]);
    const onKeyDown      = useCallback( doKeyDown.bind({callbacks, hlRowIdx, currentPage}), [callbacks, hlRowIdx, currentPage]);
    const onRowSelect    = useCallback( doRowSelect.bind({callbacks}), [callbacks]);
    const onSelectAll    = useCallback( doSelectAll.bind({callbacks}), [callbacks]);
    const onSort         = useCallback( doSort.bind({callbacks, sortInfo}), [callbacks, sortInfo]);
    const onFilter       = useCallback( doFilter.bind({callbacks, filterInfo}), [callbacks, filterInfo]);
    const onFilterSelected = useCallback( doFilterSelected.bind({callbacks, selectInfoCls}), [callbacks, selectInfoCls]);

    const headerHeight = showHeader ? 18 + (showUnits && 13) + (showTypes && 13) + (showFilters && 26) : 0;
    let totalColWidths = 0;
    if (!isEmpty(columns) && !isEmpty(columnWidths)) {
        totalColWidths = columns.reduce((pv, c, idx) => {
            const delta =  get(c, ['visibility'], 'show') === 'show' ?  columnWidths[idx] : 0;
            return pv + delta;
        }, 0);
    }

    const adjScrollTop = correctScrollTopIfNeeded(totalColWidths, scrollTop, width, height-headerHeight, rowHeight, hlRowIdx, triggeredBy);
    const adjScrollLeft = correctScrollLeftIfNeeded(totalColWidths, scrollLeft, width, triggeredBy);

    const isSingleColumnTable = (columns) => getColumns({tableData: {columns}}).length === 1;

    useEffect( () => {
        const changes = {};
        if (!isEmpty(columns)){
            if (isSingleColumnTable(columns) && (!columnWidths || columnWidths[0]!==width-15)) {
                // set 1st (only visible) column's width to table's width minus scrollbar's width (15px)
                changes.columnWidths = [width-15, ...Array(columns.length - 1).fill(0)];
            }
            else if(!columnWidths) changes.columnWidths = columnWidthsInPixel(columns, data);
        }
        if (adjScrollTop !== scrollTop)     changes.scrollTop = adjScrollTop;
        if (adjScrollLeft !== scrollLeft)   changes.scrollLeft = adjScrollLeft;

        if (!isEmpty(changes)) {
            dispatchTableUiUpdate({tbl_ui_id, ...changes});
        }
    }, [columns, columnWidths, width, adjScrollLeft, adjScrollTop]);

    const makeColumnsProps = {columns, data, selectable, selectInfoCls, renderers,
        columnWidths, filterInfo, sortInfo, showHeader, showUnits, showTypes, showFilters,
        onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected, startIdx, cellRenderers, tbl_id};

    const rowClassNameGetter = highlightedRowHandler || defHighlightedRowHandler(tbl_id, hlRowIdx, startIdx);


    const tstate = getTableState(tbl_id);
    logger.debug(`render.. state:[${tstate}] -- ${tbl_id}`);

    if (tstate === TBL_STATE.ERROR)   return  <div style={{padding: 10}}>{error}</div>;
    if (tstate === TBL_STATE.LOADING || isEmpty(columnWidths)) return <div style={{top: 0}} className='loading-mask'/>;

    const content = () => {
        if (textView) {
            return <TextView { ...{columns, data, showUnits, width, height} }/>;
        } else {
            return (
                <Table rowHeight={rowHeight}
                       rowHeightGetter={rowHeightGetter && data?.length && columnWidths?.length
                           ? (rowIdx)=>rowHeightGetter(data[rowIdx], columnWidths) : undefined}
                       headerHeight={headerHeight}
                       rowsCount={data.length}
                       isColumnResizing={false}
                       onColumnResizeEndCallback={onColumnResize}
                       onRowClick={(e, index) => callbacks.onRowHighlight && callbacks.onRowHighlight(index)}
                       onRowDoubleClick={onRowDoubleClick}
                       rowClassNameGetter={rowClassNameGetter}
                       onScrollEnd={onScrollEnd}
                       scrollTop={adjScrollTop}
                       scrollLeft={adjScrollLeft}
                       width={width}
                       height={height}>

                    {makeColumns(makeColumnsProps)}
                </Table>
            );
        }
    };

    const Status = () => {
        const msg = getTblById(tbl_id)?.status?.message;
        const status = tstate === TBL_STATE.NO_DATA ? msg || noDataMsg :
                       tstate === TBL_STATE.NO_MATCH ? msg || noDataFromFilter :
                       tstate === TBL_STATE.LOADING ? 'Loading...' : '';

        if (status) return <div className='TablePanel_NoData'> {status} </div>;
        else return null;
    };

    return (
        <Box tabIndex='-1' onKeyDown={onKeyDown} sx={{
            lineHeight:1, flexGrow:1, minHeight:0, minWidth:0,
            color: 'text.secondary' //otherwise parent's font color bleeds (which is not same in dark mode)
        }}>
            {content()}
            <Status/>
        </Box>
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
    showHeader: PropTypes.bool,
    textView: PropTypes.bool,
    rowHeight: PropTypes.number,
    rowHeightGetter: PropTypes.func,  // params: rowData and columnWidths, returns height
    showMask: PropTypes.bool,
    currentPage: PropTypes.number,
    startIdx: PropTypes.number,
    error:  PropTypes.string,
    size: PropTypes.object.isRequired,
    highlightedRowHandler: PropTypes.func,
    onRowDoubleClick: PropTypes.func,
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
    showTypes: false,
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


const TextView = ({columns, data, width, height}) => {
    const text = tableTextView(columns, data);
    return (
        <Box sx={{height, width,overflow: 'hidden'}}>
            <Typography level='body-sm' sx={{height: 1,overflow: 'auto'}}>
                <pre>{text}</pre>
            </Typography>
        </Box>
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
    } else if (scrollLeft > 0 && triggeredBy === TBL_UI_UPDATE) {
        if (totalColWidths < width) {
            // if the total widths of the columns is less than the view's width, don't apply scrollLeft
            return undefined;
        }
    }
    return scrollLeft;
}

function columnWidthsInPixel(columns, data) {

    const maxVals = getColMaxValues(columns, data, {maxColWidth: 100, maxAryWidth: 30});

    const paddings = 8;
    return maxVals.map((text, idx) => {
        const header = columns[idx].label || columns[idx].name;
        const style = header === text ? headerStyle : {fontSize:12};
        text = text.replace(/[^a-zA-Z0-9]/g, 'O');    // some non-alphanum values can be very narrow.  use 'O' in place of them.
        return getPxWidth({text, ...style}) + paddings;
    });
}

function defHighlightedRowHandler(tbl_id, hlRowIdx, startIdx) {

    const tableModel = getTblById(tbl_id);
    const hasProprietaryInfo = !isEmpty(getProprietaryInfo(tableModel));
    const relatedCols = tableModel?.tableMeta?.['tbl.relatedCols'];
    const relatedColsHL = relatedCols?.split(',')
                            .map((cname) => getCellValue(tableModel, hlRowIdx, cname?.trim()))
                            .join('|');
    const isRelated = (ridx) => {
        if (relatedCols) {
            const cVal = relatedCols.split(',')
                            .map((cname) => getCellValue(tableModel, ridx, cname?.trim()))
                            .join('|');
            return relatedColsHL === cVal;
        }
        return false;
    };

    return (rowIndex) => {
        const absRowIndex = startIdx + rowIndex;
        if (hasProprietaryInfo && !hasRowAccess(tableModel, absRowIndex)) {
            return hlRowIdx === rowIndex ? 'no-access-highlighted' : 'no-access';
        }
        if (hlRowIdx === rowIndex) return 'highlighted';
        if (isRelated(rowIndex)) return 'related';
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
    const {data, columnWidths, showHeader, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter,
        tbl_id, renderers, startIdx, cellRenderers} = props;

    if (col.visibility && col.visibility !== 'show') return false;
    const HeadRenderer = get(renderers, [col.name, 'headRenderer'], showHeader ? HeaderCell : ({})=>null);
    const CellRenderer = get(renderers, [col.name, 'cellRenderer'], cellRenderers?.[idx] || makeDefaultRenderer(col,tbl_id, startIdx));
    const fixed = col.fixed || false;
    const {resizable=true} = col;

    const cell = ({height, width, columnKey, rowIndex}) =>
                    <CellWrapper {...{height, width, columnKey, rowIndex, data, col, colIdx:idx, tbl_id, startIdx, CellRenderer}} />;
    return (
        <Column
            key={col.name}
            columnKey={idx}
            header={<HeadRenderer {...{col, showUnits, showTypes, showFilters, filterInfo, sortInfo, onSort, onFilter, tbl_id}} />}
            cell={cell}
            fixed={fixed}
            width={columnWidths[idx]}
            minWidth={5}
            isResizable={resizable}
            isReorderable={true}
            allowCellsRecycling={true}
        />
    );
}

function makeSelColTag({selectable, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected, selectInfoCls, onRowSelect}) {

    if (!selectable) return false;

    const checked = selectInfoCls.isSelectAll();
    return (
        <Column
            key='selectable-checkbox'
            columnKey='selectable-checkbox'
            header={<SelectableHeader {...{checked, onSelectAll, showUnits, showTypes, showFilters, onFilterSelected}} />}
            cell={<SelectableCell selectInfoCls={selectInfoCls} onRowSelect={onRowSelect} />}
            fixed={true}
            width={25}
            allowCellsRecycling={true}
        />
    );
}

