import DrawObj from './DrawObj';
import {makeWorldPt} from '../Point.js';
import {isEmpty, get} from 'lodash';
import ShapeDataObj from './ShapeDataObj.js';
import {makePointDataObj} from './PointDataObj.js';
import DrawOp from './DrawOp.js';
import {Style, TextLocation,DEFAULT_FONT_SIZE} from './DrawingDef.js';
import Point, {makeZeroBasedImagePt, makeFitsImagePt, makeImagePt} from '../Point.js';
import {clone} from '../../util/WebUtil.js';

const IMGFP_OBJ = 'ImgBasedFPObj';
const DEF_WIDTH = 1;


/**
 * @param data
 * @param style
 * @returns {null}
 */
function make(data, style) {
    //data = footprints256;
    if (!data) return null;

    const obj = DrawObj.makeDrawObj();
    const pixelSys = get(data, 'pixelsys', Point.IM_PT).toLowerCase();

    obj.type = IMGFP_OBJ;
    obj.totalFeet = data.feet ? Object.keys(data.feet).length : 0;
    obj.connectedObjs = convertDataToConnectedObjs(data.feet);
    obj.pixelSys = pixelSys.includes('zero') ? Point.ZERO_BASED_IM_PT
                                             : (pixelSys.includes('fits') ? Point.FITS_IM_PT : pixelSys);
    obj.style = style;

    // make basic rect objs containing 'on' cells, or 'off' cells, basic polygon objs based on feet data
    // those objs has no color, style, or text
    convertConnectedObjsToRectObjs(obj, true);
    convertConnectedObjsToRectObjs(obj, false);
    convertConnectedObjsToPolygonObjs(obj);
    convertConnectedObjPeaksToPointObjs(obj);


    return obj;
}

const draw= {

    usePathOptimization(drawObj) {
        return drawObj.lineWidth === 1;
    },

    getCenterPt(drawObj) {
        var {drawObjAry}= drawObj;
        var xSum = 0;
        var ySum = 0;
        var xTot = 0;
        var yTot = 0;

        if (drawObjAry) {
            drawObjAry.forEach((obj) => {
                if (obj && obj.pts) {
                    obj.pts.forEach((wp) => {
                        xSum += wp.x;
                        ySum += wp.y;
                        xTot++;
                        yTot++;
                    });
                }
            });
            return makeWorldPt(xSum / xTot, ySum / yTot);
        } else {
            return makeWorldPt(xSum, ySum);
        }
    },

    getScreenDist(drawObj, plot, pt) {
        let minDist = Number.MAX_VALUE;

        const {drawObjAry} = drawObj || {};

        if (drawObjAry) {
            drawObjAry.forEach((dObj) => {
                const d = ShapeDataObj.draw.getScreenDist(dObj, plot, pt);
                if (d < minDist) {
                    minDist = d;
                }
            });
        }
        return minDist;
    },

    draw(drawObj, ctx, plot, def, vpPtM, onlyAddToPath) {
        const drawParams = makeDrawParams(drawObj, def);
        drawFootprintObj(drawObj, ctx, plot, drawParams, vpPtM, onlyAddToPath);
    },

    toRegion(drawObj, plot, def) {
        return toRegion(drawObj.drawObjAry, plot, makeDrawParams(drawObj, def), drawObj.renderOptions);
    },

    translateTo(drawObj, plot, apt) {
        return;   //todo
    },

    rotateAround(drawObj, plot, angle, worldPt) {
        return; // todo
    }
};

export default {make,draw, IMGFP_OBJ};

function makeDrawParams(drawObj,def) {
    const style= drawObj.style || def.style || Style.STANDARD;
    const lineWidth= drawObj.lineWidth || def.lineWidth || DEF_WIDTH;
    const textLoc= drawObj.textLoc || def.textLoc || TextLocation.DEFAULT;
    const fontName= drawObj.fontName || def.fontName || 'helvetica';
    const fontSize= drawObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
    const fontWeight= drawObj.fontWeight || def.fontWeight || 'normal';
    const fontStyle= drawObj.fontStyle || def.fontStyle || 'normal';
    const rotationAngle = drawObj.rotationAngle||undefined;
    const color = drawObj.color || def.color || 'green';

    return {
        color,
        lineWidth,
        textLoc,
        style,
        fontName,
        fontSize,
        fontWeight,
        fontStyle,
        rotationAngle
    };
}

function drawFootprintObj(fpObj, ctx, cc, drawParams, vpPtM,onlyAddToPath) {
    const {drawObjAry} = fpObj;

    drawObjAry && drawObjAry.forEach((dObj) => {
        DrawOp.draw(dObj, ctx, cc, drawParams, vpPtM, onlyAddToPath);
    });
}


function toRegion(drawObjAry, plot, drawParams) {
    return drawObjAry.reduce((prev, dObj) => {
        const regList = DrawOp.toRegion(dObj, plot, drawParams);

        if (!isEmpty(regList)) {
            prev.push(...regList);
        }

        return prev;
    }, []);
}


