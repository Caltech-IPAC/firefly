
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   1/16/2016
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React, {PropTypes} from 'react;
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
import VisUtil from '../VisUtil.js';

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

const EMPTY= <div style={rS}></div>;
const EMPTY_READOUT='';
const magMouse= [MouseState.DRAG_COMPONENT, MouseState.DRAG, MouseState.MOVE, MouseState.DOWN];
const coordinateMap = {
	galactic:CoordinateSys.GALACTIC,
	eqb1950:CoordinateSys.EQ_B1950,
	pixelSize:CoordinateSys.PIXEL,
	sPixelSize: CoordinateSys.SCREEN_PIXEL
};
const labelMap = {
	eqj2000hms:'EQ-J2000:',
	eqj2000DCM:'EQ-J2000:',
	galactic:'Gal:',
	eqb1950:'Eq-B1950:',
	fitsIP:'Image Pixel:',
	pixelSize:'Pixel Size:',
	sPixelSize:'Screen Pixel Size:'
};

/**
 *
 * @param visRoot
 * @param plotView
 * @param mouseState
 * @returns {XML}
 * @constructor
 */
export function MouseReadout({visRoot, plotView, mouseState}) {

	if (!plotView || !mouseState) return EMPTY;


	var plot= primePlot(plotView);
	if (!plot) return'';
	if (isBlankImage(plot)) return EMPTY;

	//TODO check why this does not work
	//const hideReadout= renderEmpty(plotView,plot, mouseState, visRoot);

	if (!magMouse.includes(mouseState.mouseState)) EMPTY; //hideReadout;

	var spt= mouseState.screenPt;
	if (!spt) return EMPTY;//hideReadout;



	var leftColumn = {width: 140, display: 'inline-block'};

	var title = plotView.plots[0].title;
	var middleColumn = {width: 230, display: 'inline-block'};
	var  textStyle = {textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
	var rightColumn = {paddingLeft: '35px',  display: 'inline-block'};
	return (
			<div style={ rS}>
               <div>
				  <div style={leftColumn} onClick={ () => showDialog('pixelSize', visRoot.pixelSize)}>
					  <div style={ textStyle} > {labelMap[visRoot.pixelSize] }</div>
					  { showReadout(plot, mouseState,visRoot.pixelSize)}
				  </div>

				  <div style={middleColumn} onClick={ () => showDialog('mouseReadout1' ,visRoot.mouseReadout1)}>
					 <div style={ textStyle} > { labelMap[visRoot.mouseReadout1] } </div>
					 {showReadout(plot, mouseState,visRoot.mouseReadout1)}
				  </div>

				  <div style={rightColumn}> {title}  </div>
              </div>

			  <div>
				  <div style={leftColumn} > {showReadout(plot, mouseState,visRoot.flux ) } </div>
				  <div style={ middleColumn}  onClick={ () => showDialog('mouseReadout2' ,visRoot.mouseReadout2)}>
					 <div style={ textStyle} >{labelMap[ visRoot.mouseReadout2] } </div>
					 {showReadout(plot, mouseState, visRoot.mouseReadout2)}
				  </div>
				  <div style={rightColumn} title='Click on an image to lock the display at that point.'   >
					  <input type='checkbox' name='aLock' value='lock'
							 onChange = { (request) => setClickLock(plot,mouseState , request) } />
					  Lock by click
				  </div>
		      </div>

		  </div>

	);
}

MouseReadout.propTypes= {
	visRoot:   PropTypes.object.isRequired,
	plotView:  PropTypes.object,
	mouseState: PropTypes.object.isRequired,
};


/**
 *
 * This method map the value in coordinate option popup to its value
 * @param coordinateRadioValue : the value in the radio button
 * @returns {{coordinate: *, type: *}}
 */
function getCoordinateMap(coordinateRadioValue){
	var coordinate;
	var type;
	if (coordinateRadioValue ==='eqj2000hms'){
		coordinate = CoordinateSys.EQ_J2000;
		type = 'hms';
	}
	else if (coordinateRadioValue ==='eqj2000DCM'){
		coordinate = CoordinateSys.EQ_J2000;
		type = 'decimal';
	}
	else{
		coordinate = coordinateMap[coordinateRadioValue];
		//if coordinate is not define, assign it as below
		if (!coordinate) coordinate=CoordinateSys.UNDEFINED;
	}
	return {coordinate, type};
}
function renderEmpty(plotView,plot, mouseState, visRoot){
	var leftColumn = {width: 120, display: 'inline-block'};

	var title = plotView.plots[0].title;
	var middleColumn = {width: 250, display: 'inline-block'};
	var  textStyle = {textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
	var rightColumn = {paddingLeft: '35px',  display: 'inline-block'};
	return (
		<div style={ rS}>
			<div>
				<div style={leftColumn} onClick={ () => showDialog('pixelSize', visRoot.pixelSize)}>
					<div style={ textStyle} > {labelMap[visRoot.pixelSize] }</div>
					{EMPTY}
				</div>

				<div style={middleColumn} onClick={ () => showDialog('mouseReadout1' ,visRoot.mouseReadout1)}>
					<div style={ textStyle} > { labelMap[ visRoot.mouseReadout1] } </div>
					{EMPTY}
				</div>

				<div style={rightColumn}> {title}  </div>
			</div>

			<div>
				<div style={leftColumn} >  </div>
				<div style={ middleColumn}  onClick={ () => showDialog('mouseReadout2' ,visRoot.mouseReadout2)}>
					<div style={ textStyle} >{labelMap[visRoot.mouseReadout2] } </div>
					{EMPTY}
				</div>
				<div style={rightColumn} title='Click on an image to lock the display at that point.'   >
					<input type='checkbox' name='aLock' value='lock'
						   onChange = { (request) => setClickLock(plot,mouseState , request) } />
					Lock by click
				</div>
			</div>

		</div>

	);
}

/**
 * set the image lock
 * @param plot
 * @param mouseState
 * @param request
 */
function setClickLock( plot,mouseState, request) {

	if (request.hasOwnProperty('target')) {
		var target = request.target;
		var pixelClickLock =target.checked;
		if (pixelClickLock) {
			plot.plotState.getWebPlotRequest().setAllowImageSelection(false);
			//mouseState.setAllowImageSelection(false);

		} else {
			plot.plotState.getWebPlotRequest().setAllowImageSelection(true);
			//mouseState.setAllowImageSelection(true);
		}
	}


}

/**
 * Display the mouse readout based on the chosen coordinate
 * @param plot
 * @param mouseState
 * @param readoutValue
 * @returns {*}
 */
function showReadout(plot, mouseState, readoutValue){

	if (!plot) return EMPTY_READOUT;
	if (isBlankImage(plot)) return EMPTY_READOUT;

	var spt= mouseState.screenPt;
	if (!spt) return EMPTY_READOUT;



	var {width:screenW, height:screenH }= plot.screenSize;
	if (spt.x<0 || spt.x>screenW || spt.y<0 || spt.y>screenH){
		return EMPTY_READOUT;
	}

	if (readoutValue==='Flux'){
		//result='Flux';
		//TODO get flux
		return 'Flux';
	}

	var result;
	var cc= CysConverter.make(plot);
	var wpt= cc.getWorldCoords(mouseState.imagePt);
	if (!wpt) return;
	var {coordinate, type} =getCoordinateMap(readoutValue);


	if (coordinate){
		var ptInCoord= VisUtil.convert(wpt, coordinate);

		var lon = ptInCoord.getLon();
		var lat = ptInCoord.getLat();
		var hmsLon = CoordUtil.convertLonToString(lon, coordinate);
		var hmsLat = CoordUtil.convertLatToString(lat, coordinate);

       // console.log(coordinate.toString());
		switch (coordinate) {
			case CoordinateSys.EQ_J2000:
				if (type === 'hms') {
					result = ' ' +  hmsLon + ',  ' + hmsLat;
		        }
				else {
					//convert to decimal representation
					var dmsLon = CoordUtil.convertStringToLon(hmsLon,coordinate);//wpt.getCoordSys());
					var dmsLat = CoordUtil.convertStringToLat( hmsLat,coordinate);//wpt.getCoordSys());
					var dmsLonShort =dmsLon.toString().substring(0, 10);
					var dmsLatShort = dmsLat.toString().substring(0, 10);
					result = ' ' + dmsLonShort + ',  ' +dmsLatShort;
				}
				break;
			case CoordinateSys.GALACTIC:
			case CoordinateSys.SUPERGALACTIC:

				var lonShort = lon.toString().substring(0, 10);
				var latShort = lat.toString().substring(0, 10);
				result= ' ' + lonShort + ',  '+latShort;
				break;
			case CoordinateSys.EQ_B1950:
				result = ' ' +  hmsLon + ',  ' + hmsLat;
				break;

			case CoordinateSys.PIXEL:
				var pt =plot.projection.getPixelScaleArcSec();
				var ptShort = pt.toString().substring(0, 5);
				result = ' '+ ptShort+'"';
				break;
			case CoordinateSys.SCREEN_PIXEL:
				var pt =plot.projection.getPixelScaleArcSec()/plot.zoomFactor;
				var ptShort = pt.toString().substring(0, 5);
				result = ' '+ ptShort+'"';
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

