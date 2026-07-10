/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import chroma from 'chroma-js';
import {isTypedArray, reverse} from 'lodash';
import BrowserInfo from '../../util/BrowserInfo';
import {createCanvas, memorizeLastCall} from '../../util/WebUtil.js';
import {Band} from '../Band.js';
import {getColorTableDefinitionInfo} from './ColorTableDefinitions';

export const REVERSED_END_CHAR='R';
export const NO_COLOR_TABLE='NO_COLOR_TABLE';
export const PERCENT= 'PERCENT';
export const PACKED= 'PACKED';
export const RGB= 'RBG';


export const getColorTableMap = () => getColorTableDefinitionInfo().map;
export const getCbarTip = (baseId) => getColorTableDefinitionInfo().tipMap[baseId];
export const getCbarNumIds = () => Array.from({length: getColorTableDefinitionInfo().cbarTotal}, (v, i) => i + '');

export function isReversedColor(idStr)  {
    if (!idStr.endsWith(REVERSED_END_CHAR))  {
        return false;
    }
    const ctMap= getColorTableMap();
    const baseResult= ctMap[idStr];
    if (baseResult) return true;
    const originalId= idStr.substring(0,idStr.length - 1);
    const cAry= ctMap[originalId];
    return Boolean(cAry);
}

export function reverseId(idStr) {
    if (isReversedColor(idStr)) return idStr.substring(0,idStr.length - 1);
    if (getColorTableMap()[idStr]) return idStr+REVERSED_END_CHAR;
}

export const baseIdMatchesForOrRev= (baseId, forOrRevId) => {
    const r= isReversedColor(forOrRevId);
    return (!r && baseId===forOrRevId) || (r && baseId+REVERSED_END_CHAR===forOrRevId);
};




function getRawColorTableEntry(id) {
    const idStr= id+'';
    const ctMap= getColorTableMap();
    const baseResult= ctMap[idStr];
    if (baseResult) return baseResult;
    if (!idStr.endsWith(REVERSED_END_CHAR))  return undefined;
    const originalId= idStr.substring(0,idStr.length - 1);
    const cAry= ctMap[originalId];
    if (!cAry) return undefined;

    // now reverse it and add it to the map
    const colors=[];
    let j=0;
    for(let i=0;(i<cAry.length-1); i+=4) {
        colors[j++]={idx:cAry[i],r:cAry[i+1],g:cAry[i+2],b:cAry[i+3]};
    }
    const revColors= reverse(colors);

    const revCAry= [];
    for(let i=0;(i<revColors.length); i++) {
        const cAryIdx= i*4;
        revCAry[cAryIdx]=   255-revColors[i].idx;
        revCAry[cAryIdx+1]= revColors[i].r;
        revCAry[cAryIdx+2]= revColors[i].g;
        revCAry[cAryIdx+3]= revColors[i].b;
    }
    ctMap[originalId+REVERSED_END_CHAR] = Uint8Array.of(...revCAry);
    return ctMap[originalId+REVERSED_END_CHAR];
}

export async function getColorModelByGPUType(colorTableId, nanPixelColor=undefined) {
    const webGpu= await BrowserInfo.supportsWebGpu();
   return getColorModel(colorTableId,nanPixelColor,webGpu ? PACKED : PERCENT);
}




/**
 *
 * @param colorTableId
 * @param [nanPixelColor]
 * @param {String} [modelForm] one of PERCENT, PACKED, RGB
 * @return {*|undefined|Float32Array}
 */
export const getColorModel= (colorTableId,nanPixelColor=undefined,modelForm=RGB) => {

    if (colorTableId===NO_COLOR_TABLE) return undefined;
    const basePaletteData= getColorModelBase(colorTableId,modelForm);
    if (!nanPixelColor || !basePaletteData) return basePaletteData;

    const paletteData = modelForm===PERCENT ? Float32Array.of(...basePaletteData) : Uint32Array.of(...basePaletteData);
    const nanPix= nanPixelColor.length >2 ? nanPixelColor : [0,0,0];
    if (modelForm===PERCENT) {
        paletteData[3*255]     = nanPix[0]/255;
        paletteData[3*255 + 1] = nanPix[1]/255;
        paletteData[3*255 + 2] = nanPix[2]/255;
    }
    else if (modelForm===PACKED) {
        paletteData[255]= packColor(...nanPix);
    }
    else {
        paletteData[3*255]     = nanPix[0];
        paletteData[3*255 + 1] = nanPix[1];
        paletteData[3*255 + 2] = nanPix[2];
    }
	return paletteData;
};



/**
 *
 * @param colorTableId
 * @param {String} [modelForm] one of PERCENT, PACKED, RGB
 * @return {*|undefined|Float32Array}
 */
