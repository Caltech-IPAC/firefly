/*jshint browserify:true*/
/*jshint esnext:true*/

'use strict';
import React from 'react/addons';
//import InputFieldView from "ipac-firefly/ui/InputFieldView.jsx";
import InputFieldView from './InputFieldView.jsx';
import FormStoreLinkMixin from 'ipac-firefly/ui/model/FormStoreLinkMixin.js';



var ValidationField= React.createClass(
   {

       mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

       onChange(ev) {

           var validateState= this.getValidator()(ev.target.value);

           this.props.dispatcher.dispatch({
                           evType : 'valueChange',
                           fieldKey : this.props.fieldKey,
                           newValue : ev.target.value,
                           message :validateState.message,
                           valid : validateState.valid,
                           fieldState : this.state.fieldState
                     });
       },


       render() {
           /*jshint ignore:start */
           return (
                       <InputFieldView
                               valid={this.isValid()}
                               visible= {this.isVisible()}
                               message={this.getMessage()}
                               onChange={this.onChange}
                               value={this.getValue()}
                               tooltip={this.getTip()}
                               label={this.getLabel()}
                               labelWidth={this.props.labelWidth||this.getLabelWidth()}
                       />
           );
           /*jshint ignore:end */
       }


   });

export default ValidationField;

