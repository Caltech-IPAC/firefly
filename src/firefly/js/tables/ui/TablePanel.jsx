/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import {Box, Stack, Typography, Sheet, ChipDelete, Tooltip, Button} from '@mui/joy';
import PropTypes, {object, shape} from 'prop-types';
import {defer, truncate, get, set, defaults, isString} from 'lodash';
import {getAppOptions, getSearchActions} from '../../core/AppDataCntlr.js';
import {ActionsDropDownButton, isTableActionsDropVisible} from '../../ui/ActionsDropDownButton.jsx';

import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {ExpandButton, InfoButton, SaveButton, FilterButton, ClearFilterButton, TextViewButton, TableViewButton, SettingsButton, PropertySheetButton} from '../../visualize/ui/Buttons.jsx';
import {dispatchTableRemove, dispatchTblExpanded, dispatchTableFetch, dispatchTableAddLocal, dispatchTableUiUpdate} from '../TablesCntlr.js';
import {uniqueTblId, getTableUiById, makeBgKey, getResultSetRequest, isClientTable, getTableState,
    TBL_STATE, getMetaEntry, getTblById, parseError, isOverflow, getResultSetID} from '../TableUtil.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTableView} from './BasicTableView.jsx';
import {TableInfo, MetaInfo} from './TableInfo.jsx';
import {makeConnector} from '../TableConnector.js';
import {SelectInfo} from '../SelectInfo.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {showTableDownloadDialog} from './TableSave.jsx';
import {showOptionsPopup} from '../../ui/PopupUtil.jsx';
import {BgMaskPanel} from '../../core/background/BgMaskPanel.jsx';
import {Logger} from '../../util/Logger.js';
import {AddColumnBtn} from './AddOrUpdateColumn.jsx';
import WarningIcon from '@mui/icons-material/WarningAmberRounded';
import {PropertySheetAsTable} from 'firefly/tables/ui/PropertySheet';
import {META} from '../TableRequestUtil.js';
import {TableMask} from 'firefly/ui/panel/MaskPanel.jsx';

const logger = Logger('Tables').tag('TablePanel');

const TT_INFO = 'Show additional table info';
const TT_OPTIONS = 'Table Options';
const TT_SAVE = 'Save the table';
const TT_CLEAR_FILTER = 'Remove all filters';
const TT_EXPAND = 'Expand this panel to take up a larger area';
const TT_PROPERTY_SHEET = 'Show details for the selected row';

const defaultOptions = {
    showMetaInfo: false,
    allowUnits: true,
    showToolbar: true,
    showTitle: true,
    showPaging: true,
    showSave: true,
    showOptionButton: true,
    showFilterButton: true,
    showInfoButton: true,
    showAddColumn: true,
    showTypes: true,
    selectable: true,
    showSearchButton: true,
    showHeader: true,
    expandedMode: false,
    expandable: true,
    showToggleTextView: true,
    border: true,
};


export const TBL_CLZ_NAME = 'FF-Table';

