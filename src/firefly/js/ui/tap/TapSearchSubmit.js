import React from 'react';
import {
    ADQL_QUERY_KEY, getAsEntryForTableName, getServiceHiPS, getServiceLabel,
    makeNumberedTitle,
    makeTapSearchTitle,
    maybeQuote, TAP_UPLOAD_SCHEMA,
    USER_ENTERED_TITLE
} from 'firefly/ui/tap/TapUtil';
import {
    getHelperConstraints,
    getTapUploadSchemaEntry,
    getUploadServerFile,
    getUploadTableName,
    isTapUpload
} from 'firefly/ui/tap/Constraints';
import {makeTblRequest, setNoCache} from 'firefly/tables/TableRequestUtil';
import {PREF_KEY} from 'firefly/tables/TablePref';
import {MetaConst} from 'firefly/data/MetaConst';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr';
import {makeColsLines, tableColumnsConstraints} from 'firefly/ui/tap/TableColumnsConstraints';


export function onTapSearchSubmit(request, serviceUrl, tapBrowserState, additionalClauses='', metaInfo={}) {
    const isUserEnteredADQL = (request.selectBy === 'adql');
    let adql;
    let isUpload;
    let serverFile;
    let uploadTableName;
    let schemaEntry;
    let userColumns;
    const userTitle= request[USER_ENTERED_TITLE];
    console.log(userTitle);

    if (isUserEnteredADQL) {
        adql = request[ADQL_QUERY_KEY];
        const {TAP_UPLOAD,uploadFile}= request;
        isUpload = Boolean(TAP_UPLOAD && uploadFile);
        serverFile = isUpload && TAP_UPLOAD[uploadFile].serverFile;
        uploadTableName = isUpload && TAP_UPLOAD[uploadFile].table;
    }
    else {
        adql = getAdqlQuery(tapBrowserState, additionalClauses);
        isUpload = isTapUpload(tapBrowserState);
        schemaEntry = getTapUploadSchemaEntry(tapBrowserState);
        const cols = schemaEntry.columns;
        userColumns = cols?.filter((col) => col.use).map((col) => col.name).join(',');
        serverFile = isUpload && getUploadServerFile(tapBrowserState);
        uploadTableName = isUpload && getUploadTableName(tapBrowserState);
    }

    if (!adql) return false;


    const maxrec = request.maxrec;
    const hasMaxrec = !isNaN(parseInt(maxrec));
    const doSubmit = () => {
        const serviceLabel= getServiceLabel(serviceUrl);
        const hips= getServiceHiPS(serviceUrl);
        const adqlClean = adql.replace(/\s/g, ' ');    // replace all whitespaces with spaces
        const params = {serviceUrl, QUERY: adqlClean};
        if (isUpload) {
            params.UPLOAD= serverFile;
            params.adqlUploadSelectTable= uploadTableName;
            if (!isUserEnteredADQL) params.UPLOAD_COLUMNS= userColumns;
        }
        if (hasMaxrec) params.MAXREC = maxrec;
        const title= makeNumberedTitle(userTitle || makeTapSearchTitle(adqlClean,serviceUrl));
        const treq = makeTblRequest('AsyncTapQuery', title, params);
        setNoCache(treq);
        const additionalTapMeta= {};
        if (!isUserEnteredADQL) {
            additionalTapMeta[PREF_KEY]= `${tapBrowserState.schemaName}-${tapBrowserState.tableName}`;
        }
        additionalTapMeta.serviceLabel= serviceLabel;
        if (hips) additionalTapMeta[MetaConst.COVERAGE_HIPS]= hips;

        treq.META_INFO= {...treq.META_INFO, ...additionalTapMeta, ...metaInfo };
        dispatchTableSearch(treq, {backgroundable: true, showFilters: true, showInfoButton: true});
    };

    if (!hasMaxrec && !adql.toUpperCase().match(/ TOP | WHERE /)) {
        showYesNoPopup(disableRowLimitMsg,(id, yes) => {
            if (yes) {
                doSubmit();
                dispatchHideDropDown();
            }
            dispatchHideDialog(id);
        });
    } else {
        doSubmit();
        return true;
    }
    return false;
}



/**
 *
 * @param {TapBrowserState} tapBrowserState
 * @param {string} additionalClauses post-WHERE clauses like ORDER BY, GROUP BY, etc. that can't be extracted from UI inputs
 * @param [showErrors]
 * @returns {string|null}
 */
export function getAdqlQuery(tapBrowserState, additionalClauses, showErrors= true) {
    const tableName = maybeQuote(tapBrowserState?.tableName, true);
    if (!tableName) return;
    const isUpload= isTapUpload(tapBrowserState);

    if (isUpload) { //check for more than one upload file (in Spatial and in ObjectID col) - should this be a utility function in constraints.js?
        const { constraintFragments } = tapBrowserState;
        const entries = [...constraintFragments.values()];
        const matchingEntries = entries.filter((c) => Boolean(c.uploadFile && c.TAP_UPLOAD && c.adqlConstraint));
        if (matchingEntries.length > 1) {
            if (showErrors) showInfoPopup('We currently do not support searches with more than one uploaded table.', 'Error');
            return;
        }
    }

    const helperFragment = getHelperConstraints(tapBrowserState);
    const tableCol = tableColumnsConstraints(tapBrowserState.columnsModel,
        isUpload?getAsEntryForTableName(tableName):undefined);

    const { table:uploadTable, asTable:uploadAsTable, columns:uploadColumns}= isUpload ?
        getTapUploadSchemaEntry(tapBrowserState) : {};

    const fromTables= isUpload ?
        `${tableName} AS ${getAsEntryForTableName(tableName)}, ${TAP_UPLOAD_SCHEMA}.${uploadTable} ${uploadAsTable ? 'AS '+uploadAsTable : ''}` :
        tableName;

    // check for errors
    if (!helperFragment.valid) {
        if (showErrors) showInfoPopup(helperFragment.messages[0], 'Error');
        return;
    }
    if (!tableCol.valid) {
        if (showErrors) showInfoPopup(tableCol.message, 'Error');
        return;
    }

    // build columns
    let selcols = tableCol.selcols || (isUpload ? `${tableName}.*` : '*');
    if (isUpload) {
        const ut= uploadAsTable ?? uploadTable ?? '';
        const tCol= uploadColumns.filter(({use}) => use).map( ({name}) => ut+'.'+name);
        selcols+= tCol.length ? ',\n' + makeColsLines(tCol,true) : '';
    }

    // build up constraints
    let constraints = helperFragment.where || '';
    if (tableCol.where) {
        const addAnd = Boolean(constraints);
        constraints += (addAnd ? '\n      AND ' : '') + `(${tableCol.where})`;
    }

    if (constraints) constraints = `WHERE ${constraints}`;

    // if we use TOP  when maxrec is set `${maxrec ? `TOP ${maxrec} `:''}`,
    // overflow indicator will not be included with the results,
    // and we will not know if the results were truncated
    let query = `SELECT ${selcols} \nFROM ${fromTables} \n${constraints}`;
    if (additionalClauses) query = `${query} \n${additionalClauses}`;
    return query;
}


const disableRowLimitMsg = (
    <div style={{width: 260}}>
        Disabling the row limit is not recommended. <br/>
        You are about to submit a query without a TOP or WHERE constraint. <br/>
        This may results in a HUGE amount of data. <br/><br/>
        Are you sure you want to continue?
    </div>
);
