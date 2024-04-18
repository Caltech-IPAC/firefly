/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {Button, Checkbox, IconButton, Sheet, Stack, Switch, Tooltip, Typography} from '@mui/joy';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {
    ExpandType, dispatchChangeExpandedMode, dispatchExpandedAutoPlay, visRoot,
} from '../ImagePlotCntlr.js';
import {primePlot, getActivePlotView} from '../PlotViewUtil.js';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {DisplayTypeButtonGroup} from '../ui/Buttons.jsx';
import {ViewOptionsButton} from '../ui/ExpandedOptionsPopup.jsx';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {
    getMultiViewRoot, getExpandedViewerItemIds, getViewer, EXPANDED_MODE_RESERVED, dispatchViewerScroll,
} from '../MultiViewCntlr.js';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

import NavigateNext from '@mui/icons-material/NavigateNextRounded';
import NavigateBefore from '@mui/icons-material/NavigateBeforeRounded';
import ActiveDotIcon from '@mui/icons-material/FiberManualRecord';
import InactiveDotIcon from '@mui/icons-material/FiberManualRecordOutlined';

const viewerId= EXPANDED_MODE_RESERVED;


function createOptions(expandedMode, singleAutoPlay, plotIdAry) {
    return (
        <Stack {...{direction:'row', alignItems: 'center', flexWrap:'nowrap'}}>
            {(expandedMode===ExpandType.SINGLE && plotIdAry.length>1) ?
                <>
                    <ToolbarHorizontalSeparator/>
                    <Checkbox {...{label:'Auto play', size:'sm', checked:singleAutoPlay,
                        onChange:() => dispatchExpandedAutoPlay(!singleAutoPlay)
                    }} />
                    <ToolbarHorizontalSeparator/>
                </>
                : false
            }
        </Stack>
    );
}

function getState() {
    const {expandedMode,activePlotId, singleAutoPlay, plotViewAry}= visRoot();
    return {expandedMode,activePlotId, singleAutoPlay, plotViewAry, pv:getActivePlotView(visRoot())};
}

export function ExpandedTools({closeFunc}) {

    const {expandedMode,activePlotId, singleAutoPlay, plotViewAry}= useStoreConnector(getState);
    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const scroll= viewer?.scroll ?? false;
    const plotIdAry= viewer.itemIdAry;
    const single= plotViewAry?.length===1;

    const getPlotTitle = (plotId) => primePlot(visRoot(),plotId)?.title ?? '';


    return (
        <Sheet>
            <Stack { ...{direction:'row', alignItems:'center', sx: {px: .5}}}>
                {closeFunc && <CloseButton onClick={closeFunc}/>}
                {!single &&
                    <Stack {...{direction:'column', justifyContent:'space-between', minHeight:25, className:'disable-select'}}>
                        <div style={{alignSelf:'flex-end', whiteSpace:'nowrap', display:'flex'}}>
                            <WhichView expandedMode={expandedMode}/>
                            {!single && expandedMode===ExpandType.GRID &&
                                <>
                                    <ToolbarHorizontalSeparator/>
                                    <ViewerScroll {...{viewerId,checked:scroll,count:plotIdAry.length}}/>
                                </>
                            }
                            {createOptions(expandedMode,singleAutoPlay, plotIdAry)}
                            <PagingControl
                                viewerItemIds={getExpandedViewerItemIds(getMultiViewRoot())}
                                activeItemId={activePlotId}
                                isPagingMode={expandedMode===ExpandType.SINGLE}
                                getItemTitle={getPlotTitle}
                                onActiveItemChange={dispatchChangeActivePlotView}
                            />
                        </div>
                    </Stack>}
                <div style={{'flex': '1 1 auto'}}>
                    <VisMiniToolbar/>
                </div>
            </Stack>
        </Sheet>
    );
}

ExpandedTools.propTypes= {
    closeable : PropTypes.bool,
    closeFunc : PropTypes.func
};

const ViewerScrollTip=(
    <Typography  level='body-sm' width='50rem' component='div'>
        Use this mode when you have many images. When enabled...
        <ul>
            <li>You are able to use your browser to scroll through each image.</li>
            <li>Images will remain larger and the whole image area can be scrolled</li>
            <li>You may only zoom the active image in or out</li>
        </ul>
    </Typography>
);

