/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, has, isArray, isEmpty, isObject, isString} from 'lodash';
import Enum from 'enum';
import {getColumn, getColumnIdx, getColumnValues, getColumns, getTblById} from '../tables/TableUtil.js';
import {getCornersColumns} from '../tables/TableInfoUtil.js';
import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';


export const UCDCoord = new Enum(['eq', 'ecliptic', 'galactic']);

//const obsPrefix = 'obscore:';
const ColNameIdx = 0;
//const UCD = 1;
const UtypeColIdx = 2;
const mainMeta = 'meta.main';
const obsCorePosColumns = ['s_ra', 's_dec'];

const OBSTAPCOLUMNS = [
    ['dataproduct_type',  'meta.id',                    'ObsDataset.dataProductType'],
    ['calib_level',       'meta.code;obs.calib',        'ObsDataset.calibLevel'],
    ['obs_collection',    'meta.id',                    'DataID.collection'],
    ['obs_id',            'meta.id',                    'DataID.observationID'],
    ['obs_publisher_did', 'meta.ref.uri;meta.curation', 'Curation.publisherDID'],
    ['access_url',        'meta.ref.url',               'Access.reference'],
    ['access_format',     'meta.code.mime',             'Access.format'],
    ['access_estsize',    'phys.size;meta.file',        'Access.size'],
    ['target_name',       'meta.id;src',                'Target.name'],
    ['s_ra',              'pos.eq.ra',                  'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1'],
    ['s_dec',             'pos.eq.dec',                 'Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2'],
    ['s_fov',             'phys.angSize;instr.fov',     'Char.SpatialAxis.Coverage.Bounds.Extent.diameter'],
    ['s_region',          'pos.outline;obs.field',      'Char.SpatialAxis.Coverage.Support.Area'],
    ['s_resolution',      'pos.angResolution',          'Char.SpatialAxis.Resolution.Refval.value'],
    ['s_xel1',            'meta.number',                'Char.SpatialAxis.numBins1'],
    ['s_xel2',            'meta.number',                'Char.SpatialAxis.numBins2'],
    ['t_min',             'time.start;obs.exposure',    'Char.TimeAxis.Coverage.Bounds.Limits.StartTime'],
    ['t_max',             'time.end;obs.exposure',      'Char.TimeAxis.Coverage.Bounds.Limits.StopTime'],
    ['t_exptime',         'time.duration;obs.exposure', 'Char.TimeAxis.Coverage.Support.Extent'],
    ['t_resolution',      'time.resolution',            'Char.TimeAxis.Resolution.Refval.value'],
    ['t_xel',             'meta.number',                'Char.TimeAxis.numBins'],
    ['em_min',            'em.wl;stat.min',             'Char.SpectralAxis.Coverage.Bounds.Limits.LoLimit'],
    ['em_max',            'imit em.wl;stat.max',        'Char.SpectralAxis.Coverage.Bounds.Limits.HiLimit'],
    ['em_res_power',      'spect.resolution',           'Char.SpectralAxis.Resolution.ResolPower.refVal'],
    ['em_xel',            'Char.SpectralAxis.numBins',  'meta.number'],
    ['o_ucd',             'meta.ucd',                   'Char.ObservableAxis.ucd'],
    ['pol_states',        'meta.code;phys.polarization','Char.PolarizationAxis.stateList'],
    ['pol_xel',           'meta.number',                'Char.PolarizationAxis.numBins'],
    ['facility_name',     'meta.id;instr.tel',          'Provenance.ObsConfig.Facility.name'],
    ['instrument_name',   'meta.id;instr',              'Provenance.ObsConfig.Instrument.name']
];

const alternateMainPos = [['POS_EQ_RA_MAIN', 'POS_EQ_DEC_MAIN']];
export const posCol = {[UCDCoord.eq.key]: {ucd: [['pos.eq.ra', 'pos.eq.dec'],...alternateMainPos],
                                    coord: CoordinateSys.EQ_J2000, adqlCoord: 'ICRS'},
    [UCDCoord.ecliptic.key]: {ucd: [['pos.ecliptic.lon', 'pos.ecliptic.lat']],
                              coord: CoordinateSys.ECL_J2000, adqlCoord: 'ECLIPTIC'},
    [UCDCoord.galactic.key]: {ucd: [['pos.galactic.lon', 'pos.galactic.lat']],
                              coord: CoordinateSys.GALACTIC, adqlCoord: 'GALATIC'}};


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

