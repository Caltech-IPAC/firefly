import chroma from 'chroma-js';
import {getHistgramForNorder, getProGradeTileNumbers, getTile} from '../../tables/HpxIndexCntlr';
import {getBWBackground} from '../../util/Color';
import CoordSys from '../../visualize/CoordSys';
import {TextLocation} from '../../visualize/draw/DrawingDef';
import {DrawSymbol} from '../../visualize/draw/DrawSymbol';
import FootprintObj from '../../visualize/draw/FootprintObj';
import PointDataObj from '../../visualize/draw/PointDataObj';
import ShapeDataObj, {UnitType} from '../../visualize/draw/ShapeDataObj';
import {getCornersForCell} from '../../visualize/HiPSUtil';
import {makeDevicePt} from '../../visualize/Point';
import {computeSimpleDistance, computeSimpleSlope, lineIntersect2, RtoD} from '../../visualize/VisUtil';
import {
    BOX_GROUP_TYPE, ELLIPSE_GROUP_TYPE, HEALPIX_GROUP_TYPE, HEAT_MAP_GROUP_TYPE, HPX_STRETCH_LINEAR,
    HPX_STRETCH_LINEAR_COMPRESSED, HPX_STRETCH_LOG
} from './HpxCatalogUtil';

export function makeSmartGridTypeGroupDrawPoint({idxData, count, norder, showLabels, ipix, cell, cc, drawingDef,
                                                    selected, selectedCnt, tblIdx, groupType,heatMapStretch}) {
    const devC = getDevPointAry(idxData,cc,cell,ipix,norder);
    if (devC.length < 4) return devC.length ? [makeSingleDrawPoint(selected,tblIdx,devC[0],drawingDef)] : [];

    switch (groupType) {
        case BOX_GROUP_TYPE:
            return makeBoxGroupPoint(devC,tblIdx,norder, ipix, count,selected,selectedCnt,drawingDef);
        case HEALPIX_GROUP_TYPE:
            return makeHealpixGroupPoint(devC,tblIdx,norder, ipix, count,selected,selectedCnt,drawingDef);
        case HEAT_MAP_GROUP_TYPE:
            return makeColorFillGroupPoint(idxData,devC,tblIdx,norder, showLabels, ipix, count,selected,selectedCnt,drawingDef,heatMapStretch);
        case ELLIPSE_GROUP_TYPE:
        default:
            return makeEllipseGroupPoint(idxData,devC,tblIdx,norder, ipix, count,selected,selectedCnt,drawingDef);
    }
}

function makeEllipseGroupPoint(idxData, devC, tblIdx, norder, ipix, count, selected, selectedCnt, drawingDef) {

    const {offX,offY,textOffX,textOffY}= getGroupOffset(tblIdx);
    const {dm,angle,maxD,minD}= getMidPointAndDist(devC);
    const dmMod= makeDevicePt(dm.x+offX, dm.y+offY);
    const textMod= makeDevicePt(dm.x+textOffX, dm.y+textOffY);


    const areaPoint = ShapeDataObj.makeEllipse(dmMod, minD * .8, maxD * .9, UnitType.PIXEL, angle, UnitType.PIXEL, false);
    areaPoint.renderOptions = {lineDash: [10,5], shadow: {blur: 1, color: '#ffffff', offX: 1, offY: 1,}};
    areaPoint.lineWidth = 1;
    areaPoint.norder = norder;
    areaPoint.ipix = ipix;
    areaPoint.forceWptForRegion= true;

    const textPt = PointDataObj.make(textMod);
    textPt.symbol = DrawSymbol.TEXT;
    textPt.text = `${count}`;
    textPt.textOffset = makeDevicePt(-2, 0);
    textPt.textLoc = TextLocation.CENTER;


    const ptAry = [areaPoint, textPt];
    if (selected) {
        areaPoint.color = drawingDef.selectedColor;
        if (!isNaN(selectedCnt) && selectedCnt !== count) {
            textPt.textOffset = makeDevicePt(2, 0);
            textPt.text = `/ ${count}`;
            const selectionCntPt = PointDataObj.make(textMod);
            selectionCntPt.text = `${selectedCnt}`;
            selectionCntPt.textOffset = makeDevicePt((textPt.text.length - 1) * -5, 0);
            selectionCntPt.textLoc = TextLocation.CENTER;
            selectionCntPt.symbol = DrawSymbol.TEXT;
            selectionCntPt.color = drawingDef.selectedColor;
            selectionCntPt.norder = norder;
            selectionCntPt.ipix = ipix;
            ptAry.push(selectionCntPt);
        } else {
            textPt.color = drawingDef.selectedColor;
        }
    }
    return ptAry;

}

