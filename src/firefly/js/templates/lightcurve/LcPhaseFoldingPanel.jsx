/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './LCPanels.css';
import React, {Component, PropTypes} from 'react';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import Validate from '../../util/Validate.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {SuggestBoxInputField} from '../../ui/SuggestBoxInputField.jsx';
import Histogram from '../../charts/ui/Histogram.jsx';
import CompleteButton from '../../ui/CompleteButton.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchMultiValueChange, dispatchRestoreDefaults} from '../../fieldGroup/FieldGroupCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import FieldGroupUtils, {revalidateFields} from '../../fieldGroup/FieldGroupUtils';

import {CollapsiblePanel} from '../../ui/panel/CollapsiblePanel.jsx';
import {Tabs, Tab,FieldGroupTabs} from '../../ui/panel/TabPanel.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';


function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Light Curve'} >
                    <LCInput  groupKey={'LC_FORM'} />
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('LcParamForm', popup);
        }
        return popup;
    };
}

const dialogBuilder= getDialogBuilder();

// Could be a popup form
export function showLcParamForm() {
    dialogBuilder();
    dispatchShowDialog('LcParamForm');
}

const PanelResizableStyle = {
    width: 400,
    minWidth: 400,
    height: 300,
    minHeight: 300,
    overflow: 'auto',
    backgroundColor: '#b3edff',
    padding: '2px',
    position: 'relative'
};

const Header = {
    whiteSpace: 'nowrap',
    height: 'calc(100% - 1px)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'left',
    justifyContent: 'space-between',
    padding: '2px',
    fontSize: 'large'
    };


