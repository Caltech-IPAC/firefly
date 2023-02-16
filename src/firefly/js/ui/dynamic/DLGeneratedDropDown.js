import {isArray, isEmpty, once} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';
import {dispatchActiveTarget} from '../../core/AppDataCntlr.js';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {getJsonProperty} from '../../rpc/CoreServices.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchTableFetch, dispatchTableHighlight, dispatchTableSearch} from '../../tables/TablesCntlr.js';
import { getCellValue, getColumnIdx, getTblById, onTableLoaded, } from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {Logger} from '../../util/Logger.js';
import {cisxAdhocServiceUtype, getServiceDescriptors, standardIDs} from '../../util/VOAnalyzer.js';
import {pointEquals} from '../../visualize/Point.js';
import { ingestInitArgs,
    isSIAStandardID, makeFieldDefs, makeSearchAreaInfo, makeServiceDescriptorSearchRequest
} from './ServiceDefTools.js';
import {convertRequest, DynLayoutPanelTypes} from './DynamicUISearchPanel.jsx';
import {CIRCLE, POSITION, RANGE} from './DynamicDef.js';
import {FieldGroup} from '../FieldGroup.jsx';
import {FormPanel} from '../FormPanel.jsx';
import {FieldGroupTabs, Tab} from '../panel/TabPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {useStoreConnector} from '../SimpleComponent.jsx';

export const DL_UI_LIST= 'DL_UI_LIST';
const ACCESS_URL= 'access_url';
const SD= 'service_def';
const SEMANTICS= 'semantics';
const ID= 'id';
const DESC= 'description';

let regLoaded= false;
let regLoading= false;
let loadedTblIdCache={};

const findUrl = async () => {
    const servicesRootUrl= await getJsonProperty('inventory.serverURLAry');
    return servicesRootUrl;
};


//todo these next 3 functions could be refactored when this is generalize, we might pass an object with them
function getCollectionUrl(registryTblId, rowIdx) {
    const table= getTblById(registryTblId);
    if (!table) return;
    return getCellValue(table,rowIdx,'access_url');
}

function findUrlInReg(url, registryTblId) {
    const table= getTblById(registryTblId);
    if (!table) return false;
    const idx= getColumnIdx(table, 'access_url');
    if (idx===-1) return false;
    return table?.tableData?.data?.findIndex( (d) => d[idx]===url);
}

function makeRegistryRequest(url, registryTblId) {
    return makeFileRequest('registry', url, undefined,
        {
            pageSize: MAX_ROW,
            sortInfo: sortInfoString('facility_name'),
            tbl_id: registryTblId,
            META_INFO: {
                'col.facility_name.PrefWidth':15,
                'col.obs_collection.PrefWidth':15,
                'col.collection_label.PrefWidth':12,
                'col.instrument_name.PrefWidth':9,
                'col.coverage.PrefWidth':6,
                'col.band.PrefWidth':6,

                'col.facility_name.label':'Facility',
                'col.instrument_name.label':'Ins',
                'col.coverage.label':'Cov',
                'col.dataproduct_type.label':'Type',
                'col.collection_label.label':'Collections',
                'col.band.label':'Band',

                'col.dataproduct_type.PrefWidth':5,
                'col.obs_collection.visibility':'hidden',
                'col.description.visibility':'hidden',
                'col.desc_details.visibility':'hidden',
                'col.access_url.visibility':'hide',
                'col.info_url.visibility':'hidden',
                'col.access_format.visibility':'hidden',
            }
        }
    );
}



function isURLInRegistry(url, registryTblId) {
    return findUrlInReg(url, registryTblId)>-1;
}

async function doLoadRegistry(url, registryTblId) {
    try {
        dispatchTableFetch(makeRegistryRequest(url,registryTblId));
        const result= await onTableLoaded(registryTblId);
        const {tableModel}= result ?? {};
        if (tableModel.error) {
            tableModel.error = `Failed to get data for ${url}: ${tableModel.error}`;
        } else if (!tableModel.tableData) {
            tableModel.error = 'No data available';
        }
        regLoaded= true;
    } catch(reason) {
        const error = `Failed to get schemas for ${url}: ${reason?.message ?? reason}`;
        regLoading=false;
        return {error};
    }
}

