/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, defer} from 'lodash';
import React, {PropTypes} from 'react';

import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TblUtil from '../../tables/TableUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {FilterInfo} from '../../tables/FilterInfo.js';

import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';

import * as ChartsCntlr from '../ChartsCntlr.js';
import {getHighlighted} from '../ChartUtil.js';
import {setXYSelection, setZoom} from '../dataTypes/XYColsCDT.js';

import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {XYPlotOptions} from '../ui/XYPlotOptions.jsx';
import {XYPlot} from '../ui/XYPlot.jsx';
import {getChartProperties, updateOnStoreChange, FilterEditorWrapper} from './TblView.jsx';


import ZOOM_IN from 'html/images/icons-2014/24x24_ZoomIn.png';
import FILTER_IN from 'html/images/icons-2014/24x24_FilterAdd.png';
import SELECT_ROWS from 'html/images/icons-2014/24x24_Checkmark.png';
import DELETE from 'html/images/blue_delete_10x10.png';
import OUTLINE_EXPAND from 'html/images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
import SETTINGS from 'html/images/icons-2014/24x24_GearsNEW.png';
import ZOOM_ORIGINAL from 'html/images/icons-2014/Zoom1x-24x24-tmp.png';
import UNSELECT_ROWS from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import CLEAR_FILTERS from 'html/images/icons-2014/24x24_FilterOff_Circle.png';
import FILTER from 'html/images/icons-2014/24x24_Filter.png';
import LOADING from 'html/images/gxt/loading.gif';


export const SCATTER_TBLVIEW = {
    id : 'scatter',
    renderChart,
    renderOptions,
    renderToolbar,
    getChartProperties,
    updateOnStoreChange
};


function renderChart(props) {
    return <Chart {...props}/>;
}

class Chart extends React.Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nProps) {
        const {chartData, widthPx, heightPx, tableModel} = this.props;
        let doUpdate = nProps.chartData !== chartData ||
            nProps.widthPx !== widthPx || nProps.heightPx !== heightPx;
        if (!doUpdate) {
            doUpdate = nProps.tableModel &&
                (get(nProps.tableModel, 'highlightedRow') !== get(tableModel, 'highlightedRow') ||
                get(nProps.tableModel, 'selectInfo') !== get(tableModel, 'selectInfo') );
        }
        return Boolean(doUpdate);
    }

    render() {
        const {chartId, tblId, tableModel, chartData, widthPx, heightPx} = this.props;
        if (!TblUtil.isFullyLoaded(tblId) || !chartData) {
            return null;
        }
        if (!heightPx || !widthPx) { return (<div/>); }

        const { isDataReady, data:xyPlotData, options:xyPlotParams} = ChartsCntlr.getChartDataElement(chartId);

        if (isDataReady) {

            const hRow = getHighlighted(xyPlotParams, tblId);
            const sInfo = tableModel && tableModel.selectInfo;

            return (
                <XYPlot data={xyPlotData}
                        desc=''
                        width={widthPx}
                        height={heightPx}
                        params={xyPlotParams}
                        highlighted={hRow}
                        onHighlightChange={(highlightedRow) => {
                                TablesCntlr.dispatchTableHighlight(tblId, highlightedRow);
                            }
                       }
                        selectInfo={sInfo}
                        onSelection={(selection) => {
                        if (selectionNotEmpty(chartId, selection)) {defer(setXYSelection, chartId, undefined, selection);}
                    }}
                />
            );
        } else {
            if (xyPlotParams) {
                return (
                    <div style={{position: 'relative', width: '100%', height: '100%'}}>
                        <div className='loading-mask'/>
                    </div>
                );
            } else {
                return null;
            }
        }
    }
}

Chart.propTypes = {
    chartData : PropTypes.object,
    tblId : PropTypes.string,
    tableModel : PropTypes.object,
    chartId: PropTypes.string,
    widthPx : PropTypes.number,
    heightPx : PropTypes.number
};

