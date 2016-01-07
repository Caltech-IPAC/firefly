import React from 'react';



function makeOptions(options,alignment,fieldKey,value,onChange) {
    return options.map((option) => (
        <span key={option.value}>
            <div style={{display:'inline-block'}}>
                <input type='radio'
                       name={fieldKey}
                       value={option.value}
                       defaultChecked={value===option.value}
                       onChange={onChange}
                /> &nbsp;{option.label}&nbsp;&nbsp;
            </div>
            {alignment==='vertical' ? <br/> : ''}
         </span>
    ));
}

function RadioGroupInputFieldView({options,alignment,fieldKey,value,onChange}) {
    return (
        <div>
            {makeOptions(options,alignment,fieldKey,value,onChange)}
        </div>
    );
}

RadioGroupInputFieldView.propTypes= {
    options: React.PropTypes.array.isRequired,
    fieldKey: React.PropTypes.string.isRequired,
    value: React.PropTypes.string.isRequired,
    alignment:  React.PropTypes.string,
    onChange: React.PropTypes.func
};

export default RadioGroupInputFieldView;

