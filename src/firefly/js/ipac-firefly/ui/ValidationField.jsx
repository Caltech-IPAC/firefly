/*jshint browserify:true*/

"use strict";
var React= require('react/addons');
var InputFieldView= require ("ipac-firefly/ui/InputFieldView.jsx");
var FormStoreLinkMixin= require ("ipac-firefly/ui/model/FormStoreLinkMixin.js");



var ValidationField= module.exports= React.createClass(
   {

       mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

       onChange: function(ev) {

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


       render: function() {
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