function convertDataToConnectedObjs(data) {
    return Object.keys(data).reduce((prev, id) => {
        const oneFootData = data[id];
        const {corners, spans, peaks=[]} = oneFootData;
        const connectObj = ConnectedObj.make(corners, spans, peaks, id);
        const resultObjs = connectObj.splitOnEmptyLine();

        prev.push(...resultObjs);
        return prev;
    }, []);
}


const getMakeImageFunc = (imageSys) => {
    let makeImageFunc;

    if (imageSys === Point.ZERO_BASED_IM_PT) {
        makeImageFunc = makeZeroBasedImagePt;
    } else if (imageSys === Point.FITS_IM_PT) {
        makeImageFunc = makeFitsImagePt;
    } else {
        makeImageFunc = makeImagePt;
    }

    return makeImageFunc;
};


/**
 * create rectangle drawObjs based on the one or zero segments in the ImageLineBasedObj
 * @param imageLineObj
 * @param bCovered   one segment or zero segment
 * @param isOriginal
 * @param color      optional
 * @param style      optional
 */
export function convertConnectedObjsToRectObjs(imageLineObj, bCovered = true, isOriginal = true, color, style) {

    const cloneObjs = (objs) => {

        return objs.map((oneObj) => {
            const newObj = clone(oneObj);

            if (color) newObj.color = color;
            if (style) newObj.style = style;
            return newObj;
        });
    };

    if (bCovered && imageLineObj.oneRectObjs) {
        return cloneObjs(imageLineObj.oneRectObjs);
    } else if ((!bCovered) && (imageLineObj.zeroRectObjs)) {
        return cloneObjs(imageLineObj.zeroRectObjs);
    } else {

        const {connectedObjs, pixelSys} = imageLineObj;
        const makeImageFunc = getMakeImageFunc(pixelSys);

        const resultObjs = connectedObjs.reduce((prev, oneConnectObj) => {
            const drawObjs = oneConnectObj.makeRectDrawObjs(makeImageFunc, bCovered);

            prev.push(...drawObjs);

            return prev;
        }, []);
        if (bCovered) {
            imageLineObj.oneRectObjs = resultObjs;
        } else {
            imageLineObj.zeroRectObjs = resultObjs;
        }
        return isOriginal ? resultObjs : cloneObjs(resultObjs);
    }
}

/**
 * create polygon drawObjs based on the footprint data
 * @param imageLineObj
 * @param isOriginal
 * @param showText  optional
 * @param color     optional
 * @param style     optional
 */
export function convertConnectedObjsToPolygonObjs(imageLineObj, isOriginal = true, showText, color, style){

    const cloneObjs = (objs) => {
        return (objs).map((oneObj) => {
            const newObj = clone(oneObj);

            if (color) newObj.color = color;
            if (style) newObj.style = style;
            newObj.text = (showText) ? newObj.id : '';
            return newObj;
        });
    };

    if (imageLineObj.polygonObjs) {
        return cloneObjs(imageLineObj.polygonObjs);
    }

    const {connectedObjs, pixelSys} = imageLineObj;
    const makeImageFunc =  getMakeImageFunc(pixelSys);

    const polyDrawObjs = connectedObjs.map((oneCObj) => {
        return oneCObj.makePolygonDrawObjs(makeImageFunc);
    });

    imageLineObj.polygonObjs = polyDrawObjs;
    return isOriginal ? polyDrawObjs : cloneObjs(polyDrawObjs);
}

/**
 * create point drawObjs based on the footprint peaks
 * @param imageLineObj
 * @param isOriginal
 * @param symbolType
 * @param color
 * @param text
 * @returns {*}
 */
export function convertConnectedObjPeaksToPointObjs(imageLineObj, isOriginal = true,
                                                    symbolType, color, text) {

    const cloneObjs = (objs) => {
        return (objs).map((oneObj) => {
            const newObj = clone(oneObj);

            if (symbolType) newObj.symbol = symbolType;
            if (color) newObj.color = color;
            if (text) newObj.text = text;
            return newObj;
        });
    };

    if (imageLineObj.peakPointObjs) {
        return cloneObjs(imageLineObj.peakPointObjs);
    }

    const {connectedObjs, pixelSys} = imageLineObj;
    const makeImageFunc =  getMakeImageFunc(pixelSys);

    const points = connectedObjs.reduce((prev, oneCObj) => {
        const pointObjs = oneCObj.makePointObjsOnPeaks(makeImageFunc);
        prev.push(...pointObjs);
        return prev;
    }, []);

    imageLineObj.peakPointObjs = points;
    return isOriginal ? points : cloneObjs(points);
}


export class ConnectedObj {
    constructor(corners, spans, peaks, id) {
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
    }

    static make(corners, data, peaks, id) {
        return data ? new ConnectedObj(corners, data, peaks, id) : null;
    }

