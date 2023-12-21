import React from 'react';
import PropTypes from 'prop-types';
import {Typography, Stack} from '@mui/joy';

import {InputField} from './InputField.jsx';
import {intValidator} from '../util/Validate.js';
import LOADING from 'html/images/gxt/loading.gif';
import {MAX_ROW} from '../tables/TableRequestUtil.js';

export function PagingBar(props) {
    const {currentPage, totalRows, pageSize, showLoading, callbacks} = props;

    const showAll = (totalRows === 0) || (pageSize === MAX_ROW);
    const startIdx = (currentPage-1) * pageSize;
    const endIdx = Math.min(startIdx+pageSize, totalRows);
    var totalPages = Math.ceil((totalRows || 0)/pageSize);

    const onPageChange = (pageNum) => {
        if (pageNum.valid) {
            callbacks.onGotoPage(pageNum.value);
        }
    };
    const nchar = totalPages.toString().length;

    const pagestr = (totalRows === 0) ? '' :
                    `(${(startIdx+1).toLocaleString()} - ${endIdx.toLocaleString()} of ${totalRows?.toLocaleString()??''})`;
    const showingLabel = (  <Typography level='body-sm' noWrap>{pagestr}</Typography> );
    if (showAll) {
        return showingLabel;
    } else {
        return (
            <Typography display='flex' alignItems='center' direction='row' level='body-sm' noWrap component='div'>
                <div onClick={() => callbacks.onGotoPage(1)} className='PagingBar__button first' title='First Page'/>
                <div onClick={() => callbacks.onGotoPage(currentPage - 1)} className='PagingBar__button previous' title='Previous Page'/>
                <Stack direction='row' alignItems='center' spacing={1/2}>
                    <InputField
                        slotProps={{ input: { size: 'sm', sx: {width:'3em'} } }}
                        style={{textAlign: 'right', width: `${nchar+1}ch`}}
                        validator = {intValidator(1, totalPages, 'Page Number')}
                        tooltip = 'Jump to this page'
                        value = {currentPage+''}
                        onChange = {onPageChange}
                        actOn={['blur','enter']}
                        showWarning={false}
                    /> <div> of {totalPages}</div>
                </Stack>
                <div onClick={() => callbacks.onGotoPage(currentPage + 1)} className='PagingBar__button next'  title='Next Page'/>
                <div onClick={() => callbacks.onGotoPage(totalPages)} className='PagingBar__button last'  title='Last Page'/>
                {showingLabel}
                {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
            </Typography>
        );
    }
}

PagingBar.propTypes = {
    currentPage: PropTypes.number,
    totalRows: PropTypes.number,
    pageSize: PropTypes.number,
    showLoading: PropTypes.bool,
    callbacks: PropTypes.shape({
        onGotoPage: PropTypes.func.isRequired
    })
};

PagingBar.defaultProps = {
    currentPage: 1,
    showLoading: false,
    pageSize: 100
};

