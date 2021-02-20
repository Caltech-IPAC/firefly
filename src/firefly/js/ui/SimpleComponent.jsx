import {PureComponent, useEffect, useState} from 'react';
import {isArray, isString, pick, isEmpty} from 'lodash';
import shallowequal from 'shallowequal';
import {flux} from '../core/ReduxFlux.js';
import FieldGroupUtils, {getFldValue, getGroupFields} from '../fieldGroup/FieldGroupUtils.js';

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
 * By default, this will call Object.is() to ensure the state has changed before
 * calling setState.  To override this behavior, use useStoreConnector.bind({comparator: your_compare_function}).
 *
 * @param stateGetters  one or more functions returning a state, when called the oldState is passed as a parameter.
 * This allows a stateGetter to optionally act as a comparator.  If the stateGetter does not care about the changes
 * then it can return the oldState and there will be no state update.
 * @returns {Object[]}  an array of state's value in the order of the given stateGetters
 */
export function useStoreConnector(...stateGetters) {
    const {comparator=Object.is} = this || {};

    const rval = [];
    const setters = stateGetters.map((getter) => {
        const [val, setter] = useState(getter());
        rval.push(val);
        return [getter, setter, val];
    });

    let isMounted = true;
    useEffect(() => {
        const remover = flux.addListener(() => {
            if (isMounted) {
                setters.forEach(([getter, setter, oldState], idx) => {
                    const newState = getter(oldState);  // if getter returns oldState then no state update
                    if (newState===oldState) return;    // comparator might be overridden, use === first for efficiency
                    setters[idx][2] = newState;
                    if (!comparator(oldState, newState)) {
                        setter(newState);
                    }
                });
            }
        });
        return () => {
            isMounted = false;
            remover && remover();
        };
    }, []);     // only run once

    return rval;
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
    return fields;
}

/**
 * This hook will return a object of fields values. {[fieldName]:value}
 * @param {String} groupKey - the field group to check for
 * @param {String|Array.<String>} [fieldKeys] - an array of keys to check for or a single single key to check for
 * @return {{}}
 */
export function useFieldGroupValues(groupKey,fieldKeys) {
    const keyList= isArray(fieldKeys) ? fieldKeys : isString(fieldKeys) ? [fieldKeys] : [];
    let mounted= true;
    const stateObj= keyList.reduce( (obj,k) => {
        const [value,set]= useState(() => getFldValue(getGroupFields(groupKey),k));
        obj[k]={value,set};
        return obj;
    },{});

    useEffect(() => {
        const remover= FieldGroupUtils.bindToStore(groupKey, (updatedFields) => {
            mounted && keyList.forEach( (k) => stateObj[k].set( getFldValue(updatedFields,k)));
        });
        return () => {
            mounted= false;
            remover();
        };
    },[]);
    return Object.fromEntries(Object.entries(stateObj).map( ([k,{value}]) => [k,value] ));
}
