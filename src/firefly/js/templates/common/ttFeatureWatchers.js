import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {catalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {urlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';


export function startTTFeatureWatchers(startIds=[catalogWatcherDef.id, urlLinkWatcherDef.id]) {
    startIds.includes(catalogWatcherDef.id) && dispatchAddTableTypeWatcherDef(catalogWatcherDef);
    startIds.includes(urlLinkWatcherDef.id) && dispatchAddTableTypeWatcherDef(urlLinkWatcherDef);
}