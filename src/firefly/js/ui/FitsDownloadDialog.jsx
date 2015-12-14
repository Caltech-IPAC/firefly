/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react';
import {flux} from '../Firefly.js';
import TargetPanel from './TargetPanel.jsx';
import InputGroup from './InputGroup.jsx';
import RadioGroupInputField from './RadioGroupInputField.jsx';
import CompleteButton from './CompleteButton.jsx';
import FieldGroup from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import PopupPanel from './PopupPanel.jsx';
import CollapsiblePanel from './panel/CollapsiblePanel.jsx';
import {Tabs, Tab} from './panel/TabPanel.jsx';
import AppDataCntlr from '../core/AppDataCntlr.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import PlotViewUtil from '../visualize/PlotViewUtil.js';
import Band from '../visualize/Band.js';
import {Operation} from '../visualize/PlotState.js';
import PlotState   from '../visualize/PlotState.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {download} from '../util/WebUtil.js';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';


function getDialogBuilder(plot) {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Fits Download Dialog'} >
                    <FitsDialogTest pv={plot} groupKey={'FITS_DOWNLOAD_FORM'} />
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
        plot: React.PropTypes.object.isRequired,
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

        var plot =PlotViewUtil.getActivePlotView().primaryPlot;
        var plotState = plot.plotState;

        var threeColorBandUsed=false;
        var color;
        var band;
        if (plotState.isThreeColor()) {
            threeColorBandUsed==true;
            var bands = plotState.getBands();
            band=bands[0];
            var colorID=bands[0].toString();
            color = Band.valueOf()[colorID].name();
            /*switch(colorID){
             case 0: color='red';
             break;
             case 1: color='green';
             break;
             case 2: color='blue';
             break;
             }*/
        }



       // var operation = plotState.ops;

        var isCrop = plotState.hasOperation(Operation.CROP);
        var isRotation = plotState.hasOperation(Operation.ROTATE);
        var cropNotRotate =  isCrop && !isRotation ? true: false;
        var operationType = 'original';
        if (isCrop ||isRotation){
            operationType='modified';
        }


        return {
            plot:plot,
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

    showResults(success, request) {

        var results= (
            <PopupPanel title={'Example Dialog Results'} closePromise={closePromise} >
                {this.makeResultInfoContent(statStr,s,resolver)}
            </PopupPanel>
        );

        DialogRootContainer.defineDialog('ResultsFromExampleDialog', results);
        AppDataCntlr.showDialog('ResultsFromExampleDialog');

    },
    resultsFail(request) {
        this.showResults(false,request);
    },

    resultsSuccess(request) {
        this.showResults(true,request);
    },

    //get the updated form components' state
    _getState(){
        //check the radio type to see if it is a Fits, a Region or a PNG
        var fields = FieldGroupUtils.getGroupFields['FITS_DOWNLOAD_FORM'];

        var fileType = fields.fileType.value;


        var band=Band.NO_BAND;
        if(this.props.band!=Band.NO_BAND) {
            band = {color:fields.colorName.value, idx:Band[fields.colorName.value]};
            var color = fields.threeBandColor.value;
        }
        var ops;
        if (this.props.hasOperation){
            ops= fields.operationOption.value;
        }

        return {fileType:fileType, color:color, operationType:ops };
    },


     _doFileDownload() {


         //TODO add code to save the files
         /*    var fileName = this.plotState.getUploadFileName(this.plotState.getBands());
          //Frame f= Application.getInstance().getNullFrame();

          var {fileType, color, operationOption} = _getState();
          var url;

          if (fileType.toLowerCase() =='fits')  {

          var  fitsFile= this.plotState.getOriginalFitsFileStr(this.band) == "undefined"  || this.operation==("modified")?
          state.getWorkingFitsFileStr(band) :
          state.getOriginalFitsFileStr(band);

          url= WebUtil.encodeUrl(GWT.getModuleBaseURL()+ "servlet/Download",
          new Param("file", fitsFile),
          new Param("return", makeFileName(state,band)),
          new Param("log", "true"));
          if (url!=null) f.setUrl(url);
          }
          else if (_dType.getValue().equals("region")) {
          retrieveRegion(plot);
          }
          else {
          retrievePng(plot);
          }
          */
         var fitsFile='myFits.fits';
         download(getRootURL() + fitsFile);
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
                                                                  {label: 'FITS File', value: 'opt1'},
                                                                  {label: 'PNG File', value: 'opt2' },
                                                                  {label: 'Region File', value: 'opt3'},

                                                              ]

                                                              }
                                                   alignment={'vertical'}

                                                   fieldKey='fileType'
                                                   groupKey='FITS_DOWNLOAD_FORM'/>


                            { fitsFileRadioGroup}

                            {colorRadioGroup}

                            <div style={{'text-align':'center'}}>
                                <button type='button' onclick = {this._doFileDownload()} >Download</button>

                            </div>




                        </InputGroup>


                    </div>

                </div>
            </FieldGroup>
        );
    }
    });
