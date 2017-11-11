/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, truncate, get, set, cloneDeep, omit} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../../Firefly.js';
import {download, updateSet} from '../../util/WebUtil.js';
import * as TblUtil from '../TableUtil.js';
import {dispatchTableRemove, dispatchTblExpanded, dispatchTblResultsRemove, dispatchTableSearch} from '../TablesCntlr.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTableView} from './BasicTableView.jsx';
import {TableConnector} from '../TableConnector.js';
import {SelectInfo} from '../SelectInfo.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {dispatchJobAdd} from '../../core/background/BackgroundCntlr.js';
import FILTER from 'html/images/icons-2014/24x24_Filter.png';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import OPTIONS from 'html/images/icons-2014/24x24_GearsNEW.png';
import {DEF_BASE_URL} from '../../core/JsonUtils.js';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {doDownloadWorkspace, workspacePopupMsg} from '../../ui/WorkspaceViewer.jsx';
import {DownloadOptionsDialog, fileNameValidator, getTypeData, validateFileName,
        WORKSPACE} from '../../ui/DownloadOptionsDialog.jsx';
import {WS_SERVER_PARAM, isWsFolder, isValidWSFolder,
        getWorkspacePath} from  '../../visualize/WorkspaceCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {INFO_POPUP} from '../../ui/PopupUtil.jsx';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';

const TT_OPTIONS = 'Edit Table Options';
const TT_SAVE = 'Save the content as an IPAC table';
const TT_TEXT_VIEW = 'Text View';
const TT_TABLE_VIEW = 'Table View';
const TT_CLEAR_FILTER = 'Remove all filters';
const TT_SHOW_FILTER = 'The Filter Panel can be used to remove unwanted data from the search results';
const TT_EXPAND = 'Expand this panel to take up a larger area';

export class TablePanel extends PureComponent {
    constructor(props) {
        super(props);

        this.toggleFilter = this.toggleFilter.bind(this);
        this.toggleTextView = this.toggleTextView.bind(this);
        this.clearFilter = this.clearFilter.bind(this);
        this.saveTable = this.saveTable.bind(this);
        this.toggleOptions = this.toggleOptions.bind(this);
        this.expandTable = this.expandTable.bind(this);
        this.onOptionUpdate = this.onOptionUpdate.bind(this);
        this.onOptionReset = this.onOptionReset.bind(this);
        this.setupInitState = this.setupInitState.bind(this);

        this.state = this.setupInitState(props);
    }

    setupInitState(props) {
        var {tbl_id, tbl_ui_id, tableModel, showUnits, showFilters, pageSize} = props;

        if (!tbl_id && tableModel) {
            if (!tableModel.tbl_id) {
                tableModel.tbl_id = TblUtil.uniqueTblId();
            }
            tbl_id = tableModel.tbl_id;
        }
        tbl_ui_id = tbl_ui_id || TblUtil.uniqueTblUiId();
        this.tableConnector = TableConnector.newInstance(tbl_id, tbl_ui_id, tableModel, showUnits, showFilters, pageSize);
        const uiState = TblUtil.getTableUiById(tbl_ui_id);
        return Object.assign({}, this.props, uiState);
    }

