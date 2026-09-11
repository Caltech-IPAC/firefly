import React, {useEffect, useState} from 'react';
import {cloneDeep, isEqual} from 'lodash';
import {Button, Divider, Stack, Typography} from '@mui/joy';
import {Insights} from '@mui/icons-material';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {CollapsibleGroup, CollapsibleItem} from 'firefly/ui/panel/CollapsiblePanel';
import {useFieldGroupValue, useStoreConnector} from 'firefly/ui/SimpleComponent';
import {getChartData, dispatchChartUpdate, CHART_UPDATE} from '../../ChartsCntlr.js';
import {isSpectrum} from '../../ChartUtil.js';
import {isKnownRefPos} from 'firefly/voAnalyzer/SpectrumDM';
import {canUnitConv, convertUnitValue} from '../../dataTypes/SpectrumUnitConversion.js';
import {makeTblRequest} from 'firefly/tables/TableRequestUtil';
import {dispatchTableFetch, dispatchTableUiUpdate, dispatchTableAddLocal, TABLE_SELECT, TABLE_LOADED} from 'firefly/tables/TablesCntlr';
import {onTableLoaded, doFetchTable, getColumn, isColumnType, COL_TYPE, getTblById, getSelectedDataSync,
    getTblRowAsObj, getColumnValues, splitVals, monitorChanges, watchTableChanges} from 'firefly/tables/TableUtil';
import {SelectInfo} from 'firefly/tables/SelectInfo';
import {TablePanel} from 'firefly/tables/ui/TablePanel';
import {FieldGroup} from 'firefly/ui/FieldGroup';
import {dispatchComponentStateChange} from 'firefly/core/ComponentCntlr';
import {MISSING_COLS_HEADER_MSG, UploadTableSelector} from 'firefly/ui/UploadTableSelector';
import {getEffectiveSpectralFrameOption, SF_OPTION, USER_SPECIFIED_REDSHIFT} from './SpectrumOptions.jsx';

const recLinesTblId = (listId) => `rec-${listId}`;

// order-insensitive: checking source options checkboxes in a different order shouldn't count as a real difference
const sameSourceOptions = (a, b) => isEqual(splitVals(a).filter(Boolean).sort(), splitVals(b).filter(Boolean).sort());

// the merged, client-side table that's actually displayed/plotted from - single source of truth
const LINES_TBL_ID = 'spectral-lines';
const LINES_TBL_UI_ID = `${LINES_TBL_ID}-ui`;
const WAVELENGTH_COL = 'wavelength';
const LABEL_COL = 'label';
const DESCRIPTION_COL = 'description';
const GROUP_COL = 'list';
const WAVELENGTH_COL_UNIT = 'um'; // canonical unit of WAVELENGTH_COL's values
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
const SOURCES_COLLAPSIBLE_KEY = 'spectralLinesSources'; // panel is shared app-wide, not chart-specific - one fixed key
const SOURCE_OPTIONS_KEY = 'spectralLines.sourceOptions'; // comma-separated checked values, e.g. CheckboxGroupInputField's value
const UPLOAD_INFO_KEY = 'spectralLines.upload.info';
const UPLOAD_WAVELENGTH_COL_KEY = 'spectralLines.upload.wavelengthCol';
const UPLOAD_LABEL_COL_KEY = 'spectralLines.upload.labelCol';
const UPLOAD_DESCRIPTION_COL_KEY = 'spectralLines.upload.descriptionCol';
const UPLOAD_MAPPING_PANEL_KEY = 'spectralLinesUploadMapping';
const UPLOAD_TBL_OPTIONS = {
    // keeps the uploaded line list table from appearing in the Results view (as tbl_group defaults to 'main')
    tbl_group: 'spectralLinesUpload'
};


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
 * @returns {number|undefined} undefined if this trace has no known spectral frame at all (lines aren't
 * applicable to it), as opposed to a known rest frame (0) or a known observed-frame redshift.
 */
