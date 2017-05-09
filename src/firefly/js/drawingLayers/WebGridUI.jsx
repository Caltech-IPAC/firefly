/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 * 4/15/16
 */
import React from 'react';
import PropTypes from 'prop-types';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {dispatchAddPreference,getPreference} from '../core/AppDataCntlr.js';
import {get} from 'lodash';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {COORDINATE_PREFERENCE} from './WebGrid.js';

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

/**
 * This method create the UI component displayed at the layer property popup dialog in the toolbar.
 * @param drawLayer - DrawLayer object
 * @param pv - plotView object
 * @returns {XML} - UI component
 * @constructor
 */
function WebGridUI({drawLayer,pv}) {

   var pref= getPreference(COORDINATE_PREFERENCE);
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

/**
 * This method is dispatching the changes made in the customer field to the DrawLayerCtr and the reducer.
 * @param plotId
 * @param drawLayer
 * @param ev
 */
function onCoordinateChange(plotId, drawLayer, ev) {
    var csysName = get(ev, 'target.value');
    dispatchAddPreference(COORDINATE_PREFERENCE,csysName);
    //add or update the coordinate reference to the drawLayer
    var customChanges ={[COORDINATE_PREFERENCE]:csysName};
    dispatchModifyCustomField( drawLayer.displayGroupId,customChanges, plotId);


}


