/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   2/23/16
 *     DM-4788
 *   3/2/16
 *     DM-4789
 */
import React, {PropTypes} from 'react';
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
import {dispatchChangePointSelection} from '../ImagePlotCntlr.js';
import sCompare from 'react-addons-shallow-compare';

const rS = {
	width: 650,
	minWidth: 550,
	height: 32,
	minHeight: 32,
	display: 'inline-block',
	position: 'absolute',
	verticalAlign: 'top',
	cursor: 'pointer',
	whiteSpace: 'nowrap',
	overflow: 'hidden'
};
const EMPTY = <div style={rS}></div>;
const EMPTY_READOUT = '';
const coordinateMap = {
	galactic: CoordinateSys.GALACTIC,
	eqb1950: CoordinateSys.EQ_B1950,
	pixelSize: CoordinateSys.PIXEL,
	sPixelSize: CoordinateSys.SCREEN_PIXEL
};
const labelMap = {
	eqj2000hms: 'EQ-J2000:',
	eqj2000DCM: 'EQ-J2000:',
	galactic: 'Gal:',
	eqb1950: 'Eq-B1950:',
	fitsIP: 'Image Pixel:',
	pixelSize: 'Pixel Size:',
	sPixelSize: 'Screen Pixel Size:'
};

const columnColorBandFluxLabel = {
	width: 60,
	paddingRight: 1,
	textAlign: 'right',
	color: 'DarkGray',
	display: 'inline-block'
};
const columnColorBandFluxValue = {width: 90, display: 'inline-block'};

const column1 = {
	width: 80,
	paddingRight: 2,
	textAlign: 'right',
	textDecoration: 'underline',
	color: 'DarkGray',
	fontStyle: 'italic',
	display: 'inline-block'
};
const column1Fl = {width: 80, paddingRight: 1, textAlign: 'right', color: 'DarkGray', display: 'inline-block'};
const column2 = {width: 88, display: 'inline-block'};
const column3 = {
	width: 74,
	paddingRight: 1,
	textAlign: 'right',
	textDecoration: 'underline',
	color: 'DarkGray',
	fontStyle: 'italic',
	display: 'inline-block'
};
const column4 = {width: 152, display: 'inline-block'};
const column5 = {width: 80, paddingLeft: 4, display: 'inline-block'};
const column5_1 = {width: 90, paddingLeft: 1, display: 'inline-block'};


const precision7Digit = '0.0000000';
const precision3Digit = '0.000';
const precision1Digit = '0.0';
export class MouseReadout extends React.Component {

	constructor(props) {
		super(props);
		this.showFlux = this.showFlux.bind(this);
		this.setLockState = this.setLockState.bind(this);

		const fluxLabels = getFluxLabels(this.props.plotView);
		const pointInfo = {
			coordinates: [this.props.visRoot.mouseReadout1, this.props.visRoot.mouseReadout2, this.props.visRoot.pixelSize],
			mouseReadouts: [EMPTY_READOUT, EMPTY_READOUT, EMPTY_READOUT]
		};
		this.state = ({
			point: this.props.mouseState.imagePt,
			flux: [EMPTY_READOUT, EMPTY_READOUT, EMPTY_READOUT],
			fluxLabel: fluxLabels,
			isLocked: false,
			pointInfo: pointInfo
		});


		this.getFlux = debounce((mouseState, plot, iPt, isLocked) => {
			callGetFileFlux(plot.plotState, iPt)
				.then((result) => {
					var fluxArray = [];
					var fluxUnitStr = plot.webFitsData[0].fluxUnits;
					if (result.hasOwnProperty('NO_BAND')) {

						var fValue = result.NO_BAND;
						var fluxStr =(fValue!=='NoContext')?`${numeral(fValue).format(precision7Digit)} ${fluxUnitStr}`:'';
						fluxArray = [fluxStr, EMPTY_READOUT, EMPTY_READOUT];

						if (isLocked && mouseState.mouseState.key === 'UP'  || !isLocked  && mouseState.imagePt===iPt){
							this.setState({ flux: fluxArray});
						}

				    }
					else {
						var blueFlux = result.hasOwnProperty('Blue')  ? `${numeral(result.Blue).format(precision7Digit)} ${fluxUnitStr}` : EMPTY_READOUT;
						var greenFlux = result.hasOwnProperty('Green') ? `${numeral(result.Green).format(precision7Digit)} ${fluxUnitStr}` : EMPTY_READOUT;
						var RedFlux = result.hasOwnProperty('Red')  ? `${numeral(result.Red).format(precision7Digit)} ${fluxUnitStr}` : EMPTY_READOUT;
						fluxArray = [RedFlux, greenFlux, blueFlux];
						if (isLocked && mouseState.mouseState.key === 'UP'  || !isLocked  && mouseState.imagePt===iPt){
							this.setState({flux: fluxArray});

						}

					}
				})
				.catch((e) => {
					console.log(`flux error: ${plot.plotId}`, e);
				});
		}, 200);

	}

