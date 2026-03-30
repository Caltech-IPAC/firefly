import {Stack, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import React from 'react';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';
import HelpIcon from '../../ui/HelpIcon.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {getActivePlotView} from '../PlotViewUtil.js';
import {BOX_CHOICE_KEY, CONE_CHOICE_KEY} from './CommonUIKeys.js';
import {SelectAreaButton} from './SelectAreaUIComponents.jsx';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';

export const targetHipsDefaultMenuItemKey = {
    zoomDropDownMenu: false, overlayColorLock: false, matchLockDropDown: false, clickToSearch: false,
    recenter: false, selectArea: true, restore: false, maskOverlay: false, rotate: false, flipImageY: false,
    stretchQuick: false, selectTableRows: false, unselectTableRows: false, filterTableRows: false,
    clearTableFilters: false, zoomToSelection: false, recenterToSelection: false, imageStatistics: false,
};


export const HelpLines= ({whichOverlay, selectionHelpText, usingRadius}) => {
    const modalEndInfo = useStoreConnector(() => getComponentState('ModalEndInfo', {}));
    const setModalEndInfo= (info) => dispatchComponentStateChange('ModalEndInfo',  {...{}, ...info});
    const imageStyle={width:16, height:16};

    const selectButton= (
        <SelectAreaButton {...{pv:getActivePlotView(visRoot()),modalEndInfo,setModalEndInfo,
            tip:'Reselect an area for search', imageStyle, style:{paddingTop:3}}}/>
    );

    const simpleText= selectionHelpText
        ? selectionHelpText
        : (whichOverlay===CONE_CHOICE_KEY && !usingRadius)
            ? 'Click to choose a search center'
            : undefined;


    const nowrap= {whiteSpace:'nowrap'};
    if (simpleText) {
        return (
            <Stack {...{direction:'row', alignItems:'center', pl:.5 }}>
                <Typography level='body-xs' sx={nowrap}>{simpleText}</Typography>
            </Stack>
        );
    }
    else {
        return (
            <Stack {...{direction:'row', alignItems:'center', pl:.5 }}>
                {whichOverlay===CONE_CHOICE_KEY ?
                    (<>
                        <Typography level='body-xs' sx={nowrap}>Click to choose a search center, or use the Selection Tools
                         to choose a search center and radius.</Typography>
                    </> ) : whichOverlay===BOX_CHOICE_KEY ?
                    (<>
                        <Typography level='body-xs' sx={nowrap}>Use the Selection Tools
                            to choose a search box. Click to change the center.</Typography>
                    </> ) :
                    (<>
                        <Typography level='body-xs' sx={nowrap}>Use the Selection Tools
                         to choose a search polygon. Click to change the center.
                        </Typography>
                    </> )}
            </Stack>
        );
    }
};


export function TargetHipsPanelToolbar({visRoot, toolbarStyle={},
                                           whichOverlay= CONE_CHOICE_KEY, viewerId, menuItemKeys,
                                           toolbarHelpId='hips.VisualSelection'}) {
    const {showImageToolbar=true}= getActivePlotView(visRoot)?.plotViewCtx.menuItemKeys ?? {};
    if (!showImageToolbar) return <div/>;

    return (
        <Stack {...{direction:'column', alignItems:'flex-end', justifyContent: 'space-between',
            flexWrap:'nowrap',  height: 33, style:toolbarStyle}}>
            <Stack direction='row' justifyContent='flex-start'>
                <VisMiniToolbar style={{width:'unset'}} viewerId={viewerId} menuItemKeys={menuItemKeys}
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
    menuItemKeys: PropTypes.object,
};
