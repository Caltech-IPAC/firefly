/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {FixedPtControl} from './CatalogUI.jsx';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat';
import {primePlot} from '../visualize/PlotViewUtil';
import {PlotAttribute} from '../visualize/WebPlot';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr';
import {RadioGroupInputFieldView} from '../ui/RadioGroupInputFieldView';


export const getUIComponent = (drawLayer,pv) => <ImageRootUI drawLayer={drawLayer} pv={pv}/>;

const options= [
    {label: 'Show', value: 'show'},
    {label: 'Hide', value: 'hide'}
    ];


function changeSearchTargetVisibility(drawLayer,pv,value) {
    // dispatchModifyCustomField(drawLayer.drawLayerId, {searchTargetVisible:value==='show'}, pv.plotId);
    dispatchModifyCustomField(drawLayer.drawLayerId, {searchTargetVisible:value}, pv.plotId);
}



function ImageRootUI({drawLayer:dl, pv}) {

    const wp= get(primePlot(pv),['attributes',PlotAttribute.FIXED_TARGET]);
    const displayFixedTarget = get(pv,'plotViewCtx.displayFixedTarget', false);
    if (!wp || !displayFixedTarget) return null;

    return (
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems:'center'}}>
            <div style={{display: 'flex', alignItems: 'center'}}>
                <span style={{paddingRight: 5}} >Search: </span>
                <div> {formatWorldPt(wp,3,false)} </div>
                <FixedPtControl pv={pv} wp={wp} />
                <input type='checkbox' checked={dl.searchTargetVisible}
                       onChange={() => changeSearchTargetVisibility(dl,pv,!dl.searchTargetVisible)} />
            </div>
        </div>
    );


}

ImageRootUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};
