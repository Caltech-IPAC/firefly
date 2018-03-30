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
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {STANDARD_READOUT, dispatchChangeLockByClick, dispatchChangeLockUnlockByClick} from '../../visualize/MouseReadoutCntlr.js';
import {getNonFluxDisplayElements} from './MouseReadoutUIUtil';

//--------------- Icons --------------------------------
import LOCKED from    'html/images/icons-2014/lock_20x20.png';
import UNLOCKED from  'html/images/icons-2014/unlock_20x20.png';

const rS = {
    cursor: 'pointer',
    display: 'flex',
    padding: '2px 0 2px 3px'
};


const EMPTY = <div style={rS}></div>;


const column1 = {
    width: 74,
    paddingRight: 1,
    textAlign: 'right',
    textDecoration: 'underline',
    color: 'DarkGray',
    fontStyle: 'italic',
    display: 'inline-block'
};


const column2 = {width: 170,paddingLeft: 2, textAlign: 'left', display: 'inline-block'};

const column3 =    {width: 100,    paddingLeft: 8, textAlign: 'left',display: 'inline-block'};

export function PopupMouseReadoutMinimal({readout}){

    //get the standard readouts
    const sndReadout= readout[STANDARD_READOUT];
    if (!get(sndReadout,'readoutItems')) return EMPTY;

    const {isHiPS, readoutItems}= sndReadout;
    const displayEle= getNonFluxDisplayElements(readoutItems,  readout.readoutPref, isHiPS);
    const {readout1, readout2, showReadout1PrefChange, showReadout2PrefChange}= displayEle;

    const lock = readout.isLocked ? LOCKED:UNLOCKED;
    return (
        <div style={ rS}>
            {/*row1*/}
            <img  src= {lock}  title= 'Lock the readout panel visible' onClick ={() =>{
                      dispatchChangeLockUnlockByClick(!readout.isLocked);
                   }}
            />
            <div>
                <div>
                    <div style={column1} onClick={ showReadout1PrefChange}> { readout1.label } </div>
                    <div style={column2}> {readout1.value} </div>
                </div>
                <div>{/* row2*/}
                    <div style={column1} onClick={ showReadout2PrefChange}> {readout2.label} </div>
                    <div style={column2}>  {readout2.value}  </div>

                    <div style={column3} title='Click on an image to lock the display at that point.'>
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
        </div>
    );
}

PopupMouseReadoutMinimal.propTypes = {
    readout: PropTypes.object
};

