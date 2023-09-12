import React from 'react';
import PropTypes from 'prop-types';
import InputFieldLabel from './InputFieldLabel.jsx';
import './ButtonGroup.css';


function makeRadioGroup(options,alignment,value,onChange,tooltip,labelStyle) {

    return options.map((option) => (
        <span key={option.value}>
            <div style={{display:'inline-flex', flexDirection:'row', alignItems:'center'}} title={option.tooltip || tooltip}>
                <input type='radio'
                       title={tooltip}
                       value={option.value}
                       checked={value===option.value}
                       onChange={onChange}
                       disabled={option.disabled || false}
                /> <span style={labelStyle ? labelStyle : generateStyles(alignment, option)}>{option.label}</span>
            </div>
            {alignment==='vertical' ? <br/> : ''}
         </span>
    ));
}

function generateStyles(alignment, option){
    const vStyle={paddingLeft: 3, paddingRight: 8};
    const hStyle={paddingLeft: 0, paddingRight: 12};
    const style = alignment==='vertical' ? vStyle : hStyle;

    return option.disabled ? Object.assign(style, {opacity: 0.5}) : style;
}

const startR= '4px 0 0 4px';
const midR= '0';
const endR= '0 4px 4px 0';

function makeButtonGroup(options,value,onChange, tooltip,style) {

    return options.map((option,idx) => (
        <button type='button'   key={'' + idx} title={option.tooltip}
                style={{borderRadius: idx===0?startR : idx===options.length-1 ? endR : midR, ...style,  }}
                className={value===option.value ? 'buttonGroupButton On' : 'buttonGroupButton Off'}
                value={option.value}
                disabled={option.disabled || false}
                onClick={(ev) => option.value!==value && onChange(ev)}>
            {option.icon ?
                <img src={option.icon} alt={options.label} /> :
                 option.label
            }
        </button>
    ));
}




export function RadioGroupInputFieldView({options,alignment,value,
                                             onChange,label,inline,tooltip,
                                             buttonGroup= false,
                                             buttonGroupButtonStyle= undefined,
                                             labelWidth, wrapperStyle={}, labelStyle=undefined}) {

    const style= Object.assign({whiteSpace:'nowrap',display: inline?'inline-block':'block'},wrapperStyle);
    let innerStyle;
    if (buttonGroup) {
        innerStyle= {display: 'flex'};
    }
    else {
        innerStyle = (alignment==='vertical') ? {display: 'block', marginTop: (label ? 10 : 0)}
            : {display: 'inline-block'};
    }

    return (
        <div style={style}>
            {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} labelStyle={labelStyle}/> }
            <div style={innerStyle} >
                {buttonGroup ?
                     makeButtonGroup(options,value,onChange,tooltip,buttonGroupButtonStyle) :
                     makeRadioGroup(options,alignment,value,onChange,tooltip,labelStyle)}
            </div>
        </div>
    );
}

RadioGroupInputFieldView.propTypes= {
    options: PropTypes.array.isRequired,
    value: PropTypes.string.isRequired,
    alignment:  PropTypes.string,
    onChange: PropTypes.func,
    label : PropTypes.string,
    tooltip : PropTypes.string,
    inline : PropTypes.bool,
    labelWidth : PropTypes.number,
    wrapperStyle: PropTypes.object,
    labelStyle: PropTypes.object,
    buttonGroup : PropTypes.bool,
};

