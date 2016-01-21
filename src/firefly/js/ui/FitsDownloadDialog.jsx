

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

/**
 * This method is called when the dialog is rendered. Only when an image is loaded, the PlotView is available.
 * Then, the color band, plotState etc can be determined.
 * @returns {{plotState, colors: Array, hasThreeColorBand: boolean, hasOperation: boolean}}
 */
function getInitialPlotState() {

    var plot = PlotViewUtil.getActivePlotView(visRoot()).primaryPlot;

    var plotState = plot.plotState;

    if (plotState.isThreeColor()) {
        var threeColorBandUsed = true;

        var bands = this.plotState.getBands();//array of Band

        if (bands != Band.NO_BAND) {
            var colors = [];
            for (var i=0; i<bands.length; i++) {
                switch (bands[i]){
                    case Band.RED:
                        colors[i] = 'Red';
                        break;
                    case Band.GREEN:
                        colors[i] = 'Green';
                        break;
                    case Band.BLUE:
                        colors[i] = 'Blue';
                        break;
                    default:
                }        break;

            }

        }
    }


    var isCrop = plotState.hasOperation(Operation.CROP);
    var isRotation = plotState.hasOperation(Operation.ROTATE);
    var cropNotRotate = isCrop && !isRotation ? true : false;

    return {
        plotState,
        colors,
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
                                    tooltip: 'Please select an option'
                                    //move the label as InputFieldLabel above
                                   }}
                        options={[
                            { label:'Original', value:'fileTypeOrig'},

                            { label:'Cropped', value:'fileTypeCrop'}

                            ]}
                        alignment={'vertical'}
                        fieldKey='operationOption'

                    />
                </div>
            </div>
        );
    }
    else {
        return <br/>;
    }
}

function renderThreeBand(hasThreeColorBand, colors,leftColumn, rightColumn) {
    if (hasThreeColorBand) {
        var optionArray=[];
        for (var i=0; i<colors.length; i++){
            optionArray[i]={label: colors[i], value: colors[i]+'Radio'};
        }

        return (
            <div >

                <div style={leftColumn}>
                    <InputFieldLabel label= 'Color Band:'
                                     tooltip='Please select an option'
                    />

                </div>
                <div style={rightColumn}>
                    <RadioGroupInputField
                        initialState={{
                                    tooltip: 'Please select an option'
                                     //move the label as InputFieldLabel above
                                     }}
                        options={optionArray}

                        alignment={'vertical'}
                        fieldKey='threeBandColor'
                    />
                </div>

            </div>
        );
    }
    else {
        return <br/>;
    }
}

function FitsDownloadDialogForm() {


    const { plotState, colors, hasThreeColorBand, hasOperation} = getInitialPlotState();
    var leftColumn = {width: 200, display: 'inline-block'};

    var rightColumn = {display: 'inline-block'};

    var renderOperationButtons = renderOperationOption(hasOperation, leftColumn, rightColumn);

    var renderThreeBandButtons = renderThreeBand(hasThreeColorBand, colors, leftColumn , rightColumn);

    //leftColumn['lineHeight']=100;//change the line height for the Fits radio button group

    return (
        <FieldGroup groupKey='FITS_DOWNLOAD_FORM' keepState={true}>
            <div style={{ padding:5 }}>
                <div style={{ minWidth : 300, minHeight: 100 } }>
                    <InputGroup labelWidth={130}>
                        <PopupPanel  />

                        <div style={leftColumn}>
                            <div style={{float:'right', paddingRight:19, paddingBottom:20}}
                                title='Please select an option'>
                                Type of files:
                            </div>
                        </div>
                        <div style={rightColumn}>
                            <RadioGroupInputField
                                initialState={{
                                    tooltip: 'Please select an option'
                                    //move the label as a InputFieldLabel
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

                    {renderThreeBandButtons}
                </div>
                <div style={{'text-align':'center'}}>
                    < CompleteButton
                        text='Download'
                        onSuccess={ (request) => resultsSuccess(request, plotState )}
                        onFail={resultsFail}
                        dialogId='fitsDownloadDialog'
                    />
                </div>
            </div>
        </FieldGroup>
    );

}


function resultsFail(request) {
    console.log(request + ': Error');
}
/**
 * This function process the request
 * @param request
 * @param plotState
 */
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