export function TablePanel({tbl_id, tbl_ui_id, tableModel, variant='outlined', sx, slotProps, ...inOptions}) {
    const options= defaults({...inOptions},defaultOptions);
    tbl_id = tbl_id || tableModel?.tbl_id || uniqueTblId();
    tbl_ui_id = tbl_ui_id || `${tbl_id}-ui`;

    const uiState = useStoreConnector(() => getTableUiById(tbl_ui_id) || {columns:[]}, [tbl_ui_id]);
    const tblState = {...options, ...uiState};

    useEffect( () => {
        dispatchTableUiUpdate({tbl_ui_id, tbl_id, options});
    }, [tbl_id, tbl_ui_id]);

    useEffect( () => {
        if (tableModel && !tableModel.origTableModel) {
            tableModel.tbl_id = tbl_id;
            set(tableModel, 'request.tbl_id', tbl_id);
            dispatchTableAddLocal(tableModel, undefined, false);
        }
    }, [tbl_id, tableModel]);

    const {selectable, renderers, title, removable, rowHeight, rowHeightGetter,
        showToolbar, showTitle, showMetaInfo,
        columns, showHeader, showUnits, allowUnits, showTypes, showFilters, textView,
        error, startIdx, hlRowIdx, currentPage, selectInfo, showMask,
        filterInfo, sortInfo, data, backgroundable, highlightedRowHandler, cellRenderers, onRowDoubleClick} = tblState;

    const connector = makeConnector(tbl_id, tbl_ui_id);



    const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
    const tstate = getTableState(tbl_id);
    logger.debug({  state: `[${tstate}] -- ${tbl_id}  ${tbl_ui_id}`,
                    from: getResultSetID(tbl_id)  });

    if ([TBL_STATE.ERROR,TBL_STATE.LOADING].includes(tstate))  return <NotReady {...{showTitle, tbl_id, title, removable, backgroundable, error}}/>;

    return (
        <Sheet {...{variant, ...slotProps?.root}}
               sx ={{
                   borderRadius: variant==='outlined' ? '5px' : undefined,
                   position:'relative',
                   overflow: 'hidden',
                   boxSizing: 'border-box',
                   width:1, height:1,
                   ...sx
               }}
        >
            <Stack sx={{inset:0, position:'absolute'}}>
                {showMetaInfo && <MetaInfo tbl_id={tbl_id} {...slotProps?.meta}/> }
                <Stack className={TBL_CLZ_NAME} flexGrow={1} overflow='hidden'
                       onClick={stopPropagation}
                       onTouchStart={stopPropagation}
                       onMouseDown={stopPropagation}
                >
                    <ToolBar {...{tbl_id, tbl_ui_id, connector, tblState, slotProps}}/>
                    <Stack lineHeight={1} flexGrow={1} overflow='hidden'
                            sx={{
                                '& .fixedDataTableLayout_main': {border:'none'},
                                borderTop: showToolbar ? 1 : undefined,
                                borderColor: 'neutral.outlinedBorder'
                            }}
                           {...slotProps?.table}>
                        <BasicTableView
                            callbacks={connector}
                            { ...{columns, data, hlRowIdx, rowHeight, rowHeightGetter, selectable, showUnits,
                                allowUnits, showTypes, showFilters, selectInfoCls, filterInfo, sortInfo, textView,
                                showMask, currentPage, showHeader, renderers, tbl_ui_id, highlightedRowHandler,
                                startIdx, cellRenderers, onRowDoubleClick} }
                        />
                    </Stack>
                </Stack>
            </Stack>
        </Sheet>
    );
}

TablePanel.propTypes = {
    tbl_id: PropTypes.string,
    tbl_ui_id: PropTypes.string,
    tableModel: PropTypes.object,
    title: PropTypes.node,
    rowHeight: PropTypes.number,
    help_id: PropTypes.string,
    expandedMode: PropTypes.bool,
    pageSize: PropTypes.number,
    selectable: PropTypes.bool,
    expandable: PropTypes.bool,
    removable: PropTypes.bool,
    border: PropTypes.bool,
    showUnits: PropTypes.bool,
    allowUnits: PropTypes.bool,
    highlightedRowHandler: PropTypes.func,
    showMetaInfo: PropTypes.bool,
    showTypes: PropTypes.bool,
    showFilters: PropTypes.bool,
    showToolbar: PropTypes.bool,
    showTitle: PropTypes.bool,
    showPaging: PropTypes.bool,
    showSave: PropTypes.bool,
    showToggleTextView: PropTypes.bool,
    showOptionButton: PropTypes.bool,
    showFilterButton: PropTypes.bool,
    showAddColumn: PropTypes.bool,
    showInfoButton: PropTypes.bool,
    showSearchButton: PropTypes.bool,
    showPropertySheetButton: PropTypes.bool,
    showHeader: PropTypes.bool,
    leftButtons: PropTypes.arrayOf(PropTypes.func),   // an array of functions that returns a button-like component laid out on the left side of this table header.
    rightButtons: PropTypes.arrayOf(PropTypes.func),  // an array of functions that returns a button-like component laid out on the right side of this table header.
    renderers: PropTypes.objectOf(
        PropTypes.shape({
            cellRenderer: PropTypes.func,
            headRenderer: PropTypes.func
        })
    ),
    rowHeightGetter: PropTypes.func,
    sx: PropTypes.object,
    slotProps: shape({
        root: object,     // because there are already too many props, this is used specifically to pass custom props to top level component
        meta: object,
        toolbar: object,
        table: object
    })

};

