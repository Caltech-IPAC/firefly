import {isArray, isEmpty, once} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';
import {makeWorldPt, visRoot} from '../../api/ApiUtilImage.jsx';
import {dispatchActiveTarget} from '../../core/AppDataCntlr.js';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils.js';
import {getJsonProperty} from '../../rpc/CoreServices.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchTableFetch, dispatchTableHighlight, dispatchTableSearch} from '../../tables/TablesCntlr.js';
import { getCellValue, getColumnIdx, getTblById, onTableLoaded, } from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {Logger} from '../../util/Logger.js';
import {cisxAdhocServiceUtype, getServiceDescriptors, standardIDs} from '../../util/VOAnalyzer.js';
import {getBoolean, toBoolean} from '../../util/WebUtil.js';
import CoordSys from '../../visualize/CoordSys.js';
import {ensureHiPSInit, resolveHiPSIvoURL} from '../../visualize/HiPSListUtil.js';
import {getHiPSZoomLevelForFOV} from '../../visualize/HiPSUtil.js';
import {dispatchChangeCenterOfProjection, dispatchChangeHiPS, dispatchZoom} from '../../visualize/ImagePlotCntlr.js';
import {getPlotViewById, primePlot} from '../../visualize/PlotViewUtil.js';
import {pointEquals} from '../../visualize/Point.js';
import {isHiPS} from '../../visualize/WebPlot.js';
import {UserZoomTypes} from '../../visualize/ZoomUtil.js';
import {confirmDLMenuItem} from './FetchDatalinkTable.js';
import { ingestInitArgs,
    isSIAStandardID, makeFieldDefs, makeSearchAreaInfo, makeServiceDescriptorSearchRequest
} from './ServiceDefTools.js';
import {convertRequest, DynLayoutPanelTypes, findTargetFromRequest} from './DynamicUISearchPanel.jsx';
import {CIRCLE, POSITION, RANGE} from './DynamicDef.js';
import {FieldGroup} from '../FieldGroup.jsx';
import {FormPanel} from '../FormPanel.jsx';
import {FieldGroupTabs, Tab} from '../panel/TabPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {useStoreConnector} from '../SimpleComponent.jsx';

import './DLGeneratedDropDown.css';
import SHOW_RIGHT from 'images/show-right-3.png';
import HIDE_LEFT from 'images/hide-left-3.png';

export const DL_UI_LIST= 'DL_UI_LIST';
const ACCESS_URL= 'access_url';
const SD= 'service_def';
const SEMANTICS= 'semantics';
const ID= 'id';
const DESC= 'description';
const HIPS_PLOT_ID= 'dlGeneratedHipsPlotId';

let regLoaded= false;
let regLoading= false;
let loadedTblIdCache={};
const linkStyle= {whiteSpace:'noWrap', fontStyle:'italic', fontWeight: 'bold'};

const findUrl = async () => {
    const servicesRootUrl= await getJsonProperty('inventory.serverURLAry');
    return servicesRootUrl;
};


//todo these next 4 functions could be refactored when this is generalize, we might pass an object with them
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

const getDataSetChooserTitle= () =>
    ({
        title: 'Choose Data Collection',
        details: 'Click on data collection to search; filter or sort table to find a data set.'
   });

