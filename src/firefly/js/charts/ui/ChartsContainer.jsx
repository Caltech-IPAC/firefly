/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEqual, isEmpty, get} from 'lodash';

import {getAppOptions} from '../../core/AppDataCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {PLOT2D, DEFAULT_PLOT2D_VIEWER_ID, dispatchAddViewerItems, dispatchRemoveViewerItems, dispatchUpdateCustom, getViewerItemIds, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';
import {monitorChanges, findGroupByTblId, getActiveTableId, getTblById, isFullyLoaded} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ACTIVE, TABLE_LOADED} from '../../tables/TablesCntlr';
import {CHART_ADD, CHART_REMOVE, getChartIdsInGroup, getChartData, dispatchChartAdd} from '../ChartsCntlr.js';
import {colWithName, getNumericCols, DEFAULT_ALPHA} from '../ChartUtil.js';
import {MetaConst} from '../../data/MetaConst.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiChartViewer, getActiveViewerItemId} from './MultiChartViewer.jsx';

const DATAPOINTS_COLOR = `rgba(63, 127, 191, ${DEFAULT_ALPHA})`;

// DEFAULT_PLOT2D_VIEWER_ID, 'main'
function watchTblGroup(viewerId, tblGroup, addDefaultChart) {
    return () => {
        if (!tblGroup) return;
        const accept = (a) => {
            const {tbl_id, tbl_group, chartId} = a.payload;
            return chartId || tblGroup === (tbl_group || findGroupByTblId(tbl_id));
        };
        const actions = [CHART_ADD, CHART_REMOVE, TBL_RESULTS_ACTIVE];
        if (addDefaultChart) actions.push(TABLE_LOADED);
        return monitorChanges(actions, accept, updateViewer(viewerId, tblGroup));
    };
}

function updateViewer(viewerId, tblGroup) {
    return (action) => {
        switch (action.type) {
            case TABLE_LOADED:
                const {tbl_id} = action.payload;
                ensureDefaultChart(tbl_id);
                break;
            case TBL_RESULTS_ACTIVE:
            case CHART_ADD:
            case CHART_REMOVE:
                action.type === TBL_RESULTS_ACTIVE && ensureDefaultChart(action.payload.tbl_id);
                const {chartId} = action.payload;
                doUpdateViewer(viewerId, tblGroup, chartId);
        }
    };
}

function ensureDefaultChart(tbl_id) {
    if (getChartIdsInGroup(tbl_id).length === 0) {
        const defaultChartProps = getDefaultChartProps(tbl_id);
        if (!isEmpty(defaultChartProps))  {
            // default chart
            dispatchChartAdd({
                chartId: 'default-' + tbl_id,
                chartType: 'plot.ly',
                groupId: tbl_id,
                ...defaultChartProps
            });
        }
    }
}

function doUpdateViewer(viewerId, tblGroup, chartId) {
    const tblId = getActiveTableId(tblGroup);
    const chartIds = [];
    chartIds.push(...getChartIdsInGroup(tblId), ...getChartIdsInGroup('default'));
    const currentIds = getViewerItemIds(getMultiViewRoot(), viewerId);
    if (!isEqual(chartIds, currentIds)) {
        if (chartId && isEmpty(getChartData(chartId))) {
            //removed chart - do not change active chartId unless
            if (getActiveViewerItemId(viewerId) === chartId) {
                dispatchUpdateCustom(viewerId, {activeItemId: undefined});
            }
        } else {
            const activeItemId = chartIds.includes(chartId) ? chartId : undefined;
            dispatchUpdateCustom(viewerId, {activeItemId});
        }
        dispatchRemoveViewerItems(viewerId, currentIds);
        dispatchAddViewerItems(viewerId, chartIds, PLOT2D);
    }
}

