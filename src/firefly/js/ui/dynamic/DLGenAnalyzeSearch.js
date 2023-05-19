import {MetaConst} from '../../data/MetaConst.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {getColumnIdx, getMetaEntry, getTblById} from '../../tables/TableUtil.js';
import {Logger} from '../../util/Logger.js';
import {getServiceDescriptors} from '../../util/VOAnalyzer.js';
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
    const table= getTblById(tbl_id);
    const sdAry= getServiceDescriptors(table,false);
    if (!sdAry || !table) return;
    const {data=[]}= table.tableData ?? {};

    const ACCESS_URL= 'access_url';
    const SD= 'service_def';
    const SEMANTICS= 'semantics';
    const ID= 'id';
    const DESC= 'description';

    const semIdx= getColumnIdx(table,SEMANTICS,true);
    const sdIdx= getColumnIdx(table,SD,true);
    const idIdx= getColumnIdx(table,ID,true);
    const accessUrlIdx= getColumnIdx(table,ACCESS_URL,true);
    const descIdx= getColumnIdx(table,DESC,true);

    const bandDesc= getMetaEntry(table,'bandDesc');
    const coverage= getMetaEntry(table,'coverage');


    const makeSearchDef= (row) => {
        const serviceDef= sdAry.find( (sd) => {
            const id= sd.ID ?? sd.id ?? '';
            return id===row[sdIdx];
        });
        return {serviceDef, accessUrl: row[accessUrlIdx], id:row[idIdx], desc:row[descIdx], semantic: row[semIdx], bandDesc, coverage};
    };

    const primarySearchDef= data
        .filter( (row) => {
            if (table.tableData.data.length===1) return true;
            if (!row[sdIdx]) return false;
            const semantic= (row[semIdx] ?? '').toLowerCase();
            const sd= row[sdIdx];
            if (sd && !semantic.endsWith('cisx#concurrent-query')) return true;
            // if (semantic.endsWith('this') || semantic.endsWith('cisx#primary-query') || sd) return true;
        })
        .map(makeSearchDef);


    const concurrentSearchDef= data
        .filter( (row) => {
            if (!row[sdIdx]) return false;
            const semantic= (row[semIdx] ?? '').toLowerCase();
            if (semantic.endsWith('cisx#concurrent-query')) return true;
        })
        .map(makeSearchDef);

    const urlRows= data
        .filter((row) => Boolean(row[accessUrlIdx] && !row[semIdx]?.includes('CISX')))
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
 * @param {String} helpUrl
 * @param {String} selectedConcurrent - space separated name of searches to execute
 */
export function handleSearch(request, qAna, primaryFdAry, idx, helpUrl, selectedConcurrent) {
    const primeSd= qAna.primarySearchDef[idx].serviceDef;
    const {coverage,bandDesc}= qAna.primarySearchDef[idx];
    const {cisxUI}= qAna.primarySearchDef[0].serviceDef;
    const preferredHips= cisxUI.find( (e) => e.name==='HiPS')?.value;

    const concurrentSDAry= qAna.concurrentSearchDef.filter( (searchDef) => {
        const id= searchDef.serviceDef.ID ?? searchDef.serviceDef.id ?? '';
        const selected= selectedConcurrent.includes(searchDef.desc);
        return Boolean(id && selected);
    });

    const extraMeta= {coverage,bandDesc,helpUrl};
    if (preferredHips) extraMeta[MetaConst.COVERAGE_HIPS]=preferredHips;

    const tableRequestAry= makeAllSearchRequest(request, primeSd,concurrentSDAry, primaryFdAry, extraMeta);

    tableRequestAry.forEach( (dataTableReq) => {
        Logger('DLGeneratedDropDown').debug(dataTableReq);
        if (dataTableReq) {
            dispatchTableSearch(dataTableReq, {showFilters: true, showInfoButton: true }); //todo are the options the default?
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
 * @prop {Array.<SearchDefinition>} concurrentSearchDef - these search could happen along with the main serach
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
 *
 */