export function OverflowMarker({tbl_id, showText}) {
    if (isOverflow(tbl_id)) {
        return (
            <Tooltip color='warning' variant='outlined'
                     slotProps={{root:{sx:{width:'20em'}}}}
                     title='Query truncated by MAXREC; more rows may be available by repeating the query with a larger MAXREC.'>
                <Stack spacing={1} direction='row' alignItems='center'>
                    <WarningIcon color='warning'/>
                    {showText && <Typography level='title-sm' color='warning'>Result Truncated</Typography>}
                </Stack>
            </Tooltip>
        );
    }
}


function showTableOptionDialog(onChange, onOptionReset, clearFilter, tbl_ui_id, tbl_id) {

    const content = (
        <Stack height='65vh' width='65vw' overflow='hidden' sx={{resize:'both', minWidth:550, minHeight:200}}>
            <TablePanelOptions
                onChange={onChange}
                onOptionReset={onOptionReset}
                clearFilter={clearFilter}
                tbl_ui_id={tbl_ui_id}
                tbl_id={tbl_id}
            />
        </Stack>
    );

    showOptionsPopup({content, title: 'Table Options', modal: true, show: true});


}

function showTableInfoDialog(tbl_id)  {
    const content = (
        <Box width='65vw' height='65vh' minWidth={450} minHeight={200}
             sx={{resize:'both', overflow:'auto', position:'relative'}}>
            <TableInfo tbl_id={tbl_id}
                       p={0}
                       tabsProps={{
                           variant:'plain',
                           slotProps: {tabList: {sticky: 'top'}}
                       }}
            />
        </Box>
    );
    showOptionsPopup({content, title: 'Table Info', modal: true, show: true});
}

function showTablePropSheetDialog(tbl_id) {
    const {title=''} = getTblById(tbl_id) || {};
    showOptionsPopup({show: false});   // hide the dialog if one is currently opened
    const content = (
        <Stack height={450} width={650} overflow='hidden' sx={{resize:'both', minWidth:'40rem', minHeight:'15rem'}}>
            <PropertySheetAsTable tbl_id={tbl_id}/>
        </Stack>
    );
    defer(() => showOptionsPopup({content, title: 'Row Details: ' + title, modal: false, show: true}));
}

const stopPropagation= (ev) => ev.stopPropagation();


