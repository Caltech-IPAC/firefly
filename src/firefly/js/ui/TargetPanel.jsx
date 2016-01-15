/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {parseTarget} from '../ui/TargetPanelWorker.js';
import TargetFeedback from '../ui/TargetFeedback.jsx';
import {InputFieldView} from '../ui/InputFieldView.jsx';
import FieldGroupToStoreMixin from '../fieldGroup/FieldGroupToStoreMixin.js';



var TargetPanel= React.createClass(
   {
       mixins : [PureRenderMixin, FieldGroupToStoreMixin],


       contextTypes: {
           groupKey: React.PropTypes.string
       },

       getDefaultProps() {
           return {
               fieldKey : 'UserTargetWorldPt',
               initialState  : {
                   fieldKey : 'UserTargetWorldPt'
               }

           };
       },

       onChange(ev) {
           var displayValue= ev.target.value;

           var parseResults= parseTarget(displayValue, this.getExtraData());
           var component= this;
           var resolvePromise= parseResults.resolvePromise ? parseResults.resolvePromise.then((asyncParseResults) => {
                     return asyncParseResults ? component.makePayload(displayValue, asyncParseResults) : null;
               }) : null;
           this.fireValueChange(this.makePayload(displayValue,parseResults, resolvePromise));
       },

       makePayload(displayValue, parseResults, resolvePromise) {
           return {
               groupKey : this.props.groupKey || this.context.groupKey,
               fieldKey : this.props.fieldKey,
               newValue : parseResults.wpt ? parseResults.wpt.toString() : '',
               message : 'Could not resolve object: Enter valid object',
               valid : parseResults.valid,
               asyncUpdatePromise : resolvePromise,
               displayValue,
               wpt : parseResults.wpt,
               extraData: parseResults
           };
       },

       render() {
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
                       <TargetFeedback showHelp={showHelp} feedback={feedback||''}/>
                   </div>
           );
       }
  });

export default TargetPanel;


