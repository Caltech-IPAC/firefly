/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';
import {getColumnIdx} from './TableUtil.js';


export const DEF_CORNER_COLS= ['ra1;dec1', 'ra2;dec2', 'ra3;dec3', 'ra4;dec4'];


const makeCoordColAry= (cAry, table) => cAry.map( (c) => makeCoordCol(c,table)).filter( (cCol) => cCol);

/**
 * Convert a meta data entry description the coordinate column to an CoordColsDescription object. The meta data entry is
 * a string with two or three parts separated by a semicolon. examples- as 'ra;dec;J2000' or 'lon3;lat3;Gal'.
 * The first element of the string is optional it could be 'ra;dec' and it will default to J2000.
 *
 * @param def
 * @param {TableModel} table - used to look up column information
 * @return {CoordColsDescription|null}
 */
function makeCoordCol(def, table) {
    if (!def) return null;
    const s = def.split(';');
    if (s.length!== 3 && s.length!==2) return null;
    const s0Idx= getColumnIdx(table,s[0]);
    const s1Idx= getColumnIdx(table,s[1]);
    if (s0Idx===-1 || s1Idx=== -1) return null;
    return {
        lonCol: s[0],
        latCol: s[1],
        lonIdx: s0Idx,
        latIdx: s1Idx,
        csys : s[2] ? CoordinateSys.parse(s[2]) : CoordinateSys.EQ_J2000
    };

}

/**
 * Investigate table meta data a return a CoordColsDescription array for 4 pairs columns that represent
 * the 4 corners of an object in the table
 * @param {TableModel} table
 * @return {Array.<CoordColsDescription>}
 */
export function getCornersColumns(table) {
    if (!table) return [];
    const {tableMeta}= table;
    if (!tableMeta) return [];
    if (tableMeta[MetaConst.ALL_CORNERS]) {
        return makeCoordColAry(tableMeta[MetaConst.ALL_CORNERS].split(','),table);
    }
    return makeCoordColAry(DEF_CORNER_COLS,table);
}
