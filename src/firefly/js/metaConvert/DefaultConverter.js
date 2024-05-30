import {isEmpty} from 'lodash';
import {MetaConst} from '../data/MetaConst.js';
import {getCellValue, getColumn} from '../tables/TableUtil.js';
import {getDataSourceColumn} from '../voAnalyzer/TableAnalysis.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {makeWorldPt, parseWorldPt} from '../visualize/Point.js';
import RangeValues from '../visualize/RangeValues.js';
import {TitleOptions, WebPlotRequest} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';

const FILE = 'FILE';

/**
 * Support data that we don't know about
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @return {{}}
 */
export function makeRequestForUnknown(table, row, includeSingle, includeStandard) {

    const {tableMeta: meta} = table;

    const dataSource = findADataSourceColumn(table);
    if (!dataSource) return {};


    let positionWP = null;

    let sAry = meta[MetaConst.POSITION_COORD_COLS] && meta[MetaConst.POSITION_COORD_COLS].split(';');
    if (!sAry) sAry = meta[MetaConst.CENTER_COLUMN] && meta[MetaConst.CENTER_COLUMN].split(';');

    if (!isEmpty(sAry)) {
        const lon = Number(getCellValue(table, row, sAry[0]));
        const lat = Number(getCellValue(table, row, sAry[1]));
        const csys = CoordinateSys.parse(sAry[2]);
        positionWP = makeWorldPt(lon, lat, csys);
    } else if (meta[MetaConst.POSITION_COORD]) {
        positionWP = parseWorldPt(meta[MetaConst.POSITION_COORD]);
    }


    const retval = {};
    if (includeSingle) {
        retval.single = makeRequest(table, dataSource.name, positionWP, row);
    }

    if (includeStandard) {
        retval.standard = [makeRequest(table, dataSource.name, positionWP, row)];
        retval.highlightPlotId = retval.standard[0].getPlotId();
    }

    return retval;

}

export function makeRequestSimpleMoving(table, row, includeSingle, includeStandard) {

    const {tableMeta: meta} = table;


    const dataSource = findADataSourceColumn(table);

    if (!dataSource) return {};


    const sAry = meta[MetaConst.POSITION_COORD_COLS].split(';');
    if (!sAry || sAry.length !== 3) return [];

    let positionWP = null;
    if (!isEmpty(sAry)) {
        const lon = Number(getCellValue(table, row, sAry[0]));
        const lat = Number(getCellValue(table, row, sAry[1]));
        const csys = CoordinateSys.parse(sAry[2]);
        positionWP = makeWorldPt(lon, lat, csys);
    }

    const retval = {};
    if (includeSingle) {
        retval.single = makeMovingRequest(table, row, dataSource.name, positionWP, 'simple-moving-single-' + (row % 24));
    }

    if (includeStandard) {
        retval.standard = [makeMovingRequest(table, row, dataSource.name, positionWP, 'simple-moving-single')];
        retval.highlightPlotId = retval.standard[0].getPlotId();
    }

    return retval;

}

export const defDataSourceGuesses = ['FILE', 'FITS', 'DATA', 'SOURCE', 'URL', 'IMAGE_URL', 'access_url'];

export function findADataSourceColumn(table) {
    if (!table || table.isFetching) return false;
    const columns = table?.tableData?.columns;
    if (!columns) return false;
    const dsCol = getDataSourceColumn(table);
    if (dsCol) return getColumn(table, dsCol);
    if (dsCol === false) return false;
    // if dsCol is undefined then start guessing
    const guesses = defDataSourceGuesses.map((g) => g.toUpperCase());
    return columns.find((c) => guesses.includes(c.name.toUpperCase()));
}

/**
 *
 * @param table
 * @param row
 * @param dataSource
 * @param positionWP
 * @param plotId
 * @return {*}
 */
function makeMovingRequest(table, row, dataSource, positionWP, plotId) {
    const url = getCellValue(table, row, dataSource);
    const r = WebPlotRequest.makeURLPlotRequest(url, 'Fits Image');
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setZoomType(ZoomType.TO_WIDTH_HEIGHT);
    r.setPlotId(plotId);
    r.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
    r.setOverlayPosition(positionWP);
    return r;

}

/**
 *
 * @param table
 * @param dataSource
 * @param positionWP
 * @param row
 * @return {*}
 */
function makeRequest(table, dataSource, positionWP, row) {
    if (!table || !dataSource) return null;

    let r;
    const source = getCellValue(table, row, dataSource);
    if (dataSource.toLocaleUpperCase() === FILE) {
        r = WebPlotRequest.makeFilePlotRequest(source, 'DataProduct');
    } else {
        r = WebPlotRequest.makeURLPlotRequest(source, 'DataProduct');
    }
    r.setZoomType(ZoomType.FULL_SCREEN);
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setPlotId(source);
    if (positionWP) r.setOverlayPosition(positionWP);

    return r;
}