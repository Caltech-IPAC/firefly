/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {get, isUndefined} from 'lodash';

import {FilterEditor} from '../../tables/ui/FilterEditor.jsx';
import * as TblUtil from '../../tables/TableUtil.js';
import * as TablesCntlr from '../../tables/TablesCntlr.js';

export class FilterEditorWrapper extends Component {
    constructor(props) {
        super(props);
        this.state = {
            sortInfo: ''
        };
    }

    shouldComponentUpdate(np, ns) {
        const tblId = get(np.tableModel, 'tbl_id');
        return ns.sortInfo !== this.state.sortInfo || tblId !== get(this.props.tableModel, 'tbl_id') ||
            (TblUtil.isFullyLoaded(tblId) && np.tableModel !== this.props.tableModel); // to avoid flickering when changing the filter
    }

    render() {
        const {tableModel} = this.props;
        const {sortInfo} = this.state;
        return (
            <div className='TablePanelOptionsWrapper'>
                <div className='TablePanelOptions'>
                    <FilterEditor
                        columns={get(tableModel, 'tableData.columns', [])}
                        selectable={false}
                        filterInfo={get(tableModel, 'request.filters')}
                        sortInfo={sortInfo}
                        onChange={(obj) => {
                            if (!isUndefined(obj.filterInfo)) {
                                const newRequest = Object.assign({}, tableModel.request, {filters: obj.filterInfo});
                                TablesCntlr.dispatchTableFilter(newRequest);
                            } else if (!isUndefined(obj.sortInfo)) {
                                this.setState({sortInfo: obj.sortInfo});
                            }
                        } }/>
                </div>
            </div>
        );
    }
}

FilterEditorWrapper.propTypes = {
    toggleFilters : PropTypes.func,
    tableModel : PropTypes.object
};


