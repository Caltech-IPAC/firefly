/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set, unset, cloneDeep, omit, omitBy, isNil, pickBy, uniqueId} from 'lodash';

import {getTblById, uniqueTblId} from './TableUtil.js';
import {SelectInfo} from './SelectInfo.js';
import {ServerParams} from '../data/ServerParams.js';
import {WS_HOME} from '../visualize/WorkspaceCntlr.js';
import {getJobInfo} from '../core/background/BackgroundUtil.js';
import {fetchTable} from 'firefly/rpc/SearchServicesJson';
import {Logger} from '../util/Logger.js';


const logger = Logger('Tables').tag('TableRequestUtil');

export const MAX_ROW = Math.pow(2,31) - 1;
/* TABLE_REQUEST should match QueryUtil on the server-side */

export const META = {
    // should match TableMeta.java
    doclink: {url: 'doclink.url', desc: 'doclink.desc', label: 'doclink.label'}
};


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
    title = title ?? id;
    const tbl_id = options.tbl_id || uniqueTblId();
    var META_INFO = pickBy(Object.assign(options.META_INFO || {}, {title, tbl_id}));
    options = omit(options, 'tbl_id');
    return omitBy(Object.assign({startIdx: 0}, options, params, {META_INFO, tbl_id, id}), isNil);
}

/**
 * Creates a table request for tabular data from a file or url.  Source of file may be
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
    title = title ?? source;
    const tbl_id = options.tbl_id || uniqueTblId();
    options = omit(options, 'tbl_id');
    var META_INFO = pickBy(Object.assign(options.META_INFO || {}, {title, tbl_id}));
    return omitBy(Object.assign({startIdx: 0}, options, {source, alt_source, META_INFO, tbl_id, id}), isNil);
}

/**
 * Creates a table request for tabular data from IRSA workspace.
 * Source should be an IRSA workspace path.  i.e value returned from WorkspaceViewer
 * @param {string} source       required; IRSA workspace path.
 * @param {string} [title]      title to display with this table.
 * @param {TableRequest} [options]  more options.  see TableRequest for details.
 * @returns {TableRequest}
 * @public
 * @func makeIrsaWorkspaceRequest
 * @memberof firefly.util.table
 */
export function makeIrsaWorkspaceRequest(source, title, options={}) {
    source = source.replace(WS_HOME, '');
    const request = makeFileRequest(title, source, undefined, options);
    request[ServerParams.SOURCE_FROM] = ServerParams.IS_WS;
    return request;
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
    title = title ?? catalog;
    const tbl_id = options.tbl_id || uniqueTblId();
    const id = 'GatorQuery';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    const catalogProject = project;
    var META_INFO = pickBy(Object.assign(options.META_INFO || {}, {title, tbl_id}));

    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy(Object.assign({startIdx: 0}, options, params, {id, tbl_id, META_INFO, UserTargetWorldPt, catalogProject, catalog}), isNil);
}

/**
 * creates the request to query VO using ConeSearchByURL or another vo provider Search processor
 * @param {string} title    title to be displayed with this table result
 * @param {ConeParams|BoxParams|ElipParams} params   one of 'Cone','Eliptical','Box','Polygon','Table','AllSky'.
 * @param {TableRequest} [options]
 * @returns {TableRequest}
 * @func makeVOCatalogRequest
 * @memberof firefly.util.table
 * @see edu.caltech.ipac.firefly.server.persistence.QueryByConeSearchURL.java
 */
export function makeVOCatalogRequest(title, params={}, options={}) {
    const tbl_id = options.tbl_id || uniqueTblId();
    const id = voProviders[params.providerName] || 'ConeSearchByURL';
    const UserTargetWorldPt = params.UserTargetWorldPt || params.position;  // may need to convert to worldpt.
    const META_INFO = {...options.META_INFO, title, tbl_id};

    options = omit(options, 'tbl_id');
    params = omit(params, 'position');

    return omitBy({startIdx: 0, ...options, ...params, id, tbl_id, META_INFO, UserTargetWorldPt}, isNil);
}

const voProviders = {'NED':'NedSearch'};

/**
 * Parameters for cone search
 * @typedef {object} ResourceInfo
 * @global
 * @public
 * @prop {TableRequest} request     required. the table request to create this resource from
 * @prop {string}       [scope]       access type of this resource.  one of 'global', 'user', 'protected'.  defaults to 'global'.
 * @prop {string}       [secret]      the secret token required to access 'protected' scope
 */

/**
 * Create the given Resource
 * @param {ResourceInfo} resource   resource information
 * @public
 * @func  createResource
 * @memberof firefly.util.table
 */
export function createResource(resource) {
    handleResourceRequest({resource, action: 'create'});
}

/**
 * Delete the given Resource
 * @param {ResourceInfo} resource   resource information
 * @public
 * @func  deleteResource
 * @memberof firefly.util.table
 */