async function loadRegistry(registryTblId) {
    if (regLoaded) return;
    regLoading= true;
    const url= await findUrl();
    await doLoadRegistry(url[0], registryTblId);
    regLoading=false;
}


export function DLGeneratedDropDown({initArgs={}}) {// eslint-disable-line no-unused-vars
    const COLLECTIONS_NAV_TABLE= 'COLLECTIONS_NAV_TABLE';
    const registryTblId= COLLECTIONS_NAV_TABLE;
    const {urlApi,searchParams}= initArgs;
    const {fetchedTables={}}=  useStoreConnector(() => getComponentState(DL_UI_LIST));

    const [useCurrentIdx,setUseCurrentIdx]= useState(false);
    const [loadedTblIds, setLoadedTblIds]=  useState(loadedTblIdCache);
    const [isRegLoaded, setIsRegLoaded]= useState(regLoaded);
    const [initialRow]= useState(() => getTblById(registryTblId)?.highlightedRow ?? 0);
    const currentIdx= useStoreConnector(() => getTblById(registryTblId)?.highlightedRow ?? -1);
    const [url,setUrl]= useState();

    loadedTblIdCache= loadedTblIds;

    useEffect(() => {// load registry and merge any already fetched tables
        const load= async () =>  {
            if (regLoaded || regLoading) return;
            await loadRegistry(registryTblId);
            setIsRegLoaded(true);
        };
        void load();
        if (!isEmpty(fetchedTables)) setLoadedTblIds({...loadedTblIds, ...fetchedTables});
    },[fetchedTables]);

    useEffect(() => {
        const inUrl=  urlApi?.url ?? searchParams?.url;
        if (!inUrl) {
            setUseCurrentIdx(true);
            return;
        }
        const newUrl= isArray(inUrl) ? inUrl[0] : inUrl;
        if (!newUrl) return;
        if (isRegLoaded) {
            const rowIdx = findUrlInReg(newUrl, registryTblId);
            if (rowIdx > -1) {
                setUseCurrentIdx(true);
                dispatchTableHighlight(registryTblId, rowIdx);
            }
            else {
                setUrl(newUrl);
            }
        }
        else {
            setUrl(newUrl);
        }
    }, [urlApi?.url, searchParams?.url, isRegLoaded]);

    useEffect(() => {
        if (!useCurrentIdx && currentIdx>-1 && currentIdx!==initialRow) setUseCurrentIdx(true);
    }, [currentIdx]);

    useEffect(() => {
        if (!isRegLoaded || !useCurrentIdx) return;
        const newUrl= getCollectionUrl(registryTblId, currentIdx);
        if (newUrl) setUrl(newUrl);
    }, [currentIdx, isRegLoaded, useCurrentIdx]);

    return <DLGeneratedDropDownTables {...{registryTblId,isRegLoaded, loadedTblIds, setLoadedTblIds, url, initArgs}}/>;
}

export function DLGeneratedDropDownTables({registryTblId, isRegLoaded, loadedTblIds, setLoadedTblIds, url, initArgs}) {

    const currentTblId= loadedTblIds?.[url];
    useEffect(() => {
        if (isRegLoaded && !currentTblId) {
            const loadOptions=  {META_INFO:{[MetaConst.LOAD_TO_DATALINK_UI]: 'true'}};
            if (!url) return;
            const req= makeFileRequest('Data link UI', url, undefined, loadOptions);
            const {tbl_id}= req.META_INFO;
            dispatchTableFetch(req);
            onTableLoaded(tbl_id).then(() => {
                setLoadedTblIds({...loadedTblIds, [url]:tbl_id});
            });

        }
    }, [currentTblId,url,isRegLoaded]);

    const sideBar= <SideBarTable {...{registryTblId}}/>;
    const regHasUrl= isURLInRegistry(url,registryTblId);
    return (
        <div className='SearchPanel' style={{width:'100%', height:'100%'}}>
            <DLGeneratedTableSearch {...{currentTblId, initArgs, sideBar, regHasUrl, url}}/>
        </div>
    );
}

