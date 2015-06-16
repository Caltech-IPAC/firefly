/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*globals console*/
/*globals ffgwtVisualize*/
var React= require('react/addons');
var Promise= require("es6-promise").Promise;
var Dispatcher = require("flux").Dispatcher;

import PopupUtil from '../../util/PopupUtil.jsx';
import InputFormModel from '../../ui/model/InputFormModel.js';
import InputGroup from '../../ui/InputGroup.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import ListBoxInputField from '../../ui/ListBoxInputField.jsx';
import Validate from 'ipac-firefly/util/Validate.js';

var cDialogDispatcher= new Dispatcher();

export class ColorDialog {
    constructor() {
        this.init();
    }

    init() {

        this.bandPanelModel= new InputFormModel.FormModel(
            {
                lowerRange: {
                    fieldKey: 'lowerRange',
                    value: '0',
                    validator: Validate.floatRange.bind(null, 0, 100, 'Lower range'),
                    tooltip: 'Lower range of the Stretch',
                    label : 'Lower range:'
                },
                upperRange: {
                    fieldKey: 'upperRange',
                    value: '99',
                    validator: Validate.floatRange.bind(null, 0, 100, 'Upper range'),
                    tooltip: 'Upper range of the Stretch',
                    label : 'Upper range:'
                },
            }
        );
        this.bandPanelModel.initDispatcher(cDialogDispatcher);
    }
    showDialog() {
        var mpw= ffgwtVisualize.AllPlots.getInstance().getMiniPlotWidget();
        var content= (
            <ColorDialogPanel mpw={mpw} dispatcher={cDialogDispatcher} formModel={this.bandPanelModel}/>
        );
        PopupUtil.showDialog('Modify Color Stretch',content);
    }
}




var ColorDialogPanel= React.createClass(
{


    propTypes: {
        mpw : React.PropTypes.object.isRequired,
        dispatcher : React.PropTypes.object.isRequired,
        formModel : React.PropTypes.object.isRequired
    },

    onClick: function(ev) {
        this.doClose();
    },


    doClose() {
    },


    componentWillUnmount() {
    },


    componentDidMount() {
        if (this.props.mpw) {
            console.log('found mpw');
            console.log(this.props.mpw);
        }
        else {
            console.log('mpw not found');
        }
    },


    render() {
        var d= this.props.dispatcher;
        var m= this.props.formModel;
        return (
            <InputGroup labelWidth={130}>
                <ValidationField dispatcher={d}
                                 fieldKey={'lowerRange'}
                                 formModel={m}/>

                <ValidationField dispatcher={d}
                                 fieldKey={'upperRange'}
                                 formModel={m}/>
                <ListBoxInputField dispatcher = {d}
                                   initialState= {{
                                        tooltip: 'Type of lower',
                                        label : ''
                                    }}
                                   options={ [
                                       {label: '%', value: 'i1'},
                                       {label: 'Data', value: 'i2'},
                                       {label: 'Data Min', value: 'i3'},
                                       {label: 'Sigma', value: 'i4'}
                                   ]}
                                   multiple={false}
                                   fieldKey={'lowerType'}
                                   formModel={m}/>
            </InputGroup>
        );
    }
});