function makeRegistryRequest(url, registryTblId) {
    return makeFileRequest('registry', url, undefined,
        {
            pageSize: MAX_ROW,
            sortInfo: sortInfoString('facility_name'),
            tbl_id: registryTblId,
            META_INFO: {
                'col.facility_name.PrefWidth':6,
                'col.collection_label.PrefWidth':12,
                'col.instrument_name.PrefWidth':9,
                'col.coverage.PrefWidth':8,
                'col.band.PrefWidth':13,
                'col.dataproduct_type.PrefWidth':5,

                'col.obs_collection.PrefWidth':15,

                'col.facility_name.label':'Facility',
                'col.instrument_name.label':'Inst.',
                'col.coverage.label':'Type',
                'col.dataproduct_type.label':'Data',
                'col.collection_label.label':'Collection',
                'col.band.label':'Bands',

                'col.obs_collection.visibility':'hidden',
                'col.description.visibility':'hidden',
                'col.desc_details.visibility':'hidden',
                'col.access_url.visibility':'hide',
                'col.info_url.visibility':'hidden',
                'col.access_format.visibility':'hidden',

                [MetaConst.IMAGE_SOURCE_ID] : 'FALSE'
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

    useEffect(() => {
        setTimeout(() => confirmDLMenuItem(), 20);
    }, []);

    useEffect(() => {// load registry and merge any already fetched tables
        const load= async () =>  {
            if (regLoaded || regLoading) return;
            await loadRegistry(registryTblId);
            await ensureHiPSInit();
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

    const [sideBarShowing, setSideBarShowing]= useState(true);
    const currentTblId= loadedTblIds?.[url];
    useEffect(() => {
        if (isRegLoaded && !currentTblId) {
            const loadOptions=  {META_INFO:{
                    [MetaConst.LOAD_TO_DATALINK_UI]: 'true',
                    [MetaConst.IMAGE_SOURCE_ID] : 'FALSE'}
            };
            if (!url) return;
            const req= makeFileRequest('Data link UI', url, undefined, loadOptions);
            const {tbl_id}= req.META_INFO;
            dispatchTableFetch(req);
            onTableLoaded(tbl_id).then(() => {
                setLoadedTblIds({...loadedTblIds, [url]:tbl_id});
            });

        }
    }, [currentTblId,url,isRegLoaded]);

    useEffect(() => {
        const showChooser= toBoolean(initArgs.urlApi?.showChooser ?? true, false, ['true','']);
        setSideBarShowing(showChooser);
    },[]);


    const sideBar= <SideBarTable {...{registryTblId, setSideBarShowing}}/>;
    const regHasUrl= isURLInRegistry(url,registryTblId);
    return (
        <div className='SearchPanel' style={{width:'100%', height:'100%'}}>
            <DLGeneratedTableSearch {...{currentTblId, initArgs, sideBar, regHasUrl, url, isRegLoaded,sideBarShowing, setSideBarShowing}}/>
        </div>
    );
}


function SearchTitle({desc, isAllSky, sideBarShowing, setSideBarShowing}) {
    const titleDiv= (
        <div>
            { !isAllSky ? desc :
                <>
                    <span>{desc}</span>
                    <span className='DLGeneratedDropDown__section--title-allsky' style={{paddingLeft: 20}}>
                        (Covers Whole Sky)
                    </span>
                </> }
        </div>
    );

    if (sideBarShowing) {
        return (
            <div style={{display:'flex', flexDirection:'row', alignItems:'center', justifyContent:'center'}}>
                <div className='DLGeneratedDropDown__section--title'>
                    {titleDiv}
                </div>
            </div>
        );
    }
    else {
        return (
            <div style={{display:'flex', flexDirection:'row', alignItems:'center', justifyContent:'flex-start'}}>
                <ExpandButton style={linkStyle} icon={SHOW_RIGHT} text={'Show Other Data Sets'} onClick={()  => setSideBarShowing(true)}/>
                <div style={{display:'flex', flexDirection:'row', flexGrow:1, justifyContent:'center'}}>
                    <div className='DLGeneratedDropDown__section--title' style={{marginLeft:-60}}>
                        {titleDiv}
                    </div>
                </div>
            </div>
        );

    }

}


function ServDescPanel({fds, style, desc, setSideBarShowing, sideBarShowing, docRows, clickFuncRef, submitSearch, isAllSky})  {

    const docRowsComponents= (
        <div key='help' style={{fontSize:'larger', padding:'0 10px 3px 0', alignSelf:'flex-end'}}>
            {
                docRows.map( (row, idx) => (
                    <a href={row.accessUrl} key={idx + ''} target={'documentation'} >
                        <span style={{fontStyle:'italic'}}>Documentation: </span>
                        <span> {`${row.desc}`}</span>
                    </a> ))
            }
        </div>);

    const Wrapper= ({children}) => (
        <FormPanel submitText = 'Search' groupKey = 'DL_UI'
                   onSubmit = {submitSearch}
                   params={{hideOnInvalid: false}}
                   inputStyle={{border:'none', padding:0, marginBottom:5}}
                   getDoOnClickFunc={(clickFunc) => clickFuncRef.clickFunc= clickFunc}
                   onError = {() => showInfoPopup('Fix errors and search again', 'Error') }
                   extraWidgets={[docRowsComponents]}
                   help_id  = {'search-collections-general'}>
            <div style={{
                display:'flex',
                flexDirection:'column',
                alignItems:'center',
            }}>
                {children}
            </div>
        </FormPanel>
    );


    return  (
        <div style={{display:'flex', flexDirection:'column', justifyContent:'space-between', ...style}}>

            <SearchTitle {...{desc,isAllSky,sideBarShowing,setSideBarShowing}}/>
            <DynLayoutPanelTypes.Inset fieldDefAry={fds} plotId={HIPS_PLOT_ID} style={{height:'100%', marginTop:4}}
                                       WrapperComponent={Wrapper}
            />
        </div>
    );
}


const TabView= ({tabsKey, setSideBarShowing, sideBarShowing, searchObjFds,qAna, docRows, isAllSky, clickFuncRef, submitSearch}) => (
    <FieldGroupTabs style ={{height:'100%', width:'100%'}} initialState={{ value:searchObjFds[0].ID}} fieldKey={tabsKey}>
        {
            searchObjFds.map((sFds) => {
                const {fds, idx, ID, desc}= sFds;
                return (
                    <Tab name={`${qAna.primarySearchDef[idx].desc}`} id={ID} key={idx+''}>
                        <ServDescPanel{...{fds, setSideBarShowing, sideBarShowing, style:{width:'100%'}, desc, docRows,
                            isAllSky, clickFuncRef, submitSearch}}/>
                    </Tab>
                );
            })
        }
    </FieldGroupTabs>
);

function SideBarTable({registryTblId, setSideBarShowing}) {
    return (
        <div style={{display:'flex', flexDirection:'column'}}>
            <div style={{display:'flex', flexDirection:'row', alignItems:'center'}}>
                {<ExpandButton style={linkStyle} icon={HIDE_LEFT} text={'Hide'} tip={'Hide data set chooser'}
                               onClick={()  => setSideBarShowing(false)}/> }
                <div className='DLGeneratedDropDown__section--title' style={{marginLeft:110}} >
                    {getDataSetChooserTitle().title}
                </div>
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
            <div style={{fontSize:'larger', display:'flex', justifyContent:'space-around', padding: '12px 0 5px 0'} }>
                <div>
                    {getDataSetChooserTitle().details}
                </div>
            </div>
        </div>
    );
}


const initTargetOnce= once((wp) => wp && dispatchActiveTarget(wp));
const doSearchOnce= once((clickFunc) => clickFunc() );


function DLGeneratedTableSearch({currentTblId, initArgs, sideBar, regHasUrl, url, sideBarShowing, isRegLoaded, setSideBarShowing}) {


    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    const qAna= analyzeQueries(currentTblId);
    const tabsKey= 'Tabs='+currentTblId;

    useEffect(() => {
        if (initArgs?.urlApi?.execute) {
            setTimeout(() => {
                doSearchOnce(clickFuncRef.clickFunc);
            },10);
        }
    }, []);


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

    const findFieldDefInfo= (request) =>
        searchObjFds.length===1 ? searchObjFds[0] : searchObjFds.find(({ID}) => ID===request[tabsKey]) ?? {};


    useEffect(() => { // on table change: recenter hips if no target entered, change hips if new one is specified
        if (!currentTblId) return;
        const plot= primePlot(visRoot(),HIPS_PLOT_ID);
        if (!plot || !isHiPS(plot)) return;
        if (!qAna?.primarySearchDef?.[0]?.serviceDef?.cisxUI) return;
        const {cisxUI}= qAna.primarySearchDef[0].serviceDef;
        const raStr= cisxUI.find( (e) => e.name==='hips_initial_ra')?.value;
        const decStr= cisxUI.find( (e) => e.name==='hips_initial_dec')?.value;
        const ucdStr= cisxUI.find( (e) => e.name==='hips_initial_ra')?.UCD;
        const fovStr= cisxUI.find( (e) => e.name==='hips_initial_fov')?.value;
        const hipsUrl= cisxUI.find( (e) => e.name==='HiPS')?.value;
        const csys= ucdStr==='pos.galactic.lon' ? CoordSys.GALACTIC : CoordSys.EQ_J2000;
        const centerProjPt= makeWorldPt(raStr, decStr, csys);
        if (!centerProjPt) return;
        const request= getFieldGroupResults('DL_UI'); // todo: this might not be right, there might be an array of field groups
        const {fds}= findFieldDefInfo(request);
        const tgt= findTargetFromRequest(request,fds);
        if (!tgt) {
            dispatchChangeCenterOfProjection({plotId:HIPS_PLOT_ID, centerProjPt});

            const fov= Number(fovStr);
            const pv= getPlotViewById(visRoot(),HIPS_PLOT_ID);
            if (fov && pv) {
                const MIN_FOV_SIZE= .0025; // 9 arcsec - minimum fov for initial size
                const cleanFov= fov<MIN_FOV_SIZE ? MIN_FOV_SIZE : fov;
                const level= getHiPSZoomLevelForFOV(pv,cleanFov);
                if (!level) return;
                dispatchZoom({plotId:pv.plotId, userZoomType: UserZoomTypes.LEVEL, level});
            }
        }
        if (hipsUrl &&  hipsUrl!==plot.hipsUrlRoot) {
            resolveHiPSIvoURL(hipsUrl).then((resolvedHiPsUrl) => {
                if (resolvedHiPsUrl!==plot.hipsUrlRoot) {
                    dispatchChangeHiPS({plotId: plot.plotId, hipsUrlRoot: hipsUrl});
                }
            });
        }
    }, [currentTblId]);

    const {cisxUI}= qAna?.primarySearchDef?.[0].serviceDef ?? [];
    const isAllSky= toBoolean(cisxUI?.find( (e) => e.name==='data_covers_allsky')?.value);




    const submitSearch= (r) => {
        const {fds, standardID, idx}= findFieldDefInfo(r);
        const convertedR= convertRequest(r,fds,isSIAStandardID(standardID));

        const numKeys= [...new Set(fds.map( ({key}) => key))].length;
        if (Object.keys(convertedR).length<numKeys) {
            showInfoPopup('Please enter all of the fields', 'Error');
            return false;
        }
        handleSearch(convertedR,qAna,idx);
        return true;
    };

    const notLoaded= (
        (regHasUrl || !isRegLoaded) ?
            <div style={{position:'relative', width:'100%', height:'100%'}}>
                <div className='loading-mask'/>
            </div> :
            <div style={{alignSelf:'center', fontSize:'large', paddingLeft:40}}>
                {`No collections to load from: ${url}`}
            </div>
    );

    return (
        <div style={{display: 'flex', flexDirection:'column', width:'100%', justifyContent:'center', padding: '12px 0px 0 6px'}}>
            <div className='SearchPanel' style={{width:'100%', height:'100%'}}>
                {sideBarShowing && sideBar}
                <FieldGroup groupKey='DL_UI' keepState={true} style={{width:'100%'}}>
                    {(isRegLoaded && qAna) ? searchObjFds.length===1 ?
                            <ServDescPanel{...{initArgs, setSideBarShowing, sideBarShowing, fds:searchObjFds[0].fds,
                                clickFuncRef, submitSearch, isAllSky,
                                style:{width:'100%',height:'100%'}, desc:searchObjFds[0].desc, docRows}} /> :
                            <TabView{...{initArgs, tabsKey, setSideBarShowing, sideBarShowing, searchObjFds, qAna,
                                docRows, isAllSky, clickFuncRef, submitSearch}}/>
                        :
                        notLoaded
                    }
                </FieldGroup>
            </div>
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


export function ExpandButton({text, icon, tip='$text',style={}, onClick}) {
    const s= Object.assign({cursor:'pointer', verticalAlign:'bottom'},style);
    return (
        <div style={s} title={tip} onClick={onClick}>
            <div style={{display:'flex'}}>
                <img src={icon} height={12}/>
                <div style={{marginLeft:5}}>{text}</div>
            </div>
        </div>
    );
}



