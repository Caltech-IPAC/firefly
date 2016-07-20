/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * This Object contains the specifications of the DS9 region
 */

import {visRoot} from '../ImagePlotCntlr.js';
import {primePlot} from '../PlotViewUtil.js';
import {makeScreenPt} from '../Point.js';
import {CysConverter} from '../CsysConverter.js';
import {RegionType} from './Region.js';
import {get} from 'lodash';


export function getRegionArea(region, cc) {
    var rCover = null;
    if (!cc) {
        var plotId = get(visRoot(), 'activePlotId');
        var plot = primePlot(visRoot(), plotId);
        cc = CysConverter.make(plot);
    }

    if (region.type === RegionType.polygon) {
        rCover = getRegionLinesArea(region.wpAry, cc);
    }
    return rCover;
}


/**
 * calculate the covered area of the regions which contains multiple lines, such as polygon
 * @param wpAry
 * @param cc
 * @returns {{upperLeftPt, width: number, height: number}}
 */
export function getRegionLinesArea(wpAry, cc) {
    var minx, miny, maxx, maxy;
    var wpScreen;
    var isOutOfRange = false;

    wpScreen = cc.getScreenCoords(wpAry[0]);
    if (!wpScreen) {
        return {};
    }
    minx = wpScreen.x;
    maxx = minx;
    miny = wpScreen.y;
    maxy = miny;

    wpAry.slice(1).forEach((wp) => {
        wpScreen = cc.getScreenCoords(wp);
        if (!wpScreen) {
            isOutOfRange = true;
        }
        if (!isOutOfRange) {
            if (wpScreen.x < minx)  minx = wpScreen.x;
            if (wpScreen.x > maxx)  maxx = wpScreen.x;
            if (wpScreen.y < miny)  miny = wpScreen.y;
            if (wpScreen.y > maxy)  maxy = wpScreen.y;
        }
    });

    if (isOutOfRange) return {};

    return {upperLeftPt: makeScreenPt(minx, miny),
            width: (maxx - minx + 1),
            height: (maxy - miny + 1)};
}
