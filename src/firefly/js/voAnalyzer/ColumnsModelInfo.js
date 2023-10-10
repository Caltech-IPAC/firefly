/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, has, isArray, isEmpty} from 'lodash';
import {getColumnValues} from '../tables/TableUtil.js';
import CoordinateSys from '../visualize/CoordSys.js';
import { alternateMainPos, mainMeta, OBSTAP_MATCH_COLUMNS, posCol, UCDCoord, ucdSyntaxMap } from './VoConst.js';
import {isUCDWith} from './VoCoreUtils.js';

/**
 * table analyzer based on the table model for columns which contains column_name & ucd columns
 */
class ColumnRecognizer {
    constructor(columnsModel, posCoord = 'eq') {
        this.columnsModel = columnsModel;
        this.ucds = getColumnValues(columnsModel, 'ucd').map((v) => v || '');
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
        const centerColUCDs = has(posCol, coord) ? posCol[coord].ucd : null;
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
        } else if (posPairs[0].length > 0 && posPairs[1].length > 0) {
            // find first exact match
            const basicPair = posPairs.map((cols, i) => cols.find((c) => c.ucd === centerColUCDs[0][i]));
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


        const guess = (lon, lat, useReg = false) => {

            let lonCol;
            let latCol;
            if (useReg) {
                const reLon = new RegExp(`^[A-z]?[-_]?(${lon})[1-9]*$`);
                const reLat = new RegExp(`^[A-z]?[-_]?(${lat})[1-9]*$`);
                lonCol = findColumn(lon, reLon);
                latCol = findColumn(lat, reLat);
            } else {
                lonCol = findColumn(lon);
                latCol = findColumn(lat);
            }

            return (lonCol && latCol) ? this.setCenterColumnsInfo([lonCol, latCol]) : this.centerColumnsInfo;
        };
        return (guess('ra', 'dec') || guess('lon', 'lat') || guess('ra', 'dec', true) || guess('lon', 'lat', true));
    }


    getCenterColumns() {
        return this.getCenterColumnsOnUCD() ||
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
 * return true if all columns include the obs tap required columns
 * @param columnsModel
 * @return {boolean}
 */
export function isColumnsMatchingToObsTap(columnsModel) {
    if (!columnsModel) return false;
    const column_names = getColumnValues(columnsModel, 'column_name');
    return OBSTAP_MATCH_COLUMNS.every((columnName) => column_names.includes(columnName));
}