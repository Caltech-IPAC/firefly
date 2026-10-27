import {dispatchHideDialog} from '../../../core/ComponentCntlr';
import {dispatchCancelActionWatcher} from '../../../core/MasterSaga';
import ExtractHiPSTileTool from '../../../drawingLayers/ExtractHiPSTileTool';
import ExtractLineTool from '../../../drawingLayers/ExtractLineTool';
import ExtractPointsTool from '../../../drawingLayers/ExtractPointsTool';
import CysConverter from '../../CsysConverter';
import {dispatchDestroyDrawLayer, dispatchDetachLayerFromPlot} from '../../DrawLayerDispatch';
import {getExtName} from '../../FitsHeaderUtil';
import {getHiPSFitsTileLocation, getTilePixelAngSize} from '../../HiPSUtil';
import {dispatchAttributeChange, dispatchChangePointSelection} from '../../ImagePlotDispatch';
import {PlotAttribute} from '../../PlotAttribute';
import {
    currentP, getCubePlaneIdx, getHDU, getPlotViewAry, hasWCSProjection, isCube, isMultiHDUFits, primePlot
} from '../../PlotViewUtil';
import {makeImagePt} from '../../Point';
import {visRoot} from '../../VisStoreRoots';
import {computeDistance} from '../../VisUtil';
import {isHiPS} from '../../WebPlot';


export const EXTRACT_DIALOG_ID = 'extractionDialog';
export const HIPS_TILE_EXTRACT_DIALOG_ID = 'hipsTileExtractionDialog';
export const ZAXIS_POINT_SELECTION_ID = 'z-axisExtraction';
export const EXTRACT_END_ID = 'extractEndId';

export const Z_AXIS = 'Z_AXIS';
export const LINE = 'LINE';
export const POINTS = 'POINTS';
export const HIPS_TILE= 'HIPS_TILE';


export const POINT_CHART_TITLE= 'Point Extract Preview';

export const CUBE_WARNING=`
Values are taken directly from cube plane pixels without spatial interpolation, background subtraction, or PSF corrections. 
This is a quick-look tool for exploration, not a research-ready product.
`;

export const HIPS_APERTURE_NOTE=
    'HiPS: values come from the HiPS FITS tiles. An aperture larger than 1x1 that extends past the edge of a tile gives no value for that point.';

export const CHART_ID= 'extractionChart';

export const SELECT_TYPE_TIP= 'Choose mouse selection type: mouse line lock, mouse column lock or free selection';

const MAX_HIPS_LINE_PTS= 1500;



export function endExtraction() {
    cancelPointExtraction();
    cancelZaxisExtraction();
    cancelLineExtraction();
    cancelHiPSTileExtraction();
    dispatchCancelActionWatcher(EXTRACT_END_ID);
}

