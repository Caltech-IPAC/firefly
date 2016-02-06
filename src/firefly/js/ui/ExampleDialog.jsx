/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import TargetPanel from './TargetPanel.jsx';
import InputGroup from './InputGroup.jsx';
import Validate from '../util/Validate.js';
import ValidationField from './ValidationField.jsx';
import CheckboxGroupInputField from './CheckboxGroupInputField.jsx';
import RadioGroupInputField from './RadioGroupInputField.jsx';
import ListBoxInputField from './ListBoxInputField.jsx';
import Histogram from '../visualize/Histogram.jsx';
import CompleteButton from './CompleteButton.jsx';
import FieldGroup from './FieldGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import PopupPanel from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils';

import CollapsiblePanel from './panel/CollapsiblePanel.jsx';
import {Tabs, Tab} from './panel/TabPanel.jsx';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {flux} from '../Firefly.js';




function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Example Dialog'} >
                    <AllTest  groupKey={'DEMO_FORM'} />
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('ExampleDialog', popup);
        }
        return popup;
    };
}

const dialogBuilder= getDialogBuilder();

export function showExampleDialog() {
    dialogBuilder();
    AppDataCntlr.showDialog('ExampleDialog');
}



/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var exDialogReducer= function(inFields, action) {
    if (!inFields)  {
        return {
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
            },
            low: {
                fieldKey: 'low',
                value: '1',
                validator: Validate.intRange.bind(null, 1, 100, 'low field'),
                tooltip: 'this is a tip for low field',
                label: 'Low Field:'
            },
            high: {
                fieldKey: 'high',
                value: '3',
                validator: Validate.intRange.bind(null, 1, 100, 'high field'),
                tooltip: 'this is a tip for high field',
                label: 'High Field:'
            }
        };
    }
    else {
        var {low,high}= inFields;
        if (parseFloat(low.value)> parseFloat(high.value)) {
            low= Object.assign({},low, {valid:false, message:'must be lower than high'});
            high= Object.assign({},high, {valid:false, message:'must be higher than low'});
            return Object.assign({},inFields,{low,high});
        }
        else {
            low= Object.assign({},low, {valid:true});
            high= Object.assign({},high, {valid:true});
            return Object.assign({},inFields,{low,high});
        }
    }
};





/// test

