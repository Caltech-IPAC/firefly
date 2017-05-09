import React from 'react';
import PropTypes from 'prop-types';
import {uniqueId} from 'lodash';
import InputFieldLabel from './InputFieldLabel.jsx';

const vStyle={paddingLeft: 3, paddingRight: 8};
const hStyle={paddingLeft: 0, paddingRight: 12};


function makeOptions(options,alignment ,value,onChange,tooltip) {

    const labelStyle= alignment==='vertical' ? vStyle : hStyle;
    return options.map((option) => (
        <span key={option.value}>
            <div style={{display:'inline-block'}} title={tooltip}>
                <input type='radio'
                       title={tooltip}
                       value={option.value}
                       checked={value===option.value}
                       onChange={onChange}
                /> <span style={labelStyle}>{option.label}</span>
            </div>
            {alignment==='vertical' ? <br/> : ''}
         </span>
    ));
}

export function RadioGroupInputFieldView({options,alignment,value,
                                          onChange,label,inline,tooltip,
                                          labelWidth, wrapperStyle={}}) {
    const style= Object.assign({whiteSpace:'nowrap',display: inline?'inline-block':'block'},wrapperStyle);
    const radioStyle = (alignment && alignment==='vertical') ? {display: 'block', marginTop: (label ? 10 : 0)}
                                                             : {display: 'inline-block'};

    return (
        <div style={style}>
            {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} /> }
            <div style={radioStyle} >
                {makeOptions(options,alignment,value,onChange,tooltip)}
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
    wrapperStyle: PropTypes.object
};

