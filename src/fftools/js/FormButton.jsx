/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import PopupUtil from 'firefly/util/PopupUtil.jsx';
import Modal from 'react-modal';
import formActions from 'actions/FormActions.js';
//import Portal from "react-portal";


var appElement = document.getElementById('modal-element');
Modal.setAppElement(appElement);
//Modal.injectCSS();


var FormButton = React.createClass(
   {

       validUpdate(ev) {
           var formStore= this.props.formStore;
           var statStr= 'validate state: '+ formStore.getState().formValid;
           console.log(statStr);
           var request= formStore.getResults();
           console.log('request:');
           console.log(request);

           var s= Object.keys(request).reduce(function(buildString,k,idx,array){
               buildString+=k+'=' +request[k];
               if (idx<array.length-1) buildString+=', ';
               return buildString;
           },'');
           this.setState({modalOpen:true, request:statStr+'::::: '+s});


           var resolver= null;
           var closePromise= new Promise(function(resolve, reject) {
               resolver= resolve;
           });
           PopupUtil.showDialog('Results',this.makeResultInfoContent(statStr,s,resolver),closePromise);

       },

       getInitialState : function() {
           this.validUpdate= this.validUpdate.bind(this);
           return { results : false,
                    modalOpen : false};
       },

       componentDidMount() {
           this.props.formStore.getEventEmitter().addListener('formValid', this.validUpdate);
           formActions.mountComponent( {
               formKey: this.getFormKey(),
               fieldKey : this.props.fieldKey,
               mounted : true,
               value: this.getValue(),
               fieldState: this.props.initialState
           } );
       },

       componentWillUnmount() {
           this.props.formStore.getEventEmitter().removeListener('formValid', this.validUpdate);
           formActions.mountComponent( {
               formKey: this.getFormKey(),
               fieldKey : this.props.fieldKey,
               mounted : false,
               value: this.getValue()
           } );

       },


       onClick(ev) {
           formActions.validateForm();
       },


       onDialogClose(closePromise, ev) {
           this.props.closeDialog();

       },

       makeDialogContent(statStr,s,closePromiseClick) {

           return (
               /*jshint ignore:start */
                   <div style={{padding:'5px'}}>
                       <br/>{statStr}<br/><br/>{s}
                       <button type="button" onClick={closePromiseClick}>Another Close</button>
                   </div>
               /*jshint ignore:end */
                   );
       },

       makeModel : function() {
           var retval= null;
           /*jshint ignore:start */

           if (this.state.results) {
               retval= (
                       <FormResultTmp/>
               );

           }

           /*jshint ignore:end */
           return retval;
       },

       closeModal : function() {
           this.setState({modalOpen:false});

       },

       render: function() {
           /*jshint ignore:start */
           return (
                   <div>
                       <button type="button" onClick={this.onClick}>submit</button>

                   </div>
           );
           /*jshint ignore:end */
       }


   });

export default FormButton;
