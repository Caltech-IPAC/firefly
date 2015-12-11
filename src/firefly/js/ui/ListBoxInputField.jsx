import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';

import InputFieldLabel from './InputFieldLabel.jsx';

var ListBoxInputField = React.createClass(
    {
        mixins : [PureRenderMixin, FieldGroupToStoreMixin],

        propTypes: {
            inline : React.PropTypes.bool,
            options : React.PropTypes.array.isRequired,
            multiple : React.PropTypes.bool,
            labelWidth : React.PropTypes.number
        },

        contextTypes: {
            groupKey: React.PropTypes.string
        },

        componentWillMount() {
            // if no default value is specified, select the first option
            if (typeof this.state.fieldState.value === 'undefined' || this.state.fieldState.value==='') {
                this.state.fieldState.value = this.props.options[0].value;
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
                fieldKey : this.props.fieldKey,
                newValue : val.toString(),
                message,
                valid,
                fieldState : this.state.fieldState
            });
        },

        getCurrentValueArr() {
            var curValue = this.getValue();
            if (curValue) {
                return (typeof curValue === 'string') ? curValue.split(',') : [curValue];
            }
            else {
                return [];
            }
        },


        render() {
            var vAry= this.getCurrentValueArr();
            return (
                <div style={{whiteSpace:'nowrap', display: this.props.inline?'inline-block':'block'}}>
                    <InputFieldLabel label={this.getLabel()}
                        tooltip={this.getTip()}
                        labelWidth={this.props.labelWidth}
                    />
                    <select name={this.props.fieldKey}
                            title={this.getTip()}
                            multiple={this.props.multiple}
                            onChange={this.onChange}
                            value={this.props.multiple ? vAry : this.getValue()}>
                        {this.props.options.map(( (option) => {
                            return (
                                <option value={option.value}
                                        key={option.value}>
                                    &nbsp;{option.label}&nbsp;
                                </option>
                            );
                        }))}
                    </select>
                </div>
            );
        }


    });

export default ListBoxInputField;

