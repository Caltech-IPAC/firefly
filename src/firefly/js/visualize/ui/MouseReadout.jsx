/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   2/23/16
 *     DM-4788
 *   3/2/16
 *     DM-4789
 */
import React, {PropTypes} from 'react';
import {get} from 'lodash';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';
import CoordinateSys from '../CoordSys.js';
import CoordUtil from '../CoordUtil.js';
import VisUtil from '../VisUtil.js';
import numeral from 'numeral';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {STANDARD_READOUT, dispatchChangeLockByClick} from '../../visualize/MouseReadoutCntlr.js';

import {padEnd} from 'lodash';

const rS = {
    width: 670,
    minWidth: 660,
    height: 32,
    minHeight: 32,
    display: 'inline-block',
    position: 'absolute',
    verticalAlign: 'top',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    overflow: 'hidden'
};
const EMPTY = <div style={rS}></div>;
const EMPTY_READOUT = '';
const coordinateMap = {
    galactic: CoordinateSys.GALACTIC,
    eqb1950: CoordinateSys.EQ_B1950,
    pixelSize: CoordinateSys.PIXEL,
    sPixelSize: CoordinateSys.SCREEN_PIXEL
};
export const labelMap = {
    eqj2000hms: 'EQ-J2000:',
    eqj2000DCM: 'EQ-J2000:',
    galactic: 'Gal:',
    eqb1950: 'Eq-B1950:',
    fitsIP: 'Image Pixel:',
    pixelSize: 'Pixel Size:',
    sPixelSize: 'Screen Pixel Size:'
};

const column1 = {
    width: 60,
    paddingRight: 1,
    textAlign: 'right',
    color: 'DarkGray',
    display: 'inline-block'
};
const column2 = {width: 90, display: 'inline-block'};

const column3 = {
    width: 80,
    paddingRight: 2,
    textAlign: 'right',
    textDecoration: 'underline',
    color: 'DarkGray',
    fontStyle: 'italic',
    display: 'inline-block'
};
const column3_r2 = {width: 80, paddingRight: 1, textAlign: 'right', color: 'DarkGray', display: 'inline-block'};

const column4 = {width: 88, paddingLeft:4, display: 'inline-block'};
const column5 = {
    width: 74,
    paddingRight: 1,
    textAlign: 'right',
    textDecoration: 'underline',
    color: 'DarkGray',
    fontStyle: 'italic',
    display: 'inline-block'
};

const column6 = {width: 160,addingLeft: 2, textAlign: 'left', display: 'inline-block'};
const column7 = {width: 109, paddingLeft: 6, display: 'inline-block'};
const column7_r2 = {width: 90, paddingLeft: 3, display: 'inline-block'};


const precision7Digit = '0.0000000';
const precision1Digit = '0.0';

const myFormat= (v,precision) => numeral(v).format(padEnd('0.',precision+1,'0') );
export function MouseReadout({readout}){


   //get the standard readouts
    const sndReadout= readout[STANDARD_READOUT];
    if (!get(sndReadout,'readoutItems')) return EMPTY;


    const title = sndReadout.readoutItems.title?sndReadout.readoutItems.title.value:'';

    var objList={};
    Object.keys( readout.readoutPref).forEach( (key) =>  {
         objList[key]=getMouseReadout(sndReadout.readoutItems,  readout.readoutPref[key] );
    });

    if (!objList)return EMPTY;

    const {mouseReadout1, mouseReadout2, pixelSize} = objList;

    const {fluxLabels, fluxValues} = getFluxInfo(sndReadout);


    return (

        <div style={ rS}>

            {/*row1*/}
            <div  >
                <div style={ column1}>{fluxLabels[1]} </div>
                <div style={ column2}>  {fluxValues[1]}  </div>
                <div style={ column3} onClick={ () => showDialog('pixelSize', readout.readoutPref.pixelSize)}>
                    {labelMap[readout.readoutPref.pixelSize] }
                </div>
                <div style={column4}>{pixelSize} </div>

                <div style={ column5} onClick={ () => showDialog('mouseReadout1', readout.readoutPref.mouseReadout1)}>
                    { labelMap[readout.readoutPref.mouseReadout1] }
                </div>
                <div style={column6}> {mouseReadout1} </div>


                <div style={column7}> {title}  </div>
            </div>
            <div>{/* row2*/}
                <div style={ column1}>{fluxLabels[2]} </div>
                <div style={ column2}> { fluxValues[2]} </div>

                <div style={ column3_r2}>{fluxLabels[0]}</div>
                <div style={ column4}> {fluxValues[0]}</div>

                <div style={ column5} onClick={ () => showDialog('mouseReadout2' ,readout.readoutPref.mouseReadout2 )}>
                    {labelMap[readout.readoutPref.mouseReadout2] } </div>

                <div style={column6}>  {mouseReadout2}  </div>
                <div style={column7_r2} title='Click on an image to lock the display at that point.'>
                    <input type='checkbox' name='aLock' value='lock'
                           onChange={() => {
                           dispatchChangePointSelection('mouseReadout', !readout.lockByClick);
                            dispatchChangeLockByClick(!readout.lockByClick);

                        }}
                    />
                    Lock by click
                </div>


            </div>

        </div>
    );
}

