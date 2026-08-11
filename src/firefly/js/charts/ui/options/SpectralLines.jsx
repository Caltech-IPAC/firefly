import React, {useEffect} from 'react';
import {isEqual} from 'lodash';
import {Button, Stack} from '@mui/joy';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {useFieldValueOnly, useStoreConnector} from 'firefly/ui/SimpleComponent';
import {getChartData, dispatchChartUpdate, CHART_UPDATE} from '../../ChartsCntlr.js';
import {isSpectrum} from '../../ChartUtil.js';
import {isKnownRefPos} from 'firefly/voAnalyzer/SpectrumDM';
import {canUnitConv, convertUnitValue} from '../../dataTypes/SpectrumUnitConversion.js';
import {makeTblRequest} from 'firefly/tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableUiUpdate, dispatchTableAddLocal, TABLE_SELECT, TABLE_LOADED} from 'firefly/tables/TablesCntlr';
import {onTableLoaded, getTblById, getSelectedDataSync, getTblRowAsObj, splitVals, monitorChanges, watchTableChanges} from 'firefly/tables/TableUtil';
import {SelectInfo} from 'firefly/tables/SelectInfo';
import {TablePanel} from 'firefly/tables/ui/TablePanel';
import {FieldGroup} from 'firefly/ui/FieldGroup';

// recommended-lines source lists, each independently checkable; listId is sent to the server's
// "spectralLines" search processor to pick which resource file it serves. Adding a list later is
// just appending an entry here (plus its listId -> resource mapping server-side).
const RECOMMENDED_LINE_LISTS = [
    {listId: 'luisa', listLabel: 'Luisa'},
    {listId: 'jwst', listLabel: 'JWST'},
];
const recLinesTblId = (listId) => `rec-${listId}`;

// the merged, client-side table that's actually displayed/plotted from - single source of truth
const LINES_TBL_ID = 'spectral-lines';
const LINES_TBL_UI_ID = `${LINES_TBL_ID}-ui`;
const WAVELENGTH_COL = 'wavelength';
const LABEL_COL = 'label';
const DESCRIPTION_COL = 'description';
const GROUP_COL = 'group';
const WAVELENGTH_COL_UNIT = 'um'; // unit of WAVELENGTH_COL's values; TODO: source it from table metadata if present
const LINES_TBL_COLUMNS = [
    {name: WAVELENGTH_COL, units: WAVELENGTH_COL_UNIT, type: 'double'},
    {name: LABEL_COL, type: 'char'},
    {name: DESCRIPTION_COL, type: 'char'},
    {name: GROUP_COL, type: 'char'},
];

const SPECTRAL_LINE_COLOR = 'gray';
const SPECTRAL_LINE_FONT_FAMILY = "'SF Mono', ui-monospace, monospace";
export const SPECTRAL_LINES_GROUP = 'lines';

const SPECTRAL_LINES_FG_KEY = 'spectralLinesPanel';

// Field keys and option values ---
const SOURCE_OPTIONS_KEY = 'spectralLines.sourceOptions'; // comma-separated checked values, e.g. CheckboxGroupInputField's value
const SOURCE_UPLOAD = 'upload';

/**
 * Builds Plotly vertical-line shapes for the currently selected (checked) rows of a given spectral lines table.
 * @param {string} xUnit - unit of the chart's x-axis; wavelengths from the spectral lines table are converted to this
 * @param {string} linesTblId - tbl_id of the spectral lines table to read selected rows from; pass LINES_TBL_ID
 * for the UI-managed merged table, or any other tbl_id (e.g. from a JS API caller supplying their own table).
 * @param {number} [redshift] - redshift of the spectrum in observed frame; 0 (default) for a
 * spectrum already shown in rest frame.
 * @returns {Array<object>} Plotly shape objects, one per selected row with a valid wavelength
 */
