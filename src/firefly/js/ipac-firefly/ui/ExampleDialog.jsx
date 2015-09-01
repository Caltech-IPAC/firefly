/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*globals console*/
/*globals ffgwt*/
var Promise= require("es6-promise").Promise;

import React from 'react/addons';

import TargetPanel from '../ui/TargetPanel.jsx';
import InputGroup from '../ui/InputGroup.jsx';
import Validate from '../util/Validate.js';
import ValidationField from '../ui/ValidationField.jsx';
import CheckboxGroupInputField from '../ui/CheckboxGroupInputField.jsx';
import RadioGroupInputField from '../ui/RadioGroupInputField.jsx';
import ListBoxInputField from '../ui/ListBoxInputField.jsx';
import {ServerRequest, ID_NOT_DEFINED} from '../data/ServerRequest.js';
import WebPlotRequest from '../visualize/WebPlotRequest.js';
import Histogram from '../visualize/Histogram.jsx';
import JunkFormButton from './JunkFormButton.jsx';
import CompleteButton from './CompleteButton.jsx';
import {WorldPt, ImagePt, Pt} from '../visualize/Point.js';
import FieldGroupStore from '../store/FieldGroupStore.js';
import FieldGroupActions from '../actions/FieldGroupActions.js';
import FieldGroup from '../ui/FieldGroup.jsx';
import {defineDialog} from '../ui/DialogRootContainer.jsx';
import PopupPanel from '../ui/PopupPanel.jsx';
import DialogActions from '../actions/DialogActions.js';




class ExampleDialog {
    constructor() {
        var popup= (
            //<PopupPanel title={'Example Dialog'} closePromise={closePromise}>
            <PopupPanel title={'Example Dialog'} >
                <AllTest  groupKey={'DEMO_FORM'} />
            </PopupPanel>
        );

        FieldGroupActions.initFieldGroup({

                groupKey : 'DEMO_FORM',
                reducerFunc : testReducer,
                validatorFunc: null,
                keepState: true
            }
        );
        defineDialog('ExampleDialog', popup);
    }

    showDialog() {

        DialogActions.showDialog({dialogId: 'ExampleDialog'});
    }
}


/**
 *
 * @param inFields
 * @param actionsConst
 * @return {*}
 */
var testReducer= function(inFields, actionsConst) {
    if (!inFields)  {
        var fields= {
            field1: {
                fieldKey: 'field1',
                value: '3',
                validator: Validate.intRange.bind(null, 1, 10, 'my test field'),
                tooltip: 'this is a tip for field 1',
                label: 'Int Value:'
            },
            field2: {
                fieldKey: 'field2',
                value: '',
                validator: Validate.floatRange.bind(null, 1.2, 22.4, 2, 'a float field'),
                tooltip: 'field 2 tool tip',
                label: 'Float Value:',
                labelWidth: 100
            },
            field4: {
                fieldKey: 'field4',
                value: '',
                validator: Validate.validateEmail.bind(null, 'an email field'),
                tooltip: 'Please enter an email',
                label: 'Email:'
            }
        };
        return fields;
    }
    else {
        return inFields;
    }
};


