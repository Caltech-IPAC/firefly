/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {dispatchMountFieldGroup,dispatchUnmountFieldGroup} from '../fieldGroup/FieldGroupCntlr.js';


export class FieldGroup extends Component {

    constructor(props, context) {
        super(props, context);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }
    getChildContext() {
            return {groupKey: this.props.groupKey};
    }

    componentWillReceiveProps(nextProps) {
        var {groupKey, reducerFunc, keepState}= nextProps;
                       // support change the groupKey property on the form with out unmounting
        if (this.props.groupKey!==groupKey) {   //todo: not quite sure how to test that this works
            dispatchMountFieldGroup(groupKey, false);
            dispatchMountFieldGroup(groupKey, true, keepState, null, reducerFunc);
        }
    }

    componentWillMount() {
        var {groupKey, reducerFunc, keepState, initValues}= this.props;
        dispatchMountFieldGroup(groupKey, true, keepState, initValues, reducerFunc);
    }

    componentWillUnmount() {
        dispatchMountFieldGroup(this.props.groupKey, false);
    }

    render() {
        return (
            <div>
                {this.props.children}
            </div>

        );
    }
}

FieldGroup.propTypes= {
    groupKey : PropTypes.string.isRequired,
    reducerFunc: PropTypes.func,
    keepState : PropTypes.bool,
    initValues : PropTypes.object
};

FieldGroup.childContextTypes= {
    groupKey: PropTypes.string
};

FieldGroup.defaultProps=  {
    reducerFunc: null,
    validatorFunc: null,
    keepState : false
};

