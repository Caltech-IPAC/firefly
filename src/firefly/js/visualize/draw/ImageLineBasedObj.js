import Enum from 'enum';
import DrawObj from './DrawObj';
import {get} from 'lodash';
import ShapeDataObj from './ShapeDataObj.js';
import {makePointDataObj} from './PointDataObj.js';
import DrawOp from './DrawOp.js';
import {Style} from './DrawingDef.js';
import {makeZeroBasedImagePt, makeFitsImagePt, makeImagePt, makeAnyPt} from '../Point.js';
import {clone} from '../../util/WebUtil.js';
import {CoordinateSys} from '../CoordSys.js';
import {DrawSymbol} from './PointDataObj.js';
import {primePlot} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';

const IMGFP_OBJ = 'ImgBasedFPObj';
export const drawMode = new Enum(['data', 'highlight', 'select']);



/**
 * build an object based on footprint json containing the definition of a set of footprints
 * the return object includes a array of DrawObj object containing a CoonectObj object.
 * @param data
 * @returns {{}}
 * @constructor
 */
export const ImageLineBasedObj = (data) => {
    if (!data) return {};

    const retval = {};
    retval.pixelSys = CoordinateSys.parse(get(data, 'pixelsys', 'pixel'));
    retval.totalFeet = data.feet ? Object.keys(get(data, 'feet', {})).length : 0;
    retval.connectedObjs = convertDataToConnectedObjs(get(data, 'feet', {}), retval.pixelSys);
    retval.drawObjAry = {};

    return retval;
};

//for one footprint drawobj, one of connectedObjs in ImageLineBaseObj
function make(id, oneFootData, pixelSys) {
    if (!oneFootData) return null;
    const {corners, spans, peaks=[], rowIdx, rowNum, ra, dec, centerSys} = oneFootData;

    const obj = DrawObj.makeDrawObj();
    obj.pixelSys = pixelSys;
    obj.type = IMGFP_OBJ;
    obj.id = id;
    obj.connectObj = ConnectedObj.make(corners, spans, peaks, id, ra, dec, centerSys);
    obj.tableRowIdx = rowIdx;
    obj.tableRowNum = rowNum;
    return obj;
}

// for one footprint
const draw= {

    usePathOptimization(drawObj) {
        return false;
    },

    getCenterPt(drawObj) {
        const {connectObj, pixelSys}= drawObj;

        if (connectObj) {
            return connectObj.getCenterPt(pixelSys);
        } else {
            const makeImageFunc = getMakeImageFunc(pixelSys);
            return makeImageFunc(0, 0);
        }
    },

    getScreenDist(drawObj, plot, pt) {
        let minDist = Number.MAX_VALUE;

        const {connectObj, pixelSys}= drawObj;

        if (connectObj) {
            const centerPt = connectObj.getCenterPt(pixelSys);
            if (centerPt) {
                const cPt = plot.getScreenCoords(centerPt);
                const d = Math.sqrt((cPt.x - pt.x)**2 + (cPt.y - pt.y)**2);

                if (d < minDist) {
                    minDist = d;
                }
            }
        }

        return minDist;
    },

    draw(drawObj, ctx, plot, def, vpPtM, onlyAddToPath) {
        drawFootprintObj(ctx, plot, drawObj, def, vpPtM, onlyAddToPath);
    },

    toRegion(drawObj, plot, def) {
        return toRegion(drawObj, plot, def);
    },

    translateTo(drawObj, plot, apt) {
        return;   //todo
    },

    rotateAround(drawObj, plot, angle, worldPt) {
        return; // todo
    }
};

export default {make,draw, IMGFP_OBJ};

// draw one footprint drawobj
function drawFootprintObj(ctx, cc, drawObj, def, vpPtM, onlyAddToPath) {
    const {connectObj, pixelSys} = drawObj;
    const makeImageFunc = getMakeImageFunc(pixelSys);

    if (connectObj && cc) {
        const {x1, x2, y1, y2} = connectObj;
        const cornerInView = [[x1, y1], [x2, y1], [x2, y2], [x1, y2]].findIndex((c) => {
            const pt = makeImageFunc(c[0], c[1]);
            return (pt && cc.pointOnDisplay(pt));
        });

        if (cornerInView >= 0) {
            connectObj.draw(ctx, cc, def, vpPtM, onlyAddToPath);
        }
    }
}

