/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PropTypes} from 'react';
import './ToolbarButton.css';


function makeBadge(cnt) {
    var cName= `ff-badge ${cnt<10 ? 'badge-1-digit' : 'badge-2-digit'}`;
    return <div className={cName}>{Math.trunc(cnt)}</div>;
}


function makeImageButton(icon) {
   return  <img src={icon} />;
}

function makeTextButton(text) {
    return  <div className='menuItemText'>{text}</div>;
}


var todoStyle= {
    position : 'absolute',
    left  : 1,
    top  : 7,
    fontSize : '10px',
    color : 'white',
    background : 'rgba(245,0,255,.8)',
    borderRadius : '5px',
    whiteSpace : 'nowrap'

};


function makeToDo(todo) {
    if (!todo) return false;
    return  <div style={todoStyle}>ToDo</div>;
}

/**
 *
 * @param icon icon to display
 * @param text text to display, if icon specified, icon task precidents
 * @param tip tooltip
 * @param badgeCount if greater then 0 a badge is shown on the button
 * @param enabled if false, show faded view
 * @param dropDown drop down component that is attached
 * @param onClick function to call on click
 * @param horizontal lay out horizontal, if false lay out vertical
 * @param bgDark layout on a dark background, if false lay out on a light background
 * @param visible if false then don't show button
 * @param tipOnCB
 * @param tipOffCB
 * @param ctx
 * @return {object}
 */
export function ToolbarButton({icon,text,tip,badgeCount,enabled,dropDown,onClick,
                               horizontal, bgDark, visible,tipOnCB,tipOffCB,todo}, ctx) {

    if (!tipOnCB && ctx) tipOnCB= ctx.tipOnCB;
    if (!tipOffCB && ctx) tipOffCB= ctx.tipOffCB;

    var s= {
        display :  horizontal ? 'inline-block' : 'block',
        position: 'relative'
    };
    if (!visible) return <div style={s}></div>;
    var cName= `ff-MenuItem ${bgDark ? 'ff-MenuItem-dark' : 'ff-MenuItem-light'} ${enabled ? '' : 'ff-MenuItem-disabled'}`;


    return (
        <div title={tip} style={s} className={cName} onClick={onClick}
             onMouseOver={()=>tipOnCB(tip)} onMouseOut={tipOffCB}>
            {icon ? makeImageButton(icon) : makeTextButton(text)}
            {badgeCount ? makeBadge(badgeCount) : ''}
            {makeToDo(todo)}
        </div>
    );
}


ToolbarButton.contextTypes= {
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func
};


ToolbarButton.propTypes= {
    icon : React.PropTypes.string,
    text : React.PropTypes.string,
    tip : React.PropTypes.string,
    badgeCount : React.PropTypes.number,
    enabled : React.PropTypes.bool,
    bgDark: React.PropTypes.bool,
    todo: React.PropTypes.bool,
    useBorder : React.PropTypes.bool,
    dropDown : React.PropTypes.object,
    onClick : React.PropTypes.func,
    horizontal : React.PropTypes.bool,
    visible : React.PropTypes.bool,
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func
};

ToolbarButton.defaultProps= {
    text : 'EMPTY TEXT',
    badgeCount: 0,
    enabled : true,
    bgDark : true,
    useBorder : false,
    drawDown : null,
    horizontal : true,
    todo : false,
    visible : true

};



export function ToolbarHorizontalSeparator() {
    return <div className='ff-horizontal-separator'/>;
}



