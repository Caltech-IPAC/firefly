import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';

import InputFieldLabel from './InputFieldLabel.jsx';

var RadioGroupInputField= React.createClass(
    {
        mixins: [PureRenderMixin, FieldGroupToStoreMixin],

        propTypes: {
            options: React.PropTypes.array.isRequired,
            alignment:  React.PropTypes.string.isRequired,
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

        makeLineBreak(){
            return(<br/>);
        },
        render() {

			   var optionalElement;


				switch ( this.props.alignment ) {
					case 'vertical':
						optionalElement  =  <br/>;
						break;
					default:
						optionalElement=null;
						break;
				}

                return (


                    <div style={{whiteSpace:'nowrap'}}>
                        <InputFieldLabel label={this.getLabel()}
                                         tooltip={this.getTip()}
                                         labelWidth={this.props.labelWidth}
                        />
                        {optionalElement}
                        {this.props.options.map((option) => {

                            return (

                                <span key={option.value}>
                                    <div style={{display:'inline-block'}}>

                                        <input type='radio'
                                               name={this.props.fieldKey}
                                               value={option.value}
                                               defaultChecked={this.getValue()===option.value}
                                               onChange={this.onChange}
                                            /> &nbsp;{option.label}&nbsp;&nbsp;


                                    </div>
									{optionalElement}

                          </span>

                            );


                        })


                        }


                    </div>

                );
            }



    });

export default RadioGroupInputField;

