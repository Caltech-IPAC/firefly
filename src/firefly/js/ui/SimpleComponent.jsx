import {PureComponent, useEffect, useState} from 'react';
import shallowequal from 'shallowequal';
import {flux} from '../core/ReduxFlux.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';

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
    const [fields, setFields] = useState({});
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


