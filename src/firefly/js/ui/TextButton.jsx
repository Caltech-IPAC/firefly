/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import PropTypes from 'prop-types';

export const TextButton = ({text, style, children, ...rest}) => {
    const val = text || children || '';
    return (
        <div className='button text' style={style} {...{...rest}}>{val}</div>
    );
};

TextButton.propTypes= {
    text: PropTypes.string,
    style: PropTypes.object
};



