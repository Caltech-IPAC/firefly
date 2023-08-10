import {isArray, isEmpty, once} from 'lodash';
import React, {useEffect, useRef, useState} from 'react';
import {makeWorldPt, visRoot} from '../../api/ApiUtilImage.jsx';
import {dispatchActiveTarget} from '../../core/AppDataCntlr.js';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {dispatchMountFieldGroup} from '../../fieldGroup/FieldGroupCntlr.js';
import {getFieldGroupResults} from '../../fieldGroup/FieldGroupUtils.js';
import {getJsonProperty} from '../../rpc/CoreServices.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {makeFileRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchTableFetch, dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import { getCellValue, getColumnIdx, getTblById, getTblRowAsObj, onTableLoaded, } from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {cisxAdhocServiceUtype, standardIDs} from '../../util/VOAnalyzer.js';
import {makeSearchOnce, toBoolean} from '../../util/WebUtil.js';
import CoordSys from '../../visualize/CoordSys.js';
import {ensureHiPSInit } from '../../visualize/HiPSListUtil.js';
import {getHiPSZoomLevelForFOV} from '../../visualize/HiPSUtil.js';
import {dispatchChangeCenterOfProjection, dispatchZoom} from '../../visualize/ImagePlotCntlr.js';
import {getPlotViewById, primePlot} from '../../visualize/PlotViewUtil.js';
import {pointEquals} from '../../visualize/Point.js';
import {CONE_CHOICE_KEY} from '../../visualize/ui/CommonUIKeys.js';
import {isHiPS} from '../../visualize/WebPlot.js';
import {UserZoomTypes} from '../../visualize/ZoomUtil.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {analyzeQueries, handleSearch, hasSpatialTypes, isSpatialTypeSupported} from './DLGenAnalyzeSearch.js';
import {getSpacialSearchType, hasValidSpacialSearch} from './DynComponents.jsx';
import {confirmDLMenuItem} from './FetchDatalinkTable.js';
import { getStandardIdType, ingestInitArgs, makeFieldDefs, makeSearchAreaInfo} from './ServiceDefTools.js';
import { convertRequest, DynLayoutPanelTypes, findTargetFromRequest} from './DynamicUISearchPanel.jsx';
import {CIRCLE, CONE_AREA_KEY, POSITION, RANGE} from './DynamicDef.js';
import {FieldGroup} from '../FieldGroup.jsx';
import {FormPanel} from '../FormPanel.jsx';
import {FieldGroupTabs, Tab} from '../panel/TabPanel.jsx';
import {showInfoPopup} from '../PopupUtil.jsx';
import {useFieldGroupValue, useStoreConnector} from '../SimpleComponent.jsx';

import './DLGeneratedDropDown.css';
import SHOW_RIGHT from 'images/show-right-3.png';
import HIDE_LEFT from 'images/hide-left-3.png';

export const DL_UI_LIST= 'DL_UI_LIST';
const HIPS_PLOT_ID= 'dlGeneratedHipsPlotId';
const GROUP_KEY= 'DL_UI';

let regLoaded= false;
let regLoading= false;
let loadedTblIdCache={};
const linkStyle= {whiteSpace:'noWrap', fontStyle:'italic', fontWeight: 'bold'};

const findUrl = async () => {
    const servicesRootUrl= await getJsonProperty('inventory.serverURLAry');
    return servicesRootUrl;
};



//todo these next 5 functions could be refactored when this is generalize, we might pass an object with them
//========================
//========================
//========================
//========================
function getCollectionUrl(registryTblId, rowIdx) {
    const table= getTblById(registryTblId);
    if (!table) return;
    return getCellValue(table,rowIdx,'access_url');
}

function getCollectionAttributes(registryTblId, rowIdx) {
    const table= getTblById(registryTblId);
    if (!table) return {};
    const {band,coverage}= getTblRowAsObj(table,rowIdx);
    return {bandDesc:band, coverage};
}

function findUrlInReg(url, registryTblId) {
    const table= getTblById(registryTblId);
    if (!table) return false;
    const idx= getColumnIdx(table, 'access_url');
    if (idx===-1) return false;
    return table?.tableData?.data?.findIndex( (d) => d[idx]===url);
}

