/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView.jsx';
import {dispatchModifyCustomField, dispatchChangeVisibility} from '../visualize/DrawLayerCntlr.js';
import {isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {GroupingScope} from '../visualize/DrawLayerCntlr.js';


export const getUIComponent = (drawLayer,pv) => <CatalogUI drawLayer={drawLayer} pv={pv}/>;

function CatalogUI({drawLayer,pv}) {

    const tStyle= {
        display:'inline-block',
        whiteSpace: 'nowrap',
        minWidth: '3em',
        paddingLeft : 5
    };
    const options= [ {label: 'All', value: 'GROUP'},
                   {label: 'Row', value: 'SUBGROUP'},
                   {label: 'Image', value: 'SINGLE'}
    ];

    if (!drawLayer.supportSubgroups) return null;

    const value= drawLayer.groupingScope ? drawLayer.groupingScope.toString() : 'GROUP';
    return (
        <div>
            <div>
                Overlay:
                <div style={{display:'inline-block', paddingLeft:7}}>
                    <RadioGroupInputFieldView options={options}  value={value}
                                              onChange={(ev) => changeVisibilityScope(drawLayer,pv,ev.target.value)} />
                </div>
            </div>
        </div>
    );
}


function changeVisibilityScope(drawLayer,pv,value) {
    const groupingScope= GroupingScope.get(value);
    const {drawLayerId}= drawLayer;
    const {plotId, drawingSubGroupId}= pv;
    if (!drawingSubGroupId) return;
    dispatchModifyCustomField( drawLayerId, {groupingScope}, plotId );
    const visible= isDrawLayerVisible(drawLayer,plotId);
    switch (groupingScope) {
        case GroupingScope.GROUP : //make sure all image match the visibility of the plotId
            dispatchChangeVisibility(drawLayerId, visible,plotId);
            break;
        case GroupingScope.SUBGROUP : // change all, then put only subgroup back
            if (visible) dispatchChangeVisibility(drawLayerId, false,plotId);
            dispatchChangeVisibility(drawLayerId, visible,plotId,true, drawingSubGroupId);
            break;
        case GroupingScope.SINGLE : // change all, then put only image back
            if (visible) dispatchChangeVisibility(drawLayerId, false, plotId);
            dispatchChangeVisibility(drawLayerId, visible,plotId, false);
            break;
    }
}

CatalogUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

