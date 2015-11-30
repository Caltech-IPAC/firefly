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
var InputFieldLabel= function( { label, tooltip, labelStyle, labelWidth=200, } ) {

    var currStyle = labelStyle || { display:'inline-block', paddingRight:'4px' };
    currStyle.width = labelWidth;
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
    tooltip : React.PropTypes.string.isRequired,
};

export default InputFieldLabel;

