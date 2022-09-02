/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {getActiveTableId} from '../../tables/TableUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {isDataProductsTable} from '../../util/VOAnalyzer.js';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {MetaDataMultiProductViewer} from './MetaDataMultiProductViewer';

const closeExpanded= () => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);

/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 */
export function MultiProductViewerContainer({ tbl_id= undefined, imageExpandedMode=false,
                                              closeable=true, insideFlex=false,
                                                enableExtraction= false,
                                                noProductMessage= 'No Data Products Available'}) {


    const dataProductsTblId= useStoreConnector((lastTblId=undefined) => {
        const tbl_id= getActiveTableId('main');
        if (tbl_id===lastTblId) return tbl_id;
        return isDataProductsTable(tbl_id) ? tbl_id : lastTblId;
    });
    const dpTbl= tbl_id ?? dataProductsTblId;

    if (imageExpandedMode) {
        return  ( <ImageExpandedMode key='results-plots-expanded' insideFlex = {insideFlex}
                        closeFunc={closeable ? closeExpanded : null}/> );
    } else {
        return (
            <MetaDataMultiProductViewer {...{dataProductTableId:dpTbl,
                noProductMessage, enableExtraction}}/> );
    }
}


MultiProductViewerContainer.propTypes = {
    enableExtraction: PropTypes.bool,
    noProductMessage: PropTypes.string,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
    tbl_id: PropTypes.string, // optional - almost next used. Automaticly found
};
