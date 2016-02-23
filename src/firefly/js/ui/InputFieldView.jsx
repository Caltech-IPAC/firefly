import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import ReactDOM from 'react-dom';
import {PointerPopup} from '../ui/PointerPopup.jsx';
import InputFieldLabel from './InputFieldLabel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import './InputFieldView.css';
import EXCLAMATION from 'html/images/exclamation16x16.gif';




function computeStyle(valid,hasFocus) {
    if (!valid) {
        return 'ff-inputfield-view-error';
    }
    else {
        return hasFocus ? 'ff-inputfield-view-focus' : 'ff-inputfield-view-valid';
    }
}


function makeMessage(message) {
    return (
        <div>
            <img src={EXCLAMATION} style={{display:'inline-block', paddingRight:5}}/>
            <div style={{display:'inline-block'}}> {message} </div>
        </div>
    );
}

const makeInfoPopup = (mess,x,y) => <PointerPopup x={x} y={y} message={makeMessage(mess)}/>;


function computeWarningXY(warnIcon) {
    var bodyRect = document.body.getBoundingClientRect();
    var elemRect = warnIcon.getBoundingClientRect();
    var warningOffsetX = (elemRect.left - bodyRect.left) + warnIcon.offsetWidth / 2;
    var warningOffsetY = elemRect.top - bodyRect.top;
    return {warningOffsetX, warningOffsetY};
}

const ICON_SPACE_STYLE= {
    verticalAlign: 'middle',
    paddingLeft: 3,
    width: 16,
    height: 16,
    display:'inline-block'};


export class InputFieldView extends Component {
    constructor(props) {
        super(props);
        this.warnIcon = null;
        this.state = { hasFocus: false, infoPopup: false };
    }

    shouldComponentUpdate(np, ns) { return sCompare(this, np, ns); }


    componentDidUpdate() {
        var {infoPopup}= this.state;
        if (infoPopup) {
            var {warningOffsetX, warningOffsetY}= computeWarningXY(this.warnIcon);
            var {message}= this.props;
            this.hider = DialogRootContainer.showTmpPopup(makeInfoPopup(message, warningOffsetX, warningOffsetY));
        }
        else {
            if (this.hider) {
                this.hider();
                this.hider = null;
            }
        }
    }

    makeWarningArea(warn) {
        if (warn) {
            return (
                <div style={ICON_SPACE_STYLE}
                     onMouseOver={() => this.setState({infoPopup:true})}
                     onMouseLeave={() => this.setState({infoPopup:false})}>
                    <img src={EXCLAMATION} ref={(c) => this.warnIcon= c}/>
                </div>
            );
        }
        else {
            return <div style={ICON_SPACE_STYLE}/>;
        }
    }

    render() {
        var {hasFocus}= this.state;
        var {visible,label,tooltip,labelWidth,value,style,valid,size,onChange, onBlur, onKeyPress, showWarning, message, width}= this.props;
        if (!visible) return null;
        if (width) style = Object.assign({}, style, {width: '100%', boxSizing: 'border-box'});
        return (
            <div style={{whiteSpace:'nowrap', display: this.props.inline?'inline-block':'block', width} }>
                {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth}/> }
                <input style={Object.assign({display:'inline-block'}, style)}
                       className={computeStyle(valid,hasFocus)}
                       onChange={(ev) => onChange ? onChange(ev) : null}
                       onFocus={ () => !hasFocus ? this.setState({hasFocus:true, infoPopup:false}) : ''}
                       onBlur={ (ev) => {
                                onBlur && onBlur(ev);
                                this.setState({hasFocus:false, infoPopup:false});
                            }}
                       onKeyPress={(ev) => onKeyPress && onKeyPress(ev)}
                       value={value}
                       title={ (!showWarning && !valid) ? message : tooltip}
                       size={size}
                />
                {showWarning && this.makeWarningArea(!valid)}
            </div>
        );
    }
}

InputFieldView.propTypes= {
    valid   : PropTypes.bool,
    visible : PropTypes.bool,
    message : PropTypes.string,
    tooltip : PropTypes.string,
    label : PropTypes.string,
    inline : PropTypes.bool,
    labelWidth: PropTypes.number,
    style: PropTypes.object,
    value   : PropTypes.string.isRequired,
    size : PropTypes.number,
    onChange : PropTypes.func.isRequired,
    onBlur : PropTypes.func,
    onKeyPress : PropTypes.func,
    width: PropTypes.string,
    showWarning : PropTypes.bool
};

InputFieldView.defaultProps= {
    showWarning : true,
    valid : true,
    visible : true,
    message: ''
};

