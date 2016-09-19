import React, {Component, PropTypes} from 'react';
import shallowequal from 'shallowequal';
import {omit} from 'lodash';

import {InputField} from './InputField.jsx';
import {intValidator} from '../util/Validate.js';
import LOADING from 'html/images/gxt/loading.gif';
import {MAX_ROW} from '../tables/TableUtil.js';

export class PagingBar extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps, nState) {
        return !shallowequal(omit(nProps, 'callbacks'), omit(this.props, 'callbacks'));
    }

    render() {
        const {currentPage, totalRows, pageSize, showLoading, callbacks} = this.props;

        const showAll = pageSize === MAX_ROW;
        const startIdx = (currentPage-1) * pageSize;
        const endIdx = Math.min(startIdx+pageSize, totalRows);
        var totalPages = Math.ceil((totalRows || 0)/pageSize);

        const onPageChange = (pageNum) => {
            if (pageNum.valid) {
                callbacks.onGotoPage(pageNum.value);
            }
        };
        const showingLabel = (  <div style={{fontSize: 'smaller', marginLeft: 3}} >
                                    ({(startIdx+1).toLocaleString()} - {endIdx.toLocaleString()} of {totalRows.toLocaleString()})
                                </div>
                            );
        if (showAll) {
            return showingLabel;
        } else {
            return (
                <div className='group'>
                    <div onClick={() => callbacks.onGotoPage(1)} className='PagingBar__button first' title='First Page'/>
                    <div onClick={() => callbacks.onGotoPage(currentPage - 1)} className='PagingBar__button previous'  title='Previous Page'/>
                    <InputField
                        style={{textAlign: 'right'}}
                        validator = {intValidator(1,totalPages, 'Page Number')}
                        tooltip = 'Jump to this page'
                        size = {2}
                        value = {currentPage+''}
                        onChange = {onPageChange}
                        actOn={['blur','enter']}
                        showWarning={false}
                    /> <div style={{fontSize: 'smaller', marginLeft: 3}} > of {totalPages}</div>
                    <div onClick={() => callbacks.onGotoPage(currentPage + 1)} className='PagingBar__button next'  title='Next Page'/>
                    <div onClick={() => callbacks.onGotoPage(totalPages)} className='PagingBar__button last'  title='Last Page'/>
                    {showingLabel}
                    {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
                </div>
            );
        }
    }
}

PagingBar.propTypes = {
    currentPage: PropTypes.number,
    totalRows: PropTypes.number,
    pageSize: PropTypes.number,
    showLoading: PropTypes.bool,
    callbacks: PropTypes.shape({
        onGotoPage: PropTypes.func.required
    })
};

PagingBar.defaultProps = {
    currentPage: 1,
    showLoading: false,
    pageSize: 10
};

