import React, {PureComponent} from 'react';
import {pickBy} from 'lodash';
import {bool,string,number,object, func, oneOfType} from 'prop-types';
import {PointerPopup} from '../ui/PointerPopup.jsx';
import InputFieldLabel from './InputFieldLabel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import './InputFieldView.css';
import EXCLAMATION from 'html/images/exclamation16x16.gif';




function computeStyle(valid,hasFocus,readonly,connectedMarker) {
    if (!valid) {
        return 'ff-inputfield-view-error';
    }
    else if(readonly) {
        return 'ff-inputfield-view-readonly';
    }
    else if (connectedMarker) {
        return (hasFocus ? 'ff-inputfield-view-focus-no-bg' : 'ff-inputfield-view-valid-no-bg');
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
        const {infoPopup}= this.state;
        if (infoPopup && this.warnIcon) {
            const {warningOffsetX, warningOffsetY}= computeWarningXY(this.warnIcon);
            const {message}= this.props;
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
        const {hasFocus}= this.state;
        const {visible,disabled, label,tooltip,labelWidth,value,style,wrapperStyle,labelStyle, inputRef, autoFocus,
             valid,size,onChange, onBlur, onKeyPress, onKeyDown, onKeyUp, showWarning, connectedMarker=false,
            message, type, placeholder, readonly}= this.props;
        if (!visible) return null;
        const useWrapperStyle = {whiteSpace:'nowrap', display: this.props.inline?'inline-block':'block', ...wrapperStyle};
        let {form='__ignore'}= this.props;
        // form to relate this input field to.
        // assign form to null or empty-string to use it within a form tag (similar to input tag without form attribute).
        // if form is not given, it will default to __ignore so that it does not interfere with embedded forms.
        form = form || undefined;

        const currValue= (type==='file') ? undefined : value;

        const inputStyle= {display:'inline-block'};
        if (connectedMarker) inputStyle.backgroundColor= 'yellow';

        return (
            <div style={useWrapperStyle}>
                {label && <InputFieldLabel labelStyle={labelStyle} label={label} tooltip={tooltip} labelWidth={labelWidth}/> }
                <input style={{...inputStyle, ...style}}
                       className={computeStyle(valid,hasFocus, readonly,connectedMarker)}
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
                       disabled={readonly}
                       title={ (!showWarning && !valid) ? message : tooltip}
                       ref={inputRef}
                       {...pickBy({size, type, disabled, placeholder, form, autoFocus})}
                />
                {showWarning && this.makeWarningArea(!valid)}
            </div>
        );
    }
}

InputFieldView.propTypes= {
    valid   : bool,
    visible : bool,
    disabled : bool,
    message : string,
    tooltip : string,
    label : string,
    inline : bool,
    labelWidth: number,
    style: object,
    wrapperStyle: object,
    labelStyle: object,
    value   : oneOfType([string, number]).isRequired,
    size : number,
    onChange : func.isRequired,
    onBlur : func,
    onKeyPress : func,
    onKeyDown: func,
    onKeyUp: func,
    showWarning : bool,
    type: string,
    placeholder: string,
    form: string,
    connectedMarker: bool,
    readonly: bool
};

InputFieldView.defaultProps= {
    showWarning : true,
    valid : true,
    visible : true,
    message: '',
    type: 'text',
    readonly: false
};

export const propTypes = InputFieldView.propTypes;
