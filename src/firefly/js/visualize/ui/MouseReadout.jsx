
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
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';
import CoordinateSys from '../CoordSys.js';
import CysConverter from '../CsysConverter.js';
import CoordUtil from '../CoordUtil.js';
import debounce from 'lodash/debounce'; //TEST CODE
import {callGetFileFlux} from '../../rpc/PlotServicesJson.js'; //TEST CODE

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

	var leftColumn = {width: 200, display: 'inline-block'};

	var rightColumn = {display: 'inline-block'};
	var  textStyle = {textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
	return (
			<div style={ rS}>
               <div>

				 <div	style={leftColumn} onClick={ () => showDialog('pixelSize')}>
					 <div style={ textStyle} > { updateField('pixelSize')}</div>
				 </div>

				 <div   style={rightColumn} onClick={ () => showDialog('coordinateSys' )}>
					 <div style={ textStyle} >{ updateField('coordinateSys')} </div>
					 {showReadout(plot, mouseState, CoordinateSys.EQ_J2000)}</div>

              </div>
	         <div>
				 <div	style={leftColumn} > {showReadout(plot, mouseState) } </div>
				 <div style={ rightColumn}  onClick={ () => showDialog('imagePixel' )}>
					 <div style={ textStyle} >{updateField('imagePixel' )} </div>
					 {showReadout(plot, mouseState, CoordinateSys.PIXEL)}
				 </div>
		    </div>

		  </div>

	);
}

function showReadout(plot, mouseState, coordinate){
	if (!plot) return'';
	if (isBlankImage(plot)) return'';

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

		switch (coordinate){
			case CoordinateSys.EQ_J2000:
				var hmsRa = CoordUtil.convertLonToString(lon, wpt.getCoordSys());
				var hmsDec = CoordUtil.convertLatToString(lat, wpt.getCoordSys());
				result = ' '+ hmsRa +' ' + hmsDec;
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
	testFlux(plot, mouseState.imagePt); //TEST CODE
    return result;
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


const testFlux = debounce( (plot,iPt) =>  {
	callGetFileFlux(plot.plotState, iPt)
			.then( (result) => {
				console.log(result);
			})
			.catch ( (e) => {
				console.log(`flux error: ${plot.plotId}`, e);
			});
},200);
