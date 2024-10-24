/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 * 4/15/16
 */
import React from 'react';
import {object} from 'prop-types';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {dispatchAddPreference,getPreference} from '../core/AppDataCntlr.js';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {COORDINATE_PREFERENCE} from './WebGrid.js';

export const getUIComponent = (drawLayer,pv) => < WebGridUI drawLayer={drawLayer} pv={pv}/>;

const coordinateOptionArray = [
    {label:'Equatorial J2000 HMS',         value:'eq2000hms'},
    {label: 'Equatorial J2000 Decimal',value:'eq2000dcm'},
    {label: 'Equatorial B1950 HMS',        value:'eqb1950hms'},
    {label:'Equatorial B1950 Decimal', value:'eqb1950dcm'},
    {label:'Galactic',          value:'galactic'},
    {label: 'Super Galactic',   value:'superGalactic'},
    {label:'Ecliptic J2000',    value:'epj2000'},
    {label:'Ecliptic B1950',    value:'epb1950'}

];

/**
 * This method create the UI component displayed at the layer property popup dialog in the toolbar.
 * @param props
 * @param props.drawLayer - DrawLayer object
 * @param props.pv - plotView object
 */
function WebGridUI({drawLayer,pv}) {

   const pref= getPreference(COORDINATE_PREFERENCE);
   return  (
           <ListBoxInputFieldView
               onChange={(ev,newValue) => onCoordinateChange( pv.plotId,drawLayer,newValue) }
               options={coordinateOptionArray}
               value= {pref || coordinateOptionArray[0].value }
               tooltip={'select a coordinate'}
           />
    );
}

WebGridUI.propTypes= {
    drawLayer: object.isRequired,
    pv       : object.isRequired
};

/**
 * This method is dispatching the changes made in the customer field to the DrawLayerCtr and the reducer.
 * @param plotId
 * @param drawLayer
 * @param csysName
 */
function onCoordinateChange(plotId, drawLayer, csysName ) {
    dispatchAddPreference(COORDINATE_PREFERENCE,csysName);
    //add or update the coordinate reference to the drawLayer
    const customChanges ={[COORDINATE_PREFERENCE]:csysName};
    dispatchModifyCustomField( drawLayer.displayGroupId,customChanges, plotId);
}


