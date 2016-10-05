/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get} from 'lodash';
import React, {PropTypes} from 'react';
import BrowserInfo from '../../util/BrowserInfo.js';
import {getPlotViewById, getAllDrawLayersForPlot} from '../../visualize/PlotViewUtil.js';
import {WcsMatchType, visRoot, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {VisInlineToolbarView} from '../../visualize/ui/VisInlineToolbarView.jsx';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {dispatchChangeLayout, getViewer, getMultiViewRoot, GRID, SINGLE} from '../../visualize/MultiViewCntlr.js';
import {DEF_IMAGE_CNT, MAX_IMAGE_CNT} from './LcManager.js';



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

var options= [];

for(var i= 1; (i<=MAX_IMAGE_CNT); i+=2) {
    options.push({label: i+'', value: i});
}


export function LcImageToolbarView({activePlotId, viewerId, viewerPlotIds, layoutType, dlAry, tableId}) {

    const viewer= getViewer(getMultiViewRoot(), viewerId);
    const count= get(viewer, 'layoutDetail.count',DEF_IMAGE_CNT);
    const vr= visRoot();
    const pv= getPlotViewById(vr, activePlotId);
    const pvDlAry= getAllDrawLayersForPlot(dlAry,activePlotId,true);

    const wcsMatch= (
        <div style={{alignSelf:'center', paddingLeft:25}}>
            <div style={{display:'inline-block'}}>
                <input style={{margin: 0}}
                       type='checkbox'
                       checked={vr.wcsMatchType===WcsMatchType.Standard}
                       onChange={(ev) => wcsMatchStandard(ev.target.checked, vr.activePlotId) }
                />
            </div>
            <div style={tStyle}>WCS Match</div>
        </div>
    );

    return (
        <div style={toolsStyle}>
            <div style={{whiteSpace: 'nowrap', paddingLeft: 7}}>
                Image Count:
                <div style={{display:'inline-block', paddingLeft:7}}>
                    <RadioGroupInputFieldView options={options} inline={true} fieldKey='frames' value={count}
                                              onChange={(ev) => changeSize(viewerId, ev.target.value)} />
                </div>
            </div>
            {wcsMatch}
            <InlineRightToolbarWrapper visRoot={vr} pv={pv} dlAry={pvDlAry} />
        </div>
    );
}


function changeSize(viewerId, value) {
    value= Number(value);
    dispatchChangeLayout(viewerId, value === 1 ? SINGLE : GRID, {count:value});
}

// <ImagePager pageSize={10} tbl_id={tableId} />
// <ToolbarButton icon={ONE} tip={'Show single image at full size'}
//                imageStyle={{width:24,height:24, flex: '0 0 auto'}}
//                enabled={true} visible={true}
//                horizontal={true}
//                onClick={() => dispatchChangeLayout(viewerId,SINGLE)}/>
// <ToolbarButton icon={FULL_GRID} tip={'show before and after images'}
// enabled={true} visible={true} horizontal={true}
// imageStyle={{width:24,height:24,  paddingLeft:5, flex: '0 0 auto'}}
// onClick={() => dispatchChangeLayout(viewerId,'grid',GRID_FULL)}/>

LcImageToolbarView.propTypes= {
    dlAry : PropTypes.arrayOf(React.PropTypes.object),
    activePlotId : PropTypes.string,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    tableId: PropTypes.string
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
            />
        </div>
    );
}

InlineRightToolbarWrapper.propTypes= {
    visRoot: PropTypes.object,
    pv : PropTypes.object,
    dlAry : PropTypes.array
};

function wcsMatchStandard(doWcsStandard, plotId) {
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Standard:false, plotId});
}
