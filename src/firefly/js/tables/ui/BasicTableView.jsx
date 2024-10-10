/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useEffect, useRef} from 'react';
import {Box, Typography} from '@mui/joy';
import {arrayOf, array, bool, func, instanceOf, number, object, objectOf, shape, string} from 'prop-types';
import {Column, Table} from 'fixed-data-table-2';
import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {get, set, isEmpty, isUndefined, omitBy, pick} from 'lodash';

import {
    getCellValue, getColMaxVal, getColMaxValues, getColumns, getProprietaryInfo, getTableState, getTableUiById,
    getTblById, hasRowAccess, hasSubHighlightRows, isClientTable, isSubHighlightRow, tableTextView, TBL_STATE,
    uniqueTblUiId
} from '../TableUtil.js';
import {SelectInfo} from '../SelectInfo.js';
import {FilterInfo} from '../FilterInfo.js';
import {SortInfo} from '../SortInfo.js';
import {CellWrapper, getPxWidth, HeaderCell, headerStyle, makeDefaultRenderer, SelectableCell, SelectableHeader} from './TableRenderer.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchTableUiUpdate, TBL_UI_UPDATE} from '../TablesCntlr.js';
import {Logger} from '../../util/Logger.js';

import 'fixed-data-table-2/dist/fixed-data-table.css';
import {updateSet} from 'firefly/util/WebUtil.js';
import {TableMask} from 'firefly/ui/panel/MaskPanel.jsx';
import {TableErrorMsg} from 'firefly/tables/ui/TablePanel.jsx';

const logger = Logger('Tables').tag('BasicTable');
const noDataMsg = 'No Data Found';
const noDataFromFilter = 'No data match these criteria';

export const BY_SCROLL = 'byScroll';


// Override default fixed-data-table-2 css with joy-ui colors and styles
const tableStyleOverrides = {
    '.public_fixedDataTableCell_main, .public_fixedDataTableCell_cellContent': {
        padding: '0 2px',
    },
    '.fixedDataTableRowLayout_rowWrapper': {
        fontSize: '12px',
    },
    '.fixedDataTableCellLayout_main': {
        borderStyle: 'solid',
        borderWidth: '0 1px 1px 0',
    },
    '.public_fixedDataTable_main': {
        borderColor: 'neutral.outlinedBorder',
    },
    '.fixedDataTableRowLayout_main div, .fixedDataTableCellLayout_main [role=columnheader]': {
        backgroundColor: 'background.surface',
    },
    '.fixedDataTableRowLayout_main.highlighted div': {
        backgroundColor: 'warning.softHoverBg',
    },
    '.fixedDataTableRowLayout_main.related div': {
        backgroundColor: 'warning.softBg',
    },
    '.fixedDataTableRowLayout_main.no-access div': {
        backgroundColor: 'danger.softBg',
    },
    '.fixedDataTableRowLayout_main.no-access-highlighted div': {
        backgroundColor: 'danger.softHoverBg',
    },
    '.public_fixedDataTableCell_main, .public_fixedDataTableRow_main, .public_fixedDataTable_scrollbarSpacer, .public_fixedDataTableRow_highlighted .public_fixedDataTableCell_main, .public_fixedDataTable_header .public_fixedDataTableCell_main': {
        backgroundImage: 'none',
        backgroundColor: 'unset',
        fontWeight: 'unset',
        borderColor: 'neutral.outlinedBorder',
    },
    '.public_fixedDataTableRow_fixedColumnsDivider': {
        borderColor: 'neutral.outlinedBorder',
    },
    '.fixedDataTableCellLayout_columnResizerContainer, .public_fixedDataTable_scrollbarSpacer': {
        backgroundColor: 'unset !important',
    },
    '.public_Scrollbar_main.public_Scrollbar_mainActive, .public_Scrollbar_main': {
        backgroundColor: 'var(--scrollbar-color-track)',
        borderColor: 'neutral.outlinedBorder',
    },
    '.public_Scrollbar_mainOpaque, .public_Scrollbar_mainOpaque.public_Scrollbar_mainActive, .public_Scrollbar_mainOpaque:hover': {
        backgroundColor: 'var(--scrollbar-color-track)',
    },
    '.public_Scrollbar_face:after': {
        backgroundColor: 'var(--scrollbar-color-thumb)',
    },
    '.public_Scrollbar_main:hover .public_Scrollbar_face:after, .public_Scrollbar_mainActive .public_Scrollbar_face:after, .public_Scrollbar_faceActive:after': {
        backgroundColor: 'var(--scrollbar-color-thumb-active)',
    },
    '.public_fixedDataTableRow_columnsShadow': {
        backgroundImage: 'none',
        backgroundColor: 'unset',
    },
};


