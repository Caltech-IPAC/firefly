/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {MultiProductViewer} from './MultiProductViewer';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';

const closeExpanded= () => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);

/**
 * A wrapper component for MultiImageViewer where expended mode is supported.
 */
export function MultiProductViewerContainer({ metaDataTableId, imageExpandedMode=false, closeable=true, insideFlex=false}) {
    
    if (imageExpandedMode) {
        return  ( <ImageExpandedMode
                        key='results-plots-expanded'
                        insideFlex = {insideFlex}
                        closeFunc={closeable ? closeExpanded : null}/>
                );
    } else {
        return (
            <MultiProductViewer metaDataTableId={metaDataTableId} />
        );
    }
}


MultiProductViewerContainer.propTypes = {
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    insideFlex: PropTypes.bool,
};