var AllTest = React.createClass({

    /*eslint-enable no-unused-vars */

    showResults : function(success, request) {
        var statStr= "validate state: "+ success;
        //var request= FieldGroupUtils.getResults(this.props.groupKey);
        console.log(statStr);
        console.log(request);

        var s= Object.keys(request).reduce(function(buildString,k,idx,array){
            buildString+=k+"=" +request[k];
            if (idx<array.length-1) buildString+=', ';
            return buildString;
        },'');


        var resolver= null;
        var closePromise= new Promise(function(resolve, reject) {
            resolver= resolve;
        });

        var results= (
            <PopupPanel title={'Example Dialog'} closePromise={closePromise} >
                {this.makeResultInfoContent(statStr,s,resolver)}
            </PopupPanel>
        );

        defineDialog('ResultsFromExampleDialog', results);
        DialogActions.showDialog({dialogId: 'ResultsFromExampleDialog'});
    },


    makeResultInfoContent(statStr,s,closePromiseClick) {
        return (
            <div style={{padding:'5px'}}>
                <br/>{statStr}<br/><br/>{s}
                <button type="button" onClick={closePromiseClick}>Another Close</button>
                <CompleteButton dialogId='ResultsFromExampleDialog' />
            </div>
        );
    },



    resultsFail(request) {
        this.showResults(false,request);
    },

    resultsSuccess(request) {
        this.showResults(true,request);
    },

    render: function() {
        /* jshint ignore:start */
        return (
            <div style={{padding:'5px'}}>
                <FieldGroup groupKey={'DEMO_FORM'} reducerFunc={testReducer} validatorFunc={null} keepState={true}>
                    <InputGroup labelWidth={130}>
                        <TargetPanel groupKey='DEMO_FORM' />
                        <ValidationField fieldKey={'field1'}
                                         groupKey='DEMO_FORM'/>
                        <ValidationField fieldKey='field2'
                                         groupKey='DEMO_FORM'/>
                        <ValidationField fieldKey='field3'
                                         initialState= {{
                            fieldKey: 'field3',
                            value: '12',
                            validator: Validate.floatRange.bind(null, 1.23, 1000, 3,'field 3'),
                            tooltip: 'more tipping',
                            label : 'Another Float:',
                            labelWidth : 100
                        }}
                                         groupKey='DEMO_FORM'/>
                        <ValidationField fieldKey={'field4'}
                                         groupKey='DEMO_FORM'/>

                        <br/><br/>
                        <CheckboxGroupInputField
                            initialState= {{
                            value: '_all_',
                            tooltip: 'Please select some boxes',
                            label : 'Checkbox Group:'
                        }}
                            options={
                            [
                                {label: 'Apple', value: 'A'},
                                {label: 'Banana', value: 'B'},
                                {label: 'Cranberry', value: 'C'},
                                {label: 'Dates', value: 'D'},
                                {label: 'Grapes', value: 'G'}
                            ]
                            }
                            fieldKey='checkBoxGrpFld'
                            groupKey='DEMO_FORM'/>

                        <br/><br/>
                        <RadioGroupInputField  initialState= {{
                            tooltip: 'Please select an option',
                            label : 'Radio Group:'
                        }}
                                               options={
                                                [
                                                    {label: 'Option 1', value: 'opt1'},
                                                    {label: 'Option 2', value: 'opt2'},
                                                    {label: 'Option 3', value: 'opt3'},
                                                    {label: 'Option 4', value: 'opt4'}
                                                ]
                                                }
                                               fieldKey='radioGrpFld'
                                               groupKey='DEMO_FORM'/>
                        <br/><br/>

                        <ListBoxInputField  initialState= {{
                            tooltip: 'Please select an option',
                            label : 'ListBox Field:'
                        }}
                          options={
                            [
                                {label: 'Item 1', value: 'i1'},
                                {label: 'Another Item 2', value: 'i2'},
                                {label: 'Yet Another 3', value: 'i3'},
                                {label: 'And one more 4', value: 'i4'}
                            ]
                            }
                                            multiple={false}
                                            fieldKey='listBoxFld'
                                            groupKey='DEMO_FORM'/>
                        <br/><br/>

                        <CompleteButton groupKey='DEMO_FORM'
                                        onSuccess={this.resultsSuccess.bind(this)}
                                        onFail={this.resultsFail.bind(this)}
                                        dialogId='ExampleDialog'
                            />
                    </InputGroup>
                </FieldGroup>
            </div>

        );
        /* jshint ignore:end */
    }
});


//<JunkFormButton groupKey='DEMO_FORM' label='submit'/>

const LABEL_WIDTH= 105;

export default ExampleDialog;
