/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {DrawSymbol} from './PointDataObj.js';


export const COLOR_SELECTED_PT = 'ffff00';
export const COLOR_HIGHLIGHTED_PT = '00aaff';
export const COLOR_PT_1 = 'ff0000'; // red
export const COLOR_PT_2 = '00ff00'; //green
export const COLOR_PT_3 = 'pink';  // pink
export const COLOR_PT_4 = '00a8ff'; //blue
export const COLOR_PT_5 =  '990099'; //purple
export const COLOR_PT_6 = 'ff8000'; //orange

export const COLOR_DRAW_1 = 'ff0000';
export const COLOR_DRAW_2 = '5500ff';



/**
 * Object to hold defaults for drawing a group of objects.
 *
 * @param color
 * @param pointData
 * @return {{color: *, pointData: boolean, symbol: object, lineWidth: number, renderOptions: {shadow: null, rotAngle: null, translation: null}}}
 */
export function makeDrawingDef(color) {

	// FIXME: those are not DS9 colors, hence problem when saved in DS9 regions format file



    return {
        color,
        symbol: DrawSymbol.X,
        lineWidth:1,
        renderOptions: {shadow:null,rotAngle:null,translation:null}
    };
}
