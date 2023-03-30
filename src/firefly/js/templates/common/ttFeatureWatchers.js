import React from 'react';
import {once} from 'lodash';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {dispatchTableUiUpdate} from '../../tables/TablesCntlr.js';
import {getTableUiByTblId, getTblById} from '../../tables/TableUtil.js';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {isObsCoreLike} from '../../util/VOAnalyzer.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowCenterDef } from '../../visualize/saga/ActiveRowCenterWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';

export const getAllStartIds= ()=> [
    getMocWatcherDef().id,
    getCatalogWatcherDef().id,
    getUrlLinkWatcherDef().id,
    getActiveRowCenterDef().id,
    getObsCoreWatcherDef().id,
];

export function startTTFeatureWatchers(startIds=[
    getMocWatcherDef().id, getCatalogWatcherDef().id, getUrlLinkWatcherDef().id, getActiveRowCenterDef().id]) {
    startIds.includes(getMocWatcherDef().id) && dispatchAddTableTypeWatcherDef(getMocWatcherDef());
    startIds.includes(getCatalogWatcherDef().id) && dispatchAddTableTypeWatcherDef(getCatalogWatcherDef());
    startIds.includes(getUrlLinkWatcherDef().id) && dispatchAddTableTypeWatcherDef(getUrlLinkWatcherDef());
    startIds.includes(getActiveRowCenterDef().id) && dispatchAddTableTypeWatcherDef(getActiveRowCenterDef());
    // startIds.includes(getObsCoreWatcherDef().id) && dispatchAddTableTypeWatcherDef(getObsCoreWatcherDef());
}



/** @type {TableWatcherDef} */
const getObsCoreWatcherDef= once(() => ({
    id : 'ObsCorePackage',
    watcher : watchForObsCoreTable,
    testTable : isObsCoreLike,
    stopPropagation: true,
    allowMultiples: false,
    actions: []
}));

function watchForObsCoreTable(tbl_id, action, cancelSelf) {
    if (action) return;
    setupObsCorePackaging(tbl_id);
    cancelSelf();
}


function setupObsCorePackaging(tbl_id) {
    const table= getTblById(tbl_id);
    if (!table) return;
    const {tbl_ui_id}= getTableUiByTblId(tbl_id) ?? {} ;
    if (!tbl_ui_id) return;
    dispatchTableUiUpdate({ tbl_ui_id, leftButtons: [() => <PrepareDownload/>] });
}


const PrepareDownload = React.memo(() => (
    <div>
        <DownloadButton>
            <DownloadOptionPanel {...{dlParams:{FileGroupProcessor:'ObsCorePackager',
                    help_id:'table.obsCorePackage', BaseFileName:'download'}}}/>
        </DownloadButton>
    </div>
));
