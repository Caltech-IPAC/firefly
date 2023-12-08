/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {Button, Checkbox, Divider, IconButton, Stack, Tooltip} from '@mui/joy';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {ExpandType, dispatchChangeExpandedMode, dispatchExpandedAutoPlay, visRoot } from '../ImagePlotCntlr.js';
import {primePlot, getActivePlotView} from '../PlotViewUtil.js';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {showExpandedOptionsPopup} from '../ui/ExpandedOptionsPopup.jsx';
import {dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {getMultiViewRoot, getExpandedViewerItemIds} from '../MultiViewCntlr.js';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import LIST from 'html/images/icons-2014/ListOptions.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';
import ACTIVE_DOT from 'html/images/green-dot-10x10.png';
import INACTIVE_DOT from 'html/images/blue-dot-10x10.png';

function createOptions(expandedMode, singleAutoPlay, plotIdAry) {
    return (
        <Stack {...{direction:'row', alignItems: 'center', flexWrap:'nowrap'}}>
            {(expandedMode===ExpandType.SINGLE && plotIdAry.length>1) ?
                <>
                    <Divider orientation='vertical' sx={{mx:1}}/>
                    <Checkbox {...{label:'Auto Play', checked:singleAutoPlay,
                        onChange:() => dispatchExpandedAutoPlay(!singleAutoPlay)
                    }} />
                    <Divider orientation='vertical' sx={{mx:1}}/>
                </>
                : false
            }
        </Stack>
    );
}

const closeButtonStyle= {
    display: 'inline-block',
    padding: '1px 12px 0 1px'
};

function getState() {
    const {expandedMode,activePlotId, singleAutoPlay, plotViewAry}= visRoot();
    return {expandedMode,activePlotId, singleAutoPlay, plotViewAry, pv:getActivePlotView(visRoot())};
}

export function ExpandedTools({closeFunc}) {

    const {expandedMode,activePlotId, singleAutoPlay, plotViewAry}= useStoreConnector(getState);
    const plotIdAry= getExpandedViewerItemIds(getMultiViewRoot());
    const single= plotViewAry===1;

    const getPlotTitle = (plotId) => primePlot(visRoot(),plotId)?.title ?? '';

    return (
        <Stack { ...{direction:'row', alignItems:'center', borderBottom: '1px solid rgba(0,0,0,.2)' }}>
            {closeFunc && <CloseButton style={closeButtonStyle} onClick={closeFunc}/>}
            {!single &&
                <Stack {...{direction:'column', justifyContent:'space-between', minHeight:25, className:'disable-select'}}>
                    <div style={{alignSelf:'flex-end', whiteSpace:'nowrap', display:'flex'}}>
                        <WhichView/>
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
    );
}

ExpandedTools.propTypes= {
    closeable : PropTypes.bool,
    closeFunc : PropTypes.func
};




function WhichView() {
    const showViewButtons= getExpandedViewerItemIds(getMultiViewRoot()).length>1;
    return (
        <Stack direction='row' alignItems='center'>
            {showViewButtons &&
                   <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                                  imageStyle={{width:24,height:24}}
                                  enabled={true} visible={true}
                                  horizontal={true}
                                  onClick={() => dispatchChangeExpandedMode(ExpandType.SINGLE)}/>}
            {showViewButtons &&
                   <ToolbarButton icon={GRID} tip={'Show all as tiles'}
                                  enabled={true} visible={true} horizontal={true}
                                  imageStyle={{width:24,height:24}}
                                  onClick={() => dispatchChangeExpandedMode(ExpandType.GRID)}/>
            }
            {showViewButtons &&
                   <ToolbarButton icon={LIST} tip={'Choose which plots to show'}
                                  imageStyle={{width:24,height:24}}
                                  enabled={true} visible={true} horizontal={true}
                                  onClick={() =>showExpandedOptionsPopup() }/>
            }
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
                                sx={{minHeight:5, minWidth:5, p:'2px'}}>
                        <img src={active ? ACTIVE_DOT : INACTIVE_DOT}/>
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
                    <Button {...{size:'sm', variant:'plain', color:'neutral',
                        onClick:() => onActiveItemChange(viewerItemIds[prevIdx]),
                        startDecorator:(<img src={PAGE_LEFT}/>)}}>
                        <span style={{maxWidth:'8em', textOverflow:'ellipsis', overflow:'hidden'}}>
                            {getItemTitle(viewerItemIds[prevIdx])}
                        </span>
                    </Button>
                </Tooltip>
                <div style={{flex: '1 1 auto'}}/>
                <Tooltip title={rightTip}>
                    <Button {...{size:'sm', variant:'plain', color:'neutral',
                        onClick:() => onActiveItemChange(viewerItemIds[nextIdx]),
                        endDecorator:(<img src={PAGE_RIGHT}/>)}}>
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
