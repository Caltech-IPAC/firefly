import React from 'react';
import PropTypes from 'prop-types';
import {Typography, Stack} from '@mui/joy';

import {InputField} from './InputField.jsx';
import {intValidator} from '../util/Validate.js';
import LOADING from 'html/images/gxt/loading.gif';
import {MAX_ROW} from '../tables/TableRequestUtil.js';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';

import FirstPage from '@mui/icons-material/FirstPage';
import LastPage from '@mui/icons-material/LastPage';
import NavigateNext from '@mui/icons-material/NavigateNext';
import NavigateBefore from '@mui/icons-material/NavigateBefore';

export function PagingBar(props) {
    const {currentPage=1, totalRows, pageSize=100, showLoading=false, callbacks} = props;

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
    const showingLabel = (  <Typography level='body-sm' noWrap lineHeight={1}>{pagestr}</Typography> );
    if (showAll) {
        return showingLabel;
    } else {
        return (
            <Stack direction='row' sx={{flexWrap:'wrap', alignItems:'center'}}>
                <Stack direction='row'>
                    <ToolbarButton icon={<FirstPage/>} tip='First Page' onClick={() => callbacks.onGotoPage(1)}/>
                    <ToolbarButton icon={<NavigateBefore/>} tip='Previous Page' onClick={() => callbacks.onGotoPage(currentPage - 1)}/>
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
                        /> <Typography level='body-sm' noWrap={true}> of {totalPages}</Typography>
                    </Stack>
                    <ToolbarButton icon={<NavigateNext/>} tip='Next Page' onClick={() => callbacks.onGotoPage(currentPage + 1)}/>
                    <ToolbarButton icon={<LastPage/>} tip='Last Page' onClick={() => callbacks.onGotoPage(totalPages)}/>
                </Stack>
                <Typography level='body-sm'>
                    {showingLabel}
                    {showLoading ? <img style={{width:14,height:14,marginTop: '3px'}} src={LOADING}/> : false}
                </Typography>
            </Stack>
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


