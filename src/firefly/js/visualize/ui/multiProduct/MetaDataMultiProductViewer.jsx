/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {bool, string, object} from 'prop-types';
import React, {memo} from 'react';
import {dispatchComponentStateChange, getComponentState} from '../../../core/ComponentCntlr';
import {setFactoryTemplateOptions, getDefaultFactoryOptions} from '../../../metaConvert/DataProductsFactory.js';
import {startDataProductsWatcher} from '../../../metaConvert/DataProductsWatcher.js';
import {SD_CUTOUT_KEY, SD_DEFAULT_SPACIAL_CUTOUT_SIZE} from '../../../metaConvert/vo/ServDescProducts';
import {getObsCoreOption} from '../../../ui/tap/TableSearchHelpers';
import {MultiProductViewer} from './MultiProductViewer.jsx';

const startedWatchers=[];

function startWatcher(dpId, options) {
    if (startedWatchers.includes(dpId)) return;
    setFactoryTemplateOptions(dpId, options);
    startDataProductsWatcher({ dataTypeViewerId:dpId, factoryKey:dpId});
    startedWatchers.push(dpId);
    const defOps= getDefaultFactoryOptions();
    const key= options.dataProductsComponentKey ?? defOps.dataProductsComponentKey;
    if (!getComponentState(key,{})[SD_CUTOUT_KEY]) {
        dispatchComponentStateChange(key,{
            [SD_CUTOUT_KEY]: (getObsCoreOption('cutoutDefSizeDeg') ?? SD_DEFAULT_SPACIAL_CUTOUT_SIZE)
        } );
    }
}


/**
 * A wrapper for MultiProductViewer for data Products that starts the watcher that updates it.
 * It should be used in the case where this is the only if it's the only one the page.
 * If you ar have more that on MultiProductViewer you should lay the out directly
 */
export const MetaDataMultiProductViewer= memo(({
                                                   viewerId='DataProductsType', dataProductTableId,
                                                   autoStartWatcher=true, enableExtraction= false, noProductMessage,
                                                   dataProductsFactoryOptions= getDefaultFactoryOptions()}) => {
    autoStartWatcher && setTimeout(() => startWatcher(viewerId,dataProductsFactoryOptions),5);
    return (<MultiProductViewer {...{viewerId, metaDataTableId:dataProductTableId,
        noProductMessage, enableExtraction, factoryKey:viewerId}}/>);
});

MetaDataMultiProductViewer.propTypes= {
    viewerId : string,
    metaDataTableId : string,
    enableExtraction: bool,
    autoStartWatcher: bool,
    noProductMessage: string,
    dataProductTableId: string,
    dataProductsFactoryOptions: object
};
