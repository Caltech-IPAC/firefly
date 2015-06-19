/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*globals console*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";
var React= require('react/addons');
//var _= require("underscore");
//var SkyLight = require('react-skylight');
var PopupUtil = require('ipac-firefly/util/PopupUtil.jsx');
var Modal = require('react-modal');
var Promise= require("es6-promise").Promise;
import formActions from 'ipac-firefly/actions/FormActions.js'
//import Portal from "react-portal";


var appElement = document.getElementById('modal-element');
Modal.setAppElement(appElement);
//Modal.injectCSS();


var FormButton = module.exports= React.createClass(
   {

       validUpdate(ev) {
           var formStore= this.props.formStore;
           var statStr= "validate state: "+ formStore.getState().formValid;
           console.log(statStr);
           var request= formStore.getResults();
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
           this.props.formStore.getEventEmitter().addListener('formValid', this.validUpdate);
           formActions.mountComponent( {
               formKey: this.getFormKey(),
               fieldKey : this.props.fieldKey,
               mounted : true,
               value: this.getValue(),
               fieldState: this.props.initialState,
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

           //formActions.mountComponent(this.getFormKey(),this.props.fieldKey,false,this.getValue(),this.props.initialState)
       },


       onClick(ev) {
           formActions.validateForm();
       },




       //showSimpleDialog: function(){
       //   this.refs.simpleDialog.show();
       // },


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


//<PopupUtil.ModalDialog
//        message={this.state.request ? this.state.request : ""}
//        modalOpen={this.state.modalOpen}
//        title={"results"}
//        closeRequest={this.closeModal}
///>

//<Portal isOpen={this.state.modalOpen}>
//    <div style={s}>
//        {this.state.title}<br/>
//        {this.state.request ? this.state.request : ""}
//    </div>
//</Portal>

//<Portal openbyClickOn={button2} closeOnEsc={true}>
//    <div style={s}>
//        {this.state.title}<br/>
//        {this.state.request ? this.state.request : ""}
//    </div>
//</Portal>


//<PopupUtil.Dialog openComponent={button}
//                  message={this.state.request ? this.state.request : ""}
//                  title={"results"} />

                       //
//<PopupUtil.ModalDialog
//        message={this.state.request ? this.state.request : ""}
//        modalOpen={this.state.modalOpen}
//        title={"results"}
//        closeRequest={this.closeModal}
//        />