function makeHealpixGroupPoint(devC, tblIdx, norder, ipix, count, selected, selectedCnt, drawingDef) {
    const {dm}= getMidPointAndDist(devC);
    const {textOffX,textOffY}= getGroupOffset(tblIdx);
    const textPt= PointDataObj.make(makeDevicePt(dm.x+textOffX,dm.y+textOffY));
    textPt.size= getPtSize(count);
    textPt.text= `${count}`;
    textPt.textOffset= makeDevicePt(-2, 0);
    textPt.textLoc= TextLocation.CENTER;
    textPt.norder= norder;
    textPt.ipix= ipix;
    textPt.symbol= DrawSymbol.TEXT;

    const footPrint= FootprintObj.make([devC]);
    footPrint.norder= norder;
    footPrint.ipix= ipix;
    footPrint.renderOptions = {shadow: {blur: 1, color: '#ffffff', offX: 1, offY: 1,}};
    const ptAry= [footPrint,textPt];
    if (selected) {
        footPrint.color = drawingDef.selectedColor;
        if (!isNaN(selectedCnt) && selectedCnt !== count) {
            textPt.textOffset = makeDevicePt(2, 0);
            textPt.text = `/ ${count}`;
            const selectionCntPt = PointDataObj.make(makeDevicePt(dm.x+textOffX,dm.y+textOffY));
            selectionCntPt.text = `${selectedCnt}`;
            selectionCntPt.textOffset = makeDevicePt((textPt.text.length - 1) * -5, 0);
            selectionCntPt.textLoc = TextLocation.CENTER;
            selectionCntPt.symbol = DrawSymbol.TEXT;
            selectionCntPt.color = drawingDef.selectedColor;
            selectionCntPt.norder = norder;
            selectionCntPt.ipix = ipix;
            ptAry.push(selectionCntPt);
        } else {
            textPt.color = drawingDef.selectedColor;
        }
    }
    return ptAry;
}


function makeBoxGroupPoint(devC, tblIdx, norder, ipix, count, selected, selectedCnt, drawingDef) {
    const {dm}= getMidPointAndDist(devC);
    const {offX,offY}= getGroupOffset(tblIdx);

    const point= PointDataObj.make(makeDevicePt(dm.x+offX,dm.y+offY));
    point.size= getPtSize(count);
    point.text= `${count}`;
    point.textOffset= makeDevicePt(-2, 0);
    point.textLoc= TextLocation.CENTER;
    point.norder= norder;
    point.ipix= ipix;
    point.symbol= DrawSymbol.BOXCIRCLE;
    const ptAry= [point];
    if (selected) {
        point.color=drawingDef.selectedColor;
        if (!isNaN(selectedCnt) && selectedCnt!==count) {
            point.symbol= DrawSymbol.CIRCLE;
            const p2= PointDataObj.make(dm);
            p2.size= getPtSize(count);
            p2.symbol= DrawSymbol.SQUARE;
            p2.norder= norder;
            p2.ipix= ipix;
            ptAry.push(p2);
        }
    }
    return ptAry;
}

function makeColorFillGroupPoint(idxData, devC, tblIdx, norder, showLabels, ipix, count, selected, selectedCnt, drawingDef={},heatMapStretch) {
    const {dm}= getMidPointAndDist(devC);
    const {color,selectedColor}= drawingDef;

    const footPrint= FootprintObj.make([devC]);
    footPrint.fill= true;
    const newColor= getColorFromColorMap(heatMapStretch, color, selectedColor, selected, idxData, norder, count);

    footPrint.fillStyle= newColor;
    footPrint.norder= norder;
    footPrint.ipix= ipix;

    const ptAry= [footPrint];
    if (showLabels) {
        const textPt= PointDataObj.make(makeDevicePt(dm.x,dm.y));
        textPt.size= getPtSize(count);
        textPt.text= `${count}`;
        textPt.textOffset= makeDevicePt(-2, 0);
        textPt.textLoc= TextLocation.CENTER;
        textPt.norder= norder;
        textPt.ipix= ipix;
        textPt.symbol= DrawSymbol.TEXT;
        textPt.color=getBWBackground(newColor);
        ptAry.push(textPt);
    }
    return ptAry;
}


