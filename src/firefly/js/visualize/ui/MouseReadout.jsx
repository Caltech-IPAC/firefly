
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
import VisUtil from '../VisUtil.js';

var rS= {
	border: '1px solid white',
	width: 500,
	height: 32,
	display: 'inline-block',
	position: 'relative',
	verticalAlign: 'top'
};

const EMPTY= <div style={rS}></div>;

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

	var leftColumn = {width: 180, display: 'inline-block'};

	var title = plotView.plots[0].title;
	var middleColumn = {width: 200, display: 'inline-block'};
	var  textStyle = {textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
	var rightColumn = {paddingLeft: '10px',  display: 'inline-block'};
	return (
			<div style={ rS}>
               <div>

				 <div style={leftColumn} onClick={ () => showDialog('pixelSize', visRoot.pixelSize)}>
					 <div style={ textStyle} > {getLabel(visRoot.pixelSize) }
					 </div>
				 </div>

				 <div style={middleColumn} onClick={ () => showDialog('readout1' ,visRoot.mouseReadout1)}>
					 <div style={ textStyle} > { getLabel( visRoot.mouseReadout1) }
					 </div>
					 {showReadout(plot, mouseState,visRoot.mouseReadout1)}
				 </div>
				   <div style={rightColumn}> {title} </div>
              </div>

			  <div>
				 <div style={leftColumn} > {showReadout(plot, mouseState,visRoot.flux ) } </div>
				 <div style={ middleColumn}  onClick={ () => showDialog('readout2' ,visRoot.mouseReadout2)}>
					 <div style={ textStyle} >{getLabel( visRoot.mouseReadout2)} </div>
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
	visRoot:React.PropTypes.object.isRequired,
	plotView: React.PropTypes.object,
	mouseState:React.PropTypes.object.isRequired,
};

function getLabel(radioValue){
	var gLabel;
	switch(radioValue){
		case 'eqj2000hms':
		case 'eqj2000DCM':
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
	if (!plot) return'';
	if (isBlankImage(plot)) return'';

	var spt= mouseState.screenPt;
    if (!spt) return '';


	var {width:screenW, height:screenH }= plot.screenSize;
	if (spt.x<0 || spt.x>screenW || spt.y<0 || spt.y>screenH){
		return '';
	}


	if (readoutValue==='FLUX_VALUE'){
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
		//var ptInCoord= VisUtil.convert(wpt, coordinate);
		//TODO remove the line below: it added for testing purpose
		var ptInCoord=wpt;
		//
		var lon = ptInCoord.getLon();
		var lat = ptInCoord.getLat();
		var hmsLon = CoordUtil.convertLonToString(lon, coordinate);
		var hmsLat = CoordUtil.convertLatToString(lat, coordinate);

        console.log(coordinate.toString());
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
			case CoordinateSys.EQ_B1950:
				var lonShort = lon.toString().substring(0, 10);
				var latShort = lat.toString().substring(0, 10);
				result= ' ' + lonShort + ',  '+latShort;
				break;
			//case CoordinateSys.EQ_B1950:
			//	result=' ';// + lon + ', '+lat;
			//	break;

			case CoordinateSys.PIXEL:
			case CoordinateSys.SCREEN_PIXEL:


				result = ' '+ ptInCoord.toString();
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