function getDefaultChartProps(tbl_id) {

    if (!isFullyLoaded(tbl_id)) { return; }

    const {tableMeta, tableData, totalRows}= getTblById(tbl_id);

    if (!totalRows) {
        return;
    }

    // default chart props can be set in a table attribute
    const defaultChartDef = tableMeta[MetaConst.DEFAULT_CHART_DEF];
    const defaultChartProps = defaultChartDef && JSON.parse(defaultChartDef);
    if (defaultChartProps && defaultChartProps.data) {
        defaultChartProps.data.forEach((e) => e['tbl_id'] = tbl_id);
        return defaultChartProps;
    }

    // for catalogs use lon and lat columns
    let isCatalog = Boolean(tableMeta[MetaConst.CATALOG_OVERLAY_TYPE] && tableMeta[MetaConst.CATALOG_COORD_COLS]);
    let xCol = undefined, yCol = undefined;

    if (isCatalog) {
        const s = tableMeta[MetaConst.CATALOG_COORD_COLS].split(';');
        if (s.length !== 3) return;
        xCol = colWithName(tableData.columns, s[0]); // longtitude
        yCol = colWithName(tableData.columns, s[1]); // latitude

        if (!xCol || !yCol) {
            isCatalog = false;
        }
    }

    //otherwise use the first one-two numeric columns
    if (!isCatalog) {
        const numericCols = getNumericCols(tableData.columns);
        if (numericCols.length >= 2) {
            xCol = numericCols[0];
            yCol = numericCols[1];
        } else if (numericCols.length > 1) {
            xCol = numericCols[0];
            yCol = numericCols[0];
        }
    }

    if (xCol && yCol)  {
        const chartData = {
            data: [{
                tbl_id,
                x: xCol &&`tables::${xCol.name}`,
                y: yCol && `tables::${yCol.name}`
            }],
            layout:{
                xaxis: {
                    autorange: isCatalog ? 'reversed' : 'true'
                },
                yaxis: {showgrid: false}
            }
        };
        const maxRowsForScatter = get(getAppOptions(), 'charts.maxRowsForScatter', 5000);
        if (totalRows > maxRowsForScatter) {
            Object.assign(chartData.data[0], {type: 'fireflyHeatmap', colorscale: 'Greys', reversescale: true});
        } else {
            Object.assign(chartData.data[0], {mode: 'markers', marker: {color: DATAPOINTS_COLOR}});
        }
        return chartData;
    } else {
        return {};
    }
}

/**
 * Default viewer
 * When tbl_group is defined, only the charts related to the active chart in this table group are displayed
 * When addDefaultChart is true, a default chart is created for each table in the group
 */
export class ChartsContainer extends PureComponent {
    constructor(props) {
        super(props);

    }

    componentWillMount() {
        const {viewerId=DEFAULT_PLOT2D_VIEWER_ID, tbl_group, addDefaultChart, chartId} = this.props;
        if (tbl_group) {
            if (addDefaultChart) {
                const tbl_id = getActiveTableId(tbl_group);
                if (isFullyLoaded(tbl_id)) {
                    ensureDefaultChart(tbl_id);
                }
            }
            // make sure the viewer is updated with related charts on start
            // important when we use external viewer
            doUpdateViewer(viewerId, tbl_group, chartId);

            const monitor = watchTblGroup(viewerId, tbl_group, addDefaultChart);
            this.removeMonitor = monitor();
        }
    }

    componentWillUnmount() {
        this.removeMonitor && this.removeMonitor();
        this.removeMonitor = undefined;
    }

    render() {
        const {chartId, expandedMode, closeable, viewerId=DEFAULT_PLOT2D_VIEWER_ID} = this.props;


        if (chartId) {
            return expandedMode ?
                <ExpandedView key='chart-expanded' closeable={closeable} chartId={chartId}/> :
                <ChartPanel key={chartId} expandable={true} chartId={chartId}/>;
        } else {
            return (
                <MultiChartViewer {...{closeable, viewerId, expandedMode}}/>
            );
        }
    }
}

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    viewerId : PropTypes.string,
    tbl_group : PropTypes.string,
    addDefaultChart : PropTypes.bool
};

function ExpandedView(props) {
    const {closeable, chartId} = props;

    return (
        <div style={{position: 'absolute', top: 0, left: 0, right: 0, bottom: 0}}>
            <ChartPanel key={'expanded-'+chartId} expandedMode={true} expandable={false} chartId={chartId}/>
            {closeable && <CloseButton style={{position: 'absolute', top: 0, left: 0, paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
        </div>
    );
}

ExpandedView.propTypes = {
    closeable: PropTypes.bool,
    chartId: PropTypes.string
};
