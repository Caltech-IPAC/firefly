import {get, set, isEmpty, flatten, isArray} from 'lodash';
import {getDrawLayersByType} from './PlotViewUtil.js';
import {getDlAry, dispatchCreateDrawLayer} from './DrawLayerCntlr.js';
import HiPSMOC from '../drawingLayers/HiPSMOC.js';
import {getHealpixCornerTool} from './HiPSUtil.js';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {getBooleanMetaEntry, getTblById} from '../tables/TableUtil';
import {MetaConst} from 'firefly/data/MetaConst.js';
const HEADER_KEY_COL = 1;
const HEADER_VAL_COL = 2;

const MOC = '_moc';
let   mocCnt = 0;

export const MOCInfo = 'mocInfo';
export const UNIQCOL = 'uniqColName';
export const MOCOrder = 'mocOrder';

export function getAppHiPSForMoc() {
    return get(getAppOptions(), ['hips', 'hipsForMoc'], 'https://irsa.ipac.caltech.edu/data/hips/CDS/2MASS/Color');
}

export function makeMocTableId(ivoid) {
    const id = ivoStr(ivoid);

    return id.endsWith(MOC) ? id : id+MOC;
}

export function ivoStr(ivoid) {
    return ivoid ? ivoid.trim().replace('ivo://', '').replace(/\//g,'_') : 'moc_table_' + (++mocCnt);
}

const convertToEntries= (data) => data.map( (d) => [d[HEADER_KEY_COL], d[HEADER_VAL_COL]]);


/**
 * check if the table from upload analysis is valid with fits header
 * @param report
 * @returns {Object} valid and mocInfo including nipxColName and MOCOrder
 */
function isAnalysisTableMocFits(report) {

    if (get(report, 'parts.length') !== 2 || get(report, 'parts.1.type') !== 'Table') {
        return {valid: false};
    }

    const mocTableHeader = get(report, 'parts.1.details');
    const {data} = mocTableHeader.tableData || {};
    if (!data) return {valid:false};

    return doHeadersMatchMOC(convertToEntries(data));
}


function doHeadersMatchMOC(sourceHeaderEntries, doTableValidation= true) {

    const k= 0;
    const v= 1;
    const mocKeySet = [{PCOUNT: '0'}, {GCOUNT: '1'}, {TFIELDS: '1'},
        [
            {TFORM1: '1J',  NAXIS1: '4'},
            {TFORM1: 'J',  NAXIS1: '4'},
            {TFORM1: '1K', NAXIS1: '8'},
            {TFORM1: 'K', NAXIS1: '8'}
        ]];

    const mocKeys = flatten(mocKeySet).reduce((prev, oneCond) => {
        Object.keys(oneCond).forEach((aKey) => {
            !(prev.includes(aKey)) && prev.push(aKey);
        });
        return prev;
    }, []);

    const mandatoryKeys = {
        MOCORDER: '',
        PIXTYPE: 'HEALPIX',
        ORDERING: 'NUNIQ',
        COORDSYS: '',
        TTYPE1: ''
    };
    const mocKeyMap = {TTYPE1: UNIQCOL, MOCORDER: MOCOrder};

    const mocRetVal = {};

    const headerItems = sourceHeaderEntries.reduce((prev, oneHeaderItem) => {
        if (mocKeys.includes(oneHeaderItem[k])) {
            set(prev, oneHeaderItem[k], oneHeaderItem[v]);
        } else if (Object.keys(mandatoryKeys).includes(oneHeaderItem[k])) {
            if (!mandatoryKeys[oneHeaderItem[HEADER_KEY_COL]] ||
                mandatoryKeys[oneHeaderItem[k]].toLowerCase() === oneHeaderItem[v].toLowerCase()) {
                if (Object.keys(mocKeyMap).includes(oneHeaderItem[k])) {
                    set(mocRetVal, mocKeyMap[oneHeaderItem[k]], oneHeaderItem[v]);
                } else {
                    set(mocRetVal, oneHeaderItem[k], oneHeaderItem[HEADER_VAL_COL]);
                }
            }
        }
        return prev;
    }, {});

    if (Object.keys(mocRetVal).length !== Object.keys(mandatoryKeys).length) {
        return {valid: false};
    }
    let valid= true;

    if (doTableValidation) {
        const validateCond = (oneCond) => {
            const n = Object.keys(oneCond).findIndex((aKey) => {
                return get(headerItems, aKey, '').toLowerCase() !== oneCond[aKey].toLowerCase();
            });
            return (n < 0);
        };

        valid = !mocKeySet.find((oneCond) => {     // find if any cond is not satisfied
            if (isArray(oneCond)) {      // find if any sub condition is satisfied
                const anySubValid = oneCond.find((subCond) => {
                    return validateCond(subCond);
                });
                return !anySubValid;    // none of sub-cond is satisfied => this cond is not satisfied
            } else {
                return !validateCond(oneCond);  // this cond is not satisfied
            }
        });
    }

    return {valid, [MOCInfo]: valid && mocRetVal};

}


/**
 * check to see if the loaded table is a MOC
 * @param {TableModel} table
 * @returns {boolean} true if table is a MOC
 */
export function isTableMOC(table) {
    if (!table || !table.tableMeta || !table.tableData) return false;
    const columnsCnt= table.tableData.columns.filter( (c) => c.name!=='ROW_IDX' && c.name!=='ROW_NUM').length;
    if (columnsCnt>1) return false;
    if (getBooleanMetaEntry(table, MetaConst.IGNORE_MOC)) return false;
    const entries= [
        ['TTYPE1', get(table.tableData, ['columns','0', 'name'])],
        ...Object.entries(table.tableMeta)
    ];
    return doHeadersMatchMOC(entries, false).valid;
}

/**
 * check if the moc fits valid with fits header
 * @param report
 * @returns {Object} valid and mocInfo including nipxColName and MOCOrder
 */
export function isMOCFitsFromUploadAnalsysis(report) {
    const fileType = report && report.dataTypes;

    if (!fileType || !(fileType.includes('Table'))) {
        return {valid: false};
    }

    return isAnalysisTableMocFits(report);
}


export const NSIDE2 = new Array(64).fill(0).map((v, i) => 2**i);
export const NSIDE4 = new Array(32).fill(0).map((v, i) => 4**i);

export function getMocNuniq(order, npix) {
    return NSIDE4[order+1] + npix;
}

export function getMocOrderIndex(Nuniq) {
    const norder = Math.floor(Math.log2(Nuniq/4)/2);
    const npix = Nuniq - NSIDE4[norder + 1];

    return {norder, npix};
}

/**
 * add new layer on MOC table
 * @param {Object} params moc table id
 * @param {string} params.tbl_id moc table id
 * @param {string} [params.title] optional title
 * @param {string} params.fitsPath moc fits path at the server after upload
 * @param {string} params.mocUrl  moc fits url
 * @param {string} params.uniqColName column name for uniq number
 * @param {boolean} params.tablePreloaded - true if the MOC table has already been loaded
 * @param {string} [params.color] - color string
 * @param {string} [params.mocGroupDefColorId ] - group color id
 * @returns {T|SelectInfo|*|{}}
 */
export function addNewMocLayer({tbl_id, title, fitsPath, mocUrl, uniqColName = 'NUNIQ',
                                   tablePreloaded=false, color, mocGroupDefColorId }) {
    const dls = getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID);
    let   dl = dls.find((oneLayer) => oneLayer.drawLayerId === tbl_id);

    if (!dl) {
        if (!title && tablePreloaded && tbl_id) title= getTblById(tbl_id)?.title;
        if (title) title= 'MOC - ' + title;
        const mocFitsInfo = {fitsPath, mocUrl, uniqColName, tbl_id, tablePreloaded};
        dl = dispatchCreateDrawLayer(HiPSMOC.TYPE_ID,
            {mocFitsInfo,title,layersPanelLayoutId:'mocUIGroup', color, mocGroupDefColorId });
    }
    return dl;
}

