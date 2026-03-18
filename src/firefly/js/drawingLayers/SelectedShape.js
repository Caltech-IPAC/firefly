/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';

/**
 * @typedef SelectedShape
 * enum can be one of rect, circle
 * @prop rect
 * @prop circle
 * @prop ellipse
 * @prop polygon
 * @type {Enum}
 */

/** @type SelectedShape */
export const SelectedShape = new Enum(['rect', 'circle', 'ellipse', 'polygon']);