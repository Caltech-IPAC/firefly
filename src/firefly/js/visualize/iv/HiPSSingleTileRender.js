/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Matrix} from '../../externalSource/transformation-matrix-js/matrix';



const makeSource4Triangle= (centerPt, min, max) => {
    return [
        [{x:max, y:max}, centerPt, {x:max, y:min}],  // 3 >
        [{x:max, y:min}, centerPt, {x:min, y:min}], // 12 ^
        [{x:min, y:max}, centerPt, {x:min, y:min}], // 9 <
        [{x:max, y:max}, centerPt, {x:min, y:max}], // 6 v
    ];
};


/**
 * Compute the amount of correction based on how zoom in the display is, The more zoomed in the less correction
 * is needed. Too much correction make for distortion
 * @param tileSize
 * @param norder
 * @param desiredNorder
 * @param isMaxOrder
 * @return {number|number}
 */
function getCorrection(tileSize, norder,desiredNorder,isMaxOrder) {
    if (isMaxOrder) {
        const amountOver= desiredNorder-norder;
        if (amountOver>8) return 0;
        else if (amountOver>5) return .0001;
        else if (amountOver>3) return .0005;
        else if (amountOver>=2) return .0024;
        else if (amountOver===1) return .005;
        else return .01;
    }
    else {
        return tileSize < 80 ? .05 : tileSize < 150 ? .035 : .01;
    }
}



/**
 * Draw one HiPS tile by breaking it into 2 triangle and draw each. This function follows the aladin's example
 *
 * @param ctx - canvas context
 * @param img - the image tile
 * @param {Array.<DevicePt>} cornersAry - array of points of the 4 corners to copy the image to on the destination canvas
 * @param {Number} tileSize - width/height of image tile
 * @param {DevicePt|{x:number,y:number}} offset - an offset point
 * @param {boolean} isMaxOrder
 * @param {number} norder
 * @param {number} desiredNorder
 */
