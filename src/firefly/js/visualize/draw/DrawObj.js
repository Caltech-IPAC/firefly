/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';

export default {makeDrawObj};

/**
 * @summary the type of data this draw object can draw on
 * @description can be 'WcsCoordsOnly', 'ImageCoordsOnly', 'All'
 * @public
 * @global
 */
export const DrawingType= new Enum(['WcsCoordsOnly', 'ImageCoordsOnly', 'All']);


/**
 * @typedef {Object} DrawObj
 *
 * @prop {Object} supportedDrawingTypes
 * @prop {Object} renderOptions
 * @prop {String} type
 * @prop {boolean} forceWptForRegion
 */


/**
 *
 * @return {DrawObj}
 */
function makeDrawObj() {

    const obj= {
        // color:
        // selected:
        // highlighted
        // representCnt
        // lineWidth : 1;

        supportedDrawingTypes: DrawingType.All,
        renderOptions : {}, // can contain keys: shadow,translation,rotAngle
                            // shadow  - a shadow object, use makeShadow()
                            // translation - a ScreenPt use Point.makeScreenPt
                            // rotAngle - the angle, a number

        type : 'DrawObj',
        //getCanUsePathEnabledOptimization : () => false,
        //getScreenDist: (plot,pt) => 0,
        //getCenterPt : () => null,
        //draw : (ctx,plot,def,vpPtM,onlyAddToPath) => null,
        //toRegion : (plot,def) => [],
		//translateTo : (plot,worldPt) => this,
		//rotateAround :(plot, angle, wprldPt) => this
    };

    return obj;
}


