/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {dispatchMountFieldGroup, MOUNT_COMPONENT} from '../fieldGroup/FieldGroupCntlr.js';
import {getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';


/**
 * Watch for an unmount for a field group this is marked as mounted. Then we immediately do a mount.
 * @param action
 * @param cancelSelf
 * @param params
 */
function remountWatcher(action, cancelSelf, params) {
    const {payload}= action;
    if (action.type===MOUNT_COMPONENT && !payload.mounted && payload.groupKey===params.props.groupKey) {
        doMountDispatch(params.props, params.context);
        cancelSelf();
    }
}


function doMountDispatch(props,context) {
    const {groupKey:wrapperGroupKey}= context;
    const {groupKey, reducerFunc, keepState, initValues, actionTypes}= props;
    dispatchMountFieldGroup(groupKey, true, keepState, initValues, reducerFunc, actionTypes, wrapperGroupKey);
}




export class FieldGroup extends PureComponent {

    constructor(props, context) {
        super(props, context);
    }

    getChildContext() {
            return {groupKey: this.props.groupKey};
    }

    componentWillReceiveProps(nextProps,context) {
        const {groupKey:wrapperGroupKey}= context;
        const {groupKey, reducerFunc, keepState, actionTypes}= nextProps;
                       // support change the groupKey property on the form with out unmounting
        if (this.props.groupKey!==groupKey) {
            dispatchMountFieldGroup(groupKey, false);
            dispatchMountFieldGroup(groupKey, true, keepState, null, reducerFunc, actionTypes, wrapperGroupKey);
        }
    }

    componentWillMount() {
        const {groupKey}= this.props;
        const groupState= getFieldGroupState(groupKey);
        if (groupState && groupState.mounted) {
            // as of react 16 mount happens before unmount:
            // if a mounted group is unmounted, I will delay the remount until the componentWillUnmount is called
            dispatchAddActionWatcher({
                actions:[MOUNT_COMPONENT], callback:remountWatcher, params:{props:this.props, context:this.context}}
            );
        }
        else {
            doMountDispatch(this.props, this.context);
        }
    }

    componentWillUnmount() {
        if (!this.props.keepMounted) {
            dispatchMountFieldGroup(this.props.groupKey, false);
        }
    }

    render() {
        const {style, className} = this.props;
        return (
            <div className={className} style={style}>
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
    style : PropTypes.object,
    keepMounted: PropTypes.bool,
    className: PropTypes.string
};

FieldGroup.childContextTypes= {
    groupKey: PropTypes.string
};

FieldGroup.contextTypes = { groupKey: PropTypes.string };

FieldGroup.defaultProps=  {
    reducerFunc: null,
    validatorFunc: null,
    keepState : false,
    keepMounted : false
};