function renderOptions(chartId, optionsKey) {
    if (optionsKey === 'options') {
        const {tblStatsData} = getChartProperties(chartId);
        if (get(tblStatsData,'isColStatsReady')) {
            const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
            const chartDataElementId = chartDataElement.id;
            const formName = `chartOpt-${chartId}`;
            return (
                <XYPlotOptions key={formName}
                               groupKey={formName}
                               colValStats={tblStatsData.colStats}
                               xyPlotParams={get(chartDataElement, 'options')}
                               defaultParams={get(chartDataElement, 'defaultOptions')}
                               onOptionsSelected={(options) => {
                            ChartsCntlr.dispatchChartOptionsReplace({chartId, chartDataElementId, newOptions: options});
                         }
                       }
                />);
        } else {
            return (
                <img style={{verticalAlign:'top', height: 16, padding: 10, float: 'left'}}
                     title='Loading Options...'
                     src={LOADING}
                />);
        }
    } else if (optionsKey === 'filters') {
        const {tableModel} = getChartProperties(chartId);
        return (
            <FilterEditorWrapper tableModel={tableModel}/>
        );
    }
}


function renderToolbar(chartId, expandable, expandedMode, toggleOptions) {
    const {tblId, tableModel, deletable, help_id} = getChartProperties(chartId);
    return (
        <div className='PanelToolbar ChartPanel__toolbar'>
            <div className='PanelToolbar_group'>
                {renderSelectionButtons(chartId, tblId, tableModel)}
            </div>
            <div className='PanelToolbar_group'>
                {displayZoomOriginal(chartId) &&
                <img className='PanelToolbar__button'
                     title='Zoom out to original chart'
                     src={ZOOM_ORIGINAL}
                     onClick={() => resetZoom(chartId)}
                />}
                {displayUnselectAll(tableModel) &&
                <img className='PanelToolbar__button'
                     title='Unselect all selected points'
                     src={UNSELECT_ROWS}
                     onClick={() => TblUtil.clearSelection(tableModel)}
                />}
                {TblUtil.getFilterCount(tableModel)>0 &&
                <img className='PanelToolbar__button'
                     title='Remove all filters'
                     src={CLEAR_FILTERS}
                     onClick={() => TblUtil.clearFilters(tableModel)}
                />}
                <ToolbarButton icon={FILTER}
                               tip='Show/edit filters'
                               visible={true}
                               badgeCount={TblUtil.getFilterCount(tableModel)}
                               onClick={() => toggleOptions('filters')}/>
                <img className='PanelToolbar__button'
                     title='Chart options and tools'
                     src={SETTINGS}
                     onClick={() => toggleOptions('options')}
                />
                { expandable && !expandedMode &&
                <img className='PanelToolbar__button'
                     title='Expand this panel to take up a larger area'
                     src={OUTLINE_EXPAND}
                     onClick={() =>
                     {
                          ChartsCntlr.dispatchChartExpanded(chartId);
                          dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.xyPlots);
                     }}
                />}

                { help_id && <div style={{display: 'inline-block', position: 'relative', top: -9}}> <HelpIcon helpId={help_id} /> </div>}

                { expandable && !expandedMode && deletable &&
                <img style={{display: 'inline-block', position: 'relative', top: -9, alignSelf: 'baseline', padding: 2, cursor: 'pointer'}}
                     title='Delete this chart'
                     src={DELETE}
                     onClick={() => {ChartsCntlr.dispatchChartRemove(chartId);}}
                />}
            </div>
        </div>
    );
}


