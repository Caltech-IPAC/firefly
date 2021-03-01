/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, truncate, get} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../../core/ReduxFlux.js';
import * as TblUtil from '../TableUtil.js';
import {dispatchTableRemove, dispatchTblExpanded, dispatchTableFetch, dispatchTableAddLocal, dispatchTableFilter} from '../TablesCntlr.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTableView} from './BasicTableView.jsx';
import {TableInfo} from './TableInfo.jsx';
import {TableConnector} from '../TableConnector.js';
import {SelectInfo} from '../SelectInfo.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {showTableDownloadDialog} from './TableSave.jsx';
import {showOptionsPopup, showInfoPopup} from '../../ui/PopupUtil.jsx';
import {BgMaskPanel} from '../../core/background/BgMaskPanel.jsx';
import {CollapsiblePanel} from '../../ui/panel/CollapsiblePanel.jsx';

//import INFO from 'html/images/icons-2014/24x24_Info.png';
import FILTER from 'html/images/icons-2014/24x24_Filter.png';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import OPTIONS from 'html/images/icons-2014/24x24_GearsNEW.png';
import {ObjectTree} from './ObjectTree.jsx';
import {Logger} from '../../util/Logger.js';

const logger = Logger('Tables').tag('TablePanel');

const TT_INFO = 'Show additional table info';
const TT_OPTIONS = 'Edit Table Options';
const TT_SAVE = 'Save the content as an IPAC, CSV, or TSV table';
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
        var {tbl_id, tbl_ui_id, tableModel, ...options} = props;

        if (!tbl_id && tableModel) {
            if (!tableModel.tbl_id) {
                tableModel.tbl_id = TblUtil.uniqueTblId();
            }
            tbl_id = tableModel.tbl_id;
        }
        tbl_ui_id = tbl_ui_id || TblUtil.uniqueTblUiId();
        this.tableConnector = TableConnector.newInstance(tbl_id, tbl_ui_id, tableModel, options);
        const uiState = TblUtil.getTableUiById(tbl_ui_id);
        return Object.assign({}, this.props, uiState);
    }

    UNSAFE_componentWillReceiveProps(props) {
        if (!shallowequal(this.props, props)) {
            const {tableModel} = this.props;
            if (tableModel !== props.tableModel) {
                // table model changed..
                dispatchTableAddLocal(props.tableModel, undefined, false);
            }
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
        const {tbl_id} = this.tableConnector;
        dispatchTableFilter({tbl_id, filters: '', sqlFilter: ''});
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
            showToolbar, showTitle, showInfoButton, showMetaInfo,
            showOptionButton, showPaging, showSave, showFilterButton,
            totalRows, showLoading, columns, showUnits, allowUnits, showTypes, showFilters, textView,
            tbl_id, error, startIdx, hlRowIdx, currentPage, pageSize, selectInfo, showMask, showToggleTextView,
            filterInfo, filterCount, sortInfo, data, backgroundable, highlightedRowHandler, cellRenderers} = this.state;
        var {leftButtons, rightButtons} =  this.state;
        const {tbl_ui_id} = this.tableConnector;

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

        const showOptionsDialog = () => showTableOptionDialog(this.onOptionUpdate, this.onOptionReset, tbl_ui_id, tbl_id);
        const showInfoDialog = () => showTableInfoDialog(tbl_id);

        const tstate = getTableState(this.state);
        logger.debug(`render.. state:[${tstate}] -- ${tbl_id}`);

        if (tstate === 'ERROR')     return <TableError {...{error, tbl_id, message: error}}/>;
        if (tstate === 'LOADING')   return <Loading {...{showTitle, tbl_id, title, removable, backgroundable}}/>;

        return (
            <div style={{ position: 'relative', width: '100%', height: '100%'}}>
                <div className='TablePanel'>
                    {showMetaInfo && <MetaInfo tbl_id={tbl_id} /> }
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
                                {showToggleTextView && <div onClick={this.toggleTextView}
                                     title={TT_VIEW}
                                     className={viewIcoStyle}/>}
                                {showSave &&
                                <div onClick={showTableDownloadDialog({tbl_id, tbl_ui_id})}
                                     title={TT_SAVE}
                                     className='PanelToolbar__button save'/> }
                                {showInfoButton &&
                                <div style={{marginLeft: '4px'}}
                                     title={TT_INFO}
                                     onClick={showInfoDialog}
                                     className='PanelToolbar__button info'/> }
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
                                { ...{columns, data, hlRowIdx, rowHeight, selectable, showUnits, allowUnits, showTypes, showFilters,
                                    selectInfoCls, filterInfo, sortInfo, textView, showMask, currentPage,
                                    tableConnector, renderers, tbl_ui_id, highlightedRowHandler, startIdx, cellRenderers} }
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

function getTableState(state) {
    const {error, columns} = state;
    if (error) return 'ERROR';
    if (isEmpty(columns)) return 'LOADING';
    return 'OK';
}


function showTableOptionDialog(onChange, onOptionReset, tbl_ui_id, tbl_id) {

    const content = (
         <div className='TablePanelOptionsWrapper'>
               <TablePanelOptions
                  onChange={onChange}
                  onOptionReset={onOptionReset}
                  tbl_ui_id={tbl_ui_id}
                  tbl_id={tbl_id}
               />
         </div>
    );

    showOptionsPopup({content, title: 'Table Options', modal: true, show: true});


}

function showTableInfoDialog(tbl_id)  {
    const content = (
        <div className='TablePanelInfoWrapper'>
            <TableInfo tbl_id={tbl_id}/>
        </div>
    );
    showOptionsPopup({content, title: 'Table Info', modal: true, show: true});
}

const stopPropagation= (ev) => ev.stopPropagation();


TablePanel.propTypes = {
    tbl_id: PropTypes.string,
    tbl_ui_id: PropTypes.string,
    tableModel: PropTypes.object,
    title: PropTypes.string,
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
    showInfoButton: PropTypes.bool,
    leftButtons: PropTypes.arrayOf(PropTypes.func),   // an array of functions that returns a button-like component laid out on the left side of this table header.
    rightButtons: PropTypes.arrayOf(PropTypes.func),  // an array of functions that returns a button-like component laid out on the right side of this table header.
    renderers: PropTypes.objectOf(
        PropTypes.shape({
            cellRenderer: PropTypes.func,
            headRenderer: PropTypes.func
        })
    )
};

TablePanel.defaultProps = {
    showMetaInfo: false,
    showUnits: false,
    allowUnits: true,
    showFilters: false,
    showToolbar: true,
    showTitle: true,
    showPaging: true,
    showSave: true,
    showOptionButton: true,
    showFilterButton: true,
    showInfoButton: false,
    showTypes: true,
    selectable: true,
    expandedMode: false,
    expandable: true,
    showToggleTextView: true,
    border: true,
    pageSize: 100
};

export function MetaInfo({tbl_id, style, isOpen=false}) {
    const contentStyle={display: 'flex', flexDirection: 'column', maxHeight: 200, overflow: 'auto', paddingBottom: 1};
    const wrapperStyle={width: '100%'};

    if (!TblUtil.hasAuxData(tbl_id)) {
        return null;
    }
    const {keywords, links, params, resources, groups} = TblUtil.getTblById(tbl_id);

    return (
        <div className='TablePanel__meta' style={style}>
            { !isEmpty(keywords) &&
            <CollapsiblePanel componentKey={tbl_id + '-meta'} header='Table Meta' {...{isOpen, contentStyle, wrapperStyle}}>
                {keywords.concat()                                             // make a copy so the original array does not mutate
                    .filter( ({key}) => key)
                    .sort(({key:k1}, {key:k2}) => (k1+'').localeCompare(k2+''))        // sort it by key
                    .map(({value, key}, idx) => {                              // map it into html elements
                        return (
                            <div key={'keywords-' + idx} style={{display: 'inline-flex'}}>
                                <Keyword label={key} value={value}/>
                            </div>
                        );
                    })
                }
            </CollapsiblePanel>
            }
            { !isEmpty(params) &&
            <CollapsiblePanel componentKey={tbl_id + '-params'} header='Table Params' {...{isOpen, contentStyle, wrapperStyle}}>
                {params.concat()                                                          // same logic as keywords, but sort by name
                    .sort(({name:k1}, {name:k2}) => k1.localeCompare(k2))
                    .map(({name, value, type='N/A'}, idx) => {
                        return (
                            <div key={'params-' + idx} style={{display: 'inline-flex'}}>
                                <Keyword label={`${name}(${type})`} value={value}/>
                            </div>
                        );
                    })
                }
            </CollapsiblePanel>
            }
            { !isEmpty(groups) &&
            <CollapsiblePanel componentKey={tbl_id + '-groups'} header='Groups' {...{isOpen, contentStyle, wrapperStyle}}>
                {groups.map((rs, idx) => {
                        const showValue = () => showInfoPopup(
                            <div style={{whiteSpace: 'pre'}}>
                                <ObjectTree data={rs} title={<b>Group</b>} className='MetaInfo__tree'/>
                            </div> );
                        return (
                            <div key={'groups-' + idx} style={{display: 'inline-flex'}}>
                                { rs.ID && <Keyword label='ID' value={rs.ID}/> }
                                { rs.name && <Keyword label='name' value={rs.name}/> }
                                { rs.UCD && <Keyword label='UCD' value={rs.UCD}/> }
                                { rs.utype && <Keyword label='utype' value={rs.utype}/> }
                                <a className='ff-href' onClick={showValue}> show value</a>
                            </div>
                        );
                    })
                }
            </CollapsiblePanel>
            }
            { !isEmpty(links) &&
            <CollapsiblePanel componentKey={tbl_id + '-links'} header='Links' {...{isOpen, contentStyle, wrapperStyle}}>
                {links.map((l, idx) => {
                        const dispVal = l.value || l.href;
                        return (
                            <div key={'links-' + idx} style={{display: 'inline-flex'}}>
                                { l.ID && <Keyword label='ID' value={l.ID}/> }
                                { l.role && <Keyword label='role' value={l.role}/> }
                                { l.type && <Keyword label='type' value={l.type}/> }
                                <a style={{whiteSpace: 'nowrap', maxWidth: 300}} href={l.href} title={l.title || dispVal}>{dispVal}</a>
                            </div>
                        );
                    })
                }
            </CollapsiblePanel>
            }
            { !isEmpty(resources) &&
            <CollapsiblePanel componentKey={tbl_id + '-resources'} header='Resources' {...{isOpen, contentStyle, wrapperStyle}}>
                {resources.map((rs, idx) => {
                        const showValue = () => showInfoPopup(
                                    <div style={{whiteSpace: 'pre'}}>
                                        <ObjectTree data={rs} title={<b>Resource</b>} className='MetaInfo__tree'/>
                                    </div> );
                        return (
                            <div key={'resources-' + idx} style={{display: 'inline-flex'}}>
                                { rs.ID && <Keyword label='ID' value={rs.ID}/> }
                                { rs.name && <Keyword label='name' value={rs.name}/> }
                                { rs.type && <Keyword label='type' value={rs.type}/> }
                                { rs.utype && <Keyword label='utype' value={rs.utype}/> }
                                <a className='ff-href' onClick={showValue}> show value</a>
                            </div>
                        );
                    })
                }
            </CollapsiblePanel>
            }
        </div>
    );
}

function Keyword({style={}, label, value}) {
    return (
        <React.Fragment>
            <div className='keyword-label'>{label}</div>
            <div title = {value} style={{whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', ...style}} className='keyword-value'>{value}</div>
        </React.Fragment>
    );
}

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

function Loading({showTitle, tbl_id, title, removable, backgroundable}) {
    const height = showTitle ? 'calc(100% - 20px)': '100%';
    const maskPanel = backgroundable ? <BgMaskPanel componentKey={TblUtil.makeBgKey(tbl_id)}/> : <div className='loading-mask'/>;
    return (
        <div style={{position: 'relative', width: '100%', height: '100%', border: 'solid 1px rgba(0,0,0,.2)', boxSizing: 'border-box'}}>
            {showTitle && <Title {...{title, removable, tbl_id}}/>}
            <div style={{height, position: 'relative'}}>
                {maskPanel}
            </div>
        </div>
    );
}

function TableError({tbl_id, message}) {
    const prevReq = TblUtil.getResultSetRequest(tbl_id);
    const reloadTable = () => {
        dispatchTableFetch(JSON.parse(prevReq));
    };
    return (
        <div className='TablePanel__error'>
            <div style={{textAlign: 'center'}}>
                <div style={{display: 'flex', flexDirection: 'column', margin: '5px 0'}}>
                    <b>Table Load Error:</b>
                    <pre style={{margin: '7px 0', whiteSpace: 'pre-wrap'}}>{message}</pre>
                </div>
                {prevReq && <button type='button' className='button std' onClick={reloadTable}>Back</button>}
            </div>
        </div>
    );
}


