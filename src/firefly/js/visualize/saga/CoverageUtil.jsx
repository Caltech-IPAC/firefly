import {Stack, Typography} from '@mui/joy';
import Enum from 'enum';
import {isEmpty, isNil, isString, values} from 'lodash';
import React from 'react';
import {CatalogType} from '../../drawingLayers/Catalog';
import {getCornersColumns} from '../../tables/TableInfoUtil';
import {getTblById} from '../../tables/TableUtil';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser';
import {memorizeLastCall} from '../../util/WebUtil';
import {findTableRegionColumn, isOrbitalPathTable, isTableWithRegion} from '../../voAnalyzer/TableAnalysis';
import {dispatchAttributeChange, visRoot} from '../ImagePlotCntlr';
import {PlotAttribute} from '../PlotAttribute';
import {DEFAULT_COVERAGE_PLOT_ID, primePlot} from '../PlotViewUtil';
import {DrawLayersButton} from '../ui/Buttons';
import {showDrawingLayerPopup} from '../ui/DrawLayerPanel';

/** @type CoverageType */
export const CoverageType = new Enum(['X', 'BOX', 'REGION', 'ORBITAL_PATH', 'ALL', 'GUESS']);

export function updateWarnings(preparedTables, options) {
    const ary = values(preparedTables);
    let cnt = 0;
    let firstTableWarning;
    const tblWarnings = ary
        .filter((t) => t && t !== 'WORKING')
        .map((t) => {
            const originalCovType = getCoverageType(options, t.originalTableModel?.tbl_id ?? t.tbl_id);
            const actualCoverageType = t.coverageType ?? CoverageType.X;
            if (actualCoverageType === CoverageType.X && (originalCovType === CoverageType.BOX || originalCovType === CoverageType.REGION)) {
                const title = t.originalTableModel?.title ?? t.title;
                cnt++;
                if (cnt === 1) firstTableWarning = title;
                return `${title} has too many rows (${t.totalRows}) to show the boundaries of each frame.`;
            }
        })
        .filter(Boolean);

    const plot = primePlot(visRoot(), DEFAULT_COVERAGE_PLOT_ID);
    if (!plot) return;

    const warnings = {};

    if (cnt) {
        warnings.additionalInfo1 = (
            <Typography color='warning' key='1'>
                {`To see the individual boundaries, filter down your results to below ${options.maxRegionObjects} rows.`}
            </Typography>
        );

        warnings.tblWarnings = (
            <Stack key='tblWarnings'>
                <Typography level='title-md'>
                    Warning about too many rows
                </Typography>
                <ul>
                    {tblWarnings.map((s) => <li key={s}><Typography>{s}</Typography></li>)}
                </ul>
                <Typography>
                    Any table with too many rows to show the boundaries individually will be shown as just the centers
                    (like a catalog would be shown).
                </Typography>
            </Stack>
        );

        warnings.additionalInfo2 = <div key='2'/>;

        warnings.additionalInfo3 =
            (
                <Stack key='3'>
                    <Typography level='title-md'>
                        Warning about showing coverage like catalogs
                    </Typography>
                    <Typography component='div'>
                        Catalogs above a dynamically-adapting threshold are shown as a 'cell' with a number to indicate
                        the number of sources within that cell.
                        Zoom in to see smaller cells until individual sources are shown; see the layers pop-up
                        <Stack direction='row' alignItems='center' display='inline-flex' paddingInline='.25rem'>
                            <Typography>(</Typography>
                            <DrawLayersButton
                                tip='Manipulate overlay display: control color, visibility, and advanced options'
                                onClick={showDrawingLayerPopup}/>
                            <Typography>)</Typography>

                        </Stack>
                        to change how this is shown (and the size of the cells),
                        and see the online help for more information.
                    </Typography>
                </Stack>
            );

        warnings.title = 'Coverage Warnings';
        warnings.tooltip = cnt === 1 ? `Coverage warning for table: ${firstTableWarning}` : `Coverage warnings for ${cnt} tables`;
    }


    dispatchAttributeChange({
        plotId: DEFAULT_COVERAGE_PLOT_ID,
        changes: {[PlotAttribute.USER_WARNINGS]: warnings}
    });
}

export const getCatalogType = (dataType) => {
    switch (dataType) {
        case CoverageType.REGION:
            return CatalogType.REGION;
        case CoverageType.X:
            return CatalogType.POINT;
        case CoverageType.ORBITAL_PATH:
            return CatalogType.ORBITAL_PATH;
        case CoverageType.BOX:
            return CatalogType.BOX;
        default:
            return CatalogType.POINT;
    }
};

export function getCoverageType(options,tableOrId) {

    const table= isString(tableOrId) ? getTblById(tableOrId) : tableOrId;
    if (!table) return CoverageType.X;
    if (isOrbitalPathTable(table)) return CoverageType.ORBITAL_PATH;

    if (options.coverageType===CoverageType.GUESS ||
        options.coverageType===CoverageType.REGION ||
        options.coverageType===CoverageType.BOX ||
        options.coverageType===CoverageType.ALL) {
        const regionAry = getRegionAryFromTable(table);
        if (isTableWithRegion(table.tbl_id) && regionAry.length === 0) {
            //helpful note for developer/user in the console
            console.log(`Note: We could not parse s_region correctly, possibly because we do not support this format yet, 
            hence it is not displayed in the coverage map.`);
        }
        return (isTableWithRegion(table) && regionAry.length>0)
            ? CoverageType.REGION
            : hasCorners(options,table)
                ? CoverageType.BOX
                : CoverageType.X;
    }
    return options.coverageType;
}

const isValidNum= (n) => !isNil(n) && !isNaN(Number(n));

function hasCorners(options, table) {
    const cornerColumns= getCornersColumns(table);
    if (isEmpty(cornerColumns)) return false;
    const tblData = table?.tableData?.data ?? [];
    const dataCnt= tblData.reduce( (tot, row) =>
            cornerColumns.every( (cDef) => isValidNum(row[cDef.lonIdx]) && isValidNum(row[cDef.lonIdx]) ) ? tot+1 : tot
        ,0);
    return dataCnt > 0;
}

export const getRegionAryFromTable = memorizeLastCall( (table) => {
    const rCol = findTableRegionColumn(table);
    if (!rCol) return [];
    return (table?.tableData?.data ?? [])
        .map((row) => {
            const cornerInfo = parseObsCoreRegion(row[rCol.regionIdx], rCol.unit, true);

            return cornerInfo.valid ? cornerInfo.corners : [];
        }).filter((r) => !isEmpty(r));
});
