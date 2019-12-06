/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {MultiProductViewer} from './MultiProductViewer';
import {DEFAULT_FITS_VIEWER_ID, NewPlotMode} from '../MultiViewCntlr.js';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {CHART_DATA_VIEWER_ID, META_DATA_TBL_GROUP_ID} from './TriViewImageSection';
import {META_VIEWER_ID} from '../MultiViewCntlr';

const closeExpanded= () => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);

/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 */
export function MultiProductViewerContainer({viewerId= 'DataProductsType',
                                                metaDataTableId,
                                                tableGroupViewerId= META_DATA_TBL_GROUP_ID,
                                                chartMetaViewerId= CHART_DATA_VIEWER_ID,
                                                imageMetaViewerId= META_VIEWER_ID,
                                                imageExpandedMode=false, closeable=true, insideFlex=false,
                                           forceRowSize, Toolbar= MultiViewStandardToolbar}) {
    
    if (imageExpandedMode) {
        return  ( <ImageExpandedMode
                        key='results-plots-expanded'
                        insideFlex = {insideFlex}
                        closeFunc={closeable ? closeExpanded : null}/>
                );
    } else {
        return (
            <MultiProductViewer viewerId={viewerId}
                                            metaDataTableId={metaDataTableId}
                                            tableGroupViewerId={tableGroupViewerId}
                                            chartMetaViewerId={chartMetaViewerId}
                                            imageMetaViewerId={imageMetaViewerId}/>
        );
    }
}


MultiProductViewerContainer.propTypes = {
    viewerId: PropTypes.string,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
};