const uiConfig= {
    showOtherDataLabel: 'Show Other Data Collections',
    hideLabel: 'Hide',
    hideTip: 'Hide data collections chooser',
    chooserTitle: 'Choose Data Collection',
    chooserDetails: 'Click on data collection to search; filter or sort table to find a data collection.',
    sideBarWidth:470,
    sideTransition: 'all .5s ease-in-out'
};


function makeRegistryRequest(url, registryTblId) {
    return makeFileRequest('registry', url, undefined,
        {
            pageSize: MAX_ROW,
            sortInfo: sortInfoString(['facility_name','obs_collection']),
            tbl_id: registryTblId,
            inclCols: '"facility_name","collection_label","instrument_name","coverage","band","dataproduct_type","info_url"',
            META_INFO: {
                'col.facility_name.PrefWidth':6,
                'col.collection_label.PrefWidth':9,
                'col.instrument_name.PrefWidth':9,
                'col.coverage.PrefWidth':8,
                'col.band.PrefWidth':12,
                'col.dataproduct_type.PrefWidth':5,
                'col.info_url.PrefWidth':1,

                // eslint-disable-next-line quotes
                'col.info_url.cellRenderer': 'ATag::href=${info_url},target="dce-doc"'+ `,label=<img src='images/info-16x16.png'/>`,


                'col.facility_name.label':'Facility',
                'col.instrument_name.label':'Inst.',
                'col.coverage.label':'Type',
                'col.dataproduct_type.label':'Data',
                'col.collection_label.label':'Collection',
                'col.band.label':'Bands',
                'col.info_url.label':'i',

                // the hidden is not necssary anymore because of inclCols, but I am keeping it here for documentation
                'col.obs_collection.visibility':'hidden',
                'col.description.visibility':'hidden',
                'col.desc_details.visibility':'hidden',
                'col.access_url.visibility':'hide',
                'col.access_format.visibility':'hidden',

                [MetaConst.IMAGE_SOURCE_ID] : 'FALSE'
            }
        }
    );
}

//========================
//========================
//========================
//========================



function isURLInRegistry(url, registryTblId) {
    return findUrlInReg(url, registryTblId)>-1;
}

