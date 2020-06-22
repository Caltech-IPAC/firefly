import {getCenterOfProjection, getFoV, primePlot} from './PlotViewUtil';
import {CysConverter} from './CsysConverter';
import {makeDevicePt, makeScreenPt} from './Point';
import {isImage} from './WebPlot';

/**
 * Call the function when drag is beginning.  It returns a function that is call on each drag event.
 * call the returned function with each next screen location it returns the new scroll point.
 * @param originalDeviceX
 * @param originalDeviceY
 * @param {ScreenPt} originalScrollPt
 * @param mouseDownScreenPt
 * @param {PlotView} startingPlotView
 * @return {function({number}, {number})}
 */
export function plotMove(originalDeviceX, originalDeviceY, originalScrollPt, mouseDownScreenPt, startingPlotView) {

    const startingPlot = primePlot(startingPlotView);
    const cc = CysConverter.make(startingPlot);
    const originalScreenPt = cc.getScreenCoords(makeDevicePt(originalDeviceX, originalDeviceY));
    const startWp = cc.getWorldCoords(mouseDownScreenPt, startingPlot.imageCoordSys);
    const xDir = startingPlotView.flipY ? -1 : 1;
    const yDir = startingPlotView.flipX ? -1 : 1;
    let xdiff, ydiff;
    let lastDevPt = makeDevicePt(originalDeviceX, originalDeviceY);
    let lastWorldPt = cc.getWorldCoords(lastDevPt, startingPlot.imageCoordSys);


    if (isImage(startingPlot)) {
        return (screenX, screenY) => {
            const newDevPt = makeDevicePt(screenX, screenY);
            const newScreenPt = cc.getScreenCoords(newDevPt);
            xdiff = (newScreenPt.x - originalScreenPt.x) * xDir;
            ydiff = (newScreenPt.y - originalScreenPt.y) * yDir;

            const newScrX = originalScrollPt.x - xdiff;
            const newScrY = originalScrollPt.y - ydiff;

            return makeScreenPt(newScrX, newScrY);
        };
    } else {
        return (screenX, screenY, pv) => {
            const newDevPt = makeDevicePt(screenX, screenY);
            const plot = primePlot(startingPlotView);
            if (!startWp) return null;

            xdiff = (newDevPt.x - lastDevPt.x);
            ydiff = (newDevPt.y - lastDevPt.y);

            const actionPlot = primePlot(pv);
            const activeCC = CysConverter.make(actionPlot);
            const centerOfProj = getCenterOfProjection(actionPlot);
            const originalCenterOfProjDev = activeCC.getDeviceCoords(centerOfProj);

            if (!originalCenterOfProjDev) {
                return null;
            }


            if (lastWorldPt && Math.abs(lastWorldPt.y) > 88 && getFoV(pv) > 30) {
                // xdiff/=8;
                ydiff /= 8;
                xdiff /= 8;
            }


            const newCenterOfProjDev = makeDevicePt(originalCenterOfProjDev.x - xdiff, originalCenterOfProjDev.y - ydiff);
            const newWp = activeCC.getWorldCoords(newCenterOfProjDev, plot.imageCoordSys);

            if (!newWp) return null;


            if (newWp.y < -89.7) newWp.y = -89.7;
            if (newWp.y > 89.7) newWp.y = 89.7;

            lastDevPt = newDevPt;
            lastWorldPt = newWp;

            return newWp;

        };
    }
}