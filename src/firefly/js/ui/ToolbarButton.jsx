/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import './ToolbarButton.css';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import {DROP_DOWN_KEY} from './DropDownToolbarButton.jsx';



export function makeBadge(cnt) {
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
    dropdownCB ? dropdownCB(divElement) : dispatchHideDialog(DROP_DOWN_KEY);
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
 * @param imageStyle
 * @param tipOnCB
 * @param tipOffCB
 * @param lastTextItem
 * @param additionalStyle
 * @param todo show a todo message
 * @param ctx
 * @return {object}
 */


export class ToolbarButton extends Component {
    constructor(props) {
        super(props);
        this.click= this.click.bind(this);
        this.mouseOver= this.mouseOver.bind(this);
        this.mouseOut= this.mouseOut.bind(this);
        this.setupRef= this.setupRef.bind(this);
    }
    
    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    click() {
        var { dropDownCB, onClick} = this.props;
        handleClick(onClick,dropDownCB,this.divElement);
    }

    mouseOver() {
        var {tipOnCB,tip} = this.props;
        if (!tipOnCB && this.context) tipOnCB= this.context.tipOnCB;
        if (tipOnCB) tipOnCB(tip);
    }

    mouseOut() {
        var {tipOffCB} = this.props;
        if (!tipOffCB && this.context) tipOffCB= this.context.tipOffCB;
        if (tipOffCB) tipOffCB();
    }

    setupRef(c) { this.divElement= c; }

    render() {
        const {
            icon,text,tip,badgeCount=0,enabled=true,
            horizontal=true, bgDark, visible=true, active,
            imageStyle, lastTextItem=false, todo, additionalStyle} = this.props;


        var s= { position: 'relative'};
        if (horizontal) {
            s.display='inline-block';
        }
        else {
            s.display= 'block';
        }

        var textCName= 'menuItemText';


        if (!visible) return <div style={s}></div>;
        var cName= `ff-MenuItem ${bgDark ? 'ff-MenuItem-dark' : 'ff-MenuItem-light'}`+
            ` ${enabled ? '' : 'ff-MenuItem-disabled'} ${active ? 'ff-MenuItem-active':''}`;

        if (horizontal && !icon) {
            s.height= 'calc(100% - 7px)';
            s.verticalAlign= 'bottom';
            s.fontSize= '10pt';
            s.position= 'relative';
            textCName= 'ff-menuItemHText';
            const topStyle= Object.assign({display:'inline-block', height:'100%', flex:'0 0 auto' },additionalStyle);
            return (
                <div style={topStyle}>
                    <div style={{ display:'inline-block',
                              margin:'0 4px 0 4px',
                              height: 'calc(100% - 7px)',
                              borderLeft : '1px solid rgba(0,0,0,.6)' }} />
                    <div title={tip} style={s} className={cName}
                         ref={this.setupRef}
                         onClick={this.click} onMouseOver={this.mouseOver} onMouseOut={this.mouseOut}>
                        <div className={textCName}>{text}</div>
                        {badgeCount ? makeBadge(badgeCount) : ''}
                        {todo?<div style={todoStyle}>ToDo</div>:false}
                    </div>

                    {lastTextItem ? <div style={{ display:'inline-block',
                                              margin:'0 4px 0 4px',
                                              height: 'calc(100% - 7px)',
                                              borderLeft : '1px solid rgba(0,0,0,.6)' }} /> : false}
                </div>
            );

        }
        else {
            s.flex= '0 0 auto';
            Object.assign(s,additionalStyle);
            return (
                <div title={tip} style={s} className={cName}
                     ref={this.setupRef}
                     onClick={this.click} onMouseOver={this.mouseOver} onMouseOut={this.mouseOut}>
                    {icon ? <img style={imageStyle} src={icon} />  : <div className={textCName}>{text}</div>}
                    {badgeCount ? makeBadge(badgeCount) : ''}
                    {todo?<div style={todoStyle}>ToDo</div>:false}
                </div>
            );
        }
    }
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
    dropDownCB : PropTypes.func,
    imageStyle : PropTypes.object,
    lastTextItem : PropTypes.boolean,
    additionalStyle : PropTypes.object
};

ToolbarButton.DefaultProps= {
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


export function ToolbarHorizontalSeparator({top=0}) {
    return <div style={{top}} className='ff-horizontal-separator'/>;
}

ToolbarButton.propTypes= {
    top : PropTypes.number
};

export function DropDownVerticalSeparator() {
    return <div className='ff-vertical-separator'/>;
}



