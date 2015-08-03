/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*globals console*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";
var React= require('react/addons');
var PopupUtil = require('ipac-firefly/util/PopupUtil.jsx');
var Modal = require('react-modal');
var Promise= require("es6-promise").Promise;
import FieldGroupStore from '../store/FieldGroupStore.js';
import FieldGroupActions from '../actions/FieldGroupActions.js';
//import Portal from "react-portal";


var appElement = document.getElementById('modal-element');
Modal.setAppElement(appElement);
//Modal.injectCSS();


var JunkFormButton = module.exports= React.createClass(
   {

       validUpdate(ev) {
           var statStr= "validate state: "+ FieldGroupStore.getGroupState(this.props.groupKey).fieldGroupValid;
           console.log(statStr);
           var request= FieldGroupStore.getResults(this.props.groupKey);
           console.log("request:");
           console.log(request);

           var s= Object.keys(request).reduce(function(buildString,k,idx,array){
               buildString+=k+"=" +request[k];
               if (idx<array.length-1) buildString+=', ';
               return buildString;
           },'');
           this.setState({modalOpen:true, request:statStr+'::::: '+s});


           var resolver= null;
           var closePromise= new Promise(function(resolve, reject) {
               resolver= resolve;
           });
           PopupUtil.showDialog('Results',this.makeDialogContent(statStr,s,resolver),closePromise);

       },

       getInitialState : function() {
           this.validUpdate= this.validUpdate.bind(this);
           return { results : false,
                    modalOpen : false};
       },

       componentDidMount() {
           FieldGroupStore.getEventEmitter().addListener('fieldGroupValid', this.validUpdate);
           FieldGroupActions.mountComponent( {
               groupKey: this.props.groupKey,
               fieldKey : this.props.fieldKey,
               mounted : true,
               value: true,
               fieldState: this.props.initialState
           } );
       },

       componentWillUnmount() {
           FieldGroupStore.getEventEmitter().removeListener('fieldGroupValid', this.validUpdate);
           FieldGroupActions.mountComponent( {
               groupKey: this.props.groupKey,
               fieldKey : this.props.fieldKey,
               mounted : false,
               value: true
           } );

       },


       onClick(ev) {
           FieldGroupActions.validateFieldGroup(this.props.groupKey);
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


