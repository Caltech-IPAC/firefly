import {isEmpty} from 'lodash';
import {MetaConst} from '../../data/MetaConst.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {getMetaEntry} from '../../tables/TableUtil.js';
import {Logger} from '../../util/Logger.js';
import {getDataLinkData} from '../../voAnalyzer/VoDataLinkServDef.js';
import {CONE_CHOICE_KEY, POLY_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {CIRCLE, POINT, POLYGON} from './DynamicDef.js';
import {
    convertCircleToPointArea, convertPointAreaToCircle, isCircleSearch, isPointAreaSearch, isPolySearch
} from './DynamicUISearchPanel.jsx';
import {findFieldDefType, makeFieldDefs, makeServiceDescriptorSearchRequest} from './ServiceDefTools.js';




/**
 *
 * @param {String} tbl_id
 * @return {QueryAnalysis} - the description of all the searches to do for this table
 */
export function analyzeQueries(tbl_id) {

    const dlData= getDataLinkData(tbl_id);
    if (isEmpty(dlData)) return;
    const bandDesc= getMetaEntry(tbl_id,'bandDesc');
    const coverage= getMetaEntry(tbl_id,'coverage');

    const makeSearchDef= ({serDef,url,id,description, semantics}) => {
        return {serviceDef:serDef, accessUrl: url, id, desc:description, semantic: semantics, bandDesc, coverage};
    };

    const primarySearchDef= dlData
        .filter( (dl) => dlData.length===1 || (dl.serDef && dl.dlAnalysis.cisxPrimaryQuery))
        .map(makeSearchDef);

    const concurrentSearchDef= dlData.filter( (dl) => (dl.dlAnalysis.cisxConcurrentQuery)).map(makeSearchDef);

    const urlRows= dlData
        .filter((dl) => Boolean(dl.url && !dl.semantics?.includes('CISX')))
        .map(makeSearchDef);

    return {primarySearchDef, concurrentSearchDef, urlRows};
}


export function makeAllSearchRequest(request, primeSd, concurrentSDAry, primaryFdAry, extraPrimaryMeta) {
    const primeRequest= makeServiceDescriptorSearchRequest(request,primeSd,extraPrimaryMeta);
    const concurrentRequestAry= concurrentSDAry
        .map( (sd) => {
            const newR= convertRequestToSecondary(request, primaryFdAry?.[0], sd.serviceDef, primeSd.standardID);
            return newR ? makeServiceDescriptorSearchRequest(newR,sd.serviceDef) : undefined;
        })
        .filter( (r) => r);

    return [primeRequest, ...concurrentRequestAry];
}

/**
 * Do all the searches defined
 * @param request
 * @param {QueryAnalysis} qAna
 * @param primaryFdAry
 * @param idx
 * @param {object} extraMeta            // additional table meta to include with the TableSearch
 * @param {String} selectedConcurrent - space separated name of searches to execute
 * @param docRows   URLs to documentation
 */
export function handleSearch(request, qAna, primaryFdAry, idx, extraMeta={}, selectedConcurrent) {
    const primeSd= qAna.primarySearchDef[idx].serviceDef;
    const {coverage,bandDesc}= qAna.primarySearchDef[idx];
    const {cisxUI}= qAna.primarySearchDef[0].serviceDef;
    const preferredHips= cisxUI.find( (e) => e.name==='HiPS')?.value;

    const concurrentSDAry= qAna.concurrentSearchDef.filter( (searchDef) => {
        const id= searchDef.serviceDef.ID ?? searchDef.serviceDef.id ?? '';
        const selected= selectedConcurrent.includes(searchDef.desc);
        return Boolean(id && selected);
    });

    extraMeta= {coverage,bandDesc, ...extraMeta};
    if (preferredHips) extraMeta[MetaConst.COVERAGE_HIPS]=preferredHips;

    const tableRequestAry= makeAllSearchRequest(request, primeSd,concurrentSDAry, primaryFdAry, extraMeta);

    tableRequestAry.forEach( (dataTableReq) => {
        Logger('DLGeneratedDropDown').debug(dataTableReq);
        if (dataTableReq) {
            dispatchTableSearch(dataTableReq);
        }
    });
}

function convertRequestToSecondary(request, primaryFdAry, secondServDef, primStandardID) {
    const sFdAry= makeFieldDefs(secondServDef.serDefParams);
    if (isCircleSearch(primaryFdAry) || isPointAreaSearch(sFdAry)) {
        return convertCircleToPointArea(request, primaryFdAry, sFdAry, primStandardID, secondServDef.standardID);
    }
    if (isCircleSearch(sFdAry) || isPointAreaSearch(primaryFdAry)) {
        return convertPointAreaToCircle(request, primaryFdAry, sFdAry, primStandardID, secondServDef.standardID);
    }
    return request;
}

function getServiceDefSpacialSupports(serviceDef) {
    const fdAry= makeFieldDefs(serviceDef.serDefParams);
    const retAry=[];
    if (isCircleSearch(fdAry)) retAry.push(CIRCLE);
    if (isPointAreaSearch(fdAry)) retAry.push(POINT);
    if (isPolySearch(fdAry)) retAry.push(POLYGON);
    return retAry;
}

export function hasSpatialTypes(serviceDef) {
    return getServiceDefSpacialSupports(serviceDef).length>0;
}

export function isSpatialTypeSupported(serviceDef, spacialType) {
    const fdAry= makeFieldDefs(serviceDef.serDefParams);
    if (spacialType===CONE_CHOICE_KEY) {
        return isCircleSearch(fdAry) || isPointAreaSearch(fdAry) || Boolean(findFieldDefType(fdAry,POINT));
    }
    else if (spacialType===POLY_CHOICE_KEY) {
        return isPolySearch(fdAry);
    }
    return false;
}

/**
 * @typedef {Object} QueryAnalysis
 *
 * @prop {Array.<SearchDefinition>} primarySearchDef - this is the main search
 * @prop {Array.<SearchDefinition>} concurrentSearchDef - these search could happen along with the main search
 * @prop {Array.<SearchDefinition>} urlRows - should be not service def just a simple URL to call
 */


/**
 * @typedef {Object} SearchDefinition
 * This object is a combination of a row and its service descriptor if one exist
 *
 * @props {ServiceDescriptorDef} serviceDef
 * @prop {String} accessUrl - the base url of the search
 * @prop {String} bandDesc - a description that comes for the metadata of the table
 * @prop {String} coverage- a coverage description that comes for the metadata of the table
 * @prop {String} desc - from the description column
 * @prop {String} id
 * @prop {String} semantic - from the semantic column
 * @prop {ServiceDescriptorDef} serviceDef - the service descriptor
 *
 */
