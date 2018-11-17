/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, has, isEmpty} from 'lodash';
import Enum from 'enum';
import {getColumn, getColumnIdx} from './../tables/TableUtil.js';
import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';

const UCDCoord = new Enum(['eq', 'ecliptic', 'galactic']);

//const pObs = 'obscore:';
const ObsTapColName = 0;
//const UCD = 1;
//const Utype = 2;
const mainMeta = 'meta.main';
const posCol = {[UCDCoord.eq.key]: {ucd: ['pos.eq.ra', 'pos.eq.dec'],
                                    coord: CoordinateSys.EQ_J2000},
                [UCDCoord.ecliptic.key]: {ucd: ['pos.ecliptic.lon', 'pos.ecliptic.lat'],
                                          coord: CoordinateSys.ECL_J2000},
                [UCDCoord.galactic.key]: {ucd: ['pos.galactic.lon', 'pos.galactic.lat']},
                                          coord: CoordinateSys.GALACTIC};



const obsCorePosColumns = ['s_ra', 's_dec'];

const ObsTapMandColumns = [
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

function getLonLatIdx(tableModel, lonCol, latCol) {
    const lonIdx =  getColumnIdx(tableModel, lonCol);
    const latIdx =  getColumnIdx(tableModel, latCol);

    return (lonIdx >= 0 && latIdx >= 0) ? {lonIdx, latIdx} : null;
}


class TableRecognizer {
    constructor(tableModel, posCoord='eq') {
        this.tableModel = tableModel;
        this.columns = get(tableModel, ['tableData', 'columns'], []);
        this.obsCoreInfo = {isChecked: false, isObsCoreTable: false};
        this.posCoord = posCoord;
        this.centerColumnsInfo = null;
    }

    isObsCoreTable() {
        if (this.obsCoreInfo.isChecked) {
            return this.obsCoreInfo.isObsCoreTable;
        }
        const allColNames = this.columns.map((oneCol) => oneCol.name);

        const nonExistCol = ObsTapMandColumns
                            .map((oneColumn) => (oneColumn[ObsTapColName]))
                            .some((oneName) => {
                                return !allColNames.includes(oneName);
                            });

        this.obsCoreInfo.isChecked = true;
        this.obsCoreInfo.isObsCoreTable = !nonExistCol;

        return this.obsCoreInfo.isObsCoreTable;
    }

    getCenterColumnsInfo() {
        return isEmpty(this.centerColumnsInfo) ? null : this.centerColumnsInfo;
    }

    setCenterColumnsInfo(lonCol, latCol, lonIdx, latIdx, csys = CoordinateSys.EQ_J2000) {
        this.centerColumnsInfo = {
            lonCol,
            latCol,
            lonIdx,
            latIdx,
            csys
        };
    }

    /**
     * get columns containing the same ucd value
     * @param ucd
     * @returns {Array}
     */
    getTblColumnsOnUCD(ucd) {
        return this.columns.reduce((prev, oneCol) => {
            if (oneCol.UCD && oneCol.UCD.includes(ucd)) {
                prev.push(oneCol);
            }
            return prev;
        }, []);
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
        let posPairs = centerColUCDs.reduce((prev, eqUcd) => {
            const cols = this.getTblColumnsOnUCD(eqUcd);

            prev.push(cols);
            return prev;
        }, []);

        // no pair
        if (posPairs[0].length === 0 || posPairs[1].length === 0) {
            return pairs;
        }

        // only one pair
        if (posPairs[0].length === 1 && posPairs[1].length === 1) {
            pairs.push([posPairs[0][0], posPairs[1][0]]);
        } else {
            const mainColIdx = [];

            // find the leftmost pair with 'main.meta'
            for (let n = 0; n < 2; n++) {
                mainColIdx.push(posPairs[n].findIndex((col) => {
                    return (col.UCD && col.UCD === `${centerColUCDs[n]};${mainMeta}`);
                }));
            }

            if (mainColIdx[0] >= 0 && mainColIdx[1] >= 0) {
                pairs.push([posPairs[0][mainColIdx[0]], posPairs[1][mainColIdx[1]]]);
                posPairs = [posPairs[0].splice(mainColIdx[0], 1), posPairs[1].splice(mainColIdx[1], 1)];
            }
            if (posPairs[0].length === posPairs[1].length) {
                for (let i = 0; i < posPairs[0].length; i++) {
                    pairs.push([posPairs[0][i], posPairs[1][i]]);    //TODO: need rules to match the pair
                }
            }
        }
        return pairs;
    }

    /**
     * get center columns pair by checking UCD value
     * @returns {null|{lonCol: *, latCol: *, lonIdx: *, latIdx: *, csys: *}|*}
     */
    getCenterColumnsOnUCD() {
        let   coordsys = null;
        let   colPair;

        if (this.centerColumnsInfo) {
            return this.centerColumnsInfo;
        }
        const ucdCoords = [UCDCoord.eq];
        ucdCoords.find((oneCoord) => {
                const colPairs = this.getCenterColumnPairsOnUCD(oneCoord.key);

                // get the first pair from the found pairs
                if (!isEmpty(colPairs)) {
                    colPair = colPairs[0];
                    coordsys = posCol[oneCoord.key].coord;
                }

                return colPair;
            });

        if (colPair) {
            const idxs = getLonLatIdx(this.tableModel, colPair[0].name, colPair[1].name);

            if (idxs) {
                  this.setCenterColumnsInfo(colPair[0].name, colPair[1].name, idxs.lonIdx, idxs.latIdx, coordsys);
            }
        }

        return this.centerColumnsInfo;

    }

    /**
     * get center column pair by checking ObsCore columns
     * @returns {null|{lonCol: *, latCol: *, lonIdx: *, latIdx: *, csys: *}|*}
     */
    getCenterColumnsOnObsCore() {
        if (this.centerColumnsInfo) {
            return this.centerColumnsInfo;
        }
        if (this.isObsCoreTable()) {
            const cols = obsCorePosColumns.map((colName) => {
                return getColumn(this.tableModel, colName);
            });

            if (cols[0] && cols[1]) {
                const idxs = getLonLatIdx(this.tableModel, cols[0].name, cols[1].name);

                if (idxs) {
                    this.setCenterColumnsInfo(cols[0].name, cols[1].name, idxs.lonIdx, idxs.latIdx,
                                             posCol[UCDCoord.eq.key].coord);
                }
            }
        }

        return this.centerColumnsInfo;
    }

    /**
     * get center columns pair by checking the table meta
     * @returns {null|{lonCol: *, latCol: *, lonIdx: *, latIdx: *, csys: *}|*}
     */
    getCenterColumnsOnMeta() {
        if (this.centerColumnsInfo) {
            return this.centerColumnsInfo;
        }

        const {tableMeta} = this.tableModel || {};

        if (!tableMeta ||
            (!tableMeta[MetaConst.CATALOG_COORD_COLS] && !tableMeta[MetaConst.CENTER_COLUMN])) {
            return this.centerColumnsInfo;
        }

        const cenData= tableMeta[MetaConst.CATALOG_COORD_COLS] || tableMeta[MetaConst.CENTER_COLUMN];

        let s;
        if (cenData) s= cenData.split(';');
        if (!s || s.length !== 3) {
            return this.centerColumnsInfo;
        }


        const idxs = getLonLatIdx(this.tableModel, s[0], s[1]);
        if (idxs) {
            this.setCenterColumnsInfo(s[0], s[1], idxs.lonIdx, idxs.latIdx, CoordinateSys.parse(s[2]));
        }

        return this.centerColumnsInfo;
    }

    /**
     * get center columns pair by guessing
     * @returns {null|{lonCol: *, latCol: *, lonIdx: *, latIdx: *, csys: *}|*}
     */
    guessCenterColumns() {
        if (this.centerColumnsInfo) {
            return this.centerColumnsInfo;
        }
        const guess = (lon, lat) => {
            const lonCol = getColumn(this.tableModel, lon);
            const latCol = getColumn(this.tableModel, lat);
            if (lonCol && latCol) {
                const idxs = getLonLatIdx(this.tableModel, lonCol.name, latCol.name);
                if (idxs) {
                    this.setCenterColumnsInfo(lonCol.name, latCol.name, idxs.lonIdx, idxs.latIdx);
                }
            }
            return this.centerColumnsInfo;
        };
        guess('ra','dec') || guess('lon', 'lat') || guess('s_ra', 's_dec');

        return this.centerColumnsInfo;
    }

    /**
     * return center position or catalog coordinate columns and the associate*d coordinate system
     * by checking table meta, UCD values or ObsCore columns, otherwise by guessing if none of previous
     * methods works.
     * @returns {{lonCol, latCol, csys}|*}
     */
    getCenterColumns() {

        return  this.getCenterColumnsInfo() ||
                this.getCenterColumnsOnMeta() ||
                this.getCenterColumnsOnUCD() ||
                this.getCenterColumnsOnObsCore() ||
                this.guessCenterColumns();
    }


    static newInstance(tableModel) {
        return new TableRecognizer(tableModel);
    }
}

const tableRecognizerMap = new Map();

function makeTableRecognizer(table) {
    let tblRecog = table.tbl_id && tableRecognizerMap.get(table.tbl_id);

    if (tblRecog) {
        return tblRecog;
    }
    tblRecog = TableRecognizer.newInstance(table);
    if (tblRecog && table.tbl_id) {
        tableRecognizerMap.set(table.tbl_id, tblRecog);
    }
    return tblRecog;
}


export function findTableCenterColumns(table) {
   const tblRecog = makeTableRecognizer(table);

   return tblRecog && tblRecog.getCenterColumns();
}
