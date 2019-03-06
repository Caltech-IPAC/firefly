import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {pickBy} from 'lodash';

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
        <div style={{whiteSpace:'nowrap'}}>
            <img src={EXCLAMATION} style={{display:'inline-block', paddingRight:5}}/>
            <div style={{display:'inline-block'}}> {message} </div>
        </div>
    );
}

const makeInfoPopup = (mess,x,y) => <PointerPopup x={x} y={y} message={makeMessage(mess)}/>;


function computeWarningXY(warnIcon) {
    var bodyRect = document.body.getBoundingClientRect();
    if (!warnIcon) return {};
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


export class InputFieldView extends PureComponent {
    constructor(props) {
        super(props);
        this.warnIcon = null;
        this.state = { hasFocus: false, infoPopup: false };
    }

    componentDidUpdate() {
        var {infoPopup}= this.state;
        if (infoPopup && this.warnIcon) {
            var {warningOffsetX, warningOffsetY}= computeWarningXY(this.warnIcon);
            var {message}= this.props;
            if (this.hider) this.hider();
            this.hider = DialogRootContainer.showTmpPopup(makeInfoPopup(message, warningOffsetX, warningOffsetY));
        }
        else if (this.hider) {
            this.hider();
            this.hider = null;
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
        var {visible,disabled, label,tooltip,labelWidth,value,style,wrapperStyle,labelStyle,
             valid,size,onChange, onBlur, onKeyPress, onKeyDown, onKeyUp, showWarning, message, type, placeholder, form='__ignore'}= this.props;
        if (!visible) return null;
        wrapperStyle = Object.assign({whiteSpace:'nowrap', display: this.props.inline?'inline-block':'block'}, wrapperStyle);
        // form to relate this input field to.
        // assign form to null or empty-string to use it within a form tag (similar to input tag without form attribute).
        // if form is not given, it will default to __ignore so that it does not interfere with embedded forms.
        form = form || undefined;

        const currValue= (type==='file') ? undefined : value;

        return (
            <div style={wrapperStyle}>
                {label && <InputFieldLabel labelStyle={labelStyle} label={label} tooltip={tooltip} labelWidth={labelWidth}/> }
                <input style={Object.assign({display:'inline-block'}, style)}
                       className={computeStyle(valid,hasFocus)}
                       onChange={(ev) => onChange ? onChange(ev) : null}
                       onFocus={ () => !hasFocus ? this.setState({hasFocus:true, infoPopup:false}) : ''}
                       onBlur={ (ev) => {
                                onBlur && onBlur(ev);
                                this.setState({hasFocus:false, infoPopup:false});
                            }}
                       onKeyPress={(ev) => onKeyPress && onKeyPress(ev,currValue)}
                       onKeyDown={(ev) => onKeyDown && onKeyDown(ev,currValue)}
                       onKeyUp={(ev) => onKeyUp && onKeyUp(ev)}
                       value={currValue}
                       title={ (!showWarning && !valid) ? message : tooltip}
                       {...pickBy({size, type, disabled, placeholder, form})}
                />
                {showWarning && this.makeWarningArea(!valid)}
            </div>
        );
    }
}

InputFieldView.propTypes= {
    valid   : PropTypes.bool,
    visible : PropTypes.bool,
    disabled : PropTypes.bool,
    message : PropTypes.string,
    tooltip : PropTypes.string,
    label : PropTypes.string,
    inline : PropTypes.bool,
    labelWidth: PropTypes.number,
    style: PropTypes.object,
    wrapperStyle: PropTypes.object,
    labelStyle: PropTypes.object,
    value   : PropTypes.string.isRequired,
    size : PropTypes.number,
    onChange : PropTypes.func.isRequired,
    onBlur : PropTypes.func,
    onKeyPress : PropTypes.func,
    onKeyDown: PropTypes.func,
    onKeyUp: PropTypes.func,
    showWarning : PropTypes.bool,
    type: PropTypes.string,
    placeholder: PropTypes.string,
    form: PropTypes.string
};

InputFieldView.defaultProps= {
    showWarning : true,
    valid : true,
    visible : true,
    message: '',
    type: 'text'
};

export const propTypes = InputFieldView.propTypes;