async function doLoadRegistry(url, registryTblId, user) {
    try {
        const urlToUse= user? url+'?user='+user : url;
        dispatchTableFetch(makeRegistryRequest(urlToUse,registryTblId));
        console.log(`Registry URL: ${urlToUse}`);
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

async function loadRegistry(registryTblId, user) {
    if (regLoaded) return;
    regLoading= true;
    const url= await findUrl();
    await doLoadRegistry(url[0], registryTblId, user);
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
    const [searchAttributes,setSearchAttributes]= useState({});

    loadedTblIdCache= loadedTblIds;

    useEffect(() => {
        setTimeout(() => confirmDLMenuItem(), 20);
    }, []);

    useEffect(() => {// load registry and merge any already fetched tables
        const load= async () =>  {
            if (regLoaded || regLoading) return;
            await loadRegistry(registryTblId, urlApi?.user);
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
        if (newUrl) {
            setUrl(newUrl);
            setSearchAttributes(getCollectionAttributes(registryTblId,currentIdx));
        }
    }, [currentIdx, isRegLoaded, useCurrentIdx]);

    return <DLGeneratedDropDownTables {...{registryTblId,isRegLoaded, loadedTblIds, setLoadedTblIds, url, searchAttributes, initArgs}}/>;
}

function DLGeneratedDropDownTables({registryTblId, isRegLoaded, loadedTblIds, setLoadedTblIds, url, searchAttributes, initArgs}) {

    const [sideBarShowing, setSideBarShowing]= useState(true);
    const currentTblId= loadedTblIds?.[url];
    useEffect(() => {
        if (isRegLoaded && !currentTblId) {
            const loadOptions=  {META_INFO:{
                    [MetaConst.LOAD_TO_DATALINK_UI]: 'true',
                    [MetaConst.IMAGE_SOURCE_ID] : 'FALSE',
                    ...searchAttributes,
                },
            };
            if (!url) return;
            console.log(`Dataset url: ${url}`);
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


    const sideBar= <SideBarTable {...{registryTblId, setSideBarShowing,width:uiConfig.sideBarWidth}}/>;
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
                <ExpandButton style={linkStyle} icon={SHOW_RIGHT} text={uiConfig.showOtherDataLabel} onClick={()  => setSideBarShowing(true)}/>
                <div style={{display:'flex', flexDirection:'row', flexGrow:1, justifyContent:'center'}}>
                    <div className='DLGeneratedDropDown__section--title' style={{marginLeft:-60}}>
                        {titleDiv}
                    </div>
                </div>
            </div>
        );

    }

}


/**
 *
 * @param {Object} props
 * @param props.fds
 * @param props.style
 * @param props.desc
 * @param props.setSideBarShowing
 * @param props.sideBarShowing
 * @param props.docRows
 * @param props.clickFuncRef
 * @param props.submitSearch
 * @param props.isAllSky
 * @param {QueryAnalysis} props.qAna
 * @return {JSX.Element}
 * @constructor
 */
function ServDescPanel({fds, style, desc, setSideBarShowing, sideBarShowing, docRows, clickFuncRef, submitSearch, isAllSky, qAna})  {

    const docRowsComponents= (
        <div key='help' style={{fontSize:'larger', padding:'0 10px 3px 8px', alignSelf:'flex-end'}}>
            {
                docRows.map( (row, idx) => (
                    <a href={row.accessUrl} key={idx + ''} target={'documentation'} >
                        <span style={{fontStyle:'italic'}}>Documentation: </span>
                        <span> {`${row.desc}`}</span>
                    </a> ))
            }
        </div>);

    const SearchPanelWrapper= ({children}) => (
        <RootSearchPanel {...{additionalChildren:children, submitSearch, clickFuncRef, docRowsComponents, qAna}}/>
    );


    return  (
        <div style={{display:'flex', flexDirection:'column', justifyContent:'space-between', ...style}}>

            <SearchTitle {...{desc,isAllSky,sideBarShowing,setSideBarShowing}}/>
            <DynLayoutPanelTypes.Inset fieldDefAry={fds} plotId={HIPS_PLOT_ID} style={{height:'100%', marginTop:4}}
                                       WrapperComponent={SearchPanelWrapper} toolbarHelpId={'dlGenerated.VisualSelection'} />
        </div>
    );
}


function RootSearchPanel({additionalChildren, submitSearch, clickFuncRef, docRowsComponents, qAna}) {

    const coneAreaChoice=  useFieldGroupValue(CONE_AREA_KEY)?.[0]() ?? CONE_CHOICE_KEY;
    let cNames, disableNames;
    if (qAna?.concurrentSearchDef?.length && coneAreaChoice) {
        cNames= qAna?.concurrentSearchDef
            .filter( (c) => hasSpatialTypes(c.serviceDef) && isSpatialTypeSupported(c.serviceDef,coneAreaChoice))
            .map( (c) => c.desc);
        disableNames= qAna?.concurrentSearchDef
            .filter( (c) => hasSpatialTypes(c.serviceDef) && !isSpatialTypeSupported(c.serviceDef,coneAreaChoice))
            .map( (c) => c.desc);
    }

    const disDesc= coneAreaChoice===CONE_AREA_KEY ? 'w/ cone' : 'w/ polygon';

    const options= (cNames || disableNames) ? (
        <div style={{display:'flex', flexDirection:'column', alignItems:'center'}}>
            {Boolean(cNames?.length) &&
                <CheckboxGroupInputField
                    wrapperStyle={{marginTop:5}} fieldKey='searchOptions'
                    alignment='horizontal' labelWidth={115} labelStyle={{fontSize: 'larger'}}
                    label={`Include Search${cNames.length>1?'es':''} of: `}
                    options={cNames.map( (c) => ({label:c,value:c}))}
                    initialState={{ value: cNames.join(' '), tooltip: 'Additional Searches', label : '' }}
                />}
            {Boolean(disableNames?.length)  && (
                <div style={{display:'flex', flexDirection:'row'}}>
                    <div style={{fontSize:'larger', width:250}}>
                        <span style={{fontStyle:'italic'}}>Warning </span>
                        <span>{`- search${disableNames.length>1?'es':''} disabled ${disDesc}:`}</span>
                    </div>
                    {disableNames.map( (d) => (<div>{d}</div>))}
                </div> )
            }
        </div>

    ) : undefined;

    return (
        <FormPanel submitText = 'Search' groupKey = {GROUP_KEY}
                   onSubmit = {submitSearch}
                   params={{hideOnInvalid: false}}
                   inputStyle={{border:'none', padding:0, marginBottom:5}}
                   getDoOnClickFunc={(clickFunc) => clickFuncRef.clickFunc= clickFunc}
                   onError = {() => showInfoPopup('Fix errors and search again', 'Error') }
                   extraWidgets={[docRowsComponents]}
                   help_id  = {'search-collections-general'}>
            <div style={{ display:'flex', flexDirection:'column', alignItems:'center', }}>
                <>
                    {additionalChildren}
                    {options}
                </>
            </div>
        </FormPanel>
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
                            isAllSky, clickFuncRef, submitSearch, qAna}}/>
                    </Tab>
                );
            })
        }
    </FieldGroupTabs>
);

