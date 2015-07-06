/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*globals console*/
/*globals ffgwt*/
var React= require('react/addons');
var Promise= require("es6-promise").Promise;
var Dispatcher = require("flux").Dispatcher;

import PopupUtil from '../../util/PopupUtil.jsx';
import InputGroup from '../../ui/InputGroup.jsx';
import ValidationField from '../../ui/ValidationField.jsx';
import ListBoxInputField from '../../ui/ListBoxInputField.jsx';
import Validate from '../../util/Validate.js';

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
                    validator: Validate.floatRange.bind(null, 0, 100, 1,'Lower range'),
                    tooltip: 'Lower range of the Stretch',
                    label : 'Lower range:'
                },
                upperRange: {
                    fieldKey: 'upperRange',
                    value: '99',
                    validator: Validate.floatRange.bind(null, 0, 100, 1,'Upper range'),
                    tooltip: 'Upper range of the Stretch',
                    label : 'Upper range:'
                },
            }
        );
        this.bandPanelModel.initDispatcher(cDialogDispatcher);
    }
    showDialog() {
        var mpw= ffgwt.Visualize.AllPlots.getInstance().getMiniPlotWidget();
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
            <div style={{padding:'5px'}}>
                <div style={{display:'table', margin:'auto auto'}}>
                <ListBoxInputField dispatcher={d}
                                   key='stretchType'
                                   inline={true}
                                   labelWidth={60}
                                   initialState= {{ tooltip: 'Choose Stretch algorithm', label : 'Stretch Type: ' }}
                                   options={ [
                                                                     {label: 'Linear', value: 'linear'},
                                                                     {label: 'Log', value: 'log'},
                                                                     {label: 'LogLog', value: 'loglog'},
                                                                     {label: 'Histogram Equalization', value: 'equalization'},
                                                                     {label: 'Squared', value: 'squared '},
                                                                     {label: 'Sqrt', value: 'sqrt'}
                                                                 ]}
                                   multiple={false}
                                   fieldKey={'stretchType'}
                                   formModel={m}
                    />
                </div>
                <div style={{ whiteSpace:'no-wrap'}}>
                    <ValidationField dispatcher={d}
                                     key='lowerRange'
                                     inline={true}
                                     labelWidth={90}
                                     fieldKey={'lowerRange'}
                                     formModel={m}
                        />
                    <ListBoxInputField dispatcher={d}
                                       key='lowerType'
                                       inline={true}
                                       labelWidth={0}
                                       initialState= {{ tooltip: 'Type of lower', label : '' }}
                                       options={ [
                                                                     {label: '%', value: 'i1'},
                                                                     {label: 'Data', value: 'i2'},
                                                                     {label: 'Data Min', value: 'i3'},
                                                                     {label: 'Sigma', value: 'i4'}
                                                                 ]}
                                       multiple={false}
                                       fieldKey={'lowerType'}
                                       formModel={m}
                        />
                </div>
                <div style={{ whiteSpace:'no-wrap'}}>
                    <ValidationField dispatcher={d}
                                     labelWidth={90}
                                     inline={true}
                                     fieldKey={'upperRange'}
                                     formModel={m}
                        />
                    <ListBoxInputField dispatcher={d}
                                       key='upperType'
                                       inline={true}
                                       labelWidth={0}
                                       initialState= {{ tooltip: 'Type of lower', label : '' }}
                                       options={ [
                                                                     {label: '%', value: 'i1'},
                                                                     {label: 'Data', value: 'i2'},
                                                                     {label: 'Data Min', value: 'i3'},
                                                                     {label: 'Sigma', value: 'i4'}
                                                                 ]}
                                       multiple={false}
                                       fieldKey={'upperType'}
                                       formModel={m}
                        />
                </div>
            </div>
        );
    }
});

