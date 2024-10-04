import React from 'react';
import {isEmpty, uniqueId} from 'lodash';
import {useCallback, useContext, useEffect, useState, useTransition} from 'react';
import shallowequal from 'shallowequal';
import {flux} from '../core/ReduxFlux.js';
import FieldGroupUtils, {
    getField, getFieldsForKeys, getFieldVal, getGroupFields, getMetaState, makeFieldsObject, setFieldValue
} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from 'firefly/core/MasterSaga.js';
import {dispatchMetaStateChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {FieldGroupCtx} from './FieldGroup.jsx';
import {smartMerge} from 'firefly/tables/TableUtil.js';


/**
 * This function make use of useState and useEffect to
 * trigger a re-render of functional components when the value in the store changes.
 *
 * By default, this will call shallowequal to ensure the state has changed before
 * calling setState.  To override this behavior, use useStoreConnector.bind({comparator: your_compare_function}).
 *
 * @param {function} stateGetter  a getter function returning a state, when called the oldState is passed as a parameter.
 * This allows a stateGetter to optionally act as a comparator.  If the stateGetter does not care about the changes
 * then it can return the oldState and there will be no state update.
 * @param {Array} deps array of dependencies used by stateGetter
 * @param {boolean} markAsTransition if try then set the state with startTransition
 * @returns {Object}  new state's value
 */
export function useStoreConnector(stateGetter, deps=[], markAsTransition=false) {
    const {comparator=shallowequal} = this || {};

    const [val, setter] = useState(stateGetter());
    const [isPending,startTransition]= useTransition();

    let isMounted = true;
    useEffect(() => {
        let cState = val;
        const remover = flux.addListener(() => {
            if (isMounted) {
                const nState = stateGetter(cState);      // if getter returns oldState then no state update
                if (nState===cState) return;             // comparator might be overridden, use === first for efficiency
                if ( !comparator(cState, nState) ) {
                    cState = nState;
                    if (markAsTransition) {
                        startTransition(() =>{
                           setter(cState);
                        });
                    }
                    else {
                        setter(cState);
                    }
                }
            }
        });
        return () => {
            isMounted = false;
            remover && remover();
        };
    }, deps);     // defaults to run only once

    return val;
}


/**
 * @deprecated
 * The better approach is to use useFieldGroupValue
 * @see useFieldGroupValue
 * @param groupKey
 * @return {*|undefined}
 */
export function useBindFieldGroupToStore(groupKey) {
    let mounted= true;
    const [fields, setFields] = useState(() => getGroupFields(groupKey));
    useEffect(() => {
        const remover= FieldGroupUtils.bindToStore(groupKey, (f) => {
            if (!shallowequal(f,fields) && mounted) setFields(f);
        });
        return () => {
            mounted= false;
            remover();
        };
    },[]);
    return fields || undefined;
}


/**
 * This hook will return a setter and a getter function for a field value.
 * Note - The setter and getter functions are static until the field value has changed, they are created with react
 * useCallback. Therefor they can be used in useEffects array of values dependencies.
 * @param {String} fieldKey - a fieldKey to check for
 * @param {String} [gk] - the groupKey if not set then it is retrieved from context, the normal use is to not pass this parameter
 * @return {Array.<Function>}  return an array of 2 functions [getValue,setValue].
 * getValue(true) will return the whole field while getValue() will just return the fields value.
 * setValue will take up two arguments. setValue(value, {field group changes}). The first arguments is the new field value.
 * The second optional augment is an object with any other property of the field group. Such as-
 * setValue(4,{value:false, message: '4 is not valid'}
 */
export function useFieldGroupValue(fieldKey, gk) {
    const context= useContext(FieldGroupCtx);
    const groupKey= gk || context.groupKey;
    const setValueToState= useState(undefined)[1]; // use state here is just to force re-renders on value change
    let mounted= true;
    let value= getFieldVal(groupKey,fieldKey);
    useEffect(() => {
        const updater= () => {
            if (!mounted) return;
            const newValue= getFieldVal(groupKey,fieldKey);
            if (newValue!==value) {
                setValueToState(newValue);
                value= newValue;
            }
        };
        const remover= FieldGroupUtils.bindToStore(groupKey, updater);
        return () => {
            mounted= false;
            remover();
        };
    },[]);

    const getter= useCallback(    // getter - fullFieldInfo: true: return the full field object, false: just the value
        (fullFieldInfo=false) => fullFieldInfo ? (getField(groupKey,fieldKey) ??{}) : getFieldVal(groupKey,fieldKey,value),
        [value]);
    const setter= useCallback(
        /**
         * the setter
         * @param newValue the new value
         * @param {Object|Boolean} [inSettings] - if object then use as settings for the FieldGroupField, if boolean use as valid
         */
        (newValue,inSettings={}) => setFieldValue(groupKey,fieldKey,newValue,inSettings) , [value]); //setter
    return [ getter, setter];
}



/**
 * This hook will force a rerender if certain fields change
 * @param {Array.<String>} [fldNameAry] - array of field name keys, if undefined then rerender if any field in the group changes
 * @param {String} [gk] - the groupKey if not set then it is retrieved from context, the normal use is to not pass this parameter
 */
export function useFieldGroupRerender(fldNameAry=[], gk) {
    const context= useContext(FieldGroupCtx);
    const groupKey= gk || context.groupKey;
    const setValueToState= useState(undefined)[1]; // use state here is just to force re-renders on value change
    let mounted= true;
    const getFs= () => fldNameAry.length ? makeFieldsObject(groupKey,fldNameAry) : getGroupFields(groupKey);
    // const getFs= () => getGroupFields(groupKey);
    let value= getFs();
    useEffect(() => {
        const updater= () => {
            if (!mounted) return;
            const newValue= getFs();
            if (!shallowequal(newValue,value)) {
                setValueToState(newValue);
                value= newValue;
            }
        };
        const remover= FieldGroupUtils.bindToStore(groupKey, updater);
        return () => {
            mounted= false;
            remover();
        };
    },[]);
}



export function useFieldGroupWatch(keyAry,f,dependAry=[],gk) {
    let mounted= true;
    let isInit= true;
    const context= useContext(FieldGroupCtx);
    const groupKey= gk || context.groupKey;
    let watchFieldsAry= getFieldsForKeys(groupKey,keyAry);
    useEffect(() => {
        const watcher= (force) => {
            if (!mounted) return;
            if (!keyAry?.length) force= true;
            const newWatchFieldsAry= getFieldsForKeys(groupKey,keyAry);
            const doUpdate= force || newWatchFieldsAry.some( (field,idx) =>
                field.value!==watchFieldsAry[idx].value || field.valid!==watchFieldsAry[idx].valid);
            if (!doUpdate) return;
            watchFieldsAry= newWatchFieldsAry;
            f?.(watchFieldsAry ? watchFieldsAry.map( (fld) => fld.value) : [], isInit);
            isInit= false;
        };
        watcher(true);
        const remover= FieldGroupUtils.bindToStore(groupKey, () => watcher(false));
        return () => {
            mounted= false;
            remover();
        };

    },dependAry);
}




export function useFieldGroupMetaState(defMetaState={}, gk) {
    const context= useContext(FieldGroupCtx);
    const groupKey= gk || context.groupKey;
    const setToReactState= useState(undefined)[1]; // use state here is just to force re-renders on value change
    let mounted= true;
    let metaState= {...defMetaState, ...getMetaState(groupKey)};
    useEffect(() => {
        const updater= () => {
            if (!mounted) return;
            const newMetaState= getMetaState(groupKey);
            if (!shallowequal(newMetaState,metaState)) {
                setToReactState(newMetaState);
                metaState= newMetaState;
            }
        };
        const remover= FieldGroupUtils.bindToStore(groupKey, updater);
        return () => {
            mounted= false;
            remover();
        };
    },[metaState]);

    const getter= useCallback( () => {
        const ms= {...defMetaState, ...getMetaState(groupKey)};
        return isEmpty(ms) ? metaState : ms;
    }, [metaState]);// getter
    const setter= useCallback( (newMetaState) => dispatchMetaStateChange({groupKey, metaState:newMetaState}), [metaState]); //setter
    return [getter, setter];
}


export function useWatcher(actions, callback, params) {
   const id= uniqueId('use-watcher');
   useEffect(() => {
       dispatchAddActionWatcher({
           id, actions, params,
           callback:(action, cancelSelf, params, dispatch, getState) => {
               callback(action, cancelSelf, params, dispatch, getState);
           },
       });
       return () => {
           dispatchCancelActionWatcher(id);
       };
   },[]);
}

export function useDebugCycle({id, render=true, mount=true}) {

    useEffect(() => {
        mount && console.log(id, 'mounting');
        return () => {
            mount && console.log(id, 'unmounting');
        };
    },[]);

    useEffect(() => {
        render && console.log(id, 'rendering');
    });
}

/*
 Enforces our usage of slotProps as a way to define both the slot's component as well as its props.
 The default props and passed in props are deep merged using smartMerge.
 <Need to revisit> smartMerge works for now, but ultimately, default props should use a path-based
    key, so it can be precisely inject the default value without uncertainty.  e.g. webutil's updateSet or setIf based on lodash's set.

 When slotProps is used, it will interpret as:
 ```
 slotProps: {
   component: elementType,  // when defined, it is used to render the props.  if null, nothing will render in this slot.
   ...otherProps: any       // these props will be passed to the rendering component
 }
 ```

 Example:
 ```
 const defProps= {text: 'hello world', customMsg: 'none'};
 const passedInSlotProps = {
    component: MyResults,
    customMsg: 'I am Joe'
 }

 <Slot component={EmptyResults} {...defProps} slotProps={passedInSlotProps}/>
 ```
 This will return:
 ```
   <MyResults text='hello world' customMsg='I am Joe'/>
 ```
 */
export function Slot({component, slotProps={}, ...defProps}) {
    const {component:Component=component, ...nProps} = slotProps;
    const props = smartMerge(defProps, nProps);
    return Component ? <Component {...props}/> : false;
}