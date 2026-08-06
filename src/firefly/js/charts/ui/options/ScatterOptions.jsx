import React, {useCallback, useEffect} from 'react';
import {get, has, isUndefined, omit, range, isString, defaultsDeep} from 'lodash';
import {Stack, Typography} from '@mui/joy';

import {Expression} from '../../../util/expr/Expression.js';
import {dispatchChartUpdate, getChartData, hasUpperLimits} from '../../ChartsCntlr.js';
import {
    getChartProps, getGroupableColumns, getGroupByColumn, getMinScatterGLRows,
    isGroupByAllowed, isGroupedChart, makeScatterGroupByChanges
} from '../../ChartUtil.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';

import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {basicFieldReducer, evalChangesFromFields, LayoutOptions, submitChanges, useBasicOptions,} from './BasicOptions.jsx';
import {toBoolean, updateSet} from '../../../util/WebUtil.js';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {getColValStats, useTableColValStats} from '../../TableStatsCntlr.js';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {
    Error_X, Error_Y, errorFieldKey, errorMinusFieldKey, errorShowFieldKey, errorTypeFieldKey, getDefaultErrorType
} from './Errors.jsx';
import {getAppOptions} from '../../../core/AppDataCntlr.js';
import {getTblById} from '../../../tables/TableUtil.js';
import {PlotlyCS} from '../../Colorscale.js';
import {getSpectrumProps, spectrumType} from '../../dataTypes/FireflySpectrum.js';
import {
    CollapsibleGroup,
    CollapsibleItem
} from '../../../ui/panel/CollapsiblePanel.jsx';
import {hideColSelectPopup} from '../ColSelectView.jsx';
import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {dispatchComponentStateChange} from '../../../core/ComponentCntlr.js';

const TRACE_OPTIONS_COMPONENT_KEY = 'chart-scatter-options';


/**
 * Should we display Upper Limit field under Y?
 * @returns {*}
 */
export function yLimitUI() {
    const upperLimitUI =  get(getAppOptions(), 'charts.upperLimitUI', false);
    return upperLimitUI || get(getAppOptions(), 'charts.yLimitUI', false);
}

/**
 * @param p
 * @param p.activeTrace  given when adding a new chart or a new trace
 * @param p.tbl_id       given when adding a new chart
 * @param p.chartId
 * @param p.groupKey
 * @returns {*}
 * @constructor
 */
export function ScatterOptions({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const activeTrace = useStoreConnector(() => pActiveTrace ?? getChartData(chartId)?.activeTrace);
    useEffect(() => {
        return () => hideColSelectPopup();
    }, []);

    groupKey = groupKey || `${chartId}-scatter-${activeTrace}`;
    const {tbl_id, tablesource, dataType} = getChartProps(chartId, ptbl_id, activeTrace);
    const {UseSpectrum, X, Y, Yerrors, Xerrors, Ymax, Ymin, GroupBy} = useScatterInputs({activeTrace, tbl_id, chartId, groupKey});
    const showUseSpectrum = !isGroupedChart(chartId) && dataType === spectrumType;

    const reducerFunc = fieldReducer({chartId, activeTrace, tbl_id});
    reducerFunc.ver = chartId+activeTrace+tbl_id;

    return (
        <FieldGroup keepState={false} groupKey={groupKey} reducerFunc={reducerFunc}>
            <Stack spacing={3}>
                <Stack spacing={2} sx={{'.MuiFormLabel-root': {width: '5rem'}}}>
                    {showUseSpectrum && <UseSpectrum/>}
                    {tablesource &&
                        <Stack spacing={2}>
                            <Typography level='body-sm'>
                                For X and Y, enter a column or an expression<br/>
                                ex. log(col); 100*col1/col2; col1-col2
                            </Typography>
                            <Stack spacing={1}>
                                <X/>
                                <Xerrors/>
                            </Stack>
                            <Stack spacing={1}>
                                <Y/>
                                {(yLimitUI() || hasUpperLimits(chartId, activeTrace)) && <Ymax/>}
                                {(yLimitUI() || hasUpperLimits(chartId, activeTrace)) && <Ymin/>}
                                <Yerrors/>
                            </Stack>
                            <GroupBy/>
                        </Stack>
                    }
                </Stack>
                <CollapsibleGroup>
                    <ScatterCommonOptions {...{activeTrace, tbl_id, chartId, groupKey}}/>
                    <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey}}/>
                </CollapsibleGroup>
            </Stack>
        </FieldGroup>
    );
}

