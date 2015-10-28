/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';

var FieldGroup= React.createClass(
    {

        mixins : [React.addons.PureRenderMixin],

        propTypes: {
            groupKey : React.PropTypes.string.isRequired,
            reducerFunc: React.PropTypes.func,
            keepState : React.PropTypes.bool
        },

        getDefaultProps() {
            return {
                reducerFunc: null,
                validatorFunc: null,
                keepState : false
            };
        },

        componentWillMount() {
            var {groupKey, reducerFunc, keepState}= this.props;
            FieldGroupUtils.mountFieldGroup(groupKey, reducerFunc, keepState);
        },

        componentWillUnmount() {
            var {groupKey}= this.props;
            FieldGroupUtils.unmountFieldGroup(groupKey);
        },

        render() {
            return (
                <div>
                    {this.props.children}
                </div>

            );
        }
    });

export default FieldGroup;



