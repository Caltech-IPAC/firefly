import React, {forwardRef, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {PointerPopup} from '../ui/PointerPopup.jsx';
import {InputFieldLabel} from './InputFieldLabel.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import './InputAreaFieldView.css';
import EXCLAMATION from 'html/images/exclamation16x16.gif';




function computeStyle(valid,hasFocus,additionalClasses) {
    const extraClasses = ' ' + (additionalClasses || '');
    if (!valid) {
        return 'ff-inputfield-view-error' + extraClasses;
    }
    else {
        return (hasFocus ? 'ff-inputfield-view-focus' : 'ff-inputfield-view-valid') + extraClasses;
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
    const bodyRect = document.body.getBoundingClientRect();
    const elemRect = warnIcon.getBoundingClientRect();
    const warningOffsetX = (elemRect.left - bodyRect.left) + warnIcon.offsetWidth / 2;
    const warningOffsetY = elemRect.top - bodyRect.top;
    return {warningOffsetX, warningOffsetY};
}

const ICON_SPACE_STYLE= {
    verticalAlign: 'middle',
    paddingLeft: 3,
    width: 16,
    height: 16,
    display:'inline-block'};


export const InputAreaFieldView= forwardRef( ({ visible,label,tooltip,rows,cols,labelWidth,value,style,wrapperStyle,labelStyle,
                                       button,inline, valid,onChange, onBlur, onKeyPress, showWarning,
                                       message, type, placeholder, additionalClasses, idName },ref ) => {
    const [hasFocus,setHasFocus]= useState(false);
    const [infoPopup,setInfoPopup]= useState(false);
    const {current:warn}= useRef({warnIcon:undefined, hider:undefined});

    useEffect(()=> {
        if (infoPopup) {
            const {warningOffsetX, warningOffsetY}= computeWarningXY(warn.warnIcon);
            warn.hider = DialogRootContainer.showTmpPopup(makeInfoPopup(message, warningOffsetX, warningOffsetY));
        }
        else if (warn.hider) {
                warn.hider();
                warn.hider = null;
        }
    }, [infoPopup]);

        if (!visible) return null;

    const warningArea= showWarning ?
            !valid ? (
                    <div style={ICON_SPACE_STYLE}
                         onMouseOver={() => setInfoPopup(true)}
                         onMouseLeave={() => setInfoPopup(false)}>
                        <img src={EXCLAMATION} ref={(c) => warn.warnIcon= c}/>
                    </div> )
                : (<div style={ICON_SPACE_STYLE}/>) : false;

    return (
        <div style={{whiteSpace:'nowrap', display: inline?'inline-block':'block', ...wrapperStyle}} ref={ref}>
            {label && <InputFieldLabel labelStyle={labelStyle} label={label} tooltip={tooltip} labelWidth={labelWidth}/> }
            <textarea style={{display:'inline-block', ...style}}
                      rows={rows}
                      cols={cols}
                      spellCheck={false}
                      className={computeStyle(valid,hasFocus,additionalClasses)}
                      id={idName}
                      onChange={(ev) => onChange?.(ev)}
                      onFocus={ () => {
                          if (hasFocus) {
                              setHasFocus(true);
                              setInfoPopup(false);
                          }
                      }}
                      onBlur={ (ev) => {
                          onBlur?.(ev);
                          setHasFocus(false);
                          setInfoPopup(false);
                      }}
                      onKeyPress={(ev) => onKeyPress?.(ev)}
                      value={type==='file' ? undefined : value}
                      title={ (!showWarning && !valid) ? message : tooltip}
                      placeholder={placeholder}
            />
            {warningArea}
            {Boolean(button) && button}
        </div>
    );
});

InputAreaFieldView.propTypes= {
    valid   : PropTypes.bool,
    visible : PropTypes.bool,
    message : PropTypes.string,
    tooltip : PropTypes.string,
    label : PropTypes.string,
    inline : PropTypes.bool,
    labelWidth: PropTypes.number,
    style: PropTypes.object,
    labelStyle: PropTypes.object,
    wrapperStyle: PropTypes.object,
    value   : PropTypes.string.isRequired,
    onChange : PropTypes.func.isRequired,
    onBlur : PropTypes.func,
    onKeyPress : PropTypes.func,
    showWarning : PropTypes.bool,
    rows: PropTypes.number,
    cols: PropTypes.number,
    placeholder: PropTypes.string,
    additionalClasses: PropTypes.string,
    idName: PropTypes.string
};

InputAreaFieldView.defaultProps= {
    showWarning : true,
    valid : true,
    visible : true,
    message: '',
    rows:10,
    cols:50,
    idName: ''
};

export const propTypes = InputAreaFieldView.propTypes;
