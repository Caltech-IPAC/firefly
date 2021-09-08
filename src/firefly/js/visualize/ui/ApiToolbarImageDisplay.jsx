/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';


export const ApiToolbarImageDisplay= memo(({viewerId, renderTreeId}) => {
    return (
        <RenderTreeIdCtx.Provider value={{renderTreeId}}>
            <div style={{width:'100%', height:'100%', display:'flex', flexWrap:'nowrap',
                alignItems:'stretch', flexDirection:'column', position: 'relative'}}>
                <div style={{flex: '1 1 auto', display:'flex', width: '100%'}}>
                    <MultiImageViewer viewerId= {viewerId}
                                      insideFlex={true}
                                      canReceiveNewPlots={NewPlotMode.create_replace.key}
                                      Toolbar={MultiViewStandardToolbar}/>
                </div>
            </div>
        </RenderTreeIdCtx.Provider>
    );
});

ApiToolbarImageDisplay.propTypes= {
    forceExpandedMode : PropTypes.bool,
    closeFunc: PropTypes.func,
    viewerId: PropTypes.string,
    renderTreeId : PropTypes.string,
    showHealpixPixel: PropTypes.string // getAppOptions()?.hips?.readoutShowsPixel
};

ApiToolbarImageDisplay.defaultProps= {
    closeFunc:null
};

