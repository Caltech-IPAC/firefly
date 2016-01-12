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

    var threeColorBandUsed = false;

    var color;
    if (plotState.isThreeColor()) {
        threeColorBandUsed == true;
        var bands = this.plotState.getBands();

        var colorID = bands[0].toString();
        color = Band.valueOf()[colorID].name();

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

function renderOperationOption(hasOperation) {

    if (hasOperation) {
        return (
            <div>
                <RadioGroupInputField
                    initialState={{
                           tooltip: 'Please select an option',
                           label : 'FITS file:'
                           }}
                    options={[
                            { label:'original', value:'fileTypeOrig'},
                            { label:'crop', value:'fileTypeCrop'}

                            ]}
                    alignment={'vertical'}
                    fieldKey='operationOption'

                />
                <br/>
            </div>
        );
    }
    else {
        return <br/>;
    }
}

function renderThreeBand(hasThreeColorBand, color) {
    if (hasThreeColorBand) {
        return (
            <div>
                <RadioGroupInputField
                    initialState={{
                      tooltip: 'Please select an option',
                      label : 'Color band:'
                  }}
                    options={[
                           {  label: color,
                              value: 'colorOpt'
                           }
                         ]}
                    alignment={'vertical'}
                    fieldKey='threeBandColor'

                />

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

    var renderOperationButtons = renderOperationOption(hasOperation);
    var renderThreeBandButtons = renderThreeBand(hasThreeColorBand, color);
    return (
        <FieldGroup groupKey='FITS_DOWNLOAD_FORM' keepState={true}>
            <div style={{ padding:5 }}>
                <div style={{ minWidth : 300, minHeight: 100 } }>
                    <InputGroup labelWidth={130}>
                        <PopupPanel  />

                        <RadioGroupInputField

                            initialState={{
                                    tooltip: 'Please select an option',
                                    label : 'Type of files:'
                                   }}

                            options={ [
                                      {label: 'FITS File', value: 'fits'},
                                      {label: 'PNG File', value: 'png' },
                                       {label: 'Region File', value: 'reg'}
                                    ]}
                            alignment={'vertical'}
                            fieldKey='fileType'
                        />

                        {renderOperationButtons}
                        {renderThreeBandButtons}

                        <div style={{'text-align':'center'}}>
                            < CompleteButton
                                groupKey='FITS_DOWNLOAD_FORM'
                                text='Download'
                                onSuccess={ (request) => resultsSuccess(request, plotState )}
                                onFail={resultsFail}
                                dialogId='fitsDownloadDialog'
                            />
                            <br/>
                        </div>
                    </InputGroup>
                </div>
            </div>
        </FieldGroup>
    );

}


function showResults(success, request) {

    var rel = {};
    console.log(request);

    if (success) {


        Object.keys(request).reduce(function (buildString, k, idx, array) {
            if (idx < array.length - 1)  return;
            rel[k] = request[k];

        }, '');
    }
    return rel;
}


function resultsFail(request) {
    showResults(false, request);
}

function resultsSuccess(request, plotState) {
    var rel = showResults(true, request);

    if (!Object.keys(rel).length) {
        console.log(request);
        return resultsFail(request);
    }
    var ext;
    var bandSelect;
    var whichOp;
    Object.keys(rel).forEach(function (key) {
        var value = rel[key];
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