const UCDSyntax = new Enum(['primary', 'secondary', 'any'], {ignoreCase: true});
const ucdSyntaxMap = {
            'pos.eq.ra':  UCDSyntax.any,
            'pos.eq.dec': UCDSyntax.any,
            'meta.main':  UCDSyntax.secondary};


/**
 * check if ucd value contains the searched ucd word at the right position
 * @param ucdValue
 * @param ucdWord
 * @param syntaxCode 'P': only first word, 'S': only secondary, 'Q' either first or secondary
 */
function isUCDWith(ucdValue, ucdWord, syntaxCode = UCDSyntax.any) {
    const atoms = ucdValue.split(';');
    const idx = atoms.findIndex((atom) => {
        return atom.toLowerCase() === ucdWord.toLowerCase();
    });

    return (syntaxCode === UCDSyntax.primary && idx === 0) ||
           (syntaxCode === UCDSyntax.secondary && idx >= 1) ||
           (syntaxCode === UCDSyntax.any && idx >= 0);
}

/**
 * table analyzer based on table model for catalog or image metadata
 */
class TableRecognizer {
    constructor(tableModel, posCoord='eq') {
        this.tableModel = tableModel;
        this.columns = get(tableModel, ['tableData', 'columns'], []);
        this.obsCoreInfo = {isChecked: false, isObsCoreTable: false};
        this.posCoord = posCoord;
        this.centerColumnsInfo = null;
        this.centerColumnCandidatePairs = null;
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
     * get columns containing ucd word
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
        const centerColUCDs = has(posCol, coord ) ? posCol[coord].ucd : null;
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
        } else if (posPairs[0].length === posPairs[1].length) {
            for (let i = 0; i < posPairs[0].length; i++) {
                pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
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

        const {tableMeta} = this.tableModel || {};

        if (!tableMeta ||
            (!tableMeta[MetaConst.CATALOG_COORD_COLS] && !tableMeta[MetaConst.CENTER_COLUMN])) {
            return undefined;
        }
        return tableMeta[MetaConst.CENTER_COLUMN] || tableMeta[MetaConst.CATALOG_COORD_COLS];
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
        const cenData= this.getCenterColumnMetaEntry();
        if (!cenData) return undefined;

        const s= cenData.split(';');
        if (!s || s.length !== 3) {
            return this.centerColumnsInfo;
        }

        return this.setCenterColumnsInfo(s, CoordinateSys.parse(s[2]));
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
            return leftMostCol?
                   this.setCenterColumnsInfo(leftMostCol, posCol[UCDCoord.eq.key].coord) :
                   this.centerColumnsInfo;
        }
    }

    /**
     * search center columns pair by guessing the column name
     * @returns {null|CoordColsDescription}
     */
    guessCenterColumnsByName() {
        this.centerColumnsInfo = undefined;

        const columns= getColumns(this.tableModel);

        const findColumn = (colName, regExp) => {
            return columns.find((c) => (c.name === colName || (regExp && regExp.test(c.name))) );
        };

        const guess = (lon, lat, useReg=false) => {

            let lonCol;
            let latCol;
            if (useReg) {
                const reLon= new RegExp(`^[A-z]?[-_]?(${lon})[1-9]*$`);
                const reLat= new RegExp(`^[A-z]?[-_]?(${lat})[1-9]*$`);
                lonCol = findColumn(lon,reLon);
                latCol = findColumn(lat,reLat);
            }
            else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            return (lonCol && latCol) ? this.setCenterColumnsInfo([lonCol, latCol]) : undefined;
        };

        return (guess('ra','dec') || guess('lon', 'lat') || guess('ra','dec',true) || guess('lon', 'lat',true));
    }


    /**
     * find center columns as defined in some vo standard
     * @returns {null|CoordColsDescription}
     */
    getVODefinedCenterColumns() {
        return  this.getCenterColumnsOnUCD() ||
            this.getCenterColumnsOnObsCoreUType(this.centerColumnCandidatePairs) ||
            this.getCenterColumnsOnObsCoreName(this.centerColumnCandidatePairs);
    }




    /**
     * return center position or catalog coordinate columns and the associate*d coordinate system
     * by checking table meta, UCD values, Utype, ObsCore column name and guessing.
     * @returns {null|CoordColsDescription}
     */
    getCenterColumns() {

        if (this.isCenterColumnMetaDefined()) return this.getCenterColumnsOnMeta();
        
        return  this.getVODefinedCenterColumns() ||
                (isEmpty(this.centerColumnCandidatePairs) && this.guessCenterColumnsByName());
    }




    static newInstance(tableModel) {
        return new TableRecognizer(tableModel);
    }
}

