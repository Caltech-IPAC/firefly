/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';

export const TextButton = ({text, style, children, disabled, ...rest}) => {
    const val = text || children || '';
    const className = 'button text' + (disabled ? ' disabled' : '');
    return (
        <div className={className} style={style} {...{...rest}}>{val}</div>
    );
};

TextButton.propTypes= {
    text: PropTypes.string,
    style: PropTypes.object
};