var AllTest = React.createClass({



    render() {
        return (
            <div style={{padding:'5px'}}>
                <div>
                    <Tabs defaultSelected={0}>
                        <Tab name='First'>
                            <FieldGroupTest />
                        </Tab>
                        <Tab name='Second'>
                            <div style={{'minWidth': '300', 'minHeight': '150'}}>
                                <CollapsiblePanel header='Sample Histogram'>
                                    <Histogram data={[
                                     [1,-2.5138013781265,-2.0943590644815],
                                     [4,-2.0943590644815,-1.8749167508365],
                                     [11,-1.8749167508365,-1.6554744371915],
                                     [12,-1.6554744371915,-1.4360321235466],
                                     [18,-1.4360321235466,-1.2165898099016],
                                     [15,-1.2165898099016,-1.1571474962565],
                                     [20,-1.1571474962565,-0.85720518261159],
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
                                               desc=''
                                               binColor='#c8c8c8'
                                               height={100}
                                        />
                                </CollapsiblePanel>
                            </div>
                        </Tab>
                    </Tabs>
                </div>

            </div>

        );
    }
});


class FieldGroupTest extends Component {

    constructor(props)  {
        super(props);
        FieldGroupUtils.initFieldGroup('DEMO_FORM',exDialogReducer);
        this.state = {fields:FieldGroupUtils.getGroupFields('DEMO_FORM')};
    }

    componentWillUnmount() {
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.unbinder= FieldGroupUtils.bindToStore('DEMO_FORM', (fields) => {
            this.setState({fields});
        });
    }

    render() {
        var {fields}= this.state;
        if (!fields) return false;
        return <FieldGroupTestView fields={fields} />;
    }

}





function FieldGroupTestView ({fields}) {

    //var fields= FieldGroupUtils.getGroupFields('DEMO_FORM');
    if (fields) {
        const {radioGrpFld}= fields;
        var hide= (radioGrpFld && radioGrpFld.value==='opt2');
    }
    var field1= makeField1(hide);

    return (
        <FieldGroup groupKey={'DEMO_FORM'} reducerFunc={exDialogReducer} keepState={true}>
            <InputGroup labelWidth={110}>
                <TargetPanel/>
                {field1}
                <ValidationField fieldKey='field2' />
                <ValidationField fieldKey='field3'
                                 initialState= {{
                                          fieldKey: 'field3',
                                          value: '12',
                                          validator: Validate.floatRange.bind(null, 1.23, 1000, 3,'field 3'),
                                          tooltip: 'more tipping',
                                          label : 'Another Float:',
                                          labelWidth : 100
                                      }} />
                <ValidationField fieldKey='field4'/>
                <ValidationField fieldKey='low'/>
                <ValidationField fieldKey='high'/>

                <br/><br/>
                <CheckboxGroupInputField
                    initialState= {{
                        value: '_all_',
                        tooltip: 'Please select some boxes',
                        label : 'Checkbox Group:'
                    }}
                    options={[
                        {label: 'Apple', value: 'A'},
                        {label: 'Banana', value: 'B'},
                        {label: 'Cranberry', value: 'C'},
                        {label: 'Dates', value: 'D'},
                        {label: 'Grapes', value: 'G'}
                    ]}
                    fieldKey='checkBoxGrpFld'
                />

                <br/>
                <span>here is some text</span>
                <br/><br/>
                <RadioGroupInputField
                    inline={true}
                    alignment='vertical'
                    initialState= {{
                        tooltip: 'Please select an option',
                        label : 'Radio Group:'
                    }}
                    options={[
                        {label: 'Option 1', value: 'opt1'},
                        {label: 'Hide A Field', value: 'opt2'}
                    ]}
                    fieldKey='radioGrpFld'
                />
                <RadioGroupInputField
                    inline={true}
                    alignment='vertical'
                    initialState= {{
                        tooltip: 'Please select an option',
                        label: 'Another Group:',
                        value: 'opt2'
                    }}
                    options={[
                        {label: 'Option 2', value: 'opt1'},
                        {label: 'Option 3', value: 'opt2'}
                    ]}
                    fieldKey='radioGrpFld2'
                />

                <br/><br/>

                <ListBoxInputField  initialState= {{
                                          tooltip: 'Please select an option',
                                          label : 'ListBox Field:',
                                          value: 'i3'
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
                />

                <br/><br/>

                <CompleteButton groupKey='DEMO_FORM'
                                onSuccess={resultsSuccess}
                                onFail={resultsFail}
                                dialogId='ExampleDialog'
                />
            </InputGroup>
        </FieldGroup>
    );
}


FieldGroupTestView.propTypes= {
    fields: PropTypes.object.isRequired,
};


function showResults(success, request) {
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
        <PopupPanel title={'Example Dialog Results'} closePromise={closePromise} >
            {makeResultInfoContent(statStr,s,resolver)}
        </PopupPanel>
    );

    DialogRootContainer.defineDialog('ResultsFromExampleDialog', results);
    AppDataCntlr.showDialog('ResultsFromExampleDialog');

}


function makeResultInfoContent(statStr,s,closePromiseClick) {
    return (
        <div style={{padding:'5px'}}>
            <br/>{statStr}<br/><br/>{s}
            <button type='button' onClick={closePromiseClick}>Another Close</button>
            <CompleteButton dialogId='ResultsFromExampleDialog' />
        </div>
    );
}



function resultsFail(request) {
    showResults(false,request);
}

function resultsSuccess(request) {
    showResults(true,request);
}

function makeField1(hide) {
    var f1= (
        <ValidationField fieldKey={'field1'}
                         groupKey='DEMO_FORM'/>
    );
    var hidden= <div style={{paddingLeft:30}}>field is hidden</div>;
    console.log('hide='+hide);
    return hide ? hidden : f1;
}



//export default ExampleDialog;
