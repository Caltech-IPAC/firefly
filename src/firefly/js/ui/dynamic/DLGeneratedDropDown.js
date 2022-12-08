import React, {useEffect, useState} from 'react';
import {dispatchAddPreference, getPreference} from '../../core/AppDataCntlr.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {makeFileRequest} from '../../tables/TableRequestUtil.js';
import {dispatchTableFetch, dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {getCellValue, getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {Logger} from '../../util/Logger.js';
import {cisxAdhocServiceUtype, getServiceDescriptors, standardIDs} from '../../util/VOAnalyzer.js';
import {
    isSIAStandardID, makeFieldDefs, makeSearchAreaInfo, makeServiceDescriptorSearchRequest
} from './ServiceDefTools.js';
import {RANGE} from './DynamicDef.js';
import {convertRequest, DynLayoutPanelTypes} from './DynamicUISearchPanel.jsx';
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

export function DLGeneratedDropDown({initArgs={}}) {// eslint-disable-line no-unused-vars
    const {currentTblId, tblIdList=[]}=  useStoreConnector(() => getComponentState(DL_UI_LIST));
    const [urlAry, setUrlAry]=  useState([]);


    useEffect(() => {
        if (!tblIdList.length) {
            const newUrlAry= getPreference('dlGeneratedTableUrls');
            const loadOptions=  {META_INFO:{[MetaConst.LOAD_TO_DATALINK_UI]: 'true'}};
            newUrlAry.forEach( (url) =>  dispatchTableFetch(makeFileRequest('Data link UI', url, undefined, loadOptions)));
        }
    }, []);

    useEffect(() => {
        const genUrlAry= tblIdList
            .map( (tbl_id) => getTblById(tbl_id))
            .map( ({request}={}) => request?.source)
            .filter( (source) => source);
        if (urlAry?.length!==genUrlAry.length &&  !genUrlAry.every( (url) => urlAry.includes(url))) {
            setUrlAry(genUrlAry);
            dispatchAddPreference('dlGeneratedTableUrls', genUrlAry);
        }
    });

    return <DLGeneratedDropDownTables {...{currentTblId,tblIdList}}/>;
}

export function DLGeneratedDropDownTables({tblIdList=[], currentTblId}) {// eslint-disable-line no-unused-vars

    if (tblIdList.length<2) return (<DLGeneratedTableSearch {...{currentTblId}}/>);

    const changeActiveTable= (tbl_id) =>
        dispatchComponentStateChange(DL_UI_LIST, {currentTblId:tbl_id, tblIdList});

    return (
        <div className='SearchPanel' style={{width:'100%'}}>
            <SideBar {...{tblIdList,currentTblId,changeActiveTable}}/>
            <DLGeneratedTableSearch {...{currentTblId}}/>
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

const SideBar= ({tblIdList, currentTblId, changeActiveTable}) => (
    <div className='SearchPanel__sidebar'>
        {
            tblIdList.map( (tbl_id) => {
                const table= getTblById(tbl_id);
                const desc= getCellValue(table,0,DESC);
                return (<SearchItem {...{desc, key:tbl_id, tbl_id, active:currentTblId===tbl_id, changeActiveTable}}/>);
            })
        }
    </div> );

const SearchItem= ({desc, tbl_id, active, changeActiveTable}) => (
    <div className='SearchPanel__searchItem' title={'todo - add tip'}
        onClick={() => changeActiveTable(tbl_id)}>
        <span className={active ? 'selected' : 'normal'}>{desc}</span>
    </div>
);


function DLGeneratedTableSearch({currentTblId}) {

    const qAna= analyzeQueries(currentTblId);
    const tabsKey= 'Tabs='+currentTblId;
    if (!qAna) {
        return (
            <div style={{width:'100%', display:'flex', flexDirection:'column', justifyContent:'center'}}>
                <div style={{alignSelf:'center', fontSize:'large'}}>No collections to load</div>
            </div>
        );
    }

    const fdAry= qAna.primarySearchDef.map( (fd) => {
        const {serviceDef}= fd; //todo handle case with only an access url
        const standId= (serviceDef.standardID??'').toLowerCase();
        const utype= (serviceDef.utype||'').toLowerCase();
        if (utype===cisxAdhocServiceUtype && standId.startsWith(standardIDs.tap) && serviceDef.cisxTokenSub) {
            return makeFieldDefs(serviceDef.cisxTokenSub, undefined, makeSearchAreaInfo(serviceDef.cisxUI), false);
        }
        else {
            return makeFieldDefs(serviceDef.serDefParams, undefined, makeSearchAreaInfo(serviceDef.cisxUI), true);
        }
    });

    const docRows= qAna.urlRows.filter( ({semantic}) => semantic?.toLowerCase().endsWith('documentation'));


    const searchObjFds= fdAry
        .map((fds,idx) => {
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
                       onError = {() => showInfoPopup('Fix errors and search again', 'Error') }
                       help_id  = {'todo-AtReplaceHelpId-todo'}>
                <FieldGroup groupKey='DL_UI' keepState={true} style={{height:'100%', width:'100%'}}>
                    {searchObjFds.length===1 ?
                        <ServDescPanel{...{fds:searchObjFds[0].fds, style:{width:'100%',height:'100%'}, desc:searchObjFds[0].desc, docRows}} /> :
                        <TabView{...{tabsKey,searchObjFds,qAna, docRows}}/>}
                </FieldGroup>
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