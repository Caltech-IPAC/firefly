import {flux} from '../core/ReduxFlux';
import {DRAWING_LAYER_KEY, IMAGE_PLOT_KEY} from './VisConst';

/**
 * @returns {VisRoot}
 *
 * @public
 * @function visRoot
 * @memberof firefly.action
 * */
export function visRoot() {
    return flux.getState()[IMAGE_PLOT_KEY];
}

/**
 * return DrawLayerRoot
 * @public
 * @function dlRoot
 * @return {DrawLayerRoot}
 */
export function dlRoot() {
    return flux.getState()[DRAWING_LAYER_KEY];
}

/**
 * Return, from the store, the master array of all the drawing layers on all the plots
 * @returns {DrawLayer[]}
 * @memberof firefly.action
 * @function  getDlAry
 */
export function getDlAry() {
    return flux.getState()[DRAWING_LAYER_KEY].drawLayerAry ?? [];
}