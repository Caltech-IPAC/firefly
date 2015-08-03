/*jshint browserify:true*/
/*globals firefly, onFireflyLoaded, console, require*/
/*jshint esnext:true*/

/*eslint-disable no-unused-vars */
import {fireflyInit, application} from 'ipac-firefly/core/Application.js';
fireflyInit();

import React from 'react/addons';

import TargetPanel from 'ipac-firefly/ui/TargetPanel.jsx';
import InputGroup from 'ipac-firefly/ui/InputGroup.jsx';
import Validate from 'ipac-firefly/util/Validate.js';
import ValidationField from 'ipac-firefly/ui/ValidationField.jsx';
import CheckboxGroupInputField from 'ipac-firefly/ui/CheckboxGroupInputField.jsx';
import RadioGroupInputField from 'ipac-firefly/ui/RadioGroupInputField.jsx';
import ListBoxInputField from 'ipac-firefly/ui/ListBoxInputField.jsx';
import {ServerRequest, ID_NOT_DEFINED} from 'ipac-firefly/data/ServerRequest.js';
import WebPlotRequest from 'ipac-firefly/visualize/WebPlotRequest.js';


import Histogram from 'ipac-firefly/visualize/Histogram.jsx';

var myDispatcher= require('./Dispatcher.js');
//var FormButton= require ('./FormButton.jsx');
import FormButton from './FormButton.jsx';

import {WorldPt, ImagePt, Pt} from 'ipac-firefly/visualize/Point.js';
/*eslint-enable no-unused-vars */

import InputFormBaseStore from 'ipac-firefly/store/InputFormBaseStore.js';
import FieldGroupStore from 'ipac-firefly/store/FieldGroupStore.js';
import FieldGroupActions from 'ipac-firefly/actions/FieldGroupActions.js';



class TestFormStore extends InputFormBaseStore {
    constructor() {
        super();
        this.fields= {
            field1: {
                fieldKey: 'field1',
                value: '3',
                validator: Validate.intRange.bind(null, 1, 10, 'my test field'),
                tooltip: 'this is a tip for field 1',
                label : 'Int Value:'
            },
            field2: {
                fieldKey: 'field2',
                value: '',
                validator: Validate.floatRange.bind(null, 1.2, 22.4, 2,'a float field'),
                tooltip: 'field 2 tool tip',
                label : 'Float Value:',
                labelWidth : 100
            },
            field4: {
                fieldKey: 'field4',
                value: '',
                validator: Validate.validateEmail.bind(null, 'an email field'),
                tooltip: 'Please enter an email',
                label : 'Email:'
            }
        };
        this.formKey= 'DEMO_FORM';
    }
}
var testFormStore= application.alt.createStore(TestFormStore, 'TestFormStore' );


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


FieldGroupActions.initFieldGroup({
        groupKey : 'DEMO_FORM',
        reducerFunc : testReducer,
        validatorFunc: null,
        keepState: false
    }
);



var sr= new ServerRequest();
sr.setParam('AA',2);
sr.setParam('BB',3);
var wpr= new WebPlotRequest();
console.log(wpr);



var AllTest = React.createClass({

    /*eslint-enable no-unused-vars */

    //setCardNumber: function() { },

    render: function() {
        /* jshint ignore:start */
        return (
            <div>
                <InputGroup labelWidth={130}>
                    <TargetPanel formStore='DEMO_FORM' />
                    <ValidationField fieldKey={'field1'}
                                     formStore='DEMO_FORM'/>
                    <ValidationField fieldKey='field2'
                                     formStore='DEMO_FORM'/>
                    <ValidationField fieldKey='field3'
                                     initialState= {{
                            fieldKey: 'field3',
                            value: '12',
                            validator: Validate.floatRange.bind(null, 1.23, 1000, 3,'field 3'),
                            tooltip: 'more tipping',
                            label : 'Another Float:',
                            labelWidth : 100
                        }}
                                     formStore='DEMO_FORM'/>
                    <ValidationField fieldKey={'field4'}
                                     formStore='DEMO_FORM'/>

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
                                             formStore='DEMO_FORM'/>

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
                                           formStore='DEMO_FORM'/>
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
                                       formStore='DEMO_FORM'/>
                    <br/><br/>

                    <FormButton formStore='DEMO_FORM' label='submit'/>
                </InputGroup>
            </div>

        );
        /* jshint ignore:end */
    }
});