function SideBarTable({registryTblId, setSideBarShowing, width}) {
    return (
        <div style={{display:'flex', flexDirection:'column'}}>
            <div style={{display:'flex', flexDirection:'row', alignItems:'center'}}>
                {<ExpandButton style={linkStyle} icon={HIDE_LEFT} text={uiConfig.hideLabel} tip={uiConfig.hideTip}
                               onClick={()  => setSideBarShowing(false)}/> }
                <div className='DLGeneratedDropDown__section--title' style={{marginLeft:110}} >
                    {uiConfig.chooserTitle}
                </div>
            </div>
            <div style={{minWidth:width, flexGrow:1, backgroundColor:'inherit', padding: '4px 4px 0 2px'}}>
                <TablePanel {...{
                    key:registryTblId, tbl_id:registryTblId,
                    showToolbar: false, selectable:false, showFilters:true, showOptions: false, showUnits: false,
                    showTypes: false, textView: false, showOptionButton: false
                }}/>
            </div>
            <div style={{fontSize:'larger', display:'flex', justifyContent:'space-around', padding: '12px 0 5px 0'} }>
                <div>
                    {uiConfig.chooserDetails}
                </div>
            </div>
        </div>
    );
}


const executeInitOnce= makeSearchOnce(false);
const executeInitTargetOnce= makeSearchOnce(false);


