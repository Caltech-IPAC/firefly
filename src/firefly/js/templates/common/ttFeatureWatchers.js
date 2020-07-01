import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {catalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {urlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowCenterDef } from '../../visualize/saga/ActiveRowCenterWatcher.js';
import {mocWatcherDef} from '../../visualize/saga/MOCWatcher.js';


export function startTTFeatureWatchers(startIds=[mocWatcherDef.id, catalogWatcherDef.id, urlLinkWatcherDef.id, getActiveRowCenterDef().id]) {
    startIds.includes(mocWatcherDef.id) && dispatchAddTableTypeWatcherDef(mocWatcherDef);
    startIds.includes(catalogWatcherDef.id) && dispatchAddTableTypeWatcherDef(catalogWatcherDef);
    startIds.includes(urlLinkWatcherDef.id) && dispatchAddTableTypeWatcherDef(urlLinkWatcherDef);
    startIds.includes(getActiveRowCenterDef.id) && dispatchAddTableTypeWatcherDef(getActiveRowCenterDef());
}