export const NoDataTableView = ({sx, children}) => (
    <Box sx={{position: 'absolute', top: '50%', left: '50%', zIndex: 1, fontSize: 'lg', color: 'text.tertiary', ...sx}}>
        {children}
    </Box>
);

const BasicTableViewInternal = React.memo(({ selectable:selectableIn= false, showTypes:showTypesIn= false,
                                               showMask= false, rowHeight:rowHeightIn= 20, currentPage:currentPageIn= -1, ...rest }) => {
    const props= { selectable:selectableIn, showTypes:showTypesIn, showMask, rowHeight:rowHeightIn,
        currentPage:currentPageIn, ...rest} ;
    const {width, height} = props.size;
    const {columns, data, hlRowIdx, renderers, selectInfoCls, callbacks, rowHeight, rowHeightGetter, showHeader=true,
            error, tbl_ui_id=uniqueTblUiId(), currentPage, startIdx=0, highlightedRowHandler, cellRenderers, onRowDoubleClick} = props;

    const uiStates = getTableUiById(tbl_ui_id) || {};
    const {tbl_id, columnWidths, scrollLeft=0, scrollTop=0, triggeredBy, showTypes, showFilters, showUnits, filterInfo,
            selectable, sortInfo, textView} = uiStates;
    const tableRef = useRef();

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

    const headerHeight = showHeader ? 19 + (showUnits && 15) + (showTypes && 14) + (showFilters && 25) : 0;
    const maxScrollWidth = tableRef.current?.getApi().getCellGroupWidth() || -1;

    const adjScrollTop = correctScrollTopIfNeeded(maxScrollWidth, scrollTop, width, height-headerHeight, rowHeight, hlRowIdx, triggeredBy);
    const adjScrollLeft = correctScrollLeftIfNeeded(maxScrollWidth, scrollLeft, width, triggeredBy);

    const isSingleColumnTable = (columns) => getColumns({tableData: {columns}}).length === 1;

    useEffect( () => {
        const changes = {};
        if (!isEmpty(columns)){
            const calcWidth = width-15-( selectable ? 25 : 0);
            if (isSingleColumnTable(columns) && (!columnWidths || columnWidths[0]!==calcWidth)) {
                // set 1st (only visible) column's width to table's width minus scrollbar's width (15px)
                changes.columnWidths = [calcWidth, ...Array(columns.length - 1).fill(0)];
            } else if(columnWidths?.length !== columns.length) {
                changes.columnWidths = columnWidthsInPixel(columns, data);
            } else if (columnWidths.some?.((w) => w < 0)) {       // at least one column needs width calc
                changes.columnWidths = columnWidths.map((w, idx) => w >0 ? w : colWidthInPixel( getColMaxVal(columns[idx], idx,data), columns[idx]));
            }
        }
        if (adjScrollTop !== scrollTop)     changes.scrollTop = adjScrollTop;

        if (!isEmpty(changes)) {
            dispatchTableUiUpdate({tbl_ui_id, ...changes});
        }
    }, [columns, columnWidths, width, adjScrollLeft, adjScrollTop]);

    const makeColumnsProps = {columns, data, selectable, selectInfoCls, renderers,
        columnWidths, filterInfo, sortInfo, showHeader, showUnits, showTypes, showFilters,
        onSort, onFilter, onRowSelect, onSelectAll, onFilterSelected, startIdx, cellRenderers, tbl_id};

    const rowClassNameGetter = highlightedRowHandler || defHighlightedRowHandler(tbl_id, hlRowIdx, startIdx);


    const tstate = getTableState(tbl_id, {error});      // tableModel is used when tbl_id is not defined.
    logger.debug(`render.. state:[${tstate}] -- ${tbl_id}`);
    if (tstate === TBL_STATE.ERROR)   return  <TableErrorMsg error={error}/>;
    if (tstate === TBL_STATE.LOADING || isEmpty(columnWidths)) return <TableMask width={1}/>;

    const content = () => {
        if (textView) {
            return <TextView { ...{columns, data, showUnits, width, height} }/>;
        } else {
            return (
                <Table ref={tableRef}
                       rowHeight={rowHeight}
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

        if (status) return <NoDataTableView> {status} </NoDataTableView>;
        else return null;
    };

    return (
        <Box tabIndex='-1' onKeyDown={onKeyDown} sx={{
            lineHeight:1, flexGrow:1, minHeight:0, minWidth:0,
            color: 'text.secondary', //otherwise parent's font color bleeds (which is not same in dark mode)
            '&': tableStyleOverrides
        }}>
            {content()}
            <Status/>
        </Box>
    );
});


BasicTableViewInternal.propTypes = {
    tbl_ui_id: string,
    columns: arrayOf(object),
    data: arrayOf(array),
    hlRowIdx: number,
    selectInfoCls: instanceOf(SelectInfo),
    filterInfo: string,
    sortInfo: string,
    selectable: bool,
    showUnits: bool,
    showTypes: bool,
    showFilters: bool,
    showHeader: bool,
    textView: bool,
    rowHeight: number,
    rowHeightGetter: func,  // params: rowData and columnWidths, returns height
    showMask: bool,
    currentPage: number,
    startIdx: number,
    error:  string,
    size: object.isRequired,
    highlightedRowHandler: func,
    onRowDoubleClick: func,
    renderers: objectOf(
        shape({
            cellRenderer: func,
            headRenderer: func
        })
    ),
    callbacks: shape({
        onRowHighlight: func,
        onRowSelect: func,
        onSelectAll: func,
        onSort: func,
        onFilter: func,
        onGotoPage: func
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
    const {columnWidths=[], tbl_ui_id} = this;
    if(columnKey >= 0 && columnKey < columnWidths.length) {
        dispatchTableUiUpdate({ tbl_ui_id, columnWidths: updateSet(columnWidths, columnKey, newColumnWidth)});
    }
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
}

function doSort(cname) {
    const {callbacks, sortInfo} = this;
    if (callbacks.onSort) {
        const sortInfoCls = SortInfo.parse(sortInfo);
        callbacks.onSort(sortInfoCls.toggle(cname));
    }
}

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

function correctScrollTopIfNeeded(maxScrollWidth, scrollTop, width, height, rowHeight, hlRowIdx, triggeredBy) {
    const rowHpos = hlRowIdx * rowHeight;
    if (triggeredBy !== BY_SCROLL) {
        // delta is a workaround for the horizontal scrollbar hiding part of the last row when visible
        const delta = maxScrollWidth > width ? (.5*rowHeight) : 0;

        if (rowHpos < scrollTop) {
            return rowHpos - (height * .3);
        } else if(rowHpos + rowHeight + delta > scrollTop + height) {
            return rowHpos - height + (height * .3);
        }
    }
    return scrollTop;
}

function correctScrollLeftIfNeeded(maxScrollWidth, scrollLeft, width, triggeredBy) {

    if (scrollLeft < 0 || maxScrollWidth < 0) {
        return undefined;
    } else if (scrollLeft > 0 && triggeredBy === TBL_UI_UPDATE) {
        if (maxScrollWidth < width) return undefined;       // if the total widths of the columns is less than the view's width, don't apply scrollLeft
        if (scrollLeft > maxScrollWidth) return maxScrollWidth;               // cannot scroll beyond width
    }
    return scrollLeft;
}

function columnWidthsInPixel(columns, data, minWidth) {

    const maxVals = getColMaxValues(columns, data, {maxColWidth: 100, maxAryWidth: 30});
    return maxVals.map((text, idx) => colWidthInPixel(text, columns[idx]), minWidth);
}

function colWidthInPixel(text, col, minWidth=45, paddings=8) {
    const header = col.label || col.name;
    const style = header === text ? headerStyle : {fontSize:12};
    text = text.replace(/[^a-zA-Z0-9]/g, 'O');    // some non-alphanum values can be very narrow.  use 'O' in place of them.
    const pxNum =  getPxWidth({text, ...style}) + paddings;
    return pxNum < minWidth ? minWidth : pxNum;
}


function defHighlightedRowHandler(tbl_id, hlRowIdx, startIdx) {

    const tableModel = getTblById(tbl_id);
    const hasProprietaryInfo = !isEmpty(getProprietaryInfo(tableModel));
    const hasRelated= hasSubHighlightRows(tableModel);

    return (rowIndex) => {
        const absRowIndex = startIdx + rowIndex;
        if (hasProprietaryInfo && !hasRowAccess(tableModel, absRowIndex)) {
            return hlRowIdx === rowIndex ? 'no-access-highlighted' : 'no-access';
        }
        if (hlRowIdx === rowIndex) return 'highlighted';
        if (hasRelated && isSubHighlightRow(tableModel,rowIndex,hlRowIdx)) return 'related';
    };
}

function makeColumns (props) {
    const {columns, columnWidths} = props;

    if (isEmpty(columns) || columns.length !== columnWidths?.length) return false;

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

