/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';



var InputGroup = React.createClass(
    {

        mixins : [React.addons.PureRenderMixin],

        propTypes: {
            labelWidth   : React.PropTypes.number.isRequired
        },

        render: function() {
            /*jshint ignore:start */
            var lWidth= this.props.labelWidth;
            return (
                <div>
                    {React.Children.map(this.props.children,function(inChild) {
                        return React.cloneElement(inChild, {labelWidth: lWidth});
                        //return React.addons.cloneWithProps(inChild, );
                    })}
                </div>

            );
            /*jshint ignore:end */
        }


    });

export default InputGroup;
