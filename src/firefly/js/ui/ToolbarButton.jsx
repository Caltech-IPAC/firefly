/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import './ToolbarButton.css';
import {DropDownMenuWrapper} from './DropDownMenu.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {DROP_DOWN_KEY} from './DropDownToolbarButton.jsx';



function makeBadge(cnt) {
    var cName= `ff-badge ${cnt<10 ? 'badge-1-digit' : 'badge-2-digit'}`;
    return <div className={cName}>{Math.trunc(cnt)}</div>;
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




function handleClick(onClick, dropdownCB ,divElement) {
    if (onClick) onClick();
    dropdownCB ? dropdownCB(divElement) : AppDataCntlr.hideDialog(DROP_DOWN_KEY);
}





/**
 *
 * @param icon icon to display
 * @param text text to display, if icon specified, icon task precidents
 * @param tip tooltip
 * @param badgeCount if greater then 0 a badge is shown on the button
 * @param enabled if false, show faded view
 * @param dropDownCB callback for the dropdown, will pass the div element
 * @param onClick function to call on click
 * @param horizontal lay out horizontal, if false lay out vertical
 * @param bgDark layout on a dark background, if false lay out on a light background
 * @param visible if false then don't show button
 * @param active
 * @param tipOnCB
 * @param tipOffCB
 * @param todo show a todo message
 * @param ctx
 * @return {object}
 */
export function ToolbarButton({icon,text,tip,badgeCount,enabled,dropDownCB,
                               onClick, horizontal, bgDark, visible, active,
                               tipOnCB,tipOffCB,todo}, ctx) {

    if (!tipOnCB && ctx) tipOnCB= ctx.tipOnCB;
    if (!tipOffCB && ctx) tipOffCB= ctx.tipOffCB;

    var s= {
        display :  horizontal ? 'inline-block' : 'block',
        position: 'relative'
    };
    if (!visible) return <div style={s}></div>;
    var cName= `ff-MenuItem ${bgDark ? 'ff-MenuItem-dark' : 'ff-MenuItem-light'}`+
           ` ${enabled ? '' : 'ff-MenuItem-disabled'} ${active ? 'ff-MenuItem-active':''}`;
    var divElement;

    return (
        <div title={tip} style={s} className={cName}
             ref={(c) => divElement= c}
             onClick={() => handleClick(onClick,dropDownCB,divElement)}
             onMouseOver={()=>tipOnCB?tipOnCB(tip):false} onMouseOut={tipOffCB}>
            {icon ? <img src={icon} />  : <div className='menuItemText'>{text}</div>}
            {badgeCount ? makeBadge(badgeCount) : ''}
            {todo?<div style={todoStyle}>ToDo</div>:false}
        </div>
    );
}


ToolbarButton.contextTypes= {
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func
};


ToolbarButton.propTypes= {
    icon : PropTypes.string,
    text : PropTypes.string,
    tip : PropTypes.string,
    badgeCount : PropTypes.number,
    enabled : PropTypes.bool,
    bgDark: PropTypes.bool,
    todo: PropTypes.bool,
    useBorder : PropTypes.bool,
    onClick : PropTypes.func,
    horizontal : PropTypes.bool,
    visible : PropTypes.bool,
    active : PropTypes.bool,
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func,
    dropDownCB : PropTypes.func
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
    hideDropDowns: false,
    visible : true
};


export function ToolbarHorizontalSeparator() {
    return <div className='ff-horizontal-separator'/>;
}


export function DropDownVerticalSeparator() {
    return <div className='ff-vertical-separator'/>;
}



