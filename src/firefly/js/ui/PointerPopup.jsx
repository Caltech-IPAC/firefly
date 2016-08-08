/*eslint "prefer-template": 0*/

import React, {Component, PropTypes} from 'react';
import defer from 'lodash/defer';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import './PointerPopup.css';
import UP_POPUP_POINTER from 'html/images/up-pointer.gif';
import DOWN_POPUP_POINTER from 'html/images/down-pointer.gif';
import LEFT_DOWN_POPUP_POINTER from 'html/images/left-down-pointer.gif';

const NONE = 'none';
const NORTH = 'north';
const SOUTH_WEST = 'sWest';
const SOUTH = 'south';


function computePosition(e,dir,tgtX,tgtY) {
    var {left,top} = e.getBoundingClientRect();
    var x= tgtX-left;
    var y= tgtY-top;
    if (dir===NORTH) {
        return {x,y:y+18};
    } else if (dir===SOUTH) {
        return {x,y:y-18};
    } else {
        return {x:x+10,y:y+5};
    }
}





export class PointerPopup extends Component {
    constructor(props) {
        super(props);
        this.state= {
            dir : NONE
        };
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidMount() { this.updatePosition(); }
    componentDidUpdate() { this.updateOffsets(ReactDOM.findDOMNode(this)); }


    computeDir(e) {
        var elemRect = e.getBoundingClientRect();
        if ((this.props.y-window.scrollY)+elemRect.height+35>window.innerHeight) {
            if (this.props.x-window.scrollX+elemRect.width+35>window.innerWidth) {
                this.setState({dir: SOUTH});
            } else {
                this.setState({dir: SOUTH_WEST});
            }
        }
        else {
            this.setState({dir: NORTH});
        }
    }

    updateOffsets(e) {
        var {dir}= this.state;
        var {x,y}= this.props;
        var {innerWidth, scrollX,scrollY}= window;
        var pos = computePosition(e, dir, x, y);
        if (dir === NORTH) {
            var left = pos.x - e.offsetWidth / 2 - scrollX;
            var adjust = 0;
            if (left < 5) {
                adjust = (left - 5);
                left = 5;
            } else if (left > innerWidth - e.offsetWidth) {
                var newLeft = innerWidth - e.offsetWidth - 20;
                adjust = left - newLeft;
                left = newLeft;
            }
            e.style.left = left + 'px';
            e.style.top = (pos.y - scrollY) + 'px';
            var upPointer = ReactDOM.findDOMNode(this.refs.upPointer);
            upPointer.style.paddingLeft = (((e.offsetWidth / 2) + adjust) - 15) + 'px';
            upPointer.style.display = 'block';
            upPointer.style.marginBottom = '3px';
            e.style.visibility = 'visible';
        } else if (dir === SOUTH) {
            var left = pos.x - e.offsetWidth / 2 - scrollX;
            var adjust = 0;
            if (left < 5) {
                adjust = (left - 5);
                left = 5;
            } else if (left > innerWidth - e.offsetWidth) {
                var newLeft = innerWidth - e.offsetWidth - 20;
                adjust = left - newLeft;
                left = newLeft;
            }
            e.style.left = left + 'px';
            var top= pos.y - (e.offsetHeight/2+5);
            top= top<5 ? 5 : top;
            e.style.top = (top - scrollY) + 'px';
            var downPointer = ReactDOM.findDOMNode(this.refs.downPointer);
            downPointer.style.paddingLeft = (((e.offsetWidth / 2) + adjust) - 15) + 'px';
            downPointer.style.display = 'block';
            downPointer.style.marginBottom = '3px';
            e.style.visibility = 'visible';
        } else if (dir===SOUTH_WEST) {
            var left= pos.x+20 - scrollX;
            var pointerVisible= true;
            if (left > innerWidth - e.offsetWidth) {
                left = innerWidth-e.offsetWidth-20;
                pointerVisible = false;
            }
            e.style.left= left +'px';
            var top= pos.y - (e.offsetHeight/2+15);
            top= top<5 ? 5 : top;
            e.style.top= (top-scrollY)+'px';
            var leftDownPointer= ReactDOM.findDOMNode(this.refs.leftDownPointer);
            leftDownPointer.style.left= -20+'px';
            leftDownPointer.style.top= 8+'px';
            leftDownPointer.style.paddingLeft= 0;
            leftDownPointer.style.visibility = pointerVisible ? 'visible' : 'hidden';
            e.style.visibility='visible';
        }

    }

    updatePosition() {
        var e= ReactDOM.findDOMNode(this);
        this.updateOffsets(e);
        defer(function() {
            this.computeDir(e);
        }.bind(this));
    }



    render() {
        var {x,y,message}= this.props;
        if (!x && !y) return;
        if (this.state.dir===NORTH || this.state.dir===NONE) {
            return (
                <div style={{position:'absolute',left:0,top:0, visibility:'hidden' }}>
                    <img src={UP_POPUP_POINTER} ref='upPointer'/>
                    <div style= {{marginTop:-3}}>
                        <div style={{padding : 5}} className='ff-popup-pointer'>
                            {message}
                        </div>
                    </div>
                </div>
            );
        } else if (this.state.dir === SOUTH) {
            return (
                <div style={{position:'absolute',left:0,top:0, visibility:'hidden' }}>
                    <div style= {{marginBottom:0}}>
                        <div style={{padding : 5}} className='ff-popup-pointer'>
                            {message}
                        </div>
                    </div>
                    <img src={DOWN_POPUP_POINTER} ref='downPointer'/>
                </div>
            );
        } else {
            return (
                <div style={{position:'absolute',left:0,top:0 }}>
                    <img src={LEFT_DOWN_POPUP_POINTER}
                         ref='leftDownPointer'
                         style={{display:'inline-block', position:'absolute'}}/>
                    <div style= {{marginTop:-5,display:'inline-block'}}>
                        <div style={{padding : 5}} className='ff-popup-pointer '>
                            {message}
                        </div>
                    </div>
                </div>
            );

        }
    }
}


PointerPopup.propTypes= {
    x : PropTypes.number.isRequired,
    y : PropTypes.number.isRequired,
    message : PropTypes.object.isRequired
};
