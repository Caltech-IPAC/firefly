/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import {getChartData} from '../ChartsCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {dispatchChangeViewerLayout, dispatchUpdateCustom, getViewerItemIds, getViewer, getLayoutType, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import ONE from 'html/images/icons-2014/Images-One.png';
import GRID from 'html/images/icons-2014/Images-Tiled.png';
import PAGE_RIGHT from 'html/images/icons-2014/20x20_PageRight.png';
import PAGE_LEFT from 'html/images/icons-2014/20x20_PageLeft.png';

import {PagingControl} from '../../visualize/iv/ExpandedTools.jsx';


const toolsStyle= {
    position:'absolute',
    top: 'calc(0% + 1px)',
    left: 'calc(50% - 50px)',
    zIndex:1,
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    // alignItems: 'center',
    height: 25,
    justifyContent: 'space-between'
};

export function MultiChartToolbarStandard({viewerId,
    layoutType=getLayoutType(getMultiViewRoot(), viewerId),
    activeItemId=getViewer(getMultiViewRoot(), viewerId).customData.activeItemId}) {

    const viewerItemIds = getViewerItemIds(getMultiViewRoot(), viewerId);
    var cIdx= viewerItemIds.findIndex( (itemId) => itemId===activeItemId);
    if (cIdx<0) cIdx= 0;

    if (viewerItemIds.length===1) {
        return <div/>;
    }

    const leftImageStyle= {
        verticalAlign:'bottom',
        cursor:'pointer',
        flex: '0 0 auto',
        paddingLeft: 10
    };


    const nextIdx= cIdx===viewerItemIds.length-1 ? 0 : cIdx+1;
    const prevIdx= cIdx ? cIdx-1 : viewerItemIds.length-1;


    return (
        <div style={toolsStyle}>
            <div style={{display:'flex', flexDirection:'row', flexWrap:'nowrap'}}>
                <ToolbarButton icon={ONE} tip={'Show single chart'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               enabled={true} visible={true}
                               horizontal={true}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'single')}/>
                <ToolbarButton icon={GRID} tip={'Show all charts as tiles'}
                               enabled={true} visible={true} horizontal={true}
                               imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,'grid')}/>
                {layoutType==='single' && viewerItemIds.length>2 &&
                <img style={leftImageStyle} src={PAGE_LEFT}
                     onClick={() => dispatchUpdateCustom(viewerId, {activeItemId: viewerItemIds[prevIdx]})} />
                }
                {layoutType==='single' && viewerItemIds.length>1 &&
                <img style={{verticalAlign:'bottom', cursor:'pointer', float: 'right', paddingLeft:5, flex: '0 0 auto'}}
                     src={PAGE_RIGHT}
                     onClick={() => dispatchUpdateCustom(viewerId, {activeItemId: viewerItemIds[nextIdx]})} />
                }
            </div>
        </div>
    );
}

MultiChartToolbarStandard.propTypes= {
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    activeItemId : PropTypes.string
};


const toolsStyleExpanded= {
    position:'absolute',
    top: 0,
    left: 'calc(0% + 200px)',
    right:'calc(0% + 300px)',
    zIndex:1,
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    // alignItems: 'center',
    height: 28,
    justifyContent: 'space-between'
};

const closeButtonStyle= {
    display: 'inline-block'
};

const viewerTitleStyle= {
    display: 'inline-block',
    paddingLeft: 5,
    lineHeight: '2em',
    fontSize: '10pt',
    fontWeight: 'bold',
    alignSelf : 'center'
};

export function MultiChartToolbarExpanded({closeable, viewerId,
    layoutType=getLayoutType(getMultiViewRoot(), viewerId),
    activeItemId=getViewer(getMultiViewRoot(), viewerId).customData.activeItemId}) {

    const viewerItemIds = getViewerItemIds(getMultiViewRoot(), viewerId);

    const CloseBtn = (
        <div style={closeButtonStyle}>
            {closeable && <CloseButton style={{display: 'inline-block', paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
        </div>
    );

    if (viewerItemIds.length===1) {
        return (
            <div style={toolsStyleExpanded}>
                {CloseBtn}
            </div>);
    }


    const getChartTitle = (chartId) => {
        const {chartType} = getChartData(chartId);
        const idx = viewerItemIds.findIndex((el) => {return el === chartId;} );
        if (idx>=0) {
            return `${idx+1}-${chartType}`;
        } else {
            return chartType;
        }
    };

    var viewerTitle = (<div style={viewerTitleStyle}>{layoutType==='single' ? getChartTitle(activeItemId) : 'Tiled View'}</div>);

    return (
        <div style={toolsStyleExpanded}>
            <div style={{width:'100%', display: 'flex', justifyContent:'space-between'}} className='disable-select'>
                <div style={{alignSelf:'flex-start', whiteSpace:'nowrap'}}>
                    {CloseBtn} {viewerTitle}
                </div>
                <div style={{alignSelf:'flex-end', whiteSpace:'nowrap', height: 24, paddingBottom:3}}>
                    <div style={{display: 'inline-block', verticalAlign:'top'}}>
                        <ToolbarButton icon={ONE} tip={'Show single chart'}
                                       imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                                       enabled={true} visible={true}
                                       horizontal={true}
                                       onClick={() => dispatchChangeViewerLayout(viewerId,'single')}/>
                        <ToolbarButton icon={GRID} tip={'Show all charts as tiles'}
                                       enabled={true} visible={true} horizontal={true}
                                       imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
                                       onClick={() => dispatchChangeViewerLayout(viewerId,'grid')}/>
                    </div>
                    <div style={{display: 'inline-block', verticalAlign:'middle'}}>
                        <PagingControl
                            viewerItemIds={viewerItemIds}
                            activeItemId={activeItemId}
                            isPagingMode={layoutType==='single'}
                            getItemTitle={getChartTitle}
                            onActiveItemChange={(itemId) => dispatchUpdateCustom(viewerId, {activeItemId: itemId})}
                        />
                    </div>
                </div>
            </div>
        </div>
    );

}

MultiChartToolbarExpanded.propTypes= {
    closeable : PropTypes.bool,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string,
    activeItemId : PropTypes.string
};