function toRegion(drawObj, plot, def) {
    const {connectObj} = drawObj;
    const {polygonObjs=[], pointObjs=[]} = connectObj.drawObjs || {};

    return [...polygonObjs,...pointObjs].reduce((prev, obj) => {
        const regList = DrawOp.toRegion(obj, plot, def);
        prev.push(...regList);

        return prev;
    }, []);
}

// footprint data to connect object
function convertDataToConnectedObjs(data, pixelSys) {
    return Object.keys(data).reduce((prev, id) => {
        const connectObj = make(id, data[id], pixelSys);

        if (connectObj) {
            prev.push(connectObj);
        }
        return prev;
    }, []);
}


const getMakeImageFunc = (imageSys) => {
    let makeImageFunc;

    if (imageSys === CoordinateSys.ZEROBASED) {
        makeImageFunc = makeZeroBasedImagePt;
    } else if (imageSys === CoordinateSys.FITSPIXEL) {
        makeImageFunc = makeFitsImagePt;
    } else {
        makeImageFunc = makeImagePt;
    }

    return makeImageFunc;
};

export function getImageCoordsOnFootprint(pt, cc, pixelSys) {
    const imgPt = cc.getImageCoords(pt);

    if (pixelSys === CoordinateSys.ZEROBASED) {
        return cc.getZeroBasedImagePtFromInternal(imgPt);
    } else if (pixelSys === CoordinateSys.FITSPIXEL) {
        return cc.getFitsStandardImagePtFromInternal(imgPt);
    } else {
        return imgPt;
    }
}

/**
 * convert all connectObjs for the set of footprint unit into a array of drawObjs for display
 * @param imageLineObj
 * @param displayMode
 * @param color
 * @param showText
 * @param symbol
 * @param hideId
 * @returns {connectedObjs|*}
 */
export function convertConnectedObjsToDrawObjs(imageLineObj, displayMode, color,  showText, symbol, hideId) {

    // get rect of 'zero' regardless of the style
    //convertConnectedObjsToRectObjs(imageLineObj, false, color.hole, Style.FILL);
    // polygon outline
    (displayMode === Style.FILL)?
        convertConnectedObjsToPolygonObjs(imageLineObj, false, color.outline, color.fill, displayMode, '', true) :
        convertConnectedObjsToPolygonObjs(imageLineObj, showText, color.outline, null, Style.STANDARD, hideId, true);
    // peak points
    convertConnectedObjPeaksToPointObjs(imageLineObj, color.outline, symbol);

    // fill objects
    /*
    let oneRectObjs = [];
    if (displayMode === Style.FILL) {
        //oneRectObjs = convertConnectedObjsToRectObjs(imageLineObj, true, fillColor, displayMode);
    }
    */

    //imageLineObj.drawObjAry = [...oneRectObjs,...polygonObjs,...pointObjs,...zeroRectObjs];
    return imageLineObj.connectedObjs;
    //return imageLineObj.drawObjAry;
}

/**
 * create rectangle drawObjs based on the one or zero segments in the ImageLineBasedObj
 * @param imageLineObj
 * @param bCovered   one segment or zero segment
 * @param color      optional
 * @param style      optional
 * @param hideId
 */
export function convertConnectedObjsToRectObjs(imageLineObj, bCovered = true, color, style, hideId) {

    const {connectedObjs, pixelSys} = imageLineObj;
    const makeImageFunc = getMakeImageFunc(pixelSys);

    return connectedObjs.reduce((prev, oneFootprintObj) => {
        const oneConnectObj = oneFootprintObj.connectObj;
        if (style !== Style.FILL && bCovered) {     // no rect for covered area outline
            oneConnectObj.drawObjs[ONERECTS] = [];
        } else {                                    // uncovered area fill or outline, or covered area outline
            const basicObjs = oneConnectObj.makeRectDrawObjs(makeImageFunc, bCovered);
            const drawObjs = basicObjs.reduce((prev, oneObj) => {
                if (!hideId || hideId !== oneObj.id) {
                    prev.push(clone(oneObj, {style, color}));
                }
                return prev;
            }, []);
            bCovered ? oneConnectObj.drawObjs[ONERECTS] = drawObjs : oneConnectObj.drawObjs[ZERORECTS] = drawObjs;
        }
        bCovered ? prev.push(...oneConnectObj.drawObjs[ONERECTS]): prev.push(...oneConnectObj.drawObjs[ZERORECTS]);
        return prev;
    }, []);
}

