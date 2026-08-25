import React, {useEffect} from 'react';
import {get} from 'lodash';
import {Stack} from '@mui/joy';
import {SwitchInputField} from 'firefly/ui/SwitchInputField';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {CollapsibleGroup, CollapsibleItem} from 'firefly/ui/panel/CollapsiblePanel';
import {useFieldValueOnly, useStoreConnector} from 'firefly/ui/SimpleComponent';
import {getChartData} from '../../ChartsCntlr.js';
import {isKnownRefPos} from 'firefly/voAnalyzer/SpectrumDM';
import {canUnitConv, convertUnitValue} from '../../dataTypes/SpectrumUnitConversion.js';
import {makeTblRequest} from 'firefly/tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableUiUpdate, dispatchTableSelect} from 'firefly/tables/TablesCntlr';
import {onTableLoaded, getTblById, getSelectedDataSync, getTblRowAsObj, splitVals} from 'firefly/tables/TableUtil';
import {TablePanel} from 'firefly/tables/ui/TablePanel';

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

export function SpectralLinesOptions({activeTrace, chartId}) {
    useEffect(() => {
        // pre-register tbl_ui_id so columns/columnWidths get populated once loaded (TablePanel mounts later, too late)
        dispatchTableUiUpdate({tbl_ui_id: RECOMMENDED_LINES_TBL_UI_ID, tbl_id: RECOMMENDED_LINES_TBL_ID});
        void ensureRecommendedLines();
    }, []);

    const {enabled: initialEnabled = false, source: initialTblId = RECOMMENDED_LINES_TBL_ID} =
        useStoreConnector(() => get(getChartData(chartId), 'fireflyLayout.spectralLines')) ?? {};
    const initialSourceOptions = tblIdToSourceOption(initialTblId);

    // spectral lines need a redshift to correct against, which only exists when Spectral Frame options are shown (as opposed to read-only value)
    const hasSpectralFrame = useStoreConnector(() =>
        isKnownRefPos(getChartData(chartId)?.fireflyData?.[activeTrace]?.spectralFrame?.refPos),
        [chartId, activeTrace]);

    // The field can be undefined for one render while the dialog mounts
    // Use live form state when present, otherwise fall back to chart state
    const isEnabledField = useFieldValueOnly(ENABLED_KEY);
    const isEnabled = hasSpectralFrame && (isEnabledField ?? initialEnabled);

    // TODO: combine different source tables to a client-side table
    const sourceOptions = useFieldValueOnly(SOURCE_OPTIONS_KEY, initialSourceOptions);
    const activeTblId = sourceOptionToTblId(sourceOptions);
    const activeTblUiId = activeTblId && `${activeTblId}-ui`;

    if (!hasSpectralFrame) return false;

    return (
        <Stack spacing={1} sx={{pr: 1}}>
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
    );
}
