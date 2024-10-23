/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, has, isArray, isEmpty, isString} from 'lodash';
import {MetaConst} from '../data/MetaConst.js';
import {getColumn, getColumnIdx, getColumns, getMetaEntry} from '../tables/TableUtil.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {
    alternateMainPos, ColNameIdx, mainMeta, obsCorePosColumns, OBSTAPCOLUMNS, POS_EQ_UCD, posCol, S_REGION,
    SSA_COV_UTYPE, UCDCoord, ucdSyntaxMap, UtypeColIdx
} from './VoConst.js';
import {getObsTabColEntry, isUCDWith} from './VoCoreUtils.js';


/**
 * table analyzer based on table model for catalog or image metadata
 */
export class VoTableRecognizer {
    constructor(tableModel, posCoord = 'eq') {
        this.tableModel = tableModel;
        this.columns = get(tableModel, ['tableData', 'columns'], []);
        this.obsCoreInfo = {isChecked: false, isObsCoreTable: false};
        this.posCoord = posCoord;
        this.centerColumnsInfo = null;
        this.centerColumnCandidatePairs = null;
        this.regionColumnInfo = null;
    }

    isObsCoreTable() {
        if (this.obsCoreInfo.isChecked) {
            return this.obsCoreInfo.isObsCoreTable;
        }

        const allColNames = this.columns.map((oneCol) => oneCol.name);

        const nonExistCol = OBSTAPCOLUMNS
            .map((oneColumn) => (oneColumn[ColNameIdx]))
            .some((oneName) => {
                return !allColNames.includes(oneName);
            });

        this.obsCoreInfo.isChecked = true;
        this.obsCoreInfo.isObsCoreTable = !nonExistCol;

        return this.obsCoreInfo.isObsCoreTable;
    }

    /**
     * find and fill center column info
     * @param colPair [lonCol, latCol]
     * @param csys
     * @returns {null|CoordColsDescription}
     */
    setCenterColumnsInfo(colPair, csys = CoordinateSys.EQ_J2000) {
        this.centerColumnsInfo = null;

        if (isArray(colPair) && colPair.length >= 2) {
            const lonCol = isString(colPair[0]) ? colPair[0] : colPair[0].name;
            const latCol = isString(colPair[1]) ? colPair[1] : colPair[1].name;

            const idxs = getLonLatIdx(this.tableModel, lonCol, latCol);

            if (idxs) {
                this.centerColumnsInfo = {
                    type: 'center',
                    lonCol,
                    latCol,
                    lonIdx: idxs.lonIdx,
                    latIdx: idxs.latIdx,
                    csys
                };
            }
        }
        return this.centerColumnsInfo;
    }

    setRegionColumnInfo(col) {
        this.regionColumnInfo = null;

        const idx = getColumnIdx(this.tableModel, col.name);
        if (idx >= 0) {
            this.regionColumnInfo = {
                type: 'region',
                regionCol: col.name,
                regionIdx: idx,
                unit: col.units
            };
        }

        return this.regionColumnInfo;
    }

    /**
     * filter the columns per ucd value defined in the UCD value of relevant OBSTAP column
     * @param ucds ucd value defined in OBSTAP, it may contain more than one ucd values
     * @returns {*}
     */
    getTblColumnsOnDefinedUCDValue(ucds) {
        const ucdList = ucds.split(';');

        return ucdList.reduce((prev, ucd) => {
            prev = prev.filter((oneCol) => {
                return (has(oneCol, 'UCD') && isUCDWith(oneCol.UCD, ucd, get(ucdSyntaxMap, ucd)));
            });
            return prev;
        }, this.columns);

    }

    /**
     * get columns containing the same ucd value
     * @param ucd
     * @returns {Array}
     */
    getTblColumnsOnUCD(ucd) {
        return this.columns.filter((oneCol) => {
            return (has(oneCol, 'UCD') && isUCDWith(oneCol.UCD, ucd, get(ucdSyntaxMap, ucd)));
        });
    }


    /**
     * get columns containing the utype
     * @param utype
     * @returns {array}
     */
    getTblColumnsOnUType(utype) {
        return this.columns.filter((oneCol) => {
            return has(oneCol, 'utype') && oneCol.utype.includes(utype);
        });
    }

    /**
     * get columns containing ucd word by given table columns
     * @param cols
     * @param ucdWord
     * @returns {array}
     */
    getColumnsWithUCDWord(cols, ucdWord) {
        if (isEmpty(cols)) return [];

        return cols.filter((oneCol) => {
            return has(oneCol, 'UCD') && isUCDWith(oneCol.UCD, ucdWord, get(ucdSyntaxMap, ucdWord));
        });
    }

