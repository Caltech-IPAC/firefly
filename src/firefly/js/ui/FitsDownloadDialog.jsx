/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 *   Dec. 2015
 *   propType: define all the property variable for the component
 *   this.plot, this.plotSate are the class global variables
 *
 */
import React from 'react';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {download} from '../util/WebUtil.js';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {Band} from '../visualize/Band.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {encodeUrl, ParamType}  from '../util/WebUtil.js';
import {RequestType} from '../visualize/RequestType.js';
import {ServiceType} from '../visualize/WebPlotRequest.js';

import HelpIcon from './HelpIcon.jsx';


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
    dispatchShowDialog('fitsDownloadDialog');
}

/**
 * This method is called when the dialog is rendered. Only when an image is loaded, the PlotView is available.
 * Then, the color band, plotState etc can be determined.
 * @returns {{plotState, colors: Array, hasThreeColorBand: boolean, hasOperation: boolean}}
 */
function getInitialPlotState() {

    var plot = primePlot(visRoot());


    var plotState = plot.plotState;

    if (plotState.isThreeColor()) {
        var threeColorBandUsed = true;

        var bands = plotState.getBands();//array of Band

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
        plot,
        colors,
        hasThreeColorBand: threeColorBandUsed,
        hasOperation: cropNotRotate
    };

}


class FitsDownloadDialog extends React.Component {

    constructor(props) {
        super(props);
        this.state = {fields: FieldGroupUtils.getGroupFields('FITS_DOWNLOAD_FORM')};

    }


