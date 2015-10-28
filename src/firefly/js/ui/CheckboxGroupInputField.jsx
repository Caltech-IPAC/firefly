import React from 'react/addons';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';

import InputFieldLabel from './InputFieldLabel.jsx';

var CheckboxGroupInputField = React.createClass(
    {
        mixins : [React.addons.PureRenderMixin, FieldGroupToStoreMixin],

        propTypes: {
            options : React.PropTypes.array.isRequired
        },


        onChange(ev) {
            // when a checkbox is checked or unchecked
            // the array, representing the value of the group,
            // needs to be updated
            var val = ev.target.value;
            var checked = ev.target.checked;
            var curValueArr = this.getCurrentValueArr();
            var idx = curValueArr.indexOf(val);
            if (checked) {
                if (idx < 0) {
                    curValueArr.push(val); // add val to the array
                }
            }
            else {
                if (idx > -1) {
                    curValueArr.splice(idx, 1); // remove val from the array
                }
            }

            var {valid,message} = this.getValidator()(curValueArr.toString());

            this.fireValueChange({
                fieldKey: this.props.fieldKey,
                newValue: curValueArr.toString(),
                message,
                valid,
                fieldState: this.state.fieldState
            });

        },

        componentWillMount() {
            // resolve _all_ and _none_ values
            if (this.state.fieldState.value === '_all_') {
                this.state.fieldState.value = this.props.options.map((option) => option.value).toString();
            } else if (typeof this.state.fieldState.value  === 'undefined'
                || this.state.fieldState.value === '_none_') {
                this.state.fieldState.value = '';
            }
        },

        getCurrentValueArr() {
            var curValue = this.getValue();
            if (curValue === '') {
                return [];
            } else {
                return curValue.split(',');
            }
        },

        isChecked(val) {
            var currentGroupVal = this.state.fieldState.value;
            return (currentGroupVal.split(',').indexOf(val) > -1);
        },

        render() {
            const { fieldKey, labelWidth, options }= this.props;
            return (
                <div style={{whiteSpace: 'nowrap'}}>
                    <InputFieldLabel label={this.getLabel()}
                        tooltip={this.getTip()}
                        labelWidth={labelWidth}
                    />
                    {options.map( (option) => {
                        return (
                            <input type='checkbox'
                                key={option.value}
                                name={fieldKey}
                                value={option.value}
                                checked={this.isChecked(option.value)}
                                onChange={this.onChange}
                            >&nbsp;{option.label}&nbsp;&nbsp;</input>
                        );
                        })}
                </div>
            );
        }


    });

export default CheckboxGroupInputField;

