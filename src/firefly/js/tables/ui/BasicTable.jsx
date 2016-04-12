/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty} from 'lodash';

import * as TblUtil from '../TableUtil.js';
import {BasicTableView} from './BasicTableView.jsx';
import {RemoteTableStore, TableStore} from '../TableStore.js';
import {SelectInfo} from '../SelectInfo.js';

export class BasicTable extends Component {
    constructor(props) {
        super(props);

        if (props.tbl_id) {
            this.tableStore = RemoteTableStore.newInstance(props, (v) => this.setState(v));
        } else if (props.tableModel) {
            this.tableStore = TableStore.newInstance(props, (v) => this.setState(v));
        }
        this.state = this.tableStore.cState;
    }

    componentWillReceiveProps(nProps) {
        this.tableStore.init(nProps);
    }

    componentWillUnmount() {
        this.tableStore && this.tableStore.onUnmount();
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
    }

    render() {
        var {tableModel, columns, showUnits, showFilters, textView} = this.state;
        const {selectable, border, renderers} = this.props;
        const {tableStore} = this;
        if (isEmpty(columns) || isEmpty(tableModel)) return false;
        const {hlRowIdx, selectInfo, filterInfo, sortInfo, data} = TblUtil.prepareTableData(tableModel);
        const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);

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
                        callbacks={tableStore}
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
    tbl_ui_id: TblUtil.uniqueTblUiId(),
    selectable: false,
    border: true
};