export const ViewerScroll= ({viewerId,count,checked}) => (
    <Tooltip title={ViewerScrollTip}>
        <Switch size='sm'
                endDecorator={
                    <Typography sx={{textWrap:'nowrap'}} >
                        {checked ? `Scrolling ${count} images` : 'Scroll Images'}
                    </Typography>
                }
                checked={checked}
                onChange={() => dispatchViewerScroll({viewerId,scroll:!checked})} />
    </Tooltip>
);


function WhichView({expandedMode}) {
    const showViewButtons= getExpandedViewerItemIds(getMultiViewRoot()).length>1;
    const value= expandedMode===ExpandType.SINGLE ? 'one' : 'grid';
    return (
        <Stack direction='row' alignItems='center' pl={1}>
            {showViewButtons && <DisplayTypeButtonGroup {...{value,
                config:[
                    { value:'one', title:'Show single image at full size',
                        onClick: () => dispatchChangeExpandedMode(ExpandType.SINGLE)
                    },
                    { value:'grid', title:'Show all images as tiles',
                        onClick: () => dispatchChangeExpandedMode(ExpandType.GRID)
                    }
                ]
            }}/>}
            {showViewButtons && <ViewOptionsButton /> }
        </Stack>
    );
}

const emptyDiv= (<div style={{paddingLeft: 10, width: 300}}/>);
const pTitle= (begin,title) => title ? begin+title : '';

export function PagingControl({viewerItemIds,activeItemId,isPagingMode,getItemTitle,onActiveItemChange}) {

    if (!activeItemId || viewerItemIds.length<2 || !isPagingMode) return emptyDiv;
    const cIdx= viewerItemIds.indexOf(activeItemId);
    if (cIdx<0) return emptyDiv;

    const nextIdx= cIdx===viewerItemIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerItemIds.length-1;

    const dots= viewerItemIds.map( (plotId,idx) => {
        const active= idx===cIdx;
        const tip= active ? pTitle('Active Plot: ', getItemTitle(plotId)) : pTitle('Display: ', getItemTitle(plotId));
        return (
                <Tooltip title={tip} key={idx} >
                    <IconButton sz='sm' onClick={() => !active && onActiveItemChange(plotId)}
                                sx={{minHeight:5, minWidth:5, p:'1px'}}>
                        {active ? <ActiveDotIcon sx={{width:16}}/> : <InactiveDotIcon sx={{width:16}}/>}
                    </IconButton>
                </Tooltip>
            );
    });

    const leftTip= pTitle('Go to previous image: ',getItemTitle(viewerItemIds[prevIdx]));
    const rightTip= pTitle('Go to next image: ', getItemTitle(viewerItemIds[nextIdx]));

    return (
        <Stack {...{direction:'column', alignItems:'center', sx:{button:{minHeight:10}} }}>
            <Stack {...{direction:'row', alignItems:'center'}}>
                <Tooltip title={leftTip}>
                    <Button {...{size:'sm', variant:'plain',
                        onClick:() => onActiveItemChange(viewerItemIds[prevIdx]),
                        startDecorator:(<NavigateBefore/>)}}>
                    <span style={{maxWidth:'8em', textOverflow:'ellipsis', overflow:'hidden'}}>
                        {getItemTitle(viewerItemIds[prevIdx])}
                    </span>
                    </Button>
                </Tooltip>
                <div style={{flex: '1 1 auto'}}/>
                <Tooltip title={rightTip}>
                    <Button {...{size:'sm',variant:'plain',
                        onClick:() => onActiveItemChange(viewerItemIds[nextIdx]),
                        endDecorator:(<NavigateNext/>) }}>
                    <span style={{maxWidth:'5em', textOverflow:'ellipsis', overflow:'hidden'}}>
                        {getItemTitle(viewerItemIds[nextIdx])}
                    </span>
                    </Button>
                </Tooltip>
            </Stack>
            <Stack {...{direction:'row', mt:-.8, textAlign:'center', lineHeight: '5px'}}>
                {dots}
            </Stack>
        </Stack>
    );
}

PagingControl.propTypes= {
    viewerItemIds: PropTypes.arrayOf(PropTypes.string).isRequired,
    activeItemId: PropTypes.string,
    isPagingMode: PropTypes.bool.isRequired,
    getItemTitle : PropTypes.func.isRequired,
    onActiveItemChange : PropTypes.func.isRequired
};
