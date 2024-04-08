import {getMenu} from '../../core/AppDataCntlr.js';
import {dispatchShowDropDown, dispatchUpdateMenu} from '../../core/LayoutCntlr.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {makeFileRequest} from '../../tables/TableRequestUtil.js';
import {dispatchTableFetch} from '../../tables/TablesCntlr.js';
import {onTableLoaded} from '../../tables/TableUtil.js';
import {DL_UI_LIST} from './DLGeneratedDropDown.js';

function loadDatalinkUITable(tbl_id, url, initArgs={}, dlAction) {
    const {fetchedTables={}}=  getComponentState(DL_UI_LIST) ?? {};
    const newFetchedTables= {...fetchedTables, [url]:tbl_id};

    confirmDLMenuItem(dlAction);
    dispatchComponentStateChange(DL_UI_LIST, {currentTblId:tbl_id, fetchedTables:newFetchedTables});
    dispatchShowDropDown( { view: dlAction, initArgs});
}

export function confirmDLMenuItem(dlAction) {
    const {menuItems,selected,showBgMonitor}= getMenu();
    if (!menuItems?.find(({action}) => action===dlAction)) { // add the toolbar option
        const newMenuItems= [...menuItems];
        const dlDrop= {label:'Collection Search', action:dlAction};
        newMenuItems.splice(1,0,dlDrop);
        dispatchUpdateMenu({selected,showBgMonitor,menuItems:newMenuItems});
    }
}

/**
 *
 * @param url - location of file can reference a file on the server
 * @param idx - table index in file
 * @param initArgs - object of initial arguments, used by web api
 * @param {String} dlAction = the action command for the dropdown
 * @returns {Promise<void>}
 */
export async function fetchDatalinkUITable(url, idx=0, initArgs={}, dlAction= 'DLGeneratedDropDownCmd') {
    const loadOptions=  {META_INFO:{[MetaConst.LOAD_TO_DATALINK_UI]: 'true'}};
    const req= makeFileRequest('Data link UI', url, undefined, loadOptions);
    const {tbl_id}= req.META_INFO;
    dispatchTableFetch(req);
    await onTableLoaded(tbl_id);
    loadDatalinkUITable(tbl_id, url, initArgs, dlAction);
}

