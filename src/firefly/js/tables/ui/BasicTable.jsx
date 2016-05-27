/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, set} from 'lodash';

import {flux} from '../../Firefly.js';
import * as TblUtil from '../TableUtil.js';
import {BasicTableView} from './BasicTableView.jsx';
import {TableConnector} from '../TableConnector.js';
import {dispatchTableReplace, dispatchTableUiUpdate} from '../TablesCntlr.js';
import {SelectInfo} from '../SelectInfo.js';

/**
 * this is no longer needed.  use TablePanel with showToolBar = false
 * @deprecated
 */
export class BasicTable extends Component {

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
        const uiState = TblUtil.getTableUiById(tbl_ui_id);
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
            set(tableModel, 'meta.local', true);
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
        const uiState = TblUtil.getTableUiById(tbl_ui_id) || {columns: []};
        this.setState(uiState);
    }

    render() {
        const {selectable, border, renderers} = this.props;
        const {columns, showUnits, showFilters, textView, startIdx, showMask, currentPage,
                hlRowIdx, selectInfo, filterInfo, sortInfo, data, error} = this.state;
        const {tableConnector} = this;
        if (error) return <div className='TablePanel__error'>{error}</div>;
        if (isEmpty(columns)) return false;
        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);

        return (
            <div className={'TablePanel__wrapper' + (border ? ' border' : '')}>
                <div className='TablePanel__table' style={{top: 0}}>
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
                        showMask={showMask}
                        currentPage={currentPage}
                        callbacks={tableConnector}
                        renderers={renderers}
                    />
                </div>
            </div>
        );
    }
}

BasicTable.propTypes = {
    tbl_id: PropTypes.string,
    tbl_ui_id: PropTypes.string,
    tableModel: PropTypes.object,
    showUnits: PropTypes.bool,
    showFilters: PropTypes.bool,
    selectable: PropTypes.bool,
    border: PropTypes.bool,
    renderers: PropTypes.objectOf(
        PropTypes.shape({
            cellRenderer: PropTypes.func,
            headRenderer: PropTypes.func
        })
    )
};

BasicTable.defaultProps = {
    selectable: false,
    border: true
};
