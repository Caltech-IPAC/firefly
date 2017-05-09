/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import BrowserInfo from '../../util/BrowserInfo.js';
import {getPlotViewById, getAllDrawLayersForPlot} from '../../visualize/PlotViewUtil.js';
import {WcsMatchType, visRoot, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {VisInlineToolbarView} from '../../visualize/ui/VisInlineToolbarView.jsx';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {dispatchChangeViewerLayout, getViewer, getMultiViewRoot, GRID, SINGLE} from '../../visualize/MultiViewCntlr.js';
import {LC, getConverterData} from './LcManager.js';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {VisToolbar} from '../../visualize/ui/VisToolbar.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {SortInfo, SORT_ASC, SORT_DESC, UNSORTED} from '../../tables/SortInfo.js';


const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    justifyContent:'space-between',
    height: 30
};

const tStyle= {
    display:'inline-block',
    whiteSpace: 'nowrap',
    minWidth: '3em',
    paddingLeft : 5
};

const closeButtonStyle= {
    display: 'inline-block',
    padding: '1px 12px 0 1px'
};

var options= [];

for(var i= 1; (i<=LC.MAX_IMAGE_CNT); i+=2) {
    options.push({label: String(i), value: String(i)});
}


export function LcImageToolbarView({activePlotId, viewerId, viewerPlotIds, layoutType, dlAry, tableId, closeFunc=null}) {
    const converter = getConverterData();
    if (!converter) { return null; }

    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const count= get(viewer, 'layoutDetail.count', converter.defaultImageCount);
    const vr= visRoot();
    const pv= getPlotViewById(vr, activePlotId);
    const pvDlAry= getAllDrawLayersForPlot(dlAry,activePlotId,true);

    const wcsMatch= (
        <div style={{alignSelf:'center', padding: '0 10px 0 25px'}}>
            <div style={{display:'inline-block'}}>
                <input style={{margin: 0}}
                       type='checkbox'
                       checked={vr.wcsMatchType===WcsMatchType.Target}
                       onChange={(ev) => wcsMatchTarget(ev.target.checked, vr.activePlotId) }
                />
            </div>
            <div style={tStyle}>Target Match</div>
        </div>
    );

    var expandedUI= null;
    if (closeFunc) {
        expandedUI= (
            <div style={{display:'flex', flexDirection:'row', flexWrap:'nowrap'}}>
                <CloseButton style={closeButtonStyle} onClick={closeFunc}/>
                <div style={{'flex': '1 1 auto'}}>
                    <VisToolbar messageUnder={Boolean(closeFunc)}/>
                </div>
            </div>

        );
    }

    var getSortInfo = () => {
        if (!tableId) return '';
        const sInfo = SortInfo.parse(get(getTblById(tableId), ['request', 'sortInfo'], ''));

        const orderInfo = {[SORT_ASC]: 'ascending',
                           [SORT_DESC]:'descending'};

        if (sInfo.direction === UNSORTED) return '';
        return `Sorted by column: ${sInfo.sortColumns.join(',')} `+
               ` ${orderInfo[sInfo.direction]}`;
    };

    return (
        <div>
            {expandedUI}
            <div style={toolsStyle}>
                <div style={{whiteSpace: 'nowrap', paddingLeft: 7}}>
                    Image Count:
                    <div style={{display:'inline-block', paddingLeft:7}}>
                        <RadioGroupInputFieldView options={options} inline={true}  value={String(count)}
                                                  onChange={(ev) => changeSize(viewerId, ev.target.value)} />
                    </div>
                </div>
                <div> { getSortInfo() } </div>
                {wcsMatch}
                {!closeFunc && <InlineRightToolbarWrapper visRoot={vr} pv={pv} dlAry={pvDlAry} />}
            </div>
        </div>
    );
}


function changeSize(viewerId, value) {
    value= Number(value);
    dispatchChangeViewerLayout(viewerId, value === 1 ? SINGLE : GRID, {count:value});
}

// <ImagePager pageSize={10} tbl_id={tableId} />
// <ToolbarButton icon={ONE} tip={'Show single image at full size'}
//                imageStyle={{width:24,height:24, flex: '0 0 auto'}}
//                enabled={true} visible={true}
//                horizontal={true}
//                onClick={() => dispatchChangeViewerLayout(viewerId,SINGLE)}/>
// <ToolbarButton icon={FULL_GRID} tip={'show before and after images'}
// enabled={true} visible={true} horizontal={true}
// imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
// onClick={() => dispatchChangeViewerLayout(viewerId,'grid',GRID_FULL)}/>

LcImageToolbarView.propTypes= {
    dlAry : PropTypes.arrayOf(PropTypes.object),
    activePlotId : PropTypes.string,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    tableId: PropTypes.string,
    closeFunc : PropTypes.func
};


function InlineRightToolbarWrapper({visRoot,pv,dlAry}){
    if (!pv) return <div></div>;

    var lVis= BrowserInfo.isTouchInput() || visRoot.apiToolsView;
    var tb= visRoot.apiToolsView;
    return (
        <div>
            <VisInlineToolbarView
                pv={pv} dlAry={dlAry}
                showLayer={lVis}
                showExpand={true}
                showToolbarButton={tb}
                showDelete ={false}
                help_id={'main1TSV.images'}
            />
        </div>
    );
}

InlineRightToolbarWrapper.propTypes= {
    visRoot: PropTypes.object,
    pv : PropTypes.object,
    dlAry : PropTypes.array
};

function wcsMatchTarget(doWcsStandard, plotId) {
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Target:false, plotId});
}
