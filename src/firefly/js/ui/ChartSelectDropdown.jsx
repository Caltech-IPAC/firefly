/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import {string} from 'prop-types';
import {Stack, Typography} from '@mui/joy';

import {getNumericCols} from '../charts/ChartUtil.js';
import {ChartSelectPanel, CHART_ADDNEW} from './../charts/ui/ChartSelectPanel.jsx';
import {getActiveViewerItemId} from '../charts/ui/MultiChartViewer.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../visualize/MultiViewCntlr.js';

import CompleteButton from './CompleteButton.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {useStoreConnector} from './SimpleComponent.jsx';
import {getActiveTableId, getTblById} from '../tables/TableUtil.js';
import {dispatchLoadTblStats, getColValStats} from '../charts/TableStatsCntlr.js';

const dropdownName = 'ChartSelectDropDownCmd';


export function ChartSelectDropdown({tblGroup,name=dropdownName}) {

    const tblId = useStoreConnector(() => getActiveTableId(tblGroup));

    useEffect(() => {
        const tblStatsData = getColValStats(tblId);
        const table= getTblById(tblId);
        if (!tblStatsData && table) {
            dispatchLoadTblStats(table['request']);
        }

    }, [tblId]);

    let noChartReason='';
    if (tblId) {
        const {tableData, totalRows}= getTblById(tblId);
        if (!totalRows) {
            noChartReason = 'empty table';
        } else if (getNumericCols(tableData.columns).length < 1) {
            noChartReason = 'the table has no numeric columns';
        }
    } else {
        noChartReason = 'no active table';
    }

    if (noChartReason) {
        const msg = `Charts are not available: ${noChartReason}.`;
        return (
            <Stack flexGrow={1} spacing={4} alignItems='center' justifyContent='center'>
                <Typography level='body-lg'>{msg}</Typography>
                <CompleteButton onSuccess={hideSearchPanel} text = {'OK'}/>
            </Stack>
        );
    }

    return (
        <ChartSelectPanel {...{
            tbl_id: tblId,
            chartId: getActiveViewerItemId(DEFAULT_PLOT2D_VIEWER_ID),
            chartAction: CHART_ADDNEW,
            hideDialog: ()=>dispatchHideDropDown()}}/>
    );
}

ChartSelectDropdown.propTypes = {
    tblGroup: string, // if not present, default table group is used
    name: string
};

function hideSearchPanel() {
    dispatchHideDropDown();
}

