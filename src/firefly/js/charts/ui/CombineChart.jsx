/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import {cloneDeep, get, pick, set} from 'lodash';

import {getMultiViewRoot, getViewerItemIds} from '../../visualize/MultiViewCntlr.js';
import {dispatchChartAdd, getChartData} from '../ChartsCntlr.js';
import {getNewTraceDefaults, getTblIdFromChart, isSpectralOrder, uniqueChartId} from '../ChartUtil.js';
import {TextButton} from '../../ui/TextButton.jsx';
import {PINNED_VIEWER_ID, PINNED_GROUP, PINNED_CHART_PREFIX} from './PinnedChartPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {basicOptions, evalChangesFromFields} from './options/BasicOptions.jsx';
import {getCellValue, getColumnValues, getSelectedDataSync, getTblById} from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getGroupFields} from '../../fieldGroup/FieldGroupUtils.js';
import {getActiveViewerItemId} from './MultiChartViewer.jsx';
import {CollapsiblePanel} from '../../ui/panel/CollapsiblePanel.jsx';
import {dispatchTableAddLocal} from '../../tables/TablesCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {showInfoPopup, showPopup} from '../../ui/PopupUtil.jsx';
import {getSpectrumDM} from '../../util/VOAnalyzer.js';
import {applyUnitConversion} from './options/SpectrumOptions.jsx';
import {canUnitConv} from '../dataTypes/SpectrumUnitConversion.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';

const POPUP_ID = 'CombineChart-popup';


/**
 * This component is hard-wired to work with only PINNED_VIEWER_ID.  Will need to change if this ever changes.
 * @param p
 * @param p.viewerId
 * @return {JSX.Element|null}
 * @constructor
 */
export const CombineChart = ({viewerId}) => {

    if (viewerId !== PINNED_VIEWER_ID) return null;

    const chartIds = getViewerItemIds(getMultiViewRoot(), PINNED_VIEWER_ID);
    const showCombine = chartIds?.length > 1;

    const doCombine = async () => {
        const {chartIds, props} = await getCombineChartParams();

        const chartData = combineChart(chartIds, props);
        dispatchChartAdd({
            ...chartData,
            chartId: uniqueChartId(PINNED_CHART_PREFIX),
            groupId: PINNED_GROUP,
            viewerId: PINNED_VIEWER_ID,
            deletable: true,
            mounted: true
        });
    };
    return showCombine ? <TextButton onClick={doCombine} title='Add charts to current chart'>Combine Chart</TextButton> : null;
};

function getChartTitle(cdata, defTitle) {
    return cdata.layout?.title?.text || defTitle;
}

function addTracesTitle(chartId, current=[]) {
    const {data=[], layout} = getChartData(chartId);
    data.forEach((d, idx) => {
        let title = d?.name;
        if (!title && idx === 0) title = layout?.title?.text;
        current.push(title);
    });
    return current;
}

/* returns the selected chartId as well as a table of matching charts to combine */
function createTableModel(showAll, tbl_id) {

    const chartIds = getViewerItemIds(getMultiViewRoot(), PINNED_VIEWER_ID);
    const selChartId = getSelChartId();

    const columns = [
        {name: 'Title', width: 30},
        {name: 'From Table'},
        {name: 'ChartId', visibility: 'hidden'},
    ];
    const data = chartIds
        ?.filter((id) => id !== selChartId)      // exclude already selected chart
        ?.filter((id) => showAll || canCombine(id))     // filter out the charts that cannot be combined with the active chart
        ?.map((id) => getChartData(id))
        ?.map((cdata, idx) => {
            const title = getChartTitle(cdata, `chart-${idx}`);
            const table = getTblById(cdata.tablesources?.[0].tbl_id)?.title;
            return [title, table, cdata.chartId];});

    const table =  {title: 'Charts to combine', tableData:{columns, data}, totalRows: data.length};
    table.tbl_id = tbl_id;
    set(table, 'request.tbl_id', table.tbl_id);
    if (table.totalRows === 1) {
        table.selectInfo  = SelectInfo.newInstance({selectAll:true, rowCount:1}).data;
    } else if (table.totalRows === 0) {
        table.status = {code: 204, message: 'no compatible charts found'};
    }

    return table;
}

