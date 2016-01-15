/*eslint "prefer-template": 0*/

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import './DropDownMenu.css';



function computePosition(e,tgtX,tgtY) {
    //var {left,top} = e.getBoundingClientRect();
    //var x= tgtX-left;
    //var y= tgtY-top;
    var x= tgtX;
    var y= tgtY;
    return {x,y:y+18};
}

function placeDropDown(e,x,y) {
    var {scrollX,scrollY}= window;
    var pos= computePosition(e,x,y);

    var left= pos.x - 10 - scrollX;
    if (left<5) {
        left= 5;
    }
    e.style.left= left +'px';
    e.style.top= (pos.y + 10  - scrollY)+'px';
    e.style.visibility='visible';
}


export function SingleColumnMenu({children}) {

    return (
        <div className='ff-MenuItem-dropDown' >
            {children}
        </div>
    );

}



export class DropDownMenuWrapper extends Component {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidMount() {
        var {x,y}= this.props;
        placeDropDown(ReactDOM.findDOMNode(this),x,y);
    }
    componentDidUpdate() {
        var {x,y}= this.props;
        placeDropDown(ReactDOM.findDOMNode(this),x,y);
    }

    render() {
        var {x,y,content,visible}= this.props;
        if (!visible) return false;
        if (!x && !y && !content) return false;
        return (
            <div style={{position:'absolute',left:0,top:0, visibility:'hidden' }}
                 onClick={() => console.log('drop down clicked')} >
                    <div style={{padding : 5}} className='ff-dropdown-menu'>
                        {content}
                    </div>
            </div>
        );
    }
}


DropDownMenuWrapper.propTypes= {
    visible : PropTypes.bool,
    x : PropTypes.number.isRequired,
    y : PropTypes.number.isRequired,
    content : PropTypes.object.isRequired
};
