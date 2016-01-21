
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/16/2016
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React from 'react';
import {makeScreenPt,makeImagePt,makeWorldPt} from '../Point.js';
import MouseState from '../VisMouseCntlr.js';
import {makeImageFromTile,createImageUrl,isTileVisible} from './TileDrawHelper.jsx';
import {isBlankImage} from '../WebPlot.js';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';
import { showCoordinateOptionDialog} from './MouseReadoutOptionDialog.jsx';


var rS= {
	border: '1px solid white',
	width: 500,
	height: 32,
	display: 'inline-block',
	position: 'relative',
	verticalAlign: 'top'
};

var columnS = {
	position: 'absolute',
     left : 0,
	 width: '30%',
	height: 32,
	display: 'inline-block',

};
var bS = {
	border: 'none',
background:'none',
outline: 'none'

};
const mrMouse= [ MouseState.ENTER,MouseState.EXIT, MouseState.MOVE, MouseState.DOWN , MouseState.CLICK];
const EMPTY= <div style={rS}></div>;
export function MouseReadout({plotView:pv,size,mouseState}) {

	if (!pv || !mouseState) return EMPTY;
	var pixelValue='Pixel Size:';
	var coordinateSys='EQ-2000:';
	return (
			<div style={rS}>

				<label  fieldKey='coordinateSys' onclick={ (fieldKey) => showDialog(fieldKey )}>  { coordinateSys}</label>


			<br/>

				<InputFieldLabel  label= 'Flux:'/>
	     	    <InputFieldLabel label='Image Pixel:'/>

		  </div>

	);
}

function showDialog(fieldKey) {

		console.log('showing option dialog');
	    showCoordinateOptionDialog(fieldKey);


}

MouseReadout.propTypes= {
	plotView: React.PropTypes.object,
	size: React.PropTypes.number.isRequired,
	mouseState: React.PropTypes.object
};
