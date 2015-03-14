/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/

"use strict";
import React from 'react/addons';
import TargetPanelModel from "ipac-firefly/ui/model/TargetPanelModel.js";
import TargetFeedback from "ipac-firefly/ui/TargetFeedback.jsx";
import InputFieldView from "ipac-firefly/ui/InputFieldView.jsx";
import FormStoreLinkMixin from "ipac-firefly/ui/model/FormStoreLinkMixin.js";



var TargetPanel= module.exports= React.createClass(
   {
       mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

       getDefaultProps() {
           return {
               fieldKey : "UserTargetWorldPt",
               initialState  : {
                   fieldKey : "UserTargetWorldPt"
               }

           };
       },

       getInitialState() {
           return {
               targetModel: new TargetPanelModel(),
               outputValue : "",
               showHelp : true,
               feedback : "",
               valid :true

           };
       },


       componentDidMount() {
           this.state.targetModel.on('change',this.updateTargetFeedback);
       },

       componentWillUnmount() {
           this.state.targetModel.off('change',this.updateTargetFeedback);
       },

       updateTargetFeedback() {
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

       onChange(ev) {
           this.state.targetModel.targetInput= ev.target.value;
           this.state.targetModel.doParse();

           this.setState({outputValue : ev.target.value});
       },

       render() {
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




