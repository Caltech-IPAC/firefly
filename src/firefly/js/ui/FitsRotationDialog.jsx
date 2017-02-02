/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component} from 'react';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import Validate from '../util/Validate.js';
import {ValidationField} from './ValidationField.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {showInfoPopup} from './PopupUtil.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchRotate, ActionScope} from '../visualize/ImagePlotCntlr.js';
import {RotateType} from '../visualize/PlotState.js';
import {isOverlayLayersActive} from '../visualize/RelatedDataUtil.js';

import HelpIcon from './HelpIcon.jsx';

function getDialogBuilder() {
    var popup = null;
    return () => {
        if (!popup) {
            const popup = (
                <PopupPanel title={'Rotate Image'}>
                    <FitsRotationDialog groupKey={'FITS_ROTATION_FORM'}/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('fitsRotationDialog', popup);
        }
        return popup;
    };
}

const dialogBuilder = getDialogBuilder();

export function showFitsRotationDialog() {
    if (isOverlayLayersActive(visRoot())) {
       showInfoPopup('Rotate not yet supported with mask layers');
    }
    else {
        dialogBuilder();
        dispatchShowDialog('fitsRotationDialog');
    }
}


/**
 * This method is called when the dialog is rendered. Only when an image is loaded, the PlotView is available.
 * Then, the color band, plotState etc can be determined.
 * @returns {{plotState, colors: Array, hasThreeColorBand: boolean, hasOperation: boolean}}
 */
function getInitialPlotState() {

    var plot = primePlot(visRoot());


    var plotState = plot.plotState;


    var isCrop = plotState.hasOperation(Operation.CROP);
    var isRotation = plotState.hasOperation(Operation.ROTATE);
    var cropNotRotate = isCrop && !isRotation ? true : false;

    return {
        plot,
        colors: [],
        hasThreeColorBand: false,
        hasOperation: cropNotRotate
    };

}



class FitsRotationDialog extends Component {

    constructor(props)  {
        super(props);
        this.state = {fields:FieldGroupUtils.getGroupFields('FITS_ROTATION_FORM')};
    }

    componentWillUnmount() {

        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {

        this.iAmMounted= true;
        this.unbinder = FieldGroupUtils.bindToStore('FITS_ROTATION_FORM', (fields) => {
            if (this.iAmMounted) this.setState({fields});
        });
    }


    render() {
        return <FitsRotationDialogForm  />;
    }


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

function FitsRotationDialogForm() {

    const { plot, colors, hasThreeColorBand,hasOperation} = getInitialPlotState();

    var renderOperationButtons = renderOperationOption(hasOperation);

    var renderThreeBandButtons = renderThreeBand(hasThreeColorBand, colors);//true, ['Green','Red', 'Blue']

    var inputfield = {display: 'inline-block', paddingTop:40, paddingLeft:40, verticalAlign:'middle', paddingBottom:30};

    return (

        <FieldGroup groupKey='FITS_ROTATION_FORM' keepState={true}>
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
                                        text='OK'  groupKey='FITS_ROTATION_FORM'
                                        onSuccess={(request) =>resultsSuccess(request,plot.plotId)}
                                        onFail={resultsFail}
                                        dialogId='fitsRotationDialog'

                                    />
                                </div>
                            </td>
                            <td>
                                <div style={{ textAlign:'center', marginBottom: 20}}>
                                    <HelpIcon helpId={'visualization.imageoptions'} />
                                </div>
                            </td>
                         </tr>
                    </tbody>
                </table>
        </FieldGroup>
    );

}


function resultsSuccess(request,plotId) {
    if (request.rotation) {
        var angle= Number(request.rotation);
        const actionScope= request.checkAllimage ? ActionScope.GROUP : ActionScope.SINGLE;
        const rotateType= angle?RotateType.ANGLE:RotateType.UNROTATE;
        dispatchRotate({plotId, rotateType, angle, actionScope} );
    }
}

function resultsFail(request) {
    console.log(request + ': Error');
}
