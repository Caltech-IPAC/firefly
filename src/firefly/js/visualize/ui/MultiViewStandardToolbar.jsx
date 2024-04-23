/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {Divider, Sheet, Stack} from '@mui/joy';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {ViewerScroll} from '../iv/ExpandedTools.jsx';
import {dispatchChangeViewerLayout, getMultiViewRoot, getViewer} from '../MultiViewCntlr.js';
import {dispatchChangeActivePlotView, ExpandType} from '../ImagePlotCntlr.js';
import {BeforeButton, DisplayTypeButtonGroup, NextButton } from './Buttons.jsx';
import {ViewOptionsButton} from './ExpandedOptionsPopup.jsx';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {getActivePlotView} from '../PlotViewUtil.js';


export function MultiViewStandardToolbar({visRoot, viewerId, viewerPlotIds, toolbarVariant,
                                          layoutType= 'grid', makeDropDown, useImageList= false,
                                             toolbarStyle={}}) {
    
    let cIdx= viewerPlotIds.findIndex( (plotId) => plotId===visRoot.activePlotId);

    if (cIdx<0) cIdx= 0;

    const moreThanOne= viewerPlotIds.length>1;
    const nextIdx= cIdx===viewerPlotIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerPlotIds.length-1;
    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const layout= viewer?.layout ?? 'grid';
    const scroll= viewer?.scroll ?? false;


    const {showImageToolbar=true}= getActivePlotView(visRoot)?.plotViewCtx.menuItemKeys ?? {};
    if (!showImageToolbar) return <div/>;

    return (
        <Sheet variant={toolbarVariant}>
            <Stack {...{pl:1/2, direction:'row', flexWrap:'wrap-reverse', alignItems: 'center', justifyContent: 'space-between',
                width:'100%', minHeight: 32, style:toolbarStyle}}>
                <Stack {...{direction:'row', alignItems: 'center', flexWrap:'nowrap'}}>

                    {moreThanOne && <DisplayTypeButtonGroup {...{value:layout,
                        config:[
                            { value:'single', title:'Show single image at full size',
                                onClick: () => dispatchChangeViewerLayout(viewerId,'single')
                            },
                            { value:'grid', title:'Show all images as tiles',
                                onClick: () => dispatchChangeViewerLayout(viewerId,'grid')
                            }
                        ]
                    }}/>}
                    {useImageList && moreThanOne && <ViewOptionsButton title='Pinned Images' viewerId={viewerId}/> }
                    {moreThanOne && layout==='grid' &&
                        <>
                            <ToolbarHorizontalSeparator/>
                            <ViewerScroll {...{viewerId,checked:scroll,count:viewerPlotIds.length}}/>
                        </>
                    }
                    {layoutType==='single' && moreThanOne &&
                        <>
                        <BeforeButton
                            title='Previous Image'
                            onClick= {() => dispatchChangeActivePlotView(viewerPlotIds[prevIdx])}/>
                        <NextButton
                            title='Next Image'
                            onClick= {() => dispatchChangeActivePlotView(viewerPlotIds[nextIdx])}/>
                        </>
                    }
                    {makeDropDown?.()}
                </Stack>
                <VisMiniToolbar viewerId={viewerId}/>
            </Stack>
        </Sheet>
    );
}


MultiViewStandardToolbar.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    visRoot : PropTypes.object,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    makeDropDownFunc: PropTypes.func,
    makeDropDown: PropTypes.bool,
    useImageList: PropTypes.bool,
    toolbarStyle: PropTypes.object,
    toolbarVariant: PropTypes.string,
};
