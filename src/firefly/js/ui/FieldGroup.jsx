/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component, memo, useContext, useEffect, useLayoutEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {dispatchMountFieldGroup} from '../fieldGroup/FieldGroupCntlr.js';
import {getFieldGroupState, isFieldGroupMounted} from '../fieldGroup/FieldGroupUtils.js';

export const GroupKeyCtx = React.createContext({});
export const FieldGroup = memo( ({keepMounted, reducerFunc=undefined, groupKey, keepState=false, children, style, className}) => {
        const [fields, setFields]= useState(() => getFieldGroupState(groupKey));
        const {groupKey:wrapperGroupKey}= useContext(GroupKeyCtx);
        useLayoutEffect(() => {
            dispatchMountFieldGroup(groupKey, true, keepState, reducerFunc, wrapperGroupKey);
            return () => {
                if (!keepMounted) dispatchMountFieldGroup(groupKey, false);
            };
        }, [groupKey, wrapperGroupKey, reducerFunc?.ver]);

        useEffect(() => {
            if (isFieldGroupMounted(groupKey)) {
                dispatchMountFieldGroup(groupKey, true, keepState, reducerFunc, wrapperGroupKey);
            }
        },[keepState, keepMounted, reducerFunc]);

        useEffect(() => {
            if (reducerFunc) setFields(getFieldGroupState(groupKey));
        },[]);
        
        return (
            <GroupKeyCtx.Provider value={{groupKey}}>
                <div className={className} style={style} groupkey={groupKey}>
                    {children}
                </div>
            </GroupKeyCtx.Provider>
        );
    });


FieldGroup.propTypes= {
    groupKey : PropTypes.string.isRequired,
    reducerFunc: PropTypes.func,
    keepState : PropTypes.bool,
    style : PropTypes.object,
    keepMounted: PropTypes.bool,
    className: PropTypes.string
};

FieldGroup.contextType = GroupKeyCtx;
