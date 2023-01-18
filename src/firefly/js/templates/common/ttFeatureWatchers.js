import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowCenterDef } from '../../visualize/saga/ActiveRowCenterWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';


export function startTTFeatureWatchers(startIds=[
    getMocWatcherDef().id, getCatalogWatcherDef().id, getUrlLinkWatcherDef().id, getActiveRowCenterDef().id]) {
    startIds.includes(getMocWatcherDef().id) && dispatchAddTableTypeWatcherDef(getMocWatcherDef());
    startIds.includes(getCatalogWatcherDef().id) && dispatchAddTableTypeWatcherDef(getCatalogWatcherDef());
    startIds.includes(getUrlLinkWatcherDef().id) && dispatchAddTableTypeWatcherDef(getUrlLinkWatcherDef());
    startIds.includes(getActiveRowCenterDef().id) && dispatchAddTableTypeWatcherDef(getActiveRowCenterDef());
}