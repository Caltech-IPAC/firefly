/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, truncate} from 'lodash';
import {flux} from '../../Firefly.js';
import {download} from '../../util/WebUtil.js';
import * as TblUtil from '../TableUtil.js';
import {dispatchTableReplace, dispatchTableUiUpdate, dispatchTableRemove, dispatchTblExpanded} from '../TablesCntlr.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTableView} from './BasicTableView.jsx';
import {TableConnector} from '../TableConnector.js';
import {SelectInfo} from '../SelectInfo.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import FILTER from 'html/images/icons-2014/24x24_Filter.png';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';

const TT_OPTIONS = 'Edit Table Options';
const TT_SAVE = 'Save the content as an IPAC table';
const TT_TEXT_VIEW = 'Text View';
const TT_TABLE_VIEW = 'Table View';
const TT_CLEAR_FILTER = 'Remove all filters';
const TT_SHOW_FILTER = 'The Filter Panel can be used to remove unwanted data from the search results';
const TT_EXPAND = 'Expand this panel to take up a larger area';

export class TablePanel extends Component {
    constructor(props) {
        super(props);
        var {tbl_id, tbl_ui_id, tableModel} = props;

        if (!tbl_id && tableModel) {
            tbl_id = get(tableModel, 'tbl_id', TblUtil.uniqueTblId());
        }
        tbl_ui_id = tbl_ui_id || TblUtil.uniqueTblUiId();
        this.tableConnector = TableConnector.newInstance(tbl_id, tbl_ui_id, tableModel);
        const uiState = TblUtil.getTableUiById(tbl_ui_id);
        this.state = Object.assign({}, this.props, uiState);

        this.toggleFilter = this.toggleFilter.bind(this);
        this.toggleTextView = this.toggleTextView.bind(this);
        this.clearFilter = this.clearFilter.bind(this);
        this.saveTable = this.saveTable.bind(this);
        this.toggleOptions = this.toggleOptions.bind(this);
        this.expandTable = this.expandTable.bind(this);
        this.onOptionUpdate = this.onOptionUpdate.bind(this);
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        const {tableModel} = this.props;
        const {tbl_id, tbl_ui_id} = this.tableConnector;
        if (!get(this.state, 'tbl_id')) {
            dispatchTableUiUpdate({tbl_ui_id, tbl_id});
            if (tableModel) {
                dispatchTableReplace(tableModel);
            }
        }
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
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
        download(TblUtil.getTableSourceUrl(tbl_ui_id));
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

    render() {
        const {selectable, expandable, expandedMode, border, renderers, title, removable, rowHeight, help_id,
                showToolbar, showTitle, showOptionButton, showPaging, showSave, showFilterButton} = this.state;
        var {totalRows, showLoading, columns, showOptions, showUnits, showFilters, textView, optSortInfo} = this.state;
        const {tbl_id, error, startIdx, hlRowIdx, currentPage, pageSize, selectInfo, showMask,
                filterInfo, filterCount, sortInfo, data} = this.state;
        const {tableConnector} = this;

        const {onTableChanged} = this.props;

        if (error) return <div className='TablePanel__error'>{error}</div>;
        if (isEmpty(columns)) return <div style={{position: 'relative', width: '100%', height: '100%'}}><div msg='loading...' className='loading-mask'/></div>;

        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        const viewIcoStyle = 'tablepanel ' + (textView ? 'tableView' : 'textView');
        const origColumns = get(TblUtil.getTblById(this.tableConnector.tbl_id), 'tableData.columns');
        const tableTopPos = showToolbar ? 29 : 0;
        const TT_VIEW = textView ? TT_TABLE_VIEW : TT_TEXT_VIEW;

        return (
            <div style={{ position: 'relative', width: '100%', height: '100%'}}>
            <div style={{ display: 'flex', height: '100%', flexDirection: 'column', overflow: 'hidden'}}>
                <div className={'TablePanel__wrapper' + (border ? '--border' : '')}>
                    {showToolbar &&
                        <div role='toolbar' className='TablePanel__toolbar'>
                            {showTitle ? <TableTitle {...{tbl_id, title, removable}} /> : <div className='group'/>}
                            {showPaging && <PagingBar {...{currentPage, pageSize, showLoading, totalRows, callbacks:tableConnector}} /> }
                            <div className='group'>
                                {showFilterButton && filterCount > 0 &&
                                    <button onClick={this.clearFilter}
                                            title={TT_CLEAR_FILTER}
                                            className='tablepanel clearFilters'/>}
                                {showFilterButton &&
                                    <ToolbarButton icon={FILTER}
                                                   tip={TT_SHOW_FILTER}
                                                   visible={true}
                                                   badgeCount={filterCount}
                                                   onClick={this.toggleFilter}/>
                                }
                                <button onClick={this.toggleTextView}
                                        title={TT_VIEW}
                                        className={viewIcoStyle}/>
                                {showSave &&
                                    <button onClick={this.saveTable}
                                            title={TT_SAVE}
                                            className='tablepanel save'/> }
                                {showOptionButton &&
                                    <button style={{marginLeft: '4px'}}
                                            title={TT_OPTIONS}
                                            onClick={this.toggleOptions}
                                            className='tablepanel options'/> }
                                { expandable && !expandedMode &&
                                    <button onClick={this.expandTable} title={TT_EXPAND}>
                                        <img src={OUTLINE_EXPAND}/>
                                    </button>}
                                { help_id && <div style={{marginTop:-10}}> <HelpIcon helpId={help_id} /> </div>}
                            </div>
                        </div>
                    }
                    <div className='TablePanel__table' style={{top: tableTopPos}}>
                        <BasicTableView
                            columns={columns}
                            data={data}
                            hlRowIdx={hlRowIdx}
                            rowHeight = {rowHeight}
                            selectable={selectable}
                            showUnits={showUnits}
                            showFilters={showFilters}
                            selectInfoCls={selectInfoCls}
                            filterInfo={filterInfo}
                            sortInfo={sortInfo}
                            textView={textView}
                            showMask={showMask}
                            currentPage={currentPage}
                            callbacks={tableConnector}
                            renderers={renderers}
                            onTableChanged={onTableChanged}
                        />
                        {showOptions && <TablePanelOptions
                            columns={columns}
                            origColumns={origColumns}
                            optSortInfo={optSortInfo}
                            filterInfo={filterInfo}
                            pageSize={pageSize}
                            showUnits={showUnits}
                            showFilters={showFilters}
                            onChange={this.onOptionUpdate}
                            toggleOptions={this.toggleOptions}
                        /> }
                    </div>
                </div>
            </div>
            </div>
        );
    }
}


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
    onTableChanged: PropTypes.func,
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
    pageSize: 50
};

// eslint-disable-next-line
function TableTitle({tbl_id, title, removable}) {
    if (title) {
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
    } else return <div/>;
}