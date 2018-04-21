/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   6/03/16
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {clone} from '../../util/WebUtil.js';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {STANDARD_READOUT, dispatchChangeLockByClick,
    dispatchChangeLockUnlockByClick} from '../../visualize/MouseReadoutCntlr.js';
import {getNonFluxDisplayElements, getFluxInfo} from './MouseReadoutUIUtil.js';

//--------------- Icons --------------------------------
import LOCKED from    'html/images/icons-2014/lock_20x20.png';
import UNLOCKED from  'html/images/icons-2014/unlock_20x20.png';


const linkLook = {
    textDecoration: 'underline',
    fontStyle: 'italic',
};

const column3 = {
    width: 80,
    paddingRight: 2,
    textAlign: 'right',
    color: 'DarkGray',
    display: 'inline-block'
};
const column3_r2 = {width: 80, paddingRight: 1, textAlign: 'right', color: 'DarkGray', display: 'inline-block'};

const column4 = {width: 88, paddingLeft:4, display: 'inline-block'};
const column5 = {
    width: 74,
    paddingRight: 1,
    textAlign: 'right',
    color: 'DarkGray',
    display: 'inline-block'
};

const column6 = {width: 170,addingLeft: 2, textAlign: 'left', display: 'inline-block'};
const lockByClickStyle = {width: 100, display: 'inline-block', float: 'right', margin: '-10px 24px 0 0 '};


export function PopupMouseReadoutFull({readout, showHealpixPixel=false}){


    //get the standard readouts
    const sndReadout= readout[STANDARD_READOUT];
    const {threeColor}= readout.standardReadout;

    const rS = {
        cursor: 'pointer',
        width: 485,
        display: 'inline-block',
        position: 'relative',
    };


    if (!get(sndReadout,'readoutItems')) return <div style={rS}/>;
    const lock = readout.isLocked ? LOCKED:UNLOCKED;
    const {isHiPS, readoutItems}= sndReadout;
    const displayEle= getNonFluxDisplayElements(readoutItems,  readout.readoutPref, isHiPS);
    const {readout1, readout2, pixelSize, showReadout1PrefChange, showReadout2PrefChange,
           showPixelPrefChange, healpixPixelReadout, healpixNorderReadout}= displayEle;
    const fluxArray = getFluxInfo(sndReadout);
    const hipsPixel= showHealpixPixel && isHiPS;

    return (
        <div style={{display:'flex', height: '100%', alignItems:'center'}}>
            <div>
                <img  src= {lock}  title= 'Lock the readout panel visible' onClick ={() =>{
                      dispatchChangeLockUnlockByClick(!readout.isLocked);
                   }}
                />
            </div>
            <div style={ rS}>

                {/*row1*/}
                <div>
                    <div style={ clone(column3,linkLook)} onClick={ showPixelPrefChange}> {pixelSize.label} </div>
                    <div style={column4}>{pixelSize.value} </div>

                    <div style={ clone(column5,linkLook)} onClick={ showReadout1PrefChange}> {readout1.label} </div>
                    <div style={column6}> {readout1.value} </div>
                </div>
                <div style={{paddingTop: 3}}>{/* row2*/}
                    {!isHiPS && <div style={ column3_r2}>{fluxArray[0].label}</div>}
                    {!isHiPS && <div style={ column4}> {fluxArray[0].value}</div>}
                    {hipsPixel && <div style={ column3_r2}>{healpixPixelReadout.label}</div>}
                    {hipsPixel && <div style={ column4}>{healpixPixelReadout.value}</div>}

                    <div style={ clone(column5,linkLook)} onClick={ showReadout2PrefChange}>
                        {readout2.label } </div>

                    <div style={column6}>  {readout2.value}  </div>
                </div>
                 <div style={{height: 13, width:'100%', paddingTop:3}}>{/* row3*/}
                     {threeColor && <div style={ column3}>{fluxArray[1].label}</div>}
                     {threeColor && <div style={ column4}>{fluxArray[1].value}</div>}
                     {threeColor && <div style={ column5}>{fluxArray[2].label}</div>}
                     {threeColor && <div style={ column6}>{fluxArray[2].value}</div>}
                     {hipsPixel && <div style={ column3}>{healpixNorderReadout.label} </div>}
                     {hipsPixel && <div style={ column4}> {healpixNorderReadout.value}  </div>}
                </div>
                <div style={lockByClickStyle} title='Click on an image to lock the display at that point.'>
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
PopupMouseReadoutFull.propTypes = {
    readout: PropTypes.object,
    showHealpixPixel : PropTypes.bool
};
