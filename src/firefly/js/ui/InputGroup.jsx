/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

export function InputGroup({labelWidth,children, verticalSpace=5 }) {
    const elements =  React.Children.map(children,function(inChild) {
                if (get(inChild, 'type.propTypes.labelWidth')) {
                    return React.cloneElement(inChild, {labelWidth});
                } else {
                    return inChild;
                }
            });
    return (
        <div>{elements}</div>
    );
}

InputGroup.propTypes= {
    labelWidth   : PropTypes.number.isRequired,
    verticalSpace : PropTypes.number,
};


export default InputGroup;