/**
 * find the center column base on the table model of catalog or image metadata
 * Investigate table meta data a return a CoordColsDescription for two columns that represent and object in the row
 * @param table
 * @return {CoordColsDescription|null}
 */
export function findTableCenterColumns(table) {
   const tblRecog = get(table, ['tableData', 'columns']) && TableRecognizer.newInstance(table);
   return tblRecog && tblRecog.getCenterColumns();
}

/**
 * Given a TableModel or a table id return a table model
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {TableModel}
 */
function getTableModel(tableOrId) {
    if (isString(tableOrId)) return getTblById(tableOrId);  // was passed a table Id
    if (isObject(tableOrId)) return tableOrId;
    return undefined;
}

/**
 * table analyzer based on the table model for columns which contains column_name & ucd columns
 */
class ColumnRecognizer {
    constructor(columnsModel, posCoord = 'eq') {
        this.columnsModel = columnsModel;
        this.ucds = getColumnValues(columnsModel, 'ucd');
        this.column_names = getColumnValues(columnsModel, 'column_name');
        this.centerColumnsInfo = null;
        this.posCoord = posCoord;
    }


    setCenterColumnsInfo(colPair, csys = CoordinateSys.EQ_J2000) {
        this.centerColumnsInfo = {
            lonCol: colPair[0],
            latCol: colPair[1],
            csys
        };

        return this.centerColumnsInfo;
    }

    getColumnsWithUCDWord(cols, ucdWord) {
        if (isEmpty(cols)) return [];

        return cols.filter((oneCol) => {
            return has(oneCol, 'ucd') && isUCDWith(oneCol.ucd, ucdWord, get(ucdSyntaxMap, ucdWord));
        });
    }

