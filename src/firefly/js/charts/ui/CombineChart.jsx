/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import {cloneDeep, get, pick, set} from 'lodash';

import {getMultiViewRoot, getViewerItemIds, PINNED_CHART_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';
import {getSpectrumDM} from '../../voAnalyzer/SpectrumDM.js';
import {dispatchChartAdd, getChartData} from '../ChartsCntlr.js';
import {getNewTraceDefaults, getTblIdFromChart, isSpectralOrder, uniqueChartId} from '../ChartUtil.js';
import {PINNED_GROUP, PINNED_CHART_PREFIX} from './PinnedChartContainer.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {evalChangesFromFields, useBasicOptions} from './options/BasicOptions.jsx';
import {getColumnValues, getSelectedDataSync, getTblById} from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {getFieldVal, getGroupFields} from '../../fieldGroup/FieldGroupUtils.js';
import {getActiveViewerItemId} from './MultiChartViewer.jsx';
import {CollapsibleGroup, CollapsibleItem} from '../../ui/panel/CollapsiblePanel.jsx';
import {dispatchTableAddLocal} from '../../tables/TablesCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {showInfoPopup, showPopup} from '../../ui/PopupUtil.jsx';
import {applyUnitConversion} from './options/SpectrumOptions.jsx';
import {canUnitConv} from '../dataTypes/SpectrumUnitConversion.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {CombineChartButton} from 'firefly/visualize/ui/Buttons.jsx';
import {Alert, Button, Stack, Typography} from '@mui/joy';
import CompleteButton from 'firefly/ui/CompleteButton.jsx';
import {getColValStats} from 'firefly/charts/TableStatsCntlr';
import {quoteNonAlphanumeric} from 'firefly/util/expr/Variable';
import {ValidationField} from 'firefly/ui/ValidationField';
import Validate from 'firefly/util/Validate';
import {SwitchInputField} from 'firefly/ui/SwitchInputField';
import PropTypes from 'prop-types';
import {GridMask} from 'firefly/ui/panel/MaskPanel';
import WarningIcon from '@mui/icons-material/WarningRounded';

const POPUP_ID = 'CombineChart-popup';


/**
 * A generic component that takes two or more charts as input (along with other parameters) and creates a combined
 * chart (if possible) in the pinned-charts viewer.
 *
 * @param p
 * @param {Array.<string> | Array.<Promise>} p.chartIds ids of all charts to combine
 * @param {string} p.selectedChartId id of the chart selected to combine other charts with. If undefined, it's chartIds[0]
 * @param {boolean} p.showChartSelectionTable whether to show a table in the UI dialog to select the input chartIds
 * @param {function(string):string} p.deriveTraceTitle function to create trace title from the title of chart. Default is to use first 10 characters.
 * @param {function():void} p.onCombineComplete callback function to run after combined chart is added
 * @param {Object} p.slotProps
 * @return {JSX.Element|null}
 * @constructor
 */
export const CombineChart = ({chartIds, selectedChartId, showChartSelectionTable=true,
                                 deriveTraceTitle, onCombineComplete, slotProps}) => {
    const doCombine = async () => {
        const params = await getParamsFromDialog({chartIds, selectedChartId, showChartSelectionTable, deriveTraceTitle, slotProps});
        const combinedChartData = combineChart(params);

        dispatchChartAdd({
            ...combinedChartData,
            chartId: uniqueChartId(PINNED_CHART_PREFIX),
            groupId: PINNED_GROUP,
            viewerId: PINNED_CHART_VIEWER_ID,
            deletable: true,
            mounted: true
        });

        onCombineComplete?.(); // post-combination callback
    };

    return (chartIds?.length > 1)
        ? <CombineChartButton onClick={doCombine} {...slotProps?.button}/>
        : null;
};

CombineChart.propTypes = {
    chartIds: PropTypes.oneOfType([
        PropTypes.arrayOf(PropTypes.string),
        PropTypes.arrayOf(PropTypes.instanceOf(Promise)),
    ]).isRequired,
    selectedChartId: PropTypes.string,
    showChartSelectionTable: PropTypes.bool,
    onCombineComplete: PropTypes.func,
    deriveTraceTitle: PropTypes.func,
    slotProps: PropTypes.shape({
        button: PropTypes.object,
        dialog: PropTypes.object,
    }),
};


/**
 * Retrieves charts from the pinned-charts container and creates a combined chart there.
 *
 * @param p
 * @param {string} p.viewerId ID of the viewer where this component is placed
 * @param {Object} p.slotProps
 * @returns {JSX.Element}
 * @constructor
 */
export const CombinePinnedCharts = ({viewerId, slotProps}) => {
    if (viewerId !== PINNED_CHART_VIEWER_ID) return null;

    const chartIds = getViewerItemIds(getMultiViewRoot(), viewerId);
    const selectedChartId = getActiveViewerItemId(viewerId, true);
    return <CombineChart {...{chartIds, selectedChartId, slotProps}} />;
};

CombinePinnedCharts.propTypes = {
    viewerId: PropTypes.string,
    slotProps: CombineChart.propTypes.slotProps,
};


function getChartTitle(cdata, defTitle) {
    return cdata.layout?.title?.text || defTitle;
}

function generateTraceTitles(chartId, deriveTraceTitle) {
    const {data=[], layout} = getChartData(chartId);
    const chartTitle = layout?.title?.text ?? chartId;
    const traceTitles = [];

    let traceTitleBase = deriveTraceTitle?.(chartTitle);
    if (!traceTitleBase) {
        traceTitleBase = chartTitle.substring(0, 10);
        if (chartTitle.length > 10) traceTitleBase += '...';
    }

    if (data.length === 1) {
        //one trace chart doesn't require any suffix
        traceTitles.push(traceTitleBase);
    }
    else {
        //multi trace chart needs trace info to be suffixed
        data.forEach((traceData, idx) => {
            let traceTitle = traceData?.name;
            if (!traceTitle)  traceTitle = `trace ${idx}`;
            traceTitles.push(`${traceTitleBase}: ${traceTitle}`);
        });
    }
    return traceTitles;
}

/* returns the selected chartId as well as a table of matching charts to combine */
function createTableModel(chartIds, selectedChartId, showAll, tbl_id) {
    const columns = [
        {name: 'Title', width: 30},
        {name: 'From Table'},
        {name: 'ChartId', visibility: 'hidden'},
    ];
    const data = chartIds
        ?.filter((id) => id !== selectedChartId)      // exclude already selected chart
        ?.filter((id) => showAll || canCombine(id, selectedChartId))     // filter out the charts that cannot be combined with the active chart
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

const ChartSelectionTable = ({tbl_id, chartIds, selectedChartId}) => {
    const [showAll, setShowAll] = useState(false);
    const showAllHint = 'Show all charts even ones that may not combine well';

    useEffect(() => {
        const selTbl = createTableModel(chartIds, selectedChartId ?? chartIds[0], showAll, tbl_id);
        dispatchTableAddLocal(selTbl, undefined, false);
    }, [showAll]);

    return (
        <Stack height={125}>
            {/*--- temporarily removed to only combine charts with similar axes
                <div style={{display: 'inline-flex', alignItems: 'center'}}>
                    <label htmlFor='showAll'>Show All Charts: </label>
                    <input type='checkbox' id='showAll' title={showAllHint} value={showAll} onClick={(e) => setShowAll(e.target?.checked)}/>
                    <span className='CombineChart__hints'>({showAllHint})</span>
                </div>
                ----------*/}
            <TablePanel {...{tbl_id, showToolbar: false, showUnits: false, showTypes: false}}/>
        </Stack>
    );
};


const CombineChartDialog = ({onComplete, chartIds, selectedChartId, showChartSelectionTable, deriveTraceTitle}) => {
    const tbl_id = 'combinechart-tbl-id';
    const groupKey = 'combinechart-props';

    const [resolvedChartIds, setResolvedChartIds] = useState([]);
    const [rejectedChartReasons, setRejectedChartReasons] = useState([]);
    const doCascading = useStoreConnector(()=>getFieldVal(groupKey, 'doCascading'));
    const tblSelectedChartIds = useStoreConnector(()=>
        getColumnValues(getSelectedDataSync(tbl_id), 'ChartId'));

    useEffect(() => {
        if (chartIds[0] instanceof Promise) {
            // store fulfilled values and rejected reasons in the state when available
            Promise.allSettled(chartIds).then((results) => {
                setResolvedChartIds(results.filter(({status})=> status==='fulfilled')
                    .map(({value}) => value));
                setRejectedChartReasons(results.filter(({status})=> status==='rejected')
                    .map(({reason}) => reason));
            });
        } else {
            setResolvedChartIds(chartIds);
        }
    }, [chartIds]);


    const chartIdsToCombine = showChartSelectionTable
        ? [selectedChartId ?? chartIds[0], ...tblSelectedChartIds] //selectedChartId isn't present in table
        : resolvedChartIds; //TODO: run chartIds through canCombine() and show warning if not?

    const doApply = () => {
        const fields = getGroupFields(groupKey);
        const props = Object.fromEntries(
            Object.entries(fields).map(([k,v]) => [k, v.value])
        );

        if (showChartSelectionTable && tblSelectedChartIds?.length < 1) {
            showInfoPopup('You must select at least one chart to combine with');
        } else {
            onComplete({chartIds: chartIdsToCombine, ...props});
            closePopup();
        }
    };
    const closePopup = () => dispatchHideDialog(POPUP_ID);

    const {Title} = useBasicOptions({groupKey});

    const dimensionsSx = {height: 500, width: 700};

    if (resolvedChartIds?.length + rejectedChartReasons?.length === 0) { //still loading chart ids
        return (<GridMask rows={5} cols={1} sx={dimensionsSx}/>);
    }
    return (
        <FieldGroup groupKey={groupKey} sx={{
            ...dimensionsSx,
            resize: 'both',
            overflow: 'hidden',
            position: 'relative'
        }}>
            <Stack spacing={1} height={1}>
                <Stack spacing={2} overflow='auto' flexGrow={1}>
                    <FailedChartWarnings errors={rejectedChartReasons}/>
                    {showChartSelectionTable && <ChartSelectionTable {...{chartIds: resolvedChartIds, selectedChartId, tbl_id}} />}
                    <Title initialState={{value: 'combined'}}/>
                    <CascadePlots {...{doCascading}}/>
                    <SelChartProps {...{chartIds: chartIdsToCombine, groupKey, showOrder: doCascading, deriveTraceTitle}}/>
                </Stack>

                <Stack direction='row' justifyContent='space-between' alignItems='center'>
                    <Stack direction='row' spacing={1}>
                        <CompleteButton onSuccess={doApply}>Ok</CompleteButton>
                        <Button variant='soft' onClick={closePopup}>Cancel</Button>
                    </Stack>
                    <HelpIcon helpId={'chartarea.combineCharts'}/>
                </Stack>
            </Stack>
        </FieldGroup>
    );
};

const FailedChartWarnings = ({errors}) => (
    <Stack spacing={1}>
        {errors.map((err, i)=>(
            <Alert color='warning' key={i} startDecorator={<WarningIcon/>}>
                {err?.message}
            </Alert>
        ))}
    </Stack>
);

const CascadePlots = ({doCascading}) => {
    const paddingValidator = (val) => Validate.floatRange(-1,1,0,'Cascade Padding', val, false);
    return (
        <Stack spacing={1}>
            <SwitchInputField fieldKey='doCascading' label={'Apply cascading:'} initialState={{value: false}}/>
            {doCascading &&
                <Stack spacing={.5} pl={1}>
                    <Typography>Y-axis: (y - min(y)) / (max(y) - min(y)) + (<b>i</b> * <b>P</b>)</Typography>
                    <ValidationField fieldKey={'cascadePadding'}
                                     initialState={{value: 1}}
                                     label='Padding (P):'
                                     orientation={'horizontal'}
                                     validator={paddingValidator}
                    />
                </Stack>
            }
        </Stack>
    );
};

let totalTraces = 0;

const SelChartOpt = ({chartId, groupKey, header, traceTitles, idx}) => {
    const key = `cOpt-${idx}`;

    const TraceOpt = ({traceNum, title}) => {
        const {Name} = useBasicOptions({activeTrace: traceNum, groupKey});
        return <Name initialState={{value: title}}/>;
    };
    const isOpen = !isSpectralOrder(chartId);
    return (
        <CollapsibleItem componentKey={key} header={header} isOpen={isOpen}>
            <Stack spacing={1}>
                {traceTitles.map((title, idx) => <TraceOpt {...{key:idx, traceNum: totalTraces++, title}}/>)}
            </Stack>
        </CollapsibleItem>
    );
};

function SelChartProps ({chartIds, groupKey, showOrder, deriveTraceTitle}) {
    const getSelCharInfo = (id) => {
        const chartTitle = getChartTitle(getChartData(id));
        const traceTitles = generateTraceTitles(id, deriveTraceTitle);
        return [id, chartTitle, traceTitles];
    };

    totalTraces = 0;

    return (
        <Stack spacing={1}>
            <Typography level='title-sm'>Choose trace names below:</Typography>
            <CollapsibleGroup>
                {
                    chartIds.map(getSelCharInfo).map(([chartId, chartTitle, traceTitles], idx) => {
                        const header = (
                            <Stack direction='row' spacing={1} alignItems='baseline'>
                                <Typography {...(idx===0 && {fontWeight: 'lg'})}>{chartTitle}</Typography>
                                {showOrder && <Typography level='body-sm'>(i={idx})</Typography>}
                            </Stack>
                        );
                        return <SelChartOpt key={idx} {...{chartId, groupKey, header, traceTitles, idx}}/>;
                    })
                }
            </CollapsibleGroup>
        </Stack>
    );
}

function getParamsFromDialog(props) {
    return new Promise((resolve) => {
        const onComplete = (params) => resolve(params);
        const content = <CombineChartDialog {...{onComplete, ...props}}/>;
        showPopup({ID: POPUP_ID, content, title: 'Add charts to current chart', modal: true,
            ...props?.slotProps?.dialog});
    });
}


function combineChart({chartIds, doCascading, cascadePadding, ...plottingProps}) {

    if (chartIds?.length <= 1) return;

    const baseChartId = chartIds[0];
    const baseChartData = cloneDeep(pick(getChartData(baseChartId), ['chartType', 'activeTrace', 'data', 'fireflyData', 'layout', 'tablesources', 'viewerId']));
    const baseXUnit = baseChartData.fireflyData?.[baseChartData?.activeTrace]?.xUnit;
    const baseYUnit = baseChartData.fireflyData?.[baseChartData?.activeTrace]?.yUnit;
    const baseTraceCnt = baseChartData?.data?.length ?? 0;

    baseChartData?.tablesources?.forEach((ts) => Reflect.deleteProperty(ts, '_cancel'));

    doCascading && applyCascadingAlgo(baseChartId, baseChartData, 0, cascadePadding);

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

        doCascading && applyCascadingAlgo(chartId, chartData, i, cascadePadding);

        // merge selected chart data into new chart
        data         && baseChartData.data.push(...data);
        fireflyData  && baseChartData.fireflyData.push(...fireflyData);
        tablesources && baseChartData.tablesources.push(...tablesources);
    }

    baseChartData.data.forEach((trace, idx) => {
        if (idx >= baseTraceCnt) {
            // apply defaults settings to newly created traces
            Object.entries(getNewTraceDefaults(null, trace?.type, idx))
                .forEach(([k,v]) => !plottingProps[k] && (plottingProps[k] = v));
        }
    });

    // override new baseChartData using lodash.set fashion
    Object.entries(plottingProps).forEach(([key, val]) => {
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

function applyCascadingAlgo(chartId, chartData, idx, padding) {
    const offset = idx * padding;

    // check if yMapping of all the traces is same before applying algorithm
    const yMappings = chartData?.tablesources?.map((tableSource)=>tableSource?.mappings?.y);
    if (!yMappings || yMappings.length < 1 || !yMappings.every((y) => y===yMappings[0])) return;
    const yColName = quoteNonAlphanumeric(yMappings[0]);

    const colValStats = getColValStats(getTblIdFromChart(chartId));
    const {min=0, max=1} = colValStats.find((col) => col?.name===yMappings[0]);

    // algorithm is applied to all the traces of a chart
    chartData?.data.forEach((_, traceNum) => {
        //wrap min, max, offset in parentheses to handle negative values
        set(chartData, ['data', traceNum, 'y'], `tables::(${yColName}-(${min})) / ((${max})-(${min})) + (${offset})`);
    });
}

/**
 * @param chartId  chart ID of the chart to combine
 * @param selectedChartId chart ID of the selected chart with which the given chart will be combined
 * @return {boolean} true if the selected chart and the given chart have the same unit for its x-axis and y-axis.
 */
function canCombine(chartId, selectedChartId) {

    const activeTrace = 0;          // hard-code to only use the first trace from each chart
    const {xUnit, yUnit} = getChartData(selectedChartId)?.fireflyData?.[activeTrace];
    if (!xUnit || !yUnit) return false;

    const chartData = cloneDeep(getChartData(chartId));

    const newXUnit = doUnitConversion({chartId, chartData, axisType:'x', to:xUnit});
    const newYUnit = doUnitConversion({chartId, chartData, axisType:'y', to:yUnit});
    return xUnit === newXUnit && yUnit === newYUnit;
}