function resolveSpectralLinesRedshift(fireflyData, activeTrace) {
    // a redshift is only resolvable when Spectral Frame options are shown (as opposed to a read-only value)
    if (!isKnownRefPos(fireflyData?.[activeTrace]?.spectralFrame?.refPos)) return undefined;

    // falls back to the same default a fresh spectrum options panel would show, rather than assuming rest-frame
    const {value: sfOption, redshift: redshiftOption, userSpecified} = getEffectiveSpectralFrameOption(fireflyData?.[activeTrace]);
    if (sfOption !== SF_OPTION.OBSERVED) return 0;
    const redshift = redshiftOption === USER_SPECIFIED_REDSHIFT ? userSpecified : redshiftOption;
    return Number(redshift) || 0;
}

/**
 * Rebuilds a chart's spectral-line shapes from the merged lines table's current row selection, and dispatches
 * only if the result actually differs from what's already on the chart. When lines table has no
 * selected rows -> makeSpectralLineShapes naturally returns no shapes -> achieves spectral lines disabled behavior.
 * @param {string} chartId
 */
function resyncChartLines(chartId) {
    const {activeTrace=0, fireflyData=[], data=[], layout} = getChartData(chartId);
    const xUnit = fireflyData[activeTrace]?.xUnit;
    const redshift = resolveSpectralLinesRedshift(fireflyData, activeTrace);

    const otherShapes = (layout?.shapes ?? []).filter((s) => s.legendgroup !== SPECTRAL_LINES_GROUP);
    const spectralLineShapes = redshift === undefined ? [] : makeSpectralLineShapes(xUnit, LINES_TBL_ID, redshift);
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

let lineListsPromise = null; // cache populated by fetchLineLists()

/**
 * Fetches the server's info on available recommended line lists ({listId, listLabel} pairs), once per
 * session (cached in lineListsPromise) - the server is the single source of truth for which lists exist.
 * @returns {Promise<Array<{listId: string, listLabel: string}>>}
 */
function fetchLineLists() {
    if (!lineListsPromise) {
        const request = makeTblRequest('spectralLines', 'Spectral Line Lists', {metaOnly: true});
        lineListsPromise = doFetchTable(request).then((tbl) => JSON.parse(tbl.tableMeta?.lineLists ?? '[]'));
    }
    return lineListsPromise;
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
 * Builds rows for the merged-table from a source table: converts each row's wavelength to the merged table's
 * canonical unit (WAVELENGTH_COL_UNIT) and tags every row with the given list group label.
 * @param {TableModel} src
 * @param {string} wavelengthCol - name of src's wavelength column
 * @param {string} labelCol - name of src's label column
 * @param {string} descriptionCol - name of src's description column; if none, description row values are set to ''
 * @param {string} group - value for GROUP_COL, tagging where these rows came from
 * @returns {Array<Array>} [] if src's wavelength unit isn't convertible to WAVELENGTH_COL_UNIT at all; rows with
 * a missing/unparsable wavelength are skipped individually
 */
function makeLinesRows(src, wavelengthCol, labelCol, descriptionCol, group) {
    const wavelengthUnit = getColumn(src, wavelengthCol)?.units || WAVELENGTH_COL_UNIT;
    if (!canUnitConv({from: wavelengthUnit, to: WAVELENGTH_COL_UNIT})) return [];

    const rows = [];
    for (let rowIdx = 0; rowIdx < (src?.totalRows ?? 0); rowIdx++) {
        const row = getTblRowAsObj(src, rowIdx);
        const wavelength = convertUnitValue(Number(row[wavelengthCol]), wavelengthUnit, WAVELENGTH_COL_UNIT);
        if (!Number.isFinite(wavelength)) continue; // skip rows with a missing/unparsable wavelength
        rows.push([wavelength, row[labelCol], descriptionCol ? row[descriptionCol] : '', group]);
    }
    return rows;
}

/**
 * Builds the merged table's rows for the uploaded line list, per the user's column mapping. Wavelength and Label
 * are required for the upload to contribute rows at all; Description is optional.
 * @param {object} [uploadInfo] - uploadInfo from UploadTableSelector, or undefined if nothing's uploaded
 * @param {string} wavelengthCol - name of the uploaded table's column mapped to wavelength
 * @param {string} labelCol - name of the uploaded table's column mapped to label
 * @param {string} descriptionCol - name of the uploaded table's column mapped to description, if any
 * @returns {Promise<Array<Array>>}
 */
async function uploadedLinesRows(uploadInfo, wavelengthCol, labelCol, descriptionCol) {
    if (!uploadInfo?.tbl_id || !wavelengthCol || !labelCol) return [];

    await onTableLoaded(uploadInfo.tbl_id);
    return makeLinesRows(getTblById(uploadInfo.tbl_id), wavelengthCol, labelCol, descriptionCol, uploadInfo.fileName);
}

/**
 * Builds the merged table's rows for the checked recommended line lists, fetching any not already loaded.
 * @param {string} sourceOptions - comma-separated checked values from SOURCE_OPTIONS_KEY
 * @param {Array<{listId: string, listLabel: string}>} lineLists - the fetched info on available lists
 * @returns {Promise<Array<Array>>}
 */
async function recommendedLinesRows(sourceOptions, lineLists) {
    const checked = splitVals(sourceOptions);
    const checkedLists = lineLists.filter(({listId}) => checked.includes(listId));
    await Promise.all(checkedLists.map(({listId}) => ensureRecommendedList(listId)));

    return checkedLists.flatMap(({listId, listLabel}) =>
        makeLinesRows(getTblById(recLinesTblId(listId)), WAVELENGTH_COL, LABEL_COL, DESCRIPTION_COL, listLabel));
}

/**
 * Identity of "what would be merged from the upload right now", to detect when the upload/mapping has changed
 * since the merged table was last built (see hasPendingChanges in SpectralLinesPanel). '' when the upload
 * doesn't (yet) have a usable mapping, i.e. when it wouldn't contribute any rows - see uploadedLinesRows.
 * @param {object} [uploadInfo] - see uploadedLinesRows
 * @param {string} wavelengthCol - see uploadedLinesRows
 * @param {string} labelCol - see uploadedLinesRows
 * @param {string} descriptionCol - see uploadedLinesRows
 * @returns {string}
 */
const uploadSignature = (uploadInfo, wavelengthCol, labelCol, descriptionCol) =>
    (uploadInfo?.tbl_id && wavelengthCol && labelCol)
        ? [uploadInfo.tbl_id, wavelengthCol, labelCol, descriptionCol].join(';')
        : '';

/**
 * Rebuilds the merged, client-side lines table (LINES_TBL_ID) from recommendedLinesRows + uploadedLinesRows: one
 * row per line, tagged with its source under GROUP_COL, every row selected by default. This is the only place
 * LINES_TBL_ID's content changes - called once on panel mount and again on the "Update Lines" button click,
 * never automatically on checkbox/mapping change, so those don't plot anything until applied.
 * @param {string} sourceOptions - see recommendedLinesRows
 * @param {Array<{listId: string, listLabel: string}>} lineLists - see recommendedLinesRows
 * @param {object} [uploadInfo] - see uploadedLinesRows
 * @param {string} wavelengthCol - see uploadedLinesRows
 * @param {string} labelCol - see uploadedLinesRows
 * @param {string} descriptionCol - see uploadedLinesRows
 */
async function buildMergedLinesTable(sourceOptions, lineLists, uploadInfo, wavelengthCol, labelCol, descriptionCol) {
    const [recRows, uploadRows] = await Promise.all([
        recommendedLinesRows(sourceOptions, lineLists),
        uploadedLinesRows(uploadInfo, wavelengthCol, labelCol, descriptionCol),
    ]);
    const data = [...recRows, ...uploadRows];

    // store what this table was built from in meta, so the panel can tell when the checked lists/upload have since diverged
    const tableMeta = {sourceOptions, uploadSignature: uploadSignature(uploadInfo, wavelengthCol, labelCol, descriptionCol)};
    const columns = cloneDeep(LINES_TBL_COLUMNS); // fresh column objects are needed to prevent enum val staleness
    const table = {tbl_id: LINES_TBL_ID, title: 'Spectral Lines', tableData: {columns, data}, tableMeta};
    if (data.length === 0) table.status = {code: 204, message: 'No lines to display yet'}; // to replace default "No Data Found" status
    table.selectInfo = SelectInfo.newInstance({selectAll: true, rowCount: data.length}).data;
    dispatchTableAddLocal(table, undefined, false);
}

const uploadColumnFields = () => [
    {
        fieldKey: UPLOAD_WAVELENGTH_COL_KEY,
        name: 'Wavelength',
        guessValue: (columns) => columns?.find(({name}) =>
            ['wavelength', 'lambda'].includes(name.toLowerCase()))?.name ?? '',
        getFeedback: (value, columns) => {
            if (!value) return undefined;
            const col = columns?.find((c) => c.name === value);
            if (!isColumnType(col, COL_TYPE.NUMBER)) return 'Column type is not numeric - none of its rows will load as lines.';
            const unit = col?.units;
            if (!unit) return 'Column unit is unspecified - rows will load as lines assuming µm, which may be wrong.';
            return canUnitConv({from: unit, to: WAVELENGTH_COL_UNIT})
                ? `Column unit "${unit}" recognized - rows will load as lines in µm.`
                : `Column unit "${unit}" not recognized - none of its rows will load as lines.`;}
    },
    {fieldKey: UPLOAD_LABEL_COL_KEY, name: 'Species Label'},
    {fieldKey: UPLOAD_DESCRIPTION_COL_KEY, name: 'Description (optional)'},
];

// Wavelength/Label are required for the upload to be included in the merged table; Description stays optional,
// so (unlike UploadTableSelector's default header) it shouldn't trip the "Unspecified Column(s)" warning on its own
const uploadColumnMappingHeader = ([wavelengthCol, labelCol, descriptionCol]) =>
    (!wavelengthCol || !labelCol)
        ? <Typography color='warning'>{MISSING_COLS_HEADER_MSG}</Typography>
        : <Typography sx={{textWrap: 'wrap'}}>
              {`${wavelengthCol}, ${labelCol}` + (descriptionCol ? `, ${descriptionCol}` : '')}
          </Typography>;

/* wraps the generic UploadTableSelector with the wavelength/label/description mapping for a spectral line list */
function UploadTableSelectorSpectralLines({uploadInfo, setUploadInfo}) {
    return (
        <UploadTableSelector uploadInfo={uploadInfo} setUploadInfo={setUploadInfo}
                             columnFields={uploadColumnFields()}
                             columnMappingPanelKey={UPLOAD_MAPPING_PANEL_KEY}
                             allowUploadColumnsSelection={false}
                             allowClear={true}
                             uploadTblOptions={UPLOAD_TBL_OPTIONS}
                             slotProps={{columnMappingPanel: {
                                 headerTitle: 'Uploaded Line List Columns:',
                                 getHeaderColumnMapping: uploadColumnMappingHeader,
                             }}}/>
    );
}

/**
 * Standalone Spectral Lines dialog content - not specific to the chart it was opened from; the lines table
 * it manages is shared app-wide across all spectrum charts (see `useSpectralLinesSync`). keepState=true on the
 * FieldGroup so selections survive the dialog being closed/reopened.
 */
export function SpectralLinesPanel() {
    // FieldGroup has keepState=true, so this fixed default only matters the very first time this
    // session the dialog is opened - after that, the group's own last-seen value takes over
    const initialSourceOptions = ''; // nothing checked by default - lines table starts empty until applied
    const [getSourceOptions, setSourceOptions] = useFieldGroupValue(SOURCE_OPTIONS_KEY, SPECTRAL_LINES_FG_KEY);
    const sourceOptions = getSourceOptions() ?? initialSourceOptions;

    const [getUploadInfo, setUploadInfo] = useFieldGroupValue(UPLOAD_INFO_KEY, SPECTRAL_LINES_FG_KEY);
    const uploadInfo = getUploadInfo() || undefined;
    const [getUploadWavelengthCol, setUploadWavelengthCol] = useFieldGroupValue(UPLOAD_WAVELENGTH_COL_KEY, SPECTRAL_LINES_FG_KEY);
    const uploadWavelengthCol = getUploadWavelengthCol() ?? '';
    const [getUploadLabelCol, setUploadLabelCol] = useFieldGroupValue(UPLOAD_LABEL_COL_KEY, SPECTRAL_LINES_FG_KEY);
    const uploadLabelCol = getUploadLabelCol() ?? '';
    const [getUploadDescriptionCol, setUploadDescriptionCol] = useFieldGroupValue(UPLOAD_DESCRIPTION_COL_KEY, SPECTRAL_LINES_FG_KEY);
    const uploadDescriptionCol = getUploadDescriptionCol() ?? '';

    const [lineLists, setLineLists] = useState([]);

    useEffect(() => {
        // pre-register tbl_ui_id so columns/columnWidths get populated once loaded (TablePanel mounts later, too late)
        dispatchTableUiUpdate({tbl_ui_id: LINES_TBL_UI_ID, tbl_id: LINES_TBL_ID});
        void fetchLineLists().then((lists) => {
            setLineLists(lists);
            // build only if it doesn't exist yet - once built, row selection is user-owned and must survive
            // the dialog being closed/reopened; only the "Load Lines" button rebuilds after this point
            if (!getTblById(LINES_TBL_ID)) {
                void buildMergedLinesTable(sourceOptions, lists, uploadInfo, uploadWavelengthCol, uploadLabelCol, uploadDescriptionCol);
            }
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps -- intentionally mount-only; button click handles later rebuilds
    }, []);

    const {selectedCount, groupsCount, linesCount, loadedSourceOptions, loadedUploadSignature} = useStoreConnector(() => {
        const tbl = getTblById(LINES_TBL_ID);
        const fullTbl = tbl?.origTableModel ?? tbl; // origTableModel stores the unfiltered table after any filter is applied
        const linesCount = fullTbl?.totalRows ?? 0;
        const groupsCount = linesCount ? new Set(getColumnValues(fullTbl, GROUP_COL)).size : 0;
        const selectedCount = SelectInfo.newInstance(tbl?.selectInfo).getSelectedCount();
        const loadedSourceOptions = tbl?.tableMeta?.sourceOptions ?? '';
        const loadedUploadSignature = tbl?.tableMeta?.uploadSignature ?? '';
        return {selectedCount, groupsCount, linesCount, loadedSourceOptions, loadedUploadSignature};
    }, []);

    // true whenever the checked lists/upload mapping above no longer match what's actually loaded into the table below
    const hasPendingChanges = !sameSourceOptions(sourceOptions, loadedSourceOptions) ||
        uploadSignature(uploadInfo, uploadWavelengthCol, uploadLabelCol, uploadDescriptionCol) !== loadedUploadSignature;

    const plotHelperText = linesCount === 0
        ? (hasPendingChanges
            ? 'No lines loaded yet - click "Load Lines" above to reflect the changes in list(s) selection'
            : 'No lines loaded - select list(s) above and then click "Load Lines"')
        : (hasPendingChanges
            ? 'Showing previously loaded lines - click "Load Lines" above to reflect the changes in list(s) selection'
            : undefined);

    const onLoadLines = () => {
        void buildMergedLinesTable(sourceOptions, lineLists, uploadInfo, uploadWavelengthCol, uploadLabelCol, uploadDescriptionCol);
        dispatchComponentStateChange(SOURCES_COLLAPSIBLE_KEY, {isOpen: false}); // collapse to reveal the table below
    };

    // clears every source (checked lists + upload/mapping) and immediately rebuilds to a truly empty lines table
    const onClearAll = () => {
        setSourceOptions('');
        setUploadInfo(undefined);
        setUploadWavelengthCol('');
        setUploadLabelCol('');
        setUploadDescriptionCol('');
        void buildMergedLinesTable('', lineLists, undefined, '', '', '');
    };

    return (
        <FieldGroup groupKey={SPECTRAL_LINES_FG_KEY} keepState={true}>
            <Stack sx={{minWidth: '40rem', maxHeight: '82vh'}}>
                <Stack spacing={2} sx={{p: 1, pr: 2, overflowY: 'auto', minHeight: 0}}>
                    <CollapsibleGroup>
                        <CollapsibleItem componentKey={SOURCES_COLLAPSIBLE_KEY}
                                         header={(isOpen) => (
                                             <Stack>
                                                 <Typography level='title-md'>Select line lists to load</Typography>
                                                 {!isOpen &&
                                                     <Typography level='body-sm'>
                                                         {linesCount === 0
                                                             ? 'No lines loaded'
                                                             : `${groupsCount} list${groupsCount === 1 ? '' : 's'}, ${linesCount} line${linesCount === 1 ? '' : 's'} loaded`}
                                                     </Typography>}
                                             </Stack>
                                         )}
                                         isOpen={true}>
                            <Stack spacing={2} sx={{}}>
                                <CheckboxGroupInputField fieldKey={SOURCE_OPTIONS_KEY}
                                                         label='Available line lists:'
                                                         alignment='vertical'
                                                         initialState={{value: initialSourceOptions}}
                                                         options={lineLists.map(({listId, listLabel}) =>
                                                             ({label: listLabel, value: listId}))}/>
                                <Stack spacing={0.25}>
                                    <Typography level='title-sm'>Upload your own line list:</Typography>
                                    <UploadTableSelectorSpectralLines uploadInfo={uploadInfo} setUploadInfo={setUploadInfo}/>
                                </Stack>
                            </Stack>
                            <Divider sx={{
                                mt: 3, mb: 1,
                                mx: 'calc(-1 * var(--ListItem-paddingX))' // to extend to the edges of collapsible
                            }}/>
                            <Stack spacing={0.5}>
                                <Stack direction='row' spacing={1} alignItems='center'>
                                    <Button size='md' variant='solid' onClick={onLoadLines}>Load Lines</Button>
                                    <Button size='md' variant='soft' color='neutral' onClick={onClearAll}>Clear All</Button>
                                </Stack>
                                {hasPendingChanges &&
                                    <Typography level='body-xs' color='warning'>
                                        changes above not yet loaded in table below
                                    </Typography>}
                            </Stack>
                        </CollapsibleItem>
                    </CollapsibleGroup>
                    <Stack spacing={.5} sx={{pb: 1}}>
                        <Typography level='title-md'>Select lines to plot:</Typography>
                        {plotHelperText &&
                            <Typography level='body-sm' color='neutral'>{plotHelperText}</Typography>}
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
                                highlightedRowHandler={() => undefined}
                            />
                        </Stack>
                    </Stack>
                </Stack>
                <Divider/>
                <Stack sx={{py: 0.5, flexShrink: 0}}>
                    <Typography level='body-sm' startDecorator={<Insights color='primary'/>}>
                        {selectedCount === 0
                            ? '0 lines selected - nothing plotted on spectral chart(s) ↘'
                            : <>
                                  <Typography fontWeight='lg'>{selectedCount} lines</Typography>
                                  &nbsp;selected - plotted live on spectral chart(s) ↘
                              </>}
                    </Typography>
                </Stack>
            </Stack>
        </FieldGroup>
    );
}
