/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get} from 'lodash';

import {flux} from '../../Firefly.js';
import {download} from '../../util/WebUtil.js';
import * as TblUtil from '../TableUtil.js';
import {dispatchTableReplace, dispatchTableUiUpdate} from '../TablesCntlr.js';
import {TablePanelOptions} from './TablePanelOptions.jsx';
import {BasicTableView} from './BasicTableView.jsx';
import {TableConnector} from '../TableConnector.js';
import {SelectInfo} from '../SelectInfo.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {LO_EXPANDED, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';

import FILTER from 'html/images/icons-2014/24x24_Filter.png';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';

export class TablePanel extends Component {
    constructor(props) {
        super(props);
        var {tbl_id, tbl_ui_id, tableModel} = props;

        var isLocal = false;
        if (!tbl_id && tableModel) {
            tbl_id = get(tableModel, 'tbl_id');
            isLocal = true;
        }
        tbl_ui_id = tbl_ui_id || tbl_id + '-ui';
        this.tableConnector = TableConnector.newInstance(tbl_id, tbl_ui_id, isLocal);
        const uiState = TblUtil.findTableUiById(tbl_ui_id);
        this.state = uiState || {};
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        const {tableModel} = this.props;
        const {tbl_id, tbl_ui_id} = this.tableConnector;
        if (!get(this.state, 'tbl_id')) {
            dispatchTableUiUpdate({tbl_ui_id, tbl_id});
        }
        if (tableModel && isEmpty(this.state)) {
            dispatchTableReplace(tableModel);
        }
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    storeUpdate() {
        const {tbl_ui_id} = this.tableConnector;
        const uiState = TblUtil.findTableUiById(tbl_ui_id) || {columns: []};
        this.setState(uiState);
    }

    render() {
        const {selectable, expandable, expandedMode, border, renderers} = this.props;
        var {totalRows, request, showLoading, columns, showOptions, showUnits, showFilters, textView, colSortDir} = this.state;
        const {error, startIdx, hlRowIdx, highlightedRow, pageSize, selectInfo,
            filterInfo, filterCount, sortInfo, data} = this.state;
        const {tableConnector} = this;

        if (error) return <div className='TablePanel__error'>{error}</div>;
        if (isEmpty(columns))return <div style={{position: 'relative', flexGrow: 1}}><div msg='loading...' className='loading-mask'/></div>;

        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        const viewIcoStyle = 'tablepanel ' + (textView ? 'tableView' : 'textView');
        const origColumns = get(TblUtil.findTblById(this.tableConnector.tbl_id), 'tableData.columns');

        return (
            <div style={{ display: 'flex', flex: 'auto', flexDirection: 'column', overflow: 'hidden'}}>
                <div className={'TablePanel__wrapper' + (border ? ' border' : '')}>
                    <div role='toolbar' className='TablePanel__toolbar'>
                        <div className='group'>
                            <button style={{width:70}}>Download</button>
                        </div>

                        <PagingBar {...{highlightedRow, pageSize, showLoading, totalRows, callbacks:tableConnector}} />

                        <div className='group'>
                            {filterCount > 0 &&
                            <button onClick={() => tableConnector.onFilter('')} className='tablepanel clearFilters'/>}
                            <ToolbarButton icon={FILTER}
                                           tip='The Filter Panel can be used to remove unwanted data from the search results'
                                           visible={true}
                                           badgeCount={filterCount}
                                           onClick={() => tableConnector.onOptionUpdate({showFilters: !showFilters})}/>
                            <button onClick={() => tableConnector.onToggleTextView(!textView)} className={viewIcoStyle}/>
                            <button onClick={() => download(TblUtil.getTableSourceUrl(columns, request))}
                                    className='tablepanel save'/>
                            <button style={{marginLeft: '4px'}} onClick={() => tableConnector.onToggleOptions(!showOptions)}
                                    className='tablepanel options'/>
                            { expandable && !expandedMode &&
                                <button onClick={() => dispatchSetLayoutMode(LO_EXPANDED.tables)}>
                                    <img src={OUTLINE_EXPAND} title='Expand this panel to take up a larger area'/>
                                </button>}
                        </div>
                    </div>
                    <div className='TablePanel__table'>
                        <BasicTableView
                            columns={columns}
                            data={data}
                            hlRowIdx={hlRowIdx}
                            selectable={selectable}
                            showUnits={showUnits}
                            showFilters={showFilters}
                            selectInfoCls={selectInfoCls}
                            filterInfo={filterInfo}
                            sortInfo={sortInfo}
                            textView={textView}
                            callbacks={tableConnector}
                            renderers={renderers}
                        />
                        {showOptions && <TablePanelOptions
                            columns={columns}
                            origColumns={origColumns}
                            colSortDir={colSortDir}
                            pageSize={pageSize}
                            showUnits={showUnits}
                            showFilters={showFilters}
                            onChange={(v) => tableConnector.onOptionUpdate(v)}
                        /> }
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
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    selectable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    expandable: PropTypes.bool,
    showToolbar: PropTypes.bool,
    border: PropTypes.bool,
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
    selectable: true,
    expandedMode: false,
    expandable: true,
    showToolbar: true,
    border: true,
    pageSize: 50
};