/**
 * create polygon drawObjs based on the footprint data unit
 * @param imageLineObj
 * @param showText  if showing text
 * @param color     outline color
 * @param fillColor fill color
 * @param style     fill or outline
 * @param hideId    if hiding the display
 * @param bRemoveExist  optional reset the polygon objects
 */
export function convertConnectedObjsToPolygonObjs(imageLineObj, showText, color, fillColor, style, hideId='', bRemoveExist = false) {
    const {connectedObjs, pixelSys} = imageLineObj;
    const makeImageFunc = getMakeImageFunc(pixelSys);

    return connectedObjs.reduce((prev, oneFootprintObj) => {
        const oneCObj = oneFootprintObj.connectObj;
        const basicPolys = oneCObj.makePolygonDrawObjs(makeImageFunc);

        const polyObjs = basicPolys.reduce((prev, oneObj, idx) => {
            if (!hideId || oneObj.id !== hideId) {
                prev.push(clone(oneObj, {
                    text: ((showText&&idx===0) ? oneObj.id : ''),
                    color,
                    fillColor,
                    strokeColor: (style === Style.FILL) ? color: null,
                    style
                }));
            }
            return prev;
        }, []);

        if (oneCObj.drawObjs[POLYOBJS]&&!bRemoveExist) {
            oneCObj.drawObjs[POLYOBJS].push(...polyObjs);
        } else {
            oneCObj.drawObjs[POLYOBJS] = polyObjs;
        }

        prev.push(...oneCObj.drawObjs[POLYOBJS]);
        return prev;
    }, []);
}

/**
 * create point drawObjs based on the footprint peaks
 * @param imageLineObj
 * @param color
 * @param symbol
 * @param hideId
 * @returns {*}
 */
export function convertConnectedObjPeaksToPointObjs(imageLineObj, color, symbol, hideId) {
    const {connectedObjs, pixelSys} = imageLineObj;
    const makeImageFunc =  getMakeImageFunc(pixelSys);

    return connectedObjs.reduce((prev, oneFootprintObj) => {
        const oneCObj = oneFootprintObj.connectObj;
        const basicPoints = oneCObj.makePointObjsOnPeaks(makeImageFunc);

        oneCObj.drawObjs[POINTOBJS] = basicPoints.reduce((prev, oneObj) => {
                if (!hideId || hideId !== oneObj.id) {
                    prev.push(clone(oneObj, {symbol, color}));
                }
                return prev;
        }, []);

        prev.push(... oneCObj.drawObjs[POINTOBJS]);
        return prev;
    }, []);
}

export function drawHighlightFootprintObj(highlightedFootprint, plot, drawingDef) {
    const footprintObj = highlightedFootprint.connectObj;
    return footprintObj.getDrawObjOnHighlight(plot, drawingDef);
}

export const ONERECTS = 'oneRectObjs';
export const ZERORECTS = 'zeroRectObjs';
export const POLYOBJS = 'polygonObjs';
export const POINTOBJS = 'pointObjs';
const AllObjTypes = [ONERECTS, POLYOBJS, POINTOBJS, ZERORECTS];

