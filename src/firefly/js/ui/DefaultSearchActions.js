import {makeSearchAction, SearchTypes} from '../core/ClickToAction.js';
import {flux} from '../core/ReduxFlux.js';
import {ServerParams} from '../data/ServerParams.js';
import {sprintf} from '../externalSource/sprintf.js';
import {makeFileRequest} from '../api/ApiUtilTable.jsx';
import {makeVOCatalogRequest} from '../tables/TableRequestUtil.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {setMultiSearchPanelTab} from './MultiSearchPanel.jsx';

//note - these two redundant function are here because of circular dependencies.
// this file is imported very early and webpack is creating errors
//-----------------
const getAppOptions= () => flux.getState().app_data.appOptions ?? window.firefly?.options ?? {};

const dispatchShowDropDown= ({view, initArgs}) =>
    flux.process({type: 'layout.showDropDown', payload: {visible: true, view, initArgs}});
//------------

export const showTapSearchPanel= (searchParams={}) => {
    const view= getAppOptions()?.multiTableSearchCmdOptions?.find(({id}) => id === 'tap') ? 'MultiTableSearchCmd' : 'TAPSearch';
    dispatchShowDropDown({ view, initArgs:{defaultSelectedId: 'tap', searchParams}});
    if (view==='MultiTableSearchCmd') setMultiSearchPanelTab('tap');
};

const showImage= (initArgs) => dispatchShowDropDown( { view: 'ImageSelectDropDownCmd', initArgs});

export const makeDefTapSearchActions = () => {
    return [
        makeSearchAction('tapRadius', 'tap', 'TAP ', 'Cone Search', SearchTypes.pointRadius, .001, 2.25,
            (sa, cenWpt, radius) => showTapSearchPanel( {radiusInArcSec: radius, wp: cenWpt}), ),
        makeSearchAction('tapArea', 'tap', 'TAP', 'Area Search', SearchTypes.area, undefined, undefined,
            (sa, cenWpt, radius, corners) => showTapSearchPanel({corners}), ),
        makeSearchAction('tableTapRadius', 'tap', 'TAP ', 'Cone Search', SearchTypes.point_table_only, .001, 2.25,
            (sa, cenWpt) => showTapSearchPanel({wp: cenWpt}), ),
    ];
};


export const makeDefImageSearchActions = () => {
    return [
        makeSearchAction('imageFits', 'image', 'FITS', 'Load a FITS Image at this point', SearchTypes.point, undefined, undefined,
            (sa, wp) => showImage( {searchParams: {wp, type: 'singleChannel'}}) ),
        makeSearchAction('HiPS', 'image', 'HiPS', 'HiPS tip', SearchTypes.pointRadius, .0025, 180,
            (sa, wp, radius) => showImage( {searchParams: {wp, type: 'hipsImage', radius}}), 'Display HiPS at region center'),
    ];
};

export const makeDefTableSearchActions= () => {
    return [
        makeSearchAction('tableNed', 'resolver', 'NED', 'Search NED and load the table',
            SearchTypes.point_table_only, undefined, undefined,
            (sa, wp) => searchNed(wp,5/3600), 'Search NED at row with 5" radius'),
        makeSearchAction('tableSimbad', 'resolver', 'Simbad', 'Search Simbad and load the table',
            SearchTypes.point_table_only, undefined, undefined,
            (sa, wp) => searchSimbad(wp,5/3600), 'Search Simbad at row with 5" radius'),
        makeSearchAction('tableSimbadGoto', 'resolver', 'Simbad', 'Launch Simbad in a browser tab and show the table there',
            SearchTypes.point_table_only, undefined, undefined,
            (sa, wp) => gotoAndSearchSimbad(wp,5/3600), 'Go to and Search Simbad at row with 5" radius'),
        makeSearchAction('imageFits', 'image', 'FITS', 'Fits tip', SearchTypes.point_table_only, undefined, undefined,
            (sa, wp) => showImage( {searchParams: {wp, type: 'singleChannel'}}), 'Display FITS for row'),
        makeSearchAction('tableHiPS', 'image', 'HiPS', 'HiPS tip', SearchTypes.point_table_only, undefined, undefined,
            (sa, wp) => showImage( {searchParams: {wp, type: 'hipsImage'}}), 'Display HiPS for row'),
    ];

};

export const makeExternalSearchActions = () => {
    return [
        makeSearchAction('nedRadius','resolver', 'NED', 'Cone Search', SearchTypes.pointRadius, .001, 5,
            (sa,cenWpt,radius) => searchNed(cenWpt,radius)),
        makeSearchAction('simbadRadius','resolver', 'Simbad', 'Cone Search', SearchTypes.pointRadius, .001, 5,
            (sa,cenWpt,radius) => searchSimbad(cenWpt,radius)),
        makeSearchAction('gotoSimbadRadius','resolver', 'Simbad goto', 'Launch Simbad in a browser tab and show the table there',
            SearchTypes.pointRadius, .001, 5,
            (sa,cenWpt,radius) => gotoAndSearchSimbad(cenWpt,radius),
            (wp,size) => `Go to Simbad and search (cone) with radius of ${sprintf('%.4f',size)} degrees`, ),
    ];
};




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
