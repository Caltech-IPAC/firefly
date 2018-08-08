/*eslint "prefer-template": 0*/

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';

import {dispatchShowDialog, dispatchHideDialog, isDialogVisible, getDialogOwner} from '../core/ComponentCntlr.js';

import './DropDownMenu.css';



const computePosition= (tgtX,tgtY)  => ({x:tgtX,y:tgtY+18});

function placeDropDown(e,x,y) {
    var pos= computePosition(x,y);

    var left= pos.x - 10;
    if (left<5) {
        left= 5;
    }
    e.style.left= left +'px';
    e.style.top= (pos.y + 10)+'px';
    e.style.visibility='visible';
}


export function SingleColumnMenu({children}) {

    return (
        <div className='ff-MenuItem-dropDown' >
            {children}
        </div>
    );

}



export class DropDownMenuWrapper extends PureComponent {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        var {x,y}= this.props;
        placeDropDown(ReactDOM.findDOMNode(this),x,y);
    }
    componentDidUpdate() {
        var {x,y}= this.props;
        placeDropDown(ReactDOM.findDOMNode(this),x,y);
    }

    render() {
        var {x,y,content,visible,zIndex}= this.props;
        if (!visible) return false;
        if (!x && !y && !content) return false;
        return (
            <div style={{position:'absolute',left:0,top:0, visibility:'hidden',zIndex }}
                 onClick={futureCallback} >
                    <div style={{padding : 5}} className='ff-dropdown-menu'>
                        {content}
                    </div>
            </div>
        );
    }
}

function futureCallback() {
   // place holder to support a future callback
}

DropDownMenuWrapper.propTypes= {
    visible : PropTypes.bool,
    x : PropTypes.number.isRequired,
    y : PropTypes.number.isRequired,
    content : PropTypes.object.isRequired,
    zIndex : PropTypes.number
};



export class DropDownSubMenu extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {showSubMenu: false};
        this.show = this.show.bind(this);
        this.hide = this.hide.bind(this);

    }

    show() {
        if (this.timer) clearTimeout(this.timer);
        this.setState({showSubMenu:true});
    }

    hide() {
        this.timer = setTimeout(() => this.setState({showSubMenu:false}), 250);
    }

    render() {
        const {text, tip, visible=true, children} = this.props;
        const {showSubMenu} = this.state;
        if (!visible) return null;
        return (
            <div className='ff-MenuItem ff-MenuItem-light' title={tip}>
                <div className='menuItemText subMenu' onMouseEnter={this.show} onMouseLeave={this.hide}>
                    <div>{text}</div>
                    <div style={{position: 'relative', marginLeft: 10}}>
                        <div className='arrow-right'/>
                        {showSubMenu &&
                        <div style={{position: 'absolute', top:-10, left:8}} onMouseEnter={this.show}>
                            <SingleColumnMenu>
                                {children}
                            </SingleColumnMenu>
                        </div>
                        }
                    </div>
                </div>
            </div>
        );
    }
}

DropDownSubMenu.propTypes= {
    text: PropTypes.string.isRequired,
    visible: PropTypes.bool,
    tip: PropTypes.string,
};