export class ConnectedObj {
    constructor(corners, spans, peaks, id, ra, dec, centerSys) {
        this.corners = corners;
        this.spans = spans;
        this.peaks = peaks;
        this.id = id;
        this.oneSegments = {};
        this.zeroSegments = {};
        this.x1 = Number(Math.min(corners[0][0], corners[2][0]));
        this.y1 = Number(Math.min(corners[0][1], corners[2][1]));
        this.x2 = Number(Math.max(corners[0][0], corners[2][0]));
        this.y2 = Number(Math.max(corners[0][1], corners[2][1]));
        this.centerPt = (ra&&dec) ? makeAnyPt(ra, dec, centerSys) : null;
        this.basicObjs = AllObjTypes.reduce((prev, oneType) => {      // all drawobj for entire footporint
            prev[oneType] = null;
            return prev;
        }, {});
        this.drawObjs = AllObjTypes.reduce((prev, oneType) => {       // drawobj for showing, excluding some drawobj from basiObjs
            prev[oneType] = null;
            return prev;
        }, {});

    }

    static make(corners, data, peaks, id, ra, dec, centerSys) {
        return data ? new ConnectedObj(corners, data, peaks, id, ra, dec, centerSys) : null;
    }

    getCenterPt(pixelsys) {
        if (this.centerPt) return this.centerPt;
        const {x1, x2, y1, y2} = this;

        return (getMakeImageFunc(pixelsys))((x1+x2)/2, (y1+y2)/2);
    }

    getOneSegments() {
        const {oneSegments} = this.makeOneSegments();
        return oneSegments;
    }

    getZeroSegments() {
        const {zeroSegments} = this.makeZeroSegments();
        return zeroSegments;
    }

    getSegments() {
        return {oneSegments: this.getOneSegments(), zeroSegments: this.getZeroSegments()};
    }

    /**
     * an object containing array of '1' segments at each row, row is the key and array of '1' segments is the value.
     * @returns {{zeroSegments: ({}|*)}}
     */
    makeOneSegments() {
        const {y1, y2} = this;
        const retval = {oneSegments: this.oneSegments};

        if (Object.keys(this.oneSegments).length > 0) {
            return retval;
        }

        for (let y = y1; y <= y2; y++) {
            this.oneSegments[y] = [];
        }

        this.spans.forEach((oneSpan) => {
            const [y, x1, x2] = [...oneSpan];
            this.oneSegments[y].push([x1, x2]);
        });

        this.spans = [];     // discard original spans data
        return retval;
    }

    /**
     * an object containing array of '0' segments at each row, row is the key and array of '0' segments is the value.
     * @returns {{zeroSegments: ({}|*)}}
     */
    makeZeroSegments() {
        const {y1, y2, x1, x2} = this;
        const retval = {zeroSegments: this.zeroSegments};
        const REMOVE = 2;

        if (Object.keys(this.zeroSegments).length > 0) {
            return retval;
        }

        if (Object.keys(this.oneSegments).length === 0) {
            this.makeOneSegments();
        }
        for (let y = y1; y <= y2; y++) {
            this.zeroSegments[y] = [];
        }

        //each segment contain [x1, x2, toBeRemoved=0/1]
        for (let y = y1; y <= y2; y++) {
            const segNo = this.oneSegments[y].length;
            if (segNo === 0) {
                continue;
            }

            const toBeRemoved = (y === y1) || (y === y2) ? 1 : 0;
            let   oneSeg = this.oneSegments[y][0];
            if (x1 < oneSeg[0]) {
                this.zeroSegments[y].push([x1, oneSeg[0]-1, 1]);
            }

            if (segNo >= 2) {                                 // get zero segment between one segment
                this.oneSegments[y].forEach((seg, idx) => {
                    if (idx < segNo - 1) {
                        const seg1 = this.oneSegments[y][idx];
                        const seg2 = this.oneSegments[y][idx + 1];

                        if (seg1[1] < (seg2[0] - 1)) {
                            this.zeroSegments[y].push([seg1[1] + 1, seg2[0] - 1, toBeRemoved]);
                        }
                    }
                });
            }
            oneSeg = this.oneSegments[y][segNo - 1];
            if (x2 > oneSeg[1]) {
                this.zeroSegments[y].push([oneSeg[1]+1, x2, 1]);
            }
        }

        // clean zeroSegments which are out of footprint coverage
        const relPosition = (seg1, seg2) => {
            if (seg1[1] < seg2[0]) {
                return -1;
            } else if (seg1[0] > seg2[1]) {
                return 1;
            } else {
                return 0;
            }
        };

        const checkZeroFromOtherRow = (seg, checkY) => {
            if (this.oneSegments[checkY].length === 0) {  // other row has no one segment at all
                seg[REMOVE] = 1;
                return;
            }
            const checkSegs = this.zeroSegments[checkY];

            for (let n = 0; n < checkSegs.length; n++) {
                if (checkSegs[n][REMOVE] === 0) continue;

                const rp = relPosition(seg, checkSegs[n]);
                if (rp === -1) break;
                if (rp === 0) {
                    seg[REMOVE] = 1;
                    break;
                }
            }
        };


        const checkZeroAtRow = (y) => {
            for (let n = 0; n < this.zeroSegments[y].length; n++) {
                const oneSeg = this.zeroSegments[y][n];

                if (oneSeg[REMOVE]) continue;
                if (y > y1) checkZeroFromOtherRow(oneSeg, y - 1);
                if (oneSeg[REMOVE]) continue;
                if (y < y2) checkZeroFromOtherRow(oneSeg, y + 1);
            }
        };

        const yList = new Array(y2-y1+1).fill(0).map((v, idx) => idx+y1);

        yList.forEach((y) => {
            checkZeroAtRow(y);
        });

        yList.reverse().forEach((y) => {
           checkZeroAtRow(y);
        });

        // remove the zeroSegment not in footprint enclosure
        for (let y = y1; y <= y2; y++) {
            if (this.zeroSegments[y].length === 0) continue;

            for (let n = (this.zeroSegments[y].length - 1); n >= 0; n--) {
                const seg = this.zeroSegments[y][n];
                if (seg[REMOVE] === 0) {
                    seg.pop();
                } else {
                    this.zeroSegments[y].splice(n, 1);
                }
            }
        }
        retval.zeroSegments = this.zeroSegments;

        return retval;
    }

