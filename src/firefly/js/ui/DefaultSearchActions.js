import {getMenu} from '../core/AppDataCntlr.js';
import {makeSearchActionObj, SearchTypes} from '../core/ClickToAction.js';
import {flux} from '../core/ReduxFlux.js';
import {ServerParams} from '../data/ServerParams.js';
import {sprintf} from '../externalSource/sprintf.js';
import {getTableUiByTblId, makeFileRequest} from '../api/ApiUtilTable.jsx';
import {makeVOCatalogRequest} from '../tables/TableRequestUtil.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {findTableCenterColumns} from '../voAnalyzer/TableAnalysis.js';
import {DEFAULT_FITS_VIEWER_ID} from '../visualize/MultiViewCntlr.js';
import {setMultiSearchPanelTab} from './MultiSearchPanel.jsx';
import {Format} from 'firefly/data/FileAnalysis';
import {doJsonRequest} from 'firefly/core/JsonUtils';
import {showInfoPopup} from 'firefly/ui/PopupUtil';

//note - these two redundant function are here because of circular dependencies.
// this file is imported very early and webpack is creating errors
//-----------------
const SEARCH_TBL_SOURCE= 'EXISTING_TBL_SOURCE';

const getAppOptions= () => flux.getState().app_data.appOptions ?? window.firefly?.options ?? {};

const dispatchShowDropDown= ({view, initArgs}) =>
    flux.process({type: 'layout.showDropDown', payload: {visible: true, view, initArgs}});
//------------

export const showTapSearchPanel= (searchParams={}) => {
    const view= getAppOptions()?.multiTableSearchCmdOptions?.find(({id}) => id === 'tap') ? 'MultiTableSearchCmd' : 'TAPSearch';
    dispatchShowDropDown({ view, initArgs:{defaultSelectedId: 'tap', searchParams}});
    if (view==='MultiTableSearchCmd') setMultiSearchPanelTab('tap');
};

const showImage= (initArgs) => {
    const modInitArgs= {...initArgs, searchParams: {...initArgs.searchParams, viewerId:DEFAULT_FITS_VIEWER_ID}};
    const {menuItems}= getMenu();
    let view= 'ImageSelectDropDownCmd';
    if (modInitArgs.searchParams?.type=== 'hipsImage') {
        view= menuItems?.find(({action}) => action==='HiPSSearchPanel') ? 'HiPSSearchPanel' : 'ImageSelectDropDownCmd';
    }
    dispatchShowDropDown( { view, initArgs:modInitArgs});
};

export const makeDefTapSearchActions = () => {
    return [
        makeSearchActionObj({
            cmd:'tapRadius',
            groupId: 'tap',
            label:'TAP ',
            tip:'Cone Search',
            searchType:SearchTypes.pointRadius,
            min:.001,
            max:2.25,
            execute: (sa, cenWpt, radius) => showTapSearchPanel( {radiusInArcSec: radius, wp: cenWpt})
        } ),
        makeSearchActionObj({
            cmd:'tapArea',
            groupId:'tap',
            label:'TAP',
            tip:'Area Search',
            searchType:SearchTypes.area,
            execute: (sa, cenWpt, radius, corners) => showTapSearchPanel({corners}),
        } ),
        makeSearchActionObj({
            cmd:'tableTapRadius',
            groupId:'tap',
            label:'TAP ',
            tip:'Cone Search',
            searchType:SearchTypes.point_table_only,
            min:.001,
            max:2.25,
            execute: (sa, cenWpt) => showTapSearchPanel({wp: cenWpt}),
        } ),
    ];
};


export const makeDefImageSearchActions = () => {
    return [
        makeSearchActionObj({
            cmd:'imageFits',
            groupId:'image',
            label:'FITS',
            tip:'Load a FITS Image at this point',
            searchType: SearchTypes.point,
            execute:(sa, wp) => showImage( {searchParams: {wp, type: 'singleChannel'}})
        } ),
        makeSearchActionObj({
            cmd:'HiPS',
            groupId:'image',
            label:'HiPS',
            tip:'HiPS tip',
            searchType: SearchTypes.pointRadius,
            min:.0025,
            max:180,
            execute:(sa, wp, radius) => showImage( {searchParams: {wp, type: 'hipsImage', radius}}),
            searchDesc:'Display HiPS at region center',
        } ),
    ];
};

