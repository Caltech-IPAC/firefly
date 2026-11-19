/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, has, isArray, isEmpty, isString} from 'lodash';
import {MetaConst} from '../data/MetaConst.js';
import {COL_TYPE, getColumn, getColumnIdx, getColumns, getMetaEntry, isOfType} from '../tables/TableUtil.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {
    alternateMainPos, ColNameIdx, mainMeta, obsCorePosColumns, OBSTAPCOLUMNS, POS_EQ_UCD, posCol, S_REGION,
    SSA_COV_UTYPE, UCDCoord, UCDSyntax, ucdSyntaxMap, UtypeColIdx
} from './VoConst.js';
import {findCsysFromUCD, getObsTabColEntry, isUCDWith} from './VoCoreUtils.js';


/**
 * table analyzer based on table model for catalog or image metadata
 */
export class VoTableRecognizer {
    constructor(tableModel, posCoord) {
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
     * A column tagged with extra words, such as 'pos.eq.ra;stat.mean', is still a position, unless the words make it
     * a different quantity: an error, a proper motion or an offset (isCoordColumn).
     * @param coord
     * @returns {Array}  [[pair_1_col_ra, pair_1_col_dec], ...., [pair_n_col_ra, pair_n_col_dec]]
     */
    getCenterColumnPairsOnUCD(coord = this.posCoord || UCDCoord.eq.key) {
        const centerColUCDs = has(posCol, coord) ? posCol[coord].ucd : null;
        const pairs = [];

        if (!centerColUCDs) {
            return pairs;
        }

        // get longitude column list and latitude column list
        const posPairs = centerColUCDs.reduce((prev, ucdPair) => {
            if (isArray(ucdPair) && ucdPair.length >= 2) {
                // getTblColumnsOnUCD finds the coordinate word anywhere in a UCD, so it also returns proper motions
                // such as 'pos.pm;pos.eq.ra'; isCoordColumn keeps only the positions
                const colsLon = this.getTblColumnsOnUCD(ucdPair[0]).filter((c) => isCoordColumn(c, ucdPair[0]));
                const colsLat = this.getTblColumnsOnUCD(ucdPair[1]).filter((c) => isCoordColumn(c, ucdPair[1]));

                prev[0].push(...colsLon);
                prev[1].push(...colsLat);
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
            } else if (metaMainPair[0].length + metaMainPair[1].length === 1) {
                // meta.main is on ra but not on dec (or vice versa).
                // pair them when only one column has a UCD starting with 'pos.eq.ra' and only one with 'pos.eq.dec'.
                const positionCols = posPairs
                    .map((cols, i) => cols.filter((c) => isUCDWith(c.UCD, centerColUCDs[0][i], UCDSyntax.primary)));
                if (positionCols[0].length === 1 && positionCols[1].length === 1) {
                    pairs.push([positionCols[0][0], positionCols[1][0]]);
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

        // the coordinate systems to search: only posCoord when the caller sets it, otherwise all three
        const coordSet = this.posCoord ? [UCDCoord[this.posCoord].key] :
            [UCDCoord.eq.key, UCDCoord.galactic.key, UCDCoord.ecliptic.key];

        // ra/dec columns are searched first: every feature can use them without converting,
        // so when a table tags both ra/dec and galactic or ecliptic columns, the ra/dec columns are used
        if (coordSet.includes(UCDCoord.eq.key)) {
            const colPairs = this.getCenterColumnPairsOnUCD(UCDCoord.eq.key);
            if (colPairs.length === 1) return this.setCenterColumnsInfo(colPairs[0], posCol[UCDCoord.eq.key].coord);
            // more than one ra/dec pair: no pair is picked here, the UType and ObsCore name checks pick one.
            if (colPairs.length > 1) {
                this.centerColumnCandidatePairs = colPairs;
                return this.centerColumnsInfo;
            }
            // no ra/dec pair: the galactic and ecliptic search below runs
        }

        // no ra/dec pair: use the galactic or ecliptic pair, when the table tags exactly one
        const pair = this.getSingleCenterColumnPairOnUCD(coordSet.filter((oneCoord) => oneCoord !== UCDCoord.eq.key));
        if (pair) return this.setCenterColumnsInfo(pair.columns, posCol[pair.coord].coord);

        // no galactic or ecliptic pair, or more than one: no pair is picked here, and the UType, ObsCore name and
        // column name checks search the whole table, as they do for a table with no position UCDs
        this.centerColumnCandidatePairs = [];
        return this.centerColumnsInfo;
    }

    /**
     * Find the table's one galactic or ecliptic column pair, by tag.
     * @param {Array.<string>} coordSet  the coordinate systems to check, e.g. ['galactic', 'ecliptic']
     * @returns {{columns: Array, coord: string}|undefined}  the two column objects and their
     *     coordinate system, e.g. {columns: [{name: 'glon', ...}, {name: 'glat', ...}], coord: 'galactic'};
     *     undefined when the table has no such pair or more than one
     */
    getSingleCenterColumnPairOnUCD(coordSet) {
        // no text columns: Firefly reads text positions like '12h30m00s' only as ra/dec
        const taggedCols = this.columns.filter((c) => c.UCD && !isOfType(c.type, COL_TYPE.TEXT));
        const pairs = coordSet
            .map((coord) => ({
                coord,
                // plain tags only: 'pos.galactic.lon;stat.mean' is left to the later checks, which may find ra/dec.
                lonLatCols: posCol[coord].ucd[0].map((ucdWord) =>
                    taggedCols.filter((c) => isPlainCoordColumn(c, ucdWord)))
            }))
            .filter(({lonLatCols}) => lonLatCols[0].length > 0 && lonLatCols[1].length > 0);

        if (pairs.length !== 1) return undefined;
        const {coord, lonLatCols} = pairs[0];
        if (lonLatCols[0].length !== 1 || lonLatCols[1].length !== 1) return undefined;
        return {columns: [lonLatCols[0][0], lonLatCols[1][0]], coord};
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
            // when the UCDs give no coordinate system, ObsCore position columns are ICRS (EQ_J2000)
            this.setCenterColumnsInfo(colPairs[0], findCsysForCoordColumns(...colPairs[0]) ?? CoordinateSys.EQ_J2000);
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
        const centerPair = (isArray(colPair) && colPair.length === 2) ? colPair : leftMostCol;
        if (!centerPair) return this.centerColumnsInfo;

        // when the UCDs give no coordinate system, ObsCore position columns are ICRS (EQ_J2000)
        return this.setCenterColumnsInfo(centerPair, findCsysForCoordColumns(...centerPair) ?? CoordinateSys.EQ_J2000);
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
                let reLon = new RegExp(`^[A-z]*[-_]?(${lon})[1-9]*$`, 'i');
                let reLat = new RegExp(`^[A-z]*[-_]?(${lat})[1-9]*$`, 'i');
                lonCol = findColumn(lon, reLon);
                latCol = findColumn(lat, reLat);
                if (!lonCol || !latCol) {
                    reLon = new RegExp(`^${lon}[-_].*$`, 'i');
                    reLat = new RegExp(`^${lat}[-_].*$`, 'i');
                    lonCol = findColumn(lon, reLon);
                    latCol = findColumn(lat, reLat);
                }
                if (lonCol && latCol) {
                    const testLon= lonCol.name.toLowerCase();
                    const testLat= latCol.name.toLowerCase();
                    if (testLon.replace(lon.toLowerCase(), '') !== testLat.replace(lat.toLowerCase(), '')) return undefined;
                }
            } else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            if (!lonCol || !latCol) return undefined;
            // the UCDs give the coordinate system; when they give none, the column name may (findCsysFromColumnName)
            const csys = findCsysForCoordColumns(lonCol, latCol) ?? findCsysFromColumnName(lonCol.name, lon);
            // isPositionPair rejects columns that cannot hold the position in csys, such as an offset
            // ('arith.diff;pos.angDistance') or a proper motion ('pos.pm;pos.galactic.lon')
            if (!csys || !isPositionPair(lonCol, latCol, csys)) return undefined;
            return this.setCenterColumnsInfo([lonCol, latCol], csys);
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

/**
 * UCD1 ra and dec words (UCD1 is the UCD format before UCD1+) and the UCD1+ words they stand for.
 * IVOA UCD 1.1, appendix A: the UCD1 POS_EQ_DEC_MAIN is pos.eq.dec;meta.main in UCD1+. VOTable 1.5 still allows UCD1.
 * POS_EQ_RA_OFF and POS_EQ_DEC_OFF are offsets from a position, so they are not listed.
 * @see {@link https://www.ivoa.net/documents/REC/UCD/UCD-20050812.html#ToC22}
 * @see {@link https://www.ivoa.net/documents/VOTable/20250116/REC-VOTable-1.5.html#sec:ucd}
 */
const UCD1_COORD_WORDS = {
    pos_eq_ra_main: 'pos.eq.ra;meta.main', pos_eq_ra: 'pos.eq.ra', pos_eq_ra_other: 'pos.eq.ra',
    pos_eq_dec_main: 'pos.eq.dec;meta.main', pos_eq_dec: 'pos.eq.dec', pos_eq_dec_other: 'pos.eq.dec',
};

/**
 * Words that, after the coordinate word, make the column hold a different quantity. IVOA UCD 1.1 section 3.4 defines
 * arith.diff and arith.ratio as the difference or ratio of two such values (an offset, for a position). The rest are
 * errors, proper motions, Sun-centered positions and orbits that archives write after the coordinate word, such as
 * 'pos.eq.ra;stat.error' or 'pos.ecliptic.lon;pos.heliocentric'. Each entry also covers the words under it.
 * Qualifiers that keep the position, such as meta.main, stat.mean, obs.field or a band (em.*), are not listed, and
 * neither are stat.min and stat.max: EPN-TAP tables (VizieR B/planets/epn_core) give their only ra/dec that way.
 */
const NOT_POSITION_QUALIFIERS = ['arith', 'stat.error', 'stat.stdev', 'pos.pm', 'pos.heliocentric', 'src.orbital'];

/**
 * The words of a UCD, read as the IVOA UCD 1.1 standard (section 2) defines them: separated by semicolons,
 * case-insensitive, with no spaces, and with the optional ivoa: namespace removed. UCD1 coordinate words become their
 * UCD1+ words (UCD1_COORD_WORDS).
 * @param {string} [ucd]
 * @returns {Array.<string>} the words of the UCD, lowercase
 */
function getUCDWords(ucd) {
    return (ucd ?? '').split(';')
        .map((word) => word.replace(/\s/g, '').toLowerCase().replace(/^ivoa:/, ''))
        .flatMap((word) => (UCD1_COORD_WORDS[word] ?? word).split(';'))
        .filter(Boolean);
}

/**
 * Split a UCD into its primary word, which says what the column holds, and the words that qualify it (IVOA UCD 1.1
 * section 3.3). meta.main only marks the main column among several of the same kind, so it is left out; that also
 * reads 'meta.main;pos.eq.ra', a UCD written in the wrong order, as pos.eq.ra.
 * @param {string} [ucd]
 * @returns {{primary: (string|undefined), qualifiers: Array.<string>}}
 */
function splitUCD(ucd) {
    const [primary, ...qualifiers] = getUCDWords(ucd).filter((word) => word !== mainMeta);
    return {primary, qualifiers};
}

/**
 * @param {string} word  a UCD word, lowercase
 * @returns {boolean} true if the word, after the coordinate word, makes the column a different quantity
 */
function isNotPositionQualifier(word) {
    return NOT_POSITION_QUALIFIERS.some((listedWord) => word === listedWord || word.startsWith(listedWord + '.'));
}

/**
 * Check if the column holds the coordinate ucdWord names: its UCD's primary word is ucdWord, and no qualifier makes it
 * a different quantity (NOT_POSITION_QUALIFIERS).
 * e.g. 'pos.eq.ra;meta.main' and 'pos.galactic.lon;stat.mean' pass; 'pos.pm;pos.eq.ra' (a proper motion),
 * 'stat.error;pos.eq.ra' (an error) and 'pos.eq.ra;arith.diff' (an offset) fail.
 * @param {TableColumn} col
 * @param {string} ucdWord  the longitude or latitude UCD word, such as 'pos.eq.ra' or 'POS_EQ_RA_MAIN'
 * @returns {boolean}
 */
function isCoordColumn(col, ucdWord) {
    const {primary, qualifiers} = splitUCD(col.UCD);
    return primary === splitUCD(ucdWord).primary && !qualifiers.some(isNotPositionQualifier);
}

/**
 * Check if the column's UCD is the coordinate word ucdWord with no qualifier but meta.main.
 * e.g. 'pos.galactic.lon;meta.main' passes; 'pos.pm;pos.galactic.lon' and 'pos.galactic.lon;stat.mean' fail.
 * @param {TableColumn} col
 * @param {string} ucdWord  the longitude or latitude UCD word, such as 'pos.galactic.lon'
 * @returns {boolean}
 */
function isPlainCoordColumn(col, ucdWord) {
    const {primary, qualifiers} = splitUCD(col.UCD);
    return primary === splitUCD(ucdWord).primary && qualifiers.length === 0;
}

/**
 * Check if the two columns a name guess matched can hold the position in csys. Each passes when its UCD says nothing
 * about the quantity the column holds, or says it is that coordinate itself (isCoordColumn). A UCD says nothing when
 * it is missing or blank, or has only meta words: GAVO's rome.time_series tags its ra and dec 'meta.id;meta.main'.
 * A text column passes only for equatorial: a text column's cells need not be numbers, and makeWorldPt parses
 * non-numeric text only as equatorial sexagesimal.
 * @param {TableColumn} lonCol
 * @param {TableColumn} latCol
 * @param {CoordinateSys} csys  a coordinate system of posCol
 * @returns {boolean}
 */
function isPositionPair(lonCol, latCol, csys) {
    const [lonWord, latWord] = Object.values(posCol).find((oneEntry) => oneEntry.coord === csys).ucd[0];
    const isEquatorial = csys === CoordinateSys.EQ_J2000;
    const isPositionColumn = (col, ucdWord) => {
        if (!isEquatorial && isOfType(col.type, COL_TYPE.TEXT)) return false;
        if (getUCDWords(col.UCD).every((word) => word.startsWith('meta.'))) return true;
        return isCoordColumn(col, ucdWord);
    };
    return isPositionColumn(lonCol, lonWord) && isPositionColumn(latCol, latWord);
}

/**
 * The coordinate system a column name gives, for a pair the name guess matched and whose UCDs give none.
 * A g or gal before lon means galactic (glon, GLON, _Glon, gal_lon), and an e or ecl means ecliptic (elon, ELON,
 * ecl_lon). An sg before lon gives none: sglon is supergalactic. Any other lon, such as lon, clon (IRSA's ra and dec
 * as text) or obj_lon, is equatorial, Firefly's long-standing default, and so is the name ra.
 * @param {string} lonColName  the name of the longitude column
 * @param {string} lonWord  the longitude word the name guess matched, 'ra' or 'lon'
 * @returns {CoordinateSys|undefined}
 */
function findCsysFromColumnName(lonColName, lonWord) {
    if (lonWord === 'ra') return CoordinateSys.EQ_J2000;
    const prefix = lonColName.toLowerCase().split(lonWord)[0].replace(/[-_]/g, '');
    if (prefix === 'g' || prefix === 'gal') return CoordinateSys.GALACTIC;
    if (prefix === 'e' || prefix === 'ecl') return CoordinateSys.ECL_J2000;
    if (prefix === 'sg') return undefined;
    return CoordinateSys.EQ_J2000;
}

function findCsysForCoordColumns(lonCol, latCol) {
    if (!lonCol || !latCol) return undefined;
    const lonCsys= findCsysFromUCD(lonCol.UCD);
    const latCsys= findCsysFromUCD(latCol.UCD);
    return (lonCsys && lonCsys===latCsys) ? lonCsys : undefined;
}