    /**
     * make rect drawobj on covered or uncovered area, based on '1' or '0' segments
     * @param makeImgPt
     * @param bCovered
     * @returns {*}
     */
    makeRectDrawObjs(makeImgPt, bCovered) {
        if (bCovered && this.basicObjs[ONERECTS]) {
            return this.basicObjs[ONERECTS];
        } else if (!bCovered && this.basicObjs[ZERORECTS]) {
            return this.basicObjs[ZERORECTS];
        }

        const segs = bCovered ? this.getOneSegments() : this.getZeroSegments();

        const objs = Object.keys(segs).reduce((prev, y) => {
            const rectObjs = segs[y].map((oneSeg) => {
                const pt1 = makeImgPt(oneSeg[0]-0.5, Number(y)-0.5);
                const pt2 = makeImgPt(oneSeg[1]+0.5, Number(y)+0.5);
                const rectObj = ShapeDataObj.makeRectangleByCorners(pt1, pt2);

                rectObj.id = this.id;
                return rectObj;
            });
            prev.push(...rectObjs);
            return prev;
        }, []);

        bCovered ? this.basicObjs[ONERECTS] = objs : this.basicObjs[ZERORECTS] = objs;
        return objs;
    }


    // trace the contour of the footprint outmost pixel counterclockwise
    // direction to trace the contour
    // 0: east, 1: NE, 2: N, 3: NW, 4: West, 5: SW, 6: S, 7: SE
    // one footprint may contain more than one polygon
    makePolygonDrawObjs(makeImgPt) {
        if (this.basicObjs[POLYOBJS]) {
            return this.basicObjs[POLYOBJS];
        }
        const {x1, x2, y1, y2} = this;
        const w = x2 - x1 + 1;
        const h = y2 - y1 + 1;
        const m = [];
        const Loc = {0: [1, 0], 1: [1, 1], 2:[0, 1], 3: [-1, 1],
                     4: [-1, 0], 5: [-1, -1], 6: [0, -1], 7: [1, -1]};
        const NextDirection = { 0: [3, 4, 5, 6, 7, 0],
                                1: [3, 4, 5, 6, 7, 0, 1],
                                2: [5, 6, 7, 0, 1, 2],
                                3: [5, 6, 7, 0, 1, 2, 3],
                                4: [7, 0, 1, 2, 3, 4],
                                5: [7, 0, 1, 2, 3, 4, 5],
                                6: [1, 2, 3, 4, 5, 6],
                                7: [1, 2, 3, 4, 5, 6, 7]};
        const [EAST, NE, NORTH, NW, WEST, SW, SOUTH, SE] = [0, 1, 2, 3, 4, 5, 6, 7];
        const POLYTRACED = 2;
        const POLYSTART = 3;
        const {oneSegments, zeroSegments} = this.getSegments();

        // set matrix
        for (let i = 0; i < h; i++) {
            m[i] = new Array(w).fill(0);
            const oneSeg = oneSegments[i+y1];
            const zeroSeg = zeroSegments[i+y1];

            for (let n = 0; n < oneSeg.length; n++) {
                for (let x = oneSeg[n][0]; x <= oneSeg[n][1]; x++) {
                    m[i][x-x1] = 1;
                }
            }
            for (let n = 0; n < zeroSeg.length; n++) {
                for (let x = zeroSeg[n][0]; x <= zeroSeg[n][1]; x++) {
                    m[i][x-x1] = 1;
                }
            }
        }

        const collectPolyPts = (startX, startY) => {
            let crtX = startX;
            let crtY = startY;
            let nextX, nextY;
            let foundPt;
            let fromDirection = WEST;

            m[startY][startX] = POLYSTART;
            const polyPts = [[startX, startY]];

            while (true) {
                foundPt = null;
                for (let n = 0; n < NextDirection[fromDirection].length; n++) {
                    const next = NextDirection[fromDirection][n];
                    nextX = crtX + Loc[next][0];
                    if (nextX < 0 || nextX >= w) {
                        continue;
                    }
                    nextY = crtY + Loc[next][1];
                    if (nextY < 0 || nextY >= h) {
                        continue;
                    }

                    if (m[nextY][nextX] === POLYSTART) {
                        break;
                    } else if (m[nextY][nextX] >= 1) {
                        m[nextY][nextX] = POLYTRACED;
                        foundPt = [nextX, nextY];
                        fromDirection = (Number(next) + 4) % 8;
                        break;
                    }
                }
                if (foundPt) {
                    crtX = foundPt[0];
                    crtY = foundPt[1];
                    polyPts.push([foundPt[0], foundPt[1]]);
                } else {
                    break;   // done
                }
            }
            if (polyPts.length < 3) {
                for (let t = 3 - polyPts.length; t >= 1; t--) {
                    polyPts.push(polyPts[0]);
                }
            }
            return polyPts;
        };


        for (let scanY = 0; scanY < h; scanY++) {
            let inLine = false;
            for (let scanX = 0; scanX < w; scanX++) {
                if (m[scanY][scanX] === 1 && !inLine) {
                    const newPolyPts = collectPolyPts(scanX, scanY);
                    const ptAry = newPolyPts.map((onePt) => makeImgPt(onePt[0]+x1, onePt[1]+y1));

                    const polygonObj = ShapeDataObj.makePolygon(ptAry);
                    polygonObj.id = this.id;

                    if (this.basicObjs[POLYOBJS]) {
                        this.basicObjs[POLYOBJS].push(polygonObj);
                    } else {
                        this.basicObjs[POLYOBJS] = [polygonObj];
                    }
                    inLine = true;
                } else if (m[scanY][scanX] >= POLYTRACED && !inLine) {
                    inLine = true;
                } else if (m[scanY][scanX] === 0 && inLine && !this.inSegment(zeroSegments, scanX+x1, scanY+y1)) {
                    inLine = false;
                }
            }
        }

        return this.basicObjs[POLYOBJS];
    }