    componentWillReceiveProps(props) {
        if (!shallowequal(this.props, props)) {
            this.setState(this.setupInitState(props));
        }
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.tableConnector.onMount();
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            const {tbl_ui_id} = this.tableConnector;
            const uiState = TblUtil.getTableUiById(tbl_ui_id) || {columns: []};
            this.setState(uiState);
        }
    }

    toggleFilter() {
        this.tableConnector.onOptionUpdate({showFilters: !this.state.showFilters});
    }
    toggleTextView() {
        this.tableConnector.onToggleTextView(!this.state.textView);
    }
    clearFilter() {
        this.tableConnector.onFilter('');
    }
    saveTable() {
        const {tbl_ui_id} = this.tableConnector;
        if (this.tableConnector.tableModel) {
            TblUtil.getAsyncTableSourceUrl(tbl_ui_id).then((url) => download(url));
        } else {
            download(TblUtil.getTableSourceUrl(tbl_ui_id));
        }
    }
    toggleOptions() {
        this.tableConnector.onToggleOptions(!this.state.showOptions);
    }
    expandTable() {
        const {tbl_ui_id, tbl_id} = this.tableConnector;
        dispatchTblExpanded(tbl_ui_id, tbl_id);
        dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.tables);
    }
    onOptionUpdate(value) {
        this.tableConnector.onOptionUpdate(value);
    }
    onOptionReset() {
        this.tableConnector.onOptionReset();
    }



    render() {
        const {tableConnector} = this;
        const { selectable, expandable, expandedMode, border, renderers, title, removable, rowHeight, help_id,
                showToolbar, showTitle, showOptionButton, showPaging, showSave, showFilterButton,
                totalRows, showLoading, columns, showOptions, showUnits, showFilters, textView, optSortInfo,
                tbl_id, error, startIdx, hlRowIdx, currentPage, pageSize, selectInfo, showMask,
                filterInfo, filterCount, sortInfo, data, bgStatus} = this.state;
        var {leftButtons, rightButtons} =  this.state;

        if (error) return <TableError {...{error, tbl_id, message: error}}/>;
        if (isEmpty(columns)) return <Loading {...{showTitle, tbl_id, title, removable, bgStatus}}/>;

        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        const viewIcoStyle = 'PanelToolbar__button ' + (textView ? 'tableView' : 'textView');
        const tableTopPos = showToolbar && (leftButtons && showTitle ? 41 : 29) || 0;
        const TT_VIEW = textView ? TT_TABLE_VIEW : TT_TEXT_VIEW;

        // converts the additional left/right buttons into elements
        leftButtons =   leftButtons && leftButtons
                        .map((f) => f(this.state))
                        .map( (c, idx) => get(c, 'props.key') ? c : React.cloneElement(c, {key: idx})); // insert key prop if missing
        rightButtons = rightButtons && rightButtons
                        .map((f) => f(this.state))
                        .map( (c, idx) => get(c, 'props.key') ? c : React.cloneElement(c, {key: idx})); // insert key prop if missing

        return (
            <div style={{ position: 'relative', width: '100%', height: '100%'}}>
            <div className='TablePanel'>
                <div className={'TablePanel__wrapper' + (border ? '--border' : '')}>
                    {showToolbar &&
                        <div className='PanelToolbar TablePanel__toolbar'>
                            <LeftToolBar {...{tbl_id, title, removable, showTitle, leftButtons}}/>
                            {showPaging && <PagingBar {...{currentPage, pageSize, showLoading, totalRows, callbacks:tableConnector}} /> }
                            <div className='PanelToolbar__group'>
                                {rightButtons}
                                {showFilterButton && filterCount > 0 &&
                                    <div onClick={this.clearFilter}
                                            title={TT_CLEAR_FILTER}
                                            className='PanelToolbar__button clearFilters'/>}
                                {showFilterButton &&
                                    <ToolbarButton icon={FILTER}
                                                   tip={TT_SHOW_FILTER}
                                                   visible={true}
                                                   badgeCount={filterCount}
                                                   onClick={this.toggleFilter}/>
                                }
                                <div onClick={this.toggleTextView}
                                        title={TT_VIEW}
                                        className={viewIcoStyle}/>
                                {showSave &&
                                    <div onClick={showTableDownloadDialog(tableConnector)}
                                            title={TT_SAVE}
                                            className='PanelToolbar__button save'/> }
                                {showOptionButton &&
                                    <div style={{marginLeft: '4px'}}
                                            title={TT_OPTIONS}
                                            onClick={this.toggleOptions}
                                            className='PanelToolbar__button options'/> }
                                { expandable && !expandedMode &&
                                    <div className='PanelToolbar__button' onClick={this.expandTable} title={TT_EXPAND}>
                                        <img src={OUTLINE_EXPAND}/>
                                    </div>}
                                { help_id && <div style={{marginTop:-10}}> <HelpIcon helpId={help_id} /> </div>}
                            </div>
                        </div>
                    }
                    <div className='TablePanel__table' style={{top: tableTopPos}}
                        onClick={stopPropagation}
                        onTouchStart={stopPropagation}
                        onMouseDown={stopPropagation}
                    >
                            <BasicTableView
                                callbacks={tableConnector}
                                { ...{columns, data, hlRowIdx, rowHeight, selectable, showUnits, showFilters,
                                    selectInfoCls, filterInfo, sortInfo, textView, showMask, currentPage,
                                    tableConnector, renderers} }
                            />
                            {showOptionButton && !showToolbar &&
                            <img className='TablePanel__options--small'
                                 src={OPTIONS}
                                 title={TT_OPTIONS}
                                 onClick={this.toggleOptions}/>
                            }
                            {showOptions &&

                            <TablePanelOptions
                                onChange={this.onOptionUpdate}
                                onOptionReset={this.onOptionReset}
                                toggleOptions={this.toggleOptions}
                                { ...{columns, optSortInfo, filterInfo, pageSize, showUnits, showFilters, showToolbar}}
                            /> }
                    </div>
                </div>
            </div>
            </div>
        );
    }
}

const stopPropagation= (ev) => ev.stopPropagation();


