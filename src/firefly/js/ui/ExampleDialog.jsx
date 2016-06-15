/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {TargetPanel} from './TargetPanel.jsx';
import {InputGroup} from './InputGroup.jsx';
import Validate from '../util/Validate.js';
import {ValidationField} from './ValidationField.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SuggestBoxInputField} from './SuggestBoxInputField.jsx';
import Histogram from '../visualize/Histogram.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import {dispatchMultiValueChange, dispatchRestoreDefaults} from '../fieldGroup/FieldGroupCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import FieldGroupUtils, {revalidateFields} from '../fieldGroup/FieldGroupUtils';

import {CollapsiblePanel} from './panel/CollapsiblePanel.jsx';
import {Tabs, Tab,FieldGroupTabs} from './panel/TabPanel.jsx';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';



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
    dispatchShowDialog('ExampleDialog');
}


const defValues= {
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
        nullAllowed : true,
        labelWidth: 100
    },
    field4: {
        fieldKey: 'field4',
        value: 'a.b@c.edu',
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



/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var exDialogReducer= function(inFields, action) {
    if (!inFields)  {
        return defValues;
    }
    else {
        var {low,high}= inFields;
        // inFields= revalidateFields(inFields);
        if (!low.valid || !high.valid) {
            return inFields;
        }
        if (parseFloat(low.value)> parseFloat(high.value)) {
            low= Object.assign({},low, {valid:false, message:'must be lower than high'});
            high= Object.assign({},high, {valid:false, message:'must be higher than low'});
            return Object.assign({},inFields,{low,high});
        }
        else {
            low= Object.assign({},low, low.validator(low.value));
            high= Object.assign({},high, high.validator(high.value));
            return Object.assign({},inFields,{low,high});
        }
    }
};





/// test

var AllTest = React.createClass({



    render() {
        return (
            <div style={{padding:'5px', minWidth: 480}}>
                <div>
                    <Tabs componentKey='exampleOuterTabs' defaultSelected={0} useFlex={true}>
                        <Tab name='First'>
                            <FieldGroupTest />
                        </Tab>
                        <Tab name='Second'>
                            <div style={{'minWidth': '300', 'minHeight': '150'}}>
                                <CollapsiblePanel componentKey='exampleHistogramCollapsible' isOpen={true} header='Sample Histogram'>
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
        this.state = {fields:FieldGroupUtils.getGroupFields('DEMO_FORM')};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore('DEMO_FORM', (fields) => {
            if (fields!==this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {fields}= this.state;
        // if (!fields) return false;
        return <FieldGroupTestView fields={fields} />;
    }

}

// initValues={{extraData:'asdf'}}



function FieldGroupTestView ({fields}) {

    var hide= false;
    if (fields) {
        const {radioGrpFld}= fields;
        hide= (radioGrpFld && radioGrpFld.value==='opt2');
    }
    var field1= makeField1(hide);

    const validSuggestions = [];
    for (var i=1; i<100; i++) { validSuggestions.push(...[`w${i}mpro`, `w${i}mprosig`, `w${i}snr`]); }

    return (
        <FieldGroup style= {{padding:5}} groupKey={'DEMO_FORM'} initValues={{extraData:'asdf',field1:'4'}} 
                          reducerFunc={exDialogReducer} keepState={true}>
            <InputGroup labelWidth={110}>
                <TargetPanel/>
                <SuggestBoxInputField
                    fieldKey='suggestion1'
                    initialState= {{
                        fieldKey: 'suggestion1',
                        value: '',
                        validator:  (val) => {
                            let retval = {valid: true, message: ''};
                            if (!validSuggestions.includes(val)) {
                                retval = {valid: false, message: `${val} is not a valid column`};
                            }
                            return retval;
                        },
                        tooltip: 'Start typing and the list of suggestions will appear',
                        label : 'Suggestion Field:',
                        labelWidth : 100
                    }}
                    getSuggestions = {(val)=>{
                        const suggestions = validSuggestions.filter((el)=>{return el.startsWith(val);});
                        return suggestions.length > 0 ? suggestions : validSuggestions;
                    }}
                />

                {field1}
                <ValidationField fieldKey='field2' />
                <ValidationField fieldKey='field3'
                                 forceReinit={true}
                                 initialState= {{
                                          fieldKey: 'field3',
                                          value: '12.12322',
                                          validator: Validate.floatRange.bind(null, 1.23333, 1000, 3,'field 3'),
                                          tooltip: 'more tipping',
                                          label : 'Another Float:',
                                          labelWidth : 100
                                      }} />
                <ValidationField fieldKey='field4'/>
                <ValidationField fieldKey='low'/>
                <ValidationField fieldKey='high'/>

                <br/><br/>
                <br/>
                <span>here is some text</span>
                <br/><br/>
                <RadioGroupInputField
                    inline={true}
                    alignment='vertical'
                    initialState= {{
                        tooltip: 'Please select an option',
                        label : 'Radio Group:',
                        value: 'opt1'
                    }}
                    options={[
                        {label: 'Option 1', value: 'opt1'},
                        {label: 'Hide A Field', value: 'opt2'}
                    ]}
                    fieldKey='radioGrpFld'
                />
                <br/>
                <RadioGroupInputField
                    inline={true}
                    alignment='vertical'
                    initialState= {{
                        tooltip: 'Please select an option',
                        label: 'Another Group:',
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



                <FieldGroupTabs initialState= {{ value:'x2' }}
                                fieldKey='TabsFgTest'>
                    <Tab name='X 1' id='x1'>
                        <CheckboxGroupInputField
                            initialState= {{
                                    value: '_all_',
                                    tooltip: 'Please select some boxes',
                                    label : 'Checkbox Group:' }}
                            options={[
                                    {label: 'Apple', value: 'A'},
                                    {label: 'Banana', value: 'B'},
                                    {label: 'Cranberry', value: 'C'},
                                    {label: 'Dates', value: 'D'},
                                    {label: 'Grapes', value: 'G'}
                                ]}
                            fieldKey='checkBoxGrpFld'
                            alignment='vertical'
                        />
                    </Tab>
                    <Tab name='X 2' id='x2'>
                        <ValidationField fieldKey='fieldInTabX2'
                                         initialState= {{
                                          fieldKey: 'fieldInTabX2',
                                          value: '87',
                                          validator: Validate.intRange.bind(null, 66, 666, 'Tab Test Field'),
                                          tooltip: 'more tipping',
                                          label : 'tab test field:',
                                          labelWidth : 100
                                      }} />
                    </Tab>
                    <Tab name='X 3' id='x3'>
                        <div>
                            <ValidationField fieldKey='fieldInTabX3'
                                             initialState= {{
                                          fieldKey: 'fieldInTabX3',
                                          value: '25',
                                          validator: Validate.intRange.bind(null, 22, 33, 'Tab Test Field 22-33'),
                                          tooltip: 'more tipping',
                                          label : 'tab test field:',
                                          labelWidth : 100
                                      }} />
                            <div style={{paddingTop: 10}}></div>
                            <CheckboxGroupInputField
                                initialState= {{
                                    value: '_all_',
                                    tooltip: 'Please select some boxes',
                                    label : 'Checkbox Group:' }}
                                options={[
                                    {label: 'Carrots', value: 'C'},
                                    {label: 'Squash', value: 'S'},
                                    {label: 'Green Beans', value: 'G'},
                                    {label: 'Peas', value: 'P'}
                                ]}
                                fieldKey='checkBoxGrpFldAgain'
                            />
                        </div>
                    </Tab>
                </FieldGroupTabs>
                
                
                

                <br/><br/>

                <button type='button' className='button std hl'  onClick={() => resetSomeDefaults()}>
                    <b>Reset Some Defaults</b>
                </button>
                <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                    <b>Reset All Defaults</b>
                </button>

                <CompleteButton groupKey='DEMO_FORM'
                                onSuccess={resultsSuccess}
                                onFail={resultsFail}
                                dialogId='ExampleDialog'
                                includeUnmounted={false}
                />
            </InputGroup>
        </FieldGroup>
    );
}


FieldGroupTestView.propTypes= {
    fields: PropTypes.object
};


function resetSomeDefaults() {
    const defValueAry= Object.keys(defValues).map( (k) => defValues[k]);
    dispatchMultiValueChange('DEMO_FORM', defValueAry);
}

function resetDefaults() {
    dispatchRestoreDefaults('DEMO_FORM');

}

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
    var closePromise= new Promise(function(resolve) {
        resolver= resolve;
    });

    var results= (
        <PopupPanel title={'Example Dialog Results'} closePromise={closePromise} >
            {makeResultInfoContent(statStr,s,resolver)}
        </PopupPanel>
    );

    DialogRootContainer.defineDialog('ResultsFromExampleDialog', results);
    dispatchShowDialog('ResultsFromExampleDialog');

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
        <ValidationField fieldKey={'field1'} />
    );
    var hidden= <div style={{paddingLeft:30}}>field is hidden</div>;
    return hide ? hidden : f1;
}



//export default ExampleDialog;
