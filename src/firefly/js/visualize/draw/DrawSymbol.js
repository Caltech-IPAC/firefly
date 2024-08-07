import Enum from 'enum';

/**
 * @typedef {Object} DrawSymbol
 * enum can be one of
 * @prop X
 * @prop SQUARE
 * @prop CROSS
 * @prop DIAMOND
 * @prop DOT
 * @prop CIRCLE
 * @prop SQUARE_X
 * @prop EMP_CROSS
 * @prop EMP_SQUARE_X
 * @prop BOXCIRCLE
 * @prop ARROW
 * @prop TEXT
 * @type {Enum}
 */

/** @type DrawSymbol */
export const DrawSymbol = new Enum([
    'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE', 'SQUARE_X', 'EMP_CROSS','EMP_SQUARE_X',
    'BOXCIRCLE', 'ARROW', 'ROTATE', 'POINT_MARKER', 'TEXT'
], { ignoreCase: true });