const sideCells = [[0], [0, 1, 3, 2]];   // side cells from order 0

/**
 * tile at each side as the tile of order 0 is sub-divided into 4 tiles recursively up to order 'maxOrder'
 * @param {number} maxOrder  the max order to generate the side cells
 * @return {array} side cells from left bottom corner of order 0 to maxOrder, the return is like
 *                  [ [0]   // cell 0 at order 0
 *                    [0, 1, 3, 2]   // side cells at order 1, from the side cells of order 0
 *                    [0, .........] // side cells at order 2, from the side cells of order 1
 *                   ...]
 */
export function computeSideCellsToOrder(maxOrder) {
    const sideOffset = [[0, 1], [1, 3], [3, 2], [2, 0]];               // offset used to get sub-divided side cells of next order at each side
    const cornerOffset = [[2, 0, 1], [0, 1, 3], [1, 3, 2], [3, 2, 0]];  // offset used to get sub-divided corner cells of next order at each corner

    for (let i = sideCells.length; i <= maxOrder; i++) {
        const cornerInt = (2**(i-1)) - 1;

        // sub-divide side cells of previous order
        const cells = sideCells[i-1].reduce((prev, cellNum, idx) => {
                const atSide = Math.floor(idx/cornerInt);             // side #
                if (idx%cornerInt === 0) {                            // cell at the corner
                    const corners = cornerOffset[atSide].map((offset) => (cellNum*4+offset));   // sub-divided cells at the side

                    prev = [...prev,...corners];
                } else {                                              // side but corner cells
                    const sCells = sideOffset[atSide].map((offset) => (cellNum*4 + offset));  // sub-divided cells at the side

                    prev = [...prev,...sCells];
                }
                return prev;
        }, []);
        const newCells = [...cells.slice(1), cells[0]];

        sideCells.push(newCells);
    }

    return sideCells;
}


