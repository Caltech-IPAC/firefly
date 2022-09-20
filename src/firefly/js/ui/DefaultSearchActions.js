import {makeSearchAction, SearchTypes} from '../core/ClickToAction.js';
import {flux} from '../core/ReduxFlux.js';

//note - these two redundant function are here because of circular dependencies.
// this file is imported very early and webpack is creating errors
//-----------------
const getAppOptions= () => flux.getState().app_data.appOptions ?? window.firefly?.options ?? {};

const dispatchShowDropDown= ({view, initArgs}) =>
    flux.process({type: 'layout.showDropDown', payload: {visible: true, view, initArgs}});
//------------

const showTap= (initArgs) => {
    const view= getAppOptions()?.multiTableSearchCmdOptions?.find(({id}) => id === 'tap') ? 'MultiTableSearchCmd' : 'TAPSearch';
    dispatchShowDropDown({ view, initArgs});
};

const showImage= (initArgs) => dispatchShowDropDown( { view: 'ImageSelectDropDownCmd', initArgs});

export const makeDefTapSearchActions = () => {
    return [
        makeSearchAction('tapRadius', 'TAP ', 'Cone Search', SearchTypes.pointRadius, .001, 2.25,
            (sa, cenWpt, radius) => {
                showTap( {defaultSelectedId: 'tap', searchParams: {radiusInArcSec: radius, wp: cenWpt}});
            }),
        makeSearchAction('tapArea', 'TAP', 'Area Search', SearchTypes.area, undefined, undefined,
            (sa, cenWpt, radius, corners) => {
                showTap({defaultSelectedId: 'tap', searchParams: {corners}});
            }),
        makeSearchAction('tapRadius', 'TAP ', 'Cone Search', SearchTypes.point_table_only, .001, 2.25,
            (sa, cenWpt) => {
                showTap({defaultSelectedId: 'tap', searchParams: {wp: cenWpt}});
            }),
    ];
};


export const makeDefImageSearchActions = () => {
    return [
        makeSearchAction('imageFits', 'FITS', 'Fits tip', SearchTypes.point, undefined, undefined,
            (sa, wp) => showImage( {searchParams: {wp, type: 'singleChannel'}}) ),
        makeSearchAction('HiPS', 'HiPS', 'HiPS tip', SearchTypes.point, undefined, undefined,
            (sa, wp) => showImage( {searchParams: {wp, type: 'hipsImage'}})),
    ];
};