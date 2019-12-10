import {get} from 'lodash';

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 *
 * Draw one HiPS tile by breaking it into 2 triangle and draw each. This function follows the aladin's example
 *
 * @param ctx - canvas context
 * @param img - the image tile
 * @param {Array.<DevicePt>} cornersAry
 * @param tileSize - width/height of image tile
 * @param offsetPt
 * @param applyCorrection
 * @param norder
 */
export function drawOneHiPSTile(ctx, img, cornersAry, tileSize, offsetPt, applyCorrection, norder) {

    const delta = norder<=3 ? 0.2 : 0;
    const triangles= get(window,'firefly.hipsTriangles',2);
    

    if (triangles===2) {
        const triangle1= [{x:tileSize-delta, y:tileSize-delta}, {x:tileSize-delta, y:delta}, {x:delta, y:tileSize-delta}];
        const triangle2= [ {x:tileSize-delta, y:delta}, {x:delta, y:tileSize-delta}, {x:delta, y:delta} ];

        drawTexturedTriangle(ctx, img,
            cornersAry[0], cornersAry[1], cornersAry[3],
            triangle1[0], triangle1[1], triangle1[2],
            offsetPt, applyCorrection);
        drawTexturedTriangle(ctx, img,
            cornersAry[1], cornersAry[3], cornersAry[2],
            triangle2[0], triangle2[1], triangle2[2],
            offsetPt, applyCorrection);
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

        drawTexturedTriangle(ctx, img,
            cornersAry[0], cPt, cornersAry[1],
            triangle1[0], triangle1[1], triangle1[2],
            offsetPt, applyCorrection);
        drawTexturedTriangle(ctx, img,
            cornersAry[1], cPt, cornersAry[2],
            triangle2[0], triangle2[1], triangle2[2],
            offsetPt, applyCorrection);
        drawTexturedTriangle(ctx, img,
            cornersAry[3], cPt, cornersAry[2],
            triangle3[0], triangle3[1], triangle3[2],
            offsetPt, applyCorrection);
        drawTexturedTriangle(ctx, img,
            cornersAry[0], cPt, cornersAry[3],
            triangle4[0], triangle4[1], triangle4[2],
            offsetPt, applyCorrection);

    }
}


/**
 * reproject and draw the part of the image defined by triangle source point 0,1,2 to triangle pt 0,1,2 of the destination image
 *
 * this code from Aladin which took it from demo code http://www.dhteumeuleu.com/lab/image3D.html
 * it takes a triangle from a source image and maps to a triangle in the target image
 *
 * This is effectively 3d triangle drawing without using web GL.
 *
 * I don't really understand the affine transform math. it is a far more advanced combination
 * of rotation, translation and scaling than normally done.
 *
 * @param ctx
 * @param img
 * @param pt0
 * @param pt1
 * @param pt2
 * @param srcP0
 * @param srcP1
 * @param srcP2
 * @param {Point} offsetPt
 * @param applyCorrection
 */
function drawTexturedTriangle(ctx, img, pt0, pt1, pt2, srcP0, srcP1, srcP2,
                              offsetPt, applyCorrection= false) {

    let {x:x0, y:y0}= pt0;
    let {x:x1, y:y1}= pt1;
    let {x:x2, y:y2}= pt2;
    let {x:srcX0, y:srcY0}= srcP0;
    let {x:srcX1, y:srcY1}= srcP1;
    let {x:srcX2, y:srcY2}= srcP2;


    const {x:offX, y:offY}= offsetPt;
    srcX0 += offX;
    srcX1 += offX;
    srcX2 += offX;
    srcY0 += offY;
    srcY1 += offY;
    srcY2 += offY;

    const xc = (x0 + x1 + x2) / 3;
    const yc = (y0 + y1 + y2) / 3;
    ctx.save();


    let coeff = 0.02;

    // ---- scale triangle by (1 + coeff) to remove anti-aliasing and draw ----
    ctx.beginPath();
    ctx.moveTo(((1+coeff) * x0 - xc * coeff), ((1+coeff) * y0 - yc * coeff));
    ctx.lineTo(((1+coeff) * x1 - xc * coeff), ((1+coeff) * y1 - yc * coeff));
    ctx.lineTo(((1+coeff) * x2 - xc * coeff), ((1+coeff) * y2 - yc * coeff));
    ctx.closePath();
    ctx.clip();

    // this is needed to prevent to see some lines between triangles
    if (applyCorrection) {
        coeff = 0.01;
        x0 = ((1+coeff) * x0 - xc * coeff);
        y0 = ((1+coeff) * y0 - yc * coeff);
        x1 = ((1+coeff) * x1 - xc * coeff);
        y1 = ((1+coeff) * y1 - yc * coeff);
        x2 = ((1+coeff) * x2 - xc * coeff);
        y2 = ((1+coeff) * y2 - yc * coeff);
    }

    // ---- transform texture ----
    const d_inv = 1/ (srcX0 * (srcY2 - srcY1) - srcX1 * srcY2 + srcX2 * srcY1 + (srcX1 - srcX2) * srcY0);
    ctx.transform(
        -(srcY0 * (x2 - x1) -  srcY1 * x2  + srcY2 *  x1 + (srcY1 - srcY2) * x0) * d_inv, // m11
        (srcY1 *  y2 + srcY0  * (y1 - y2) - srcY2 *  y1 + (srcY2 - srcY1) * y0) * d_inv, // m12
        (srcX0 * (x2 - x1) -  srcX1 * x2  + srcX2 *  x1 + (srcX1 - srcX2) * x0) * d_inv, // m21
        -(srcX1 *  y2 + srcX0  * (y1 - y2) - srcX2 *  y1 + (srcX2 - srcX1) * y0) * d_inv, // m22
        (srcX0 * (srcY2 * x1  -  srcY1 * x2) + srcY0 * (srcX1 *  x2 - srcX2  * x1) + (srcX2 * srcY1 - srcX1 * srcY2) * x0) * d_inv, // imageOffsetX
        (srcX0 * (srcY2 * y1  -  srcY1 * y2) + srcY0 * (srcX1 *  y2 - srcX2  * y1) + (srcX2 * srcY1 - srcX1 * srcY2) * y0) * d_inv  // imageOffsetY
    );
    ctx.drawImage(img, 0, 0);
    ctx.restore();
}
