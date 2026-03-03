/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {Box, Sheet, Stack, Typography} from '@mui/joy';
import {MultiImageViewer} from '../../visualize/ui/MultiImageViewer.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';

import {getTblIdsByGroup} from '../../tables/TableUtil.js';
import {visRoot} from 'firefly/visualize/ImagePlotCntlr';
import {getPlotViewAry} from 'firefly/visualize/PlotViewUtil.js';

import {ALERT} from './AlertManager.js';
import {PropertySheetAsTable} from 'firefly/tables/ui/PropertySheet';
import {MultiProductChoice} from 'firefly/visualize/ui/multiProduct/MultiProductChoice';
import {SHOW_CHART, SHOW_TABLE} from 'firefly/metaConvert/DataProductsType';
import {dispatchSetLayoutMode, getLayouInfo, LO_MODE, LO_VIEW} from 'firefly/core/LayoutCntlr';
import {getExpandedChartProps} from 'firefly/charts/ChartsCntlr.js';
import {ChartsContainer} from 'firefly/charts/ui/ChartsContainer.jsx';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import {ImageExpandedMode} from 'firefly/visualize/iv/ImageExpandedMode.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID} from 'firefly/visualize/MultiViewCntlr.js';


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

export function AlertResultView() {
    const {expanded} = useStoreConnector(() => {
        const {mode} = getLayouInfo();
        return { expanded: mode?.expanded ?? LO_VIEW.none };
    });

    const [whatToShow, setWhatToShow] = React.useState(SHOW_TABLE);

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

    if (expanded !== LO_VIEW.none) {
        return (
            <ExpandedView expanded={expanded}/>
        );
    }

    return (
        <Stack sx={{width: 1, height: 1, overflow: 'hidden', minHeight: 0, minWidth: 0, display: 'flex',
        }}>

            {/* Top: tables */}
            <Box sx={{display: 'flex', flex: '1 1 0%', gap: 1, p: 1, overflow: 'hidden', minHeight: 0, minWidth: 0,
            }}>
                <Box sx={{flex: '1 1 0%', overflow: 'hidden', minHeight: 0, minWidth: 0}}>
                    {hasMainTable ? (
                        <MultiProductChoice
                            dataProductsState={null} //allowed to be null
                            dpId='alert'
                            chartViewerId={ALERT.CHART_VIEWER_1}
                            tableGroupViewerId={ALERT.TABLE_GROUP_MAIN}
                            whatToShow={whatToShow}
                            onChange={handleShowChange}
                            mayToggle={true}
                        />
                    ) : (
                        <EmptySlot label='Table' />
                    )}
                </Box>

                <Box sx={{flex: '1 1 0%', overflow: 'hidden', minHeight: 0, minWidth: 0}}>
                    {hasDetailTable ? (
                        <PropertySheetAsTable
                            tbl_id={ALERT.TABLE_2_ID}
                            tbl_group={ALERT.TABLE_GROUP_DETAILS}
                            tblOptions={{
                                showFilters: true,
                                showToolbar: true,
                                removable: false,
                            }}
                        />
                    ) : (
                        <EmptySlot label='Details Table' />
                    )}
                </Box>
            </Box>


            {/* Bottom: 3 image viewers */}
            <Box sx={{display: 'flex', flex: '1 1 0%', gap: 1, p: 1, overflow: 'hidden', minHeight: 0, minWidth: 0,
            }}>
                {[ALERT.IMG_VIEWER_1, ALERT.IMG_VIEWER_2, ALERT.IMG_VIEWER_3].map((viewerId, idx) => (
                    <Box key={viewerId} sx={{flex: '1 1 0%', overflow: 'hidden', minHeight: 0, minWidth: 0}}>
                        {hasImages ? (
                            <MultiImageViewer
                                viewerId={viewerId}
                                insideFlex={true}
                                forceRowSize={1}
                            />
                        ) : (
                            <EmptySlot label={`Image ${idx+1}`} />
                        )}
                    </Box>
                ))}
            </Box>

        </Stack>
    );
}

function ExpandedView({expanded}) {
    let view;

    if (expanded === LO_VIEW.tables) {
        view = (
            <TablesContainer
                mode='both'
                closeable={true}
                expandedMode={true}
                tbl_group={ALERT.TABLE_GROUP_MAIN}
            />
        );
    }
    else if (expanded === LO_VIEW.xyPlots) {
        const {expandedViewerId} = getExpandedChartProps();
        const chartExpandedMode = true;

        view = (
            <ChartsContainer
                closeable={true}
                tbl_group={ALERT.TABLE_GROUP_MAIN}
                addDefaultChart={false}
                viewerId={expandedViewerId}
                useOnlyChartsInViewer={
                    chartExpandedMode &&
                    expandedViewerId &&
                    expandedViewerId !== DEFAULT_PLOT2D_VIEWER_ID
                }
                expandedMode={true}
            />
        );
    }
    else {
        //images expanded mode uses Firefly’s built-in close rendering
        view = (
            <ImageExpandedMode
                key='alert-images-expanded'
                closeFunc={closeExpanded}
            />
        );
    }

    return (
        <Stack direction='row' flexGrow={1} overflow='hidden'>
            {view}
        </Stack>
    );
}

function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}


function EmptySlot({label}) {
    return (
        <Sheet variant='outlined'
            sx={{height: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gcolor: 'background.level1'}}
        >
            <Typography level='body-sm' color='neutral'>
                {label} - No data
            </Typography>
        </Sheet>
    );
}