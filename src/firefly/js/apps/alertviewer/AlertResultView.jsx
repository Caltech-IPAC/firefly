/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import {Sheet, Stack, Typography} from '@mui/joy';
import {MultiImageViewer} from '../../visualize/ui/MultiImageViewer.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';

import {getTblById, getTblIdsByGroup} from '../../tables/TableUtil.js';
import {visRoot} from 'firefly/visualize/ImagePlotCntlr';
import {getPlotViewAry} from 'firefly/visualize/PlotViewUtil.js';

import {ALERT} from './AlertIDs.js';
import {PropertySheetAsTable} from 'firefly/tables/ui/PropertySheet';
import {MultiProductChoice} from 'firefly/visualize/ui/multiProduct/MultiProductChoice';
import {SHOW_CHART, SHOW_TABLE} from 'firefly/metaConvert/DataProductsType';
import {dispatchChartAdd, getChartData} from 'firefly/charts/ChartsCntlr.js';
import {MultiViewStandardToolbar} from 'firefly/visualize/ui/MultiViewStandardToolbar';
import {getDefaultChartProps} from 'firefly/charts/ChartUtil';
import DockLayoutPanel from 'firefly/ui/panel/DockLayoutPanel.jsx';
import {getComponentState} from 'firefly/core/ComponentCntlr';

const ALERT_STANDARD_LAYOUT = {
    east: {index: 0, defaultSize: '50%'},
    west: {index: 1},
    south: {index: 2, defaultSize: '50%'},
};

function getAlertData() {
    const mainIds = getTblIdsByGroup(ALERT.TABLE_GROUP_MAIN) || [];
    const detailIds = getTblIdsByGroup(ALERT.TABLE_GROUP_DETAILS) || [];

    const plotViews = getPlotViewAry(visRoot()) || [];
    const hasImages = plotViews.length > 0;

    return {
        hasMainTable: mainIds.length > 0,
        hasDetailTable: detailIds.length > 0,
        hasImages,
    };
}

function useEnsureMainChart() {
    const mainTblLoaded = useStoreConnector(() => {
        const tbl = getTblById?.(ALERT.TABLE_1_ID);
        return Boolean(tbl && !tbl.isFetching && tbl.tableData);
    });

    useEffect(() => {
        if (!mainTblLoaded) return;

        if (getChartData(ALERT.CHART_1_ID, null)) return;

        const def = getDefaultChartProps(ALERT.TABLE_1_ID);
        if (!def) return;

        dispatchChartAdd({
            chartId: ALERT.CHART_1_ID,
            viewerId: ALERT.CHART_VIEWER_1,
            groupId: ALERT.TABLE_1_ID,
            ...def,
            deletable: false,
        });
    }, [mainTblLoaded]);
}

export function AlertResultView() {

    useEnsureMainChart();

    const [whatToShow, setWhatToShow] = useState(SHOW_CHART);

    const handleShowChange = (...args) => {
        const v =
            (args.length >= 2 ? args[1] : undefined) ??
            args[0]?.target?.value ??
            args[0];

        if (v === SHOW_TABLE || v === SHOW_CHART) {
            setWhatToShow(v);
        } else {
            console.warn('[Alert] ignoring unexpected whatToShow:', v, args);
        }
    };

    const {hasMainTable, hasDetailTable, hasImages} = useStoreConnector(getAlertData);
    const {id, source, fileName} = useStoreConnector(() => getComponentState(ALERT.STATE_ID));
    const fallbackTitle = fileName || source || 'Alert Viewer';
    const alertTitleDisplay = id ? (
        <Stack direction='row' spacing={1}>
            <Typography level='title-md'>
                Alert ID:
            </Typography>
            <Typography level='body-md'>
                {id}
            </Typography>
        </Stack>
    ) : (
        <Typography level='title-md'>
            {fallbackTitle}
        </Typography>
    );

    const standardPanels = [
        hasDetailTable ? (
            <Stack sx={{width: 1, height: 1, pt: '28px'}}>
                <Stack direction='row' alignItems='center' sx={{height: '28px', mt: '-28px', px: 1}}>
                    {alertTitleDisplay}
                </Stack>
                <PropertySheetAsTable
                    tbl_id={ALERT.TABLE_2_ID}
                    tbl_group={ALERT.TABLE_GROUP_DETAILS}
                    tblOptions={{
                        showFilters: true,
                        showToolbar: true,
                        removable: false,
                    }}
                />
            </Stack>
        ) : (
            <Stack sx={{width: 1, height: 1, pt: '28px'}}>
                <Stack direction='row' alignItems='center' sx={{height: '28px', mt: '-28px', px: 1}}>
                    {alertTitleDisplay}
                </Stack>
                <EmptySlot label='Details Table' />
            </Stack>
        ),
        hasMainTable ? (
            <MultiProductChoice
                dpId='alert'
                chartViewerId={ALERT.CHART_VIEWER_1}
                tableGroupViewerId={ALERT.TABLE_GROUP_MAIN}
                whatToShow={whatToShow}
                onChange={handleShowChange}
                mayToggle={true}
            />
        ) : (
            <EmptySlot label='Table' />
        ),
        hasImages ? (
            <Stack sx={{width: 1, height: 1}}>
                <MultiImageViewer
                    viewerId={ALERT.IMG_VIEWER}
                    insideFlex={true}
                    forceRowSize={1}
                    Toolbar={MultiViewStandardToolbar}
                    showViewerScroll={false}
                />
            </Stack>
        ) : (
            <EmptySlot label='Images' />
        ),
    ];

    return (
        <Sheet sx={{width: 1, height: 1, display: 'flex', minHeight: 0}}>
            <DockLayoutPanel config={ALERT_STANDARD_LAYOUT}>
                {standardPanels}
            </DockLayoutPanel>
        </Sheet>
    );
}

function EmptySlot({label}) {
    return (
        <Sheet variant='outlined' sx={{height: 1, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
            <Typography level='body-sm' color='neutral'>
                {label} - No data
            </Typography>
        </Sheet>
    );
}