    inSegment(segments, x, y){
        const segs = segments[y] || [];

        return segs.some((seg) => {
            return (x >= seg[0] && x <= seg[1]);
        });
    }

    makePointObjsOnPeaks(makeImgPt) {
        if (this.basicObjs[POINTOBJS]) {
            return this.basicObjs[POINTOBJS];
        }
        if (!this.peaks) return [];

        const pointObjs = this.peaks.map((onePeak) => {
            const pt = makeImgPt(onePeak[0], onePeak[1]);

            const pointObj = makePointDataObj(pt);
            pointObj.id = this.id;
            return pointObj;
        });

        this.basicObjs[POINTOBJS] = pointObjs;
        this.peaks = [];      // discard original peaks data
        return pointObjs;
    }

    containPoint(pt, pixelSys, cc) {
        const {x1, x2, y1, y2} = this;

        const inside = (pt.x >= x1 && pt.x <= x2 && pt.y >= y1 && pt.y <= y2
                        && this.inSegment(this.oneSegments, pt.x, Math.trunc(pt.y)));
        if (inside) {
            const cPt = getImageCoordsOnFootprint(this.getCenterPt(pixelSys), cc, pixelSys);
            return {inside, dist:  (Math.sqrt((cPt.x - pt.x)**2 + (cPt.y - pt.y)**2))};
        } else {
            return {inside};
        }
    }

