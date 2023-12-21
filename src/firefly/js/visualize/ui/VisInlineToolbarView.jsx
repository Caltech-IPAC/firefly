/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, IconButton, Stack} from '@mui/joy';
import React, {memo} from 'react';
import {makeMouseStatePayload, fireMouseCtxChange, MouseState} from '../VisMouseSync.js';
import PropTypes from 'prop-types';
import {dispatchDeletePlotView} from '../ImagePlotCntlr.js';
import {pvEqualExScroll} from '../PlotViewUtil.js';
import shallowequal from 'shallowequal';
import DELETE from 'images/blue_delete_10x10.png';



export const VisInlineToolbarView = memo( (props) => {
        const {pv, showDelete,show, topOffset=0}= props;
        if (!pv) return undefined;
        const deleteClick= () => {
            const mouseStatePayload= makeMouseStatePayload(undefined,MouseState.EXIT,undefined,0,0);
            fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse
            dispatchDeletePlotView({plotId:pv.plotId});
        };

        const topStyle= {
            visibility: show ? 'visible' : 'hidden',
            opacity: show ? 1 : 0,
            transition: show ? 'opacity .15s linear' : 'visibility 0s .15s, opacity .15s linear',
            top: topOffset,
            position : 'absolute',
            right : 0,
        };

        return (
            <Box style={topStyle}>
                <Stack {...{direction:'row', alignItems:'center',
                        position: 'relative', sx:{verticalAlign: 'top', zIndex : 1} }}>
                    {showDelete &&
                        <IconButton onClick={deleteClick}
                                    sx={{alignSelf:'flex-start', minHeight:12, minWidth:12, p:.5}}
                                    title='Delete Image'>
                            <img src={DELETE}/>
                        </IconButton>}
                </Stack>
            </Box>
        );
    },
    (p,nP) => shallowequal({...p, pv:undefined}, {...nP,pv:undefined}) && pvEqualExScroll(p.pv, nP.pv)
);

VisInlineToolbarView.propTypes= {
    pv : PropTypes.object,
    showDelete : PropTypes.bool,
    show : PropTypes.bool,
    help_id : PropTypes.string,
    topOffset: PropTypes.number
};
