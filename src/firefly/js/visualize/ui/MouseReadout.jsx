
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   2/23/16
 *     DM-4788
 */
import React, {PropTypes} from 'react';
import MouseState from '../VisMouseCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {isBlankImage} from '../WebPlot.js';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';
import CoordinateSys from '../CoordSys.js';
import CysConverter from '../CsysConverter.js';
import CoordUtil from '../CoordUtil.js';
import VisUtil from '../VisUtil.js';
import {debounce} from 'lodash';
import {callGetFileFlux} from '../../rpc/PlotServicesJson.js';
import numeral from 'numeral';
import Band from '../Band.js';

const rS= {
	width: 550,
	height: 32,
	display: 'inline-block',
	position: 'relative',
	verticalAlign: 'top',
	cursor:'pointer'
};

const EMPTY= <div style={rS}></div>;
const EMPTY_READOUT='';
//const readoutMouse= [MouseState.DRAG_COMPONENT, MouseState.DRAG, MouseState.MOVE, MouseState.DOWN, MouseState.ENTER];
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
const column1 = {width: 90, paddingRight: 5, textAlign:'right',textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
const column1Fl = {width: 90, paddingRight: 5, textAlign:'right', display: 'inline-block'};
const column2 = {width: 94, display: 'inline-block'};
const column3 = {width: 74,  paddingRight: 5, textAlign:'right',textDecoration: 'underline', color: 'DarkGray', fontStyle:'italic' ,  display: 'inline-block'};
const column4 = {width: 148,display: 'inline-block'};
const column5 = {width: 90, paddingLeft:8, display: 'inline-block'};
const column5_1 = {width: 90, paddingLeft:5, display: 'inline-block'};
const precision7Digit='0.0000000';
const precision3Digit='0.000';
const precision1Digit='0.0';


export  class MouseReadout extends React.Component {


	constructor(props) {
		super(props);
		this.state=({point:this.props.mouseState.imagePt, flux:EMPTY_READOUT});
		this.showFlux=this.showFlux.bind(this);
	}


	componentWillReceiveProps() {
		this.setState({point:this.props.mouseState.imagePt, flux:EMPTY_READOUT});
		this.showFlux(this.props.plotView, this.props.mouseState);
	}

	render() {


		const {visRoot, plotView, mouseState}= this.props;
		if (!plotView || !mouseState) return EMPTY;

		var plot= primePlot(plotView);
		if (!plot) return EMPTY;
		if (isBlankImage(plot)) return EMPTY;

		var spt= mouseState.screenPt;
		if (!spt) return EMPTY;


		var {width:screenW, height:screenH }= plot.screenSize;
		var bands = plot.plotState.getBands();
		var fluxLabel;
		if (bands.length===1) {
		     fluxLabel = showFluxLabel(plot, bands[0]);
		 }
		 else {
		 //TODO three bands
		 }
		var flux;

		var title = plotView.plots[0].title;

		if (spt.x<0 || spt.x>screenW || spt.y<0 || spt.y>screenH) {
			fluxLabel=EMPTY_READOUT;
		}
		else {
			flux=this.state.flux;
		}

		if (bands.length===1) {

			return (
				  <MouseReadoutForm visRoot={visRoot} plot={plot}
									 mouseState={mouseState}
									 fluxLabel={fluxLabel}
									 flux={flux} title={title}
		    	/>
	     	);
		}
        else {
			//TODO three color
		}


	}

	showFlux(plotView, mouseState) {

		var plot = primePlot(plotView);

		if (!plot) return;


		const getFlux = debounce((plot, iPt) => {
			callGetFileFlux(plot.plotState, iPt)

				.then((result) => {

					if (result.hasOwnProperty('NO_BAND')) {
						var fluxStr = `${numeral(result.NO_BAND).format(precision7Digit)} ${plot.webFitsData[0].fluxUnits}`;
						var currentPt = mouseState.imagePt;
						if (currentPt  === this.state.point) {

							this.setState({point:currentPt, flux:fluxStr});
				    	}
					}
					else {
						//TODO three color band
					}
				})
				.catch((e) => {
					console.log(`flux error: ${plot.plotId}`, e);
				});
		}, 200);

		var iPt=mouseState.imagePt;
		getFlux(plot, iPt);

	}

}

MouseReadout.propTypes= {
	visRoot:   PropTypes.object.isRequired,
	plotView:  PropTypes.object,
	mouseState:PropTypes.object.isRequired
};


function showFluxLabel(plot, band){

	if (!plot) return EMPTY_READOUT;

	var webFitsData= plot.webFitsData;
	if (!band) return EMPTY_READOUT;
	var fluxUnits= webFitsData[band.value].fluxUnits;

	var start;
	switch (band) {
		case Band.RED : start= 'Red '; break;
		case Band.GREEN : start= 'Green '; break;
		case Band.BLUE : start= 'Blue '; break;
		case Band.NO_BAND : start=''; break;
		default : start= ''; break;
	}
	var  valStr= start.length >0 ? 'Val: ' : 'Value: ';

	var fluxUnitInUpperCase=fluxUnits.toUpperCase();
	if (fluxUnitInUpperCase==='DN' ||  fluxUnitInUpperCase==='FRAMES' || fluxUnitInUpperCase==='') {
		return  start + valStr;
	}
	else {
		return  start + 'Flux: ';
	}

}


function MouseReadoutForm({visRoot, plot, mouseState, fluxLabel, flux, title}) {

		return (
			<div style={ rS}>
               <div  >
				    <div style={ column1} onClick={ () => showDialog('pixelSize', visRoot.pixelSize)}>
						{labelMap[visRoot.pixelSize] }
					 </div>
					 <div style={column2} >{ showReadout(plot, mouseState,visRoot.pixelSize)}</div>


				    <div  style={ column3} onClick={ () => showDialog('mouseReadout1' ,visRoot.mouseReadout1)}>
						 { labelMap[visRoot.mouseReadout1] }
				    </div>
					<div style={column4} > {showReadout(plot, mouseState,visRoot.mouseReadout1)} </div>


				  <div style={column5}> {title}  </div>
             </div>


				<div>
					<div style={ column1Fl} >{fluxLabel}</div>
					<div style={ column2}  > {flux}</div>
					<div  style={ column3} onClick={ () => showDialog('mouseReadout2' ,visRoot.mouseReadout2)}>
						{labelMap[ visRoot.mouseReadout2] } </div>
					<div style={column4} >	{showReadout(plot, mouseState, visRoot.mouseReadout2)}</div>

					<div style={column5_1}  title='Click on an image to lock the display at that point.'   >
						<input  type='checkbox' name='aLock' value='lock'
							   onChange = { (request) => setClickLock(plot,mouseState , request) } />
						Lock by click
					</div>
				</div>

			</div>




	);
}

MouseReadoutForm.propTypes= {
	visRoot:   PropTypes.object.isRequired,
	plot:  PropTypes.object.isRequired,
	mouseState: PropTypes.object.isRequired,
	fluxLabel:PropTypes.string.isRequired,
	flux:PropTypes.string,
	title:PropTypes.string.isRequired
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

	var spt= mouseState.screenPt;

	var {width:screenW, height:screenH }= plot.screenSize;
	if (spt.x<0 || spt.x>screenW || spt.y<0 || spt.y>screenH){
		return EMPTY_READOUT;
	}



	//if (!readoutMouse.includes(mouseState.mouseState)) return EMPTY_READOUT;

	if (readoutValue==='fitsIP'){
			return ` ${numeral(mouseState.imagePt.x).format(precision1Digit)}, ${
			numeral(mouseState.imagePt.y).format(precision1Digit)}`;
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

		switch (coordinate) {
			case CoordinateSys.EQ_J2000:
				if (type === 'hms') {
					result = ` ${hmsLon}, ${hmsLat}`;
		        }
				else {
					//convert to decimal representation
					var dmsLon = CoordUtil.convertStringToLon(hmsLon,coordinate);
					var dmsLat = CoordUtil.convertStringToLat( hmsLat,coordinate);
					result =` ${numeral(Number(dmsLon)).format(precision7Digit)}, ${numeral(Number(dmsLat)).format(precision7Digit)}`;
				}
				break;
			case CoordinateSys.GALACTIC:
			case CoordinateSys.SUPERGALACTIC:

				var lonShort = numeral(lon).format(precision7Digit) ;
				var latShort = numeral(lat).format(precision7Digit);
				result= ` ${lonShort}, ${latShort}`;
				break;
			case CoordinateSys.EQ_B1950:
				result = ` ${hmsLon}, ${hmsLat}`;
				break;

			case CoordinateSys.PIXEL:
				var pt =plot.projection.getPixelScaleArcSec();
				result= `  ${numeral(pt).format(precision3Digit)}"`;
				break;
			case CoordinateSys.SCREEN_PIXEL:
				var size =plot.projection.getPixelScaleArcSec()/plot.zoomFactor;
				result= `  ${numeral(size).format(precision3Digit)}"`;
				break;

			default:
				result='';
				break;
		}

	}


    return result;
}

function showDialog(fieldKey, radioValue) {

		//console.log('showing ' + fieldKey+ ' option dialog');
	    showMouseReadoutOptionDialog(fieldKey, radioValue);

}

