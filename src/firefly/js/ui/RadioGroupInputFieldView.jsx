import React, {PropTypes}  from 'react';
import InputFieldLabel from './InputFieldLabel.jsx';



function makeOptions(options,alignment,fieldKey,value,onChange,tooltip) {
    return options.map((option) => (
        <span key={option.value}>
            <div style={{display:'inline-block'}} title={tooltip}>
                <input type='radio'
                       title={tooltip}
                       name={fieldKey}
                       value={option.value}
                       defaultChecked={value===option.value}
                       checked={value===option.value}
                       onChange={onChange}
                /> &nbsp;{option.label}&nbsp;&nbsp;
            </div>
            {alignment==='vertical' ? <br/> : ''}
         </span>
    ));
}

export function RadioGroupInputFieldView({options,alignment,fieldKey,value,
                                          onChange,label,inline,tooltip,
                                          labelWidth, wrapperStyle={}}) {
    const style= Object.assign({whiteSpace:'nowrap',display: inline?'inline-block':'block'},wrapperStyle);
    return (
        <div style={style}>
            {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} /> }
            <div style={{display:'inline-block'}} >
                {makeOptions(options,alignment,fieldKey,value,onChange,tooltip)}
            </div>
        </div>
    );
}

RadioGroupInputFieldView.propTypes= {
    options: PropTypes.array.isRequired,
    fieldKey: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    alignment:  PropTypes.string,
    onChange: PropTypes.func,
    label : PropTypes.string,
    tooltip : PropTypes.string,
    inline : PropTypes.bool,
    labelWidth : PropTypes.number,
    wrapperStyle: PropTypes.object
};

