import {useEffect, useState} from 'react';
import shallowequal from 'shallowequal';
import {flux} from '../../core/ReduxFlux.js';
import {addImageReadoutUpdateListener} from '../VisMouseSync.js';

/**
 * This hook is for a few very specific cases where a very high level component needs to
 * connect to both the store and VisMouseSync. When store or mouse events happen a new state is only set
 * if the new state differs from the old state with using a shallowequal compare
 * @param {Function} makeState function to create a new state obj
 * @return {object} the new state object
 */
export function useMouseStoreConnector(makeState) {
    let mounted = true;
    const [stateObj, setStateObj] = useState(makeState?.());

    const storeUpdate = () => {
        const newState = makeState?.();
        if (!shallowequal(stateObj, newState) && mounted) setStateObj(newState);
    };

    useEffect(() => {
        const removeFluxListener = flux.addListener(() => storeUpdate(stateObj, setStateObj));
        const removeMouseListener = addImageReadoutUpdateListener(() => storeUpdate(stateObj, setStateObj));
        return () => {
            mounted = false;
            removeFluxListener();
            removeMouseListener();
        };
    }, []);

    return stateObj;
}