let sidePoints = {};
const sideCorners = [[2, 3], [3, 0], [0, 1], [1, 2]];

function setPoints(norder, npix, corners) {
    if (!get(sidePoints, norder)) {
        set(sidePoints, norder, {[npix]: corners});
    } else {
        set(sidePoints, [norder, npix], corners);
    }
}

/**
 * get the stored pixels along the sides of the tile
 * @param order
 * @param npix
 * @param wpCorners
 * @returns {*}
 */
export function getSidePointsNorder(order, npix, wpCorners) {
    let corners;
    let retVal = get(sidePoints, [order, npix]);

    if (isEmpty(retVal)) {
        if (wpCorners) {
            corners = [2, 3, 0, 1].map((t) => wpCorners[t]);
            setPoints(order, npix, corners);
        }
        retVal = corners;
    }

    return retVal;
}

/**
 * get 4 corners of the tile based on all pixels around its polygon representation
 * @param ptAry
 * @returns {*}
 */
export function getCornersFromSidePoints(ptAry) {
    if (ptAry.length > 4) {
        const sideLen = Math.trunc(ptAry.length / 4);
        const selCorners = [];

        for (let i = 0; i < ptAry.length; i += sideLen) {
            selCorners.push(ptAry[i]);
        }
        return selCorners;
    } else {
        return ptAry;
    }
}

export function initSidePoints(storedSidePoints) {
    sidePoints = storedSidePoints;
}


/**
 * get corners from healpix, some fixing on order 0, need more investigation later.
 * @param wpCorners  in original order
 * @param npix
 * @param healpixCache
 * @param coordsys
 * @returns {*}      in original order
 */
export function fixCornersOrderZero(wpCorners, npix, healpixCache, coordsys) {
    const base_npix = npix * 4;
    const selCorners = [2, 3, 0, 1];
    sideCells[1].forEach((ipx, idx) => {
        const pt = healpixCache.makeCornersForPix(base_npix + ipx, 2, coordsys);
        const cIdx = selCorners[idx];
        if (pt.wpCorners[cIdx].x !== wpCorners[cIdx].x ||
            pt.wpCorners[cIdx].y !== wpCorners[cIdx].y) {
            wpCorners[cIdx] = pt.wpCorners[cIdx];
        }
    });
    return wpCorners;
}

/**
 * return tile corner pixels from stored side points or the corners are either passed externally or calculated
 * internally. The returned corners is in original order.
 * Note: some tile of order 0 is fixed by side points of order 1, the calculation on the corners of
 * order 0 tile is to be investigated later.
 * @param norder
 * @param npix
 * @param coordsys
 * @param healpixCache
 * @param corners  original corner
 * @param bKeep
 * @returns {*}  original corner
 */
export function getCornerForPix(norder, npix, coordsys, healpixCache, corners, bKeep = true) {
    const ptAry = getSidePointsNorder(norder, npix, null);

    if (!isEmpty(ptAry)) {    // already exist
        const selCorners = getCornersFromSidePoints(ptAry);

        return {wpCorners: [2, 3, 0, 1].map((i) => selCorners[i])};
    }

    const {wpCorners} = corners ? {wpCorners: corners} : healpixCache.makeCornersForPix(npix, NSIDE2[norder], coordsys);

    // correct the corners for order 0 to coincide with those of higher order
    if (norder === 0) {
        fixCornersOrderZero(wpCorners, npix, healpixCache, coordsys);
    }

    if (bKeep) {
        getSidePointsNorder(norder, npix, wpCorners);
    }
    return {wpCorners};
}



//const upMap = new Array(30).fill(0).map ((i, idx) => Math.floor(idx/2) + Math.floor(idx/3));
/**
 * return the pixels around the MOC tile represented by polygon including at least 4 corners of the tile
 * more pixels between every two corners are produced for better rendering resolution
 * @param norder
 * @param npix
 * @param topOrder
 * @param coordsys
 * @param isAllSky
 * @returns {*}
 */