TablePanel.propTypes = {
    tbl_id: PropTypes.string,
    tbl_ui_id: PropTypes.string,
    tableModel: PropTypes.object,
    pageSize: PropTypes.number,
    rowHeight: PropTypes.number,
    selectable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    expandable: PropTypes.bool,
    border: PropTypes.bool,
    title: PropTypes.string,
    help_id: PropTypes.string,
    removable: PropTypes.bool,
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    showToolbar: PropTypes.bool,
    showTitle: PropTypes.bool,
    showPaging: PropTypes.bool,
    showSave: PropTypes.bool,
    showOptionButton: PropTypes.bool,
    showFilterButton: PropTypes.bool,
    renderers: PropTypes.objectOf(
        PropTypes.shape({
            cellRenderer: PropTypes.func,
            headRenderer: PropTypes.func
        })
    )
};

TablePanel.defaultProps = {
    showUnits: false,
    showFilters: false,
    showToolbar: true,
    showTitle: true,
    showPaging: true,
    showSave: true,
    showOptionButton: true,
    showFilterButton: true,
    selectable: true,
    expandedMode: false,
    expandable: true,
    border: true,
    pageSize: 100
};

// eslint-disable-next-line
function LeftToolBar({tbl_id, title, removable, showTitle, leftButtons}) {
    const style = {display: 'flex'};
    if (leftButtons) {
        Object.assign(style, {flexDirection: 'column', justifyContent: 'center'});
    }
    return (
        <div style={style}>
            { showTitle && <Title {...{title, removable, tbl_id}}/>}
            {leftButtons && <div>{leftButtons}</div>}
        </div>
    );
}

// eslint-disable-next-line
function Title({title, removable, tbl_id}) {
    return (
        <div className='TablePanel__title'>
            <div style={{display: 'inline-block', marginLeft: 5, marginTop: 2}}
                 title={title}>{truncate(title)}</div>
            {removable &&
            <div style={{right: -5}} className='btn-close'
                 title='Remove Tab'
                 onClick={() => dispatchTableRemove(tbl_id)}/>
            }
        </div>
    );
}

// eslint-disable-next-line
function Loading({showTitle, tbl_id, title, removable, bgStatus}) {
    const toBg = () => {
        dispatchTblResultsRemove(tbl_id);
        dispatchJobAdd(bgStatus);
    };
    const height = showTitle ? 'calc(100% - 20px)': '100%';
    
    return (
        <div style={{position: 'relative', width: '100%', height: '100%', border: 'solid 1px rgba(0,0,0,.2)', boxSizing: 'border-box'}}>
            {showTitle && <Title {...{title, removable, tbl_id}}/>}
            <div style={{height, position: 'relative'}}>
                <div className='loading-mask'/>
                {bgStatus &&
                <div className='TablePanel__mask'>
                    <button type='button' className='TablePanel__mask--button button std' onClick={toBg}>Send to background</button>
                </div>
                }
            </div>
        </div>
    );
}

function TableError({error, tbl_id, message}) {
    const {request} = TblUtil.getTblById(tbl_id);
    const canReset = get(request, 'filters') || get(request, 'sortInfo');
    const reloadTable = () => {
        const origRequest = omit(cloneDeep(request), 'filters', 'sortInfo');
        dispatchTableSearch(origRequest);
    };
    return (
        <div className='TablePanel__error'>
            <div>{message}</div>
            {canReset && <button type='button' className='button std' onClick={reloadTable}>Reload</button>}
        </div>
    );
}


const fKeyDef = {
    fileName: {fKey: 'fileName', label: 'Save as:'},
    location: {fKey: 'fileLocation', label: 'File Location:'},
    wsSelect: {fKey: 'wsSelect', label: ''},
    overWritable: {fKey: 'fileOverwritable', label: 'File overwritable: '}
};

const labelWidth = 100;
const defValues = {
    [fKeyDef.fileName.fKey]: Object.assign(getTypeData(fKeyDef.fileName.fKey, '',
        'Please enter a filename, a default name will be used if it is blank', fKeyDef.fileName.label, labelWidth), {validator: null}),
    [fKeyDef.location.fKey]: Object.assign(getTypeData(fKeyDef.location.fKey, 'isLocal',
        'select the location where the file is downloaded to', fKeyDef.location.label, labelWidth), {validator: null}),
    [fKeyDef.wsSelect.fKey]: Object.assign(getTypeData(fKeyDef.wsSelect.fKey, '',
        'workspace file system', fKeyDef.wsSelect.label, labelWidth), {validator: null}),
    [fKeyDef.overWritable.fKey]: Object.assign(getTypeData(fKeyDef.overWritable.fKey, '0',
        'File is overwritable', fKeyDef.overWritable.label, labelWidth), {validator: null})
};