export function ScatterCommonOptions({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey, ...props}) {

    const {activeTrace, tbl_id, noColor, multiTrace} = getChartProps(chartId, ptbl_id, pActiveTrace);
    const {Symbol, ColorMap, ColorSize, ColorScale, Mode} = useScatterInputs({activeTrace, tbl_id, chartId, groupKey});
    const {ChooseTrace, Name, Color} = useBasicOptions({activeTrace, tbl_id, chartId, groupKey});
    const colValStats = useTableColValStats(tbl_id);
    const isGrouped = isGroupedChart(chartId);

    useEffect(() => {
        dispatchComponentStateChange(TRACE_OPTIONS_COMPONENT_KEY, {isOpen: isGrouped});
    }, [chartId, isGrouped]);

    return (
        <CollapsibleItem header='Trace Options' componentKey={TRACE_OPTIONS_COMPONENT_KEY} isOpen={isGrouped}
                                   {...props /* to allow AccordionGroup styling props inserted by joy UI, propagate to Accordion */}>
            <Stack spacing={2} sx={{'.MuiFormLabel-root': {width: '6rem'}}}>
                {isGrouped && (
                    <Stack spacing={1}>
                        <Typography level='body-sm'>You can modify the selected trace:</Typography>
                        <ChooseTrace/>
                    </Stack>
                )}
                {multiTrace && <Name/>}
                <Mode/>
                <Symbol/>
                {!noColor && <Color/>}
                {colValStats && !isGrouped && (
                    <Stack spacing={1}>
                        <ColorMap/>
                        <ColorScale/>
                        <ColorSize/>
                    </Stack>
                )}
            </Stack>
        </CollapsibleItem>
    );
}


export function fieldReducer({chartId, activeTrace, tbl_id}) {

    const basicReducer = basicFieldReducer({chartId, activeTrace});

    return (inFields, action) => {
        if (!inFields) return ;

        inFields = basicReducer(inFields, action);

        const {payload:{fieldKey='', value=''}, type} = action;

        const chartData = getChartData(chartId);
        const {data} = chartData;

        if (type === VALUE_CHANGE) {
            const colorKey = `data.${activeTrace}.marker.color`;
            const colorMapKey = `_tables.data.${activeTrace}.marker.color`;
            if (fieldKey === colorKey && value) {
                if (inFields[colorMapKey]?.value) inFields = updateSet(inFields, [colorMapKey, 'value'], '');    // blanks out color map when a color is entered
            } else if (fieldKey === colorMapKey && value) {
                if (inFields[colorKey]?.value) inFields = updateSet(inFields, [colorKey, 'value'], '');          // blanks out color when a color map is entered
            }
            // when field changes, clear error fields
            ['x','y'].forEach((a) => {
                if (fieldKey === `_tables.data.${activeTrace}.${a}`) {
                    inFields = updateSet(inFields, [errorTypeFieldKey(activeTrace, `${a}`), 'value'], undefined);
                    inFields = updateSet(inFields, [errorFieldKey(activeTrace, `${a}`), 'value'], undefined);
                    inFields = updateSet(inFields, [errorMinusFieldKey(activeTrace, `${a}`), 'value'], undefined);
                }
            });

            // when switches back to useSpectrum, populate all spectrum read-only fields with values from SpectrumDM
            if (fieldKey === `fireflyData.${activeTrace}.useSpectrum`) {
                if (value) {
                    const {xUnit, yUnit, x, y, xErrArray, xErrArrayMinus, yErrArray, yErrArrayMinus, xMax, xMin, yMax, yMin, xLabel, yLabel} = getSpectrumProps(tbl_id);
                    inFields = updateSet(inFields, [`fireflyData.${activeTrace}.xUnit`, 'value'], xUnit);
                    inFields = updateSet(inFields, [`fireflyData.${activeTrace}.yUnit`, 'value'], yUnit);

                    inFields = updateSet(inFields, [`_tables.data.${activeTrace}.x`, 'value'], x);
                    inFields = updateSet(inFields, [`_tables.data.${activeTrace}.y`, 'value'], y);

                    inFields = updateSet(inFields, [errorTypeFieldKey(activeTrace, 'x'), 'value'], getDefaultErrorType(chartData, activeTrace, 'x'));
                    inFields = updateSet(inFields, [errorFieldKey(activeTrace, 'x'), 'value'], xErrArray);
                    inFields = updateSet(inFields, [errorMinusFieldKey(activeTrace, 'x'), 'value'], xErrArrayMinus);
                    inFields = updateSet(inFields, [errorTypeFieldKey(activeTrace, 'y'), 'value'], getDefaultErrorType(chartData, activeTrace, 'y'));
                    inFields = updateSet(inFields, [errorFieldKey(activeTrace, 'y'), 'value'], yErrArray);
                    inFields = updateSet(inFields, [errorMinusFieldKey(activeTrace, 'y'), 'value'], yErrArrayMinus);

                    inFields = updateSet(inFields, [`_tables.fireflyData.${activeTrace}.xMax`, 'value'], xMax);
                    inFields = updateSet(inFields, [`_tables.fireflyData.${activeTrace}.xMin`, 'value'], xMin);
                    inFields = updateSet(inFields, [`_tables.fireflyData.${activeTrace}.yMax`, 'value'], yMax);
                    inFields = updateSet(inFields, [`_tables.fireflyData.${activeTrace}.yMin`, 'value'], yMin);

                    inFields = updateSet(inFields, ['layout.xaxis.title.text', 'value'], xLabel);
                    inFields = updateSet(inFields, ['layout.yaxis.title.text', 'value'], yLabel);
                }
                // useSpectrum should be the same for all traces
                range(data.length).forEach((idx) => {
                    inFields = updateSet(inFields, [`fireflyData.${idx}.useSpectrum`, 'value'], value);
                });
            }

        }
        return inFields;

    };
}