const ServDescPanel= ({fds, style, desc, docRows}) => {
    return  (
        <div style={{display:'flex', flexDirection:'column', justifyContent:'space-between', ...style}}>
            <div style={{fontSize:'large', fontWeight:'bold', alignSelf:'center', padding:'5px 0 4px 0'}}>
                {desc}
            </div>
            <DynLayoutPanelTypes.Simple fieldDefAry={fds} popupHiPS={false} style={{height:'100%'}}/>
            <div style={{fontSize:'larger', padding:'1px 5px 5px 0', alignSelf:'flex-end'}}>
                {
                    docRows.map( (row, idx) => (
                        <a href={row.accessUrl} key={idx + ''} target={'documentation'} >
                            <span style={{fontStyle:'italic'}}>Documentation: </span>
                            <span> {`${row.desc}`}</span>
                        </a> ))
                }
            </div>
        </div>
    );
};


const TabView= ({tabsKey, searchObjFds,qAna, docRows}) => (
    <FieldGroupTabs style ={{height:'100%', width:'100%'}} initialState={{ value:searchObjFds[0].ID}} fieldKey={tabsKey}>
        {
            searchObjFds.map((sFds) => {
                const {fds, idx, ID, desc}= sFds;
                return (
                    <Tab name={`${qAna.primarySearchDef[idx].desc}`} id={ID} key={idx+''}>
                        <ServDescPanel{...{fds, style:{width:'100%'}, desc, docRows}}/>
                    </Tab>
                );
            })
        }
    </FieldGroupTabs>
);

function SideBarTable({registryTblId}) {
    return (
        <div style={{display:'flex', flexDirection:'column', backgroundColor:'rgb(255,255,255)'}}>
            <div style={{fontSize:'large', fontWeight:'bold', alignSelf:'center', padding:'6px 0 0 0'}}>
                Choose Data Set
            </div>
            <div style={{minWidth:460, flexGrow:1, backgroundColor:'inherit', padding: '4px 4px 0 2px'}}>
                <TablePanel {...{
                    key:registryTblId,
                    tbl_id:registryTblId,
                    showToolbar: false,
                    selectable:false,
                    showFilters:true,
                    showOptions: false,
                    showUnits: false,
                    showTypes: false,
                    textView: false,
                    showOptionButton: false
                }}/>
            </div>
        </div>
    );
}


const initTargetOnce= once((wp) => wp && dispatchActiveTarget(wp));
const doSearchOnce= once((clickFunc) => clickFunc() );


