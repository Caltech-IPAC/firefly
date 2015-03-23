/*jshint browserify:true*/"use strict";
/*jshint esnext:true*/


var React= require('react/addons');
var FormStoreLinkMixin = require('ipac-firefly/ui/model/FormStoreLinkMixin.js');

import InputFieldLabel from "./InputFieldLabel.jsx";

var CheckboxGroupInputField= React.createClass(
    {

        mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

        propTypes: {
            options : React.PropTypes.array.isRequired
        },

        getInitialState() {
            return {
                currentValue : ""
            };
        },


        onChange(ev) {
            // a checkbox is checked or unchecked
            // the array, representing the value of the checkbox group,
            // needs to be updated
            var val = ev.target.value;
            var checked = ev.target.checked;
            var curValueArr = this.getValue().split(',');
            var idx = curValueArr.indexOf(val);
            if (checked) {
                if (idx < 0) {
                    curValueArr.push(val); // add val to the array
                }
            } else {  // unchecked
                if (idx > -1) {
                    curValueArr.splice(idx, 1); // remove val from the array
                }
            }

            var validateState= this.getValidator()(curValueArr);

            this.props.dispatcher.dispatch({
                evType : 'valueChange',
                fieldKey : this.props.fieldKey,
                newValue : curValueArr.toString(),
                message :validateState.message,
                valid : validateState.valid,
                fieldState : this.state.fieldState
            });
        },


        render() {
            return (
                <div style={{whiteSpace:"nowrap"}}>
                    <InputFieldLabel label={this.getLabel()}
                        tooltip={this.getTip()}
                        width={this.props.labelWidth}
                    />
                    {this.props.options.map((function(option, i) {
                        return <input type="checkbox"
                                name={this.props.fieldKey}
                                value={option["value"]}
                                defaultChecked={((this.state.fieldState.value).indexOf(option["value"])>-1)}
                                onChange={this.onChange}
                            >&nbsp;{option["label"]}&nbsp;</input>;
                        }).bind(this))}
                </div>
            )
        }


    });

export default CheckboxGroupInputField;

