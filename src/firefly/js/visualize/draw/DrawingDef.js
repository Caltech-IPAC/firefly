/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {DrawSymbol} from './PointDataObj.js';
import Enum from 'enum';


export const COLOR_SELECTED_PT = '#ffff00';
export const COLOR_HIGHLIGHTED_PT = '#ff8000'; // orange
export const COLOR_PT_1 = '#ff0000'; // red
export const COLOR_PT_2 = '#00ff00'; //green
export const COLOR_PT_3 = 'pink';  // pink
export const COLOR_PT_4 = '#00a8ff'; //blue
export const COLOR_PT_5 =  '#990099'; //purple
export const COLOR_PT_6 = '#ff8000'; //orange

export const COLOR_DRAW_1 = '#ff0000';
export const COLOR_DRAW_2 = '#5500ff';

/** 
 *  enum
 *  a very long enum, look at code in DrawingDef.js
 * */
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
    'REGION_NE',
    'REGION_NW',
    'REGION_SE',
    'REGION_SW',
    'CENTER']); // use MID_X, MID_X_LONG, MID_Y, MID_Y_LONG for vertical or horizontal lines

export const Style= new Enum(['STANDARD','HANDLED', 'LIGHT']);

export const DEFAULT_FONT_SIZE = '9pt';


/**
 * @typedef {Object} DrawingDef
 *
 * The defaults that a drawing layer might use. Note that all the properties of this object are optional.
 * It can be created with any subset.
 *
 * @prop {String} color color css style
 * @prop {DrawSymbol} symbol default: DrawSymbol.X,
 * @prop {Number} lineWidth default:1,
 * @prop {Number} size  default: 4,
 * @prop {{shadow:number, rotAngle:number,translation:Object}} renderOptions
 * @prop {TextLocation} textLoc
 * @prop {String} fontName default: 'helvetica',
 * @prop {String} fontSize css size
 * @prop {String} fontWeight default: 'normal',
 * @prop {String} fontStyle default: 'normal',
 * @prop {String} selectedColor color css style
 * 
 */



/**
 * Object to hold defaults for drawing a group of objects.
 *
 * @param color
 * @param pointData
 * @return {DrawingDef}
 */
export function makeDrawingDef(color= 'red', presetDefaults= {}) {

	// FIXME: those are not DS9 colors, hence problem when saved in DS9 regions format file


    const def= {
        color,
        symbol: DrawSymbol.X,
        lineWidt:1,
        size : 4,
        renderOptions: {shadow:null,rotAngle:null,translation:null},
        textLoc: TextLocation.DEFAULT,
        fontName: 'helvetica',
        fontSize:  DEFAULT_FONT_SIZE,
        fontWeight: 'normal',
        fontStyle: 'normal',
        selectedColor: COLOR_SELECTED_PT
    };
    return Object.assign({}, def, presetDefaults);
}



export function *colorGenerator() {
    const defColors= [COLOR_PT_1, COLOR_PT_2, COLOR_PT_3, COLOR_PT_5, COLOR_PT_6];
    var colorCnt= 0;
    while (true) {
        yield defColors[colorCnt% defColors.length];
        colorCnt++;
    }
}

const nextColor= colorGenerator();

export const getNextColor= () => nextColor.next().value;
