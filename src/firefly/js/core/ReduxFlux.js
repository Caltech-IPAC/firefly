/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Logger} from '../util/Logger';

/**
 * @typedef {Object} Action
 * @prop {String} type - the action constant, a unique string identifying this action
 * @prop {Object} [payload] - object with anything, the data
 * @global
 * @public
 */


const throwUninitError= () => {throw Error('firefly has not been bootstrapped');};

export const flux = {
    getState: throwUninitError,
    process: throwUninitError,
    addListener: throwUninitError,
    getRedux: throwUninitError,
    registerCreator:  throwUninitError,
    createDrawLayer: throwUninitError,
    getDrawLayerFactory:throwUninitError,
};


function makeProcess(redux, getActionCreators, preDispatch, postDispatch) {
    /**
     * Process the rawAction.  This uses the actionCreators map to resolve
     * the ActionCreator given the action.type.  If one is not mapped, then it'll
     * create a simple 'pass through' ActionCreator that returns the rawAction as an action.
     *
     * <i>Note: </i> Often it makes sense to have a utility function call <code>process</code>. In that case
     * the utility function should meet the follow criteria.  This is a good way to document and default the
     * payload parameters.  The utility function should implement the following standard:
     * <ul>
     *     <li>The function name should start with "dispatch"</li>
     *     <li>The action type as the second part of the name</li>
     *     <li>The function should be exported from the controller</li>
     *     <li>The function parameters should the documented with jsdocs</li>
     *     <li>Optional parameters should be clear</li>
     * </ul>
     * Utility function Example - if action type is <code>PLOT_IMAGE</code> and the <code>PLOT_IMAGE</code> action
     * is exported from the ImagePlotCntlr module.  The the name should be <code>processPlotImage</code>.
     *
     *
     * @param {Action} rawAction
     */
    const process= (rawAction) => {
        preDispatch();
        const ac = getActionCreators().get(rawAction.type);
        if (!rawAction.payload) rawAction= {...rawAction, payload:{}};
        redux.dispatch(ac ? ac(rawAction) : rawAction);
        postDispatch(rawAction);
    };
    return process;
}

export function bootstrapRedux(bootstrapRegistry, preDispatch= () => undefined, postDispatch= () => undefined) {
    if (!bootstrapRegistry) {
        Logger('ReduxFlux').error('bootstrapRegistry parameter is require for firefly startup');
        return;
    }
    if (flux.getRedux!==throwUninitError) {
        Logger('ReduxFlux').error('bootstrapRedux should only be called once');
        return;
    }
    initFluxObj(bootstrapRegistry.createRedux(),bootstrapRegistry,preDispatch, postDispatch);
    bootstrapRegistry.startCoreSagas();
}

function initFluxObj(redux,bootstrapRegistry,preDispatch, postDispatch) {
    Object.assign(flux, {
        process: makeProcess(redux,bootstrapRegistry.getActionCreators,preDispatch, postDispatch),
        getRedux: () => redux,
        addListener: (listener) => redux.subscribe(listener),
        getState: () => redux.getState(),
        registerCreator:  (actionCreator, types) => bootstrapRegistry?.registerCreator(actionCreator,types),
        createDrawLayer: (drawLayerTypeId, params) => bootstrapRegistry?.createDrawLayer(drawLayerTypeId, params),
        getDrawLayerFactory: () => bootstrapRegistry?.getDrawLayerFactory(),
    });
}
