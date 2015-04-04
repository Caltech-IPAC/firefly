/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*globals console*/
/*jshint esnext:true*/

"use strict";
var React= require('react/addons');
var _= require("underscore");
//var SkyLight = require('react-skylight');
var PopupUtil = require('ipac-firefly/util/PopupUtil.jsx');
var Modal = require('react-modal');
//import Portal from "react-portal";


var appElement = document.getElementById('modal-element');
Modal.setAppElement(appElement);
//Modal.injectCSS();


var FormButton = module.exports= React.createClass(
   {

       //mixins : [React.addons.PureRenderMixin],
       //
       //propTypes: {
       //    dispatcher : React.PropTypes.object.isRequired,
       //    formModel : React.PropTypes.object.isRequired,
       //    label : React.PropTypes.string.isRequired
       //},

       //getInitialState : function() {
       //
       //},
       //
       //
       //componentWillMount : function() {
       //},
       //
       //
       //componentDidMount : function() {
       //},
       //
       //componentWillUnmount : function () {
       //},
       //
       //updateState : function() {
       //},

       getInitialState : function() {
           return { results : false,
                    modalOpen : false};
       },


       onClick: function(ev) {
           this.props.formModel.validateForm().then(
                   function(state) {
                       var statStr= "validate state: "+ state;
                       console.log(statStr);
                       var request= {};
                       _.keys(this.props.formModel.attributes).forEach(function(fieldKey) {
                           request[fieldKey] = this.props.formModel[fieldKey].value;
                       },this);
                       console.log("request:");
                       console.log(request);

                       var s= _.keys(request).reduce(function(buildString,k){
                           buildString+=k+"=" +request[k]+", ";
                           return buildString;
                       },"");
                       //this.setState({results:true});
                       //this.showSimpleDialog();
                       //PopupUtil.showModal("here", "try this");
                       this.setState({modalOpen:true,
                                      request:statStr+"::::: "+s});

                       PopupUtil.showDialog("Results",this.makeDialogContent());

                   }.bind(this)
           );
       },

       //showSimpleDialog: function(){
       //   this.refs.simpleDialog.show();
       // },

       makeDialogContent() {
           //var s= {position : "absolute",
           //    width : "100px",
           //    height : "100px",
           //    background : "blue",
           //    left : "40px",
           //    right : "170px"};
           return (
               /*jshint ignore:start */
                   <div style={{padding:'5px'}}>
                       {this.state.title}<br/>
                       {this.state.request ? this.state.request : ""}
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
           var button= <button type="button" onClick={this.onClick}>submit</button>;
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

