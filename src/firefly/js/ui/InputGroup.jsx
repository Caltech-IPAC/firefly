/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';

export function InputGroup({labelWidth,children, verticalSpace=5 }) {
    return (
        <div>
            {React.Children.map(children,function(inChild) {
                return (
                    <div style={{paddingBottom:verticalSpace }}>
                        { inChild && React.cloneElement(inChild, {labelWidth})}
                    </div>
                );
            })}
        </div>

    );
}

InputGroup.propTypes= {
    labelWidth   : PropTypes.number.isRequired,
    verticalSpace : PropTypes.number,
};


export default InputGroup;
