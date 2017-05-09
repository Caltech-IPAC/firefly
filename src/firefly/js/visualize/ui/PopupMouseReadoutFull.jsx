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
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import {STANDARD_READOUT, dispatchChangeLockByClick, dispatchChangeLockUnlockByClick} from '../../visualize/MouseReadoutCntlr.js';
import {getMouseReadout, labelMap, getFluxInfo} from './MouseReadout.jsx';

//--------------- Icons --------------------------------
import LOCKED from    'html/images/icons-2014/lock_20x20.png';
import UNLOCKED from  'html/images/icons-2014/unlock_20x20.png';



const linkLook = {
    textDecoration: 'underline',
    fontStyle: 'italic',
};

const column1 = {
    width: 80,
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


export function PopupMouseReadoutFull({readout}){


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
    var objList={};
    Object.keys( readout.readoutPref).forEach( (key) =>  {
        objList[key]=getMouseReadout(sndReadout.readoutItems,  readout.readoutPref[key] );
    });

    if (!objList)return <div style={rS}/>;

    const {mouseReadout1, mouseReadout2, pixelSize} = objList;

    const {fluxLabels, fluxValues} = getFluxInfo(sndReadout);


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
                    <div style={ clone(column3,linkLook)} onClick={ () => showDialog('pixelSize', readout.readoutPref.pixelSize)}>
                        {labelMap[readout.readoutPref.pixelSize] }
                    </div>
                    <div style={column4}>{pixelSize} </div>

                    <div style={ clone(column5,linkLook)} onClick={ () => showDialog('mouseReadout1', readout.readoutPref.mouseReadout1)}>
                        { labelMap[readout.readoutPref.mouseReadout1] }
                    </div>
                    <div style={column6}> {mouseReadout1} </div>

                </div>
                <div style={{paddingTop: 3}}>{/* row2*/}
                    <div style={ column3_r2}>{fluxLabels[0]}</div>
                    <div style={ column4}> {fluxValues[0]}</div>

                    <div style={ clone(column5,linkLook)} onClick={ () => showDialog('mouseReadout2' ,readout.readoutPref.mouseReadout2 )}>
                        {labelMap[readout.readoutPref.mouseReadout2] } </div>

                    <div style={column6}>  {mouseReadout2}  </div>
                </div>
                 <div style={{height: 13, width:'100%', paddingTop:3}}>{/* row3*/}
                     {threeColor && <div style={ column3}>{fluxLabels[1]}</div>}
                     {threeColor && <div style={ column4}>{fluxValues[1]}</div>}
                     {threeColor && <div style={ column5}>{fluxLabels[2]}</div>}
                     {threeColor && <div style={ column6}>{fluxValues[2]}</div>}
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
    readout: PropTypes.object
};

function showDialog(fieldKey, radioValue) {

    showMouseReadoutOptionDialog(fieldKey, radioValue);

}