	componentWillReceiveProps() {


		if (this.props.plotView && (this.state.isLocked && this.props.mouseState.mouseState.key === 'UP' || !this.state.isLocked)) {
			this.setState( {
				point: this.props.mouseState.imagePt,
				fluxLabel: getFluxLabels(this.props.plotView),
				pointInfo: getAllMouseReadouts(this.props.plotView, this.props.mouseState, this.props.visRoot)
			} );

			this.showFlux();
		}


	}
	shouldComponentUpdate(np,ns) {
		return sCompare(this,np,ns);

	}

	setLockState(request) {

		if (request.hasOwnProperty('target')) {
			var target = request.target;
			var pixelClickLock = target.checked;

			//add a callback function to handle the asyn behavior
			this.setState({isLocked: pixelClickLock}, ()=>{
				//force this to be executed before the lock state is st
				setPointLock(this.props.plotView, pixelClickLock);

				this.setState({isLocked:pixelClickLock, flux:[], fluxLabel:[]});
			});
		}

	}

	showFlux() {
		var plot = primePlot(this.props.plotView);
		if (!plot) return EMPTY;
		var spt = this.props.mouseState.screenPt;
		if (!spt) return false;
		var iPt = this.props.mouseState.imagePt;
		if (iPt) {
			this.getFlux(this.props.mouseState, plot, iPt, this.state.isLocked);
		}

	}

	render() {

		const {visRoot, plotView, mouseState}= this.props;

		if (!plotView) return EMPTY;

		var plot = primePlot(plotView);
		if (!plot) return EMPTY;
		if (isBlankImage(plot)) return EMPTY;

		var spt = mouseState.screenPt;

		var bands = plot.plotState.getBands();


		var title = plotView.plots[0].title;
		var isLocked = this.state.isLocked;

		var pointInfo = this.state.pointInfo;
		var currentCoordinates = [this.props.visRoot.mouseReadout1, this.props.visRoot.mouseReadout2, this.props.visRoot.pixelSize];

		var {mouseReadout1, mouseReadout2, pixelSize} = precessReadoutInfo(pointInfo,  this.state.point, currentCoordinates, plot, spt, isLocked);
		var {width:screenW, height:screenH }= plot.screenSize;
		var fluxValues = [];
		var fluxLabels = [];
		const isOutside = (spt && (spt.x < 0 || spt.x > screenW || spt.y < 0 || spt.y > screenH));
		if (isOutside && isLocked || !isOutside) {
				for (var i = 0; i < bands.length; i++) {
						fluxValues[i] = this.state.flux[i];
						fluxLabels[i] = this.state.fluxLabel[i];

				}

		}


		return (

			<div style={ rS}>

				{renderMouseReadoutRow1({visRoot, title, mouseReadout1, pixelSize, fluxLabels, fluxValues})}

				<div>
					<div style={ columnColorBandFluxLabel}>{fluxLabels[2]} </div>
					<div style={ columnColorBandFluxValue}> { fluxValues[2]} </div>

					<div style={ column1Fl}>{fluxLabels[0]}</div>
					<div style={ column2}> {fluxValues[0]}</div>

					<div style={ column3} onClick={ () => showDialog('mouseReadout2' ,visRoot.mouseReadout2)}>
						{labelMap[visRoot.mouseReadout2] } </div>

					<div style={column4}>  {mouseReadout2}  </div>
					<div style={column5_1} title='Click on an image to lock the display at that point.'>
						<input type='checkbox' name='aLock' value='lock'
							   onChange={ (request) => this.setLockState( request) }/>
						Lock by click
					</div>


				</div>

			</div>
		);


	}

}

