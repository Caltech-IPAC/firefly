/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray, once} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';
import FieldGroupUtils, {getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils.js';
import {makeSearchOnce} from 'firefly/util/WebUtil.js';
import {dispatchMultiValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {ExtraButton, FormPanel} from 'firefly/ui/FormPanel.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {intValidator} from 'firefly/util/Validate.js';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import CreatableSelect from 'react-select/creatable/dist/react-select.esm.js';
import {RadioGroupInputField} from 'firefly/ui/RadioGroupInputField.jsx';
import {makeTblRequest, setNoCache} from 'firefly/tables/TableRequestUtil.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr.js';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil.jsx';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr.js';

import {tableColumnsConstraints} from 'firefly/ui/tap/TableColumnsConstraints.jsx';
import {skey, tableSearchMethodsConstraints} from 'firefly/ui/tap/TableSearchMethods.jsx';
import {commonSelectStyles, selectTheme} from 'firefly/ui/tap/Select.jsx';
import {getMaxrecHardLimit, getTapBrowserState, tapHelpId, TAP_SERVICES_FALLBACK} from 'firefly/ui/tap/TapUtil.js';
import { gkey, SectionTitle, AdqlUI, BasicUI} from 'firefly/ui/tap/TableSelectViewPanel.jsx';



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
const activateInitArgsAdqlOnce= once((initArgs) => initArgs?.adql && setTimeout(() => populateAndEditAdql(initArgs.adql), 5));

/** if an extra service is found from the api that is not in the list then set webApiUserAddedService */
const initApiAddedServiceOnce= once((initArgs) => {
    if (initArgs?.service) {
        const listedEntry= getTapServiceOptions().find( (e) => e.value===initArgs.service);
        if (!listedEntry) webApiUserAddedService = {label: initArgs.service, value: initArgs.service};
    }
});

/**
 * Return true it the initArgs has populated the tap fields so a valid search can be executed.
 * @param fields
 * @param initArgs
 * @return {boolean} true if the field are valid to doa search
 */
function validateAutoSearch(fields, initArgs) {
    const {columnsModel} = getTapBrowserState();
    if (columnsModel && getAdqlQuery(false)) {
        const searchMethodFields= FieldGroupUtils.getGroupFields(skey);
        if (searchMethodFields) {
            const adqlFragment = tableSearchMethodsConstraints(columnsModel);
            if (adqlFragment && adqlFragment.valid) {
                const constraintInitArgs= ['WorldPt', 'radiusInArcSec']; // this should grow as we support more params in initArgs
                const usesWhere= Object.keys(initArgs).find( (i) => constraintInitArgs.includes(i));
                return usesWhere ? Boolean(adqlFragment.where) : true;
            }
        }
    }
    return Boolean(initArgs.adql);
}

//----------
//----------
// end of once api functions
//----------
//----------

function getInitServiceUrl(initArgs,tapOps) {
    let {serviceUrl=tapOps[0].value} = getTapBrowserState();
    initServiceUsingAPIOnce(true, () => {
        if (initArgs?.service) serviceUrl= initArgs.service;
    });
    return serviceUrl;
}

export function TapSearchPanel({initArgs= {}, titleOn=true}) {
    if (!initArgs?.execute) searchFromAPIOnce(true); // if not execute then mark as done, i.e. disable any auto searching
    initApiAddedServiceOnce(initArgs);  // only look for the extra service the first time
    const tapOps= getTapServiceOptions();
    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    const [selectBy, setSelectBy]= useState('basic');
    const [obsCoreTables, setObsCoreTables] = useState();
    const [serviceUrl, setServiceUrl]= useState(() => getInitServiceUrl(initArgs,tapOps));
    activateInitArgsAdqlOnce(initArgs);

    const obsCoreEnabled = obsCoreTables?.length > 0;

    const onTapServiceOptionSelect= (selectedOption) => {
        if (!selectedOption) return;
        dispatchMultiValueChange(gkey,
            [
                {fieldKey: 'defAdqlKey', value: ''},
                {fieldKey: 'adqlQuery', placeholder: '', value: ''}
            ]
        );
        setServiceUrl(selectedOption.value);
        setObsCoreTables(undefined);
    };

    useEffect(() => {
        return FieldGroupUtils.bindToStore( gkey, (fields) => {
            setSelectBy(getFieldVal(gkey,'selectBy',selectBy));
            const obsCoreTables = getTapBrowserState().obsCoreTables;
            setObsCoreTables(obsCoreTables);
            searchFromAPIOnce( () => validateAutoSearch(fields,initArgs), () => setTimeout(() => clickFuncRef.clickFunc?.(), 5));
        });
    }, []);

    return (
        <div style={{width: '100%'}}>
            <FormPanel  inputStyle = {{display: 'flex', flexDirection: 'column', backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                        groupKey={gkey}
                        getDoOnClickFunc={(clickFunc) => clickFuncRef.clickFunc= clickFunc}
                        params={{hideOnInvalid: false}}
                        onSubmit={(request) => onTapSearchSubmit(request, serviceUrl)}
                        extraWidgets={makeExtraWidgets(initArgs,selectBy)}
                        buttonStyle={{justifyContent: 'left'}}
                        submitBarStyle={{padding: '2px 3px 3px'}}
                        help_id = {tapHelpId('form')} >
                <TapSearchPanelComponents {...{
                    initArgs, selectBy, serviceUrl, onTapServiceOptionSelect, titleOn, tapOps, obsCoreEnabled}} />
            </FormPanel>
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



function TapSearchPanelComponents({initArgs, serviceUrl, onTapServiceOptionSelect, tapOps, titleOn=true, selectBy, obsCoreEnabled}) {

    const label= (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.labelOnly)) || '';
    const placeholder = serviceUrl ? `${serviceUrl} - Replace...` : 'Select TAP...';

    const tableSelectStyleEnhanced= {
        ...tableSelectStyleEnhancedTemplate,
        placeholder: (provided) => ({ ...provided, ':before': makePlaceHolderBeforeStyle(label) })
    };

    const options = [
        {label: 'Single Table (UI assisted) ', value: 'basic', tooltip: 'Search a single table using a GUI query builder'},
        {label: 'Edit ADQL (advanced)', value: 'adql', tooltip: 'Enter or edit directly an ADQL query; supports complex queries including JOINs'}
    ];

    if (obsCoreEnabled) {
        options.push({label: 'Image Search (ObsTAP)', value: 'obscore', tooltip: 'Search the ObsTAP image metadata on this service with a specialized GUI query builder'});
    }

    let queryTypeEpilogue = '';
    if (selectBy === 'obscore') {
        // This component does not know the actual name of the table, but it is guaranteed
        // that name.toLowerCase() === 'ivoa.ObsCore'.toLowerCase()
        queryTypeEpilogue =
            <div style={{display: 'inline-flex', marginTop: '4px'}}>(Searching the <pre style={{margin: '0px .5em'}}>ivoa.ObsCore</pre> table on this service...)</div>;
    }

    return (
        <FieldGroup groupKey={gkey} keepState={true} style={{flexGrow: 1, display: 'flex'}}>
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
                        initialState = {{
                            defaultValue: 'basic',
                            options: options,
                            tooltip: 'Please select an interface type to use'
                        }}
                        options = {options}
                        wrapperStyle={{alignSelf: 'center'}}
                    />
                    {queryTypeEpilogue}
                </div>
                {(selectBy === 'basic' || selectBy === 'obscore') && <BasicUI  serviceUrl={serviceUrl} selectBy={selectBy} initArgs={initArgs}/>}
                {selectBy === 'adql' && <AdqlUI serviceUrl={serviceUrl}/>}
            </div>
        </FieldGroup>
    );
}

function makeExtraWidgets(initArgs, selectBy) {
    const extraWidgets = [
        (<ValidationField fieldKey='maxrec' key='maxrec' groupKey={gkey}
                         tooltip='Maximum number of rows to return (via MAXREC)' label= 'Row Limit:' labelWidth={0}
                         initialState= {{
                             value: Number(initArgs.MAXREC) || Number(getAppOptions().tap?.defaultMaxrec ?? 50000),
                             validator: intValidator(0, getMaxrecHardLimit(), 'Maximum number of rows'),
                         }}
                         wrapperStyle={{marginLeft: 30, height: '100%', alignSelf: 'center'}}
                         style={{height: 17, width: 70}} />)
        ];
    if (selectBy==='basic' || selectBy==='obscore') {
        extraWidgets.push( (<ExtraButton key='editADQL' text='Populate and edit ADQL'
                                         onClick={() => populateAndEditAdql()} style={{marginLeft: 30}} />));
    }
    return extraWidgets;
}

function populateAndEditAdql(inAdql) {
    const adql = inAdql ?? getAdqlQuery();
    if (!adql) return;
    dispatchMultiValueChange(gkey,   //set adql and switch tab to ADQL
        [
            {fieldKey: 'defAdqlKey', value: adql},
            {fieldKey: 'adqlQuery', value: adql},
            {fieldKey: 'selectBy', value: 'adql'}
        ]
    );
}

const hasElements= (a) => Boolean(isArray(a) && a?.length);
const getTapServiceOptions= () => getTapServices().map(({label,value})=>({label:value, value, labelOnly:label}));

function getTapServices() {
    const tapServices = getAppOptions()?.tap?.services;
    const additionalServices = getAppOptions()?.tap?.additional?.services;
    const retVal= hasElements(tapServices) ? [...tapServices] : [...TAP_SERVICES_FALLBACK];
    hasElements(additionalServices) && retVal.unshift(...additionalServices);
    webApiUserAddedService && retVal.push(webApiUserAddedService);
    return retVal;
}


function onTapSearchSubmit(request,serviceUrl) {
    const isADQL = (request.selectBy === 'adql');
    let adql = undefined;
    let title = undefined;
    const maxrec = request.maxrec;
    if (isADQL) {
        adql = request.adqlQuery;
        // use service name for title
        const found = serviceUrl.match(/.*:\/\/(.*)\/.*/i);
        title = found && found[1];
    } else {
        adql = getAdqlQuery();
        title = request.tableName;
    }
    if (adql) {
        const hasMaxrec = !isNaN(parseInt(maxrec));
        adql = adql.replace(/\s/g, ' ');    // replace all whitespaces with spaces
        const doSubmit = () => {
            const params = {serviceUrl, QUERY: adql};
            if (hasMaxrec) params.MAXREC = maxrec;
            const options = {};

            const treq = makeTblRequest('AsyncTapQuery', title, params, options);
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



function getAdqlQuery(showErrors= true) {
    const tableName = FieldGroupUtils.getGroupFields(gkey)?.tableName?.value;
    if (!tableName) return;

    const {columnsModel} = getTapBrowserState();

    // spatial and temporal constraints
    const whereFragment = tableSearchMethodsConstraints(columnsModel);
    if (!whereFragment.valid) {
        return null;
    }
    let constraints = whereFragment.where || '';
    const addAnd = Boolean(constraints);

    // table column constraints and column selections
    const adqlFragment = tableColumnsConstraints(columnsModel);
    if (!adqlFragment.valid && showErrors) {
        showInfoPopup(adqlFragment.message, 'Error');
        return null;
    }
    if (adqlFragment.where) {
        constraints += (addAnd ? ' AND ' : '') + `(${adqlFragment.where})`;
    }
    const selcols = adqlFragment.selcols || '*';

    if (constraints) {
        constraints = `WHERE ${constraints}`;
    }

    // if we use TOP  when maxrec is set `${maxrec ? `TOP ${maxrec} `:''}`,
    // overflow indicator will not be included with the results
    // and we will not know if the results were truncated
    return `SELECT ${selcols} FROM ${tableName} ${constraints}`;
}