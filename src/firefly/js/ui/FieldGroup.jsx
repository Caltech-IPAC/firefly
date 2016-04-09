/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {dispatchMountFieldGroup} from '../fieldGroup/FieldGroupCntlr.js';


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

    componentWillReceiveProps(nextProps,context) {
        const {groupKey:wrapperGroupKey}= context;
        const {groupKey, reducerFunc, keepState, actionTypes}= nextProps;
                       // support change the groupKey property on the form with out unmounting
        if (this.props.groupKey!==groupKey) {   //todo: not quite sure how to test that this works
            dispatchMountFieldGroup(groupKey, false);
            dispatchMountFieldGroup(groupKey, true, keepState, null, reducerFunc, actionTypes, wrapperGroupKey);
        }
    }

    componentWillMount() {
        const {groupKey:wrapperGroupKey}= this.context;
        var {groupKey, reducerFunc, keepState, initValues, actionTypes}= this.props;
        dispatchMountFieldGroup(groupKey, true, keepState, initValues, reducerFunc, actionTypes, wrapperGroupKey);
    }

    componentWillUnmount() {
        dispatchMountFieldGroup(this.props.groupKey, false);
    }

    render() {
        const s= Object.assign({},this.props.style);
        return (
            <div style={s}>
                {this.props.children}
            </div>

        );
    }
}

FieldGroup.propTypes= {
    groupKey : PropTypes.string.isRequired,
    reducerFunc: PropTypes.func,
    actionTypes: PropTypes.arrayOf(PropTypes.string),
    keepState : PropTypes.bool,
    initValues : PropTypes.object,
    style : PropTypes.object
};

FieldGroup.childContextTypes= {
    groupKey: PropTypes.string
};

FieldGroup.contextTypes = { groupKey: PropTypes.string };

FieldGroup.defaultProps=  {
    reducerFunc: null,
    validatorFunc: null,
    keepState : false
};

