/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {bool, object, string} from 'prop-types';
import {getActiveTableId, getTblById} from '../../tables/TableUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {isDataProductsTable} from '../../voAnalyzer/TableAnalysis.js';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {MetaDataMultiProductViewer} from './multiProduct/MetaDataMultiProductViewer.jsx';

const closeExpanded= () => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);

let lastMetaDataTbls=[];

function getMetaDataTbl() {
    const tbl_id= getActiveTableId('main');
    const tblAry= lastMetaDataTbls;
    if (!tbl_id && tblAry.length===0) return;
    if (isDataProductsTable(tbl_id)) {
        if (tblAry[0]!==tbl_id) { //make sure it is the first entry
            lastMetaDataTbls= tblAry.filter( (id) => id!==tbl_id);
            lastMetaDataTbls.unshift(tbl_id);
        }
        return tbl_id;
    }
    else {
        const newTblId= tblAry.find( (id) => {
            return Boolean(getTblById(id));
        });
        if (newTblId!==tblAry[0]) { // filter out any dead tables
            lastMetaDataTbls= tblAry.filter( (id) => Boolean(getTblById(id)));
        }
        return newTblId;
    }
}


/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 */
export function MultiProductViewerContainer({ viewerId, tbl_id= undefined, imageExpandedMode=false,
                                              closeable=true, insideFlex=false,
                                                enableExtraction= false,
                                                dataProductsFactoryOptions,
                                                noProductMessage= 'No Data Products Available'}) {


    const dataProductsTblId= useStoreConnector(() => getMetaDataTbl() );
    const dpTbl= tbl_id ?? dataProductsTblId;

    if (imageExpandedMode) {
        return  ( <ImageExpandedMode key='results-plots-expanded' insideFlex = {insideFlex}
                        closeFunc={closeable ? closeExpanded : null}/> );
    } else {
        return (
            <MetaDataMultiProductViewer {...{dataProductTableId:dpTbl, dataProductsFactoryOptions,
                noProductMessage, enableExtraction, viewerId}}/> );
    }
}

MultiProductViewerContainer.propTypes = {
    enableExtraction: bool,
    viewerId: string,
    noProductMessage: string,
    imageExpandedMode : bool,
    closeable: bool,
    insideFlex: bool,
    dataProductsFactoryOptions: object,
    tbl_id: string, // optional - almost next used. Automaticly found
};
