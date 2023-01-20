/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {once} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';
import FieldGroupUtils, {getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils.js';
import {makeSearchOnce} from 'firefly/util/WebUtil.js';
import {dispatchMultiValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {ExtraButton, FormPanel} from 'firefly/ui/FormPanel.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {intValidator} from 'firefly/util/Validate.js';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import CreatableSelect from 'react-select/creatable';
import {RadioGroupInputField} from 'firefly/ui/RadioGroupInputField.jsx';
import {makeTblRequest, setNoCache} from 'firefly/tables/TableRequestUtil.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr.js';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil.jsx';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr.js';
import {ConstraintContext, getHelperConstraints} from './Constraints.js';

import {tableColumnsConstraints} from 'firefly/ui/tap/TableColumnsConstraints.jsx';
import {commonSelectStyles, selectTheme} from 'firefly/ui/tap/Select.jsx';
import {
    getMaxrecHardLimit, tapHelpId, getTapServices,
    loadObsCoreSchemaTables, maybeQuote, defTapBrowserState
} from 'firefly/ui/tap/TapUtil.js';
import { SectionTitle, AdqlUI, BasicUI} from 'firefly/ui/tap/TableSelectViewPanel.jsx';
import {useFieldGroupMetaState} from '../SimpleComponent.jsx';



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
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const [serviceUrl, setServiceUrl]= useState(() => getInitServiceUrl(tapState,initArgs,tapOps));
    activateInitArgsAdqlOnce(tapState,initArgs);

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
        return FieldGroupUtils.bindToStore( TAP_PANEL_GROUP_KEY, (fields) => {
            setSelectBy(getFieldVal(TAP_PANEL_GROUP_KEY,'selectBy',selectBy));
            const ts= getTapBrowserState();
            setObsCoreTableModel(ts.obsCoreTableModel);

            searchFromAPIOnce( // searchFromAPIOnce only matters if the urlApi.execute is true
                () => validateAutoSearch(fields,initArgs,ts),
                () => setTimeout(() => clickFuncRef.clickFunc?.(), 5));
        });
    }, []);

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
                            extraWidgets={makeExtraWidgets(initArgs,selectBy,tapState)}
                            buttonStyle={{justifyContent: 'left'}}
                            submitBarStyle={{padding: '2px 3px 3px'}}
                            help_id = {tapHelpId('form')} >
                    <TapSearchPanelComponents {...{
                        initArgs, selectBy, serviceUrl, onTapServiceOptionSelect, titleOn, tapOps, obsCoreEnabled}} />
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



