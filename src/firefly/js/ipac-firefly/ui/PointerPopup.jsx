/*jshint browserify:true*/
"use strict";
var React= require('react/addons');
var _= require("underscore");


var UP_POPUP_POINTER = "images/up-pointer.gif";
var LEFT_DOWN_POPUP_POINTER = "images/left-down-pointer.gif";
var HOR_PTR_IMAGE_OFFSET= -6;

var NONE = "none";
var NORTH = "north";
var SOUTH_WEST = "sWest";

var PointerPopup = React.createClass(
   {
       mixins : [React.addons.PureRenderMixin],

       statics : {
           NONE : NONE,
           NORTH : NORTH,
           SOUTH_WEST : SOUTH_WEST
       },


       propTypes: {
           x : React.PropTypes.number.isRequired,
           y : React.PropTypes.number.isRequired,
           message : React.PropTypes.object.isRequired
       },


       getInitialState : function() {
           return {
               dir : NONE
           };
       },

       computePosition : function(dir) {
           var retval= {};
           if (dir===NORTH) {
               retval.x= this.props.x;
               retval.y= this.props.y+30;
           }
           else {
               retval.x= this.props.x+10;
               retval.y= this.props.y+5;
           }
           return retval;

       },

       computeDir : function(e) {

           var elemRect = e.getBoundingClientRect();

           if ((this.props.y-window.scrollY)+elemRect.height+30>window.innerHeight) {
               this.setState({dir: SOUTH_WEST});
           }
           else {
               this.setState({dir: NORTH});
           }

       },

       updateOffsets : function(e) {
           var pos= this.computePosition(this.state.dir);
           if (this.state.dir===NORTH) {
               var left= pos.x - e.offsetWidth/2;
               var adjust= 0;
               if (left<5) {
                   adjust= (left-5);
                   left= 5;

               }
               e.style.left= left +"px";
               e.style.top= pos.y+"px";
               var upPointer= this.refs.upPointer.getDOMNode();
               upPointer.style.paddingLeft= (((e.offsetWidth/2)+adjust) -10)+"px";
               e.style.visibility="visible";
           }
           else if (this.state.dir===SOUTH_WEST) {
               e.style.left= (pos.x+23) +"px";
               var top= pos.y - (e.offsetHeight/2+3);
               top= top<5 ? 5 : top;
               e.style.top= top+"px";
               var leftDownPointer= this.refs.leftDownPointer.getDOMNode();
               leftDownPointer.style.left= -20+"px";
               leftDownPointer.style.top= 8+"px";
               leftDownPointer.style.paddingLeft= 0;
               e.style.visibility="visible";
           }

       },


       //resizeListener : _.debounce(function() {
       //    this.forceUpdate();
       //}.bind(this),200),
       //
       //
       //componentWillUnmount : function() {
       //    window.removeEventListener("resize",this.resizeListener)
       //
       //},

       componentDidMount: function () {
           var e= this.getDOMNode();
           this.updateOffsets(e)
           _.defer(function() {
               this.computeDir(e);
           }.bind(this));
           //window.addEventListener("resize",this.resizeListener)
       },


       componentDidUpdate: function () {
           var e= this.getDOMNode();
           this.updateOffsets(e)
       },


       render: function() {
           var retval;
           if (this.state.dir===NORTH || this.state.dir===NONE) {
               return (
                       <div style={{position:"absolute",left:0,top:0, visibility:"hidden" }}>
                           <img src={UP_POPUP_POINTER} ref="upPointer"/>
                           <div className="standard-border" style= {{marginTop:"-5px"}}>
                               <div style={{padding : "5px"}}
                                       className="popup-pane-pointer-shadow firefly-popup-pointer-main-panel">
                              {this.props.message}
                               </div>
                           </div>
                       </div>
               );
           }
           else {
               return (
                       <div style={{position:"absolute",left:0,top:0 }}>
                           <img src={LEFT_DOWN_POPUP_POINTER}
                                   ref="leftDownPointer"
                                   style={{display:"inline-block", position:"absolute"}}/>
                           <div className="standard-border" style= {{marginTop:"-5px",display:"inline-block"}}>
                               <div style={{padding : "5px"}}
                                       className="popup-pane-pointer-shadow firefly-popup-pointer-main-panel">
                              {this.props.message}
                               </div>
                           </div>
                       </div>
               );

           }
       }
   });

module.exports= PointerPopup;
