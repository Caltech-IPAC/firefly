/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/

"use strict";
import React from 'react/addons';
//import TargetPanelModel from "ipac-firefly/ui/model/TargetPanelModel.js";
import {parseTarget} from "ipac-firefly/ui/model/TargetPanelWorker.js";
import TargetFeedback from "ipac-firefly/ui/TargetFeedback.jsx";
import InputFieldView from "ipac-firefly/ui/InputFieldView.jsx";
import FormStoreLinkMixin from "ipac-firefly/ui/model/FormStoreLinkMixin.js";
import formActions from '../actions/FormActions.js'



var TargetPanel= module.exports= React.createClass(
   {
       mixins : [React.addons.PureRenderMixin, FormStoreLinkMixin],

       getDefaultProps() {
           return {
               fieldKey : "UserTargetWorldPt",
               initialState  : {
                   fieldKey : "UserTargetWorldPt",
               }

           };
       },

       //updateStore(displayValue, parseResults) {
       //    //this.setState({displayValue, parseResults  });
       //    formActions.valueChange({
       //        formKey : this.getFormKey(),
       //        fieldKey : this.props.fieldKey,
       //        newValue : parseResults.wpt ? parseResults.wpt.toString() : "",
       //        message : "Enter something valid",
       //        valid : parseResults.valid,
       //        asyncUpdate : parseResults.resolvePromise?true:false,
       //        displayValue,
       //        wpt : parseResults.wpt,
       //        extraData: parseResults,
       //    });
       //},

       //onChange(ev) {
       //    var displayValue= ev.target.value;
       //    var parseResults= parseTarget(displayValue, this.getExtraData())
       //    formActions.valueChange(this.makePayload(displayValue,parseResults));
       //    //this.updateStore(displayValue, parseResults);
       //    var component= this;
       //    if (parseResults.resolvePromise) {
       //        parseResults.resolvePromise.then(asyncParseResults => {
       //            if (asyncParseResults) {
       //                formActions.valueChange(component.makePayload(displayValue, asyncParseResults));
       //            }
       //        }).catch(e => console.log(e));;
       //    }
       //},

       onChange(ev) {
           var displayValue= ev.target.value;

           var parseResults= parseTarget(displayValue, this.getExtraData())
           var component= this;
           var resolvePromise= parseResults.resolvePromise ? parseResults.resolvePromise.then(asyncParseResults => {
                     return asyncParseResults ? component.makePayload(displayValue, asyncParseResults) : null;
               }) : null;
           formActions.valueChange(this.makePayload(displayValue,parseResults, resolvePromise));
       },

       makePayload(displayValue, parseResults, resolvePromise) {
           return {
               formKey : this.getFormKey(),
               fieldKey : this.props.fieldKey,
               newValue : parseResults.wpt ? parseResults.wpt.toString() : "",
               message : "Enter something valid",
               valid : parseResults.valid,
               asyncUpdate : resolvePromise,
               displayValue,
               wpt : parseResults.wpt,
               extraData: parseResults,
           }
       },

       render() {
           /* jshint ignore:start */
           var { showHelp, feedback, valid} = this.getExtraData();
           if (typeof valid==='undefined') valid= true;
           if (typeof showHelp==='undefined') showHelp= true;
           return (
                   <div>
                       <InputFieldView
                               valid={valid}
                               visible= {true}
                               message={this.getMessage()}
                               onChange={this.onChange}
                               label={"Name or Position:"}
                               value={this.getDisplayValue()}
                               tooltip={"Enter a target"}
                               labelWidth={this.props.labelWidth||this.getLabelWidth()}
                       />
                       <TargetFeedback showHelp={showHelp} feedback={feedback||""}/>
                   </div>
           );
           /* jshint ignore:end */
       }
  });