function ToolBar({tbl_id, tbl_ui_id, connector, tblState, slotProps}) {

    const uiState = useStoreConnector(() => getTableUiById(tbl_ui_id) || {columns:[]}, [tbl_ui_id]);
    const searchActions= getSearchActions();

    const {expandable, expandedMode, title, removable, help_id,
        showTitle, showInfoButton, showOptions, showToolbar,
        showOptionButton, showPaging, showSave, showFilterButton,showSearchButton,
        totalRows, showLoading, showFilters, textView,
        currentPage, pageSize, showToggleTextView, showPropertySheetButton,
        filterCount, } = tblState;
    const showPropertySheet = showPropertySheetButton ?? getAppOptions()?.table?.showPropertySheetButton ?? true;

    const toggleFilter = () => connector.onOptionUpdate({showFilters: !showFilters});
    const toggleTextView = () => connector.onToggleTextView(!textView);
    const clearFilter = () => connector.applyFilterChanges({filterInfo: '', sqlFilter: ''});
    const toggleOptions = () => connector.onToggleOptions(!showOptions);
    const onOptionUpdate = (value) => connector.onOptionUpdate(value);
    const onOptionReset = () => connector.onOptionReset();

    const expandTable = () => {
        dispatchTblExpanded(tbl_ui_id, tbl_id);
        dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.tables);
    };

    let {leftButtons, rightButtons, showAddColumn} = tblState;
    showAddColumn = isClientTable(tbl_id) ? false : showAddColumn;
    // converts the additional left/right buttons into elements
    leftButtons =   leftButtons && leftButtons
        .map((f) => f(uiState))
        .map( (c, idx) => get(c, 'props.key') ? c : React.cloneElement(c, {key: idx})); // insert key prop if missing
    rightButtons = rightButtons && rightButtons
        .map((f) => f(uiState))
        .map( (c, idx) => get(c, 'props.key') ? c : React.cloneElement(c, {key: idx})); // insert key prop if missing

    const showOptionsDialog = () => showTableOptionDialog(onOptionUpdate, onOptionReset, clearFilter, tbl_ui_id, tbl_id);
    const showInfoDialog = () => showTableInfoDialog(tbl_id);

    if (showOptionButton && !showToolbar) {
        return (
            <SettingsButton tip={TT_OPTIONS}
                            onClick={showOptionsDialog}
                            iconButtonSize='25px'
                            slotProps={{
                                root: {
                                    sx: {position:'absolute', top:-1, right:-2, zIndex: 2,
                                        '& button': {p:0}
                                    },
                                    component: Sheet,
                                    variant: 'plain'
                                }
                            }}
            />
        );
    }

    if (!showToolbar) return null;

    return (
        <Sheet component={Stack} variant='soft' className='FF-Table-Toolbar' direction='row' justifyContent='space-between' width={1} {...slotProps?.toolbar}>
            <LeftToolBar {...{tbl_id, title, removable, showTitle, leftButtons}}/>
            <Stack direction='row' spacing={1} alignItems='center'>
                {showPaging && <PagingBar {...{currentPage, pageSize, showLoading, totalRows, callbacks:connector}} /> }
                <OverflowMarker tbl_id={tbl_id}/>
            </Stack>
            <Stack direction='row' alignItems='center'>
                {rightButtons}
                {showSearchButton &&  isTableActionsDropVisible(searchActions,tbl_id ) &&
                <ActionsDropDownButton {...{searchActions, tbl_id}} tip='Search drop down: search based on table'/>
                }
                {showFilterButton && filterCount > 0 &&
                <ClearFilterButton tip={TT_CLEAR_FILTER}
                                   onClick={clearFilter}/>
                }
                {showFilterButton &&
                <FilterButton  badgeCount={filterCount}
                               onClick={toggleFilter}/>
                }
                {showToggleTextView &&
                    textView ? <TableViewButton onClick={toggleTextView}/>
                             : <TextViewButton onClick={toggleTextView}/>
                }
                {showSave &&
                <SaveButton tip={TT_SAVE} onClick={showTableDownloadDialog({tbl_id, tbl_ui_id})}/>
                }
                {showAddColumn && <AddColumnBtn tbl_id={tbl_id} tbl_ui_id={tbl_ui_id}/> }
                {showInfoButton &&
                <InfoButton tip={TT_INFO} onClick={showInfoDialog}/>
                }
                {showPropertySheet &&
                <PropertySheetButton tip={TT_PROPERTY_SHEET}
                               onClick={() => showTablePropSheetDialog(tbl_id)}/>
                }
                {showOptionButton &&
                <SettingsButton tip={TT_OPTIONS}
                               onClick={showOptionsDialog}/>
                }
                { expandable && !expandedMode &&
                <ExpandButton tip={TT_EXPAND} onClick={expandTable}/>
                }
                { help_id && <HelpIcon helpId={help_id} />}
            </Stack>
        </Sheet>
    );
}