/*eslint-disable no-unused-vars */




var All = React.createClass({

    /*eslint-enable no-unused-vars */

    //setCardNumber: function() { },

    render: function() {
        /* jshint ignore:start */
        var histogramDivStyle = {
            width: '400px',
            height: '200px',
            overflow: 'auto'
        };
        return (
            <div>
                <InputGroup labelWidth={130}>
                    <TargetPanel dispatcher={myDispatcher}
                        formModel={testFormModel} />
                    <ValidationField  fieldKey={'field1'}
                        formStore={testFormStore}/>
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey='field2'
                        formModel={testFormModel}/>
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey='field3'
                        initialState= {{
                            fieldKey: 'field3',
                            value: '12',
                            validator: Validate.floatRange.bind(null, 1.23, 1000, 3,'field 3'),
                            tooltip: 'more tipping',
                            label : 'Another Float:',
                            labelWidth : 100
                        }}
                        formModel={testFormModel}/>
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey={'field4'}
                        formModel={testFormModel}/>
                    <br/><br/>

                    <CheckboxGroupInputField dispatcher = {myDispatcher}
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
                        formModel={testFormModel}/>
                    <br/><br/>

                    <RadioGroupInputField dispatcher = {myDispatcher}
                        initialState= {{
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
                        formModel={testFormModel}/>
                    <br/><br/>

                    <ListBoxInputField dispatcher = {myDispatcher}
                        initialState= {{
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
                        formModel={testFormModel}/>
                    <br/><br/>

                    <formbutton dispatcher={mydispatcher}
                        formmodel={testformmodel}
                        label='submit'/>

                </InputGroup>
                <div style={histogramDivStyle}>
                    <Histogram data={[
                           [1,-2.5138013781265,-2.0943590644815],
                           [4,-2.0943590644815,-1.8749167508365],
                           [11,-1.8749167508365,-1.6554744371915],
                           [12,-1.6554744371915,-1.4360321235466],
                           [18,-1.4360321235466,-1.2165898099016],
                           [15,-1.2165898099016,-1.1571474962565],
                           [20,-1.15714749625658,-0.85720518261159],
                           [24,-0.85720518261159,-0.77770518261159],
                           [21,-0.77770518261159,-0.55826286896661],
                           [36,-0.55826286896661,-0.33882055532162],
                           [40,-0.33882055532162,-0.11937824167663],
                           [51,-0.11937824167663,0.10006407196835],
                           [59,0.10006407196835,0.21850638561334],
                           [40,0.21850638561334,0.31950638561334],
                           [42,0.31950638561334,0.53894869925832],
                           [36,0.53894869925832,0.75839101290331],
                           [40,0.75839101290331,0.9778333265483],
                           [36,0.9778333265483,1.1972756401933],
                           [23,1.1972756401933,1.4167179538383],
                           [18,1.4167179538383,1.6361602674833],
                           [9,1.6361602674833,1.8556025811282],
                           [12,1.8556025811282,2.0750448947732],
                           [0,2.0750448947732,2.2944872084182],
                           [4,2.2944872084182,2.312472786789]
                    ]}
                               desc='sample fld'
                               binColor='#f8ac6c'
                               height='200'
                        />
                </div>
            </div>

        );
        /* jshint ignore:end */
    }
});

//React.render(<All />, document.getElementById('example'));
React.render(<AllTest />, document.getElementById('example'));
//window.onFireflyLoaded= function() {
//    /* jshint ignore:start */
//    //React.render(<All />, document.getElementById('example'));
//    React.render(<AllTest />, document.getElementById('example'));
//    /* jshint ignore:end */
//};

