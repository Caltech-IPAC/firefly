
import {Box} from '@mui/joy';
import React from 'react';
import {dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import {PagingBar} from '../../ui/PagingBar.jsx';
import {getTblInfoById} from '../../tables/TableUtil.js';





export function ImagePager({pageSize, tbl_id, style={}}) {
    const {totalRows, showLoading, currentPage} = getTblInfoById(tbl_id, pageSize);
    const onGotoPage = (pageNum) => {
        const hlRowIdx = Math.max( pageSize * (pageNum-1), 0 );
        dispatchTableHighlight(tbl_id, hlRowIdx);
    };

    return (
        <Box role='PanelToolbar' style={style}>
            <PagingBar {...{currentPage, pageSize, showLoading, totalRows, callbacks:{onGotoPage}}} />
        </Box>
    );
}
