/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, ChipDelete, Stack, Typography} from '@mui/joy';
import {isString} from 'lodash';
import React, {memo} from 'react';
import {object, bool, number, string, func} from 'prop-types';
import {showInfoPopup} from '../../ui/PopupUtil';
import {checkProps} from '../../ui/SimpleComponent';
import {PlotAttribute} from '../PlotAttribute';
import {makeMouseStatePayload, fireMouseCtxChange, MouseState} from '../VisMouseSync.js';
import {dispatchDeletePlotView} from '../ImagePlotDispatch';
import {currentP, pvEqualExScroll} from '../PlotViewUtil.js';
import shallowequal from 'shallowequal';
import {WarningButton} from './Buttons';



export const PvControlsAndFeedback = memo( (props) => {
        const {pv, showDelete,controlsVisible, makeLegend}= props;
        checkProps(props, PvControlsAndFeedback);
        if (!pv) return undefined;
        const deleteClick= () => {
            const mouseStatePayload= makeMouseStatePayload(undefined,MouseState.EXIT,undefined,0,0,'');
            fireMouseCtxChange(mouseStatePayload);  // this for anyone listening directly to the mouse
            dispatchDeletePlotView({plotId:pv.plotId});
        };

        const deleteStyle= {
            visibility: controlsVisible ? 'visible' : 'hidden',
            opacity: controlsVisible ? 1 : 0,
            transition: controlsVisible ? 'opacity .15s linear' : 'visibility 0s .15s, opacity .15s linear',
        };

        const warnAry= getWarningsAry(pv);
        const legend= makeLegend?.(pv.plotId);
        if (!showDelete && !warnAry?.length && !legend) return;
        const legendAdd= pv.plotViewCtx.useForSearchResults && !pv.plotViewCtx.useForCoverage ?  deleteStyle : {};

        return (
            <Box style={{ top: 0, position : 'absolute', right : 0}}>
                <Stack {...{sx: {zIndex : 1, position: 'relative'}}}>
                    <Stack {...{direction:'row', alignItems:'center',  sx:{verticalAlign: 'top'} }}>
                        <WarningsAlert pv={pv}/>
                        {showDelete &&
                            <ChipDelete onClick={deleteClick}
                                        sx={{alignSelf:'flex-start', minHeight:12, minWidth:12, p:.5,
                                            ...deleteStyle}}
                                        title='Remove Image'/>}
                    </Stack>
                    { makeLegend &&
                        <Box sx={{ top: showDelete ? 30 : 37, position : 'absolute', right : 0, ...legendAdd}} >
                            {legend}
                        </Box>
                    }

                </Stack>
            </Box>
        );
    },
    (p,nP) => shallowequal({...p, pv:undefined}, {...nP,pv:undefined}) && pvEqualExScroll(p.pv, nP.pv)
);


function getWarningsAry(pv) {
    const warnings= currentP(pv.plotId).plot?.attributes[PlotAttribute.USER_WARNINGS] ?? {};
    if (isString(warnings)) {
        return [<Typography>{warnings}</Typography>];
    }
    return Object.entries(warnings ?? {})
        .filter(([k]) => (k!=='title' && k!=='tooltip'))
        .map(([,v]) => v);
}



function WarningsAlert({pv}) {
    const warnings= currentP(pv.plotId).plot?.attributes[PlotAttribute.USER_WARNINGS] ?? {};
    const defaultTip= isString(warnings) ? warnings : 'click for warnings';
    const warnAry= getWarningsAry(pv);
    if (!warnAry?.length) return;

    return (
        <WarningButton {...{
            tip:warnings.tooltip ?? defaultTip,
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




PvControlsAndFeedback.propTypes= {
    pv : object,
    showDelete : bool,
    controlsVisible : bool,
    help_id : string,
    topOffset: number,
    makeLegend : func,
};
