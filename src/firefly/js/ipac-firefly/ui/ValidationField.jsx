
'use strict';
import React from 'react/addons';


/*eslint-disable no-unused-vars */
import InputFieldView from './InputFieldView.jsx';
import FormStoreLinkMixin from 'ipac-firefly/ui/model/FormStoreLinkMixin.js';
/*eslint-enable no-unused-vars */


var ValidationField= React.createClass(
   {

       mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],


       propTypes: {
           formStore: React.PropTypes.object,
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
                               valid={this.isValid()}
                               visible= {this.isVisible()}
                               message={this.getMessage()}
                               onChange={this.onChange}
                               value={this.getValue()}
                               tooltip={this.getTip()}
                               label={this.getLabel()}
                               inline={this.props.inline||false}
                               labelWidth={this.props.labelWidth||this.getLabelWidth()}
                       />
           );
       }


   });

export default ValidationField;