function TapSearchPanelComponents({initArgs, serviceUrl, onTapServiceOptionSelect, tapOps, titleOn=true, selectBy}) {

    const label= (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.labelOnly)) || '';
    const placeholder = serviceUrl ? `${serviceUrl} - Replace...` : 'Select TAP...';
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const hasObsCoreTable = obsCoreTableModel?.tableData?.data?.length > 0;

    const tableSelectStyleEnhanced= {
        ...tableSelectStyleEnhancedTemplate,
        placeholder: (provided) => ({ ...provided, ':before': makePlaceHolderBeforeStyle(label) })
    };

    const options = [
        {label: 'Single Table (UI assisted) ', value: 'basic', tooltip: 'Search a single table using a GUI query builder'},
        {label: 'Edit ADQL (advanced)', value: 'adql', tooltip: 'Enter or edit directly an ADQL query; supports complex queries including JOINs'}
    ];

    if (hasObsCoreTable) {
        options.push({label: 'Image Search (ObsTAP)', value: 'obscore', tooltip: 'Search the ObsTAP image metadata on this service with a specialized GUI query builder'});
    }

    let queryTypeEpilogue = '';
    if (selectBy === 'obscore') {
        let obsCoreTableName = 'ivoa.ObsCore';
        if (hasObsCoreTable){
            obsCoreTableName = obsCoreTableModel?.tableData?.data[0][1];
        }
        // This component does not know the actual name of the table, but it is guaranteed
        // that name.toLowerCase() === 'ivoa.ObsCore'.toLowerCase()
        queryTypeEpilogue =
            <div style={{display: 'inline-flex', marginTop: '4px'}}>(Searching the <pre style={{margin: '0px .5em'}}>{obsCoreTableName}</pre> table on this service...)</div>;
    }

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
                <div className='TapSearch__section' title={SERVICE_TIP}>
                    <SectionTitle title='1. Select TAP Service ' helpId='tapService' tip={SERVICE_TIP}/>
                    <div style={{flexGrow: 1, marginRight: 3, maxWidth: 1000,  zIndex: 9999}}>
                        <CreatableSelect
                            options={tapOps} isClearable={true} onChange={onTapServiceOptionSelect}
                            placeholder={placeholder} theme={selectTheme} styles={ tableSelectStyleEnhanced}/>
                    </div>
                </div>

                <div className='TapSearch__section' >
                    <SectionTitle title='2. Select Query Type  ' helpId='selectBy'/>
                    <RadioGroupInputField
                        fieldKey = 'selectBy'
                        initialState = {{ tooltip: 'Please select an interface type to use' }}
                        defaultValue = {initArgs?.urlApi?.selectBy}
                        options = {options}
                        wrapperStyle={{alignSelf: 'center'}}
                    />
                    {queryTypeEpilogue}
                </div>
                { selectBy === 'adql' ?
                    <AdqlUI {...{serviceUrl}}/> : <BasicUI  {...{serviceUrl, serviceLabel: label, selectBy, initArgs, obsCoreTableModel}}/>}
            </div>
        </FieldGroup>
    );
}

function makeExtraWidgets(initArgs, selectBy, tapBrowserState) {
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
    if (selectBy==='basic' || selectBy==='obscore') {
        extraWidgets.push( (<ExtraButton key='editADQL' text='Populate and edit ADQL'
                                         onClick={() => populateAndEditAdql(tapBrowserState)} style={{marginLeft: 30}} />));
    }
    return extraWidgets;
}

function populateAndEditAdql(tapBrowserState,inAdql) {
    const adql = inAdql ?? getAdqlQuery(tapBrowserState);
    if (!adql) return;
    dispatchMultiValueChange(TAP_PANEL_GROUP_KEY,   //set adql and switch tab to ADQL
        [
            {fieldKey: 'defAdqlKey', value: adql},
            {fieldKey: 'adqlQuery', value: adql},
            {fieldKey: 'selectBy', value: 'adql'}
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



function onTapSearchSubmit(request,serviceUrl,tapBrowserState) {
    const isADQL = (request.selectBy === 'adql');
    let adql = isADQL ? request.adqlQuery : getAdqlQuery(tapBrowserState);
    const maxrec = request.maxrec;

    if (adql) {
        const hasMaxrec = !isNaN(parseInt(maxrec));
        adql = adql.replace(/\s/g, ' ');    // replace all whitespaces with spaces
        const doSubmit = () => {
            const params = {serviceUrl, QUERY: adql};
            if (hasMaxrec) params.MAXREC = maxrec;
            const options = {};

            const treq = makeTblRequest('AsyncTapQuery', getTitle(adql,serviceUrl), params, options);
            setNoCache(treq);
            dispatchTableSearch(treq, {backgroundable: true, showFilters: true, showInfoButton: true});

        };
        if (!hasMaxrec && !adql.toUpperCase().match(/ TOP | WHERE /)) {
            const msg = (
                <div style={{width: 260}}>
                    Disabling the row limit is not recommended. <br/>
                    You are about to submit a query without a TOP or WHERE constraint. <br/>
                    This may results in a HUGE amount of data. <br/><br/>
                    Are you sure you want to continue?
                </div>
            );
            showYesNoPopup(msg,(id, yes) => {
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
    const helperFragment = getHelperConstraints(tapBrowserState);
    const tableCol = tableColumnsConstraints(tapBrowserState.columnsModel);

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
    const selcols = tableCol.selcols || '*';

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
    return `SELECT ${selcols} \nFROM ${tableName} \n${constraints}`;
}