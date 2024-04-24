/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Arrays, {delay, isFunction, isUndefined} from 'lodash';
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


async function collectInputs(registeredComponents, groupKey, includeUnmounted= false) {

    if (!groupKey) return [{}, true];   // if no groupKey, then no parameters to collect and therefore pass validation

    const groups = isFunction(groupKey) ? groupKey() : groupKey;
    const valid = await validateFieldGroup(groups, includeUnmounted);

    let inputs;
    if (Arrays.isArray(groups)) {
        inputs = groups.reduce( (obj,gkey) => {
            obj[gkey]= getFieldGroupResults(gkey,includeUnmounted);
            return obj;
        },{});
    }
    else {
        inputs = getFieldGroupResults(groups,includeUnmounted);
    }
    return [addComponentData(inputs,registeredComponents), valid];
}


async function onClick(onSuccess, onFail, registeredComponents, closeOnValid, groupKey,
                       dialogId, includeUnmounted, changeMasking, requireAllValid) {

    changeMasking?.(true);
    const [inputs, valid] = await collectInputs(registeredComponents, groupKey, includeUnmounted);
    changeMasking?.(false);

    const passValidation = valid || !requireAllValid;
    const funcToCall = (passValidation) ? onSuccess : onFail;
    const continueValid = funcToCall?.(inputs, passValidation) ?? true;
    if (continueValid && dialogId && closeOnValid) dispatchHideDialog(dialogId);
}


export function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK',
                          color=undefined, variant=undefined, requireAllValid=true,
                          closeOnValid=true, dialogId,includeUnmounted= false, primary=true,
                          style={}, sx, changeMasking, fireOnEnter= false,
                                    getDoOnClickFunc}) {
    const {registeredComponents,groupKey:ctxGroupKey}= useContext(FieldGroupCtx);
    if (!groupKey && ctxGroupKey) groupKey= ctxGroupKey;
    if (groupKey===NONE) groupKey= undefined;
    const onComplete = () => onClick(onSuccess,onFail,registeredComponents,closeOnValid,groupKey,dialogId,
                                      includeUnmounted,changeMasking, requireAllValid);


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
    groupKey: oneOfType([ string, arrayOf(string), func ]),  // when func, it returns array of string
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
