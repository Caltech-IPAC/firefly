/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {once, set} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';
import FieldGroupUtils, {getFieldVal, setFieldValue} from 'firefly/fieldGroup/FieldGroupUtils.js';
import {makeSearchOnce} from 'firefly/util/WebUtil.js';
import {dispatchMultiValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {ExtraButton, FormPanel} from 'firefly/ui/FormPanel.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {intValidator} from 'firefly/util/Validate.js';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import CreatableSelect from 'react-select/creatable';
import {makeTblRequest, setNoCache} from 'firefly/tables/TableRequestUtil.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr.js';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil.jsx';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr.js';
import {
    ConstraintContext, getTapUploadSchemaEntry, getUploadServerFile, getUploadTableName,
    getHelperConstraints, getUploadConstraint,
    isTapUpload
} from './Constraints.js';

import {makeColsLines, tableColumnsConstraints} from 'firefly/ui/tap/TableColumnsConstraints.jsx';
import {commonSelectStyles, selectTheme} from 'firefly/ui/tap/Select.jsx';
import {
    getMaxrecHardLimit, tapHelpId, getTapServices,
    loadObsCoreSchemaTables, maybeQuote, defTapBrowserState, TAP_UPLOAD_SCHEMA, getAsEntryForTableName,
} from 'firefly/ui/tap/TapUtil.js';
import {SectionTitle, AdqlUI, BasicUI} from 'firefly/ui/tap/TableSelectViewPanel.jsx';
import {useFieldGroupMetaState} from '../SimpleComponent.jsx';
import {PREF_KEY} from 'firefly/tables/TablePref.js';



export const TAP_PANEL_GROUP_KEY = 'TAP_PANEL_GROUP_KEY';
const SERVICE_TIP= 'Select a TAP service, or type to enter the URL of any other TAP service';

//-------------
//-------------
// set up the one time (once) functions to be used with the API via initArgs. initArgs is really only used the
// first time though the UI.
//-------------
//-------------

let webApiUserAddedService;
const initServiceUsingAPIOnce= makeSearchOnce(false); // call one time during first construction
const searchFromAPIOnce= makeSearchOnce(); // setup options to immediately execute the search the first time
const activateInitArgsAdqlOnce= once((tapBrowserState,initArgs) => initArgs?.urlApi?.adql && setTimeout(() => populateAndEditAdql(tapBrowserState,initArgs.urlApi?.adql), 5));
let lastServicesShowing= false;

/** if an extra service is found from the api that is not in the list then set webApiUserAddedService */
const initApiAddedServiceOnce= once((initArgs) => {
    if (initArgs?.urlApi?.service) {
        const listedEntry= getTapServiceOptions().find( (e) => e.value===initArgs.urlApi?.service);
        if (!listedEntry) webApiUserAddedService = {label: initArgs.urlApi?.service, value: initArgs.urlApi?.service};
    }
});

/**
 * Return true if the initArgs has populated the tap fields so a valid search can be executed.
 * @param fields
 * @param initArgs
 * @param {TapBrowserState} tapBrowserState
 * @return {boolean} true if the field are valid to doa search
 */
function validateAutoSearch(fields, initArgs, tapBrowserState) {
    const {urlApi = {}} = initArgs;
    if (urlApi.adql) return true;
    if (!getAdqlQuery(tapBrowserState, false)) return false; // if we can't build a query then we are not initialized yet
    const {valid, where} = getHelperConstraints(tapBrowserState);
    if (!valid) return false;
    const notWhereArgs = ['MAXREC', 'execute', 'schema', 'service', 'table'];
    const needsWhereClause = Object.keys(urlApi).some((arg) => !notWhereArgs.includes(arg));
    return needsWhereClause ? Boolean(where) : true;
}


//----------
//----------
// end of once api functions
//----------
//----------

function getInitServiceUrl(tapBrowserState,initArgs,tapOps) {
    let {serviceUrl=tapOps[0].value} = tapBrowserState;
    initServiceUsingAPIOnce(true, () => {
        if (initArgs?.urlApi?.service) serviceUrl= initArgs.urlApi.service;
    });
    return serviceUrl;
}

export function TapSearchPanel({initArgs= {}, titleOn=true}) {
    const [getTapBrowserState]= useFieldGroupMetaState(defTapBrowserState,TAP_PANEL_GROUP_KEY);
    const tapState= getTapBrowserState();
    if (!initArgs?.urlApi?.execute) searchFromAPIOnce(true); // if not execute then mark as done, i.e. disable any auto searching
    initApiAddedServiceOnce(initArgs);  // only look for the extra service the first time
    const tapOps= getTapServiceOptions();
    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    const [selectBy, setSelectBy]= useState(initArgs?.urlApi?.selectBy || 'basic');
    const [servicesShowing, setServicesShowingInternal]= useState(lastServicesShowing);
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const [serviceUrl, setServiceUrl]= useState(() => getInitServiceUrl(tapState,initArgs,tapOps));
    activateInitArgsAdqlOnce(tapState,initArgs);

    const setServicesShowing= (showing) => {
        setServicesShowingInternal((showing));
        lastServicesShowing= showing;
    };

    const obsCoreEnabled = obsCoreTableModel?.tableData?.data?.length > 0;

    const onTapServiceOptionSelect= (selectedOption) => {
        if (!selectedOption) return;
        dispatchMultiValueChange(TAP_PANEL_GROUP_KEY,
            [
                {fieldKey: 'defAdqlKey', value: ''},
                {fieldKey: 'adqlQuery', placeholder: '', value: ''}
            ]
        );
        setServiceUrl(selectedOption.value);
        setObsCoreTableModel(undefined);
    };

    useEffect(() => {
        const {serviceUrl:u}= initArgs?.searchParams ?? {};
        u && u!==serviceUrl && setServiceUrl(u);
    }, [initArgs?.searchParams?.serviceUrl]);

    useEffect(() => {
        return FieldGroupUtils.bindToStore( TAP_PANEL_GROUP_KEY, (fields) => {
            setSelectBy(getFieldVal(TAP_PANEL_GROUP_KEY,'selectBy',selectBy));
            const ts= getTapBrowserState();
            setObsCoreTableModel(ts.obsCoreTableModel);

            searchFromAPIOnce( // searchFromAPIOnce only matters if the urlApi.execute is true
                () => validateAutoSearch(fields,initArgs,ts),
                () => setTimeout(() => clickFuncRef.clickFunc?.(), 5));
        });
    }, []);

    useEffect(() => {
        setFieldValue(TAP_PANEL_GROUP_KEY, 'selectBy', selectBy);
    }, [selectBy]);

    const ctx= {
        setConstraintFragment: (key,value) => {
            value ?
                getTapBrowserState().constraintFragments.set(key,value) :
                getTapBrowserState().constraintFragments.delete(key);
        }
    };

    return (
        <div style={{width: '100%'}}>
            <ConstraintContext.Provider value={ctx}>
                <FormPanel  inputStyle = {{display: 'flex', flexDirection: 'column', backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                            groupKey={TAP_PANEL_GROUP_KEY}
                            getDoOnClickFunc={(clickFunc) => clickFuncRef.clickFunc= clickFunc}
                            params={{hideOnInvalid: false}}
                            onSubmit={(request) => onTapSearchSubmit(request, serviceUrl,tapState)}
                            extraWidgets={makeExtraWidgets(initArgs,selectBy,setSelectBy, tapState)}
                            // extraWidgetsRight={makeExtraWidgetsRight(servicesShowing, setServicesShowing)}
                            buttonStyle={{justifyContent: 'left'}}
                            submitBarStyle={{padding: '2px 3px 3px'}}
                            includeUnmounted={true}
                            help_id = {tapHelpId('form')} >
                    <TapSearchPanelComponents {...{
                        servicesShowing, setServicesShowing,
                        initArgs, selectBy, setSelectBy, serviceUrl, onTapServiceOptionSelect, titleOn, tapOps, obsCoreEnabled}} />
                </FormPanel>
            </ConstraintContext.Provider>
        </div>
    );

}


const extStyles= {display: 'inline-block', fontWeight: 'bold'};

const tableSelectStyleEnhancedTemplate= {
    ...commonSelectStyles,
    singleValue: (provided, {data: {labelOnly=''}}) => ({
        ...provided,
        ':before': {
            ...extStyles, content: `"${labelOnly}"`, marginRight: 8, height: 12, width: `${labelOnly.length*.65}em`,
        },
    }),
    option: (provided, {data: {labelOnly=''}}) => ({
        ...provided, ':before': {...extStyles, content: `"${labelOnly}"`, marginRight: 8, width: 100},
    }),
};

const makePlaceHolderBeforeStyle= (l) =>
    ({ ...extStyles, content: `"Using ${l}"`, height: 10, width: `${(l.length+7)*.6}em`});



function TapSearchPanelComponents({initArgs, serviceUrl, servicesShowing, setServicesShowing, onTapServiceOptionSelect, tapOps, titleOn=true, selectBy, setSelectBy}) {

    const label= (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.labelOnly)) || '';
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const hasObsCoreTable = obsCoreTableModel?.tableData?.data?.length > 0;

    const loadObsCoreTables = (requestServiceUrl) => {
        loadObsCoreSchemaTables(requestServiceUrl).then((tableModel) => {
            setObsCoreTableModel(tableModel);
        });
    };

    useEffect(() => {
        loadObsCoreTables(serviceUrl);
    }, [serviceUrl]);

    return (
        <FieldGroup groupKey={TAP_PANEL_GROUP_KEY} keepState={true} style={{flexGrow: 1, display: 'flex'}}>
            <div className='TapSearch'>
                {titleOn && <div className='TapSearch__title'>TAP Searches</div>}
                <Services {...{serviceUrl, servicesShowing, tapOps, onTapServiceOptionSelect}}/>
                { selectBy === 'adql' ?
                    <AdqlUI {...{serviceUrl, servicesShowing, setServicesShowing, setSelectBy}}/> :
                    <BasicUI  {...{serviceUrl, serviceLabel: label, selectBy, initArgs, obsCoreTableModel,
                        servicesShowing, setServicesShowing, hasObsCoreTable, setSelectBy}}/>
                }
            </div>
        </FieldGroup>
    );
}

function Services({serviceUrl, servicesShowing, tapOps, onTapServiceOptionSelect} ) {
    const label= (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.labelOnly)) || '';
    const placeholder = serviceUrl ? `${serviceUrl} - Replace...` : 'Select TAP...';
    const [extraStyle,setExtraStyle] = useState({overflow:'hidden'});

    const tableSelectStyleEnhanced= {
        ...tableSelectStyleEnhancedTemplate,
        placeholder: (provided) => ({ ...provided, ':before': makePlaceHolderBeforeStyle(label) })
    };
    const title= <div style={{width:170}}>{'Select TAP Service'}</div>;

    useEffect(() => {
        if (servicesShowing) {
            setTimeout(() => setExtraStyle({overflow:'visible'}), 250);
        }
        else {
            setExtraStyle({overflow:'hidden'});
        }
    }, [servicesShowing]);

    return (
        <div
            className={servicesShowing?'TapSearch__section':'TapSearch__section TapSearch__hide'}
            style={{height: servicesShowing?62:0, justifyContent:'space-between', alignItems:'center',...extraStyle}}
            title={SERVICE_TIP}>
            <div style={{display:'flex', alignItems:'center', width:'100%'}}>
                <SectionTitle helpId='tapService' tip={SERVICE_TIP} title={title}/>
                <div style={{flexGrow: 1, marginRight: 3, maxWidth: 1000,  zIndex: 9999}}>
                    <CreatableSelect
                        options={tapOps} isClearable={true} onChange={onTapServiceOptionSelect}
                        placeholder={placeholder} theme={selectTheme} styles={ tableSelectStyleEnhanced}/>
                </div>
            </div>
        </div>
    );
}

function makeExtraWidgets(initArgs, selectBy, setSelectBy, tapBrowserState) {
    const extraWidgets = [
        (<ValidationField fieldKey='maxrec' key='maxrec' groupKey={TAP_PANEL_GROUP_KEY}
                         tooltip='Maximum number of rows to return (via MAXREC)' label= 'Row Limit:' labelWidth={0}
                         initialState= {{
                             value: Number(initArgs?.urlApi?.MAXREC) || Number(getAppOptions().tap?.defaultMaxrec ?? 50000),
                             validator: intValidator(0, getMaxrecHardLimit(), 'Maximum number of rows'),
                         }}
                         wrapperStyle={{marginLeft: 30, height: '100%', alignSelf: 'center'}}
                         style={{height: 17, width: 70}} />)
        ];
    if (selectBy==='basic') {
        extraWidgets.push( (<ExtraButton key='editADQL' text='Populate and edit ADQL'
                                         onClick={() => populateAndEditAdql(tapBrowserState)}
                                         style={{marginLeft: 10}} />));
    }
    else {
        extraWidgets.push( (<ExtraButton key='singleTable' text='Single Table (UI assisted)'
                                         onClick={() => setSelectBy('basic')}
                                         style={{marginLeft: 10}} />));

    }
    return extraWidgets;
}

function populateAndEditAdql(tapBrowserState,inAdql) {
    const adql = inAdql ?? getAdqlQuery(tapBrowserState);
    if (!adql) return;
    const {TAP_UPLOAD,uploadFile}=  isTapUpload(tapBrowserState) ? getUploadConstraint(tapBrowserState) : {};
    dispatchMultiValueChange(TAP_PANEL_GROUP_KEY,   //set adql and switch tab to ADQL
        [
            {fieldKey: 'defAdqlKey', value: adql},
            {fieldKey: 'adqlQuery', value: adql},
            {fieldKey: 'selectBy', value: 'adql'},
            {fieldKey: 'TAP_UPLOAD', value: TAP_UPLOAD},
            {fieldKey: 'uploadFile', value: uploadFile},
        ]
    );
}

const getTapServiceOptions= () => getTapServices(webApiUserAddedService).map(({label,value})=>({label:value, value, labelOnly:label}));

function getTableNameFromADQL(adql) {
    if (!adql) return;
    const adqlUp= adql.toUpperCase();
    const start= adqlUp.toUpperCase().indexOf('FROM ')+5;
    const end= adqlUp.lastIndexOf('WHERE');
    if  (start>5 && end>-1) {
        const tStr=  adql.substring(start,end)?.trim();
        if (tStr) return tStr.split(/[\s,]+/)[0]; // pull the first works out between the FROM and the WHERE
    }
    return undefined;
}

function getTitle(adql, serviceUrl) {
    const tname = getTableNameFromADQL(adql);
    const host= serviceUrl?.match(/.*:\/\/(.*)\/.*/i)?.[1]; // table name or service url
    if (tname && host) return `${tname} - ${host}`;
    return tname || host;
}

const disableRowLimitMsg = (
    <div style={{width: 260}}>
        Disabling the row limit is not recommended. <br/>
        You are about to submit a query without a TOP or WHERE constraint. <br/>
        This may results in a HUGE amount of data. <br/><br/>
        Are you sure you want to continue?
    </div>
);


function onTapSearchSubmit(request,serviceUrl,tapBrowserState) {
    const isADQL = (request.selectBy === 'adql');
    let adql;
    let isUpload;
    let serverFile;
    let uploadTableName;

    if (isADQL) {
        adql = request.adqlQuery;
        const {TAP_UPLOAD,uploadFile}= request;
        isUpload = Boolean(TAP_UPLOAD && uploadFile);
        serverFile = isUpload && TAP_UPLOAD[uploadFile].serverFile;
        uploadTableName = isUpload && TAP_UPLOAD[uploadFile].table;
    }
    else {
        adql = getAdqlQuery(tapBrowserState);
        isUpload = isTapUpload(tapBrowserState);
        serverFile = isUpload && getUploadServerFile(tapBrowserState);
        uploadTableName = isUpload && getUploadTableName(tapBrowserState);
    }

    if (!adql) return false;

    const maxrec = request.maxrec;
    const hasMaxrec = !isNaN(parseInt(maxrec));
    const doSubmit = () => {
        const adqlClean = adql.replace(/\s/g, ' ');    // replace all whitespaces with spaces
        const params = {serviceUrl, QUERY: adqlClean};
        if (isUpload) {
            params.UPLOAD= serverFile;
            params.adqlUploadSelectTable= uploadTableName;
        }
        if (hasMaxrec) params.MAXREC = maxrec;
        const treq = makeTblRequest('AsyncTapQuery', getTitle(adqlClean,serviceUrl), params);
        setNoCache(treq);
        if (!isADQL) set(treq, `META_INFO.${PREF_KEY}`, `${tapBrowserState.schemaName}-${tapBrowserState.tableName}`);
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
 * @param [showErrors]
 * @returns {string|null}
 */
function getAdqlQuery(tapBrowserState, showErrors= true) {
    const tableName = maybeQuote(tapBrowserState?.tableName, true);
    if (!tableName) return;
    const isUpload= isTapUpload(tapBrowserState);
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
        constraints += (addAnd ? ' AND ' : '') + `(${tableCol.where})`;
    }

    if (constraints) constraints = `WHERE ${constraints}`;

    // if we use TOP  when maxrec is set `${maxrec ? `TOP ${maxrec} `:''}`,
    // overflow indicator will not be included with the results
    // and we will not know if the results were truncated
    return `SELECT ${selcols} \nFROM ${fromTables} \n${constraints}`;
}