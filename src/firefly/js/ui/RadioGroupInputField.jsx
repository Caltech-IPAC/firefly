import React, {PropTypes}  from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';

import InputFieldLabel from './InputFieldLabel.jsx';

var RadioGroupInputField= React.createClass(
    {
        mixins: [PureRenderMixin, FieldGroupToStoreMixin],

        propTypes: {
            inline : PropTypes.bool,
            options: PropTypes.array.isRequired,
            alignment:  PropTypes.string.isRequired
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
            // the value of the group is the value of the selected option
            var val = ev.target.value;
            var checked = ev.target.checked;

            if (checked) {
                this.fireValueChange({
                    fieldKey: this.props.fieldKey,
                    newValue: val,
                    fieldState: this.state.fieldState
                });
            }
        },

        render() {

            const {alignment, labelWidth, fieldKey} = this.props;

            let optionalElement;

            switch (alignment) {
                case 'vertical':
                    optionalElement = <br />;
                    break;
                default:
                    optionalElement = null;
                    break;
            }

            return (
                <div style={{whiteSpace:'nowrap',display: this.props.inline?'inline-block':'block'}}>
                    <InputFieldLabel label={this.getLabel()}
                                     tooltip={this.getTip()}
                                     labelWidth={labelWidth}
                    />
                    {optionalElement}
                    <div style={{display:'inline-block'}} >
                        {this.props.options.map((option) => {

                            return (
                                <div style={optionalElement ? {display:'block'}:{display:'inline-block'}} key={option.value}>
                                    <input type='radio'
                                           name={fieldKey}
                                           value={option.value}
                                           defaultChecked={this.getValue()===option.value}
                                           onChange={this.onChange}
                                    /> {option.label}&nbsp;&nbsp;
                                </div>);
                        })}
                    </div>
                </div>
            );
        }
    });

export default RadioGroupInputField;

