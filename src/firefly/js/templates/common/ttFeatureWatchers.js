import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {catalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {urlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {activeRowCenterDef } from '../../visualize/saga/ActiveRowCenterWatcher.js';
import {mocWatcherDef} from '../../visualize/saga/MOCWatcher.js';


export function startTTFeatureWatchers(startIds=[mocWatcherDef.id, catalogWatcherDef.id, urlLinkWatcherDef.id, activeRowCenterDef.id]) {
    startIds.includes(mocWatcherDef.id) && dispatchAddTableTypeWatcherDef(mocWatcherDef);
    startIds.includes(catalogWatcherDef.id) && dispatchAddTableTypeWatcherDef(catalogWatcherDef);
    startIds.includes(urlLinkWatcherDef.id) && dispatchAddTableTypeWatcherDef(urlLinkWatcherDef);
    startIds.includes(activeRowCenterDef.id) && dispatchAddTableTypeWatcherDef(activeRowCenterDef);
}