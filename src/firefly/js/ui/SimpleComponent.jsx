import {PureComponent, useEffect, useState} from 'react';
import shallowequal from 'shallowequal';

import {flux} from '../Firefly.js';

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
 * @param stateGetters  one or more functions returning a state
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

    useEffect(() => {
        return flux.addListener(() => {
                setters.forEach(([getter, setter, oldState], idx) => {
                    const newState = getter();
                    setters[idx][2] = newState;
                    if (!comparator(oldState, newState)) {
                        setter(newState);
                    }
                });
        });
    }, []);     // only run once

    return rval;
}