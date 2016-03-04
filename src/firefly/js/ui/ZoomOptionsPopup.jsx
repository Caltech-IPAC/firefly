/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component, PropTypes} from 'react';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import Validate from '../util/Validate.js';
import ValidationField from './ValidationField.jsx';
import CheckboxGroupInputField from './CheckboxGroupInputField.jsx';
import RadioGroupInputField from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import FieldGroup from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import PopupPanel from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import Band from '../visualize/Band.js';
import {visRoot, dispatchRotate, ActionScope} from '../visualize/ImagePlotCntlr.js';
import {RotateType} from '../visualize/PlotChangeTask.js';
import {makeZoomLevelMatcher} from '../visualize/ZoomUtil';

function getDialogBuilder() {
    var popup = null;
    return () => {
        if (!popup) {
            const popup = (
                <PopupPanel title={'Zoom Level'}>
                    <ZoomOptionsPopup groupKey={'ZOOM_OPTIONS_FORM'}/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('zoomOptionsPopup', popup);
        }
        return popup;
    };
}

const dialogBuilder = getDialogBuilder();

export function showZoomOptionsPopup() {
    dialogBuilder();
    AppDataCntlr.showDialog('zoomOptionsPopup');
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
        plot,
        colors,
        hasThreeColorBand: threeColorBandUsed,
        hasOperation: cropNotRotate
    };

}

function renderOperationOption(hasOperation) {

    var leftColumn = { display: 'inline-block', paddingLeft:135, paddingBottom:15, verticalAlign:'middle'};
    var rightColumn = {display: 'inline-block', paddingLeft:20};

    if (hasOperation) {
        return (
            <div  style={{ minWidth : 300, minHeight: 100} }>
                <div title = 'Please select an option'  style={leftColumn}>FITS file: </div>
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

function renderThreeBand(hasThreeColorBand, colors) {

    var rightColumn={display: 'inline-block', paddingLeft:18};
    var leftColumn;



    if (hasThreeColorBand) {
        switch (colors.length){
            case 1:
                leftColumn= { display: 'inline-block', paddingLeft:125};
                break;
            case 2:
                leftColumn = { display: 'inline-block', paddingLeft:125, verticalAlign: 'middle', paddingBottom:20};
                break;
            case 3:
                leftColumn ={ display: 'inline-block', paddingLeft:125,verticalAlign: 'middle', paddingBottom:40};
                break;
        }

        var optionArray=[];
        for (var i=0; i<colors.length; i++){
            optionArray[i]={label: colors[i], value: colors[i]+'Radio'};
        }

        return (
            <div  style={{ minWidth:300, minHeight: 100} }>

                <div title ='Please select an option' style={leftColumn}>Color Band:   </div>

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

class ZoomOptionsPopup extends React.Component {

    constructor(props)  {
        super(props);
        FieldGroupUtils.initFieldGroup('ZOOM_OPTIONS_FORM');
        this.state = {fields:FieldGroupUtils.getGroupFields('ZOOM_OPTIONS_FORM')};
    }

    componentWillUnmount() {

        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {

        this.unbinder = FieldGroupUtils.bindToStore('ZOOM_OPTIONS_FORM', (fields) => {
            this.setState({fields});
        });
    }


    render() {

        var {fields}= this.state;
        if (!fields) return false;
        return <ZoomOptionsPopupForm  />;
    }


}


function ZoomOptionsPopupForm() {

    const { plot, colors, hasThreeColorBand,hasOperation} = getInitialPlotState();

    var renderOperationButtons = renderOperationOption(hasOperation);

    var renderThreeBandButtons = renderThreeBand(hasThreeColorBand, colors);//true, ['Green','Red', 'Blue']

    var inputfield = {display: 'inline-block', paddingTop:40, paddingLeft:40, verticalAlign:'middle', paddingBottom:30};

        return (

            <FieldGroup groupKey='ZOOM_OPTIONS_FORM' keepState={true}>
                    <div style={inputfield}>
                        <ValidationField fieldKey='rotation'
                             initialState= {{
                                   fieldKey: 'rotation',
                                   value: '',
                                   validator: Validate.floatRange.bind(null, 0.0, 360.0, 2, 'rotation angle'),
                                   tooltip: 'enter the angle between 0 and 360',
                                   label: 'Rotation Angle:',
                                   labelWidth: 100
                             }} />
                    </div>

                    <div style={{paddingLeft:40,marginBottom: 20}}>
                        <CheckboxGroupInputField
                            initialState= {{
                                  tooltip: 'Apply rotation to all related images',
                                  label : 'Apply rotation to all related images:',
                                  value: ''
                              }}
                            options={
                                      [
                                          {label: '', value: 'True'}
                                      ]
                                      }
                            fieldKey='checkAllimage'
                        />
                    </div>

                    <div style={{'textAlign':'center', marginBottom: 20}}>
                        < CompleteButton
                            text='OK'  groupKey='ZOOM_OPTIONS_FORM'
                            onSuccess={resultsOK}
                            onFail={resultsFail}
                            dialogId='zoomOptions'
                        />
                    </div>
            </FieldGroup>
        );

}

/*function showResults(success, request) {
    var statStr= `validate state: ${success}`;
    console.log(statStr);
    console.log(request);

    var s= Object.keys(request).reduce(function(buildString,k,idx,array){
        buildString+=`${k}=${request[k]}`;
        if (idx<array.length-1) buildString+=', ';
        return buildString;
    },'');


    var resolver= null;
    var closePromise= new Promise(function(resolve, reject) {
        resolver= resolve;
    });

    var results= (
        <PopupPanel title={'Rotation Dialog Results'} closePromise={closePromise} >
            {makeResultInfoContent(statStr,s,resolver)}
        </PopupPanel>
    );

    DialogRootContainer.defineDialog('ResultsFromRotationDialog', results);
    AppDataCntlr.showDialog('ResultsFromRotationDialog');

}


function makeResultInfoContent(statStr,s,closePromiseClick) {
    return (
        <div style={{padding:'10px'}}>
            <br/>{statStr}<br/><br/>{s}
            <CompleteButton dialogId='ResultsFromZoomOptionsPopup.' />
        </div>
    );
}

function resultsSuccess(request) {
    showResults(true,request);
}
*/
function resultsOK(request) {
    console.log(request + 'You clicked OK ');
}

function resultsFail(request) {
    console.log(request + ': Error');
}
