/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';



var InputGroup = React.createClass(
    {

        mixins : [PureRenderMixin],

        propTypes: {
            labelWidth   : React.PropTypes.number.isRequired
        },

        render() {
            var lWidth= this.props.labelWidth;
            return (
                <div>
                    {React.Children.map(this.props.children,function(inChild) {
                        return React.cloneElement(inChild, {labelWidth: lWidth});
                        //return React.addons.cloneWithProps(inChild, );
                    })}
                </div>

            );
        }

    });

export default InputGroup;
