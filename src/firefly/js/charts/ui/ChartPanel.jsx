import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {Box, ChipDelete, Divider, Stack} from '@mui/joy';
import {get, isEmpty, isUndefined} from 'lodash';

import {wrapResizer} from '../../ui/SizeMeConfig.js';
import {PlotlyChartArea} from './PlotlyChartArea.jsx';
import {PlotlyToolbar} from './PlotlyToolbar.jsx';
import {dispatchChartMounted, dispatchChartRemove, dispatchChartUnmounted, getChartData, getErrors} from '../ChartsCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';

import {allowPinnedCharts} from '../ChartUtil.js';
import {PinChart, ShowTable} from './PinnedChartContainer.jsx';
import {CombinePinnedCharts} from './CombineChart.jsx';


function ChartPanelView(props) {

    const {chartId, Chart= PlotlyChartArea, tbl_group, chartData, expandable, expandedMode, Toolbar=PlotlyToolbar,
        showToolbar=true, showChart=true, glass} = props;

    useEffect(() => {
        dispatchChartMounted(chartId);
        return () => {
            dispatchChartUnmounted(chartId);
        };
    }, [chartId]);

    if (isEmpty(chartData?.chartType) || isUndefined(Toolbar)) {
        return <div/>;
    }

    if (showToolbar) {
        // chart with toolbar
        return (
            <Stack id='chart-panel' height={1} overflow='hidden'>
                <ChartToolbar {...{Chart, chartId, tbl_group, expandable, expandedMode, Toolbar}}/>
                <Divider orientation='horizontal'/>
                <ChartArea glass={glass} {...props}/>
            </Stack>
        );
    } else {
        // chart only
        return <ChartArea glass={glass} {...{...props, showToolbar, showChart, Chart}}/>;
    }
}

ChartPanelView.propTypes = {
    chartId: PropTypes.string.isRequired,
    tbl_group: PropTypes.string,
    chartData: PropTypes.object,
    deletable: PropTypes.bool,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    showToolbar: PropTypes.bool,
    showChart: PropTypes.bool,
    glass: PropTypes.bool, // for non-interactive chart area (when toolbar is missing)
    Chart: PropTypes.func,
    Toolbar: PropTypes.func
};




const ResizableChartAreaInternal = React.memo((props) => {
    const {errors, Chart, size, chartData}= props;
    const {width:widthPx, height:heightPx}= size;
    const knownSize = widthPx && heightPx;

    if (!knownSize) return <div/>;

    const hasData = chartData?.data?.length > errors?.length;

    return (
        <Stack id='chart-resizer' height={1} width={1} sx={{flexGrow:1, position:'absolute', overflow:'hidden', inset:0}}>
            {!hasData || isUndefined(Chart) ?
                <ErrorPanel errors={errors}/> :
                <Chart {...Object.assign({}, props, {widthPx, heightPx})}/>
            }
        </Stack>
    );
});

export const ChartToolbar = (props={}) => {
    const {Toolbar=PlotlyToolbar, chartId, expandable, expandedMode, viewerId, tbl_group} = props;

    // logic for PinnedChartContainer toolbar added here
    if (allowPinnedCharts()) {
        return (
            <Stack id='chart-toolbar' direction='row' alignItems='center'>
                <CombinePinnedCharts {...{viewerId, tbl_group}} />
                <ShowTable {...{viewerId, tbl_group}} />
                <PinChart {...{viewerId, tbl_group}}/>
                <Toolbar {...{chartId, expandable, expandedMode}}/>
            </Stack>
        );
    }
    return <Toolbar {...{chartId, expandable, expandedMode}}/>;
};

const ChartArea = (props) => {
    const {chartId, chartData, deletable:deletableProp, glass} = props;
    const deletable = isUndefined(get(chartData, 'deletable')) ? deletableProp : get(chartData, 'deletable');
    const errors  = getErrors(chartId);
    return (
        <Stack id='chart-area' flexGrow={1} sx={{position:'relative'}}>
            <ResizableChartArea
                {...Object.assign({}, props, {errors})} />
            {glass && <Stack flexGrow={1} sx={{backgroundColor:'transparent'}}/>}
            {deletable &&
                <ChipDelete
                    title='Delete this chart'
                    style={{display: 'inline-block', position: 'absolute', top: 0, right: 0, alignSelf: 'baseline', padding: 2, cursor: 'pointer'}}
                    onClick={(ev) => {
                        dispatchChartRemove(chartId);
                        ev.stopPropagation();
                    }} />
                }
        </Stack>
    );
};


const ResizableChartArea= wrapResizer(ResizableChartAreaInternal);


function ErrorPanel({errors}) {
    return (
      <Box id='chart-error' position='relative' width={1} height={1}>
          {errors.map((error, i) => {
              const {message='Error', reason=''} = error;
                    return (
                        <div key={i} style={{padding: 20, textAlign: 'center', overflowWrap: 'normal'}}>
                            <h3>{message}</h3>
                            {`${reason}`}
                        </div>
                    );
              })}
      </Box>
    );
}

ErrorPanel.propTypes = {
    errors: PropTypes.arrayOf(
        PropTypes.shape({
            message: PropTypes.string,
            reason: PropTypes.oneOfType([PropTypes.object,PropTypes.string]) // reason can be an Error object
        }))
};

export const ChartPanel = ({chartId, ...props}) => {
    const chartData = useStoreConnector(() => getChartData(chartId));
    return (
        <ChartPanelView key={chartId} {...{chartId, chartData}} {...props}/>
    );
};

ChartPanel.propTypes = {
    chartId: PropTypes.string.isRequired,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    deletable: PropTypes.bool,
    showToolbar: PropTypes.bool,
    showChart: PropTypes.bool,
    thumbnail: PropTypes.bool
};

