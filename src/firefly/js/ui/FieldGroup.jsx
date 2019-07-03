/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import React, {Component} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';
import {dispatchMountFieldGroup, MOUNT_FIELD_GROUP} from '../fieldGroup/FieldGroupCntlr.js';
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
    if (action.type===MOUNT_FIELD_GROUP && !payload.mounted && payload.groupKey===params.props.groupKey) {
        doMountDispatch(params.props, params.wrapperGroupKey);
        cancelSelf();
    }
}


function doMountDispatch(props,wrapperGroupKey) {
    const {groupKey, reducerFunc, keepState, initValues, actionTypes}= props;
    dispatchMountFieldGroup(groupKey, true, keepState, initValues, reducerFunc, actionTypes, wrapperGroupKey);
}

function doMounting(props, wrapperGroupKey) {
    const groupState= getFieldGroupState(props.groupKey);
    if (groupState && groupState.mounted) {
        // as of react 16 mount happens before unmount:
        // if a mounted group is unmounted, I will delay the remount until the componentWillUnmount is called
        dispatchAddActionWatcher({
            actions:[MOUNT_FIELD_GROUP], callback:remountWatcher, params:{props, wrapperGroupKey}}
        );
    }
    else {
        doMountDispatch(props, wrapperGroupKey);
    }

}


export const GroupKeyCtx = React.createContext({});

export class FieldGroup extends Component {

    constructor(props, context) {
        super(props);
        const wrapperGroupKey= context;
        doMounting(props,wrapperGroupKey);
        this.firstMount= true;
    }

    shouldComponentUpdate(nextProps,nextState, nextContext) {// using this function to do a remount side effect
        if (shallowequal(this.props, nextProps)) return false;
        const wrapperGroupKey= nextContext;
        const {groupKey, reducerFunc, keepState, actionTypes}= nextProps;
        if (this.props.groupKey!==groupKey) {// support change the groupKey property on the form without unmounting
            dispatchMountFieldGroup(groupKey, false);
            dispatchMountFieldGroup(groupKey, true, keepState, null, reducerFunc, actionTypes, wrapperGroupKey);
        }
        return true;
    }

    componentDidMount() {
        if (this.firstMount) {
            this.firstMount= false;
        }
        else {
            const wrapperGroupKey= this.context;
            doMounting(this.props,wrapperGroupKey);
        }
    }

    componentWillUnmount() {
        if (!this.props.keepMounted) {
            dispatchMountFieldGroup(this.props.groupKey, false);
        }
    }

    render() {
        const {style, className, groupKey} = this.props;
        return (
            <GroupKeyCtx.Provider value={{groupKey}}>
                <div className={className} style={style} groupkey={groupKey}>
                    {this.props.children}
                </div>
            </GroupKeyCtx.Provider>
        );
    }
}

FieldGroup.contextType = GroupKeyCtx;

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


FieldGroup.defaultProps=  {
    reducerFunc: null,
    validatorFunc: null,
    keepState : false,
    keepMounted : false
};

