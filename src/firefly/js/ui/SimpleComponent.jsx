import {PureComponent, useCallback, useContext, useEffect, useState} from 'react';
import {isArray, isString, uniqueId} from 'lodash';
import shallowequal from 'shallowequal';
import {flux} from '../core/ReduxFlux.js';
import FieldGroupUtils, {getFieldVal, getFldValue, getGroupFields} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from 'firefly/core/MasterSaga.js';
import {dispatchValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {GroupKeyCtx} from './FieldGroup.jsx';

export class SimpleComponent extends PureComponent {
    constructor(props) {
        super(props);
        this.state = this.getNextState(props);
    }

    UNSAFE_componentWillReceiveProps(np) {
        if (!this.isUnmounted) {
            if (!shallowequal(this.props, np)) {
                this.setState(this.getNextState(np));
            }
        }
    }
    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }
    componentWillUnmount() {
        this.isUnmounted=true;
        this.removeListener && this.removeListener();
    }

    getNextState(np) {
        return {};      // need to implement
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            this.setState(this.getNextState(this.props));
        }
    }
}



/**
 * A replacement for SimpleComponent.
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
 * @returns {Object}  new state's value
 */
export function useStoreConnector(stateGetter, deps=[]) {
    const {comparator=shallowequal} = this || {};

    const [val, setter] = useState(stateGetter());

    let isMounted = true;
    useEffect(() => {
        let cState = val;
        const remover = flux.addListener(() => {
            if (isMounted) {
                const nState = stateGetter(cState);      // if getter returns oldState then no state update
                if (nState===cState) return;             // comparator might be overridden, use === first for efficiency
                if ( !comparator(cState, nState) ) {
                    cState = nState;
                    setter(cState);
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

// todo - keep around for a while - this is the array version of useFieldGroupValue
//        I don't think we need it
// export function useFieldGroupValuesOLD2(groupKey,fieldKeys) {
//     const keyList= isArray(fieldKeys) ? fieldKeys : isString(fieldKeys) ? [fieldKeys] : [];
//     let mounted= true;
//     const stateList= keyList.map( (fieldKey) => {
//         const [value,setValue]= useState(() => getFldValue(getGroupFields(groupKey),fieldKey));
//         return {fieldKey,value,setValue};
//     },{});
//
//     useEffect(() => {
//         const remover= FieldGroupUtils.bindToStore(groupKey, (updatedFields) => {
//             mounted && stateList.forEach( ({fieldKey,setValue}) => setValue( getFldValue(updatedFields,fieldKey)));
//         });
//         return () => {
//             mounted= false;
//             remover();
//         };
//     },[]);
//
//     return stateList.map( ({fieldKey,value}) =>
//         [
//             useCallback(() =>getFieldVal(groupKey,fieldKey,value), [value]),   // getter
//             useCallback((newValue,valid=true) => dispatchValueChange({fieldKey, groupKey, value:newValue,valid, displayValue:''})) //setter
//         ]);
// }

/**
 * This hook will return a object of fields values. {[fieldName]:value}
 * @param {String} fieldKey - a fieldKey to check for
 * @param {String} [gk] - the groupKey if not set then it is retrieved from context, the normal use is to not pass this parameter
 * @return {Array.<Function>}  return an array of 2 functions [getValue,setValue]
 */
export function useFieldGroupValue(fieldKey, gk) {
    const context= useContext(GroupKeyCtx);
    const groupKey= gk || context.groupKey;
    let mounted= true;
    const [value,setValue]= useState(() => getFldValue(getGroupFields(groupKey),fieldKey));
    useEffect(() => {
        const remover= FieldGroupUtils.bindToStore(groupKey, (updatedFields) => {
            mounted && setValue( getFldValue(updatedFields,fieldKey));
        });
        return () => {
            mounted= false;
            remover();
        };
    },[]);
    return [
        useCallback(() =>getFieldVal(groupKey,fieldKey,value), [value]),   // getter
        useCallback((newValue,valid=true) => dispatchValueChange({fieldKey, groupKey, value:newValue,valid, displayValue:''})) //setter
    ];
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