    componentWillUnmount() {

        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {

        this.iAmMounted= true;
        this.unbinder = FieldGroupUtils.bindToStore('FITS_DOWNLOAD_FORM', (fields) => {
            if (this.iAmMounted) this.setState({fields});
        });
    }


    render() {
        return <FitsDownloadDialogForm  />;
    }


}


function renderOperationOption(hasOperation) {

    var leftColumn = { display: 'inline-block', paddingLeft:85, paddingBottom:15, verticalAlign:'middle'};
    var rightColumn = {display: 'inline-block', paddingLeft:20};

    if (hasOperation) {
        return (
            <div  style={{ minWidth : 300, minHeight: 100} }>
                <div title = 'Please select an option'  style={leftColumn}>FITS file: </div>
                <div style={rightColumn}>
                    <RadioGroupInputField
                        labelWidth={0}
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

function renderThreeBand(hasThreeColorBand, colors) {

    var rightColumn={display: 'inline-block', paddingLeft:18};
    var leftColumn;


    if (hasThreeColorBand) {
        switch (colors.length){
            case 1:
                leftColumn= { display: 'inline-block', paddingLeft:75};
                break;
            case 2:
                leftColumn = { display: 'inline-block', paddingLeft:75, verticalAlign: 'middle', paddingBottom:20};
                break;
            case 3:
                leftColumn ={ display: 'inline-block', paddingLeft:75,verticalAlign: 'middle', paddingBottom:40};
                break;
        }

        var optionArray=[];
        for (var i=0; i<colors.length; i++){
            optionArray[i]={label: colors[i], value: colors[i]+'Radio'};
        }

        return (
             <div  style={{ minWidth:300, minHeight: 100} }>
                <div title ='Please select an option' style={leftColumn}> Color Band:   </div>
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


    const { plot, colors, hasThreeColorBand, hasOperation} = getInitialPlotState();

    var renderOperationButtons = renderOperationOption(hasOperation);

    var renderThreeBandButtons = renderThreeBand(hasThreeColorBand, colors);//true, ['Green','Red', 'Blue']);


    var leftColumn = { display: 'inline-block', paddingLeft:75, verticalAlign:'middle', paddingBottom:30};

    var rightColumn = {display: 'inline-block',  paddingLeft:18};

	var dialogStyle = { minWidth : 300, minHeight: 100 , padding:'15px 5px 5px 5px'};
    return (

        <FieldGroup groupKey='FITS_DOWNLOAD_FORM' keepState={true}>
                <div style={ dialogStyle}>
                        <div style={leftColumn}  title='Please select an option'> Type of files:  </div>
                        <div style={rightColumn}>
                            <RadioGroupInputField
                                initialState={{
                                    tooltip: 'Please select an option',
                                    value : 'fits'
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



                </div>
                <div>
                    {renderOperationButtons}

                    {renderThreeBandButtons}
                </div>
                <table style={{width:300}}>
                    <colgroup>
                        <col style={{width: '20%'}} />
                        <col style={{width: '60%'}} />
                        <col style={{width: '20%'}} />
                    </colgroup>
                    <tbody>
                    <tr>
                        <td></td>
                        <td>
                            <div style={{'textAlign':'center', marginBottom: 20}}>
                                < CompleteButton
                                    text='Download'
                                    onSuccess={ (request) => resultsSuccess(request, plot )}
                                    onFail={resultsFail}
                                    dialogId='fitsDownloadDialog'
                                />
                            </div>
                        </td>
                        <td>
                            <div style={{ textAlign:'center', marginBottom: 20}}>
                               <HelpIcon helpid={'visualization.fitsDownloadOptions'} />
                            </div>
                        </td>
                    </tr>
                    </tbody>
                 </table>
        </FieldGroup>
    );

}


function resultsFail(request) {
    console.log(request + ': Error');
}
/**
 * This function process the request
 * @param request
 * @param plot
 */
function resultsSuccess(request, plot) {
    // var rel = showResults(true, request);

    var plotState = plot.plotState;

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
        var param={file: fitsFile, return:makeFileName(plot, band), log: true};
        var  url = encodeUrl(getRootURL() + '/servlet/Download', param);
        //download(getRootURL() + '/servlet/Download?file=' + fitsFile);
        download(url);
    }

}

function  makeFileName(plot,  band) {


    var plotState = plot.plotState;
    var req= plotState.getWebPlotRequest(band);

    if (req.getDownloadFileNameRoot()) {
        return req.getDownloadFileNameRoot()+'.fits';
    }


   var rType= req.getRequestType();

    var retval;
    switch (rType) {
        case RequestType.SERVICE:
            retval= makeServiceFileName(req,plot, band);
            break;
        case RequestType.FILE:
            retval= 'USE_SERVER_NAME';
            break;
        case RequestType.URL:
            retval= makeTitleFileName(band);
            break;
        case RequestType.ALL_SKY:
            retval= 'allsky.fits';
            break;
        case RequestType.BLANK:
            retval= 'blank.fits';
            break;
        case RequestType.PROCESSOR:
            retval= makeTitleFileName(plot, band);
            break;
        case RequestType.RAWDATASET_PROCESSOR:
            retval= makeTitleFileName(plot, band);
            break;
        case RequestType.TRY_FILE_THEN_URL:
            retval= makeTitleFileName(plot, band);
            break;
        default:
            retval= makeTitleFileName(plot, band);
            break;

    }
    return retval;
}

function  makeServiceFileName(req,plot, band) {

    var sType= req.getServiceType();
    var retval;
    switch (sType) {
        case ServiceType.IRIS:
            retval= 'iris-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.ISSA:
            retval= 'issa-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.DSS:
            retval= 'dss-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.SDSS:
            retval= 'sdss-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.TWOMASS:
            retval= 'twomass-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.MSX:
            retval= 'msx-'+req.getSurveyKey()+'.fits';
            break;
        case ServiceType.DSS_OR_IRIS:
            retval= 'fits-'+req.getSurveyBand()+'.fits';
            break;
        case ServiceType.WISE:
            retval= 'wise-'+req.getSurveyKey()+'-'+req.getSurveyBand()+'.fits';
            break;
        case ServiceType.NONE:
            retval= makeTitleFileName(plot, band);
            break;
        default:
            retval= makeTitleFileName(plot, band);
            break;
    }
    return retval;
}
function  makeTitleFileName(plot, band) {


    var retval = plot.title;
    if (band!=Band.NO_BAND) {
        retval= retval + '-'+ band;
    }
    retval= getHyphenatedName(retval);

    return retval +  '.fits';
}
/**
 * This method split a string by ':' and white spaces.
 * After the str is spitted to an array, reconnect the array to a string
 * using '-'.
 *
 * @param str: input string
 * @returns {T} a string by replace ':' and white spaces by '-' in the input str.
 */
function getHyphenatedName(str){

	//filter(Boolean) will only keep the truthy values in the array.
    var sArray=str.split(/[ :]+/).filter(Boolean);

    var fName=sArray[0];
    for(var i=1; i<sArray.length; i++){
        fName=fName+'-'+sArray[i];
    }
    return fName;
}