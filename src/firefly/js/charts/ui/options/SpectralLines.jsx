import React, {useEffect} from 'react';
import {isEqual} from 'lodash';
import {Stack} from '@mui/joy';
import {SwitchInputField} from 'firefly/ui/SwitchInputField';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {CollapsibleGroup, CollapsibleItem} from 'firefly/ui/panel/CollapsiblePanel';
import {useFieldValueOnly, useStoreConnector} from 'firefly/ui/SimpleComponent';
import {getChartData, dispatchChartUpdate, CHART_UPDATE} from '../../ChartsCntlr.js';
import {isSpectrum} from '../../ChartUtil.js';
import {isKnownRefPos} from 'firefly/voAnalyzer/SpectrumDM';
import {canUnitConv, convertUnitValue} from '../../dataTypes/SpectrumUnitConversion.js';
import {makeTblRequest} from 'firefly/tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableUiUpdate, dispatchTableSelect, TABLE_SELECT, TABLE_LOADED} from 'firefly/tables/TablesCntlr';
import {onTableLoaded, getTblById, getSelectedDataSync, getTblRowAsObj, splitVals, monitorChanges, watchTableChanges} from 'firefly/tables/TableUtil';
import {TablePanel} from 'firefly/tables/ui/TablePanel';
import {toBoolean} from 'firefly/util/WebUtil';
import {FieldGroup} from 'firefly/ui/FieldGroup';
import {getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils';
import {VALUE_CHANGE, MULTI_VALUE_CHANGE} from 'firefly/fieldGroup/FieldGroupCntlr';

const RECOMMENDED_LINES_TBL_ID = 'recommended-spectral-lines';
const RECOMMENDED_LINES_TBL_UI_ID = `${RECOMMENDED_LINES_TBL_ID}-ui`;
const WAVELENGTH_COL = 'wavelength_um';
const LABEL_COL = 'species';
const TRANSITION_COL = 'transition';
const PHASE_COL = 'phase';
const WAVELENGTH_COL_UNIT = 'um'; // unit of WAVELENGTH_COL's values; TODO: source it from table metadata if present

const SPECTRAL_LINE_COLOR = 'gray';
const SPECTRAL_LINE_FONT_FAMILY = "'SF Mono', ui-monospace, monospace";
export const SPECTRAL_LINES_GROUP = 'lines';

const SPECTRAL_LINES_FG_KEY = 'spectralLinesPanel';

// Field keys and option values ---
const ENABLED_KEY = 'spectralLines.enabled';
const SOURCE_OPTIONS_KEY = 'spectralLines.sourceOptions'; // comma-separated checked values, e.g. CheckboxGroupInputField's value
const SOURCE_RECOMMENDED = 'recommended';
const SOURCE_UPLOAD = 'upload';

/**
 * Resolves the checked source options to the concrete tbl_id to read lines from.
 * Note: only one table can be active for now — combining multiple checked sources (e.g. recommended + uploaded)
 * into a single client-side tableModel is deferred to a later pass, once upload is implemented.
 * @param {string} sourceOptions - comma-separated checked values from SOURCE_OPTIONS_KEY
 * @returns {string|undefined} tbl_id
 */
export function sourceOptionToTblId(sourceOptions) {
    return splitVals(sourceOptions).includes(SOURCE_RECOMMENDED) ? RECOMMENDED_LINES_TBL_ID : undefined;
}

/**
 * Resolves a concrete lines-table tbl_id back to the checked source options it corresponds to.
 * @param {string} linesTblId
 * @returns {string} SOURCE_RECOMMENDED, or '' if none
 */
function tblIdToSourceOption(linesTblId) {
    return linesTblId === RECOMMENDED_LINES_TBL_ID ? SOURCE_RECOMMENDED : '';
}

/**
 * Builds Plotly vertical-line shapes for the currently selected (checked) rows of a given spectral lines table.
 * @param {string} xUnit - unit of the chart's x-axis; wavelengths from the spectral lines table are converted to this
 * @param {string} linesTblId - tbl_id of the spectral lines table; only selected rows are used
 * @param {number} [redshift] - redshift of the spectrum in observed frame; 0 (default) for a
 * spectrum already shown in rest frame.
 * @returns {Array<object>} Plotly shape objects, one per selected row with a valid wavelength
 */
export function makeSpectralLineShapes(xUnit, linesTblId, redshift=0) {
    // TODO: build shapes from an uploaded lines table once file upload + column mapping is implemented
    if (linesTblId !== RECOMMENDED_LINES_TBL_ID) return [];

    if (!canUnitConv({from: WAVELENGTH_COL_UNIT, to: xUnit})) return [];

    const selectedLines = getSelectedDataSync(RECOMMENDED_LINES_TBL_ID);
    const linesToPlot = [];
    for (let rowIdx = 0; rowIdx < selectedLines.totalRows; rowIdx++) {
        const row = getTblRowAsObj(selectedLines, rowIdx);
        const lineWvl = row[WAVELENGTH_COL] * (1 + redshift); // redshift the rest-frame wavelength of a spectral line
        const x = convertUnitValue(lineWvl, WAVELENGTH_COL_UNIT, xUnit);
        if (!Number.isFinite(x)) continue; // skip rows with a missing/unparsable wavelength
        linesToPlot.push({x, label: row[LABEL_COL], transition: row[TRANSITION_COL], phase: row[PHASE_COL]});
    }

    return linesToPlot.map(({x, label, transition, phase}, i) => ({
        type: 'line',
        x0: x, x1: x,
        y0: 0, y1: 1,
        xref: 'x', yref: 'paper',
        line: {color: SPECTRAL_LINE_COLOR, width: 1, dash: 'dot'},
        label: {
            text: label,
            textposition: 'end',
            yanchor: 'bottom',
            font: {size: 9.5, color: SPECTRAL_LINE_COLOR, family: SPECTRAL_LINE_FONT_FAMILY},
            padding: 2,
        },
        hovertext: `<b>${label}</b>  λ ${x} ${xUnit}` +
            (transition ? `<br>${transition}` : '') + (phase ? `<br>${phase}` : ''),
        legendgroup: SPECTRAL_LINES_GROUP,
        showlegend: i === 0, // legend entry goes only on the first shape item
        name: 'Lines',
    }));
}

/**
 * Resolves the redshift to correct spectral lines against, from a chart's already-committed spectral frame state.
 * Lines are rest-frame (lab) wavelengths; when the spectrum itself is shown in observed frame (i.e. not already
 * rest-frame corrected), lines must be shifted by the same redshift to match - no shift needed in rest frame.
 * @param {Array<object>} fireflyData
 * @param {number} activeTrace
 * @returns {number}
 */
function resolveSpectralLinesRedshift(fireflyData, activeTrace) {
    // TODO: spectralFrameOption is undefined until Modify Trace is applied at least once, so this
    // assumes rest-frame (0) until then even if the real default would be observed with a redshift
    const {value: sfOption, redshift: redshiftOption, userSpecified} = fireflyData?.[activeTrace]?.spectralFrameOption ?? {};
    if (sfOption !== 'observed') return 0;
    const redshift = redshiftOption === 'userSpecified' ? userSpecified : redshiftOption;
    return Number(redshift) || 0;
}

/**
 * Rebuilds a chart's spectral-line shapes from the current Spectral Lines FieldGroup + lines table
 * selection, and dispatches only if the result actually differs from what's already on the chart.
 * @param {string} chartId
 */
function resyncChartLines(chartId) {
    const {activeTrace=0, fireflyData=[], data=[], layout} = getChartData(chartId);
    const enabled = toBoolean(getFieldVal(SPECTRAL_LINES_FG_KEY, ENABLED_KEY, false));
    const tblId = sourceOptionToTblId(getFieldVal(SPECTRAL_LINES_FG_KEY, SOURCE_OPTIONS_KEY));
    const xUnit = fireflyData[activeTrace]?.xUnit;
    const redshift = resolveSpectralLinesRedshift(fireflyData, activeTrace);

    const otherShapes = (layout?.shapes ?? []).filter((s) => s.legendgroup !== SPECTRAL_LINES_GROUP);
    const spectralLineShapes = enabled ? makeSpectralLineShapes(xUnit, tblId, redshift) : [];
    const changes = {
        'layout.shapes': [...otherShapes, ...spectralLineShapes],
        'layout.showlegend': data.length > 1 || enabled,
    };

    // don't update chart unless the changes are really new
    if (isEqual(changes['layout.shapes'], layout?.shapes) && changes['layout.showlegend'] === layout?.showlegend) return;
    dispatchChartUpdate({chartId, changes});
}

/**
 * Keeps one chart's plotted spectral-line shapes in sync with the (app-wide) Spectral Lines FieldGroup
 * and the active lines table's row-selection - no chart ever stores spectral-lines settings itself, this
 * just consults the current fields + makeSpectralLineShapes every time something relevant changes.
 * Uses explicit action watchers (like ChartUtil.js's setupTableWatcher) rather than a generic store
 * subscription, so each relevant change is reacted to individually and repeatedly, not just the first.
 * No JSX output - call directly from a component body (e.g. ChartPanel.jsx), not rendered as an element.
 * @param {string} chartId
 */
export function useSpectralLinesSync(chartId) {
    useEffect(() => {
        if (!isSpectrum(chartId)) return;

        resyncChartLines(chartId); // reflect current state immediately

        const resync = () => resyncChartLines(chartId);
        const cancels = [
            // the enable switch and source checkboxes
            monitorChanges([VALUE_CHANGE, MULTI_VALUE_CHANGE],
                (a) => a.payload.groupKey === SPECTRAL_LINES_FG_KEY,
                resync, `sl-fg-${chartId}`),
            // row (de)selection / (re)load of the lines table itself
            watchTableChanges(RECOMMENDED_LINES_TBL_ID, [TABLE_SELECT, TABLE_LOADED], resync, `sl-tbl-${chartId}`),
            // this chart's own xUnit/spectral-frame change (Modify Trace) under the 'fireflyData.' path
            // note: resyncChartLines's own writes only ever touch 'layout.shapes|showlegend' in chart update so it
            // avoids a self-triggering feedback loop
            monitorChanges([CHART_UPDATE],
                (a) => a.payload.chartId === chartId && Object.keys(a.payload.changes ?? {}).some((k) => k.startsWith('fireflyData')),
                resync, `sl-chart-${chartId}`),
        ];
        return () => cancels.forEach((cancel) => cancel?.());
    }, [chartId]);
}

/* fetches the recommended spectral lines table if not already loaded, then selects all its rows by default */
async function ensureRecommendedLines() {
    if (getTblById(RECOMMENDED_LINES_TBL_ID)) return;

    const request = makeTblRequest(
        'spectralLines', 'Spectral Lines', {},
        {tbl_id: RECOMMENDED_LINES_TBL_ID}
    );
    dispatchTableFetch(request); // headless: doesn't render in results UI

    // all lines are checked by default; set post-load since request.META_INFO.selectInfo can silently get lost in transit
    const tableModel = await onTableLoaded(RECOMMENDED_LINES_TBL_ID);
    dispatchTableSelect(RECOMMENDED_LINES_TBL_ID, {selectAll: true, exceptions: new Set(), rowCount: tableModel.totalRows});
}

/**
 * Standalone Spectral Lines dialog content. keepState=true on the FieldGroup so selections survive the
 * dialog being closed/reopened (e.g. from a different chart) - every use input is live (see useSpectralLinesSync)
 * @param {object} props
 * @param {string} props.chartId
 * @param {number} props.activeTrace
 */
export function SpectralLinesPanel({activeTrace, chartId}) {
    useEffect(() => {
        // pre-register tbl_ui_id so columns/columnWidths get populated once loaded (TablePanel mounts later, too late)
        dispatchTableUiUpdate({tbl_ui_id: RECOMMENDED_LINES_TBL_UI_ID, tbl_id: RECOMMENDED_LINES_TBL_ID});
        void ensureRecommendedLines();
    }, []);

    // FieldGroup has keepState=true, so these fixed defaults only matter the very first time this
    // session the dialog is opened - after that, the group's own last-seen values take over
    const initialEnabled = false;
    const initialSourceOptions = tblIdToSourceOption(RECOMMENDED_LINES_TBL_ID);

    // spectral lines need a redshift to correct against, which only exists when Spectral Frame options are shown (as opposed to read-only value)
    const hasSpectralFrame = useStoreConnector(() =>
        isKnownRefPos(getChartData(chartId)?.fireflyData?.[activeTrace]?.spectralFrame?.refPos),
        [chartId, activeTrace]);
    const isEnabledField = useFieldValueOnly(ENABLED_KEY, false, SPECTRAL_LINES_FG_KEY);
    const isEnabled = hasSpectralFrame && isEnabledField;

    // TODO: combine different source tables to a client-side table
    const sourceOptions = useFieldValueOnly(SOURCE_OPTIONS_KEY, initialSourceOptions, SPECTRAL_LINES_FG_KEY);
    const activeTblId = sourceOptionToTblId(sourceOptions);
    const activeTblUiId = activeTblId && `${activeTblId}-ui`;

    return (
        <FieldGroup groupKey={SPECTRAL_LINES_FG_KEY} keepState={true}>
            {hasSpectralFrame && (
                <Stack spacing={1} sx={{pr: 1, minWidth: '35rem'}}>
                    <Stack direction='row' spacing={1.5} alignItems='baseline'>
                        <SwitchInputField fieldKey={ENABLED_KEY}
                                          label='Spectral lines:'
                                          initialState={{value: initialEnabled}}/>
                    </Stack>
                    {isEnabled && (
                        <Stack spacing={1}>
                            <CheckboxGroupInputField fieldKey={SOURCE_OPTIONS_KEY}
                                                     label='Lines list:'
                                                     initialState={{value: initialSourceOptions}}
                                                     options={[
                                                         {label: 'Recommended', value: SOURCE_RECOMMENDED},
                                                         {label: 'Upload mine', value: SOURCE_UPLOAD, disabled: true}
                                                     ]}/>
                            {/* TODO: "Upload mine" — file upload + column mapper */}
                            {activeTblId && (
                                <CollapsibleGroup>
                                    <CollapsibleItem componentKey={`${chartId}-spectralLines`} header='View/Edit Lines' isOpen={true}
                                                     slotProps={{header: {sx: {fontSize: 'sm'}}}}>
                                        <Stack sx={{height: 240}}>
                                            <TablePanel
                                                tbl_id={activeTblId}
                                                tbl_ui_id={activeTblUiId}
                                                border={false}
                                                showToolbar={false}
                                                showOptionButton={false}
                                                showTypes={false}
                                                showUnits={true}
                                                selectable={true}
                                                showSelectRowFilter={false}
                                            />
                                        </Stack>
                                        {/* TODO: add "Add row" button with comma-delimited input to append custom lines */}
                                    </CollapsibleItem>
                                </CollapsibleGroup>
                            )}
                        </Stack>
                    )}
                </Stack>
            )}
        </FieldGroup>
    );
}
