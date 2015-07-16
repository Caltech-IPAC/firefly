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
import FieldGroup from '../../ui/FieldGroup.jsx';
import ListBoxInputField from '../../ui/ListBoxInputField.jsx';
import CheckboxGroupInputField from '../../ui/CheckboxGroupInputField.jsx';
import Validate from '../../util/Validate.js';
import InputFormBaseStore from '../../store/InputFormBaseStore.js';
import ImagePlotsStore from '../../store/ImagePlotsStore.js';
import {PERCENTAGE, MAXMIN, ABSOLUTE,SIGMA,ZSCALE} from '../../visualize/RangeValues.js'
import {STRETCH_LINEAR, STRETCH_LOG, STRETCH_LOGLOG, STRETCH_EQUAL} from '../../visualize/RangeValues.js'
import {STRETCH_SQUARED, STRETCH_SQRT, STRETCH_ASINH, STRETCH_POWERLAW_GAMMA} from '../../visualize/RangeValues.js'
import FieldGroupStore from '../../store/FieldGroupStore.js';
import FieldGroupActions from '../../actions/FieldGroupActions.js';

import {RED_PANEL,
        GREEN_PANEL,
        BLUE_PANEL,
        NO_BAND_PANEL,
        colorPanelChange} from '../../store/visualize/ColorPanelReducer.js'


var {AllPlots, Band } = ffgwt.Visualize;


export class ColorDialog {
    constructor() {
        this.init();
    }

    init() {

    }
    showDialog() {

        FieldGroupActions.initFieldGroup({
                groupKey : NO_BAND_PANEL,
                reducerFunc : colorPanelChange,
                validatorFunc: null,
                keepState: true
            }
        );

        var mpw= ffgwt.Visualize.AllPlots.getInstance().getMiniPlotWidget();
        var content= (
            <ColorDialogPanel mpw={mpw} groupKey={NO_BAND_PANEL} band={Band.NO_BAND}/>
        );
        PopupUtil.showDialog('Modify Color Stretch',content);
    }
}



const LABEL_WIDTH= 105;

var ColorDialogPanel= React.createClass(
{

    formStoreListenerRemove : null,

    propTypes: {
        mpw :      React.PropTypes.object.isRequired,
        groupKey : React.PropTypes.string.isRequired
    },

    getInitialState() {
        return {fields : FieldGroupStore.getGroupFields(this.props.groupKey)}
    },

    onClick: function(ev) {
        this.doClose();
    },


    doClose() {
    },


    componentWillUnmount() {
        if (this.formStoreListenerRemove) this.formStoreListenerRemove();
    },


    componentDidMount() {
        this.formStoreListenerRemove= FieldGroupStore.listen(this.formStoreUpdate.bind(this));
    },

    update() {
    },

    formStoreUpdate() {
        this.setState( {fields : FieldGroupStore.getGroupFields(this.props.groupKey)});
        this.update();
    },

    getStretchTypeField() {
        return (
            <ListBoxInputField fieldKey='algorithm'
                               inline={true}
                               labelWidth={60}
                               options={ [
                                    {label: 'Linear',                 value: STRETCH_LINEAR},
                                    {label: 'Log',                    value: STRETCH_LOG},
                                    {label: 'Log-Log',                value: STRETCH_LOGLOG},
                                    {label: 'Histogram Equalization', value: STRETCH_EQUAL},
                                    {label: 'Squared',                value: STRETCH_SQUARED },
                                    {label: 'Sqrt',                   value: STRETCH_SQRT},
                                    {label: 'Asinh',                  value: STRETCH_ASINH},
                                    {label: 'Power Law Gamma',        value: STRETCH_POWERLAW_GAMMA},
                                 ]}
                               multiple={false}
                               groupKey={this.props.groupKey}
                />
        );
    },

    getUpperAndLowerFields() {
        var groupKey= this.props.groupKey;
        return (
            <div>
                <div style={{ whiteSpace:'no-wrap'}}>
                    <ValidationField inline={true}
                                     labelWidth={LABEL_WIDTH}
                                     fieldKey='lowerRange'
                                     groupKey={this.props.groupKey}
                        />
                    {this.getTypeMinField()}
                </div>
                <div style={{ whiteSpace:'no-wrap'}}>
                    <ValidationField  labelWidth={LABEL_WIDTH}
                                      inline={true}
                                      fieldKey='upperRange'
                                      groupKey={this.props.groupKey}
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
                               groupKey={this.props.groupKey}
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
                               groupKey={this.props.groupKey}
                />
        );
    },


    renderStandard() {
        return  this.getUpperAndLowerFields();
    },


    renderAsinH() {
        var {groupKey}=this.props;
        var {lowerType,zscale}= FieldGroupStore.getGroupFields(groupKey);
        var range= (zscale.value==='zscale') ? this.renderZscale() : this.getUpperAndLowerFields();
        return (
            <div>
                {range}
                <div style={{paddingTop:'10px'}}/>
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='DR' groupKey={groupKey} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='BP' groupKey={groupKey}  />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='WP' groupKey={groupKey} />
            </div>
        );
    },

    renderGamma() {
        var {groupKey}=this.props;
        var {lowerType,zscale}= FieldGroupStore.getGroupFields(groupKey);
        var range= (zscale.value==='zscale') ? this.renderZscale() : this.getUpperAndLowerFields();
        return (
            <div>
                {range}
                <div style={{paddingTop:'10px'}}/>
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='gamma' groupKey={groupKey} />
            </div>
        );
    },

    renderZscale() {
        var {groupKey}=this.props;
        var {lowerType}= FieldGroupStore.getGroupFields(groupKey);
        return (
            <div>
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='contrast' groupKey={groupKey} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='numSamp' groupKey={groupKey} />
                <ValidationField  labelWidth={LABEL_WIDTH} fieldKey='sampPerLine' groupKey={groupKey} />
            </div>
        );
    },

    render() {
        var groupKey=this.props.groupKey;
        var {algorithm, lowerType, zscale}= FieldGroupStore.getGroupFields(groupKey);
        var a= Number.parseInt(algorithm.value);
        var lt= Number.parseInt(lowerType.value);
        var panel;
        if (a===STRETCH_ASINH) {
            panel= this.renderAsinH();
        }
        else if (a===STRETCH_POWERLAW_GAMMA) {
            panel= this.renderGamma();
        }
        else if (zscale.value==='zscale') {
            panel= this.renderZscale();
        }
        else {
            panel= this.renderStandard();
        }
        return (
            <FieldGroup groupKey={groupKey} reducerFunc={colorPanelChange} validatorFunc={null} keepState={true}>
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
                            groupKey={groupKey}/>
                    </div>
                </div>
            </FieldGroup>
            );
    }
});