    draw(ctx, cc, def,  vpPtM, onlyAddToPath) {
        let allObjs;

        if (def.drawMode && def.drawMode === drawMode.highlight.key) {
            allObjs = this.getDrawObjOnHighlight(cc, def);
        } else if (def.drawMode && def.drawMode === drawMode.select.key) {
            allObjs = this.getDrawObjsOnSelect(cc, def);
        } else {
            allObjs = AllObjTypes.reduce((prev, oneObjType) => {
                if (this.drawObjs[oneObjType]) {
                    prev.push(...this.drawObjs[oneObjType]);
                }
                return prev;
            }, []);
        }

        allObjs.forEach((oneObj) => {
            DrawOp.draw(oneObj, ctx, cc, def, vpPtM, onlyAddToPath);
        });
    }

    getDrawObjOnHighlight(cc, drawingDef) {

        const polyAry = this.basicObjs[POLYOBJS];
        const pointAry = this.basicObjs[POINTOBJS];

        if (!polyAry) return [];
        const {lineWidth = 1} = drawingDef;
        const lw = lineWidth+1;

        const plot = primePlot(visRoot(), cc.plotId);
        const polyHighlight = polyAry.reduce((prev, onePoly, idx) => {
            const newhObj = DrawOp.makeHighlight(onePoly, plot, drawingDef);
            newhObj.highlight = 1;
            newhObj.text = (idx === 0) ? newhObj.id : '';
            newhObj.style = Style.STANDARD;
            newhObj.lineWidth = lw;

            prev.push(newhObj);
            return prev;
        }, []);

        const pointHighlight = pointAry.reduce((prev, onePoint) => {
            const pointObj = clone(onePoint, {symbol: (drawingDef.symbol || DrawSymbol.X)});
            const newhObj = DrawOp.makeHighlight(pointObj, plot, drawingDef);
            newhObj.highlight = 1;
            newhObj.lineWidth = lw;

            prev.push(newhObj);
            return prev;
        }, []);
        return [...polyHighlight,...pointHighlight];
    }

    getDrawObjsOnSelect(cc, drawingDef) {
        const polyAry = this.basicObjs[POLYOBJS];
        const pointAry = this.basicObjs[POINTOBJS];
        const selDrawDef= Object.assign({}, drawingDef, {selectColor:drawingDef.selectedColor});

        const plot = primePlot(visRoot(), cc.plotId);
        const polySels = polyAry && polyAry.reduce((prev, onePoly) => {
                const newhObj = DrawOp.makeHighlight(onePoly, plot, selDrawDef);
                newhObj.highlight = 1;
                newhObj.style = Style.STANDARD;

                prev.push(newhObj);
                return prev;
            }, []);

        const pointSels = (pointAry) && pointAry.reduce((prev, onePoint) => {
                const pointObj = clone(onePoint, {symbol: (drawingDef.symbol || DrawSymbol.X)});
                const newhObj = DrawOp.makeHighlight(pointObj, plot, selDrawDef);
                newhObj.highlight = 1;

                prev.push(newhObj);
                return prev;
            }, []);

        return [...polySels,...pointSels];
    }
}