    getCenterColumnPairsOnUCD(coord) {
        const centerColUCDs = has(posCol, coord ) ? posCol[coord].ucd : null;
        const pairs = [];

        if (!centerColUCDs) {
            return pairs;
        }

        // get 'ra' column list and 'dec' column list
        // output in form of [ <ra column array>, <dec column array> ] and each column is like {ucd: column_name: }
        const posPairs = centerColUCDs.reduce((prev, eqUcdPair) => {
            if (isArray(eqUcdPair) && eqUcdPair.length >= 2) {
                const colsRA = this.ucds.reduce((p, ucd, i) => {
                    if (ucd.includes(eqUcdPair[0])) {
                        p.push({ucd, column_name: this.column_names[i]});
                    }
                    return p;
                }, []);
                const colsDec = this.ucds.reduce((p, ucd, i) => {
                    if (ucd.includes(eqUcdPair[1])) {
                        p.push({ucd, column_name: this.column_names[i]});
                    }
                    return p;
                }, []);

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

        if (metaMainPair[0].length || metaMainPair[1].length) {  // get the column with ucd containing meta.main
            if (metaMainPair[0].length === metaMainPair[1].length) {
                for (let i = 0; i < metaMainPair[0].length; i++) {
                    pairs.push([metaMainPair[0][i], metaMainPair[1][i]]);    //TODO: need rules to match the rest pair
                }
            }
        } else if (posPairs[0].length === posPairs[1].length) {
            for (let i = 0; i < posPairs[0].length; i++) {
                pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the rest pair
            }
        }

        return pairs;
    }

    getCenterColumnsOnUCD() {
        let colPairs;
        const coordSet = this.posCoord ? [UCDCoord[this.posCoord].key] :
                         [UCDCoord.eq.key, UCDCoord.galactic.key, UCDCoord.ecliptic.key];

        coordSet.find((oneCoord) => {
            colPairs = this.getCenterColumnPairsOnUCD(oneCoord);
            if (colPairs && colPairs.length >= 1) {
                this.setCenterColumnsInfo(colPairs[0], posCol[oneCoord].coord);  // get the first pair
                return true;
            } else {
                return false;
            }
        });
        return this.centerColumnsInfo;
    }

    guessCenterColumnsByName() {
        this.centerColumnsInfo = null;

        const findColumn = (colName, regExp) => {
            let col;
            this.column_names.find((name, i) => {
                if (name === colName || (regExp && regExp.test(name))) {
                    col = {column_name: name, ucd: this.ucds[i]};
                    return true;
                } else {
                    return false;
                }
            });
            return col;
        };


        const guess = (lon, lat, useReg=false) => {

            let lonCol;
            let latCol;
            if (useReg) {
                const reLon= new RegExp(`^[A-z]?[-_]?(${lon})[1-9]*$`);
                const reLat= new RegExp(`^[A-z]?[-_]?(${lat})[1-9]*$`);
                lonCol = findColumn(lon,reLon);
                latCol = findColumn(lat,reLat);
            }
            else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            return (lonCol && latCol) ? this.setCenterColumnsInfo([lonCol, latCol]) : this.centerColumnsInfo;
        };
        return (guess('ra','dec') || guess('lon', 'lat') || guess('ra','dec',true) || guess('lon', 'lat',true));
    }


    getCenterColumns() {
        return this.getCenterColumnsOnUCD()||
               this.guessCenterColumnsByName();
    }

    static newInstance(tableModel) {
        return new ColumnRecognizer(tableModel);
    }
}

/**
 * find the center columns based on the columns table model
 * @param columnsModel
 * @returns {*|{lonCol: {ucd, column_name}, latCol: {ucd, column_name}, csys}|*}
 */
export function findCenterColumnsByColumnsModel(columnsModel) {
    const colRecog = columnsModel && get(columnsModel, ['tableData', 'columns']) && ColumnRecognizer.newInstance(columnsModel);

    return colRecog && colRecog.getCenterColumns();
}


/**
 * Test to see it this is a catalog. A catalog must have one of the following:
 *  - CatalogOverlayType meta data entry defined and not equal to 'FALSE' and we must be able to find the columns
 *                            either by meta data or by guessing
 *  - We find the columns by some vo standard
 *
 *  Note- if the CatalogOverlayType meta toUpperCase === 'FALSE' then we will treat it as not a catalog no matter how the
 *  vo columns might be defined.
 *
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} True if the table is a catalog
 * @see MetaConst.CATALOG_OVERLAY_TYPE
 */
export function isCatalog(tableOrId) {

    const table= getTableModel(tableOrId);

    if (!table) return false;
    const {tableMeta, tableData}= table;
    if (!get(tableData, 'columns') || !tableMeta) return false;

    if (isString(tableMeta[MetaConst.CATALOG_OVERLAY_TYPE])) {
        if (tableMeta[MetaConst.CATALOG_OVERLAY_TYPE].toUpperCase()==='FALSE') return false;
        return Boolean(TableRecognizer.newInstance(table).getCenterColumns());
    }
    else {
        return Boolean(TableRecognizer.newInstance(table).getVODefinedCenterColumns());
    }
}

/**
 * @summary check if there is center column or corner columns defined, if so this table has coverage information
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @returns {boolean} True if there  is coverage data in this table
 */
export function hasCoverageData(tableOrId) {
    const table= getTableModel(tableOrId);
    if (!table) return false;
    if (!table.totalRows) return false;
    return !isEmpty(findTableCenterColumns(table)) || !isEmpty(getCornersColumns(table));
}


/**
 * Guess if this table contains image meta data. It contains image meta data if IMAGE_SOURCE_ID is defined
 * or a DATA_SOURCE column name is defined
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {boolean} true if there is image meta data
 * @see MetaConst.DATA_SOURCE
 * @see MetaConst.IMAGE_SOURCE_ID
 */
export function isMetaDataTable(tableOrId) {
    const table= getTableModel(tableOrId);
    const dataSourceUpper = MetaConst.DATA_SOURCE.toUpperCase();
    if (isEmpty(table)) return false;
    const {tableMeta, totalRows} = table;
    if (!tableMeta || !totalRows) return false;

    const hasDsCol = Boolean(Object.keys(tableMeta).find((key) => key.toUpperCase() === dataSourceUpper));

    return Boolean(tableMeta[MetaConst.IMAGE_SOURCE_ID] || tableMeta[MetaConst.DATASET_CONVERTER] || hasDsCol);
}



const DEFAULT_TNAME_OPTIONS = [
    'name',         // generic
    'pscname',      // IRAS
    'target',       //  our own table output
    'designation',  // 2MASS
    'starid'        // PCRS
];

// moved from java, if we decide to use it this is what we had before
export const findTargetName = (columns) => columns.find( (c) => DEFAULT_TNAME_OPTIONS.includes(c));

/**
 * @global
 * @public
 * @typedef {Object} CoordColsDescription
 *
 * @summary And object that describes a pairs of columns in a table that makes up a coordinate
 *
 * @prop {string} lonCol - name of the longitudinal column
 * @prop {string} latCol - name of the latitudinal column
 * @prop {number} lonIdx - column index for the longitudinal column
 * @prop {number} latIdx - column index for the latitudinal column
 * @prop {CoordinateSys} csys - the coordinate system to use
 */


