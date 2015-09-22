/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 3/7/15.
 */
/*jshint browserify:true*/
"use strict";

var React= require('react/addons');
var Modal = require('react-modal');

import PopupPanel from '../ui/PopupPanel.jsx';
//import Portal from "react-portal";

var modalDiv= null;

//var appElement = document.getElementById('modal-element');



var init= function() {
    if (!modalDiv) {
        modalDiv = document.createElement('div');
        document.body.appendChild(modalDiv);
        Modal.setAppElement(modalDiv);
    }
};

var ModalInternal = React.createClass(
        {

            getInitialState : function() {
                return {  modalOpen : true};
            },


            onClick: function(ev) {
                this.setState({modalOpen : false});
                this.props.closing();
            },

            render: function() {
                /*jshint ignore:start */
                var retval= null;
                if (this.state.modalOpen) {
                    retval= (
                            <Modal isOpen={this.state.modalOpen}
                                    onRequestClose={this.okClick} >
                                <h2>{this.props.title}</h2>
                            {this.props.message}
                                <div>
                                    <button onClick={this.onClick}>close</button>
                                </div>
                            </Modal>
                    );

                }
                return retval;
                /*jshint ignore:end */
            }


        });




var getModal = function(title,message,show,closing) {
    if (!modalDiv) {
        modalDiv = document.createElement('div');
        document.body.appendChild(modalDiv);
        Modal.setAppElement(modalDiv);
    }

    var retval= null;
    if (show) {
        /*jshint ignore:start */
        retval= (
                <ModalInternal title={title}
                        message={message}
                        closing={closing}
                />
        );
        /*jshint ignore:end */
    }
    return retval;

};


var ModalDialog = React.createClass(
{

    propTypes: {
        modalOpen   : React.PropTypes.bool.isRequired,
        title   : React.PropTypes.string.isRequired,
        message : React.PropTypes.any.isRequired,
        closeRequest : React.PropTypes.func.isRequired
    },

    onClick: function(ev) {
        this.setState({modalOpen : false});
        this.props.closeRequest();
    },

    render: function() {
        /*jshint ignore:start */
        var retval= null;
        if (this.props.modalOpen) {
            retval= (
                    <Modal isOpen={true}
                            onRequestClose={this.okClick}>
                        <h2>{this.props.title}</h2>
                            {this.props.message}
                        <div>
                            <button onClick={this.onClick}>close</button>
                        </div>
                    </Modal>
            );

        }
        return retval;
        /*jshint ignore:end */
    }

});



//var Dialog = React.createClass(
//{
//
//    propTypes: {
//        openComponent: React.PropTypes.element,
//        title   : React.PropTypes.string.isRequired,
//        message : React.PropTypes.any.isRequired,
//    },
//
//    onClick: function(ev) {
//        this.setState({modalOpen : false});
//        this.props.closeRequest();
//    },
//
//    render: function() {
//        /*jshint ignore:start */
//
//        var s= {position : "absolute",
//            width : "100px",
//            height : "100px",
//            background : "blue",
//            left : "40px",
//            right : "170px"};
//
//        return (
//                <Portal openbyClickOn={this.props.openComponent} closeOnEsc={true}>
//                    <div style={s}>
//                        {this.props.title}<br/>
//                        {this.props.message}
//                    </div>
//                </Portal>
//        );
//        /*jshint ignore:end */
//    }
//
//});
//


var idCnt= 0;
const DIALOG_DIV= "dialogDiv";
const freeElementList= [];



//var Dialog= React.createClass(
//{
//    propTypes: {
//        closeCallback : React.PropTypes.object.isRequired,
//        component: React.PropTypes.element,
//    },
//
//    componentWillUnmount() {
//    },
//
//    onClick: function(ev) {
//        this.props.closeCallback();
//    },
//
//    render: function() {
//
//        var s= {position : "absolute",
//            width : "100px",
//            height : "100px",
//            background : "white",
//            left : "40px",
//            right : "170px"};
//        /*jshint ignore:start */
//        return  (
//                <div style={s}>
//                    {this.props.children}
//                    <button type="button" onClick={this.onClick}>close here</button>
//                </div>
//        );
//        /*jshint ignore:end */
//    }
//
//});



var IndependentWrapper = React.createClass(
{
    propTypes: {
        divId   : React.PropTypes.string.isRequired,
    },

    closeCallback: function(ev) {
        var e = document.getElementById(this.props.divId);
        React.unmountComponentAtNode(e);
        freeElementList.push(e);
    },

    render: function() {
        /*jshint ignore:start */
        var newChildren = React.Children.map(this.props.children, child => {
          return React.cloneElement(child, { closeCallback: this.closeCallback })
        });

        return  (
                <div>
                {newChildren}
                </div>
        );
        /*jshint ignore:end */
    }

});



var showDialog= function(title,reactComponent, closePromise) {

    var divElement;
    if (!freeElementList.length) {
        var divId= DIALOG_DIV + (idCnt++);
        divElement= document.createElement("div");
        document.body.appendChild(divElement);
        divElement.id= divId;
    }
    else {
        divElement= freeElementList.shift();
    }
    var wrapper= (
            <IndependentWrapper divId={divElement.id}>
                <PopupPanel title={title} closePromise={closePromise} >
                    {reactComponent}
                </PopupPanel>
            </IndependentWrapper>
    );

    React.render(wrapper, divElement);
}



//----------------------------------------------------
//----------------------------------------------------
//----------------------------------------------------




//----------------------------------------------------
//----------------------------------------------------
//----------------------------------------------------





exports.getModal= getModal;
exports.ModalDialog= ModalDialog;
exports.showDialog= showDialog;
