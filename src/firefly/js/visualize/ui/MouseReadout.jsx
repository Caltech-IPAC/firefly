
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

const EMPTY= <div style={rS}></div>;

/**
 *
 * @param visRoot
 * @param pv
 * @param mouseState
 * @returns {XML}
 * @constructor
 */
export function MouseReadout({visRoot, plotView:pv,mouseState}) {

	if (!pv || !mouseState) return EMPTY;

	var plot= primePlot(pv);

	var leftColumn = {width: 200, display: 'inline-block'};

	var rightColumn = {display: 'inline-block'};
	var  textStyle = {textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
	return (
			<div style={ rS}>
               <div>

				 <div style={leftColumn} onClick={ () => showDialog('pixelSize', visRoot.pixelSize)}>
					 <div style={ textStyle} > {getLabel(visRoot.pixelSize) }
					 </div>
				 </div>

				 <div style={rightColumn} onClick={ () => showDialog('readout1' ,visRoot.mouseReadout1)}>
					 <div style={ textStyle} > { getLabel( visRoot.mouseReadout1) }
					 </div>
					 {showReadout(plot, mouseState,visRoot.mouseReadout1)}
				 </div>
              </div>

			  <div>
				 <div style={leftColumn} > {showReadout(plot, mouseState,visRoot.mouseReadout2 ) } </div>
				 <div style={ rightColumn}  onClick={ () => showDialog('readout2' ,visRoot.mouseReadout2)}>
					 <div style={ textStyle} >{getLabel( visRoot.mouseReadout2)} </div>
					 {showReadout(plot, mouseState, visRoot.mouseReadout2)}
				 </div>
		    </div>

		  </div>

	);
}
MouseReadout.propTypes= {
	visRoot:React.PropTypes.object.isRequired,
	plot:React.PropTypes.object.isRequired,
	mouseReadout:React.PropTypes.object.isRequired,
	radioValues:React.PropTypes.object.isRequired
};

function getLabel(radioValue){
	var gLabel;
	switch(radioValue){
		case 'eqj2000hms' || 'eqj2000DCM':
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
 * Display the mouse readout based on the chosen coordinate
 * @param plot
 * @param mouseState
 * @param readoutValue
 * @returns {*}
 */
function showReadout(plot, mouseState, readoutValue){
	if (!plot) return'';
	if (isBlankImage(plot)) return'';

	var {coordinate, type} =getCoordinateMap(readoutValue);

	var cc= CysConverter.make(plot);
	var wpt= cc.getWorldCoords(mouseState.imagePt);
	if (!wpt) return;

	var spt= mouseState.screenPt;

    if (!spt) return '';
	var {width:screenW, height:screenH }= plot.screenSize;
	if (spt.x<0 || spt.x>screenW || spt.y<0 || spt.y>screenH){

		return '';
	}
	var result;
	var lon = wpt.getLon();
	var lat = wpt.getLat();


	if (coordinate){

		switch (coordinate) {
			case CoordinateSys.EQ_J2000:
				if (type === 'hms') {

				var hmsRa = CoordUtil.convertLonToString(lon, wpt.getCoordSys());
				var hmsDec = CoordUtil.convertLatToString(lat, wpt.getCoordSys());
				result = ' ' + hmsRa + ' ' + hmsDec;
		        }
				else {
					var hmsRa = CoordUtil.convertLonToString(lon, wpt.getCoordSys());
					var hmsDec = CoordUtil.convertLatToString(lat, wpt.getCoordSys());
					var dmsRa = CoordUtil.convertStringToLon(hmsRa, wpt.getCoordSys());
					var dmsDec = CoordUtil.convertStringToLat(hmsDec, wpt.getCoordSys());
					var dmsRaShort = dmsRa.toString().substring(0, 8);
					var dmsDecShort = dmsDec.toString().substring(0, 8);
					result = ' ' + dmsRaShort + ' ' +dmsDecShort;
				}
				break;
			case CoordinateSys.GALACTIC || CoordinateSys.SUPERGALACTIC:
				//var lonRa = CoordUtil.convertLonToString(lon, wpt.getCoordSys());
				//var latDec = CoordUtil.convertLatToString(lat, wpt.getCoordSys());
				//result=  ' '+lonRa + ' '+ latDec;
				result= ' ' + lon + ' '+ lat;
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