export function makeSpectralLineShapes(xUnit, linesTblId, redshift=0) {
    if (!canUnitConv({from: WAVELENGTH_COL_UNIT, to: xUnit})) return [];

    const selectedLines = getSelectedDataSync(linesTblId);
    const linesToPlot = [];
    for (let rowIdx = 0; rowIdx < selectedLines.totalRows; rowIdx++) {
        const row = getTblRowAsObj(selectedLines, rowIdx);
        const lineWvl = row[WAVELENGTH_COL] * (1 + redshift); // redshift the rest-frame wavelength of a spectral line
        const x = convertUnitValue(lineWvl, WAVELENGTH_COL_UNIT, xUnit);
        if (!Number.isFinite(x)) continue; // skip rows with a missing/unparsable wavelength
        linesToPlot.push({x, label: row[LABEL_COL], description: row[DESCRIPTION_COL]});
    }

    return linesToPlot.map(({x, label, description}, i) => ({
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
        hovertext: `<b>${label}</b>  λ ${x} ${xUnit}` + (description ? `<br>${description}` : ''),
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
 * Rebuilds a chart's spectral-line shapes from the merged lines table's current row selection, and dispatches
 * only if the result actually differs from what's already on the chart. No group selected -> table has no
 * selected rows -> makeSpectralLineShapes naturally returns no shapes -> achieves spectral lines disabled behavior.
 * @param {string} chartId
 */
function resyncChartLines(chartId) {
    const {activeTrace=0, fireflyData=[], data=[], layout} = getChartData(chartId);
    const xUnit = fireflyData[activeTrace]?.xUnit;
    const redshift = resolveSpectralLinesRedshift(fireflyData, activeTrace);

    const otherShapes = (layout?.shapes ?? []).filter((s) => s.legendgroup !== SPECTRAL_LINES_GROUP);
    const spectralLineShapes = makeSpectralLineShapes(xUnit, LINES_TBL_ID, redshift);
    const changes = {
        'layout.shapes': [...otherShapes, ...spectralLineShapes],
        'layout.showlegend': data.length > 1 || spectralLineShapes.length > 0,
    };

    // don't update chart unless the changes are really new
    if (isEqual(changes['layout.shapes'], layout?.shapes) && changes['layout.showlegend'] === layout?.showlegend) return;
    dispatchChartUpdate({chartId, changes});
}

/**
 * Keeps one chart's plotted spectral-line shapes in sync with the merged lines table's row-selection - no chart
 * ever stores spectral-lines settings itself, this just consults makeSpectralLineShapes every time something
 * relevant changes. Uses explicit action watchers (like ChartUtil.js's setupTableWatcher) rather than a generic
 * store subscription, so each relevant change is reacted to individually and repeatedly, not just the first.
 * No JSX output - call directly from a component body (e.g. ChartPanel.jsx), not rendered as an element.
 * @param {string} chartId
 */
export function useSpectralLinesSync(chartId) {
    useEffect(() => {
        if (!isSpectrum(chartId)) return;

        resyncChartLines(chartId); // reflect current state immediately

        const resync = () => resyncChartLines(chartId);
        const cancels = [
            // row (de)selection / (re)build of the merged lines table itself
            watchTableChanges(LINES_TBL_ID, [TABLE_SELECT, TABLE_LOADED], resync, `sl-tbl-${chartId}`),
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

/* fetches a single recommended-lines source table (by its listId) if not already loaded */
async function ensureRecommendedList(listId) {
    const tbl_id = recLinesTblId(listId);
    if (getTblById(tbl_id)) return;
    const request = makeTblRequest('spectralLines', 'Spectral Lines', {listId}, {tbl_id});
    dispatchTableFetch(request); // headless: doesn't render in results UI
    await onTableLoaded(tbl_id);
}

/**
 * Rebuilds the merged, client-side lines table (LINES_TBL_ID) from the checked source options: one row per
 * line, tagged with its source under GROUP_COL, with every row selected by default. This is the only place
 * LINES_TBL_ID's content changes - called once on panel mount and again on the "Update Lines" button click,
 * never automatically on checkbox change, so checking a box doesn't plot anything until applied.
 * @param {string} sourceOptions - comma-separated checked values from SOURCE_OPTIONS_KEY
 */
async function buildMergedLinesTable(sourceOptions) {
    const checked = splitVals(sourceOptions);
    const checkedLists = RECOMMENDED_LINE_LISTS.filter(({listId}) => checked.includes(listId));
    await Promise.all(checkedLists.map(({listId}) => ensureRecommendedList(listId)));
    // TODO: once upload + column mapping is implemented, include checked upload groups here too

    const data = checkedLists.flatMap(({listId, listLabel}) => {
        const src = getTblById(recLinesTblId(listId));
        return Array.from({length: src?.totalRows ?? 0}, (_, rowIdx) => {
            const row = getTblRowAsObj(src, rowIdx);
            return [row[WAVELENGTH_COL], row[LABEL_COL], row[DESCRIPTION_COL], listLabel];
        });
    });

    const table = {tbl_id: LINES_TBL_ID, title: 'Spectral Lines', tableData: {columns: LINES_TBL_COLUMNS, data}};
    table.selectInfo = SelectInfo.newInstance({selectAll: true, rowCount: data.length}).data;
    dispatchTableAddLocal(table, undefined, false);
}

/**
 * Standalone Spectral Lines dialog content. keepState=true on the FieldGroup so selections survive the
 * dialog being closed/reopened (e.g. from a different chart) - every use input is live (see useSpectralLinesSync)
 * @param {object} props
 * @param {string} props.chartId
 * @param {number} props.activeTrace
 */
export function SpectralLinesPanel({activeTrace, chartId}) {
    // FieldGroup has keepState=true, so this fixed default only matters the very first time this
    // session the dialog is opened - after that, the group's own last-seen value takes over
    const initialSourceOptions = ''; // nothing checked by default - lines table starts empty until applied
    const sourceOptions = useFieldValueOnly(SOURCE_OPTIONS_KEY, initialSourceOptions, SPECTRAL_LINES_FG_KEY);

    useEffect(() => {
        // pre-register tbl_ui_id so columns/columnWidths get populated once loaded (TablePanel mounts later, too late)
        dispatchTableUiUpdate({tbl_ui_id: LINES_TBL_UI_ID, tbl_id: LINES_TBL_ID});
        // build only if it doesn't exist yet - once built, row selection is user-owned and must survive
        // the dialog being closed/reopened; only the "Update Lines" button rebuilds after this point
        if (!getTblById(LINES_TBL_ID)) void buildMergedLinesTable(sourceOptions);
        // eslint-disable-next-line react-hooks/exhaustive-deps -- intentionally mount-only; button click handles later rebuilds
    }, []);

    // spectral lines need a redshift to correct against, which only exists when Spectral Frame options are shown (as opposed to read-only value)
    const hasSpectralFrame = useStoreConnector(() =>
        isKnownRefPos(getChartData(chartId)?.fireflyData?.[activeTrace]?.spectralFrame?.refPos),
        [chartId, activeTrace]);

    return (
        <FieldGroup groupKey={SPECTRAL_LINES_FG_KEY} keepState={true}>
            {hasSpectralFrame && (
                <Stack spacing={1} sx={{pr: 1, minWidth: '35rem'}}>
                    <CheckboxGroupInputField fieldKey={SOURCE_OPTIONS_KEY}
                                             label='Lines list:'
                                             initialState={{value: initialSourceOptions}}
                                             options={[
                                                 ...RECOMMENDED_LINE_LISTS.map(({listId, listLabel}) =>
                                                     ({label: `${listLabel} (recommended)`, value: listId})),
                                                 {label: 'Upload mine', value: SOURCE_UPLOAD, disabled: true}
                                             ]}/>
                    {/* TODO: "Upload mine" — file upload + column mapper */}
                    <Stack direction='row'>
                        <Button size='sm' onClick={() => buildMergedLinesTable(sourceOptions)}>Update Lines</Button>
                    </Stack>
                    <Stack sx={{height: 240}}>
                        <TablePanel
                            tbl_id={LINES_TBL_ID}
                            tbl_ui_id={LINES_TBL_UI_ID}
                            border={false}
                            showToolbar={false}
                            showOptionButton={false}
                            showTypes={false}
                            showUnits={true}
                            selectable={true}
                            showSelectRowFilter={false}
                            showFilters={true}
                        />
                    </Stack>
                </Stack>
            )}
        </FieldGroup>
    );
}
