/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {once} from 'lodash';

/**
 * @typedef {Object} Action
 * @prop {String} type - the action constant, a unique string identifying this action
 * @prop {Object} [payload] - object with anything, the data
 * @global
 * @public
 */

/**
 * @function process
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

/** a function to show init has not happened */
const throwUnInitError= () => {throw Error('firefly has not been bootstrapped');};
const defDecor= (x) => x;

export const flux = {
    getState: throwUnInitError,
    process: throwUnInitError,
    addListener: throwUnInitError,
    getRedux: throwUnInitError,
    registerCreator:  throwUnInitError,
    createDrawLayer: throwUnInitError,
    getDrawLayerFactory:throwUnInitError,
};

/**
 * Start redux and setup flux export.  This function can only be called once.
 * @param {BootstrapRegistry} bootstrapRegistry
 * @param {Function} processDecorator - a function that will take the process function as a parameter and
 * return a new process function that should wrap the original
 * @param {Function} addListDecorator- a function that will take the addListener function as a parameter and
 * return a new addListener function that should wrap the original
 */
export const bootstrapRedux= once((bootstrapRegistry, processDecorator= defDecor, addListDecorator= defDecor) => {
    if (!bootstrapRegistry) throw(Error('bootstrapRegistry parameter is require for firefly startup'));

    const redux= bootstrapRegistry.createRedux();
    const {registerCreator,createDrawLayer,getDrawLayerFactory, getActionCreators, startCoreSagas}= bootstrapRegistry;

       // setup process function. All actions are dispatched though this
    const process= processDecorator( (rawAction) => {
            const ac = getActionCreators().get(rawAction.type);
            if (!rawAction.payload) rawAction= {...rawAction, payload:{}};
            redux.dispatch(ac ? ac(rawAction) : rawAction);
        }
    );
      // update the exported object with the initialized functions
    Object.assign(flux, {
        process, registerCreator, createDrawLayer, getDrawLayerFactory,
        getRedux: () => redux,
        addListener: addListDecorator((listener) => redux.subscribe(listener)),
        getState: () => redux.getState(),
    });
    startCoreSagas();
});