const CombineChartDialog = ({onComplete}) => {

    const [showAll, setShowAll] = useState(false);

    const tbl_id = 'combinechart-tbl-id';
    const groupKey = 'combinechart-props';

    useEffect(() => {
        const selTbl = createTableModel(showAll, tbl_id);
        dispatchTableAddLocal(selTbl, undefined, false);
    }, [showAll]);

    const doApply = () => {
        const selChartIds = getColumnValues(getSelectedDataSync(tbl_id), 'ChartId');
        const fields = getGroupFields(groupKey);
        const props = Object.fromEntries(
            Object.entries(fields).map(([k,v]) => [k, v.value])
        );
        if (selChartIds?.length < 1) {
            showInfoPopup('You must select at least one chart to combine with');
        } else {
            onComplete({chartIds:[getSelChartId(), ...selChartIds], props});
            closePopup();
        }
    };
    const closePopup = () => dispatchHideDialog(POPUP_ID);

    const {Title} = basicOptions({groupKey, fieldProps:{labelWidth: 50, size: 40}});
    const showAllHint = 'Show all charts even ones that may not combine well';

    return (
        <div className='CombineChart__popup'>
            {/*--- temporarily removed to only combine charts with similar axes
            <div style={{display: 'inline-flex', alignItems: 'center'}}>
                <label htmlFor='showAll'>Show All Charts: </label>
                <input type='checkbox' id='showAll' title={showAllHint} value={showAll} onClick={(e) => setShowAll(e.target?.checked)}/>
                <span className='CombineChart__hints'>({showAllHint})</span>
            </div>
            ----------*/}
            <div className='CombineChart__popup--tbl'>
                <TablePanel {...{tbl_id, showToolbar:false, showUnits:false, showTypes:false}}/>
            </div>
            <FieldGroup keepState={false} groupKey={groupKey} style={{overflow: 'hidden', display: 'flex', flexDirection: 'column'}}>
                <Title initialState={{value: 'combined'}}/>
                <div style={{marginTop:5}}>Choose trace names below.</div>
                <div style={{overflow: 'auto', maxHeight: 400}}>
                    <SelChartProps {...{tbl_id, groupKey}}/>
                </div>
            </FieldGroup>
            <div className='CombineChart__popup--tbar'>
                <button type='button' className='button std' style={{marginRight: 5}} onClick={doApply}>Ok</button>
                <button type='button' className='button std' onClick={closePopup}>Cancel </button>
                <HelpIcon helpId={'chartarea.combineCharts'} style={{float: 'right', marginTop: 4}}/>
            </div>
        </div>
    );
};

let totalTraces = 0;

const SelChartOpt = ({chartId, groupKey, ctitle, traces, idx}) => {
    const key = `cOpt-${idx}`;

    const TraceOpt = ({traceNum, title}) => {
        const {Name} = basicOptions({activeTrace: traceNum, groupKey, fieldProps:{labelWidth: 32, size: 40}});
        if (!title || title.toLowerCase().startsWith('trace '))  title = `trace ${traceNum}`;

        return (
            <div className='FieldGroup__vertical'>
                <Name initialState={{value: title}}/>
            </div>
        ) ;
    };
    const isOpen = !isSpectralOrder(chartId);
    return (
        <CollapsiblePanel componentKey={key} header={ctitle} isOpen={isOpen}>
            {traces.map((title, idx) => <TraceOpt {...{key:idx, traceNum: totalTraces++, title}}/>)}
        </CollapsiblePanel>
    );

};

function SelChartProps ({tbl_id, groupKey}) {

    useStoreConnector(() => getTblById(tbl_id)?.selectInfo);     // rerender when selectInfo changes
    const selChartId = getSelChartId();
    const selectedData = getSelectedDataSync(tbl_id, ['ChartId']);       // this always return new objects.  that's why it's not used for useStoreConnector

    const getSelCharInfo = (id) => {
        const cTitle = getChartTitle(getChartData(id));
        const traces = addTracesTitle(id);

        return [selChartId, cTitle, traces];
    };

    const charts = [getSelCharInfo(selChartId)];
    totalTraces = 0;

    for(let idx = 0; idx < selectedData.totalRows; idx++) {
        const cid = getCellValue(selectedData, idx, 'ChartId');
        charts.push(getSelCharInfo(cid));
    }

    return (
        <React.Fragment>
            {
                charts.map(([chartId, ctitle, traces], idx) => {
                    if (idx === 0) ctitle = <b>{ctitle}</b>;
                    return <SelChartOpt key={idx} {...{chartId, groupKey, ctitle, traces, idx}}/>;
                })
            }
        </React.Fragment>
    );
};

function getCombineChartParams() {
    return new Promise((resolve) => {
        const onComplete = (props) => resolve(props);
        const content = <CombineChartDialog {...{onComplete}}/>;
        showPopup({ID: POPUP_ID, content, title: 'Add charts to current chart', modal: true});
    });
}