function DLGeneratedTableSearch({currentTblId, initArgs, sideBar, regHasUrl, url, sideBarShowing, isRegLoaded, setSideBarShowing}) {


    const [,setCallId]= useState('none');
    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    const qAna= analyzeQueries(currentTblId);
    const tabsKey= 'Tabs-'+currentTblId;
    const matchUrl= initArgs?.urlApi?.url?.[0]===url;

    useEffect(() => {
        if (initArgs?.urlApi?.execute && clickFuncRef.clickFunc && matchUrl) {
            executeInitOnce(true, () => {
                setCallId(initArgs.urlApi.callId ?? 'none'); //forces one more render after unmount
                dispatchMountFieldGroup(GROUP_KEY, false, false); // unmount to force to forget default so it will reinit
                setTimeout(() => clickFuncRef?.clickFunc?.(),10);
            }, initArgs.urlApi.callId);
        }
    }, [clickFuncRef.clickFunc, initArgs?.urlApi?.callId, matchUrl]);


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
            if (!pointEquals(originalWp,initWp)) {
                executeInitTargetOnce(true, () => initWp && dispatchActiveTarget(initWp), initArgs?.urlApi?.callId);
            }
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
        const request= getFieldGroupResults(GROUP_KEY); // todo: this might not be right, there might be an array of field groups
        const {fds}= findFieldDefInfo(request);
        const tgt= findTargetFromRequest(request,fds);
        if (tgt) return;

        const {cisxUI}= qAna.primarySearchDef[0].serviceDef;
        const raStr= cisxUI.find( (e) => e.name==='hips_initial_ra')?.value;
        const decStr= cisxUI.find( (e) => e.name==='hips_initial_dec')?.value;
        const ucdStr= cisxUI.find( (e) => e.name==='hips_initial_ra')?.UCD;
        const fovStr= cisxUI.find( (e) => e.name==='hips_initial_fov')?.value;
        const coordSys= ucdStr==='pos.galactic.lon' ? CoordSys.GALACTIC : CoordSys.EQ_J2000;
        const centerProjPt= makeWorldPt(raStr, decStr, coordSys);
        if (!centerProjPt) return;
        dispatchChangeCenterOfProjection({plotId:HIPS_PLOT_ID, centerProjPt});
        const fov= Number(fovStr);
        const pv= getPlotViewById(visRoot(),HIPS_PLOT_ID);
        if (fov && pv) {
            const MIN_FOV_SIZE= .0025; // 9 arcsec - minimum fov for initial size
            const cleanFov= Math.max(fov,MIN_FOV_SIZE);
            const level= getHiPSZoomLevelForFOV(pv,cleanFov);
            if (level) dispatchZoom({plotId:pv.plotId, userZoomType: UserZoomTypes.LEVEL, level});
        }
    }, [currentTblId]);

    const {cisxUI}= qAna?.primarySearchDef?.[0].serviceDef ?? [];
    const isAllSky= toBoolean(cisxUI?.find( (e) => e.name==='data_covers_allsky')?.value);




    const submitSearch= (r) => {
        const {fds, standardID, idx}= findFieldDefInfo(r);

        if (!hasValidSpacialSearch(r,fds)) {
            showInfoPopup( getSpacialSearchType(r,fds)===CONE_CHOICE_KEY ?
                'Target is required' :
                'Search Area is require and must have at least 3 point pairs, each separated by commas');
            return false;
        }
        
        const convertedR= convertRequest(r,fds,getStandardIdType(standardID));
        const selectedConcurrent= r.searchOptions ?? '';

        const numKeys= [...new Set(fds.map( ({key}) => key))].length;
        if (Object.keys(convertedR).length<numKeys) {
            showInfoPopup('Please enter all of the fields', 'Error');
            return false;
        }
        handleSearch(convertedR,qAna,fdAry, idx, docRows?.[0]?.accessUrl, selectedConcurrent);
        return true;
    };

    const notLoaded= (
        (regHasUrl || !isRegLoaded) ?
            (<div style={{position:'relative', width:'100%', height:'100%'}}>
                <div className='loading-mask'/>
            </div>) :
            (<div style={{alignSelf:'center', fontSize:'large', paddingLeft:40}}>
                {`No collections to load from: ${url}`}
            </div>)
    );

    return (
        <div style={{display: 'flex', flexDirection:'column', width:'100%', justifyContent:'center', padding: '12px 0px 0 6px'}}>
            <div className='SearchPanel' style={{width:'100%', height:'100%'}}>
                <SideBarAnimation {...{sideBar,sideBarShowing,width:uiConfig.sideBarWidth}}/>
                <FieldGroup groupKey={GROUP_KEY} keepState={true} style={{width:'100%'}}>
                    {(isRegLoaded && qAna) ? searchObjFds.length===1 ?
                            <ServDescPanel{...{initArgs, setSideBarShowing, sideBarShowing, fds:searchObjFds[0].fds,
                                clickFuncRef, submitSearch, isAllSky, qAna,
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

function SideBarAnimation({sideBar,sideBarShowing,width}) {
    const w= sideBarShowing?width:0;
    return (
        <div style={{minWidth:w, width:w, overflow:'hidden', display:'flex', transition:uiConfig.sideTransition}} >
            {sideBar}
        </div>
    );
}

function ExpandButton({text, icon, tip='$text',style={}, onClick}) {
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