function renderSelectionButtons(chartId, tblId, tableModel) {
    if (displaySelectionOptions(chartId)) {
        return (
            <div style={{display:'inline-block', whiteSpace: 'nowrap'}}>
                <img className='PanelToolbar__button'
                     title='Zoom in the enclosed points'
                     src={ZOOM_IN}
                     onClick={() => addZoom(chartId)}
                />
                {<img className='PanelToolbar__button'
                     title='Select enclosed points'
                     src={SELECT_ROWS}
                     onClick={() => addSelection(chartId, tblId, tableModel)}
                />}
                <img className='PanelToolbar__button'
                     title='Filter in the selected points'
                     src={FILTER_IN}
                     onClick={() => addFilter(chartId, tableModel)}
                />
            </div>
        );
    }
}

function displaySelectionOptions(chartId) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
    const selection = get(chartDataElement, 'options.selection');
    return Boolean(selection);
}


function displayZoomOriginal(chartId) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
    const zoom = get(chartDataElement, 'options.zoom');
    return Boolean(zoom);
}

function addZoom(chartId) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
    const chartDataElementId = chartDataElement.id;
    setZoom(chartId, chartDataElementId, get(chartDataElement, 'options.selection'));
}


function resetZoom(chartId) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
    const chartDataElementId = chartDataElement.id;
    setZoom(chartId, chartDataElementId);
}

function displayUnselectAll(tableModel) {
    const selectInfo = get(tableModel, 'selectInfo');
    return selectInfo && (selectInfo.selectAll || selectInfo.exceptions.size>0);
}

function addSelection(chartId, tblId, tableModel) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
    if (get(chartDataElement,'data.decimateKey')) {
        showInfoPopup('Your data set is too large to select. You must filter it down first.',
            `Can't Select`); // eslint-disable-line quotes
    } else {
        const selection = get(chartDataElement, 'options.selection');
        const rows = get(chartDataElement,'data.rows');
        if (tableModel && rows && selection) {
            const {xMin, xMax, yMin, yMax} = selection;
            const selectInfoCls = SelectInfo.newInstance({rowCount: tableModel.totalRows});
            // add all rows which fall into selection
            const xIdx = 0, yIdx = 1, rowIdx = 2;
            rows.forEach((arow) => {
                const x = Number(arow[xIdx]);
                const y = Number(arow[yIdx]);
                if (x >= xMin && x <= xMax && y >= yMin && y <= yMax) {
                    selectInfoCls.setRowSelect(Number(arow[rowIdx]), true);
                }
            });
            const selectInfo = selectInfoCls.data;
            TablesCntlr.dispatchTableSelect(tblId, selectInfo);
        }
    }
}


function selectionNotEmpty(chartId, selection) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);
    const rows = get(chartDataElement, 'data.rows');
    if (rows) {
        if (selection) {
            const {xMin, xMax, yMin, yMax} = selection;
            const xIdx = 0, yIdx = 1;
            const aPt = rows.find((arow) => {
                const x = Number(arow[xIdx]);
                const y = Number(arow[yIdx]);
                return (x >= xMin && x <= xMax && y >= yMin && y <= yMax);
            });
            return Boolean(aPt);
        } else {
            return true; // empty selection replacing non-empty
        }
    } else {
        return false;
    }
}


function addFilter(chartId, tableModel) {
    const chartDataElement = ChartsCntlr.getChartDataElement(chartId);

    const options = get(chartDataElement, 'options');
    const selection = get(options, 'selection');
    const xCol = get(options, 'x.columnOrExpr');
    const yCol = get(options, 'y.columnOrExpr');
    if (selection && xCol && yCol) {
        const {xMin, xMax, yMin, yMax} = selection;
        const filterInfo = get(tableModel, 'request.filters');
        const filterInfoCls = FilterInfo.parse(filterInfo);
        filterInfoCls.setFilter(xCol, '> '+xMin);
        filterInfoCls.addFilter(xCol, '< '+xMax);
        filterInfoCls.setFilter(yCol, '> '+yMin);
        filterInfoCls.addFilter(yCol, '< '+yMax);
        const newRequest = Object.assign({}, tableModel.request, {filters: filterInfoCls.serialize()});
        TablesCntlr.dispatchTableFilter(newRequest, 0);
    }
}