/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Constants extracted from TableUtil.js into a separate file to break the
 * circular dependency chain:
 *   TableUtil → TablesCntlr → BackgroundUtil → TableUtil
 *
 * When Rollup bundles ApiUtilTable.jsx (which re-exports TableUtil.js) as a
 * namespace for ApiBuild.js, it eagerly evaluates every const value in that
 * namespace.  Because the cycle above defers TableUtil.js's module body,
 * those consts are still in the Temporal Dead Zone when the namespace is
 * created, causing a ReferenceError at bundle load time.
 *
 * Placing these constants here, with no dependency on TablesCntlr.js or
 * anything downstream, ensures Rollup initialises them before the namespace
 * object is constructed.
 */
import Enum from 'enum';

/* TABLE_REQUEST should match QueryUtil on the server-side */
export const META = {
    // should match TableMeta.java
    doclink: {url: 'doclink.url', desc: 'doclink.desc', label: 'doclink.label'}
};

export const MAX_ROW = Math.pow(2, 31) - 1;
export const SYS_COLUMNS = ['ROW_IDX', 'ROW_NUM'];
export const NOT_CELL_DATA = '__NOT_A_VALID_DATA___';
export const DOC_FUNCTIONS_URL = 'https://duckdb.org/docs/sql/functions/overview.html';

const TEXT  = ['char'];
const INT   = ['long', 'int', 'short', 'integer'];
const FLOAT = ['double', 'float', 'real'];
const BOOL  = ['boolean', 'bool'];
const DATE  = ['date'];
const NUMBER     = [...INT, ...FLOAT];
const USE_STRING = [...TEXT, ...DATE];
const ENUM_TYPES = [...TEXT, ...INT, ...BOOL];

// export const COL_TYPE = new Enum(['ALL', 'NUMBER', 'TEXT', 'INT', 'FLOAT']);
export const COL_TYPE = new Enum({ANY: [], TEXT, INT, FLOAT, BOOL, DATE, NUMBER, USE_STRING, ENUM_TYPES});
export const TBL_STATE = new Enum(['ERROR', 'LOADING', 'NO_DATA', 'NO_MATCH', 'OK']);