export function submitChangesScatter({chartId, activeTrace, fields, tbl_id, renderTreeId}) {

    // trace type can switch between scatter and scattergl depending on the number of points
    const changes = {[`data.${activeTrace}.type`] : getTraceType(chartId, tbl_id, activeTrace)};

    // highlighted and selected traces might need to be updated too
    const {highlighted, selected} = getChartData(chartId);

    // check if size field is a constant
    const sizeMap = fields[`_tables.data.${activeTrace}.marker.size`];
    if (sizeMap) {
        const colValStats = getColValStats(tbl_id);
        const colNames = colValStats.map((colVal) => {return colVal.name;});
        const expr = new Expression(sizeMap, colNames);
        if (expr.isValid() && (expr.getParsedVariables().length === 0)) {
            const symSize = expr.getValue();
            changes[`data.${activeTrace}.marker.size`] = symSize;
            if (highlighted) { changes['highlighted.marker.size'] = symSize; }
            if (selected) { changes['selected.marker.size'] = symSize; }
            fields = omit(fields, `_tables.data.${activeTrace}.marker.size`);
        }
    }
    
    // check if error should be visible or symmetric
    ['x','y'].forEach((a) => {
        const errorsType = fields[errorTypeFieldKey(activeTrace, a)];
        const errorsVisible = toBoolean(fields[errorShowFieldKey(activeTrace, a)]);
        changes[`data.${activeTrace}.error_${a}.visible`] = errorsVisible;
        if (highlighted) { changes[`highlighted.error_${a}.visible`] = errorsVisible; }
        if (selected) { changes[`selected.error_${a}.visible`] = errorsVisible; }
        if (errorsVisible) {
            const errorsSymmetric = errorsType === 'sym';
            changes[`data.${activeTrace}.error_${a}.symmetric`] = errorsSymmetric;
            if (highlighted) { changes[`highlighted.error_${a}.symmetric`] = errorsSymmetric; }
            if (selected) { changes[`selected.error_${a}.symmetric`] = errorsSymmetric; }
        }
    });

    Object.assign(changes, fields);

    const groupByKey = groupByFieldKey(activeTrace);
    const groupByColumn = fields[groupByKey] || '';
    const currentGroupBy = getGroupByColumn(chartId) || '';
    const groupByChanged = groupByColumn !== currentGroupBy;
    if (chartId && has(fields, groupByKey) && groupByChanged && isGroupByAllowed(chartId, activeTrace)) {
        const groupedFields = omit(changes, groupByKey);
        const evalChanges = evalChangesFromFields(chartId, tbl_id, groupedFields);
        const groupChanges = makeScatterGroupByChanges({chartId, tbl_id, activeTrace, groupByColumn, changes: evalChanges});
        dispatchChartUpdate({chartId, changes: groupChanges, replaceTableSources: true});
        return;
    }
    submitChanges({chartId, fields: changes, tbl_id, renderTreeId});
}

/**
 * Returns gl or non-gl scatter type based on the number of table rows (if unknown)
 * @param chartId
 * @param tbl_id
 * @param activeTrace
 */