export const makeDefTableSearchActions= () => {
    return [
        makeSearchActionObj({cmd:'tableNed',
            groupId:'resolver',
            label:'NED',
            tip:'Search NED and load the table',
            searchType: SearchTypes.point_table_only,
            execute: (sa, wp) => searchNed(wp,5/3600),
            searchDesc:'Search NED at row with 5" radius',
        } ),
        makeSearchActionObj({
            cmd:'tableSimbad',
            groupId: 'resolver',
            label: 'Simbad',
            tip:'Search Simbad and load the table',
            searchType: SearchTypes.point_table_only,
            execute: (sa, wp) => searchSimbad(wp,5/3600),
            searchDesc: 'Search Simbad at row with 5" radius'
        } ),
        makeSearchActionObj({cmd:'tableSimbadGoto',
            groupId:'resolver',
            label:'Simbad',
            tip:'Launch Simbad in a browser tab and show the table there',
            searchType: SearchTypes.point_table_only,
            execute: (sa, wp) => gotoAndSearchSimbad(wp,5/3600),
            searchDesc: 'Go to and Search Simbad at row with 5" radius'
        } ),
        makeSearchActionObj({
            cmd: 'imageFits',
            groupId: 'image',
            label: 'FITS',
            tip:'Fits tip',
            searchType: SearchTypes.point_table_only,
            execute: (sa, wp) => showImage( {searchParams: {wp, type: 'singleChannel'}}),
            searchDesc: 'Display FITS for row'
        } ),
        makeSearchActionObj({
            cmd: 'tableHiPS',
            groupId: 'image',
            label: 'HiPS',
            tip: 'HiPS tip',
            searchType: SearchTypes.point_table_only,
            execute: (sa, wp) => showImage( {searchParams: {wp, type: 'hipsImage'}}),
            searchDesc: 'Display HiPS for row'
        } ),

        makeSearchActionObj({
            cmd: 'tableTapUpload',
            groupId: 'tableTap',
            label: 'Upload Table',
            tip: 'Upload Whole Table',
            searchType: SearchTypes.wholeTable,
            execute: (sa,table) => searchWholeTable(table),
            searchDesc: 'Use table as an upload to TAP search'
        })
];

};

export const makeExternalSearchActions = () => {
    return [
        makeSearchActionObj({
            cmd:'nedRadius',
            groupId:'resolver',
            label:'NED',
            tip:'Cone Search',
            searchType: SearchTypes.pointRadius,
            min:.001,
            max:1,
            execute: (sa,cenWpt,radius) => searchNed(cenWpt,radius)
        } ),
        makeSearchActionObj({
            cmd:'simbadRadius',
            groupId:'resolver',
            label:'Simbad',
            tip:'Cone Search',
            searchType: SearchTypes.pointRadius,
            min:.001,
            max:5,
            execute: (sa,cenWpt,radius) => searchSimbad(cenWpt,radius)
        } ),
        makeSearchActionObj({
            cmd: 'gotoSimbadRadius',
            groupId: 'resolver',
            label: 'Simbad goto',
            tip: 'Launch Simbad in a browser tab and show the table there',
            searchType: SearchTypes.pointRadius,
            min: .001,
            max: 5,
            execute: (sa,cenWpt,radius) => gotoAndSearchSimbad(cenWpt,radius),
            searchDesc: (wp,size) => `Go to Simbad and search (cone) with radius of ${sprintf('%.4f',size)} degrees`} ),
    ];
};

async function searchWholeTable(table) {
        const params ={
            [ServerParams.COMMAND]: ServerParams.TABLE_SAVE,
            [ServerParams.REQUEST]: JSON.stringify(table.request),
            file_name: table.request?.META_INFO?.title ?? 'search_tbl_upload',
            file_format: Format.IPACTABLE,
            save_to_temp: 'true'
        };
        const tbl = getTableUiByTblId(table?.tbl_id);
        const columns = tbl.columns.map((col) =>
            col.visibility === 'hide' || col.visibility === 'hidden'? ({...col, use:false}) :  ({...col, use:true}));
        const {lonCol='', latCol=''}= findTableCenterColumns({tableData:{columns}}) ?? {};
        const columnsList = columns.map((col) => col.name === lonCol || col.name === latCol? ({...col, use:true}) :  ({...col, use:false})); //select position cols
        const columnsSelected = columnsList.filter( (c) => c.visibility!=='hidden');
        const result = await doJsonRequest(ServerParams.TABLE_SAVE, params);
        if (!result.success) {
            showInfoPopup('Error loading this table', result.error);
            return false;
        }
        const uploadInfo = {
            serverFile: result?.serverFile ?? null,
            title: tbl.title,
            fileName: tbl.title,
            tbl_id: tbl.tbl_id,
            columns: columnsSelected,
            totalRows: tbl.totalRows,
            tableSource: SEARCH_TBL_SOURCE,
        };
        showTapSearchPanel( {uploadInfo});
}

function searchNed(cenWpt,radius) {
    const accessUrl = 'http://ned.ipac.caltech.edu/cgi-bin/NEDobjsearch?search_type=Near+Position+Search&of=xml_main&';
    const request = makeVOCatalogRequest('NED',
        {
            [ServerParams.USER_TARGET_WORLD_PT]: cenWpt.toString(),
            SearchMethod: 'Cone',
            radius,
            providerName: 'NED',
            accessUrl
        }
    );
    dispatchTableSearch(request);
}

function searchSimbad(cenWpt,radius) {
    const base = 'http://simbad.cds.unistra.fr/simbad/sim-coo';
    const params = {
        ['Radius.unit']: 'degree',
        Radius: radius,
        Coord: cenWpt.x + ' ' + cenWpt.y,
        submit: 'submit query',
        ['output.format']: 'VOTable'
    };
    const url = base + '?' + new URLSearchParams(params).toString();
    const request= makeFileRequest('Simbad', url);
    dispatchTableSearch(request);
}

function gotoAndSearchSimbad(cenWpt,radius) {
    const base = 'http://simbad.cds.unistra.fr/simbad/sim-coo';
    const params = {
        ['Radius.unit']: 'degree',
        Radius: radius,
        Coord: cenWpt.x + ' ' + cenWpt.y,
        submit: 'submit query',
    };
    const url = base + '?' + new URLSearchParams(params).toString();
    console.log(url);
    window.open(url, '_blank');
}
