"use strict";


var React= require('react/addons');
var FormStoreLinkMixin = require('ipac-firefly/ui/model/FormStoreLinkMixin.js');

import InputFieldLabel from "./InputFieldLabel.jsx";

var ListBoxInputField= React.createClass(
    {
        mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

        propTypes: {
            inline : React.PropTypes.bool,
            options : React.PropTypes.array.isRequired,
            multiple : React.PropTypes.bool
        },

        componentWillMount() {
            // if no default value is specified, select the first option
            if (typeof(this.state.fieldState.value) == 'undefined' || this.state.fieldState.value==="") {
                this.state.fieldState.value = this.props.options[0]["value"];
            }
        },


        onChange(ev) {
            // the value of select is an array of selected option values
            //var val = [].map(ev.target.selectedOptions, function(option) {return option["value"];});
            var options = ev.target.options;
            var val = [];
            for (var i = 0; i<options.length; i++) {
                if (options[i].selected) {
                    val.push(options[i].value);
                }
            }

            var {valid,message}=this.getValidator()(val.toString());

            // the value of this input field is a string
            this.fireValueChange({
                formKey : this.getFormKey(),
                fieldKey : this.props.fieldKey,
                newValue : val.toString(),
                message,
                valid,
                fieldState : this.state.fieldState
            });
        },

        getCurrentValueArr() {
            var curValue = this.getValue();
            if (curValue==="") {
                return [];
            } else {
                return curValue.split(',');
            }
        },

        render() {
            return (
                <div style={{whiteSpace:"nowrap", display: this.props.inline?'inline-block':'block'}}>
                    <InputFieldLabel label={this.getLabel()}
                        tooltip={this.getTip()}
                        labelWidth={this.props.labelWidth}
                    />
                    <select name={this.props.fieldKey}
                        multiple={this.props.multiple}
                        defaultValue={this.props.multiple?this.getCurrentValueArr():this.getValue()}
                        onChange={this.onChange}>
                        {this.props.options.map((function(option) {
                            return <option key={option["value"]} value={option["value"]}>
                            &nbsp;{option["label"]}&nbsp;
                            </option>;
                        }).bind(this))}
                    </select>
                </div>
            )
        }


    });

export default ListBoxInputField;