const defValues= {
    fieldInt: {
        fieldKey: 'fieldInt',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 10, 'test field'),
        tooltip: 'this is a tip for field 1',
        label: 'Int Value:'
    },
    FluxColumn: {
        fieldKey: 'fluxcolumn',
        value: '',
        validator: Validate.floatRange.bind(null, 0.2, 20.5, 2, 'Flux Column'),
        tooltip: 'Flux Column',
        label: 'Flux Column:',
        nullAllowed : true,
        labelWidth: 100
    },
    field1: {
        fieldKey: 'field1',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 10, 'test field'),
        tooltip: 'this is a tip for field 1',
        label: 'Int Value:'
    },
    field2: {
        fieldKey: 'field2',
        value: '',
        validator: Validate.floatRange.bind(null, 1.5, 50.5, 2, 'a float field'),
        tooltip: 'field 2 tool tip',
        label: 'Float Value:',
        nullAllowed : true,
        labelWidth: 100
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


var LCInput = React.createClass({



    render() {
        return (
            <div style={{padding:'5px', minWidth: 480}}>
                <div>
                    <Tabs componentKey='LCInputTabs' defaultSelected={0} useFlex={true}>
                        <Tab name='Phase Folding'>
                            <LcParamForm />
                        </Tab>
                    </Tabs>
                </div>

            </div>

        );
    }
});


export class LcParamForm extends Component {

    constructor(props)  {
        super(props);
        this.state = {fields:FieldGroupUtils.getGroupFields('LC_FORM')};
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
    }


    componentDidMount() {
        this.iAmMounted= true;
        this.unbinder= FieldGroupUtils.bindToStore('LC_FORM', (fields) => {
            if (fields!==this.state.fields && this.iAmMounted) {
                this.setState({fields});
            }
        });
    }

    render() {
        var {fields}= this.state;
        // if (!fields) return false;
        return <LcCurveOptionsPanel fields={fields} />;
    }

}

export function LcPFOptionsPanel ({fields}) {

    var hide= true;
    if (fields) {
        const {radioGrpFld}= fields;
        hide= (radioGrpFld && radioGrpFld.value==='opt2');
    }
    var fieldInt= makeField1(hide);

    //Todo: get valication Suggestion from table

    const validSuggestions = [];
    for (var i=1; i<100; i++) { validSuggestions.push(...[`mjd${i}`, `w${i}`, `w${i}mprosig`, `w${i}snr`]); }

    return (

            <FieldGroup style= {PanelResizableStyle} groupKey={'LC_FORM'} initValues={{timeCol:'mjd1',field1:'4'}}
                              reducerFunc={DialogReducer} keepState={true}>
                <InputGroup labelWidth={110}>

                    <span style={Header}>Light Curve Parameters</span>
                    <br/><br/>
                    <SuggestBoxInputField
                        fieldKey='timeCol'
                        initialState= {{
                            fieldKey: 'timeCol',
                            value: '',
                            validator:  (val) => {
                                let retval = {valid: true, message: ''};
                                if (!validSuggestions.includes(val)) {
                                    retval = {valid: false, message: `${val} is not a valid column`};
                                }
                                return retval;
                            },
                            tooltip: 'Start typing and the list of suggestions will appear',
                            label : 'Time Column:',
                            labelWidth : 100
                        }}
                        getSuggestions = {(val)=>{
                            const suggestions = validSuggestions.filter((el)=>{return el.startsWith(val);});
                            return suggestions.length > 0 ? suggestions : validSuggestions;
                        }}
                    />

                    <br/>
                    <ValidationField fieldKey='flux'
                         forceReinit={true}
                         initialState= {{
                                  fieldKey: 'flux',
                                  value: '',
                                  validator: Validate.floatRange.bind(null, 0.55555, 1.22222, 3,'Flux Column'),
                                  tooltip: 'Flux Column',
                                  label : 'Flux Column:',
                                  labelWidth : 100
                          }} />

                    <br/>

                    <ValidationField fieldKey='fluxerror'
                         forceReinit={true}
                         initialState= {{
                                  fieldKey: 'fluxerrorl',
                                  value: '',
                                  validator: Validate.floatRange.bind(null, 0.55555, 1.22222, 3,'fluxerror'),
                                  tooltip: 'Flux Error Column',
                                  label : 'Flux Error:',
                                  labelWidth : 100
                         }} />
                    <br/>

                    <ValidationField fieldKey='period'
                         forceReinit={true}
                         initialState= {{
                                  fieldKey: 'period',
                                  value: '',
                                  validator: Validate.floatRange.bind(null, 0.55555, 1.22222, 3,'period'),
                                  tooltip: 'Period',
                                  label : 'Period:',
                                  labelWidth : 100
                         }} />

                    <br/> <br/>

                    <button type='button' className='button std hl'  onClick={(request) => onSearchSubmit(request)}>
                        <b>Phase Folded</b>
                    </button>
                    <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                        <b>Reset</b>
                    </button>

                    <br/>
                </InputGroup>
            </FieldGroup>

    );
}


LcPFOptionsPanel.propTypes= {
    fields: PropTypes.object
};

/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
var DialogReducer= function(inFields, action) {
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


function resetDefaults() {
    dispatchRestoreDefaults('LC_FORM');

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
        <PopupPanel title={'LC Parameters'} closePromise={closePromise} >
            {makeResultInfoContent(statStr,s,resolver)}
        </PopupPanel>
    );

    DialogRootContainer.defineDialog('ResultsFromLcParamForm', results);
    dispatchShowDialog('ResultsFromLcParamForm');

}


function makeResultInfoContent(statStr,s,closePromiseClick) {
    return (
        <div style={{padding:'5px'}}>
            <br/>{statStr}<br/><br/>{s}
            <button type='button' onClick={closePromiseClick}>Another Close</button>
            <CompleteButton dialogId='ResultsFromLcParamForm' />
        </div>
    );
}



function resultsFail(request) {
    showResults(false,request);
}

function resultsSuccess(request) {
    showResults(true,request);
}

function onSearchSubmit(request) {
    console.log(request);
    if (request.Tabs==='LC Param') {
        doPhaseFolding(request);
    }
    else {
        console.log('request no supported');
    }
}

//here to plug in the phase folding processor
function doPhaseFolding(request) {
    var tReq;
    if (tReq !== null) {
        dispatchTableSearch(tReq, {removable: false});
    }
}

function makeField1(hide) {
    var f1= (
        <ValidationField fieldKey={'field1'} />
    );
    var hidden= <div style={{paddingLeft:30}}>field is hidden</div>;
    return hide ? hidden : f1;
}



//export default LcParamForm;