MouseReadout.propTypes = {
    readout: PropTypes.object
};

/**
 * This method passes the standard readout and then get the flux information
 * @param sndReadout
 * @returns {{fluxLabels: Array, fluxValues: Array}}
 */
export function getFluxInfo(sndReadout){

    var fluxObj = [];
    if (sndReadout.threeColor){
        if (sndReadout.readoutItems.hasOwnProperty('REDFlux')){
            fluxObj.push(sndReadout.readoutItems['REDFlux']);
        }
        if (sndReadout.readoutItems.hasOwnProperty('GREENFlux')){
            fluxObj.push(sndReadout.readoutItems['GREENFlux']);
        }
        if (sndReadout.readoutItems.hasOwnProperty('BLUEFlux')){
            fluxObj.push(sndReadout.readoutItems['BLUEFlux']);
        }
    }
    else if (sndReadout.readoutItems.hasOwnProperty('nobandFlux')){
        fluxObj.push(sndReadout.readoutItems.nobandFlux);
    }
    var fluxValueArrays=[];
    var fluxLabelArrays=[];
    var fluxValue,  formatStr;

    for (let i = 0; i < fluxObj.length; i++) {

            fluxValue = (fluxObj[i].value < 1000) ? `${myFormat(fluxObj[i].value, fluxObj[i].precision)}` : fluxObj[i].value.toExponential(6).replace('e+', 'E');
            fluxValueArrays.push(fluxValue);
            fluxLabelArrays.push(fluxObj[i].title);
     }

    if (fluxLabelArrays.length<3) { //fill with empty
        for (let i = fluxLabelArrays.length; i < 3; i++) {
            fluxValueArrays.push(EMPTY_READOUT);
            fluxLabelArrays.push(EMPTY_READOUT);
        }
    }
    return {fluxLabels:fluxLabelArrays, 'fluxValues':fluxValueArrays};

}
/**
 * Get the mouse readouts from the standard readout and convert to the values based on the toCoordinaeName
 * @param readoutItems
 * @param toCoordinateName
 * @returns {*}
 */
export function  getMouseReadout(readoutItems, toCoordinateName) {
    var imagePt=readoutItems.imagePt;
    if (!imagePt || !imagePt.value) return;

    if (toCoordinateName === 'fitsIP') {
        return ` ${numeral(imagePt.value.x).format(precision1Digit)}, ${
            numeral(imagePt.value.y).format(precision1Digit)}`;
    }
    var wpt = readoutItems.worldPt;
    if (!wpt.value) return;

    var result,fStr, obj;
    var {coordinate, type} = getCoordinateMap(toCoordinateName);
    if (coordinate) {
        var ptInCoord = VisUtil.convert(wpt.value, coordinate);
        var lon = ptInCoord.getLon();
        var lat = ptInCoord.getLat();
        var hmsLon = CoordUtil.convertLonToString(lon, coordinate);
        var hmsLat = CoordUtil.convertLatToString(lat, coordinate);


        switch (coordinate) {

            case CoordinateSys.EQ_J2000:
                if (type === 'hms') {
                    result = ` ${hmsLon}, ${hmsLat}`;
                }
                else {
                    //convert to decimal representation
                    var dmsLon = CoordUtil.convertStringToLon(hmsLon, coordinate);
                    var dmsLat = CoordUtil.convertStringToLat(hmsLat, coordinate);
                    result = ` ${numeral(Number(dmsLon)).format(precision7Digit)}, ${numeral(Number(dmsLat)).format(precision7Digit)}`;
                }
                break;
            case CoordinateSys.GALACTIC:
            case CoordinateSys.SUPERGALACTIC:

                var lonShort = numeral(lon).format(precision7Digit);
                var latShort = numeral(lat).format(precision7Digit);
                result = ` ${lonShort}, ${latShort}`;
                break;
            case CoordinateSys.EQ_B1950:
                result = ` ${hmsLon}, ${hmsLat}`;
                break;

            case CoordinateSys.PIXEL:
                obj =readoutItems.pixel;
                 result = `${myFormat(obj.value, obj.precision)}  ${obj.unit || ''}`;
                break;
            case CoordinateSys.SCREEN_PIXEL:
                 obj =readoutItems.screenPixel;
                 result = `${myFormat(obj.value, obj.precision)}  ${obj.unit || ''}`;
                 break;

            default:
                result = '';
                break;
        }

    }


    return result;

}

/**
 *
 * This method map the value in coordinate option popup to its value
 * @param coordinateName : the value in the radio button
 * @returns {{coordinate: *, type: *}}
 */
function getCoordinateMap(coordinateName) {
    var coordinate;
    var type;

    if ( coordinateName === 'eqj2000hms') {
        coordinate = CoordinateSys.EQ_J2000;
        type = 'hms';
    }
    else if (coordinateName === 'eqj2000DCM') {
        coordinate = CoordinateSys.EQ_J2000;
        type = 'decimal';
    }
    else {
        coordinate = coordinateMap[coordinateName];
        //if coordinate is not define, assign it as below
        if (!coordinate) coordinate = CoordinateSys.UNDEFINED;
    }
    return {coordinate, type};
}


function showDialog(fieldKey, radioValue) {

    showMouseReadoutOptionDialog(fieldKey, radioValue);

}

