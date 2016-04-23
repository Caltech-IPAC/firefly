import React, {Component, PropTypes} from 'react';
import shallowequal from 'shallowequal';
import {omit} from 'lodash';

import {InputField} from './InputField.jsx';
import {intValidator} from '../util/Validate.js';
import LOADING from 'html/images/gxt/loading.gif';

export class PagingBar extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        return !shallowequal(omit(nProps, 'callbacks'), omit(this.props, 'callbacks'));
    }

    render() {
        const {highlightedRow, totalRows, pageSize, showLoading, callbacks} = this.props;

        const currentPage = highlightedRow >= 0 ? Math.floor(highlightedRow / pageSize)+1 : 1;
        const startIdx = (currentPage-1) * pageSize;
        const endIdx = Math.min(startIdx+pageSize, totalRows);
        var totalPages = Math.ceil((totalRows || 0)/pageSize);

        const onPageChange = (pageNum) => {
            if (pageNum.valid) {
                callbacks.onGotoPage(pageNum.value);
            }
        };

        return (
            <div className='group'>
                <button onClick={() => callbacks.onGotoPage(1)} className='paging_bar first' title='First Page'/>
                <button onClick={() => callbacks.onGotoPage(currentPage - 1)} className='paging_bar previous'  title='Previous Page'/>
                <InputField
                    style={{textAlign: 'right'}}
                    validator = {intValidator(1,totalPages, 'Page Number')}
                    tooltip = 'Jump to this page'
                    size = {2}
                    value = {currentPage+''}
                    onChange = {onPageChange}
                    actOn={['blur','enter']}
                    showWarning={false}
                /> <div style={{fontSize: 'smaller'}} >&nbsp; of {totalPages}</div>
                <button onClick={() => callbacks.onGotoPage(currentPage + 1)} className='paging_bar next'  title='Next Page'/>
                <button onClick={() => callbacks.onGotoPage(totalPages)} className='paging_bar last'  title='Last Page'/>
                <div style={{fontSize: 'smaller'}} > &nbsp; ({(startIdx+1).toLocaleString()} - {endIdx.toLocaleString()} of {totalRows.toLocaleString()})</div>
                {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
            </div>
        );
    }
}

PagingBar.propTypes = {
    highlightedRow: PropTypes.number,
    totalRows: PropTypes.number,
    pageSize: PropTypes.number,
    showLoading: PropTypes.bool,
    callbacks: PropTypes.shape({
        onGotoPage: PropTypes.func.required
    })
};

PagingBar.defaultProps = {
    showLoading: false,
    pageSize: 10
};