function LeftToolBar({tbl_id, title, removable, showTitle, leftButtons}) {
    const style = {display: 'inline-flex', alignItems: 'center'};
    const lbStyle = showTitle ? {...style, paddingLeft:10, alignSelf:'center'} : style;

    const doclinkUrl = getMetaEntry(tbl_id, META.doclink.url);
    if (doclinkUrl) {
        const doclinkLabel = getMetaEntry(tbl_id, META.doclink.label, 'Data Help');
        const doclinkDesc = getMetaEntry(tbl_id, META.doclink.desc) || doclinkLabel;

        const doclink = (
            <Stack direction='row' key={`doclink-${tbl_id}`}>
                {leftButtons?.length && <ToolbarHorizontalSeparator/>}
                <a href={doclinkUrl} target='doclink' title={doclinkDesc} key={doclinkUrl} style={{ textDecoration: 'none'}}>
                    <ToolbarButton text={doclinkLabel}/>
                </a>
                <ToolbarHorizontalSeparator/>
            </Stack>
        );
        if (leftButtons) {
            leftButtons.push(doclink);
        } else {
            leftButtons = [doclink];
        }
    }

    return (
        <Stack direction='row' style={style}>
            { showTitle && <Title {...{title, removable, tbl_id}}/>}
            {leftButtons && <Stack direction='row' spacing={1} style={lbStyle}>{leftButtons}</Stack>}
        </Stack>
    );
}

function Title({title, removable, tbl_id}) {
    const titleUi = isString(title) ? <Typography level='body-sm' noWrap={true} title={title}>{truncate(title)}</Typography> : title;
    return (
        <Stack direction='row' alignSelf='start' sx={{ml:1/4}}>
            {titleUi}
            {removable &&
                <Tooltip title='Remove table'>
                    <ChipDelete sx={{'--Chip-deleteSize': '1.2rem'}} onClick={() => dispatchTableRemove(tbl_id)}/>
                </Tooltip>
            }
        </Stack>
    );
}


function NotReady({showTitle, tbl_id, title, removable, backgroundable, error}) {
    if (backgroundable) {
        const height = showTitle ? 'calc(100% - 20px)': '100%';
        return (
            <div style={{position: 'relative', width: '100%', height: '100%', border: 'solid 1px rgba(0,0,0,.2)', boxSizing: 'border-box'}}>
                {showTitle && <Title {...{title, removable, tbl_id}}/>}
                <div style={{height, position: 'relative'}}>
                    <BgMaskPanel componentKey={makeBgKey(tbl_id)}/>
                </div>
            </div>
        );
    } else {
        const prevReq = getResultSetRequest(tbl_id);
        const reloadTable = () => {
            dispatchTableFetch(JSON.parse(prevReq));
        };
        if (error) {
            return <TableErrorMsg {...{error, prevReq, reloadTable}}/>;
        } else {
            return <TableMask/>;
        }
    }
}

export function TableErrorMsg({error, prevReq, reloadTable, ...props}) {
    const {message, type, cause} = parseError(error);
    return (
        <Stack spacing={1} m='auto' width={.8} height={1} justifyContent='center' {...props}>
            <Typography level='title-lg' color='danger'>{message}</Typography>
            { cause && (
                <Stack>
                    <Stack direction='row'>
                        <Typography level='title-lg' mr={1}>Cause:</Typography>
                        {type && <Typography level='body-md'>[{type}]</Typography>}
                    </Stack>
                    <Typography level='body-md' ml={1}>{cause}</Typography>
                </Stack>
            )}
            {prevReq && <Button color='neutral' variant='solid' onClick={reloadTable} sx={{alignSelf: 'baseline'}}>Back</Button>}
        </Stack>
    );
}