function getDevPointAry(idxData,cc, cell, ipix,norder) {
    const devC = cell.wpCorners.map((wp) => cc.getDeviceCoords(wp)).filter(Boolean);
    if (devC.length < 4) return devC;

    let pgTiles= getProGradeTileNumbers(ipix).map( (ipix) => getTile(idxData.orderData, norder+1, ipix));
    pgTiles= pgTiles?.filter(Boolean);
    if (!pgTiles || pgTiles.length!==4) return devC;
    let useAllTiles= pgTiles.every( (t) => t);
    if (!useAllTiles) useAllTiles= (pgTiles[1] && pgTiles[2]) || (pgTiles[0] && pgTiles[3]);

    let wpAry;
    if (useAllTiles) {
        wpAry= cell.wpCorners;
    }
    else {
        const filteredPg= pgTiles.filter( (t) => t);
        if (filteredPg.length===1) {
            const pgTilesDeeper= getProGradeTileNumbers(filteredPg[0].pixel)
                .map( (ipix) => getTile(idxData.orderData, norder+2, ipix))
                .filter( (t) => t);
            if (pgTilesDeeper.length===1) {
                wpAry= getCornersForCell(norder+2,pgTilesDeeper[0].pixel,CoordSys.EQ_J2000)?.wpCorners ?? [];
            }
            else {
                wpAry= getCornersForCell(norder+1,filteredPg[0].pixel,CoordSys.EQ_J2000)?.wpCorners ?? [];
            }
        }
        else if (filteredPg.length===2) {
            const {wpCorners:c1}= getCornersForCell(norder+1,filteredPg[0].pixel,CoordSys.EQ_J2000);
            const {wpCorners:c2}= getCornersForCell(norder+1,filteredPg[1].pixel,CoordSys.EQ_J2000);
            if (c1?.length===4 && c2.length===4) {
                wpAry= adjoiningToOne(cc, c1,c2);
            }
            else {
                return devC;
            }
            
        }
        else {
            wpAry= cell.wpCorners;
        }
    }

    return  wpAry.map((wp) => cc.getDeviceCoords(wp)).filter(Boolean);
}


function getMidPointAndDist(devC) {
    const mp1 = makeDevicePt((devC[0].x + devC[1].x) / 2, (devC[0].y + devC[1].y) / 2);
    const mp2 = makeDevicePt((devC[1].x + devC[2].x) / 2, (devC[1].y + devC[2].y) / 2);
    const aveX = devC.reduce((total, p) => total + p.x, 0) / devC.length;
    const aveY = devC.reduce((total, p) => total + p.y, 0) / devC.length;
    const center = makeDevicePt(aveX, aveY);

    const d1 = computeSimpleDistance(mp1, center);
    const d2 = computeSimpleDistance(mp2, center);

    const minD = Math.min(d1, d2);
    const maxD = Math.max(d1, d2);

    const slope= maxD===d2 ? computeSimpleSlope(mp2,center) : computeSimpleSlope(mp1,center);
    const angle= Math.atan( -1/slope);
    const angleD= RtoD * angle;
    return {dm: center,d1,d2,angle, angleD,maxD,minD};
}



function getGroupOffset(tblIdx) {
    if (tblIdx<1) return {offX:0,offY:0,textOffX:0,textOffY:0};
    if (tblIdx % 4 === 1) return {offX:3,offY:3,textOffX:12,textOffY:12};
    if (tblIdx % 4 === 2) return {offX:-3,offY:-3,textOffX:-12,textOffY:-12};
    if (tblIdx % 4 === 3) return {offX:-3,offY:3,textOffX:-12,textOffY:12};
    if (tblIdx % 4 === 0) return {offX:3,offY:-3,textOffX:12,textOffY:-12};
    return {offX:0,offY:0,textOffX:0,textOffY:0};
}




function getPtSize(count) {
    if (count < 30) return 10;
    else if (count < 100) return 11;
    else if (count < 200) return 12;
    else if (count < 400) return 13;
    else if (count < 800) return 14;
    else if (count < 1000) return 15;
    else if (count < 10000) return 18;
    else if (count < 20000) return 21;
    return 25;
}


export function makeSingleDrawPoint(selected, idx, wp, drawingDef) {
    const obj = PointDataObj.make(wp);
    obj.fromRow = idx;
    if (selected) obj.color = drawingDef.selectedColor;
    return obj;
}