MouseReadout.propTypes = {
	visRoot: PropTypes.object.isRequired,
	plotView: PropTypes.object,
	mouseState: PropTypes.object.isRequired

};

//===================end of MouseReadout class===========================================================

function renderMouseReadoutRow1({visRoot, title,  mouseReadout1, pixelSize, fluxLabels, fluxValues}) {

	return (
		<div  >
			<div style={ columnColorBandFluxLabel}>{fluxLabels[1]} </div>
			<div style={ columnColorBandFluxValue}> { fluxValues[1]} </div>
			<div style={ column1} onClick={ () => showDialog('pixelSize', visRoot.pixelSize)}>
				{labelMap[visRoot.pixelSize] }
			</div>
			<div style={column2}>{ pixelSize}</div>

			<div style={ column3} onClick={ () => showDialog('mouseReadout1' ,visRoot.mouseReadout1)}>
				{ labelMap[visRoot.mouseReadout1] }
			</div>
			<div style={column4}> {mouseReadout1} </div>


			<div style={column5}> {title}  </div>
		</div>



	);

}
renderMouseReadoutRow1.propTypes = {
	visRoot: PropTypes.object.isRequired,
	mouseReadout1: PropTypes.string.isRequired,
	title: PropTypes.string.isRequired,
	pixelSize: PropTypes.string.isRequired,
	fuxLabel: PropTypes.array.isRequired,
	fluxValue: PropTypes.array.isRequired

};

function setPointLock(plotView, isLocked) {

	var plot = primePlot(plotView);
	if (!plot) return EMPTY;
	dispatchChangePointSelection('mouseReadout',isLocked);
	plot.plotState.getWebPlotRequest().setAllowImageSelection(!isLocked);
	
}

function precessReadoutInfo(pointInfo, imagePtInPt, currentCoordinates, plot, spt, isLocked) {




	var coordinatesInPt = pointInfo.coordinates;
	var mouseReadouts = pointInfo.mouseReadouts;


	var i;
	var {width:screenW, height:screenH }= plot.screenSize;
	if (spt && (spt.x < 0 || spt.x > screenW || spt.y < 0 || spt.y > screenH)) {
		if (isLocked) {
			for (i = 0; i < 3; i++) {
				//convert the existing readouts to the newly changed coordinates
				if (currentCoordinates[i] != coordinatesInPt[i]) {
					mouseReadouts[i] = getSingleMouseReadout(plot, imagePtInPt, currentCoordinates[i]);
				}
			}
		}
		else {

			for (i = 0; i < 3; i++) {
				mouseReadouts[i] = EMPTY_READOUT;
			}
		}
	}

	var mouseReadout1 = mouseReadouts[0];
	var mouseReadout2 = mouseReadouts[1];
	var pixelSize = mouseReadouts[2];
	return {mouseReadout1, mouseReadout2, pixelSize};

}

