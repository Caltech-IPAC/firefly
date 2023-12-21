/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box} from '@mui/joy';
import {isUndefined} from 'lodash';
import React, {memo, useContext, useEffect, useLayoutEffect, useState} from 'react';
import PropTypes, {bool} from 'prop-types';
import {dispatchMountFieldGroup} from '../fieldGroup/FieldGroupCntlr.js';
import {
    getField, getFieldGroupState, getFieldVal, isFieldGroupMounted, makeFieldsObject, setField, setFieldValue
} from '../fieldGroup/FieldGroupUtils.js';

export const FieldGroupCtx = React.createContext({});
export const ForceValidCtx = React.createContext({forceValid:undefined});


function makeCtx(groupKey,register,unregister, wrapperRegisteredComponents, registeredComponents,keepState) {
    return {
        groupKey,
        register,
        unregister,
        keepState,
        registeredComponents:wrapperRegisteredComponents??registeredComponents,
        getVal: (key,def) => getFieldVal(groupKey,key, def),
        getFld: (key) => getField(groupKey,key),
        setVal: (key,value,fieldUpdates) => setFieldValue(groupKey,key,value,fieldUpdates),
        setFld: (key,fieldUpdates) => setField(groupKey,key,fieldUpdates),
        makeFldObj: (fieldNameAry) => makeFieldsObject(groupKey,fieldNameAry)
    };
}

export const FieldGroup = memo( ({keepMounted, reducerFunc=undefined, groupKey, keepState=false, children,
                                     sx, style, className}) => {
    const [, setFields]= useState(() => getFieldGroupState(groupKey));
    const [registeredComponents, setRegisteredComponents]= useState({});
    const {
        groupKey:wrapperGroupKey,
        register:wrapperRegister,
        unregister:wrapperUnregister,
        registeredComponents:wrapperRegisteredComponents}= useContext(FieldGroupCtx);


    const register= (key, f) => {
        if (wrapperRegister) {
            wrapperRegister(key,f);
        }
        else {
            registeredComponents[key]= f;
            setRegisteredComponents(registeredComponents);
        }
    };
    const unregister= (key) => {
        if (wrapperUnregister ) {
            wrapperUnregister(key);
        }
        else if (!isUndefined(registeredComponents[key])){
            // setRegisteredComponents({...registeredComponents,[key]:undefined});
            registeredComponents[key]= undefined;
            setRegisteredComponents(registeredComponents);
        }
    };
    const [ctx,setCtx]= useState(() => makeCtx(groupKey,register,unregister,wrapperRegisteredComponents,registeredComponents,keepState) );


    useEffect(() => {
            setCtx(makeCtx(groupKey,register,unregister,wrapperRegisteredComponents,registeredComponents,keepState));
        },[groupKey,wrapperGroupKey, wrapperRegister, wrapperUnregister,keepState]
    );

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
            <Box className={className} sx={sx} style={style} groupkey={groupKey}>
                {children}
            </Box>
        </FieldGroupCtx.Provider>
    );
});


FieldGroup.propTypes= {
    groupKey : PropTypes.string.isRequired,
    reducerFunc: PropTypes.func,
    keepState : PropTypes.bool,
    style : PropTypes.object,
    sx : PropTypes.object,
    keepMounted: PropTypes.bool,
    className: PropTypes.string
};

FieldGroup.contextType = FieldGroupCtx;



export const ForceFieldGroupValid = memo( ({forceValid=false, children}) => {
    return (
        <ForceValidCtx.Provider value={{forceValid}}>
                {children}
        </ForceValidCtx.Provider>
    );
});

ForceFieldGroupValid.propTypes= {
    forceValid: bool.isRequired
};