    /**
     * get center columns pairs by checking ucd values
     * @param coord
     * @returns {Array}  [[pair_1_col_ra, pair_1_col_dec], ...., [pair_n_col_ra, pair_n_col_dec]]
     */
    getCenterColumnPairsOnUCD(coord = this.posCoord || UCDCoord.eq.key) {
        const centerColUCDs = has(posCol, coord) ? posCol[coord].ucd : null;
        const pairs = [];

        if (!centerColUCDs) {
            return pairs;
        }

        // get 'ra' column list and 'dec' column list
        const posPairs = centerColUCDs.reduce((prev, eqUcdPair) => {
            if (isArray(eqUcdPair) && eqUcdPair.length >= 2) {
                const colsRA = this.getTblColumnsOnUCD(eqUcdPair[0]);
                const colsDec = this.getTblColumnsOnUCD(eqUcdPair[1]);

                prev[0].push(...colsRA);
                prev[1].push(...colsDec);
            }
            return prev;
        }, [[], []]);


        const metaMainPair = posPairs.map((posCols, idx) => {
            const mainMetaCols = this.getColumnsWithUCDWord(posCols, mainMeta);
            if (!isEmpty(posCols) && isEmpty(mainMetaCols)) {
                alternateMainPos.find((oneAlt) => {
                    const altCols = this.getColumnsWithUCDWord(posCols, oneAlt[idx], ucdSyntaxMap.any);

                    mainMetaCols.push(...altCols);
                    return !isEmpty(altCols);
                });
            }
            return mainMetaCols;
        });

        if (metaMainPair[0].length || metaMainPair[1].length) {
            if (metaMainPair[0].length === metaMainPair[1].length) {
                for (let i = 0; i < metaMainPair[0].length; i++) {
                    pairs.push([metaMainPair[0][i], metaMainPair[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        } else if (posPairs[0].length > 0 && posPairs[1].length > 0) {
            // find first exact match, that is a double, if not found the try first exact match
            let basicPair = posPairs
                .map((cols, i) => cols
                    .find((c) => c.UCD === centerColUCDs[0][i] && (c.type==='double' || c.type==='float') ));
            if (!basicPair[0] || !basicPair[1]) {
                basicPair = posPairs
                    .map((cols, i) => cols
                        .find((c) => c.UCD === centerColUCDs[0][i]));
            }

            if (basicPair[0] && basicPair[1]) {
                pairs.push(basicPair);
            } else if (posPairs[0].length === posPairs[1].length) {
                // TODO: how do we separate positions from the related fields, like variance?
                for (let i = 0; i < posPairs[0].length; i++) {
                    pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        }

        return pairs;
    }

    getCenterColumnPairsOnUType(columnPairs) {
        const centerUTypes = centerColumnUTypesFromObsTap();

        if (isEmpty(centerUTypes)) return columnPairs;
        let pairs = [];

        /* filter out the column with unequal utype value */
        if (!isEmpty(columnPairs)) {
            pairs = columnPairs.filter((oneColPair) => {
                if ((!has(oneColPair[0], 'utype')) || (!has(oneColPair[1], 'utype')) ||
                    (oneColPair[0].utype.includes(centerUTypes[0]) && oneColPair[1].utype.includes(centerUTypes[1]))) {
                    return oneColPair;
                }
            });
        } else {   // check all table columns
            const posPairs = centerUTypes.map((posUtype) => {
                return this.getTblColumnsOnUType(posUtype);
            });

            if (posPairs[0].length === posPairs[1].length) {
                for (let i = 0; i < posPairs[0].length; i++) {
                    pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        }
        return pairs;

    }

    getCenterColumnPairOnName(columnPairs) {
        if (!isEmpty(columnPairs)) {
            return columnPairs.find((onePair) => {
                return (onePair[0].name.toLowerCase() === obsCorePosColumns[0]) &&
                    (onePair[1].name.toLowerCase() === obsCorePosColumns[1]);
            });
        } else {
            const cols = obsCorePosColumns.map((colName) => {
                return getColumn(this.tableModel, colName);
            });
            return (cols[0] && cols[1]) ? cols : [];
        }
    }


    /**
     *
     * @return {String}
     */
    getCenterColumnMetaEntry() {
        this.centerColumnsInfo = null;

        //note: CATALOG_COORD_COLS,POSITION_COORD_COLS are both deprecated and will removed in the future
        const {tableMeta} = this.tableModel || {};
        const {CATALOG_COORD_COLS, POSITION_COORD_COLS, CENTER_COLUMN} = MetaConst;
        if (!tableMeta) return undefined;
        return tableMeta[CENTER_COLUMN] || tableMeta[CATALOG_COORD_COLS] || tableMeta[POSITION_COORD_COLS];
    }

    /**
     * @returns {Boolean}
     */
    isCenterColumnMetaDefined() {
        return Boolean(this.getCenterColumnMetaEntry());
    }


    /**
     * get center columns pair by checking the table meta
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnMeta() {
        const cenData = this.getCenterColumnMetaEntry();
        if (!cenData) return undefined;

        const s = cenData.split(';');
        if (!s || s.length !== 3) {
            return this.centerColumnsInfo;
        }

        return this.setCenterColumnsInfo(s, CoordinateSys.parse(s[2]));
    }

    getImagePtColumnsOnMeta() {
        const cenData = getMetaEntry(this.tableModel, MetaConst.IMAGE_COLUMN);
        if (!cenData) return undefined;

        const s = cenData.split(';');
        if (!s || s.length !== 2) {
            return;
        }

        return {
            type: 'ImageCenterPt',
            xCol: s[0],
            yCol: s[1],
            xIdx: getColumnIdx(this.table, s[0]),
            yIdx: getColumnIdx(this.table, s[1]),
        };
    }


    /**
     * search center columns pair by checking UCD value
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnUCD() {
        this.centerColumnsInfo = null;

        const colPairs = this.getCenterColumnPairsOnUCD(UCDCoord.eq.key);

        if (colPairs && colPairs.length === 1) {
            return this.setCenterColumnsInfo(colPairs[0], posCol[UCDCoord.eq.key].coord);
        } else {
            this.centerColumnCandidatePairs = colPairs;
        }

        return this.centerColumnsInfo;
    }

    /**
     * search center column pairs based on existing candidate pairs or all table columns
     * @param candidatePairs
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnObsCoreUType(candidatePairs) {
        this.centerColumnsInfo = null;

        const colPairs = this.getCenterColumnPairsOnUType(candidatePairs);

        if (colPairs && colPairs.length === 1) {
            this.setCenterColumnsInfo(colPairs[0], posCol[UCDCoord.eq.key].coord);
        }
        this.centerColumnCandidatePairs = colPairs;

        return this.centerColumnsInfo;
    }

    /**
     * search center column pair by checking ObsCore columns on existing candidate pairs or all table columns
     * @param candidatePairs
     * @returns {null|CoordColsDescription}
     */
    getCenterColumnsOnObsCoreName(candidatePairs) {
        this.centerColumnsInfo = null;

        const leftMostCol = (isEmpty(candidatePairs))
            ? null : candidatePairs[0];

        const colPair = this.getCenterColumnPairOnName(candidatePairs);

        if (isArray(colPair) && colPair.length === 2) {
            return this.setCenterColumnsInfo(colPair, posCol[UCDCoord.eq.key].coord);
        } else {
            return leftMostCol ?
                this.setCenterColumnsInfo(leftMostCol, posCol[UCDCoord.eq.key].coord) :
                this.centerColumnsInfo;
        }
    }

    /**
     * search center columns pair by guessing the column name
     * @param {boolean} acceptArrayCol - if true then allow of a single column with an array entry for RA and Dec
     * @returns {null|CoordColsDescription}
     */
    guessCenterColumnsByName(acceptArrayCol) {
        this.centerColumnsInfo = undefined;

        const columns = getColumns(this.tableModel);

        const findColumn = (colName, regExp) => {
            return columns.find((c) => (c.name === colName || (regExp && regExp.test(c.name))));
        };

        const guess = (lon, lat, useReg = false) => {

            let lonCol;
            let latCol;
            if (useReg) {
                let reLon = new RegExp(`^[A-z]*[-_]?(${lon})[1-9]*$`);
                let reLat = new RegExp(`^[A-z]*[-_]?(${lat})[1-9]*$`);
                lonCol = findColumn(lon, reLon);
                latCol = findColumn(lat, reLat);
                if (!lonCol || !latCol) {
                    reLon = new RegExp(`^${lon}[-_].*$`);
                    reLat = new RegExp(`^${lat}[-_].*$`);
                    lonCol = findColumn(lon, reLon);
                    latCol = findColumn(lat, reLat);
                }
                if (lonCol && latCol) {
                    if (lonCol.name.replace(lon, '') !== latCol.name.replace(lat, '')) return undefined;
                }
            } else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            return (lonCol && latCol) ? this.setCenterColumnsInfo([lonCol, latCol]) : undefined;
        };

        const centerColumnInfo = (guess('ra', 'dec') || guess('lon', 'lat') || guess('ra', 'dec', true) || guess('lon', 'lat', true));
        if (centerColumnInfo) return centerColumnInfo;

        const c = getColumn(this.tableModel, 'coord_obs');
        if (acceptArrayCol && c?.arraySize &&
            (c?.type === 'double' || c?.type === 'float') ||
            (c?.utype?.toLowerCase().includes(SSA_COV_UTYPE) || c?.UCD?.toLowerCase().includes(POS_EQ_UCD) ) ) {
            return this.setCenterColumnsInfo([c, c]);
        }
        return undefined;
    }

    /**
     * find center columns as defined in some vo standard
     * @returns {null|CoordColsDescription}
     */
    getVODefinedCenterColumns() {
        return this.getCenterColumnsOnUCD() ||
            this.getCenterColumnsOnObsCoreUType(this.centerColumnCandidatePairs) ||
            this.getCenterColumnsOnObsCoreName(this.centerColumnCandidatePairs);
    }


    /**
     * return center position or catalog coordinate columns and the associate*d coordinate system
     * by checking table meta, UCD values, Utype, ObsCore column name and guessing.
     * @param {boolean} acceptArrayCol - if true then allow of a single column with an array entry for RA and Dec
     * @returns {null|CoordColsDescription}
     */
    getCenterColumns(acceptArrayCol = false) {

        if (this.isCenterColumnMetaDefined()) return this.getCenterColumnsOnMeta();

        return this.getVODefinedCenterColumns() ||
            (isEmpty(this.centerColumnCandidatePairs) && this.guessCenterColumnsByName(acceptArrayCol));
    }

    getRegionColumnOnUCD(cols) {
        this.regionColumnInfo = null;
        const columns = !isEmpty(cols) ? cols : this.columns;
        const ucds = get(getObsTabColEntry(S_REGION), 'ucd', '').split(';');

        const regionCols = ucds.reduce((prev, oneUcd) => {
            if (prev.length > 0) {
                prev = this.getColumnsWithUCDWord(prev, oneUcd);
            }
            return prev;
        }, columns);

        if (regionCols.length === 1) {
            this.setRegionColumnInfo(regionCols[0]);
        } else if (regionCols.length > 1) {
            if (!this.getRegionColumnOnObsCoreName(regionCols)) {
                this.setRegionColumnInfo(regionCols[0]);
            }
        }
        return this.regionColumnInfo;
    }

    getRegionColumnOnObsCoreUType(cols) {
        const columns = !isEmpty(cols) ? cols : this.columns;
        const obsUtype = get(getObsTabColEntry(S_REGION), 'utype', '');

        this.regionColumnInfo = null;

        const regionCols = (obsUtype) && !isEmpty(columns) && columns.filter((col) => {
            return (has(col, 'utype') && col.utype.includes(obsUtype));
        });

        if (regionCols.length === 1) {
            this.setRegionColumnInfo(regionCols[0]);
        } else if (regionCols.length > 1) {
            if (!this.getRegionColumnOnObsCoreName(regionCols)) {
                this.setRegionColumnInfo(regionCols[0]);
            }
        }

        return this.regionColumnInfo;
    }

    getRegionColumnOnObsCoreName(cols) {
        this.regionColumnInfo = null;
        const columns = !isEmpty(cols) ? cols : this.columns;

        const regionCol = !isEmpty(columns) && columns.find((oneCol) => oneCol.name.toLowerCase() === S_REGION);
        if (regionCol) {
            this.setRegionColumnInfo(regionCol);
        }
        return this.regionColumnInfo;
    }

    /**
     * return region column by checking column name or UCD values
     * @returns {null|RegionColDescription}
     */
    getVODefinedRegionColumn() {
        return this.getRegionColumnOnUCD() ||
            this.getRegionColumnOnObsCoreUType() ||
            this.getRegionColumnOnObsCoreName();
    }


    getRegionColumn() {
        return this.getVODefinedRegionColumn();
    }

    static newInstance(tableModel) {
        return new VoTableRecognizer(tableModel);
    }
}


function getLonLatIdx(tableModel, lonCol, latCol) {
    const lonIdx =  getColumnIdx(tableModel, lonCol);
    const latIdx =  getColumnIdx(tableModel, latCol);

    return (lonIdx >= 0 && latIdx >= 0) ? {lonIdx, latIdx} : null;
}

function centerColumnUTypesFromObsTap() {
    const obsTapColNames = OBSTAPCOLUMNS.map((col) => col[ColNameIdx]);

    const centerUTypes = obsCorePosColumns.map((posColName) => {
        const idx = obsTapColNames.indexOf(posColName);

        return (idx >= 0) ? OBSTAPCOLUMNS[idx][UtypeColIdx] : null;
    });

    return centerUTypes.findIndex((oneUtype) => !oneUtype) >= 0 ? null : centerUTypes;
}
