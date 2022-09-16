/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {getPlotViewById, getAllDrawLayersForPlot} from '../../visualize/PlotViewUtil.js';
import {WcsMatchType, visRoot, dispatchWcsMatch} from '../../visualize/ImagePlotCntlr.js';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {
    dispatchChangeViewerLayout, getViewer, getMultiViewRoot, GRID, SINGLE, getLayoutType, getLayoutDetails
} from '../../visualize/MultiViewCntlr.js';
import {LC, getConverterData} from './LcManager.js';
import {CloseButton} from '../../ui/CloseButton.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {SortInfo, SORT_ASC, SORT_DESC, UNSORTED} from '../../tables/SortInfo.js';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';


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
    paddingLeft : 5,
    marginTop: -1
};

const closeButtonStyle= {
    display: 'inline-block',
    padding: '1px 12px 0 1px'
};

export function LcImageToolbarView({viewerId, tableId, closeFunc=null}) {
    const converter = getConverterData();
    if (!converter) { return null; }

    const count= getLayoutDetails(getMultiViewRoot(), viewerId)?.count ?? converter.defaultImageCount;
    const vr= visRoot();

    const wcsMatch= (
        <div style={{alignSelf:'center', padding: '0 10px 0 25px', display:'flex', alignItems:'center'}}>
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

    var getSortInfo = () => {
        if (!tableId) return '';
        const sInfo = SortInfo.parse(get(getTblById(tableId), ['request', 'sortInfo'], ''));

        const orderInfo = {[SORT_ASC]: 'ascending',
                           [SORT_DESC]:'descending'};

        if (sInfo.direction === UNSORTED) return '';
        return `Sorted by column: ${sInfo.sortColumns.join(',')} `+
               ` ${orderInfo[sInfo.direction]}`;
    };

    const options= [];
    for(var i= 1; (i<=LC.MAX_IMAGE_CNT); i+=2) {
        options.push({label: String(i), value: String(i)});
    }

    return (
        <div>
            <div style={{...toolsStyle, marginBottom: closeFunc ? 3 : 0}}>
                {closeFunc &&<CloseButton style={closeButtonStyle} onClick={closeFunc}/>}
                <div style={{whiteSpace: 'nowrap', paddingLeft: 7, display:'flex', alignItems:'center'}}>
                    <div>Image Count:</div>
                    <div style={{display:'inline-block', paddingLeft:7}}>
                        <RadioGroupInputFieldView options={options} inline={true}  value={String(count)}
                                                  onChange={(ev) => changeSize(viewerId, ev.target.value)} />
                    </div>
                </div>
                <div> { getSortInfo() } </div>
                {wcsMatch}
                <VisMiniToolbar style={{width:350}}/>
            </div>
        </div>
    );
}


function changeSize(viewerId, value) {
    value = Number(value);
    dispatchChangeViewerLayout(viewerId, value === 1 ? SINGLE : GRID, {count: value});
}

LcImageToolbarView.propTypes= {
    viewerId : PropTypes.string.isRequired,
    tableId: PropTypes.string,
    closeFunc : PropTypes.func
};


function wcsMatchTarget(doWcsStandard, plotId) {
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Target:false, plotId});
}
