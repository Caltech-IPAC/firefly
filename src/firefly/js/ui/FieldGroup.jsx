/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import FieldGroupStore from '../store/FieldGroupStore.js';
import FieldGroupActions from '../actions/FieldGroupActions.js';

var FieldGroup= React.createClass(
    {

        mixins : [React.addons.PureRenderMixin],

        propTypes: {
            groupKey : React.PropTypes.string.isRequired,
            reducerFunc: React.PropTypes.func,
            validatorFunc: React.PropTypes.func,
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
            var {groupKey, reducerFunc, validatorFunc, keepState}= this.props;
            FieldGroupActions.mountFieldGroup({groupKey, reducerFunc, validatorFunc, keepState, mounted:true})
        },

        componentWillUnmount() {
            var {groupKey}= this.props;
            FieldGroupActions.mountFieldGroup({groupKey, mounted:false})
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



