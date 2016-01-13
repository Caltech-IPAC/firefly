/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   Dec. 2015
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React from 'react';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {download} from '../util/WebUtil.js';
import InputGroup from './InputGroup.jsx';
import RadioGroupInputField from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import FieldGroup from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import PopupPanel from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import PlotViewUtil from '../visualize/PlotViewUtil.js';
import Band from '../visualize/Band.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import InputFieldLabel from './InputFieldLabel.jsx';

function getDialogBuilder() {
    var popup = null;
    return () => {
        if (!popup) {
            const popup = (
                <PopupPanel title={'Fits Download Dialog'}>
                    <FitsDownloadDialog groupKey={'FITS_DOWNLOAD_FORM'}/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('fitsDownloadDialog', popup);
        }
        return popup;
    };
}

const dialogBuilder = getDialogBuilder();

export function showFitsDownloadDialog() {
    dialogBuilder();
    AppDataCntlr.showDialog('fitsDownloadDialog');
}


function getInitialPlotState() {

    var plot = PlotViewUtil.getActivePlotView(visRoot()).primaryPlot;

    var plotState = plot.plotState;

   // var threeColorBandUsed = false;

    //var color;
    if (plotState.isThreeColor()) {
        var threeColorBandUsed = true;
        var bands = this.plotState.getBands();
        var colorID = bands[0].toString();
        var color = Band.valueOf()[colorID].name();

    }


    var isCrop = plotState.hasOperation(Operation.CROP);
    var isRotation = plotState.hasOperation(Operation.ROTATE);
    var cropNotRotate = isCrop && !isRotation ? true : false;

    return {
        plotState,
        color,
        hasThreeColorBand: threeColorBandUsed,
        hasOperation: cropNotRotate
    };

}


class FitsDownloadDialog extends React.Component {

    constructor(props) {
        super(props);
        FieldGroupUtils.initFieldGroup('FITS_DOWNLOAD_FORM');
        this.state = {fields: FieldGroupUtils.getGroupFields('FITS_DOWNLOAD_FORM')};

    }


    componentWillUnmount() {

        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {

        this.unbinder = FieldGroupUtils.bindToStore('FITS_DOWNLOAD_FORM', (fields) => {
            this.setState({fields});
        });
    }


    render() {

        var {fields}= this.state;
        if (!fields) return false;
			return <FitsDownloadDialogForm  />;
		}


}
/// Fits dialog test

function renderOperationOption(hasOperation,leftColumn, rightColumn) {

    if (hasOperation) {
        return (
            <div>
                <div style={leftColumn}>
                    <InputFieldLabel label= 'FITS file:'
                                     tooltip='Please select an option'
                   />

                </div>
                <div style={rightColumn}>
                    <RadioGroupInputField
                        initialState={{
                                    tooltip: 'Please select an option',
                                   }}
                    options={[
                            { label:'Original', value:'fileTypeOrig'},

                            { label:'Cropped', value:'fileTypeCrop'}

                            ]}
                    alignment={'vertical'}
                    fieldKey='operationOption'

                    />
                </div>
                <br/>
            </div>
        );
    }
    else {
        return <br/>;
    }
}

function renderThreeBand(hasThreeColorBand, color,leftColumn, rightColumn) {
    if (hasThreeColorBand) {
        return (
            <div >

                <div style={{width: '50%', display: 'inline-block', marginLeft:10}}>
                    <RadioGroupInputField
                        initialState={{
                                    tooltip: 'Please select an option',
                                    label: 'Color Band:'
                                     }}

                      options={[
                           {  label: color, value: 'colorOpt'},


                         ]}

                     fieldKey='threeBandColor'

                   />
                </div>
                <br/>
            </div>

        );
    }
    else {
        return <br/>;
    }
}

function FitsDownloadDialogForm() {


    const { plotState, color, hasThreeColorBand, hasOperation} = getInitialPlotState();
    var leftColumnRoot = {width: '50%', float: 'left', 'text-align': 'center', 'vertical-align': 'middle',
        display: 'inline-block', 'line-height': 80};

    var rightColumn = {width: '50%', display: 'inline-block'};


    var renderOperationButtons = renderOperationOption(true, leftColumnRoot, rightColumn);//hasOperation

    var lc1 = Object.assign({}, leftColumnRoot);
    lc1['line-height']=10;
    lc1['margin-left']=30;
    var renderThreeBandButtons = renderThreeBand(true, 'Green', lc1 , rightColumn);//hasThreeColorBand, color,

    var leftColumn = Object.assign({}, leftColumnRoot);
    leftColumn['line-height']=100;

    return (
        <FieldGroup groupKey='FITS_DOWNLOAD_FORM' keepState={true}>
            <div style={{ padding:5 }}>
                <div style={{ minWidth : 300, minHeight: 100 } }>
                    <InputGroup labelWidth={130}>
                        <PopupPanel  />

						<div style={leftColumn}>

                            <InputFieldLabel label= 'Type of files:'
                                             tooltip='Please select an option'

                            />
                        </div>
                        <div style={rightColumn}>
                           <RadioGroupInputField
                               initialState={{
                                    tooltip: 'Please select an option',

                                   }}
                            options={ [
                                      {label: 'FITS File', value: 'fits'},
                                      {label: 'PNG File', value: 'png' },
                                       {label: 'Region File', value: 'reg'}
                                    ]}
                            alignment={'vertical'}
                            fieldKey='fileType'
                          />
                      </div>

                   </InputGroup>

                </div>
                <div>
                    {renderOperationButtons}

                    <div>{renderThreeBandButtons}</div>
               </div>
                <br/>
                <div style={{'text-align':'center'}}>
                    < CompleteButton
                        text='Download'
                        onSuccess={ (request) => resultsSuccess(request, plotState )}
                        onFail={resultsFail}
                        dialogId='fitsDownloadDialog'
                    />
                    <br/>
                </div>
            </div>
        </FieldGroup>
    );

}

/*
function showResults(success, request) {

    var rel = {};
    console.log(request);

    if (success) {
        Object.keys(request).forEach(function (key) {
            rel[key] = request[key];
        });
    }

    return rel;
}
*/

function resultsFail(request) {
    console.log(request + ': Error');
}

function resultsSuccess(request, plotState) {
   // var rel = showResults(true, request);

    if (!Object.keys(request).length) {
        console.log(request);
        return resultsFail(request);
    }
    var ext;
    var bandSelect;
    var whichOp;
    Object.keys(request).forEach(function (key) {
        var value = request[key];
        if (key === 'fileType') {
            ext = value;
        }
        if (key === 'threeBandColor') {
            bandSelect = value;
        }
        if (key === 'operationOption') {
            whichOp = value;
        }
    });

    var band = Band.NO_BAND;
    if (bandSelect) {
        band = Band[bandSelect];
    }


    var fitsFile = !plotState.getOriginalFitsFileStr(band) || !whichOp ?
        plotState.getWorkingFitsFileStr(band) :
        plotState.getOriginalFitsFileStr(band);

    if (ext && ext.toLowerCase() == 'fits') {

        download(getRootURL() + '/servlet/Download?file=' + fitsFile);
    }

}

