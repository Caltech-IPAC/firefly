/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, unset, cloneDeep, omit, omitBy, isNil} from 'lodash';

import {ServerParams} from '../data/ServerParams.js';
import {uniqueTblId} from './TableUtil.js';
import {Keys} from '../core/background/BackgroundStatus.js';

export const MAX_ROW = Math.pow(2,31) - 1;
export const DataTagMeta = ['META_INFO', Keys.DATA_TAG]; // a tag describing the content of this table.  ie. 'catalog', 'imagemeta'
/* TABLE_REQUEST should match QueryUtil on the server-side */


/**
 * Creates a table request object for the given id.
 * @param {string} id       required.  SearchProcessor ID.
 * @param {string} [title]  title to display with this table.
 * @param {object} [params] the parameters to include with this request.
 * @param {TableRequest} [options] more options.  see TableRequest for details.
 * @returns {TableRequest}
 * @public
 * @func  makeTblRequest
 * @memberof firefly.util.table
 */
export function makeTblRequest(id, title, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};
    title = title || id;
    const tbl_id = options.tbl_id || uniqueTblId();
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});
    options = omit(options, 'tbl_id');
    return omitBy(Object.assign(req, options, params, {META_INFO, tbl_id, id}), isNil);
}

/**
 * Creates a table request for tabular data from a file.  Source of file may be
 * from a url or an absolute path on the server.
 * @param {string} [title]      title to display with this table.
 * @param {string} source       required; location of the ipac table. url or file path.
 * @param {string} [alt_source] use this if source does not exists.
 * @param {TableRequest} [options]  more options.  see TableRequest for details.
 * @returns {TableRequest}
 * @public
 * @func makeFileRequest
 * @memberof firefly.util.table
 */
export function makeFileRequest(title, source, alt_source, options={}) {
    const id = 'IpacTableFromSource';
    var req = {startIdx: 0, pageSize: 100};
    title = title || source;
    const tbl_id = options.tbl_id || uniqueTblId();
    options = omit(options, 'tbl_id');
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});
    return omitBy(Object.assign(req, options, {source, alt_source, META_INFO, tbl_id, id}), isNil);
}


/**
 * Parameters for cone search
 * @typedef {object} ConeParams
 * @global
 * @prop {string} SearchMethod  'Cone'.
 * @prop {string} position  name or coordinates of the search
 * @prop {string} radius    radius of the search in arcsec
 *
 */

/**
 * Parameters for eliptical search
 * @typedef {object} ElipParams
 * @global
 * @prop {string} SearchMethod  'Eliptical'.
 * @prop {string} position  name or coordinates of the search
 * @prop {string} radius    radius of the search in arcsec
 * @prop {string} radunits  the units for the radius or side, must be arcsec,arcmin,degree, default arcsec
 * @prop {string} ratio     ratio for elliptical request
 * @prop {string} posang    pa for elliptical request
 *
 */

/**
 * Parameters for box search
 * @typedef {object} BoxParams
 * @global
 * @prop {string} SearchMethod 'Eliptical'.
 * @prop {string} position  name or coordinates of the search
 * @prop {string} size      the length of a side for a box search
 *
 */

/**
 * creates the request to query IRSA catalogs.
 * @param {string} title    title to be displayed with this table result
 * @param {string} project
 * @param {string} catalog  the catalog name to search
 * @param {ConeParams|BoxParams|ElipParams} params   one of 'Cone','Eliptical','Box','Polygon','Table','AllSky'.
 * @param {TableRequest} [options]
 * @returns {TableRequest}
 * @public
 * @func makeIrsaCatalogRequest
 * @memberof firefly.util.table
 */
export function makeIrsaCatalogRequest(title, project, catalog, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};
    title = title || catalog;
    options.use = options.use || 'catalog_overlay';
    const tbl_id = options.tbl_id || uniqueTblId();
    const id = 'GatorQuery';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    const catalogProject = project;
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});

    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy(Object.assign(req, options, params, {id, tbl_id, META_INFO, UserTargetWorldPt, catalogProject, catalog}), isNil);
}

/**
 * creates the request to query LSST catalogs.  // TODO: more detail to be updated based on the LSST catalog DD content
 * @param {string} title    title to be displayed with this table result
 * @param {string} project
 * @param {string} database
 * @param {string} catalog  the catalog name to search
 * @param {ConeParams|BoxParams|ElipParams} params   one of 'Cone','Eliptical','Box','Polygon','Table','AllSky'.
 * @param {TableRequest} [options]
 * @returns {TableRequest}
 * @func makeLsstCatalogRequest
 * @memberof firefly.util.table
 */
export function makeLsstCatalogRequest(title, project, database, catalog, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};

    title = title || catalog;
    options.use = options.use || 'lsst_catalog_overlay';
    const tbl_id = options.tbl_id || uniqueTblId();
    const id = get(params, 'SearchMethod')==='Table'?'LSSTMultiObjectSearch':'LSSTCataLogSearch';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    const table_name = catalog;
    const meta_table = catalog;
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});


    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy(Object.assign(req, options, params,
                                {id, tbl_id, META_INFO, UserTargetWorldPt, database, table_name, meta_table, project}), isNil);
}

/**
 * creates the request to query VO catalogmakeLsstCatalogRequest
 * @param {string} title    title to be displayed with this table result
 * @param {ConeParams|BoxParams|ElipParams} params   one of 'Cone','Eliptical','Box','Polygon','Table','AllSky'.
 * @param {TableRequest} [options]
 * @returns {TableRequest}
 * @func makeVOCatalogRequest
 * @memberof firefly.util.table
 */
export function makeVOCatalogRequest(title, params={}, options={}) {
    var req = {startIdx: 0, pageSize: 100};
    options.use = options.use || 'catalog_overlay';
    const tbl_id = options.tbl_id || uniqueTblId();
    const providerid = voProviders[params.providerName];

    const id = providerid || 'ConeSearchByURL';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    var META_INFO = Object.assign(options.META_INFO || {}, {title, tbl_id});

    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy(Object.assign(req, options, params, {id, tbl_id, META_INFO, UserTargetWorldPt}), isNil);
}

const voProviders = {'NED':'NedSearch'};

/**
 * create a deep clone of the given request.  tbl_id is removed from the cloned request.
 * @param {TableRequest} request  the original request to clone
 * @param {Object} params   additional parameters to add to the cloned request
 * @returns {TableRequest}
 * @public
 * @func cloneRequest
 * @memberof firefly.util.table
 */
export function cloneRequest(request, params = {}) {
    const req = cloneDeep(omit(request, 'tbl_id'));
    unset(req, 'META_INFO.tbl_id');
    return Object.assign(req, params);
}

/**
 * create a request which will perform a function on the given searchRequest
 * @param {TableRequest} searchRequest  required. the table's request this function should operate on
 * @param {string} id                   required.  SearchProcessor ID.
 * @param {string} [title]              title to display with this table.
 * @param {object} [params]             the parameters to include with this request.
 * @param {TableRequest} [options]      more options.  see TableRequest for details.
 * @returns {TableRequest}
 * @public
 * @func makeTableFunctionRequest
 * @memberof firefly.util.table
 */
export function makeTableFunctionRequest(searchRequest, id, title, params={}, options={}) {
    const req = makeTblRequest(id, title, params, options);
    req.searchRequest = JSON.stringify(searchRequest);
    return req;
}

