/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {delay, isFunction, isUndefined} from 'lodash';
import React, {useContext, useEffect} from 'react';
import {Box, Button} from '@mui/joy';
import {object, string, func, bool, oneOfType, arrayOf} from 'prop-types';
import {validateFieldGroup, getFieldGroupResults} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import {FieldGroupCtx} from './FieldGroup';


export const NONE='COMPLETE-BUTTON-NO-CONTEXT';

function inGroup(e, groupKey) {
    for(;e; e= e.parentElement) {
        if (e.getAttribute('groupkey')===groupKey) {
            return true;
        }
    }
    return false;
}


function addComponentData(inR={}, registeredComponents={}) {
    return Object.entries(registeredComponents).reduce((r, [key,funcOrData]) => {
        if (!isUndefined(funcOrData)) {
            r[key]= isFunction(funcOrData) ? funcOrData() : funcOrData;
        }
        return r;
    },{...inR});
}


function validUpdate(valid,onSuccess,onFail,registeredComponents, closeOnValid,groupKey,
                     dialogId, groupsToUse, includeUnmounted= false) {
    const funcToCall = valid ? onSuccess : onFail;
    let continueValid= true;

    if (Array.isArray(groupKey)) {
        const groupsToValidate= groupsToUse();
        const requestObj = groupsToValidate.reduce( (obj,groupKey) => {
            obj[groupKey]= getFieldGroupResults(groupKey,includeUnmounted);
            return obj;
        },{});
        if (funcToCall) funcToCall(addComponentData(requestObj,registeredComponents));
    }
    else {
        const request = getFieldGroupResults(groupKey,includeUnmounted);
        if (funcToCall) continueValid= funcToCall(addComponentData(request,registeredComponents));
    }

    const stillValid= valid && (continueValid ?? true);

    if (stillValid && dialogId && closeOnValid) dispatchHideDialog(dialogId);
}

function onClick(onSuccess,onFail,registeredComponents, closeOnValid,groupKey,
                 dialogId,groupsToUse, includeUnmounted, changeMasking, requireAllValid) {
    if (groupKey) {
        if (changeMasking) changeMasking(true);
        validateFieldGroup(groupsToUse(), includeUnmounted)
            .then( (valid)=> {
                if (changeMasking) changeMasking(false);
                validUpdate(valid||!requireAllValid,onSuccess,onFail,registeredComponents,closeOnValid,groupKey,dialogId, groupsToUse, includeUnmounted);
            });
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId && closeOnValid) dispatchHideDialog(dialogId);
    }
}




export function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK',
                          color=undefined, variant=undefined, requireAllValid=true,
                          closeOnValid=true, dialogId,includeUnmounted= false, primary=true,
                          groupsToUse= () => groupKey,
                          style={}, sx, changeMasking, fireOnEnter= false,
                                    getDoOnClickFunc}) {
    const {registeredComponents,groupKey:ctxGroupKey}= useContext(FieldGroupCtx);
    if (!groupKey && ctxGroupKey) groupKey= ctxGroupKey;
    if (groupKey===NONE) groupKey= undefined;
    const onComplete = () => onClick(onSuccess,onFail,registeredComponents,closeOnValid,groupKey,dialogId,
                                      groupsToUse, includeUnmounted,changeMasking, requireAllValid);


    useEffect(() => {
        if (fireOnEnter) {
            const keyCheck= (ev) => {
                if ( ev.key==='Enter' && ev.target.tagName==='INPUT' && inGroup(ev.target,groupKey)) {
                    delay( () => {
                        onComplete();
                    }, 100);
                }
            };
            document.addEventListener('keypress',keyCheck);
            return () => document.removeEventListener('keypress', keyCheck);
        }
    }, []);

    useEffect(() => {
        getDoOnClickFunc?.( () => onComplete());
    }, [getDoOnClickFunc]);


    return (
        <Box sx={sx} style={style}>
           <Button {...{className: 'ff-CompleteButton', size:'md',
                variant: variant? variant : primary?'solid':undefined,
                color: color? color : primary?'primary':undefined,
                onClick: onComplete}}>{text}</Button>
        </Box>
    );
}


CompleteButton.propTypes= {
    onFail: func,
    onSuccess: func,
    groupsToUse: func,
    groupKey: oneOfType([ string, arrayOf(string) ]),
    text: string,
    closeOnValid: bool,
    dialogId: string,
    style: object,
    sx: object,
    innerStyle: object,
    includeUnmounted : bool,
    changeMasking: func,
    fireOnEnter: bool,
    getDoOnClickFunc: func,
    variant: string,
    color: string,
};

export default CompleteButton;
