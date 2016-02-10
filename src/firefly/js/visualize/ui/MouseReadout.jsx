
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
import {primePlot} from '../PlotViewUtil.js';
import {isBlankImage} from '../WebPlot.js';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';
import {showMouseReadoutOptionDialog, getCoordinateMap} from './MouseReadoutOptionPopups.jsx';
import CoordinateSys from '../CoordSys.js';
import CysConverter from '../CsysConverter.js';
import CoordUtil from '../CoordUtil.js';

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

export function MouseReadout({plotView:pv,mouseState}) {

	if (!pv || !mouseState) return EMPTY;

	var plot= primePlot(pv);
	//var mouseReadout= {readout1:'EQ-J2000:', readout2:'Image Pixel:', pixelSize:'Pixel Size:'};
	var radioValues={readout1:'eqj2000Dhms', readout2:'fitsIP', pixelSize:'pixelSize'};

	var leftColumn = {width: 200, display: 'inline-block'};

	var rightColumn = {display: 'inline-block'};
	var  textStyle = {textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
	return (
			<div style={ rS}>
               <div>

				 <div style={leftColumn} onClick={ () => showDialog('pixelSize', radioValues.pixelSize)}>
					 <div style={ textStyle} > {getRadioGroupLabel( radioValues.pixelSize) }
					 </div>
				 </div>

				 <div style={rightColumn} onClick={ () => showDialog('readout1' ,radioValues.readout1)}>
					 <div style={ textStyle} > { getRadioGroupLabel( radioValues.readout1) }
					 </div>
					 {showReadout(plot, mouseState,radioValues.readout1)}
				 </div>

              </div>

			  <div>
				 <div style={leftColumn} > {showReadout(plot, mouseState,radioValues.readout2 ) } </div>
				 <div style={ rightColumn}  onClick={ () => showDialog('readout2' ,radioValues.readout2)}>
					 <div style={ textStyle} >{getRadioGroupLabel( radioValues.readout2)} </div>
					 {showReadout(plot, mouseState, radioValues.readout2)}
				 </div>
		    </div>

		  </div>

	);
}
MouseReadout.propTypes= {
	plot:React.PropTypes.object.isRequired,
	mouseReadout:React.PropTypes.object.isRequired,
	radioValues:React.PropTypes.object.isRequired
};

function getRadioGroupLabel(radioValue){
	var gLabel;
	switch(radioValue){
		case 'eqj2000Dhms' || 'eqj2000DCM':
			gLabel='EQ-J2000:';
			break;
		case 'galactic':
			gLabel='Gal:';
			break;
		case 'eqb1950':
			gLabel='Eq-B1950:';
			break;
		case 'fitsIP':
			gLabel='Image Pixel:';
			break;
		case 'pixelSize':
			gLabel='Pixel Size:';
			break;
		case 'sPixelSize':
			gLabel='Screen Pixel Size:';
			break;
	}
    return gLabel;
}
function showReadout(plot, mouseState, readoutValue){
	if (!plot) return'';
	if (isBlankImage(plot)) return'';

	var {coordinate, type} =getCoordinateMap(readoutValue);

	var cc= CysConverter.make(plot);
	var wpt= cc.getWorldCoords(mouseState.imagePt);
	var spt= mouseState.screenPt;

    if (!spt) return '';
	var {width:screenW, height:screenH }= plot.screenSize;
	if (spt.x<0 || spt.x>screenW || spt.y<0 || spt.y>screenH){
		//console.log(spt+  ' outside the screen');
		return '';
	}
	var result;
	var lon = wpt.getLon();
	var lat = wpt.getLat();

	if (coordinate){

		switch (coordinate) {
			case CoordinateSys.EQ_J2000:
				if (type === 'hhmmss') {

				var hmsRa = CoordUtil.convertLonToString(lon, wpt.getCoordSys());
				var hmsDec = CoordUtil.convertLatToString(lat, wpt.getCoordSys());
				result = ' ' + hmsRa + ' ' + hmsDec;
		        }
				else {
					//TODO
				}
				break;
			case CoordinateSys.GALACTIC || CoordinateSys.SUPERGALACTIC:
				result=  ' '+lon + ' '+ lat;
				break;
			case CoordinateSys.PIXEL:
				//result = mouseState.pixelX + ' ' +  mouseState.pixelY;
				result = ' '+spt.x + ' ' + spt.y;
				break;
			default:
				result='';
				break;
		}

	}
	else {
		//TODO readout for pixel size

	}

    return result;
}
function showDialog(fieldKey, radioValue) {

		console.log('showing ' + fieldKey+ ' option dialog');
	    showMouseReadoutOptionDialog(fieldKey, radioValue);

}

MouseReadout.propTypes= {
	plotView: React.PropTypes.object,
	mouseState: React.PropTypes.object,
	mouseReadout:React.PropTypes.object
};
