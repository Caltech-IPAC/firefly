/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {STANDARD_READOUT, dispatchChangeLockByClick} from '../../visualize/MouseReadoutCntlr.js';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';

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
const EMPTY = <div style={rS}/>;

const column1 = {
    width: 60,
    paddingRight: 1,
    textAlign: 'right',
    color: 'DarkGray',
    display: 'inline-block'
};
const column2 = {
    width: 90,
    display: 'inline-block',
    overflow:'hidden',
    textOverflow: 'ellipsis'
};

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

const column4 = {width: 105, paddingLeft:4, display: 'inline-block', textOverflow: 'ellipsis', overflow:'hidden'};
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

export function MouseReadout({readout, showHealpixPixel=true }){


   //get the standard readouts
   //  const sndReadout= readout[STANDARD_READOUT];
    if (!get(readout,[STANDARD_READOUT, 'readoutItems'])) return EMPTY;

    const sndReadout= get(readout, STANDARD_READOUT);
    
    const title = sndReadout.readoutItems.title?sndReadout.readoutItems.title.value:'';
    const {isHiPS, readoutItems}= sndReadout;


    const displayEle= getNonFluxDisplayElements(readoutItems,  readout.readoutPref, isHiPS);
    const {readout1, readout2, pixelSize, showReadout1PrefChange,
         showReadout2PrefChange, showPixelPrefChange, healpixPixelReadout, healpixNorderReadout}= displayEle;

    const fluxArray = getFluxInfo(sndReadout);
    const hipsPixel= showHealpixPixel && isHiPS;

    return (

        <div style={ rS}>

            {/*row1*/}
            <div  >
                {!isHiPS && <div style={ column1}>{fluxArray[1].label} </div>}
                {!isHiPS && <div style={ column2}>  {fluxArray[1].value}  </div>}
                {hipsPixel && <div style={ column1}>{healpixNorderReadout.label} </div>}
                {hipsPixel && <div style={ column2}>  {healpixNorderReadout.value}  </div>}
                
                <div style={ column3} onClick={ showPixelPrefChange}>
                    {pixelSize.label}
                </div>
                <div style={column4}>{pixelSize.value} </div>

                <div style={ column5} onClick={ showReadout1PrefChange}>
                    { readout1.label }
                </div>
                <div style={column6}> {readout1.value} </div>


                <div style={column7}> {title}  </div>
            </div>
            <div>{/* row2*/}
                <div style={ column1}>{fluxArray[2].label} </div>
                <div style={ column2}> {fluxArray[2].value} </div>

                {!isHiPS && <div style={ column3_r2}>{fluxArray[0].label}</div>}
                {!isHiPS && <div style={ column4}> {fluxArray[0].value}</div>}
                {hipsPixel && <div style={ column3_r2}>{healpixPixelReadout.label} </div>}
                {hipsPixel && <div style={ column4}>{healpixPixelReadout.value}  </div>}

                <div style={ column5} onClick={ showReadout2PrefChange}>
                    {readout2.label } </div>

                <div style={column6}>  {readout2.value}  </div>
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
    readout: PropTypes.object,
    showHealpixPixel : PropTypes.bool
};



function showDialog(fieldKey, radioValue) {

    showMouseReadoutOptionDialog(fieldKey, radioValue);

}

