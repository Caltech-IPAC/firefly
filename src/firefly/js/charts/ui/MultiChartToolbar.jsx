/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useContext} from 'react';
import PropTypes from 'prop-types';
import {Sheet, Stack, Typography} from '@mui/joy';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {BeforeButton, DisplayTypeButtonGroup, NextButton} from '../../visualize/ui/Buttons.jsx';

import {getChartData} from '../ChartsCntlr.js';
import {dispatchChangeViewerLayout, dispatchUpdateCustom, getViewerItemIds, getViewer, getLayoutType, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';

import {PagingControl} from '../../visualize/iv/ExpandedTools.jsx';
import {ChartToolbar} from './ChartPanel';
import {CloseButton} from '../../ui/CloseButton';
import {closeExpandedChart} from 'firefly/charts/ui/ChartsContainer.jsx';
import {AddBtn} from './PlotlyToolbar.jsx';


export function MultiChartToolbarStandard({viewerId, chartId, tbl_group, expandable, expandedMode,
    layoutType=getLayoutType(getMultiViewRoot(), viewerId),
    activeItemId=getViewer(getMultiViewRoot(), viewerId).customData.activeItemId}) {


    const {jsApi=false}= useContext(AppPropertiesCtx);
    return (
        <Stack direction='row' justifyContent='space-between'>
            <Stack direction='row' spacing={3}>
                {!jsApi && <AddBtn/>}
                <MultiChartStd {...{viewerId, layoutType, activeItemId}}/>
            </Stack>
            <ChartToolbar {...{chartId, tbl_group, viewerId, expandable, expandedMode}}/>
        </Stack>
    );
}

MultiChartToolbarStandard.propTypes= {
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    activeItemId : PropTypes.string,
    chartId: PropTypes.string.isRequired,
    tbl_group: PropTypes.string,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool
};


export function MultiChartToolbarExpanded({viewerId, chartId, tbl_group, expandable, expandedMode, closeable, layoutType, activeItemId}) {

    const {jsApi=false}= useContext(AppPropertiesCtx);
    layoutType = layoutType || getLayoutType(getMultiViewRoot(), viewerId);
    activeItemId = activeItemId || getViewer(getMultiViewRoot(), viewerId)?.customData?.activeItemId;

    return (
        <Stack direction='row' justifyContent='space-between' alignItems='center'>
            {closeable && <CloseButton onClick={() => closeExpandedChart(viewerId)}/>}
            <Stack direction='row' spacing={3}>
                {!jsApi && <AddBtn/>}
                <MultiChartExt {...{viewerId, layoutType, activeItemId}}/>
            </Stack>
            <ChartToolbar {...{chartId, tbl_group, expandable, expandedMode, viewerId}}/>
        </Stack>
    );

}

MultiChartToolbarExpanded.propTypes= {
    viewerId : PropTypes.string.isRequired,
    tbl_group: PropTypes.string,
    layoutType : PropTypes.string,
    activeItemId : PropTypes.string,
    chartId: PropTypes.string.isRequired,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool
};

const MultiChartStd = ({viewerId, layoutType, activeItemId}) => {

    const viewerItemIds = getViewerItemIds(getMultiViewRoot(), viewerId);
    var cIdx= viewerItemIds.findIndex( (itemId) => itemId===activeItemId);
    if (cIdx<0) cIdx= 0;

    if (viewerItemIds.length===1) {
        return <div/>;
    }

    const nextIdx= cIdx===viewerItemIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerItemIds.length-1;


    return (
        <Stack direction='row' py={1/4}>
            <DisplayTypeButtonGroup {...{value:getViewer(getMultiViewRoot(), viewerId)?.layout ?? 'single',
                config:[
                    { value:'single', title:'Show single chart',
                        onClick: () => dispatchChangeViewerLayout(viewerId,'single')
                    },
                    { value:'grid', title:'Show all charts as tiles',
                        onClick: () => dispatchChangeViewerLayout(viewerId,'grid')
                    }
                ]
            }}/>
            {layoutType==='single' && viewerItemIds.length>2 &&
            <BeforeButton onClick={() => dispatchUpdateCustom(viewerId, {activeItemId: viewerItemIds[prevIdx]})} /> }
            {layoutType==='single' && viewerItemIds.length>1 &&
                <NextButton onClick={() => dispatchUpdateCustom(viewerId, {activeItemId: viewerItemIds[nextIdx]})} /> }
        </Stack>
    );
};


const MultiChartExt = ({viewerId, layoutType, activeItemId}) => {
    const viewerItemIds = getViewerItemIds(getMultiViewRoot(), viewerId);

    if (viewerItemIds.length < 2) {
        return null;
    }

    return (
        <Sheet component={Stack} direction='row'
               sx={{
                   flexGrow: 1,
                   px: 1,
                   alignItems: 'center',
                   justifyContent:'space-evenly',
               }}
        >
            <Typography level='h4' whiteSpace='nowrap'>
                {layoutType==='single' ? getChartTitle(activeItemId, viewerItemIds) : 'Tiled View'}
            </Typography>
            <Stack direction='row' sx={{whiteSpace:'nowrap'}}>
                <Stack direction='row'>
                    <DisplayTypeButtonGroup {...{value:getViewer(getMultiViewRoot(), viewerId)?.layout ?? 'single',
                        config:[
                            { value:'single', title:'Show single chart',
                                onClick: () => dispatchChangeViewerLayout(viewerId,'single')
                            },
                            { value:'grid', title:'Show all charts as tiles',
                                onClick: () => dispatchChangeViewerLayout(viewerId,'grid')
                            }
                        ]
                    }}/>
                </Stack>
                <Stack>
                    <PagingControl
                        viewerItemIds={viewerItemIds}
                        activeItemId={activeItemId}
                        isPagingMode={layoutType==='single'}
                        getItemTitle={(itemId) => getChartTitle(itemId, viewerItemIds)}
                        onActiveItemChange={(itemId) => dispatchUpdateCustom(viewerId, {activeItemId: itemId})}
                    />
                </Stack>
            </Stack>
        </Sheet>
    );
};

const getChartTitle = (chartId, viewerItemIds) => {
    const chartData = getChartData(chartId);
    let title = chartData?.layout?.title?.text;         // use chart title if exists, otherwise titled it `Plot n`, where n is the index of the charts in this viewer.
    if (!title) {
        const idx = viewerItemIds.findIndex((id) => id === chartId);
        title = `Plot ${idx}`;
    }
    return title;
};