    splitOnEmptyLine() {
        const resultObjs = [];
        const {y1, y2, x1, x2} = this;
        let firstY = y1;

        this.makeOneSegments();
        const moveSpans = (spans, peaks, sy, ey) => {
            const newSpans = [], oldSpans = [];
            const newPeaks = [], oldPeaks = [];
            let n_x1 = x2, n_x2 = x1;

            for (let n = 0; n < spans.length; n++) {
                const [y, x_0, x_1] = [...spans[n]];

                if (y > ey) {
                    oldSpans.push(...spans.slice(n));
                    break;
                }
                if (y < sy) {
                    oldSpans.push(spans[n]);
                    continue;
                }
                newSpans.unshift(spans[n]);

                if (x_0 < n_x1) n_x1 = x_0;
                if (x_1 > n_x2) n_x2 = x_1;
            }

            for (let n = 0; n < peaks.length; n++) {
                if (peaks[n][1] < sy || peaks[n][1] > ey) {
                    newPeaks.push(peaks[n]);
                } else {
                    oldPeaks.push(peaks[n]);
                }
            }
            return {newSpans, oldSpans, newPeaks, oldPeaks, new_x1: n_x1, new_x2: n_x2, new_y1: sy, new_y2: ey };
        };


        for (let y = (y1+1); y < y2; y++) {
            if (this.oneSegments[y].length === 0) {
                if (y > firstY) {
                    const {newSpans, oldSpans, newPeaks, oldPeaks, new_x1, new_x2, new_y1, new_y2} = moveSpans(this.spans, this.peaks, firstY, y - 1);
                    if (newSpans.length > 0) {
                        const newCorners = [[new_x1, new_y1], [new_x2, new_y1], [new_x2, new_y2], [new_x1, new_y2]];
                        const newCObj = ConnectedObj.make(newCorners, newSpans, newPeaks, this.id);
                        resultObjs.push(newCObj);
                        this.spans = oldSpans;
                        this.peaks = oldPeaks;
                    }
                }
                firstY = y+1;
            }
        }

        if (resultObjs.length > 0) {
            let n_x1 = x2, n_x2 = x1, n_y1 = y2, n_y2 = y1;
            this.spans.forEach((oneSpan) => {
                if (oneSpan[1] < n_x1) n_x1 = oneSpan[1];
                if (oneSpan[2] > n_x2) n_x2 = oneSpan[2];
                if (oneSpan[0] < n_y1)  n_y1 = oneSpan[0];
                if (oneSpan[0] > n_y2)  n_y2 = oneSpan[0];
            });

            const newCrtObj = ConnectedObj.make([[n_x1, n_y1], [n_x2, n_y1], [n_x2, n_y2], [n_x1, n_y2]], this.spans, this.peaks,
                                                 this.id);
            resultObjs.unshift(newCrtObj);

        } else {
            resultObjs.unshift(this);
        }
        return resultObjs;
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

        return retval;
    }

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
            if (segNo === 0) continue;

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

    makeRectDrawObjs(makeImgPt, bCovered) {
        const segs = bCovered ? this.getOneSegments() : this.getZeroSegments();

        return Object.keys(segs).reduce((prev, y) => {
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
    }


    // trace the contour of the footprint outmost pixel counterclockwise
    // direction to trace the contour
    // 0: east, 1: NE, 2: N, 3: NW, 4: West, 5: SW, 6: S, 7: SE
    makePolygonDrawObjs(makeImgPt) {
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
        const POLYSTART = 2;
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

        const polyPts = [];

        // trace contour from lowest row of the matrix
        const startX = m[0].findIndex((x) => (x === 1));
        let   fromDirection = WEST;
        polyPts.push([startX, 0]);
        m[0][startX] = POLYSTART;

        let crtX = startX, crtY = 0;
        let nextX, nextY;
        let foundPt = null;


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
                if (m[nextY][nextX] === 1) {
                    //m[nextY][nextX] = 2;        // mark 'traced'
                    foundPt = [nextX, nextY];
                    fromDirection = (Number(next)+4)%8;
                    break;
                } else if (m[nextY][nextX] === POLYSTART) {
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

        const ptAry = polyPts.map((onePt) => makeImgPt(onePt[0]+x1, onePt[1]+y1));

        const polygonObj = ShapeDataObj.makePolygon(ptAry);
        polygonObj.id = this.id;

        polygonObj.centerPt = makeImgPt((x1+x2)/2, (y1+y2)/2);
        polygonObj.parentConnectObj = this;
        polygonObj.id = this.id;

        return polygonObj;
    }

    makePointObjsOnPeaks(makeImgPt) {
        if (!this.peaks) return [];

        const pointObjs = this.peaks.map((onePeak) => {
            const pt = makeImgPt(onePeak[0], onePeak[1]);

            const pointObj = makePointDataObj(pt);
            pointObj.id = this.id;
            return pointObj;
        });

        return pointObjs;
    }

    containPoint(pt) {
        const {x1, x2, y1, y2} = this;

        return (pt.x >= x1 && pt.x <= x2 && pt.y >= y1 && pt.y <= y2);
    }
}