export function adjoiningToOne(cc,wpAry1,wpAry2) {
    const normalize= (v) => Math.trunc(v*10**6);
    let x,y;
    const tmp1= [...wpAry1];
    const tmp2= [...wpAry2];

    for(let i=0; (i<4); i++) {
        x= normalize(tmp1[i].x);
        y= normalize(tmp1[i].y);
        const foundIdx= tmp2.findIndex( (wp) => wp && normalize(wp.x)===x && normalize(wp.y)===y);
        if (foundIdx>-1) {
            tmp1[i]= undefined;
            tmp2[foundIdx]= undefined;
        }
    }

    const ary= [ ...tmp1.filter(Boolean), ...tmp2.filter(Boolean) ];
    const devC = ary.map((wp) => cc.getDeviceCoords(wp)).filter(Boolean);

    const d= devC.toSorted( (p1, p2) =>  {
        if (p1.x===p2.x) {
           return p1.y-p2.y;
        }
        return p1.x-p2.x;
    });
    const cr1= lineIntersect2(d[0].x,d[0].y,  d[1].x,d[1].y , d[2].x,d[2].y,  d[3].x,d[3].y);
    const cr2= lineIntersect2(d[1].x,d[1].y,  d[2].x,d[2].y , d[3].x,d[3].y,  d[0].x,d[0].y);
    if (cr1 || cr2) {
        const tmp= d[2];
        d[2]= d[3];
        d[3]= tmp;
    }
    return d.map( (dp) => cc.getWorldCoords(dp));
}



function getColorFromColorMap(heatMapStretch, color, selectedColor, selected, idxData, norder, count) {
    const inColor= selected?selectedColor:color;
    switch (heatMapStretch) {
        case HPX_STRETCH_LOG :
            return getColorFromColorMapLog(inColor,idxData,norder,count);
        case HPX_STRETCH_LINEAR_COMPRESSED :
            return getColorFromColorMapLinearCompressed(inColor,idxData,norder,count);
        case HPX_STRETCH_LINEAR :
        default:
            return getColorFromColorMapLinear(inColor,idxData,norder,count);
    }
}

const chromaCache= new Map();

function getColorFromColorMapLog(inColor, idxData, norder, count) {
    const hist=getHistgramForNorder(idxData,norder);
    if (!hist) return inColor;
    const {min,max}= hist;

    const newMin= min<5 ? 5 : min;
    const newMax= max-newMin;
    if (count<newMin) count= newMin;
    const newMaxLog= Math.log(newMax);
    const newMinLog= Math.log(newMin);
    const diff= newMaxLog-newMinLog;

    const binPercent= (Math.log(count)-newMinLog) / diff;
    return getMapColor(inColor,binPercent);
}

function getColorFromColorMapLinear(inColor, idxData, norder, count) {
    const hist=getHistgramForNorder(idxData,norder);
    if (!hist) return inColor;
    const {min,max,standDev,mean}= hist;
    const newMin= min<5 ? 5 : min;

    const sd3= standDev*3;
    const newMax= (mean+sd3 > max ? mean+sd3 : max);
    const diff= newMax-newMin;
    const minAdd= mean-standDev<0 ? standDev : standDev-newMin;

    const binPercent= ( (count-newMin)+minAdd)/ diff;
    return getMapColor(inColor,binPercent);
}

function getColorFromColorMapLinearCompressed(inColor, idxData, norder, count) {
    const hist=getHistgramForNorder(idxData,norder);
    if (!hist) return inColor;
    const {min,max,standDev,mean}= hist;
    const newMin= min<5 ? 5 : min;

    const sd4= standDev*4;
    const newMax= (mean+sd4 < max ? mean+sd4 : max);
    const diff= newMax-newMin;
    const minAdd= mean-standDev<0 ? standDev : standDev-newMin;

    const binPercent= ( (count-newMin)+minAdd)/ diff;
    return getMapColor(inColor,binPercent);
}


function getMapColor(inColor,binPercent) {
    const percent= binPercent>1 ? 1 : binPercent;
    const color= inColor;
    let getColor= chromaCache.get(inColor);
    if (!getColor) {
        getColor= chroma.scale([chroma(color).brighten(3),  chroma(color).darken(2)]  );
        chromaCache.set(inColor,getColor);
    }
    return getColor(percent).toString();
}