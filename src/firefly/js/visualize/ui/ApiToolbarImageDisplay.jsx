/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack} from '@mui/joy';
import React, {memo} from 'react';
import {string} from 'prop-types';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';


export const ApiToolbarImageDisplay= memo(({viewerId, renderTreeId}) => (
        <RenderTreeIdCtx.Provider value={{renderTreeId}}>
            <Stack {...{width:1, height:1, flexWrap:'nowrap', alignItems:'stretch', position: 'relative',
                border: '1px solid', borderColor: 'divider', borderRadius: '5px'
            }}>
                <Stack {...{flex: '1 1 auto', direction:'row', width:1}}>
                    <MultiImageViewer viewerId= {viewerId}
                                      insideFlex={true}
                                      toolbarVariant='soft'
                                      canReceiveNewPlots={NewPlotMode.create_replace.key}
                                      Toolbar={MultiViewStandardToolbar}/>
                </Stack>
            </Stack>
        </RenderTreeIdCtx.Provider>
    ));

ApiToolbarImageDisplay.propTypes= {
    viewerId: string,
    renderTreeId : string,
};