const tblDownloadGroupKey = 'TABLE_DOWNLOAD_FORM';
export function showTableDownloadDialog(tableConnector) {
    return () => {
        const popupId = 'TABLE_DOWNLOAD_POPUP';
        const dialogWidth = 400;

        const startTableDownloadPopup = () => {
            const popup = (
                <PopupPanel title={'Download table'}>
                    <div style={{margin: 10, width: dialogWidth}}>
                        <FieldGroup groupKey={tblDownloadGroupKey} keepState={true}
                                    reducerFunc={TableDLReducer(tableConnector.tbl_ui_id)}>
                            <DownloadOptionsDialog fromGroupKey={tblDownloadGroupKey}
                                                   />
                        </FieldGroup>
                        <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 30}}>
                            <div style={{display: 'flex', width: '60%', alignItems: 'flex-end'}}>
                                <div style={{marginRight: 10}}>
                                    <CompleteButton
                                        groupKey={tblDownloadGroupKey}
                                        onSuccess={resultSuccess(tableConnector, popupId)}
                                        onFail={resultFail()}
                                        text={'Download'}/>
                                </div>
                                <div>
                                    <button type='button' className='button std hl'
                                            onClick={() => closePopup(popupId)}>Cancel
                                    </button>
                                </div>
                            </div>
                            <div style={{ textAlign:'right', marginRight: 10}}>
                                <HelpIcon helpId={'visualization.imageoptions'}/>
                            </div>
                        </div>
                    </div>
                </PopupPanel>
            );

            DialogRootContainer.defineDialog(popupId, popup);
            dispatchShowDialog(popupId);
        };

        startTableDownloadPopup();
    };
}

function TableDLReducer(tbl_ui_id) {
    return (inFields, action) => {
        const {request} = TblUtil.getTableUiById(tbl_ui_id) || {};

        if (!inFields) {
            const defV = Object.assign({}, defValues);

            set(defV, [fKeyDef.wsSelect.fKey, 'value'], '');
            set(defV, [fKeyDef.wsSelect.fKey, 'validator'], isWsFolder());
            set(defV, [fKeyDef.fileName.fKey, 'validator'], fileNameValidator(tblDownloadGroupKey));
            set(defV, [fKeyDef.fileName.fKey, 'value'], get(request, 'META_INFO.title'));
            return defV;
        } else {
            switch (action.type) {
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    inFields = updateSet(inFields, [fKeyDef.fileName.fKey, 'value'], get(request, 'META_INFO.title'));
                    break;
            }
            return Object.assign({}, inFields);
        }
    };
}

function closePopup(popupId) {
    dispatchHideDialog(popupId);
    if (isDialogVisible(INFO_POPUP)) {
        dispatchHideDialog(INFO_POPUP);
    }
}


function resultFail() {
    return (request) => {
        const {wsSelect, fileLocation} = request;

        if (fileLocation === WORKSPACE) {
            if (!wsSelect) {
                workspacePopupMsg('please select a workspace folder', 'Save to workspace');
            } else {
                const isAFolder = isValidWSFolder(wsSelect);
                if (!isAFolder.valid) {
                    workspacePopupMsg(isAFolder.message, 'Save to workspace');
                }
            }
        }
    };
}

function resultSuccess(tableConnector, popupId) {
    return (request) => {
        const {fileName, fileLocation, wsSelect} = request || {};
        const isWorkspace = () => (fileLocation && fileLocation === WORKSPACE);

        if (isWorkspace()) {
            if (!validateFileName(wsSelect, fileName)) return false;
        }

        const getOtherParams = (fName) => {
            return (!isWorkspace()) ? {file_name: fName}
                                    : {wsCmd: ServerParams.WS_PUT_TABLE_FILE,
                                      [WS_SERVER_PARAM.currentrelpath.key]: getWorkspacePath(wsSelect, fName),
                                      [WS_SERVER_PARAM.newpath.key] : fName,
                                      [WS_SERVER_PARAM.should_overwrite.key]: true};

        };

        const downloadFile = (urlOrOp) => {
            if (isWorkspace()) {
                doDownloadWorkspace(DEF_BASE_URL,
                                    {params: urlOrOp});
            } else {
                download(urlOrOp);
            }

            if (popupId) {
                dispatchHideDialog(popupId);
                if (isDialogVisible(INFO_POPUP)) {
                    dispatchHideDialog(INFO_POPUP);
                }
            }
        };

        const {tbl_ui_id} = tableConnector;
        if (tableConnector.tableModel) {
            TblUtil.getAsyncTableSourceUrl(tbl_ui_id, getOtherParams(fileName)).then((urlOrOp) => {
                downloadFile(urlOrOp);
            });
        } else {
            const urlOrOp = TblUtil.getTableSourceUrl(tbl_ui_id, getOtherParams(fileName));
            downloadFile(urlOrOp);
        }
    };
}