/*jshint browserify:true*/

"use strict";
var PointerPopup= require('ipac-firefly/ui/PointerPopup.jsx');
var React= require('react/addons');

var EXCLAMATION= 'tmp-stuff/exclamation16x16.gif'

var InputFieldView = module.exports= React.createClass(
{

       mixins : [React.addons.PureRenderMixin],

       propTypes: {
           valid   : React.PropTypes.bool,
           visible : React.PropTypes.bool,
           message : React.PropTypes.string,
           tooltip : React.PropTypes.string,
           label : React.PropTypes.string,
           value   : React.PropTypes.string.isRequired,
           onChange : React.PropTypes.func.isRequired,
       },

       getDefaultProps : function() {
           return {
               valid : true,
               visible : true,
               message: ""

           };
       },

       getInitialState : function() {
           return {
               hasFocus : false,
               infoPopup : false,
               onChange : null
           };
       },

       onChange: function(ev) {
           if (this.props.onChange) {
               this.props.onChange(ev);
           }
       },

       alertEntry: function(ev) {
           this.setState({infoPopup:true});
       },
       alertLeave: function(ev) {
          this.setState({infoPopup:false});
       },

       onFocus: function(ev) {
           if (!this.state.hasFocus) {
               this.setState({hasFocus:true});
           }
       },


       onBlur: function(ev) {
           this.setState({hasFocus:false});
       },

       makeWarningArea : function(warn) {
           /*jshint ignore:start */
           var warnIcon= "";
           if (warn) {
               warnIcon= (
                       <div onMouseOver={this.alertEntry} onMouseLeave={this.alertLeave}>
                           <img ref={function(c){
                               this.computeWarningXY(c);}.bind(this)
                                   } src={EXCLAMATION}/>
                       </div>
               );
           }

           //<img ref={(c)=>this.computeWarningXY(c);} src={EXCLAMATION}/>
           var retval= (
                   <div style={
                      {
                       paddingLeft: "3px",
                       width: "16px",
                       height: "16px",
                       display:'inline-block'}
                       }>
                        {warnIcon}
                   </div>
               );
           return retval;
           /*jshint ignore:end */
       },

       computeStyle : function() {
           if (!this.props.valid) {
               return "firefly-inputfield-error";
           }
           else {
               return this.state.hasFocus ?  "firefly-inputfield-focus" : "firefly-inputfield-valid";
           }
       },

       makeMessage : function() {
           return (
                   <div>
                       <img src={EXCLAMATION}
                               style={{display:'inline-block',
                                       paddingRight:5}}/>
                       <div style={{display:'inline-block'}}> {this.props.message} </div>
                   </div>
           );
       },

    warningOffsetX : 0,
    warningOffsetY : 0,

    componentDidMount : function() {
        //if (!this.props.valid) {
        //    this.computeWarningXY();
        //}

    },

    computeWarningXY : function(warnIcon) {
        var retval= "";
        if (warnIcon) {
            var e= React.findDOMNode(warnIcon);
            var bodyRect = document.body.getBoundingClientRect();
            var elemRect = e.getBoundingClientRect();
            this.warningOffsetX   = (elemRect.left - bodyRect.left) + e.offsetWidth/2;
            this.warningOffsetY   = elemRect.top - bodyRect.top;
        }
    },

    makeInfoPopup : function() {

        var retval= <PointerPopup x={this.warningOffsetX} y={this.warningOffsetY}
                message={this.makeMessage()}/>
        return retval;
    },


    createLabel : function() {
        var retval= null;
        var labelStyle= {
            display:'inline-block',
            paddingRight:'4px',
        };
        if (this.props.labelWidth) {
            labelStyle.width= this.props.labelWidth;
        }
        /*jshint ignore:start */
        if (this.props.label) {
            retval= (
                    <div style={labelStyle} title={this.props.tooltip}>
                       {this.props.label}
                    </div>
            );
        }
        /*jshint ignore:end */
        return retval;
    },

    render: function() {
        var retval= null;
        if (this.props.visible) {
            retval= (
                    <div style={{whiteSpace:"nowrap"}}>
                       {this.createLabel()}
                        <input style={{display:'inline-block'}}
                                className={this.computeStyle()}
                                onChange={this.onChange}
                                onFocus={this.onFocus}
                                onBlur={this.onBlur}
                                value={this.props.value}
                                title={this.props.tooltip}
                        />
                       {this.makeWarningArea(!this.props.valid)}
                       {this.state.infoPopup?this.makeInfoPopup() : ""}
                    </div>
            );
        }

        return retval;
    }


});


