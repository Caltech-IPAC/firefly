/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {convertAngle, convert} from '../VisUtil.js';
import {WebPlotRequest, GridOnStatus, TitleOptions} from '../WebPlotRequest.js';
import {ZoomType} from '../ZoomType.js';
import {CoordinateSys} from '../CoordSys.js';
import {makeWorldPt} from '../Point.js';


/**
 *
 * @param wp
 * @param size
 * @param baseTitle
 * @param blank
 * @param gridOn
 * @param width
 * @return {*|request}
 */
export function getCoverageRequest(wp, size, baseTitle, blank= false, gridOn= GridOnStatus.FALSE, width=1) {
    var title;
    var request;
    size *= 2.2;  //image fetch takes radius as diameter
    const radiusAS = convertAngle('degree', 'arcsec', size);
    if (blank) {
        const asSize= size*3600;
        const pixPerAS= asSize/width;
        request= WebPlotRequest.makeBlankPlotRequest(wp,pixPerAS,width,width); // pass width to width & height to make a square
        if (gridOn!==GridOnStatus.FALSE) request.setGridOn(gridOn);
        request.setTitle(baseTitle);
    }
    else if (radiusAS < 500) {
        request = WebPlotRequest.make2MASSRequest(wp, 'k', size);
        title = baseTitle + ' 2MASS k';
        request.setTitle(title);
    } else if (radiusAS < 1800) {
        request = WebPlotRequest.makeDSSOrIRISRequest(wp, 'poss2ukstu_red', '100',size);
        title = baseTitle + ' DSS';
        request.setTitle(title);
    } else if (size < 12.5) {
        size = Math.ceil(size);
        request = WebPlotRequest.makeIRISRequest(wp, '100', size);
        title = baseTitle + ' IRAS:IRIS 100';
        request.setTitle(title);
    } else {
        request = WebPlotRequest.makeAllSkyPlotRequest();
        title = baseTitle + ' All Sky';
        if (size < 30) {
            const wpGal = convert(wp, CoordinateSys.GALACTIC);

            size *= 2;
            const startWP = makeWorldPt(wpGal.x - size, wpGal.y - size);
            const endWP = makeWorldPt(wpGal.x + size, wpGal.y + size);

            var correctX1 = startWP.x;
            var correctY1 = startWP.y;
            var correctX2 = endWP.x;
            var correctY2 = endWP.y;

            if (correctX1 < 0) {
                correctX1 = 0;
                correctX2 = size * 2;
            }
            if (correctX1 > 360) {
                correctX1 = 360 - (size * 2 + 1);
                correctX2 = 359.75;
            }
            if (correctX2 < 0) {
                correctX2 = 0;
                correctX1 = size * 2;
            }
            if (correctX2 > 360) {
                correctX2 = 360 - (size * 2 + 1);
                correctX1 = 359.75;
            }


            if (correctY1 < -90) {
                correctY1 = -90;
                correctY2 = -90 + size * 2;
            }
            if (correctY1 > 90) {
                correctY1 = 90 - (size * 2 + 1);
                correctY2 = 89.75;
            }
            if (correctY2 < -90) {
                correctY2 = -90;
                correctY1 = -90 + size * 2;
            }
            if (correctY2 > 90) {
                correctY2 = 90 - (size * 2 + 1);
                correctY1 = 89.75;
            }

            if (Math.abs(correctX1-correctY1)<30 && Math.abs(correctY1-correctY2)<30) {
                title += '-cropped';
                request.setPostCrop(true);
                request.setCropPt1(makeWorldPt(correctX1, correctY1, CoordinateSys.GALACTIC));
                request.setCropPt2(makeWorldPt(correctX2, correctY2, CoordinateSys.GALACTIC));
            }

        }
        request.setTitleOptions(TitleOptions.PLOT_DESC);

    }
    request.setGridOn(gridOn);
    request.setZoomType(ZoomType.FULL_SCREEN);
    return request;
}
