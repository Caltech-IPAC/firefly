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
import PlotState,{Operation} from '../visualize/PlotState.js';
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


function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Fits Download Dialog'} >
                    <FitsDialogTest groupKey={'FITS_DOWNLOAD_FORM'} />
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('fitsDownloadDialog', popup);
        }
        return popup;
    };
}

const fitsDialogBuilder= getDialogBuilder();

export function showFitsDownloadDialog() {
    fitsDialogBuilder();
    AppDataCntlr.showDialog('fitsDownloadDialog');
}


/// Fits dialog test

var FitsDialogTest= React.createClass({


    propTypes: {

        band: React.PropTypes.object,
        colorName: React.PropTypes.string,
        hasThreeColorBand: React.PropTypes.bool.isRequired,
        hasOperation: React.PropTypes.bool.isRequired,
    },

    getDefaultProps: function() {
        //return { plotState: 'undefined', fileType:'fits',hasThreeColorBand:true,hasOperation:false, band: Band.NO_BAND,  operationType: 'original'};
        return { hasThreeColorBand:false,colorName:'undefined', hasOperation:false, };

    },

	/**
     * This method will find out if the plot has three band color, and if it has operations such as crop or rotate.  It will determine the color
     * band if it has three color band.
     * @returns {{plotState: (*|string|PlotState), band: *, operation: Array, colorName: *, hasOperation: boolean, hasThreeColorBand: boolean}}
     */
    getInitialState() {

        //plot and plotState are global variable for this class
        this.plot =PlotViewUtil.getActivePlotView().primaryPlot;
        this.plotState = this.plot.plotState;

        var threeColorBandUsed=false;
        var color;
        var band;
        if (this.plotState.isThreeColor()) {
            threeColorBandUsed==true;
            var bands = this.plotState.getBands();
            band=bands[0];
            var colorID=bands[0].toString();
            color = Band.valueOf()[colorID].name();

        }



        var isCrop = this.plotState.hasOperation(Operation.CROP);
        var isRotation = this.plotState.hasOperation(Operation.ROTATE);
        var cropNotRotate =  isCrop && !isRotation ? true: false;
        var operationType = 'original';
        if (isCrop ||isRotation){
            operationType='modified';
        }


        return {

            band:band,
            colorName: color,
            hasThreeColorBand:threeColorBandUsed,
            hasOperation: cropNotRotate,

        };

    },


    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    },


    componentDidMount() {
        this.unbinder= FieldGroupUtils.bindToStore('FITS_DOWNLOAD_FORM', (fields) => this.setState({fields}));

    },

    //return the field and value as an object literals
    _showResults(success, request) {


        console.log(request);

        var rel={};
        var s= Object.keys(request).reduce(function(buildString,k,idx,array){
            rel[k]=request[k];
            if (idx<array.length-1)  return;

        },'');

        return rel;
    },


     // download the fits,png and reg files
     _doFileDownload(request) {

         var rel = this._showResults(true,request);
         var ext;
         var bandSelect=null;
         var whichOp=null;
         for (var key in rel){
             var value=rel[key];
             if (key=='fileType'){
                 ext = value;

             }
             if (key == 'threeBandColor'){
                 bandSelect=value;
             }
             if (key == 'operationOption'){
                 whichOp=value;
             }
         }

         var band = Band.NO_BAND;
         if (bandSelect != null) {
             band = Band[bandSelect];
         }


         var fitsFile = this.plotState.getOriginalFitsFileStr(band) == 'undefined' || whichOp == null ?
         this.plotState.getWorkingFitsFileStr(band) :
         this.plotState.getOriginalFitsFileStr(band);

         if (ext.toLowerCase() =='fits') {

             download(getRootURL() + '/servlet/Download?file='+ fitsFile);
         }

         //this does not work, how to convert a fits to a png image??
        /* else if (ext.toLowerCase() =='png') {
             var indx = fitsFile.indexOf('fits');
             var pngFile =fitsFile.substr(0, indx) + 'png';

             download(getRootURL() + '/servlet/Download?png='+ pngFile);
         }*/

    },



    render() {

        var fitsFileRadioGroup=<br/>;
       // if (this.plotState.hasOperation(Operation.CROP.name) && !this.plotState.hasOperation(Operation.ROTATE.name) ){
        if(this.props.hasOperation){

            fitsFileRadioGroup = (
                <div>
                    <br/>
                    <RadioGroupInputField initialState={{
                           tooltip: 'Please select an option',
                           label : 'FITS file:'

                           }}
                                          options={
                                   [
                                      { label:'original', value:'fileTypeOpt1'},
                                      { label:'crop', value:'fileTypeOpt2'},

                                   ]}
                                          alignment={'vertical'}
                                          fieldKey='operationOption'
                                          groupKey='FITS_DOWNLOAD_FORM'/>
                </div>
            );

        }
        var colorRadioGroup=<br/>;
        if(this.props.hasThreeColorBand){
            colorRadioGroup = (
                <div>
                    <br/>
                    <RadioGroupInputField initialState={{
                                          tooltip: 'Please select an option',
                                          label : 'Color band:'

                                          }}
                                          options={
                                                     [
                                                       { label: this.props.band[this.props.colorName].value, value:'colorOpt1'},

                                                     ]}
                                          fieldKey='threeBandColor'
                                          groupKey='FITS_DOWNLOAD_FORM'/>
                </div>
            );
        }

        return (
            <FieldGroup groupKey={'FITS_DOWNLOAD_FORM'}  keepState={true}>
                <div style={{padding:'5px'}}>
                    <div style={{'minWidth': '300', 'minHeight': '100'} }>
                        <InputGroup labelWidth={130} >
                            <PopupPanel groupKey='FITS_DOWNLOAD_FORM' />

                            <RadioGroupInputField  initialState= {{
                                          tooltip: 'Please select an option',
                                          label : 'Type of files:'
                                          }}
                                                   options={
                                                              [
                                                                  {label: 'FITS File', value: 'fits'},
                                                                  {label: 'PNG File', value: 'png' },
                                                                  {label: 'Region File', value: 'reg'},

                                                              ]

                                                              }
                                                   alignment={'vertical'}

                                                   fieldKey='fileType'
                                                   groupKey='FITS_DOWNLOAD_FORM'/>


                            { fitsFileRadioGroup}

                            {colorRadioGroup}

                            <div style={{'text-align':'center'}}>
                                < CompleteButton
                                    groupKey='FITS_DOWNLOAD_FORM'
                                    text='Download'
                                    onSuccess={this. _doFileDownload}
                                    onFail={this.resultsFail}
                                    ialogId='FitsDownloadDialog'
                                />

                                <br/>
                            </div>




                        </InputGroup>


                    </div>

                </div>
            </FieldGroup>
        );
    }
    });
