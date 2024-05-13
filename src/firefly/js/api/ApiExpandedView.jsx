/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Sheet, Stack} from '@mui/joy';
import React, {useRef} from 'react';
import {PinnedChartPanel} from '../charts/ui/PinnedChartContainer.jsx';
import {TablesContainer} from '../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../charts/ui/ChartsContainer.jsx';
import {useStoreConnector} from '../ui/SimpleComponent.jsx';
import {PINNED_CHART_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import {ApiExpandedDisplay} from '../visualize/ui/ApiExpandedDisplay.jsx';
import {FireflyRoot} from '../ui/FireflyRoot.jsx';
import {isDefined} from '../util/WebUtil.js';
import {dispatchChangeExpandedMode, ExpandType} from '../visualize/ImagePlotCntlr.js';
import {dispatchSetLayoutMode, getExpandedMode, LO_MODE, LO_VIEW} from '../core/LayoutCntlr.js';
import {getExpandedChartProps} from '../charts/ChartsCntlr.js';

const closeFunc = () => {
    dispatchChangeExpandedMode(ExpandType.COLLAPSE);
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
};

export function ApiExpandedView() {
    const expanded= useStoreConnector(() => getExpandedMode());
    const {current} = useRef({ savedOverflow: undefined});

    const expandType = LO_VIEW.get(expanded) ?? LO_VIEW.none;
    if (expandType === LO_VIEW.none) {
        if (isDefined(current.saveOverflow)) document.body.style.overflow= current.saveOverflow;
        return false;
    }

    current.saveOverflow= document.body.style.overflow;
    document.body.style.overflow= 'hidden';

    return (
        <FireflyRoot sx={{height:1, width:1}} ctxProperties={{jsApi:true}}>
            <ApiExpandedViewInner {...{expandType}}/>
        </FireflyRoot>
    );
}



function ApiExpandedViewInner({expandType}) {
    if (expandType===LO_VIEW.none) return false;
    return (
        <Sheet {...{variant:'soft',
            sx: (theme) => (
                {
                    zIndex: 100, overflow: 'hidden', position: 'fixed',
                    top: 1, left: 1, bottom: 3, right: 3,
                    boxShadow: `1px 1px 5px 3px ${theme.vars.palette['neutral']?.softActiveBg}`,
                    borderRadius: '5px'
                })
        }} >
            <Stack {...{p:1/4, width:1,height:1}}>
                <ExpandedView {...{expandType}}/>
            </Stack>
        </Sheet>
    );
}

function ExpandedView({expandType}) {
    if (expandType === LO_VIEW.tables) return <TablesContainer  mode='expanded' />;
    if (expandType === LO_VIEW.xyPlots) {
        const {chartId, expandedViewerId:viewerId} = expandType===LO_VIEW.xyPlots ? getExpandedChartProps() : {};
        if (viewerId=== PINNED_CHART_VIEWER_ID) {
            return (
                <PinnedChartPanel {...{
                    closeable:true, expandedMode:true, key:'api',
                    useOnlyChartsInViewer:false, tbl_group:'main', addDefaultChart:true, }}/>
            );
        }
        else {
            return <ChartsContainer {...{key:'api', expandedMode:true, closeable:true, chartId}}/>;
        }
    }
    return <ApiExpandedDisplay closeFunc={closeFunc}/>;
}
