/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*globals console*/
/*globals ffgwt*/
var React= require('react/addons');
var Promise= require("es6-promise").Promise;

import {application} from 'ipac-firefly/core/Application.js';
import PopupUtil from '../../util/PopupUtil.jsx';
import InputGroup from '../../ui/InputGroup.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import ListBoxInputField from '../../ui/ListBoxInputField.jsx';
import CheckboxGroupInputField from '../../ui/CheckboxGroupInputField.jsx';
import Validate from '../../util/Validate.js';
import InputFormBaseStore from '../../store/InputFormBaseStore.js';
import ImagePlotsStore from '../../store/ImagePlotsStore.js';
import {PERCENTAGE, MAXMIN, ABSOLUTE,SIGMA,ZSCALE} from '../../visualize/RangeValues.js'
import {STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL} from '../../visualize/RangeValues.js'
import {STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../../visualize/RangeValues.js'
import {noBandStore} from '../../store/visualize/ColorDialogStores.js'



var {AllPlots, Band } = ffgwt.Visualize;

//class ColorDialogStore extends InputFormBaseStore {
//    constructor() {
//        super();
//        this.fields= {
//            lowerRange: {
//                fieldKey: 'lowerRange',
//                value: '0',
//                validator: Validate.floatRange.bind(null, 0, 100, 1, 'Lower range'),
//                tooltip: 'Lower range of the Stretch',
//                label: 'Lower range:'
//            },
//            upperRange: {
//                fieldKey: 'upperRange',
//                value: '99',
//                validator: Validate.floatRange.bind(null, 0, 100, 1, 'Upper range'),
//                tooltip: 'Upper range of the Stretch',
//                label: 'Upper range:'
//            },
//            stretchType: {
//                tooltip: 'Choose Stretch algorithm',
//                label: 'Stretch Type: '
//            },
//            lowerType : {
//                tooltip: 'Type of lower',
//                label : ''
//            },
//            upperType : {
//                tooltip: 'Type of upper',
//                label : ''
//            }
//        };
//        this.formKey = 'COLOR_STRETCH_DIALOG';
//    }
//}
//
//var colorDialogStore= application.alt.createStore(ColorDialogStore, 'ColorDialogStore' );




export class ColorDialog {
    constructor() {
        this.init();
    }

    init() {

    }
    showDialog() {
        var mpw= ffgwt.Visualize.AllPlots.getInstance().getMiniPlotWidget();
        var content= (
            <ColorDialogPanel mpw={mpw} formStore={noBandStore} band={Band.NO_BAND}/>
        );
        PopupUtil.showDialog('Modify Color Stretch',content);
    }
}



const LABEL_WIDTH= 105;

var ColorDialogPanel= React.createClass(
{

    allPlotsListenerRemove : null,
    formStoreListenerRemove : null,

    propTypes: {
        mpw : React.PropTypes.object.isRequired,
        formStore : React.PropTypes.object.isRequired
    },

    getInitialState() {
        return {fields : this.props.formStore.getState().fields}
    },

    onClick: function(ev) {
        this.doClose();
    },


    doClose() {
    },


    componentWillUnmount() {
        if (this.formStoreListenerRemove) this.formStoreListenerRemove();
        if (this.allPlotsListenerRemove) this.allPlotsListenerRemove();
    },


    componentDidMount() {
        //this.allPlotsListenerRemove= ImagePlotsStore.listen(this.plotUpdate.bind(this));
        this.formStoreListenerRemove= this.props.formStore.listen(this.formStoreUpdate.bind(this));
        if (this.props.mpw) {
            console.log('found mpw');
            console.log(this.props.mpw);
        }
        else {
            console.log('mpw not found');
        }
    },

    update() {
    },

    //plotUpdate() {
    //    console.log('plotUpdate');
    //    this.update();
    //},

    formStoreUpdate() {
        console.log('formStoreUpdate');
        this.setState( {fields : this.props.formStore.getState().fields});
        this.update();
    },

    getStretchTypeField() {
        return (
            <ListBoxInputField fieldKey='stretchType'
                               inline={true}
                               labelWidth={60}
                               options={ [
                                    {label: 'Linear',                 value: STRETCH_LINEAR},
                                    {label: 'Log',                    value: STRETCH_LOG},
                                    {label: 'Log Log',                value: STRETCH_LOGLOG},
                                    {label: 'Histogram Equalization', value: STRETCH_EQUAL},
                                    {label: 'Squared',                value: STRETCH_SQUARED },
                                    {label: 'Sqrt',                   value: STRETCH_SQRT},
                                    {label: 'Sqrt',                   value: STRETCH_SQRT},
                                    {label: 'Asinh',                  value: STRETCH_ASINH},
                                    {label: 'Power Law Gamma',        value: STRETCH_POWERLAW_GAMMA},
                                 ]}
                               multiple={false}
                               formStore={this.props.formStore}
                />
        );
    },

    getUpperAndLowerFields() {
        var store= this.props.formStore;
        return (
            <div>
                <div style={{ whiteSpace:'no-wrap'}}>
                    <ValidationField inline={true}
                                     labelWidth={LABEL_WIDTH}
                                     fieldKey='lowerRange'
                                     formStore={store}
                        />
                    {this.getTypeMinField()}
                </div>
                <div style={{ whiteSpace:'no-wrap'}}>
                    <ValidationField  labelWidth={LABEL_WIDTH}
                                      inline={true}
                                      fieldKey='upperRange'
                                      formStore={store}
                        />
                    {this.getTypeMaxField()}
                </div>
            </div>
        );
    },

    getTypeMinField() {
        return (
            <ListBoxInputField fieldKey={'lowerType'}
                               inline={true}
                               labelWidth={0}
                               options={ [ {label: '%', value: PERCENTAGE},
                                                   {label: 'Data', value: ABSOLUTE},
                                                   {label: 'Data Min', value: MAXMIN},
                                                   {label: 'Sigma', value: SIGMA}
                                                  ]}
                               multiple={false}
                               formStore={this.props.formStore}
                />
        );
    },

    getTypeMaxField() {
        return (
            <ListBoxInputField fieldKey='upperType'
                               inline={true}
                               labelWidth={0}
                               options={ [ {label: '%', value: PERCENTAGE},
                                                   {label: 'Data', value: ABSOLUTE},
                                                   {label: 'Data Min', value: MAXMIN},
                                                   {label: 'Sigma', value: SIGMA}
                                                  ]}
                               multiple={false}
                               formStore={this.props.formStore}
                />
        );
    },


    renderStandard() {
        return  this.getUpperAndLowerFields();
    },

    renderAsinH() {
        var store= this.props.formStore;
        var {lowerType}= this.props.formStore.getState().fields;
        var range= lowerType.value===ZSCALE ? this.renderZscale() : this.getUpperAndLowerFields();
        return (
            <div>
                {range}
                <div style={{paddingTop:'10px'}}/>
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='DR' formStore={store} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='BP' formStore={store} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='WP' formStore={store} />
            </div>
        );
    },

    renderGamma() {
        var {lowerType}= this.props.formStore.getState().fields;
        var range= lowerType.value===ZSCALE ? this.renderZscale() : this.getUpperAndLowerFields();
        return (
            <div>
                {range}
                <div style={{paddingTop:'10px'}}/>
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='gamma' formStore={this.props.formStore} />
            </div>
        );
    },

    renderZscale() {
        var store= this.props.formStore;
        return (
            <div>
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='contrast' formStore={store} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='numSamp' formStore={store} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='sampPerLine' formStore={store} />
            </div>
        );
    },

    render() {
        var store= this.props.formStore;
        var {stretchType, lowerType}= this.props.formStore.getState().fields;
        var panel;
        if (stretchType.value===STRETCH_ASINH) {
            panel= this.renderAsinH();
        }
        else if (stretchType.value===STRETCH_POWERLAW_GAMMA) {
            panel= this.renderGamma();
        }
        else if (lowerType.value===ZSCALE) {
            panel= this.renderZscale();
        }
        else {
            panel= this.renderStandard();
        }
        return (
            <div style={{padding:'5px'}}>
                <div style={{display:'table', margin:'auto auto'}}>
                    {this.getStretchTypeField()}
                </div>
                {panel}
                <div style={{display:'table', margin:'auto auto', paddingTop:'20px'}}>
                    <CheckboxGroupInputField
                        options={ [ {label: 'Use ZScale for bounds', value: 'zscale'} ] }
                        fieldKey='zscale'
                        labelWidth={0}
                        formStore={store}/>
                </div>
            </div>
            );
    }
});





//var getCurrentPlot= function() {
//    var retval= null;
//    var mpw= AllPlots.getInstance().getMiniPlotWidget();
//    if (mpw) {
//       retval= mpw.getCurrentPlot()
//    }
//    return retval;
//}
