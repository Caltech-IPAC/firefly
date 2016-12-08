import React from 'react';

/**
 *
 * @param label
 * @param tooltip
 * @param labelStyle
 * @param labelWidth
 * @return {XML}
 * @constructor
 */
export const InputFieldLabel= function( { label= '', tooltip= '', labelStyle, labelWidth=200, } ) {

    //var currStyle = labelStyle || { display:'inline-block', paddingRight:'4px' };
    var currStyle = Object.assign({ display:'inline-block', paddingRight:'4px' }, labelStyle);

    if (labelWidth > 0) {
        currStyle.width = labelWidth;
    }

    return (
        <div style={currStyle} title={tooltip} className={'disable-select'} >
            {label}
        </div>
    );
};


InputFieldLabel.propTypes= {
    label : React.PropTypes.string.isRequired,
    labelStyle : React.PropTypes.object,
    labelWidth : React.PropTypes.number,
    tooltip : React.PropTypes.string,
};

export default InputFieldLabel;

