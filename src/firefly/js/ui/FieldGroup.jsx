/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isUndefined} from 'lodash';
import React, {memo, useContext, useEffect, useLayoutEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {dispatchMountFieldGroup} from '../fieldGroup/FieldGroupCntlr.js';
import {getFieldGroupState, isFieldGroupMounted} from '../fieldGroup/FieldGroupUtils.js';

export const FieldGroupCtx = React.createContext({});

export const FieldGroup = memo( ({keepMounted, reducerFunc=undefined, groupKey, keepState=false, children, style, className}) => {
    const [fields, setFields]= useState(() => getFieldGroupState(groupKey));
    const [registeredComponents, setRegisteredComponents]= useState({});
    const {
        groupKey:wrapperGroupKey,
        register:wrapperRegister,
        unregister:wrapperUnregister,
        registeredComponents:wrapperRegisteredComponents}= useContext(FieldGroupCtx);

    const register= (key, f) => {
        wrapperRegister ? wrapperRegister(key,f) : setRegisteredComponents({...registeredComponents,[key]:f});
    };
    const unregister= (key) => {
        if (wrapperUnregister ) {
            wrapperUnregister(key);
        }
        else if (!isUndefined(registeredComponents[key])){
            setRegisteredComponents({...registeredComponents,[key]:undefined});
        }
    };

    const ctx= {groupKey,register, unregister, registeredComponents:wrapperRegisteredComponents??registeredComponents};

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
        <FieldGroupCtx.Provider value={ctx}>
            <div className={className} style={style} groupkey={groupKey}>
                {children}
            </div>
        </FieldGroupCtx.Provider>
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

FieldGroup.contextType = FieldGroupCtx;