export function deleteResource(resource) {
    handleResourceRequest({resource, action: 'delete'});
}

/**
 * @param {ResourceInfo} resource   resource information
 * @param {string} [title]          title to display with this table.
 * @param {TableRequest} [options]  table request options.  see TableRequest for details.
 * @returns {TableRequest} the request to query data from the given resource
 * @public
 * @func  makeResourceRequest
 * @memberof firefly.util.table
 */
export function makeResourceRequest(resource={}, title, options={}) {
    return handleResourceRequest({resource, title, options, action: 'query'});
}

/**
 * private function to handle Resource related actions
 * @param {object} p                function parameters
 * @param {ResourceInfo} p.resource resource information
 * @param {string} [p.title]        title to display with this table.
 * @param {TableRequest} [p.options] table request options.  see TableRequest for details.
 * @param {string} p.action one of 'query', 'create', 'delete'.  defautls to 'query'.
 *                          when action is 'create' or 'delete', it will submit the request.
 *                          no further action is needed.
 */
function handleResourceRequest({resource={}, title, options={}, action='query'}) {
    const {request, scope, secret} = resource;
    const searchRequest = JSON.stringify(request);

    if (action === 'query') {
        return makeTblRequest('ResourceProcessor', title, {searchRequest, action, scope, secret}, options);
    } else {
        const req = makeTblRequest('ResourceProcessor', null, {searchRequest, action, scope, secret});
        return fetchTable(req)   // initiate and load the resource
            .catch((err) => logger.error(`Failed to ${action} Resource from request: ${err}`, `request=${request}`));
    }
}

/**
 * create a deep clone of the given request.  tbl_id is removed from the cloned request.
 * @param {TableRequest}        request  the original request to clone
 * @param {Object} params       additional parameters to add to the cloned request
 * @param {boolean} createTblId when true, create a new unique tbl_id for this request
 * @returns {TableRequest}
 * @public
 * @func cloneRequest
 * @memberof firefly.util.table
 */
export function cloneRequest(request={}, params = {}, createTblId) {
    const req = cloneDeep(omit(request, 'tbl_id'));
    if (createTblId) {
        const tbl_id = uniqueId(request.tbl_id || 'tbl_id-');
        set(req, 'META_INFO.tbl_id', tbl_id);
        req.tbl_id = tbl_id;
    } else {
        unset(req, 'META_INFO.tbl_id');
    }
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


/**
 * create a request which will perform a sub query on the given searchRequest
 * @param {TableRequest} searchRequest  required. the table's request this function should operate on
 * @param {string} [title]              title to display with this table.
 * @param {object} [params]             the parameters to include with this request.
 * @param {TableRequest} [options]      more options.  see TableRequest for details.
 * @returns {TableRequest}
 * @public
 * @func makeSubQueryRequest
 * @memberof firefly.util.table
 */
export function makeSubQueryRequest(searchRequest, title, params={}, options={}) {
    const req = makeTblRequest('SubQueryProcessor', title, params, options);
    req.searchRequest = JSON.stringify(searchRequest);
    return req;
}

/**
 * return the tbl_id from the request object
 * @param {TableRequest} request
 * @returns {string}
 */
export function getTblId(request) {
    return get(request, 'META_INFO.tbl_id') || get(request, 'tbl_id'); 
}

/**
 * set the resultSetID into the request object
 * @param {TableRequest} request
 * @param {string} resultSetID
 */
export function setResultSetID(request, resultSetID) {
    set(request, 'META_INFO.resultSetID', resultSetID);
}

/**
 * set the resultSetRequest into the request object
 * @param {TableRequest} request
 * @param {string} resultSetRequest
 */
export function setResultSetRequest(request, resultSetRequest) {
    set(request, 'META_INFO.resultSetRequest', resultSetRequest);
}

/**
 * set selectInfo
 * @param {TableRequest} request
 * @param {object} selectInfo
 */
export function setSelectInfo(request, selectInfo) {
    if (selectInfo) {
        set(request, 'META_INFO.selectInfo', SelectInfo.newInstance(selectInfo).toString());
    }
}

/**
 * set timestamp parameter to prevent search results from caching
 * @param {TableRequest} request
 */
export function setNoCache(request) {
    set(request, '__searchID', Date.now());
}


/**
 * @param jobId
 * @returns {Request} returns search request from the given jobId
 */
export function getRequestFromJob(jobId) {
    const request = getJobInfo(jobId)?.parameters?.[ServerParams.REQUEST];
    return request ? JSON.parse(request) : {};
}

/**
 * @param {string} tbl_id
 * @returns {string} returns Job ID from the given tbl_id
 */
export function getJobIdFromTblId(tbl_id) {
    const request = getTblById(tbl_id)?.request;
    return request?.META_INFO?.[ServerParams.JOB_ID];
}

