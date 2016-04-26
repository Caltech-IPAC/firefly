/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 * 4/15/16
 */
import React, {PropTypes} from 'react';
import {dispatchForceDrawLayerUpdate, dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {get} from 'lodash';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {COORDIANTE_PREFERENCE} from './ComputeWebGridData.js';


export const getUIComponent = (drawLayer,pv) => < WebGridUI drawLayer={drawLayer} pv={pv}/>;

const coordinateOptionArray = [
    {label:'Eq. J2000',         value:'eq2000hms'},
    {label: 'Eq. J2000 Decimal',value:'eq2000dcm'},
    {label: 'Eq. B1950',        value:'eqb1950hms'},
    {label:'Eq. B1950 Decimal', value:'eqb1950dcm'},
    {label:'Galactic',          value:'galactic'},
    {label: 'Super Galactic',   value:'superGalactic'},
    {label:'Ecliptic J2000',    value:'epj2000'},
    {label:'Ecliptic B1950',    value:'epb1950'}

];


function WebGridUI({drawLayer,pv}) {

    var pref= AppDataCntlr.getPreference(COORDIANTE_PREFERENCE);
   return  (
        <div>
           <ListBoxInputFieldView
               onChange={(request) => onCoordinateChange( pv.plotId,drawLayer, request) }
               options={ coordinateOptionArray}
               multiple={false}
               value= {pref}
               labelWidth={2}
               label={''}
               tooltip={'select a coordinate'}
           />
        </div>
    );

}



WebGridUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};


function onCoordinateChange(plotId, drawLayer, ev) {
    var csysName = get(ev, 'target.value');
    AppDataCntlr.dispatchAddPreference(COORDIANTE_PREFERENCE,csysName);
    dispatchModifyCustomField(drawLayer.displayGroupId,{COORDIANTE_PREFERENCE:csysName}, plotId);
    //dispatchForceDrawLayerUpdate(drawLayer.displayGroupId, plotId);

}


