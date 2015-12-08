/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React from 'react/addons';

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



function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Fits Download Dialog'} >
                    <FitsDialogTest  groupKey={'FITS_DOWNLOAD_FORM'} />
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
        colorBand:  React.PropTypes.string.required,
        operation : React.PropTypes.string.required,
    },

    getDefaultProps: function() {
        return { colorBand: 'grey',operation: 'original'};
     },

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    },


    componentDidMount() {
        this.unbinder= FieldGroupUtils.bindToStore('FITS_DOWNLOAD_FORM', (fields) => this.setState({fields}));
    },

    showResults(success, request) {
        var statStr= `validate state: ${success}`;
        //var request= FieldGroupUtils.getResults(this.props.groupKey);
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
            <PopupPanel title={'Fits Download Dialog Results'} closePromise={closePromise} >
                {this.makeResultInfoContent(statStr,s,resolver)}
            </PopupPanel>
        );

        DialogRootContainer.defineDialog('ResultsFromFitsDownloadDialog', results);
        AppDataCntlr.showDialog('ResultsFromFitsDownloadDialog');

    },


    makeResultInfoContent(statStr,s,closePromiseClick) {
        return (
            <div style={{padding:'5px'}}>
                <br/>{statStr}<br/><br/>{s}
                <button type='button' onClick={closePromiseClick}>Another Close</button>
                <CompleteButton dialogId='ResultsFromFitsDownloadDialog' />
            </div>
        );
    },



    resultsFail(request) {
        this.showResults(false,request);
    },

    resultsSuccess(request) {
        this.showResults(true,request);
    },

    render() {
        var colorBand = this.props.colorBand;
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

                                                     fieldKey='radioGrpFld1'
                                                     groupKey='FITS_DOWNLOAD_FORM'/>

                              <br/><br/>
                              <RadioGroupInputField initialState= {{
                                          tooltip: 'Please select an option',
                                          label : 'FITS file:'

                                          }}
                                              options={
                                                     [
                                                       { label:'original', value:'fileTypeOpt1'},
                                                       { label:'crop', value:'fileTypeOpt2'},

                                                     ]}
                                              alignment={'vertical'}
                                              fieldKey='radioGrpFld2'

                                              groupKey='FITS_DOWNLOAD_FORM'/>

                              <br/><br/>


                              <RadioGroupInputField initialState= {{
                                          tooltip: 'Please select an option',
                                          label : 'Color band:'

                                          }}
                                                    options={
                                                     [
                                                       { label: 'red', value:'colorOpt1'},

                                                     ]}
                                                    fieldKey='radioGrpFld3'

                                                    groupKey='FITS_DOWNLOAD_FORM'/>

                              <div style={{'text-align':'center'}}>
                              <button type='button' >Download</button>
                                  </div>




                          </InputGroup>


                 </div>

              </div>
            </FieldGroup>
        );
    }
});