function combineChart(chartIds, props={}) {

    if (chartIds?.length <= 1) return;

    const baseChartId = chartIds[0];
    const baseChartData = cloneDeep(pick(getChartData(baseChartId), ['chartType', 'activeTrace', 'data', 'fireflyData', 'layout', 'tablesources', 'viewerId']));
    const baseXUnit = baseChartData.fireflyData?.[baseChartData?.activeTrace]?.xUnit;
    const baseYUnit = baseChartData.fireflyData?.[baseChartData?.activeTrace]?.yUnit;
    const baseTraceCnt = baseChartData?.data?.length ?? 0;

    baseChartData?.tablesources?.forEach((ts) => Reflect.deleteProperty(ts, '_cancel'));

    for (let i = 1; i < chartIds.length; i++) {
        const chartId = chartIds[i];
        const pxIdx = i+1;      // plotly axis index starts from 1

        const chartData = cloneDeep(getChartData(chartId));
        chartData.activeTrace=0;        // hard-code to only use the first trace from each chart
        const {data, fireflyData, tablesources, layout, activeTrace} = chartData;

        tablesources?.forEach((ts) => Reflect.deleteProperty(ts, '_cancel'));

        // apply unit conversion to xAxis if needed.
        const xUnit = doUnitConversion({chartId, chartData, axisType:'x', to:baseXUnit});
        if (baseXUnit !== xUnit) {
            // create new x-axis
            set(data, [activeTrace, 'xaxis'], 'x' + pxIdx);
            set(baseChartData, ['layout', `xaxis${pxIdx}`], {
                title: layout?.xaxis?.title || `xaxis${pxIdx}`,
                // overlaying: 'x',
                side: i % 2 === 1 ? 'top' : 'bottom'
            });
        }

        // apply unit conversion to yAxis if needed.
        const yUnit = doUnitConversion({chartId, chartData, axisType:'y', to:baseYUnit});

        if (baseYUnit !== yUnit) {
            // create new y-axis
            set(data, [activeTrace, 'yaxis'], 'y'+pxIdx);
            set(data, [activeTrace, 'name'], 'y'+pxIdx + ' data');
            set(baseChartData, ['layout', `yaxis${pxIdx}`], {
                title: layout?.yaxis?.title || `yaxis${pxIdx}`,
                overlaying: 'y',
                side: i%2 === 1 ? 'right' : 'left'
            });
        }

        const xmapped = (data?.[activeTrace]?.x || '').startsWith?.('tables:');
        const ymapped = (data?.[activeTrace]?.y || '').startsWith?.('tables:');
        if (xmapped ^ ymapped) {
            // all mapped fields should be handled: x, y, error_y.array, etc.
            Object.entries(tablesources?.[activeTrace]?.mappings || {}).forEach(([key, val]) => {
                // need get to handle compound keys, like 'error_y.array'
                if (!get(data, `${activeTrace}.${key}`)?.startsWith?.('tables:')) {
                    set(data, `${activeTrace}.${key}`, `tables::${val}`);
                }
            });
        }

        // merge selected chart data into new chart
        data         && baseChartData.data.push(...data);
        fireflyData  && baseChartData.fireflyData.push(...fireflyData);
        tablesources && baseChartData.tablesources.push(...tablesources);
    }

    baseChartData.data.forEach((trace, idx) => {
        if (idx >= baseTraceCnt) {
            // apply defaults settings to newly created traces
            Object.entries(getNewTraceDefaults(null, trace?.type, idx))
                .forEach(([k,v]) => !props[k] && (props[k] = v));
        }
    });

    // override new baseChartData using lodash.set fashion
    Object.entries(props).forEach(([key, val]) => {
        set(baseChartData, key, val);
    });

    return baseChartData;
}

/* return unit for the given axisType */
function doUnitConversion({chartId, chartData, axisType, to}) {
    const {activeTrace, fireflyData, data} = chartData;

    // apply unit conversion to axis if needed.
    let unit = fireflyData?.[activeTrace]?.[`${axisType}Unit`];
    if (to !== unit) {
        if (canUnitConv({from: unit, to})) {
            const tbl_id = getTblIdFromChart(chartId, activeTrace);
            const axis = getSpectrumDM(getTblById(tbl_id))?.[axisType === 'x' ? 'spectralAxis' : 'fluxAxis'];
            // unit conversion changes for input fields
            let changes = applyUnitConversion({
                fireflyData,
                data,
                inFields: {},
                axisType,
                newUnit: to,
                traceNum: activeTrace,
                axis
            });
            // field changes must be converted to state changes
            changes = evalChangesFromFields(chartId, tbl_id, changes);
            Object.entries(changes).forEach(([key, val]) => {
                set(chartData, key, val);
            });
            unit = to;
            set(fireflyData, [activeTrace, `${axisType}Unit`], unit);
        }
    }
    return unit;
}

/**
 * @param chartId  chart ID of the chart to combine
 * @return {boolean} true if the selected chart and the given chart have the same unit for its x-axis and y-axis.
 */
function canCombine(chartId) {

    const activeTrace = 0;          // hard-code to only use the first trace from each chart
    const selChartId = getSelChartId();
    const {xUnit, yUnit} = getChartData(selChartId)?.fireflyData?.[activeTrace];
    if (!xUnit || !yUnit) return false;

    const chartData = cloneDeep(getChartData(chartId));

    const newXUnit = doUnitConversion({chartId, chartData, axisType:'x', to:xUnit});
    const newYUnit = doUnitConversion({chartId, chartData, axisType:'y', to:yUnit});
    return xUnit === newXUnit && yUnit === newYUnit;
}

function getSelChartId(viewerId=PINNED_VIEWER_ID) {
    return  getActiveViewerItemId(viewerId, true);
}