export function drawOneHiPSTile(ctx, img, cornersAry, tileSize, offset, isMaxOrder, norder, desiredNorder) {
    const triangles= norder===2 || (tileSize < 70) ? 2 : 4;
    const correction= getCorrection(tileSize,norder,desiredNorder,isMaxOrder);
    // correction= window.cor ?? correction;
    // window.firefly.debug && console.log(`tri:${triangles}, tileSize:${tileSize}, maxOrder:${isMaxOrder}, ${norder}, ${desiredNorder}, ${correction}`);

    if (triangles===2) {
        const triangle1= [{x:tileSize, y:tileSize}, {x:tileSize, y:0}, {x:0, y:tileSize}];
        const triangle2= [{x:tileSize, y:0}, {x:0, y:tileSize}, {x:0, y:0}];

        drawTexturedTriangle(ctx, img, offset, ...triangle1, cornersAry[0], cornersAry[1], cornersAry[3], correction, true);
        drawTexturedTriangle(ctx, img,offset, ...triangle2, cornersAry[1], cornersAry[3], cornersAry[2], correction, true);
    }
    else if (triangles===4) {
        const mp=tileSize/2;
        const cMx= cornersAry.reduce( (total,pt) => total+pt.x, 0)/cornersAry.length;
        const cMy= cornersAry.reduce( (total,pt) => total+pt.y, 0)/cornersAry.length;
        const cPt= {x:cMx,y:cMy};

        const [triangle1, triangle2, triangle3, triangle4]= makeSource4Triangle({x:mp, y:mp}, 0, tileSize);
        drawTexturedTriangle(ctx, img, offset, ...triangle1, cornersAry[0], cPt, cornersAry[1], correction, false); // 3 >
        drawTexturedTriangle(ctx, img, offset, ...triangle2, cornersAry[1], cPt, cornersAry[2], correction, false); // 12 ^
        drawTexturedTriangle(ctx, img, offset, ...triangle3, cornersAry[3], cPt, cornersAry[2], correction, false); // 9 <
        drawTexturedTriangle(ctx, img, offset, ...triangle4, cornersAry[0], cPt, cornersAry[3], correction, false); // 6 v
    }
    else if (triangles===16) { // keep this section around if we have to use it, it is not complete and not tested
        // const mp=tileSize/2;
        // const cMx= cornersAry.reduce( (total,pt) => total+pt.x, 0)/cornersAry.length;
        // const cMy= cornersAry.reduce( (total,pt) => total+pt.y, 0)/cornersAry.length;
        // const cPt= {x:cMx,y:cMy};
        //
        //
        //
        //
        //
        // bottom - right
        // const sec1cp= mp+mp/2;
        // const sec1CenPt= {x:sec1cp, y:sec1cp};
        // const triangle1_1= [{x:tileSize, y:tileSize}, sec1CenPt, {x:tileSize, y:mp}];  // 3
        // const triangle1_2= [{x:tileSize, y:tileSize}, sec1CenPt, {x:mp, y:tileSize}];  // 6
        // const triangle1_3= [{x:mp, y:tileSize}, sec1CenPt, {x:mp, y:mp}];              // 9
        // const triangle1_4= [{x:mp, y:mp}, sec1CenPt, {x:tileSize, y:tileSize}];        // 12

        // const srcTriangles= [
        //     ...makeSource4Triangle(mp+(mp/2),mp,tileSize), // bottom right
        //     ...makeSource4Triangle(mp-(mp/2), mp,tileSize), // top right
        //     ...makeSource4Triangle(mp+(mp/2), 0,mp), // bottom left
        //     ...makeSource4Triangle(mp/2, 0,mp), // bottom left
        // ];



        // top - right
        // const sec2cp= mp-mp/2;
        // const sec2CenPt= {x:sec2cp,y:sec2cp};
        //
        //
        //
        //
        //
        // // const triangle1= [{x:tileSize-delta, y:tileSize-delta}, {x:mp-delta, y:mp-delta}, {x:tileSize-delta, y:delta}];
        // const triangle2= [{x:tileSize-delta, y:delta}, {x:mp-delta, y:mp-delta}, {x:delta, y:delta}];
        // const triangle3= [{x:delta, y:tileSize-delta}, {x:mp-delta, y:mp-delta}, {x:delta, y:delta}];
        // const triangle4= [{x:tileSize-delta, y:tileSize-delta}, {x:mp-delta, y:mp-delta}, {x:delta, y:tileSize-delta}];
        //
        // drawTexturedTriangle(ctx, img, offset, ...triangle1, cornersAry[0], cPt, cornersAry[1], correction);
        // drawTexturedTriangle(ctx, img, offset, ...triangle2, cornersAry[1], cPt, cornersAry[2], correction);
        // drawTexturedTriangle(ctx, img, offset, ...triangle3, cornersAry[3], cPt, cornersAry[2], correction);
        // drawTexturedTriangle(ctx, img, offset, ...triangle4, cornersAry[0], cPt, cornersAry[3], correction);
    }
}

const scaleCoeff = 0.01;

/**
 * Reproject and draw the part of the image defined by triangle source point 0,1,2 to triangle pt 0,1,2 of the destination image
 *
 * This started with code from Aladin which it took from demo code http://www.dhteumeuleu.com/lab/image3D.html
 * it takes a triangle from a source image and maps to a triangle in the target image
 * I have modified it to use transformation-matrix-js since the triangle transform is well understood.
 * transformation-matrix-js is used to compute the affine transform to do the drawing
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
 * @param {boolean} centerPtCorrection - in a 4 triangle case this should be false, you don't want to have correction * internal to the tile
 * todo - we we do 16 triangle we need more than a boolean to specify where the correction is applied
 */
function drawTexturedTriangle(ctx, img, offset,
                              s0, s1, s2, {x:x0, y:y0}, {x:x1, y:y1}, {x:x2, y:y2}, correction= .01, centerPtCorrection= false) {
    const xc = (x0 + x1 + x2) / 3;
    const yc = (y0 + y1 + y2) / 3;

    const applyC= (v,c) => ((1+correction) * v - c * correction);

    const tgtCenterPt= centerPtCorrection ? [applyC(x1,xc), applyC(y1,yc)]  : [x1,y1] ;
    const tgtAry= correction ?
        // [ applyC(x0,xc), applyC(y0,yc), applyC(x1,xc), applyC(y1,yc), applyC(x2,xc), applyC(y2,yc)] :
        [ applyC(x0,xc), applyC(y0,yc), ...tgtCenterPt, applyC(x2,xc), applyC(y2,yc)] :
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