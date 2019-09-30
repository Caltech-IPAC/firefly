import Enum from 'enum';

/**
 *  enum
 *  one of 'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE', 'SQUARE_X', 'EMP_CROSS','EMP_SQUARE_X', 'BOXCIRCLE', 'ARROW'
 */
export const DrawSymbol = new Enum([
    'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE', 'SQUARE_X', 'EMP_CROSS','EMP_SQUARE_X',
    'BOXCIRCLE', 'ARROW', 'ROTATE', 'POINT_MARKER'
], { ignoreCase: true });

