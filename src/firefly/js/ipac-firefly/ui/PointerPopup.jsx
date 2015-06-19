/*jshint browserify:true*/
/*jshint esnext:true*/
'use strict';
import React from 'react/addons';
import _ from 'underscore';


const UP_POPUP_POINTER = 'images/up-pointer.gif';
const LEFT_DOWN_POPUP_POINTER = 'images/left-down-pointer.gif';
//const HOR_PTR_IMAGE_OFFSET= -6;

const NONE = 'none';
const NORTH = 'north';
const SOUTH_WEST = 'sWest';

var PointerPopup = React.createClass(
   {
       //mixins : [React.addons.PureRenderMixin],

       statics : {
           NONE,
           NORTH,
           SOUTH_WEST
       },


       propTypes: {
           x : React.PropTypes.number.isRequired,
           y : React.PropTypes.number.isRequired,
           message : React.PropTypes.object.isRequired
       },


       getInitialState() {
           return {
               dir : NONE
           };
       },

       computePosition(e,dir) {
           var retval= {};
           var elemRect = e.getBoundingClientRect();
           var x= this.props.x-elemRect.left;
           var y= this.props.y-elemRect.top;
           if (dir===NORTH) {
               retval.x= x;
               retval.y= y+18;
           }
           else {
               retval.x= x+10;
               retval.y= y+5;
           }
           return retval;

       },

       computeDir(e) {

           var elemRect = e.getBoundingClientRect();

           if ((this.props.y-window.scrollY)+elemRect.height+30>window.innerHeight) {
               this.setState({dir: SOUTH_WEST});
           }
           else {
               this.setState({dir: NORTH});
           }

       },

       updateOffsets(e) {
           var pos= this.computePosition(e,this.state.dir);
           if (this.state.dir===NORTH) {
               var left= pos.x - e.offsetWidth/2;
               var adjust= 0;
               if (left<5) {
                   adjust= (left-5);
                   left= 5;

               }
               e.style.left= left +'px';
               e.style.top= pos.y+'px';
               var upPointer= React.findDOMNode(this.refs.upPointer);
               upPointer.style.paddingLeft= (((e.offsetWidth/2)+adjust) -15)+'px';
               e.style.visibility='visible';
           }
           else if (this.state.dir===SOUTH_WEST) {
               e.style.left= (pos.x+20) +'px';
               var top= pos.y - (e.offsetHeight/2+15);
               top= top<5 ? 5 : top;
               e.style.top= top+'px';
               var leftDownPointer= React.findDOMNode(this.refs.leftDownPointer);
               leftDownPointer.style.left= -20+'px';
               leftDownPointer.style.top= 8+'px';
               leftDownPointer.style.paddingLeft= 0;
               e.style.visibility='visible';
           }

       },


       //resizeListener : _.debounce(function() {
       //    this.forceUpdate();
       //}.bind(this),200),
       //
       //
       //componentWillUnmount : function() {
       //    window.removeEventListener('resize',this.resizeListener)
       //
       //},

       updatePosition() {
           var e= React.findDOMNode(this);
           this.updateOffsets(e);
           _.defer(function() {
               this.computeDir(e);
           }.bind(this));
       },

       componentDidMount() {
           this.updatePosition();
           //window.addEventListener('resize',this.resizeListener)
       },


       componentDidUpdate() {
           var e= React.findDOMNode(this);
           this.updateOffsets(e);
       },


       render() {
           if (!this.props.x && !this.props.y) return;
           if (this.state.dir===NORTH || this.state.dir===NONE) {
               return (
                       <div style={{position:'absolute',left:0,top:0, visibility:'hidden' }}>
                           <img src={UP_POPUP_POINTER} ref='upPointer'/>
                           <div className='firefly-popup-pointer-curve-radius' style= {{marginTop:'-3px'}}>
                               <div style={{padding : '5px'}}
                                       className='popup-pane-pointer-shadow firefly-popup-pointer-main-panel firefly-popup-pointer-curve-radius'>
                              {this.props.message}
                               </div>
                           </div>
                       </div>
               );
           }
           else {
               return (
                       <div style={{position:'absolute',left:0,top:0 }}>
                           <img src={LEFT_DOWN_POPUP_POINTER}
                                   ref='leftDownPointer'
                                   style={{display:'inline-block', position:'absolute'}}/>
                           <div className='firefly-popup-pointer-curve-radius' style= {{marginTop:'-5px',display:'inline-block'}}>
                               <div style={{padding : '5px'}}
                                       className='popup-pane-pointer-shadow firefly-popup-pointer-main-panel firefly-popup-pointer-curve-radius'>
                              {this.props.message}
                               </div>
                           </div>
                       </div>
               );

           }
       }
   });

export default PointerPopup;