function getFluxLabels(plotView) {

	var plot = primePlot(plotView);
	if (!plot) return EMPTY;
	var bands = plot.plotState.getBands();
	var fluxLabels = [];
	for (var i = 0; i < 3; i++) {
		if (i === 0 || bands.length === 3) {
			fluxLabels[i] = showSingleBandFluxLabel(plot, bands[i]);
		}
		else {
			fluxLabels[i] = EMPTY_READOUT;
		}
	}
	return fluxLabels;

}
function showSingleBandFluxLabel(plot, band) {

	if (!plot) return EMPTY_READOUT;

	var webFitsData = plot.webFitsData;
	if (!band) return EMPTY_READOUT;
	var fluxUnits = webFitsData[band.value].fluxUnits;

	var start;
	switch (band) {
		case Band.RED :
			start = 'Red ';
			break;
		case Band.GREEN :
			start = 'Green ';
			break;
		case Band.BLUE :
			start = 'Blue ';
			break;
		case Band.NO_BAND :
			start = '';
			break;
		default :
			start = '';
			break;
	}
	var valStr = start.length > 0 ? 'Val: ' : 'Value: ';

	var fluxUnitInUpperCase = fluxUnits.toUpperCase();
	if (fluxUnitInUpperCase === 'DN' || fluxUnitInUpperCase === 'FRAMES' || fluxUnitInUpperCase === '') {
		return start + valStr;
	}
	else {
		return start + 'Flux: ';
	}

}


/**
 *
 * This method map the value in coordinate option popup to its value
 * @param coordinateRadioValue : the value in the radio button
 * @returns {{coordinate: *, type: *}}
 */
function getCoordinateMap(coordinateRadioValue) {
	var coordinate;
	var type;
	if (coordinateRadioValue === 'eqj2000hms') {
		coordinate = CoordinateSys.EQ_J2000;
		type = 'hms';
	}
	else if (coordinateRadioValue === 'eqj2000DCM') {
		coordinate = CoordinateSys.EQ_J2000;
		type = 'decimal';
	}
	else {
		coordinate = coordinateMap[coordinateRadioValue];
		//if coordinate is not define, assign it as below
		if (!coordinate) coordinate = CoordinateSys.UNDEFINED;
	}
	return {coordinate, type};
}


function getAllMouseReadouts(plotView, mouseState, visRoot){
	var results = [];

	var readoutValues = [visRoot.mouseReadout1, visRoot.mouseReadout2, visRoot.pixelSize];

	var plot = primePlot(plotView);
	for (var i = 0; i < readoutValues.length; i++) {

		results[i] = getSingleMouseReadout(plot, mouseState.imagePt, readoutValues[i]);
	}
	return  {coordinates: readoutValues, mouseReadouts: results};
}

function getSingleMouseReadout(plot, imagePt, toCoordinateName) {
	if (!imagePt) return;
	if (toCoordinateName === 'fitsIP') {
		return ` ${numeral(imagePt.x).format(precision1Digit)}, ${
			numeral(imagePt.y).format(precision1Digit)}`;
	}
	var cc = CysConverter.make(plot);
	var wpt = cc.getWorldCoords(imagePt);
	if (!wpt) return;
	var result;
	var {coordinate, type} =getCoordinateMap(toCoordinateName);

	if (coordinate) {
		var ptInCoord = VisUtil.convert(wpt, coordinate);

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
					var dmsLon = CoordUtil.convertStringToLon(hmsLon, coordinate);
					var dmsLat = CoordUtil.convertStringToLat(hmsLat, coordinate);
					result = ` ${numeral(Number(dmsLon)).format(precision7Digit)}, ${numeral(Number(dmsLat)).format(precision7Digit)}`;
				}
				break;
			case CoordinateSys.GALACTIC:
			case CoordinateSys.SUPERGALACTIC:

				var lonShort = numeral(lon).format(precision7Digit);
				var latShort = numeral(lat).format(precision7Digit);
				result = ` ${lonShort}, ${latShort}`;
				break;
			case CoordinateSys.EQ_B1950:
				result = ` ${hmsLon}, ${hmsLat}`;
				break;

			case CoordinateSys.PIXEL:
				var pt = plot.projection.getPixelScaleArcSec();
				result = `  ${numeral(pt).format(precision3Digit)}"`;
				break;
			case CoordinateSys.SCREEN_PIXEL:
				var size = plot.projection.getPixelScaleArcSec() / plot.zoomFactor;
				result = `  ${numeral(size).format(precision3Digit)}"`;
				break;

			default:
				result = '';
				break;
		}

	}


	return result;

}

function showDialog(fieldKey, radioValue) {

	showMouseReadoutOptionDialog(fieldKey, radioValue);

}

