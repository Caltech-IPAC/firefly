/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useContext, useEffect} from 'react';
import PropTypes from 'prop-types';
import {TargetPanel} from './TargetPanel.jsx';
import {InputGroup} from './InputGroup.jsx';
import Validate, {intValidator} from '../util/Validate.js';
import {ValidationField} from './ValidationField.jsx';
import {CheckboxGroupInputField} from './CheckboxGroupInputField.jsx';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SuggestBoxInputField} from './SuggestBoxInputField.jsx';
import {PlotlyWrapper} from '../charts/ui/PlotlyWrapper.jsx';
import CompleteButton from './CompleteButton.jsx';
import {FieldGroup, FieldGroupCtx} from './FieldGroup.jsx';
import {dispatchMultiValueChange, dispatchRestoreDefaults, VALUE_CHANGE} from '../fieldGroup/FieldGroupCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {showModal} from './PopupUtil.jsx';
import {updateSet} from '../util/WebUtil.js';

import {CollapsiblePanel} from './panel/CollapsiblePanel.jsx';
import {StatefulTabs, Tab,FieldGroupTabs} from './panel/TabPanel.jsx';
import {dispatchShowDialog} from '../core/ComponentCntlr.js';
import {NaifidPanel} from './NaifidPanel.jsx';
import {useBindFieldGroupToStore} from 'firefly/ui/SimpleComponent.jsx';
import {getGroupFields} from '../fieldGroup/FieldGroupUtils.js';
import {CoordinateSys, makeWorldPt, WebPlotRequest} from '../api/ApiUtilImage.jsx';
import {TargetHiPSPanel, VisualTargetPanel} from '../visualize/ui/TargetHiPSPanel.jsx';
import {SizeInputFields} from 'firefly/ui/SizeInputField.jsx';


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
    high: {
        fieldKey: 'high',
        value: '3',
        validator: Validate.intRange.bind(null, 1, 100, 'high field'),
        tooltip: 'this is a tip for high field',
        label: 'High Field:'
    },
    radioGrpFld: {
        fieldKey: 'radioGrpFld',
        orientation: 'horizontal',
        tooltip: 'Please select an option',
        label: 'Radio Group:',
        options: [
            {label: 'Option 1', value: 'opt1'},
            {label: 'Hide A Field', value: 'opt2'}
        ],
        value: 'opt1'
    }
};



/**
 *
 * @param {object} inFields
 * @param {object} action
 * @return {object}
 */
