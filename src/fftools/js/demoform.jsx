/*jshint browserify:true*/
/*globals firefly, onFireflyLoaded, console*/
/*jshint esnext:true*/
"use strict";

import {fireflyInit} from "ipac-firefly/util/BrowserUtil.js";
fireflyInit();

require("babel/polyfill");
var React= require('react/addons');

var InputFormModel= require ("ipac-firefly/ui/model/InputFormModel.js");
import TargetPanel from "ipac-firefly/ui/TargetPanel.jsx";
var InputGroup= require ("ipac-firefly/ui/InputGroup.jsx");
var Validate= require("ipac-firefly/util/Validate.js");
import ValidationField from "ipac-firefly/ui/ValidationField.jsx";
import CheckboxGroupInputField from 'ipac-firefly/ui/CheckboxGroupInputField.jsx';
import RadioGroupInputField from 'ipac-firefly/ui/RadioGroupInputField.jsx';
import ListBoxInputField from 'ipac-firefly/ui/ListBoxInputField.jsx';
import {ServerRequest, ID_NOT_DEFINED} from "ipac-firefly/data/ServerRequest.js";
import WebPlotRequest from "ipac-firefly/visualize/WebPlotRequest.js";

var myDispatcher= require("./Dispatcher.js");
//var FormButton= require ("./FormButton.jsx");
import FormButton from "./FormButton.jsx";

import {WorldPt, ImagePt, Pt} from "ipac-firefly/visualize/Point.js";

var testFormModel= new InputFormModel.FormModel(
    {
        field1: {
            fieldKey: 'field1',
            value: '3',
            validator: Validate.intRange.bind(null, 1, 10, "my test field"),
            tooltip: "this is a tip for field 1",
            label : "Int Value:"
        },
        field2: {
            fieldKey: 'field2',
            value: '',
            validator: Validate.floatRange.bind(null, 1.2, 22.4, 2,"a float field"),
            tooltip: "field 2 tool tip",
            label : "Float Value:",
            labelWidth : 100
        },
        field4: {
            fieldKey: 'field4',
            value: '',
            validator: Validate.validateEmail.bind(null, "an email field"),
            tooltip: "Please enter an email",
            label : "Email:"
        }
    }
);
testFormModel.initDispatcher(myDispatcher);

var sr= new ServerRequest();
sr.setParam("AA",2);
sr.setParam("BB",3);
var wpr= new WebPlotRequest();
console.log(wpr);

var All = React.createClass({


    setCardNumber: function() {

    },

    render: function() {
        /* jshint ignore:start */
        return (
            <div>
                <InputGroup labelWidth={130}>
                    <TargetPanel dispatcher={myDispatcher}
                        formModel={testFormModel} />
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey={"field1"}
                        formModel={testFormModel}/>
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey="field2"
                        formModel={testFormModel}/>
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey="field3"
                        initialState= {{
                            fieldKey: 'field3',
                            value: '12',
                            validator: Validate.floatRange.bind(null, 1.23, 1000, 3,"field 3"),
                            tooltip: "more tipping",
                            label : "Another Float:",
                            labelWidth : 100
                        }}
                        formModel={testFormModel}/>
                    <ValidationField dispatcher={myDispatcher}
                        fieldKey={"field4"}
                        formModel={testFormModel}/>
                    <br/><br/>

                    <CheckboxGroupInputField dispatcher = {myDispatcher}
                        initialState= {{
                            value: '_all_',
                            tooltip: "Please select some boxes",
                            label : "Checkbox Group:"
                        }}
                        options={
                            [
                                {label: "Apple", value: "A"},
                                {label: "Banana", value: "B"},
                                {label: "Cranberry", value: "C"},
                                {label: "Dates", value: "D"},
                                {label: "Grapes", value: "G"},
                            ]
                            }
                        fieldKey="checkBoxGrpFld"
                        formModel={testFormModel}/>
                    <br/><br/>

                    <RadioGroupInputField dispatcher = {myDispatcher}
                        initialState= {{
                            tooltip: "Please select an option",
                            label : "Radio Group:"
                        }}
                        options={
                            [
                                {label: "Option 1", value: "opt1"},
                                {label: "Option 2", value: "opt2"},
                                {label: "Option 3", value: "opt3"},
                                {label: "Option 4", value: "opt4"}
                            ]
                            }
                        fieldKey="radioGrpFld"
                        formModel={testFormModel}/>
                    <br/><br/>

                    <ListBoxInputField dispatcher = {myDispatcher}
                        initialState= {{
                            tooltip: "Please select an option",
                            label : "ListBox Field:"
                        }}
                        options={
                            [
                                {label: "Item 1", value: "i1"},
                                {label: "Another Item 2", value: "i2"},
                                {label: "Yet Another 3", value: "i3"},
                                {label: "And one more 4", value: "i4"}
                            ]
                            }
                        multiple={false}
                        fieldKey="listBoxFld"
                        formModel={testFormModel}/>
                    <br/><br/>

                    <FormButton dispatcher={myDispatcher}
                        formModel={testFormModel}
                        label="Submit"/>

                </InputGroup>
            </div>

        );
        /* jshint ignore:end */
    }
});

window.onFireflyLoaded= function() {
    /* jshint ignore:start */
    React.render(<All />, document.getElementById('example'));
    /* jshint ignore:end */
};

