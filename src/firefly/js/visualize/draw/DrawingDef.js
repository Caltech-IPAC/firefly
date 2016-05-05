/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {DrawSymbol} from './PointDataObj.js';
import Enum from 'enum';


export const COLOR_SELECTED_PT = '#ffff00';
export const COLOR_HIGHLIGHTED_PT = '#00aaff';
export const COLOR_PT_1 = '#ff0000'; // red
export const COLOR_PT_2 = '#00ff00'; //green
export const COLOR_PT_3 = 'pink';  // pink
export const COLOR_PT_4 = '#00a8ff'; //blue
export const COLOR_PT_5 =  '#990099'; //purple
export const COLOR_PT_6 = '#ff8000'; //orange

export const COLOR_DRAW_1 = '#ff0000';
export const COLOR_DRAW_2 = '#5500ff';

export const TextLocation = new Enum([ 'DEFAULT',
    'LINE_TOP',
    'LINE_BOTTOM',
    'LINE_MID_POINT',
    'LINE_MID_POINT_OR_BOTTOM',
    'LINE_MID_POINT_OR_TOP',
    'CIRCLE_NE',
    'CIRCLE_NW',
    'CIRCLE_SE',
    'CIRCLE_SW',
    'RECT_NE',
    'RECT_NW',
    'RECT_SE',
    'RECT_SW',
    'ELLIPSE_NE',
    'ELLIPSE_NW',
    'ELLIPSE_SE',
    'ELLIPSE_SW',
    'CENTER']); // use MID_X, MID_X_LONG, MID_Y, MID_Y_LONG for vertical or horizontal lines

export const Style= new Enum(['STANDARD','HANDLED', 'LIGHT']);

export const DEFAULT_FONT_SIZE = '9pt';

/**
 * Object to hold defaults for drawing a group of objects.
 *
 * @param color
 * @param pointData
 * @return {{color: *, pointData: boolean, symbol: object, lineWidth: number, renderOptions: {shadow: null, rotAngle: null, translation: null}}}
 */
export function makeDrawingDef(color= 'red') {

	// FIXME: those are not DS9 colors, hence problem when saved in DS9 regions format file


    return {
        color,
        symbol: DrawSymbol.X,
        lineWidth:1,
        size : 4,
        renderOptions: {shadow:null,rotAngle:null,translation:null},
        textLoc: TextLocation.DEFAULT,
        fontName: 'helvetica',
        fontSize:  DEFAULT_FONT_SIZE,
        fontWeight: 'normal',
        fontStyle: 'normal',
        selectedColor: COLOR_SELECTED_PT
    };
}