export const getColorModelBase= memorizeLastCall((colorTableId,modelForm=RGB) => {
    let old_dn, old_red, old_green, old_blue;
    let offset;
    let k;

    //palette: 3 bytes per color * 256 colors = 768 bytes in length
    const paletteData = modelForm===PERCENT ? new Float32Array(768) : new Uint32Array(768);
    const ct = getRawColorTableEntry(colorTableId);

    if (colorTableId === 'file') console.log('file color tables not yet supported');

    if (!ct) {
        console.log('ColorTable ERROR: no color table with the ID = ' + colorTableId);
        return [];
    }

    k = 0;
    let dn = ct[k];
    let red = ct[k + 1];
    let green = ct[k + 2];
    let blue = ct[k + 3];
    k += 4;

    while (true) {
        old_dn = dn;
        old_red = red;
        old_green = green;
        old_blue = blue;

        if (k >= ct.length) break;
        dn = ct[k];
        red = ct[k + 1];
        green = ct[k + 2];
        blue = ct[k + 3];
        k += 4;

        const inc = (dn > old_dn) ? 1 : -1;

        for (let kk = old_dn; kk !== dn; kk += inc) {
            if (kk >= 0 && kk <= 255) {
                offset = (kk - old_dn) / (dn - old_dn);
                const idx=3*kk;
                paletteData[idx] = (old_red + Math.trunc(offset * (red - old_red)));
                paletteData[idx + 1] = (old_green + Math.trunc(offset * (green - old_green)));
                paletteData[idx+ 2] = (old_blue + Math.trunc(offset * (blue - old_blue)));
            }
        }
    }

    if (old_dn >= 0 && old_dn <= 255) {
        const idx=3*old_dn;
        paletteData[idx] = old_red;
        paletteData[idx + 1] = old_green;
        paletteData[idx + 2] = old_blue;
    }

    switch (modelForm) {
        case PERCENT:
            return paletteData.map((p) => p/255);
        case PACKED:
            const packedPalette = new Uint32Array(256);
            for (let i = 0; i < 256; i++) {
                const i3= i*3;
                packedPalette[i] = packColor(paletteData[i3], paletteData[i3 + 1], paletteData[i3 + 2]);
            }
            return packedPalette;
        default:
            return paletteData;
    }

},3);


const toRGBAString= (r,g,b,a) => `rgba(${r}, ${g}, ${b}, ${a})`;
export const packColor= (r,g,b,a=255) => a<<24 | b<<16 | g<<8 | r; // Pack into little-endian, byte array will be r,g,b,a so write a,b,g,r

function drawLine(ctx,color, lineWidth, sx, sy, ex, ey) {
	ctx.save();
	ctx.lineWidth=lineWidth;
	ctx.strokeStyle=color;
	ctx.beginPath();
	ctx.moveTo(sx, sy);
	ctx.lineTo(ex, ey);
	ctx.stroke();
	ctx.restore();
}


export function makeColorTableImage(ctOrBand,width,height) {
	const div    = width / 254;
	const canvas= createCanvas(width,height);
	const ctx= canvas.getContext('2d');
	const band= isTypedArray(ctOrBand) ? Band.NO_BAND : ctOrBand;
    const ct= isTypedArray(ctOrBand) ? ctOrBand : undefined;
	ctx.lineWidth=1;

	for (let i=0; (i<width); i++) {
		const idx= Math.trunc(i/div);
		ctx.strokeStyle= band===Band.NO_BAND ? getCtColor(ct,idx) : get3CColor(band,i);
		drawLine(ctx,band===Band.NO_BAND ? getCtColor(ct,idx) : get3CColor(band,i), 1, i,0, i, height-1);
	}
	return canvas;
}

function get3CColor(band, idx) {
	switch (band) {
		case Band.RED: return toRGBAString(idx, 0, 0, 1);
		case Band.GREEN: return toRGBAString(0, idx, 0, 1);
		case Band.BLUE: return toRGBAString(0, 0, idx, 1);
	}
}

function getCtColor(ct,idx) {
	const pixel= idx*3;
	return toRGBAString( Math.trunc(ct[pixel]), Math.trunc(ct[pixel+1]), Math.trunc(ct[pixel+2]), 1 );
}

const UPPER = 'UPPER';
const LOWER = 'LOWER';

