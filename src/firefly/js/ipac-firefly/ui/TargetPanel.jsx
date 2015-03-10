/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/

"use strict";
var React= require('react/addons');
var TargetPanelModel= require ("ipac-firefly/ui/model/TargetPanelModel.js");
var TargetFeedback= require ("ipac-firefly/ui/TargetFeedback.jsx");
var InputFieldView= require ("ipac-firefly/ui/InputFieldView.jsx");
var FormStoreLinkMixin= require ("ipac-firefly/ui/model/FormStoreLinkMixin.js");



var TargetPanel= module.exports= React.createClass(
   {
       mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

       getDefaultProps : function() {
           return {
               fieldKey : "UserTargetWorldPt",
               initialState  : {
                   fieldKey : "UserTargetWorldPt"
               }

           };
       },

       getInitialState : function() {
           return {
               targetModel: new TargetPanelModel(),
               outputValue : "",
               showHelp : true,
               feedback : "",
               valid :true

           };
       },


       componentDidMount: function () {
           this.state.targetModel.on('change',this.updateTargetFeedback);
       },

       componentWillUnmount : function () {
           this.state.targetModel.off('change',this.updateTargetFeedback);
       },

       updateTargetFeedback : function() {
           var tm= this.state.targetModel;
           this.setState(tm.changedAttributes());
           this.props.dispatcher.dispatch({
                                     evType : 'valueChange',
                                     fieldKey : this.props.fieldKey,
                                     newValue : tm.wpt ? tm.wpt.toString() : "",
                                     message : "Enter something valid",
                                     valid : tm.valid,
                                     asyncUpdate : tm.resolving,
                                     fieldState : this.state.fieldState
                               });
       },

       onChange: function(ev) {
           this.state.targetModel.targetInput= ev.target.value;
           this.state.targetModel.doParse();

           this.setState({outputValue : ev.target.value});
       },

       render: function() {
           /* jshint ignore:start */
           return (
                   <div>
                       <InputFieldView
                               valid={this.state.valid}
                               visible= {true}
                               message={this.getMessage()}
                               onChange={this.onChange}
                               label={"Name or Position:"}
                               value={this.state.outputValue}
                               tooltip={"Enter a target"}
                               labelWidth={this.props.labelWidth||this.getLabelWidth()}
                       />
                       <TargetFeedback showHelp={this.state.showHelp} feedback={this.state.feedback}/>
                   </div>
           );
           /* jshint ignore:end */
       }
  });