function DLGeneratedTableSearch({currentTblId, initArgs, sideBar, regHasUrl, url}) {

    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    useEffect(() => {
        if (initArgs?.urlApi?.execute) {
            setTimeout(() => {
                doSearchOnce(clickFuncRef.clickFunc);
            },10);
        }
    }, []);


    const qAna= analyzeQueries(currentTblId);
    const tabsKey= 'Tabs='+currentTblId;
    const fdAry= qAna?.primarySearchDef.map( (fd) => {
        const {serviceDef}= fd; //todo handle case with only an access url
        const standId= (serviceDef.standardID??'').toLowerCase();
        const utype= (serviceDef.utype||'').toLowerCase();
        let fdEntryAry;
        if (utype===cisxAdhocServiceUtype && standId.startsWith(standardIDs.tap) && serviceDef.cisxTokenSub) {
            fdEntryAry= makeFieldDefs(serviceDef.cisxTokenSub, undefined, makeSearchAreaInfo(serviceDef.cisxUI), false);
        }
        else {
            fdEntryAry= makeFieldDefs(serviceDef.serDefParams, undefined, makeSearchAreaInfo(serviceDef.cisxUI), true);
        }
        if (!isEmpty(initArgs.urlApi)) {
            const originalWp= fdEntryAry.find((fd) => fd.type===POSITION)?.initValue ?? fdEntryAry.find((fd) => fd.type===CIRCLE)?.targetDetails?.centerPt;
            const initFdEntryAry= ingestInitArgs(fdEntryAry,initArgs.urlApi);
            const initWp= initFdEntryAry.find((fd) => fd.type===POSITION)?.initValue ?? initFdEntryAry.find((fd) => fd.type===CIRCLE)?.targetDetails?.centerPt;
            if (!pointEquals(originalWp,initWp)) initTargetOnce(initWp);
            return initFdEntryAry;
        }
        else {
            return fdEntryAry;
        }

    });

    const docRows= qAna?.urlRows.filter( ({semantic}) => semantic?.toLowerCase().endsWith('documentation'));


    const searchObjFds= fdAry
        ?.map((fds,idx) => {
            fds= fds.filter( (entry) => entry.type!==RANGE);
            const primeSd= qAna.primarySearchDef[idx].serviceDef;
            const desc= qAna.primarySearchDef[idx].desc;
            const {standardID='', ID}= primeSd;
            return {fds, standardID, idx, ID, desc};
        });

    const submitSearch= (r) => {
        const {fds, standardID, idx}= searchObjFds.length===1 ?
            searchObjFds[0] :
            searchObjFds.find(({ID}) => ID===r[tabsKey]) ?? {};

        const convertedR= convertRequest(r,fds,isSIAStandardID(standardID));

        const numKeys= [...new Set(fds.map( ({key}) => key))].length;
        if (Object.keys(convertedR).length<numKeys) {
            showInfoPopup('Please enter all of the fields', 'Error');
            return false;
        }
        handleSearch(convertedR,qAna,idx);
        return true;
    };

    return (
        <div style={{display: 'flex', flexDirection:'column', width:'100%', height:'100%', justifyContent:'center'}}>
            <FormPanel submitText = 'Search' groupKey = 'DL_UI'
                       onSubmit = {submitSearch}
                       params={{hideOnInvalid: false}}
                       getDoOnClickFunc={(clickFunc) => clickFuncRef.clickFunc= clickFunc}
                       onError = {() => showInfoPopup('Fix errors and search again', 'Error') }
                       help_id  = {'todo-AtReplaceHelpId-todo'}>
                <div className='SearchPanel' style={{width:'100%', height:'100%'}}>
                    {sideBar}
                    {qAna ?
                        <FieldGroup groupKey='DL_UI' keepState={true} style={{height:'100%', width:'100%'}}>
                            {searchObjFds.length===1 ?
                                <ServDescPanel{...{initArgs, fds:searchObjFds[0].fds, style:{width:'100%',height:'100%'}, desc:searchObjFds[0].desc, docRows}} /> :
                                <TabView{...{initArgs, tabsKey,searchObjFds,qAna, docRows}}/>}
                        </FieldGroup>
                        :
                        regHasUrl ?
                            <div className='loading-mask'/> :
                            <div style={{alignSelf:'center', fontSize:'large', paddingLeft:40}}>
                                {`No collections to load from: ${url}`}
                            </div>
                    }
                </div>
            </FormPanel>
        </div>
    );
}


function handleSearch(request, qAna, idx) {
    const primeSd= qAna.primarySearchDef[idx].serviceDef;
    const primeSdId= primeSd.ID ?? primeSd.id;

    const concurrentSDAry= qAna.concurrentSearchDef.filter( (searchDef) => {
        const id= searchDef.serviceDef.ID ?? searchDef.serviceDef.id ?? '';
        return id.startsWith(primeSdId);
    });

    const tableRequestAry= makeAllSearchRequest(request, primeSd,concurrentSDAry);

    tableRequestAry.forEach( (dataTableReq) => {
        Logger('DLGeneratedDropDown').debug(dataTableReq);
        if (dataTableReq) {
            dispatchTableSearch(dataTableReq, {showFilters: true, showInfoButton: true }); //todo are the options the default?
        }
    });
}

function makeAllSearchRequest(request, primeSd, concurrentSDAry) {
    const primeRequest= makeServiceDescriptorSearchRequest(request,primeSd);
    const concurrentRequestAry= concurrentSDAry
        .map( (sd) => makeServiceDescriptorSearchRequest(request,sd))
        .filter( (url) => url);

    return [primeRequest, ...concurrentRequestAry];
}




function analyzeQueries(tbl_id) {
    const table= getTblById(tbl_id);
    const sdAry= getServiceDescriptors(table,false);
    if (!sdAry || !table) return;
    const {data=[]}= table.tableData ?? {};

    const semIdx= getColumnIdx(table,SEMANTICS,true);
    const sdIdx= getColumnIdx(table,SD,true);
    const idIdx= getColumnIdx(table,ID,true);
    const accessUrlIdx= getColumnIdx(table,ACCESS_URL,true);
    const descIdx= getColumnIdx(table,DESC,true);

    const makeSearchDef= (row) => {
        const serviceDef= sdAry.find( (sd) => {
            const id= sd.ID ?? sd.id ?? '';
            return id===row[sdIdx];
        });
        return {serviceDef, accessUrl: row[accessUrlIdx], id:row[idIdx], desc:row[descIdx], semantic: row[semIdx] };
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