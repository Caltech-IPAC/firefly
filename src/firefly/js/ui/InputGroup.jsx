/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

export function InputGroup({labelWidth,children, className, style }) {
    const elements =  React.Children.map(children,function(inChild) {
                if (get(inChild, 'type.propTypes.labelWidth')) {
                    return React.cloneElement(inChild, {labelWidth});
                } else {
                    return inChild;
                }
            });
    return (
        <div className={className} style={style}>{elements}</div>
    );
}

InputGroup.propTypes= {
    labelWidth   : PropTypes.number.isRequired,
    className : PropTypes.string,
    style : PropTypes.object
};


export default InputGroup;