export function makeColorHistImage(ctOrBand,ctId, width,height, hist, histColorIdx) {
	const canvas= createCanvas(width,height);
	const ctx= canvas.getContext('2d');
	const band= isTypedArray(ctOrBand) ? Band.NO_BAND : ctOrBand;
	const ct= isTypedArray(ctOrBand) ? ctOrBand : undefined;
	const bottomColorSize= 4;
	ctx.lineWidth=1;

	ctx.fillStyle= band===Band.NO_BAND  && (ctId===1 || ctId===0) ?
		                      toRGBAString(0xCC, 0xCC, 0x99,1) : toRGBAString(181, 181, 181,1);
	ctx.fillRect(0,0,width,height);

	const upperBounds= -1;
	const lowerBounds= -1;
	const upperBounds2= -1;
	const lowerBounds2= -1;
	const graphWidth= width;
	const graphHeight= height;
	let   y, idx, lastIdx=0, stepSize;

	const       yTop= height - 1;
	const       yBottom= 0;
	const div= graphWidth / hist.length;
	let max= 0;
	let max2= 0;
	let min= Number.MAX_VALUE;
	let markOutOfBounds= false;
	const do2nd= true;

	const lineDataSize= Array(graphWidth);
	const orginalHistogramIdx= Array(graphWidth);
	ctx.strokeStyle= '#000000';
	for(let i=0; (i<hist.length); i++) {
		if (hist[i] > max) max= hist[i];
		if (hist[i] > max2 && hist[i] < max) max2= hist[i];
		if (hist[i] < min) min= hist[i];
	}
	if (do2nd) max= max2;


	const weight= max/(graphHeight-1);
	const maxY= Math.trunc(max / weight) - bottomColorSize;


	for (let i=0; (i<graphWidth); i++) {
		idx= Math.trunc(i/div);
		stepSize= idx-lastIdx;
		lastIdx = idx;

		// if there is not data check the bins before and after,  to find a better line to draw
		if (hist[idx]===0 && stepSize>=3) {
			if (hist[idx-1] > hist[idx+1]) idx= idx-1;
			else                           idx= idx+1;
		}

		y= Math.trunc(hist[idx] / weight);
		if (hist[idx] > 0 && y < 2) y= 2;

		if (y > maxY) {
			y= maxY;
			markOutOfBounds= true;
		}
		else {
			markOutOfBounds= false;
		}
		const cidx= histColorIdx[idx] & 0xFF;
		const color= band===Band.NO_BAND  ? getCtColor(ct,cidx) : get3CColor(band,cidx);

		drawLine(ctx,color, 1, i, yTop, i, yTop-(y+bottomColorSize));
		drawLine(ctx,'#FFFFFF',1,  i, yTop-(y+1+bottomColorSize), i, yTop-(y+1+bottomColorSize));
		lineDataSize[i]       = hist[idx];
		orginalHistogramIdx[i]= idx;
		if (markOutOfBounds) drawOutofBounds(ctx, i, yTop-(y+2+bottomColorSize));

	}


	if (upperBounds2 > -1) drawBounds(ctx,'#0000FF' , Math.trunc(upperBounds2 * div), yTop, yBottom, UPPER);
	if (lowerBounds2 > -1) drawBounds(ctx,'#0000FF',  Math.trunc(lowerBounds2 * div), yTop, yBottom, LOWER);
	if (upperBounds > -1) drawBounds(ctx, '#FF0000', Math.trunc(upperBounds * div), yTop, yBottom, UPPER);
	if (lowerBounds > -1) drawBounds(ctx, '#FF0000', Math.trunc(lowerBounds * div), yTop, yBottom, LOWER);

	return canvas;
}

function drawOutofBounds(ctx, x, y) {
	drawLine(ctx, '#FF0000', 1,  x-1, y+1, x, y);
	drawLine(ctx, '#FF0000', 1 , x+1, y+1, x, y);
}

function drawBounds(ctx, color, x, yTop, yBottom, which) {
	const dir= (which===LOWER) ? 5 : -5;
	drawLine(ctx,color,  1, x, yTop,        x, yBottom);
	drawLine(ctx, color, 1, x+dir, yTop,    x, yTop);
	drawLine(ctx, color, 1, x+dir, yBottom, x, yBottom);
}


export const findAContrastColor= memorizeLastCall( (colorTableId) => {
    const colors=[];
    let j=0;
    const cAry= getRawColorTableEntry(colorTableId);
    if (!cAry) return [0,0,0];
    for(let i=0;(i<cAry.length); i+=4) {
        colors[j++]=[cAry[i+1],cAry[i+2],cAry[i+3]];
    }
    let bestHue = -1;
    let maxDistance = -1;
    const hues= colors.map( (c) => chroma(c).hsl()[0]).filter( (n) =>n);
    const numSteps= 20;
    for(let i=0;(i<numSteps); i++) {
        const potentialHue = (360 / numSteps) * i;
        let minDistanceToExisting = Infinity;
        for (const hue of hues) {
            // Calculate distance on the color wheel (0 to 360 degrees)
            let distance = Math.abs(potentialHue - hue);
            if (distance > 180) distance = 360 - distance;// Handle wrap-around on the color wheel
            if (distance < minDistanceToExisting) minDistanceToExisting = distance;
        }
        if (minDistanceToExisting > maxDistance) {
            maxDistance = minDistanceToExisting;
            bestHue = potentialHue;
        }
    }
    return chroma({ h:bestHue, s:.64, l:.64}).rgb();
}, 100);