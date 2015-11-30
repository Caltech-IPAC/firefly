import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';


import InputFieldView from './InputFieldView.jsx';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';


var ValidationField= React.createClass(
{

    mixins : [PureRenderMixin, FieldGroupToStoreMixin],


    propTypes: {
        fieldKey: React.PropTypes.string,
        inline : React.PropTypes.bool
    },


    onChange(ev) {

        var {valid,message}= this.getValidator()(ev.target.value);

        this.fireValueChange({
            fieldKey : this.props.fieldKey,
            newValue : ev.target.value,
            message,
            valid,
            fieldState : this.state.fieldState
        });
    },


    render() {
        return (
            <InputFieldView
                style={this.props.style}
                valid={this.isValid()}
                visible= {this.isVisible()}
                message={this.getMessage()}
                onChange={this.onChange}
                value={String(this.getValue())}
                tooltip={this.getTip()}
                label={this.getLabel()}
                inline={this.props.inline||false}
                labelWidth={this.props.labelWidth||this.getLabelWidth()}
            />
        );
    }


});

export default ValidationField;

