/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, ChipDelete, Stack, Typography} from '@mui/joy';
import {isString} from 'lodash';
import React, {memo} from 'react';
import {object, bool, number, string} from 'prop-types';
import {showInfoPopup} from '../../ui/PopupUtil';
import {PlotAttribute} from '../PlotAttribute';
import {makeMouseStatePayload, fireMouseCtxChange, MouseState} from '../VisMouseSync.js';
import {dispatchDeletePlotView, visRoot} from '../ImagePlotCntlr.js';
import {primePlot, pvEqualExScroll} from '../PlotViewUtil.js';
import shallowequal from 'shallowequal';
import {WarningButton} from './Buttons';



export const VisInlineToolbarView = memo( (props) => {
        const {pv, showDelete,deleteVisible, topOffset=0}= props;
        if (!pv) return undefined;
        const deleteClick= () => {
            const mouseStatePayload= makeMouseStatePayload(undefined,MouseState.EXIT,undefined,0,0);
            fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse
            dispatchDeletePlotView({plotId:pv.plotId});
        };

        const deleteStyle= {
            visibility: deleteVisible ? 'visible' : 'hidden',
            opacity: deleteVisible ? 1 : 0,
            transition: deleteVisible ? 'opacity .15s linear' : 'visibility 0s .15s, opacity .15s linear',
        };

        const warnAry= getWarningsAry(pv);
        if (!showDelete && !warnAry?.length) return;

        return (
            <Box style={{ top: topOffset, position : 'absolute', right : 0}}>
                <Stack {...{direction:'row', alignItems:'center',
                        position: 'relative', sx:{verticalAlign: 'top', zIndex : 1} }}>
                    <WarningsAlert pv={pv}/>
                    {showDelete &&
                        <ChipDelete onClick={deleteClick}
                                    sx={{alignSelf:'flex-start', minHeight:12, minWidth:12, p:.5, ...deleteStyle}}
                                    title='Remove Image'/>}
                </Stack>
            </Box>
        );
    },
    (p,nP) => shallowequal({...p, pv:undefined}, {...nP,pv:undefined}) && pvEqualExScroll(p.pv, nP.pv)
);


function getWarningsAry(pv) {
    const warnings= primePlot(visRoot(),pv.plotId)?.attributes[PlotAttribute.USER_WARNINGS] ?? {};
    return Object.entries(warnings ?? {})
        .filter(([k]) => (k!=='title' && k!=='tooltip'))
        .map(([,v]) => v);
}



function WarningsAlert({pv}) {
    const warnings= primePlot(visRoot(),pv.plotId)?.attributes[PlotAttribute.USER_WARNINGS] ?? {};
    const warnAry= getWarningsAry(pv);
    if (!warnAry?.length) return;

    return (
        <WarningButton {...{
            tip:warnings.tooltip ?? 'warnings',
            onClick: () => {
                const wc= (
                    <Stack {...{spacing:1, width:1}}>
                        { warnAry.map( (s) => isString(s) ? <Typography key={s}>{s}</Typography> : s) }
                    </Stack>
                );
                showInfoPopup( wc, warnings.title ?? 'Warnings', { '.FF-Popup-Content':  {width: '50rem'} } );
            },
        }}
        />);
}




VisInlineToolbarView.propTypes= {
    pv : object,
    showDelete : bool,
    deleteVisible : bool,
    help_id : string,
    topOffset: number
};
