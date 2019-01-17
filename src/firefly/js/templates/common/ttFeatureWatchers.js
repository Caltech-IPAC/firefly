import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {catalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';


export function startTTFeatureWatchers(startIds=[catalogWatcherDef.id]) {
    startIds.includes(catalogWatcherDef.id) && dispatchAddTableTypeWatcherDef(catalogWatcherDef);
}