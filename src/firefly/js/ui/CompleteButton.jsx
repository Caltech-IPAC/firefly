/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import AppDataCntlr from '../core/AppDataCntlr.js';

var CompleteButton = React.createClass(
   {
       propTypes: {
           onFail: React.PropTypes.func,
           onSuccess: React.PropTypes.func,
           groupKey: React.PropTypes.any,
           text: React.PropTypes.strng,
           closeOnValid: React.PropTypes.bool,
           dialogId: React.PropTypes.string
       },

       getDefaultProps() {
           return {
               text: 'OK',
               closeOnValid: true,
               dialogId: null
           };

       },


       validUpdate(valid) {
           var {onSuccess, onFail, groupKey, dialogId} = this.props;
           var funcToCall = valid ? onSuccess : onFail;

           if (valid && dialogId) AppDataCntlr.hideDialog(dialogId);

           if (Array.isArray(groupKey)) {
               var requestAry = groupKey.map( (key) => FieldGroupUtils.getResults(key));
               funcToCall(requestAry);
           }
           else {
               var request = FieldGroupUtils.getResults(groupKey);
               funcToCall(request);
           }
       },

       getInitialState() {
           this.validUpdate= this.validUpdate;
           return { };
       },

       onClick() {
           var {onSuccess, groupKey, dialogId}= this.props;
           if (groupKey) {
               FieldGroupUtils.validate(this.props.groupKey, this.validUpdate);
           }
           else {
               if (onSuccess) onSuccess();
               if (dialogId) AppDataCntlr.hideDialog(dialogId);
           }
       },


       render() {
           return (
                   <div>
                       <button type='button' onClick={this.onClick}>{this.props.text}</button>
                   </div>
           );
       }


   });


export default CompleteButton;
