
import React from 'react';
import PropTypes from 'prop-types';
import {Sheet, Stack, Typography} from '@mui/joy';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {CONE_CHOICE_KEY} from './CommonUIKeys.js';
import {SelectAreaButton} from './SelectAreaDropDownView.jsx';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {getActivePlotView} from '../PlotViewUtil.js';



export const HelpLines= ({whichOverlay}) => {
    const modalEndInfo = useStoreConnector(() => getComponentState('ModalEndInfo', {}));
    const setModalEndInfo= (info) => dispatchComponentStateChange('ModalEndInfo',  {...{}, ...info});
    const imageStyle={width:16, height:16};

    const selectButton= (
        <SelectAreaButton {...{pv:getActivePlotView(visRoot()),modalEndInfo,setModalEndInfo,
            tip:'Reselect an area for search', imageStyle, style:{paddingTop:3}}}/>
    );
    const nowrap= {whiteSpace:'nowrap'};
    return (
        <Stack {...{direction:'row', alignItems:'center', pl:.5 }}>
            {whichOverlay===CONE_CHOICE_KEY ?
                (<>
                    <Typography level='body-xs' sx={nowrap}>Click to choose a search center, or use the Selection Tools (</Typography>
                    {selectButton}
                    <Typography level='body-xs' sx={nowrap}>) to choose a search center and radius.</Typography>
                </> ) :
                (<>
                    <Typography level='body-xs' sx={nowrap}>Use the Selection Tools (</Typography>
                    {selectButton}
                    <Typography level='body-xs' sx={nowrap}>) to choose a search polygon. Click to change the center.
                    </Typography>
                </> )}
        </Stack>
    );

};


export function TargetHipsPanelToolbar({visRoot, toolbarStyle={},
                                           whichOverlay= CONE_CHOICE_KEY, viewerId,
                                           toolbarHelpId='hips.VisualSelection'}) {
    const {showImageToolbar=true}= getActivePlotView(visRoot)?.plotViewCtx.menuItemKeys ?? {};
    if (!showImageToolbar) return <div/>;

    return (
        <Stack {...{direction:'column', alignItems:'flex-end', justifyContent: 'space-between',
            flexWrap:'nowrap',  height: 33, style:toolbarStyle}}>
            <Stack direction='row' justifyContent='flex-start'>
                <VisMiniToolbar style={{width:'unset'}} viewerId={viewerId}
                                tips={{selectArea:'Select an area to search'}}/>
                <HelpIcon helpId={toolbarHelpId} />
            </Stack>
        </Stack>
    );
}


TargetHipsPanelToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    makeDropDownFunc: PropTypes.func,
    makeDropDown: PropTypes.bool,
    toolbarStyle: PropTypes.object,
    whichOverlay: PropTypes.string,
    toolbarHelpId: PropTypes.string,
};

