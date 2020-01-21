/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useContext, useState, useEffect} from 'react';
import {once} from 'lodash';
import {startDataProductsWatcher} from '../saga/DataProductsWatcher';
import {MultiProductViewer} from './MultiProductViewer';


const startWatcher= once((dpId) => startDataProductsWatcher({ dataTypeViewerId:dpId, dpId}) );


/**
 * A wrapper for MultiProductViewer for data Products that starts the watcher that updates it.
 * It should be use in the case where this is the only if it the the only one one the page.
 * If you are have more that on MultiProductViewer you should lay the out directly
 */
export const MetaDataMultiProductViewer= memo(({ dpId='DataProductsType', metaDataTableId, autoStartWatcher=true, }) => {
    autoStartWatcher && startWatcher(dpId);
    return (<MultiProductViewer viewerId={dpId} metaDataTableId={metaDataTableId}/>);
});