function getTraceType(chartId, tbl_id, activeTrace) {
    const chartData = getChartData(chartId);
    let type = get(chartData, `data.${activeTrace}.type`);
    if (isUndefined(type)) {
        const {totalRows=0}= getTblById(tbl_id);
        // use scatter or scattergl depending on the number of rows
        type = (totalRows > getMinScatterGLRows()) ? 'scattergl' : 'scatter';
    }
    return type;
}

/*
 * This function returns a collection of components using `useCallback`, ensuring they are not recreated between re-renders.
 * To modify this behavior, you can set the `baseDeps` parameter accordingly.
 */
export const useScatterInputs = ({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey, orientation='horizontal'}, baseDeps=[pActiveTrace]) => {
    const {activeTrace, tbl_id, data, fireflyData, mappings} = getChartProps(chartId, ptbl_id, pActiveTrace);
    const colValStats = useTableColValStats(tbl_id);
    const deps = [...baseDeps, colValStats];
    const strOrNull = (v) => isString(v) ? v : undefined;

    const withDefaults = (props) => {
        const defaults = {slotProps: {control: {orientation}}};
        // deep-merge defaults to props only if they don't exist already
        return defaultsDeep({...props}, defaults); //pass a copy of props to avoid mutation
    };

    return {
        Mode: useCallback((props) => ( <ListBoxInputField fieldKey={`data.${activeTrace}.mode`}
                                                 label='Trace Style:'
                                                 initialState= {{value: strOrNull(get(data, `${activeTrace}.mode`))}}
                                                 options={[{label: 'points', value:'markers'},
                                                     {label: 'connected points', value:'lines+markers'},
                                                     {label: 'lines', value:'lines'}]}
                                                 {...withDefaults(props)}/>), deps),
        Symbol: useCallback((props) => (<ListBoxInputField fieldKey={`data.${activeTrace}.marker.symbol`}
                                                  label='Symbol:'
                                                  initialState= {{value: strOrNull(get(data, `${activeTrace}.marker.symbol`))}}
                                                  options={[{value:'circle'}, {value:'circle-open'}, {value:'square'}, {value:'square-open'},
                                                      {value:'diamond'}, {value:'diamond-open'},{value:'cross'}, {value:'x'},
                                                      {value:'triangle-up'}, {value:'hexagon'}, {value:'star'}]}
                                                  {...withDefaults(props)}/>), deps),
        X: useCallback((props) => (<ColumnOrExpression fldPath={`_tables.data.${activeTrace}.x`}
                                      initValue= {get(mappings, 'x', '')}
                                      label='X:'
                                      name='X'
                                      nullAllowed={false}
                                      colValStats={colValStats}
                                      tbl_id={tbl_id}
                                      groupKey={groupKey} {...withDefaults(props)}/>), deps),
        Y: useCallback((props) => (<ColumnOrExpression fldPath={`_tables.data.${activeTrace}.y`}
                                      initValue={get(mappings, 'y', '')}
                                      label='Y:'
                                      name='Y'
                                      nullAllowed={false}
                                      colValStats={colValStats}
                                      tbl_id={tbl_id}
                                      groupKey={groupKey} {...withDefaults(props)}/>), deps),
        GroupBy: useCallback((props) => {
                                    const groupableCols = chartId && isGroupByAllowed(chartId, activeTrace) ? getGroupableColumns(tbl_id) : [];
                                    if (groupableCols.length === 0) return null;
                                    return (
                                        <ListBoxInputField fieldKey={groupByFieldKey(activeTrace)}
                                                           label='Group by:'
                                                           tooltip='Group scatter trace by enum-like column'
                                                           initialState={{value: getGroupByColumn(chartId) || ''}}
                                                           options={[
                                                               {label: 'None', value: ''},
                                                               ...groupableCols.map(({name, label}) => ({label: label || name, value: name}))
                                                           ]}
                                                           {...withDefaults(props)}/>);
                            }, [chartId, tbl_id, activeTrace, data?.length, ...deps]),
        Xerrors: useCallback((props) => (<Error_X {...{chartId, groupKey, activeTrace, tbl_id, ...withDefaults(props)}}/>), deps),
        Yerrors: useCallback((props) => (<Error_Y {...{chartId, groupKey, activeTrace, tbl_id, ...withDefaults(props)}}/>), deps),
        Xmin: useCallback((props) => (<ColumnOrExpression fldPath = {`_tables.fireflyData.${activeTrace}.xMin`}
                                         initValue={getFieldVal(groupKey, `_tables.fireflyData.${activeTrace}.xMin`) ?? get(mappings, `fireflyData.${activeTrace}.xMin`, '')}
                                         label = 'Spectral axis lower limit column:'
                                         name = 'Lower Limit'
                                         nullAllowed = {true}
                                         colValStats={colValStats}
                                         tbl_id={tbl_id}
                                         groupKey = {groupKey} {...withDefaults(props)}/>), deps),
        Xmax: useCallback((props) => (<ColumnOrExpression fldPath = {`_tables.fireflyData.${activeTrace}.xMax`}
                                         initValue={getFieldVal(groupKey, `_tables.fireflyData.${activeTrace}.xMax`) ?? get(mappings, `fireflyData.${activeTrace}.xMax`, '')}
                                         label = 'Spectral axis upper limit column:'
                                         name = 'Upper Limit'
                                         nullAllowed={true}
                                         colValStats={colValStats}
                                         tbl_id={tbl_id}
                                         groupKey = {groupKey} {...withDefaults(props)}/>), deps),
        Ymin: useCallback((props) => (<ColumnOrExpression fldPath = {`_tables.fireflyData.${activeTrace}.yMin`}
                                         initValue={getFieldVal(groupKey, `_tables.fireflyData.${activeTrace}.yMin`) ??  get(mappings, `fireflyData.${activeTrace}.yMin`, '')}
                                         label = {'\u21A5:'}
                                         name = 'Lower Limit'
                                         nullAllowed = {true}
                                         colValStats={colValStats}
                                         tbl_id={tbl_id}
                                         groupKey = {groupKey} {...withDefaults(props)}/>), deps),
        Ymax: useCallback((props) => (<ColumnOrExpression fldPath = {`_tables.fireflyData.${activeTrace}.yMax`}
                                         initValue={getFieldVal(groupKey, `_tables.fireflyData.${activeTrace}.yMax`) ?? get(mappings, `fireflyData.${activeTrace}.yMax`, '')}
                                         label = {'\u21A7:'}
                                         name = 'Upper Limit'
                                         nullAllowed={true}
                                         colValStats={colValStats}
                                         tbl_id={tbl_id}
                                         groupKey = {groupKey} {...withDefaults(props)}/>), deps),
        ColorMap: useCallback((props) => (<ColumnOrExpression fldPath = {`_tables.data.${activeTrace}.marker.color`}
                                         initValue={get(mappings, 'marker.color', '')}
                                         label = 'Color Map:'
                                         name = 'Color Map'
                                         key='colorMap'
                                         colValStats={colValStats}
                                         tbl_id={tbl_id}
                                         nullAllowed={true}
                                         groupKey = {groupKey} {...withDefaults(props)}/>), deps),
        ColorSize: useCallback((props) => (<ColumnOrExpression fldPath = {`_tables.data.${activeTrace}.marker.size`}
                                         initValue={get(mappings, 'marker.size', '')}
                                         label = 'Size Map:'
                                         name = 'Size Map'
                                         key='sizeMap'
                                         nullAllowed={true}
                                         tooltip='marker size. Please use expression to convert column value to valid pixels'
                                         colValStats={colValStats}
                                         tbl_id={tbl_id}
                                         groupKey = {groupKey} {...withDefaults(props)}/>), deps),
        ColorScale: useCallback((props) => ( <ListBoxInputField fieldKey={`data.${activeTrace}.marker.colorscale`}
                                         label='Color Scale:'
                                         initialState= {{value: strOrNull(get(data, `${activeTrace}.marker.colorscale`))}}
                                         tooltip='Select colorscale for color map'
                                         options={PlotlyCS.map((e)=>({value:e}))}
                                         nullAllowed={true}
                                         {...withDefaults(props)}/>), deps),
        /* temporarily disabled:
        UseSpectrum: useCallback((props) => <CheckboxGroupInputField fieldKey={`fireflyData.${activeTrace}.useSpectrum`}
                                         initialState={{value: getFieldVal(groupKey, `fireflyData.${activeTrace}.useSpectrum`) ?? fireflyData?.[activeTrace]?.useSpectrum}}
                                         options={[{label: 'Use spectrum preset', value: 'true'}]} {...withDefaults(props)}/>, deps),
        */
        UseSpectrum: useCallback(() => null, deps),
    };
};

function groupByFieldKey(activeTrace) {
    return `fireflyData.${activeTrace}.groupBy.column`;
}
