/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import Point, {parseWorldPt, isValidPoint} from '../Point.js';
import CoordinateSys from '../CoordSys.js';
import CysConverter from '../CsysConverter.js';
import VisUtil from '../VisUtil.js';
import CoordUtil from '../CoordUtil.js';
import numeral from 'numeral';
import {Band} from '../Band.js';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {NUM_VAL,DESC_VAL,POINT_VAL, STANDARD_READOUT, dispatchChangeLockByClick} from '../MouseReadoutCntlr.js';


// ------- NOTE
// ------- NOTE
//
// This is just an testing and example file of how we are going to handle mouse readout in the future
//
// ------- NOTE
// ------- NOTE

const defStyle=  {
    minWidth: 250,
    minHeight: 100,
    padding : 5
};


const precision7Digit = '0.0000000';
const precision3Digit = '0.000';
const precision1Digit = '0.0';

const format3= (n) => numeral(Number(n)).format(precision3Digit);
const format7= (n) => numeral(Number(n)).format(precision7Digit);
const format1= (n) => numeral(Number(n)).format(precision1Digit);


export function VerySimpleMouseReadout({readout}) {


    const sndReadout= readout[STANDARD_READOUT];

    if (!get(sndReadout,'readoutItems')) return <div style={defStyle}></div>;

    const result= Object.keys(sndReadout.readoutItems).map( (k) => makeReadoutEntry(sndReadout.readoutItems[k]));
    
    return (
        <div style={defStyle}>

            <div>
                <input type='checkbox'
                       checked={readout.lockByClick}
                       onChange={(ev) => {
                                dispatchChangePointSelection('mouseReadout', !readout.lockByClick);
                                dispatchChangeLockByClick(!readout.lockByClick);
                       }}
                />
                <div style={{display:'inline-block', whiteSpace: 'nowrap'}}> Lock By Click</div>
            </div>
            <div style={{paddingTop: 5}}>
                {result}
            </div>

        </div>
    );
}


VerySimpleMouseReadout.propTypes = {
    readout: PropTypes.object
};


function makeReadoutEntry(obj) {
    var {title='', value='', precision=3}= obj;
    if (obj.value) {
        if (obj.type===POINT_VAL) {
            const pt= value;
            if (pt.type===Point.SPT || pt.type===Point.IM_PT) {
                value= `${format1(pt.x)}, ${format1(pt.y)}`;
            }
            else if (pt.type===Point.W_PT) {
                value= formatWorldPt(pt,CoordinateSys.EQ_J2000, 'hms');
            }
            else {
                value= pt.toString();
            }
        }
        else if (obj.type===NUM_VAL) {
            // value= numeral(value).format(precision);
            const fStr= '0.'+ new Array(precision + 1).join('0');
            value= `${numeral(value).format(fStr)} ${obj.unit ||''}`;
        }
    }
    return  (
        <div key={obj.title+obj.value}>
            {title && <div style={{display:'inline-block',width:'8em'}}>{title}: </div>}
            <div style={{display:'inline-block'}}>{value}</div>
        </div>
    );
    
}



export function formatWorldPt(wpt, coordinate, type) {


    if (!coordinate)  return '';
    var ptInCoord = VisUtil.convert(wpt, coordinate,type);
    var result= '';

    var lon = ptInCoord.getLon();
    var lat = ptInCoord.getLat();
    var hmsLon = CoordUtil.convertLonToString(lon, coordinate);
    var hmsLat = CoordUtil.convertLatToString(lat, coordinate);

    switch (coordinate) {
        case CoordinateSys.EQ_J2000:
            result= type === 'hms' ? ` ${hmsLon}, ${hmsLat}` : ` ${format7(lon)}, ${format7(lat)}`;
            break;
        case CoordinateSys.GALACTIC:
        case CoordinateSys.SUPERGALACTIC:
            result = ` ${format7(lon)}, ${format7(lat)}`;
            break;
        case CoordinateSys.EQ_B1950:
            result = ` ${hmsLon}, ${hmsLat}`;
            break;

        default:
            result = '';
            break;
    }
    return result;
}
