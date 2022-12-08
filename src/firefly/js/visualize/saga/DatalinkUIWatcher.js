import {once} from 'lodash';
import {dispatchSetMenu, getMenu} from '../../core/AppDataCntlr.js';
import {dispatchShowDropDown} from '../../core/LayoutCntlr.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {getBooleanMetaEntry} from '../../tables/TableUtil.js';
import {DL_UI_LIST} from '../../ui/dynamic/DLGeneratedDropDown.js';
import {isTableDatalink} from '../../util/VOAnalyzer.js';

/** @type {TableWatcherDef} */
export const getDatalinkUIWatcherDef= once(() => ({
    id : 'ServiceDescriptorUIWatcher',
    watcher : watchForDatalinkUI,
    testTable : (table) => isTableDatalink(table) && getBooleanMetaEntry(table, MetaConst.LOAD_TO_DATALINK_UI),
    stopPropagation: true,
    allowMultiples: false,
    actions: []
}));



export function watchForDatalinkUI(tbl_id, action, cancelSelf) {

    if (action) return; //this watcher is only used the first time, when the table loads, action will be undefined
    cancelSelf();

    const {tblIdList=[]}=  getComponentState(DL_UI_LIST);
    const {menuItems,selected,showBgMonitor}= getMenu();

    if (!menuItems?.find(({action}) => action==='DLGeneratedDropDownCmd')) { // add the toolbar option
        const newMenuItems= [...menuItems];
        const dlDrop= {label:'Collections', action:'DLGeneratedDropDownCmd'};
        newMenuItems.splice(1,0,dlDrop);
        dispatchSetMenu({selected,showBgMonitor,menuItems:newMenuItems});
    }

    dispatchComponentStateChange(DL_UI_LIST, {currentTblId:tbl_id, tblIdList:[... new Set([...tblIdList,tbl_id])]});
    dispatchShowDropDown( { view: 'DLGeneratedDropDownCmd', initArgs:{}});
}