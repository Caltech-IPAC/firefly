/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import {once} from 'lodash';
import {startDataProductsWatcher} from '../../saga/DataProductsWatcher.js';
import {MultiProductViewer} from './MultiProductViewer.jsx';


const startWatcher= once((dpId) => startDataProductsWatcher({ dataTypeViewerId:dpId, dpId}) );


/**
 * A wrapper for MultiProductViewer for data Products that starts the watcher that updates it.
 * It should be used in the case where this is the only if it's the only one the page.
 * If you ar have more that on MultiProductViewer you should lay the out directly
 */
export const MetaDataMultiProductViewer= memo(({ viewerId='DataProductsType', dataProductTableId,
                                                   enableExtraction= false,
                                                   autoStartWatcher=true, noProductMessage}) => {
    autoStartWatcher && setTimeout(() => startWatcher(viewerId),5);
    return (<MultiProductViewer {...{viewerId, metaDataTableId:dataProductTableId,
        noProductMessage, enableExtraction}}/>);
});