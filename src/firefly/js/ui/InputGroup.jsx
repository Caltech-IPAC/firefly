/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';

export function InputGroup({labelWidth,children }) {
    return (
        <div>
            {React.Children.map(children,function(inChild) {
                return React.cloneElement(inChild, {labelWidth});
            })}
        </div>

    );
}

InputGroup.propTypes= {
    labelWidth   : React.PropTypes.number.isRequired
};


export default InputGroup;