export function cancelHiPSTileExtraction() {
    const pvAry= getPlotViewAry(visRoot()).filter( (pv) => isHiPS(primePlot(pv)));
    dispatchHideDialog(HIPS_TILE_EXTRACT_DIALOG_ID);
    pvAry.forEach( (pv) => {
        const plotId= pv.plotId;
        dispatchDetachLayerFromPlot(ExtractHiPSTileTool.TYPE_ID, plotId);
        dispatchAttributeChange({
            plotId,
            changes: {
                [PlotAttribute.ACTIVE_HIPS_CELL]: undefined,
                [PlotAttribute.ACTIVE_HIPS_NORDER]: undefined
            }
        });
    });
    dispatchDestroyDrawLayer(ExtractHiPSTileTool.TYPE_ID);
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

function cancelZaxisExtraction() {
    dispatchChangePointSelection(ZAXIS_POINT_SELECTION_ID, false);
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

export function cancelLineExtraction() {
    const {pv} = currentP();
    if (pv) {
        dispatchDetachLayerFromPlot(ExtractLineTool.TYPE_ID, pv.plotId, true);
        dispatchAttributeChange({
            plotId: pv.plotId,
            changes: {[PlotAttribute.SELECT_ACTIVE_CHART_PT]: undefined}
        });
        dispatchDestroyDrawLayer(ExtractLineTool.TYPE_ID);
    }
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

function cancelPointExtraction() {
    const {pv} = currentP();
    if (pv) {
        dispatchDetachLayerFromPlot(ExtractPointsTool.TYPE_ID, pv.plotId, true);
        dispatchDestroyDrawLayer(ExtractPointsTool.TYPE_ID);
    }
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

export function makeHiPSLineExtractionTitle(plot, wptAry) {
    const cubeInfo= isCube(plot) ? `Plane #${getCubePlaneIdx(plot)+1} - ` : '';
    const ptStr= (wpt) => `(${wpt.x.toFixed(5)},${wpt.y.toFixed(5)})`;
    return `Line Extract Preview - ${cubeInfo}${ptStr(wptAry[0])} to ${ptStr(wptAry[wptAry.length-1])}`;
}

export function makeLineExtractionTitle(pv,x1,y1,x2,y2) {
    const plot= primePlot(pv);
    let hduInfo= '';
    let cubeInfo= '';
    if (isMultiHDUFits(pv)) {
        const extName= getExtName(plot);
        hduInfo= (extName || ` HDU #${getHDU(plot)}`)  + ' - ';
    }
    if (getCubePlaneIdx(plot)>-1) {
        cubeInfo= `Plane #${getCubePlaneIdx(plot)+1} - `;
    }
    return  `Line Extract Preview - ${hduInfo}${cubeInfo}(${x1},${y1}) to (${x2},${y2})`;
}


/**
 * Make the points along a line on a HiPS, spaced about one FITS tile pixel apart. The line is straight in the
 * HiPS projection, the same as it is drawn. A point in the same tile pixel as the point before it is skipped.
 * @param {WebPlot} plot
 * @param {ImagePt} ipt1 - the start of the line, image point of the HiPS projection
 * @param {ImagePt} ipt2 - the end of the line, image point of the HiPS projection
 * @param {number} hipsNorder - the norder of the FITS tiles
 * @return {{imPtAry:Array.<ImagePt>, hipsTileLocAry:Array.<Object>, hipsWptAry:Array.<WorldPt>}}
 *    imPtAry - the image point of the HiPS projection of each point, used by the chart
 *    hipsTileLocAry - the FITS tile location of each point, in the same order as imPtAry
 *    hipsWptAry - the world point of each point, in the same order as imPtAry
 */
export function makeHiPSLinePtAry(plot, ipt1, ipt2, hipsNorder) {
    const imPtAry= [];
    const hipsTileLocAry= [];
    const hipsWptAry= [];
    const cc= CysConverter.make(plot);
    const wpt1= cc.getWorldCoords(ipt1);
    const wpt2= cc.getWorldCoords(ipt2);
    if (!wpt1 || !wpt2) return {imPtAry, hipsTileLocAry, hipsWptAry};

    const steps= Math.min(Math.ceil(computeDistance(wpt1,wpt2)/getTilePixelAngSize(hipsNorder)), MAX_HIPS_LINE_PTS);
    let lastTilePixel;
    for(let i=0; i<=steps; i++) {
        const fraction= steps ? i/steps : 0;
        const imPt= makeImagePt(ipt1.x+(ipt2.x-ipt1.x)*fraction, ipt1.y+(ipt2.y-ipt1.y)*fraction);
        const wpt= cc.getWorldCoords(imPt);
        if (!wpt) continue;
        const tileLoc= getHiPSFitsTileLocation(plot, wpt, hipsNorder);
        if (!tileLoc.tileUrl) continue;
        const tilePixel= `${tileLoc.tileUrl};${tileLoc.tilePt.x},${tileLoc.tilePt.y}`;
        if (tilePixel===lastTilePixel) continue;
        lastTilePixel= tilePixel;
        imPtAry.push(imPt);
        hipsTileLocAry.push(tileLoc);
        hipsWptAry.push(wpt);
    }
    return {imPtAry, hipsTileLocAry, hipsWptAry};
}

/**
 * Convert the selected points to image points. For HiPS also find the FITS tile location of each point,
 * points that are not in a FITS tile are removed.
 * @param {WebPlot|undefined} plot
 * @param {Array.<Point>} ptAry - the selected points
 * @param {number} [hipsNorder] - for HiPS, the norder of the FITS tiles
 * @return {{foundPts:boolean, imPtAry:Array.<ImagePt>, hipsTileLocAry:Array.<Object>,
 *             wptAry:Array.<WorldPt>}}
 *    foundPts - true if there are any points to extract
 *    imPtAry - the image points, for HiPS these are in the HiPS projection.
 *              For an image these are passed to callGetPointExtractionAry
 *    hipsTileLocAry - HiPS only, the FITS tile location of each point, in the same order as imPtAry.
 *              These are passed to callGetPointExtractionAry
 *    wptAry - the world point of each point, in the same order as imPtAry, used by the chart.
 *              undefined for an image without a WCS projection
 */
export function makeExtractPtAry(plot, ptAry, hipsNorder) {
    if (!plot) return {foundPts:false, imPtAry:[]};
    const cc= CysConverter.make(plot);
    const toImPt= (pt) => {
        const ipt= cc.getImageCoords(pt);
        return ipt ? makeImagePt(Math.trunc(ipt.x), Math.trunc(ipt.y)) : undefined;
    };

    if (isHiPS(plot)) {
        const hipsPtAry= ptAry
            .map( (pt) => {
                const wpt= cc.getWorldCoords(pt);
                return {imPt: toImPt(pt), wpt, tileLoc: getHiPSFitsTileLocation(plot, wpt, hipsNorder)};
            })
            .filter( ({imPt, wpt, tileLoc}) => imPt && wpt && tileLoc.tileUrl);
        return {
            foundPts: hipsPtAry.length>0,
            imPtAry: hipsPtAry.map( ({imPt}) => imPt),
            hipsTileLocAry: hipsPtAry.map( ({tileLoc}) => tileLoc),
            wptAry: hipsPtAry.map( ({wpt}) => wpt),
        };
    }
    else {
        const wcs= hasWCSProjection(plot);
        const imagePtAry= ptAry
            .map( (pt) => ({imPt: toImPt(pt), wpt: wcs ? cc.getWorldCoords(pt) : undefined}))
            .filter( ({imPt, wpt}) => imPt && (!wcs || wpt));
        return {
            foundPts: imagePtAry.length>0,
            imPtAry: imagePtAry.map( ({imPt}) => imPt),
            wptAry: wcs ? imagePtAry.map( ({wpt}) => wpt) : undefined,
        };
    }
}
