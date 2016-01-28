
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
import {makeImageFromTile,createImageUrl,isTileVisible} from './../iv/TileDrawHelper.jsx';
import {isBlankImage} from '../WebPlot.js';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionDialog.jsx';


var rS= {
	border: '1px solid white',
	width: 500,
	height: 32,
	display: 'inline-block',
	position: 'relative',
	verticalAlign: 'top'
};

const mrMouse= [ MouseState.ENTER,MouseState.EXIT, MouseState.MOVE, MouseState.DOWN , MouseState.CLICK];
const EMPTY= <div style={rS}></div>;
export function MouseReadout({plotView:pv,size,mouseState}) {

	if (!pv || !mouseState) return EMPTY;


	var leftColumn = {width: 200, display: 'inline-block'};

	var rightColumn = {display: 'inline-block'};
	return (
			<div style={ rS}>
               <div>

				 <div	style={leftColumn} onClick={ () => showDialog('pixelSize')}>  { updateField('pixelSize')}</div>
				 <div   style={rightColumn} onClick={ () => showDialog('coordinateSys' )}>  { updateField('coordinateSys')}</div>

              </div>
	         <div>

				 <div style={{display: 'inline-block', paddingLeft:200}}  onClick={ () => showDialog('imagePixel' )}>{updateField('imagePixel' )}</div>
		    </div>

		  </div>

	);
}

function getReadOut(fieldKey){
	console.log('TODO');
}
function showDialog(fieldKey) {

		console.log('showing ' + fieldKey+ ' option dialog');
	   showMouseReadoutOptionDialog(fieldKey);

}
function updateField(fieldKey){
	if (fieldKey==='pixelSize'){
		return 'Pixel Size:';
	}
	else  if (fieldKey==='coordinateSys'){
		return 'EQ-J2000:';
	}
	else {
		return 'Image Pixel:';;
	}
}
MouseReadout.propTypes= {
	plotView: React.PropTypes.object,
	size: React.PropTypes.number.isRequired,
	mouseState: React.PropTypes.object
};
