/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, truncate, get} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../../Firefly.js';
import * as TblUtil from '../TableUtil.js';
import {dispatchTableRemove, dispatchTblExpanded, dispatchTableSearch} from '../TablesCntlr.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTableView} from './BasicTableView.jsx';
import {TableConnector} from '../TableConnector.js';
import {SelectInfo} from '../SelectInfo.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {showTableDownloadDialog} from './TableSave.jsx';
import {showOptionsPopup} from '../../ui/PopupUtil.jsx';
import {BgMaskPanel} from '../../core/background/BgMaskPanel.jsx';


import FILTER from 'html/images/icons-2014/24x24_Filter.png';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import OPTIONS from 'html/images/icons-2014/24x24_GearsNEW.png';

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
            totalRows, showLoading, columns, showUnits, showFilters, textView,
            tbl_id, error, startIdx, hlRowIdx, currentPage, pageSize, selectInfo, showMask,
            filterInfo, filterCount, sortInfo, data, bgStatus} = this.state;
        var {leftButtons, rightButtons} =  this.state;
        const {tbl_ui_id} = this.tableConnector;

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

        const showOptionsDialog = () => showTableOptionDialog(this.onOptionUpdate, this.onOptionReset, tbl_ui_id);

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
                                <div onClick={showTableDownloadDialog({tbl_id, tbl_ui_id})}
                                     title={TT_SAVE}
                                     className='PanelToolbar__button save'/> }
                                {showOptionButton &&
                                <div style={{marginLeft: '4px'}}
                                     title={TT_OPTIONS}
                                     onClick={showOptionsDialog}
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
                                    tableConnector, renderers, tbl_ui_id} }
                            />
                            {showOptionButton && !showToolbar &&
                            <img className='TablePanel__options--small'
                                 src={OPTIONS}
                                 title={TT_OPTIONS}
                                 onClick={showOptionsDialog}/>
                            }

                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

function showTableOptionDialog(onChange, onOptionReset, tbl_ui_id) {

    const content = (
         <div className='TablePanelOptionsWrapper'>
               <TablePanelOptions
                  onChange={onChange}
                  onOptionReset={onOptionReset}
                  tbl_ui_id={tbl_ui_id}
               />
         </div>
    );

    showOptionsPopup({content, title: 'Table Options', modal: true,show: true});


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

function Loading({showTitle, tbl_id, title, removable, bgStatus}) {
    const height = showTitle ? 'calc(100% - 20px)': '100%';
    return (
        <div style={{position: 'relative', width: '100%', height: '100%', border: 'solid 1px rgba(0,0,0,.2)', boxSizing: 'border-box'}}>
            {showTitle && <Title {...{title, removable, tbl_id}}/>}
            <div style={{height, position: 'relative'}}>
                <BgMaskPanel componentKey={TblUtil.makeBgKey(tbl_id)}/>
            </div>
        </div>
    );
}

function TableError({tbl_id, message}) {
    const prevReq = TblUtil.getResultSetRequest(tbl_id);
    const reloadTable = () => {
        dispatchTableSearch(JSON.parse(prevReq));
    };
    return (
        <div className='TablePanel__error'>
            <div style={{textAlign: 'center'}}>
                <div style={{display: 'flex', flexDirection: 'column', margin: '5px 0'}}>
                    <b>Table Load Error:</b>
                    <pre style={{margin: '7px 0'}}>{message}</pre>
                </div>
                {prevReq && <button type='button' className='button std' onClick={reloadTable}>Back</button>}
            </div>
        </div>
    );
}
