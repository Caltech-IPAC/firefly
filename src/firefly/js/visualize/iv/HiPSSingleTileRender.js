/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Matrix} from '../../externalSource/transformation-matrix-js/matrix';

/**
 * Draw one HiPS tile by breaking it into 2 triangle and draw each. This function follows the aladin's example
 *
 * @param ctx - canvas context
 * @param img - the image tile
 * @param {Array.<DevicePt>} cornersAry - array of points of the 4 corners to copy the image to on the destination canvas
 * @param {Number} tileSize - width/height of image tile
 * @param {DevicePt|{x:number,y:number}} offset - an offset point
 * @param {Number} norder
 */
export function drawOneHiPSTile(ctx, img, cornersAry, tileSize, offset, norder) {

    // const delta = norder<=3 ? 0.2 : 0;
    const delta = 0;
    // const triangles= window.firefly?.hipsTriangles ?? 4;
    const triangles= tileSize < 80 ? 2 : 4;
    const correction= tileSize < 80 ? .05 : tileSize < 150 ? .03 : .01;

    if (triangles===2) {
        const triangle1= [{x:tileSize-delta, y:tileSize-delta}, {x:tileSize-delta, y:delta}, {x:delta, y:tileSize-delta}];
        const triangle2= [ {x:tileSize-delta, y:delta}, {x:delta, y:tileSize-delta}, {x:delta, y:delta} ];

        drawTexturedTriangle(ctx, img, offset, ...triangle1, cornersAry[0], cornersAry[1], cornersAry[3], correction);
        drawTexturedTriangle(ctx, img,offset, ...triangle2, cornersAry[1], cornersAry[3], cornersAry[2], correction);
    }
    else if (triangles===4) {
        const mp=tileSize/2;
        const cMx= cornersAry.reduce( (total,pt) => total+pt.x, 0)/cornersAry.length;
        const cMy= cornersAry.reduce( (total,pt) => total+pt.y, 0)/cornersAry.length;
        const cPt= {x:cMx,y:cMy};

        const triangle1= [{x:tileSize-delta, y:tileSize-delta}, {x:mp-delta, y:mp-delta}, {x:tileSize-delta, y:delta}];
        const triangle2= [{x:tileSize-delta, y:delta}, {x:mp-delta, y:mp-delta}, {x:delta, y:delta}];
        const triangle3= [{x:delta, y:tileSize-delta}, {x:mp-delta, y:mp-delta}, {x:delta, y:delta}];
        const triangle4= [{x:tileSize-delta, y:tileSize-delta}, {x:mp-delta, y:mp-delta}, {x:delta, y:tileSize-delta}];

        drawTexturedTriangle(ctx, img, offset, ...triangle1, cornersAry[0], cPt, cornersAry[1], correction);
        drawTexturedTriangle(ctx, img, offset, ...triangle2, cornersAry[1], cPt, cornersAry[2], correction);
        drawTexturedTriangle(ctx, img, offset, ...triangle3, cornersAry[3], cPt, cornersAry[2], correction);
        drawTexturedTriangle(ctx, img, offset, ...triangle4, cornersAry[0], cPt, cornersAry[3], correction);
    }
}

const scaleCoeff = 0.01;

/**
 * Reproject and draw the part of the image defined by triangle source point 0,1,2 to triangle pt 0,1,2 of the destination image
 *
 * This started with code from Aladin which took it from demo code http://www.dhteumeuleu.com/lab/image3D.html
 * it takes a triangle from a source image and maps to a triangle in the target image
 * I have modified it to use transformation-matrix-js since the triangle transform is well understood.
 * transformation-matrix-js is used to compute the affine transform, triangle conversion affine transform
 *
 * This is effectively 3d triangle drawing without using web GL.
 *
 * @param ctx - canvas context
 * @param img - source image
 * @param {Point} offset an offset point, an offset into existing image of the source pt
 * @param {Point} s0 - source point 0
 * @param {Point} s1 - source point 1
 * @param {Point} s2 - source point 2
 * @param {Point} pt0 - destination point 0
 * @param {Point} pt1 - destination point 1
 * @param {Point} pt2 - destination point 2
 * @param {number} correction
 */
function drawTexturedTriangle(ctx, img, offset,
                              s0, s1, s2, {x:x0, y:y0}, {x:x1, y:y1}, {x:x2, y:y2}, correction= .01) {
    const xc = (x0 + x1 + x2) / 3;
    const yc = (y0 + y1 + y2) / 3;

    const applyC= (v,c) => ((1+correction) * v - c * correction);
    const tgtAry= correction ?
        [ applyC(x0,xc), applyC(y0,yc), applyC(x1,xc), applyC(y1,yc), applyC(x2,xc), applyC(y2,yc)] :
        [x0,y0,x1,y1,x2,y2];

    const srcPtAry= [s0.x+offset.x, s0.y+offset.y, s1.x+offset.x, s1.y+offset.y, s2.x+offset.x, s2.y+offset.y];

    // ---- set up clip region to prevent anti-aliasing, scale triangle by (1 + scaleCoeff)
    ctx.save();
    ctx.beginPath();
    ctx.moveTo(((1+scaleCoeff) * x0 - xc * scaleCoeff), ((1+scaleCoeff) * y0 - yc * scaleCoeff));
    ctx.lineTo(((1+scaleCoeff) * x1 - xc * scaleCoeff), ((1+scaleCoeff) * y1 - yc * scaleCoeff));
    ctx.lineTo(((1+scaleCoeff) * x2 - xc * scaleCoeff), ((1+scaleCoeff) * y2 - yc * scaleCoeff));
    ctx.closePath();
    ctx.clip();
    // --- setup triangle transform and draw
    ctx.setTransform(Matrix.fromTriangles(srcPtAry, tgtAry));
    ctx.drawImage(img, 0, 0);
    ctx.restore();
}