function exDialogReducer(inFields, action) {
    if (!inFields)  {
        return defValues;
    }
    else {
        let {low={valid:false},high}= inFields;
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
}


const dependentOptions1 =  [
    {label: 'Dependent 1', value: 'dep1'},
    {label: 'Dependent 2', value: 'dep2'}
];
const dependentOptions2 = [
    {label: 'Dependent 3', value: 'dep3'},
    {label: 'Dependent 4', value: 'dep4'},
    {label: 'Dependent 5', value: 'dep5'}
];

function masterDependentReducer(inFields, action) {
    if (!inFields)  {
        return {
            master: {
                fieldKey: 'master',
                orientation: 'horizontal',
                tooltip: 'Please select an option',
                label: 'Master:',
                labelWidth: 40,
                options: [
                    {label: 'Option 1', value: 'opt1'},
                    {label: 'Option 2', value: 'opt2'}
                ],
                value: 'opt1'
            },
            dependent: {
                fieldKey: 'dependent',
                tooltip: 'Please select a dependent option',
                label: 'Dependent:',
                labelWidth: 60,
                options: dependentOptions1,
                value: 'dep1'
            }
        };
    }
    else {
        if (action?.type === VALUE_CHANGE) {
            // update the options and value in the dependent radio group
            // based on the value of master radio group
            const {fieldKey, value} = action.payload;
            if (fieldKey === 'master') {
                const newOptions = (value === 'opt1') ?  dependentOptions1 : dependentOptions2;
                const newValue = newOptions[0].value;
                inFields = updateSet(inFields, 'dependent.options', newOptions);
                inFields = updateSet(inFields, 'dependent.value', newValue);
            }
        }
        return inFields;
    }
}

// Make sure the options are set in one place: either in reducer function or in render, not in both places,
// In general, field attributes, controlled by a reducer function, should not be set in render.
function FieldGroupWithMasterDependent() {
    return (
        <FieldGroup style= {{padding:5}} groupKey={'MasterDependent'}
                    reducerFunc={masterDependentReducer} keepState={true}>
            <h4>Dependent field options are changing with the value of master field</h4>
            <RadioGroupInputField fieldKey='master'/>
            <br/>
            <ListBoxInputField fieldKey='dependent'/>
        </FieldGroup>
    );
}


/// test
const AllTest= () => (
        <div style={{padding:'5px', minWidth: 480}}>
            <div>
                <StatefulTabs componentKey='exampleOuterTabs' useFlex={true}>
                    <Tab name='First'>
                        <FieldGroupTest />
                    </Tab>
                    <Tab name='Second'>
                        <div style={{minWidth: 300, minHeight: 150}}>
                            <CollapsiblePanel componentKey='exampleHistogramCollapsible' isOpen={true} header='Sample Histogram'>
                                {createSampleHistogram()}
                            </CollapsiblePanel>
                        </div>
                    </Tab>
                    <Tab name='Dependent'>
                        <div style={{minWidth: 300, minHeight: 150}}>
                            <FieldGroupWithMasterDependent />
                        </div>
                    </Tab>
                    <Tab name='HiPS/Target'>
                        <div style={{minWidth: 500, minHeight: 400}}>
                            <CreateHiPSTargetExample/>
                        </div>
                    </Tab>
                    <Tab name='HiPS/Target - popup'>
                        <div style={{minWidth: 500, minHeight: 400}}>
                            <CreateHiPSTargetPopupExample/>
                        </div>
                    </Tab>
                </StatefulTabs>
            </div>
        </div>
    );

function createSampleHistogram() {
    const {x=[], y=[], binWidth=[]} = {};
    [[1,-2.5138013781265,-2.0943590644815],
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
        [4,2.2944872084182,2.312472786789]].forEach((row) => {
        x.push((row[2]+row[1])/2);
        y.push(row[0]);
        binWidth.push(row[2]-row[1]);
    });
    const data = [{type: 'bar', x, y, width: binWidth, marker: {line: {width: 1, color: 'lightgray'}}}];
    const layout = {title: 'Sample Histogram'};
    return (
        <PlotlyWrapper data={data}
                       layout={layout}
                       style={{width: '700px',height: '400px'}}/>
    );


}

const FieldGroupTest= () => {
    const fields=useBindFieldGroupToStore('DEMO_FORM');
    return <FieldGroupTestView fields={fields} />;
};


const makeSpan= (w) => <span style={{paddingLeft: `${w}px`}}/>;

/*
    If you want to see the examples back to default, just replace this with ={}
 */
const defaultExamples = <div style={{display : 'inline-block'}}>
    {makeSpan(5)} 'M17' {makeSpan(15)} 'NGC6946' {makeSpan(15)}  '141.607 -47.347 gal'
    <br />
    {makeSpan(15)} '46.53 -0.251 gal' {makeSpan(5)}'12h34m27.0504s +2d11m17.304s Equ J2000'
    <br />
    {makeSpan(5)}  '20h27m36.3467s +40d01m21.649s Equ B1950'
</div>;

let lowDefValue=2;
let lowKey= 'abc';


function FieldGroupTestView ({fields={}}) {

    var hide = false;
    if (fields) {
        const {radioGrpFld} = fields;
        hide = (radioGrpFld && radioGrpFld.value === 'opt2');
    }
    var field1 = makeField1(hide);

    const lowField= getGroupFields('DEMO_FORM')?.low;
    const highField= getGroupFields('DEMO_FORM')?.high;
    if (Number(highField?.value)===100) {
        lowDefValue=50;
        lowKey='newLowKey';
    }
    console.log(lowField);

    const tabX3CheckBoxOps= [
        {label: 'Carrots', value: 'C'},
        {label: 'Squash', value: 'S'},
        {label: 'Green Beans', value: 'G'},
        {label: 'Peas', value: 'P'}
    ];
    if (fields.fieldInTabX3?.value > 30 ) tabX3CheckBoxOps[0].label='Carrots!!!!';








    const validSuggestions = [];
    for (var i=1; i<100; i++) { validSuggestions.push(...[`w${i}mpro`, `w${i}mprosig`, `w${i}snr`]); }

    return (
        <FieldGroup style= {{padding:5}} groupKey={'DEMO_FORM'}
                    reducerFunc={exDialogReducer} keepState={true}>
            <InputGroup labelWidth={110}>
                <TargetPanel examples={defaultExamples} />
                <NaifidPanel label={'NAIF-ID:'} popStyle={{width: 300, padding:2}}/>
                <SuggestBoxInputField
                    fieldKey='suggestion1'
                    initialState= {{
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
                                     value: '12.12322',
                                     validator: Validate.floatRange.bind(null, 1.23333, 1000, 3,'field 3'),
                                     tooltip: 'more tipping',
                                     label : 'Another Float:',
                                     labelWidth : 100
                                 }} />
                <ValidationField fieldKey='field4'/>
                <ValidationField fieldKey='low'
                                 key={lowKey}
                                 initialState= {{
                                     value: lowDefValue,
                                     validator: intValidator(1, 100, 'low field')
                                 }}
                                 tooltip='this is a tip for low field'
                                 label= 'Low Field:'

                />
                <ValidationField fieldKey='high'/>
                
                <br/>
                <span>here is some text</span>
                <br/><br/>
                <RadioGroupInputField
                    inline={true}
                    fieldKey='radioGrpFld'
                />

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
                />
                <br/><br/>


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
                            orientation='vertical'
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
                        <X3Stuff {...{tabX3CheckBoxOps}} />
                    </Tab>
                </FieldGroupTabs>




                <br/><br/>

                <button type='button' className='button std hl'  onClick={() => resetSomeDefaults()}>
                    <b>Reset Some Defaults</b>
                </button>
                <button type='button' className='button std hl'  onClick={() => resetDefaults()}>
                    <b>Reset All Defaults</b>
                    Display Modal Dialog
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


function X3Stuff({tabX3CheckBoxOps}) {
    const {register, unregister}= useContext(FieldGroupCtx);
    
    useEffect( () => {
        register('tabX3Data',() => tabX3CheckBoxOps.map( ({label}) => label).join('---'));
        return () => unregister('tabX3Data');
    },[tabX3CheckBoxOps]);


    return (
        <div>
            <ValidationField fieldKey='fieldInTabX3'
                             tooltip= 'more tipping' label= 'tab test field:' labelWidth ={100}
                             validator= {Validate.intRange.bind(null, 11, 55, 'Tab Test Field 11-55')}
                             initialState= {{ value: 25}}
            />
            <div style={{padding: '10px 0 5px 0', fontSize:'smaller'}}>
                if you enter a number over 30 you will get another checkbox, also try less than 23</div>
            <CheckboxGroupInputField fieldKey='checkBoxGrpFldAgain'
                                     tooltip= 'Please select some boxes' label='Checkbox Group:'
                                     options={tabX3CheckBoxOps}
                                     initialState= {{value: 'C,S,P'}} />
        </div>

    );
}

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

function sampleModal() {
    return (
        <div style={{padding: 5, align: 'center'}}>
            <div style={{padding: 5}}>
                Important message here
            </div>
            <button type='button' onClick={() => showModal(null, false)}>OK</button>
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
    const f1= (
        <ValidationField fieldKey={'field1'} />
    );
    const hidden= <div style={{paddingLeft:30}}>field is hidden</div>;
    return hide ? hidden : f1;
}


function CreateHiPSTargetExample() {
    const someMocUrls= [
        'https://irsa.ipac.caltech.edu/data/hips/CDS/GALEX/GR6-03-2014/AIS-Color/Moc.fits', // 79%
        'https://irsa.ipac.caltech.edu/data/hips/CDS/SPITZER/IRAC1/Moc.fits', // 1.37%
        'http://alasky.cds.unistra.fr/DES/CDS_P_DES-DR1_Y/Moc.fits', //   12.7% , center  22,34, fov 120
        'http://alasky.cds.unistra.fr/VISTA/VVV_DR4/VISTA-VVV-DR4-J/Moc.fits' // 1.38%  center 246,49, fov 85
    ];
    return (
        <FieldGroup style= {{padding:5}} groupKey={'HiPS_TARGET'}>
            <div>
                <TargetHiPSPanel centerPt={makeWorldPt(246,-49)} hipsUrl='ivo://CDS/P/DSS2/color'
                                 style={{height:600}}
                                 hipsFOVInDeg={86} searchAreaInDeg={1.5}
                                 coordinateSys={ CoordinateSys.GALACTIC}
                                 mocList={[
                                     {
                                         mocUrl: 'http://alasky.cds.unistra.fr/VISTA/VVV_DR4/VISTA-VVV-DR4-J/Moc.fits',
                                         title: 'Vista Coverage'
                                     }
                                 ]}
                />
            </div>
        </FieldGroup>
    );
}

function CreateHiPSTargetPopupExample() {
    return (
        <FieldGroup style= {{padding:5}} groupKey='HiPS_TARGET_POPUP'>
            <div>
                <div style={{display:'flex', flexDirection:'column', marginLeft:10, marginTop: 20}}>
                    <VisualTargetPanel style={{paddingTop: 10}} labelWidth={100}
                                       hipsUrl='ivo://CDS/P/2MASS/color'
                                       centerPt={makeWorldPt(246,-49)}
                                       sizeKey='HiPSPanelRadius'
                                       hipsFOVInDeg={86} searchAreaInDeg={1.5}
                                       mocList={[
                                           {
                                               mocUrl: 'http://alasky.cds.unistra.fr/VISTA/VVV_DR4/VISTA-VVV-DR4-J/Moc.fits',
                                               title: 'Vista Coverage'
                                           }
                                       ]}
                    />
                    <SizeInputFields fieldKey='HiPSPanelRadius' showFeedback={true} labelWidth= {100}  nullAllowed={false}
                                     label={'Search Area'}
                                     initialState={{ unit: 'arcsec', value: 1.5+'', min: 1 / 3600, max: 100 }} />
                </div>
            </div>
        </FieldGroup>
    );
}


//export default ExampleDialog;