export function getMocSidePointsNuniq(norder, npix, topOrder, coordsys, isAllSky) {
    const sPoints = getSidePointsNorder(norder, npix);

    if (!sPoints) return null;
    const healpixCache = getHealpixCornerTool();
    const newSidePoints = sPoints.slice();
    const crtSidePointsOrder = Math.log2((newSidePoints.length / 4));
    const  dUp = isAllSky ? Math.min((topOrder - norder), 8) : Math.floor((topOrder - norder +1)/2);

    if (crtSidePointsOrder === dUp) {
        return newSidePoints;
    } else if (crtSidePointsOrder < dUp) {   // needs to insert more points by dUp-crtSidePointsOrder levels
        const sCells = computeSideCellsToOrder(dUp);

        // repeatedly insert the corner points into current side points representation order by order up

        for (let i = crtSidePointsOrder + 1; i <= dUp; i++) {             // order difference from norder
            const nextOrder = norder + i;             // order of next side points representation
            const base_npix = npix * (NSIDE4[i]);
            const upCells = sCells[i];
            const totalPtsOneSide = upCells.length / 4;
            let insertAt = 1;

            [0, 1, 2, 3].forEach((s) => {
                const firstIdx = s * totalPtsOneSide + 1;
                const lastIdx = firstIdx - 1 + totalPtsOneSide;
                for (let j = firstIdx; j <= lastIdx; j += 2) {
                    const k = j % upCells.length;
                    const nextNpix = base_npix + upCells[k];
                    const {wpCorners} = getCornerForPix(nextOrder, nextNpix, coordsys, healpixCache, null, false);

                    newSidePoints.splice(insertAt, 0, wpCorners[sideCorners[s][0]]);
                    insertAt += 2;
                }
            });
        }
        setPoints(norder, npix, newSidePoints);
        return newSidePoints;
    } else if (crtSidePointsOrder > dUp) {            // already insert enough points
        if (topOrder < norder) {
            return getCornersFromSidePoints(newSidePoints);   // original 4 corners
        } else {
            const levelDown = NSIDE2[crtSidePointsOrder - dUp];
            const newPoints = newSidePoints.reduce((prev, v, idx) => {
                if (idx%levelDown === 0) {
                    prev.push(v);
                }
                return prev;
            }, []);
            return newPoints;
        }
    }
}

export function isTileVisibleByPosition(wpCorners, cc) {
    const selCorners = wpCorners.length > 4 ? getCornersFromSidePoints(wpCorners) : wpCorners;
    const {width, height} = get(cc, 'viewDim') || {};
    if (!width || !height) return false;

    return selCorners.find((onePt) => {
        const devPt = cc.getDeviceCoords(onePt);
        return (devPt) ? ((devPt.x >= 0 && devPt.x <= width)&&(devPt.y >= 0 && devPt.y <= height)) : false;
    });
}


export function getDefaultMOCList() {
    return [
        'ivo://CDS/P/GALEXGR6/AIS/color',
        'ivo://CDS/P/GALEXGR6/AIS/FUV',
        'ivo://CDS/P/GALEXGR6/AIS/NUV ',
        'ivo://CDS/P/ROSATWFC/color',
        'ivo://cxc.harvard.edu/P/cda/hips/allsky/rgb',
        'ivo://CDS/P/PanSTARRS/DR1/color-z-zg-g',
        'ivo://CDS/P/SDSS9/color',
        'ivo://CDS/P/ZTF/DR7/color',
        'ivo://CDS/P/ZTF/DR7/g',
        'ivo://CDS/P/ZTF/DR7/i',
        'ivo://CDS/P/ZTF/DR7/r',
        'ivo://CDS/P/DES-DR1/Y',
        'ivo://CDS/P/DES-DR1/g',
        'ivo://CDS/P/DES-DR1/i',
        'ivo://CDS/P/DES-DR1/r',
        'ivo://CDS/P/DES-DR1/z',
        'ivo://CDS/P/VISTA/VVV/DR4/J',
        'ivo://CDS/P/VISTA/VVV/DR4/Y',
        'ivo://CDS/P/VISTA/VVV/DR4/Z',
        'ivo://CDS/C/HIPASS',
        'ivo://CDS/P/NVSS',
        'ivo://CDS/P/SUMSS',
        'ivo://ov-gso/P/SUMSS',
        'ivo://ESAVO/P/HERSCHEL/PACS-color',
        'ivo://CDS/P/SPITZER/IRAC1',
        'ivo://CDS/P/SPITZER/IRAC2',
        'ivo://CDS/P/SPITZER/IRAC3',
        'ivo://CDS/P/SPITZER/IRAC4',
        'ivo://xcatdb/P/XMM/PN/eb4',
        'ivo://ESAVO/P/EXOSAT/all',
        'ivo://CDS/P/HST/PHAT/F275W',
        'ivo://CDS/P/HLA/SDSSg'
    ];
}