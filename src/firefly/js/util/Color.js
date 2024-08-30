import chroma from 'chroma-js';
import {isArray} from 'lodash';

export default { makeSimpleColorMap, getBWBackground, shadeColor};

export const toRGBA= (color, alpha=1) => chroma.valid(color) ? chroma(color).alpha(alpha).rgba() : [0,0,0,alpha];
export const toRGB= (color) => chroma.valid(color) ? chroma(color).rgb() : [0,0,0];

/**
 * @summary extract R,G,B amd alpha value from color string like 'rgba(...)', 'rgb(...)' or '#rrggbb'
 * @param color
 * @returns {Array} [r, g, b, a]
 */
export const getRGBA= (color)  => chroma.valid(color) ? chroma(color).rgba() : [0,0,0,1];


export const brighter = (colorStr,level=1) =>
    chroma.valid(colorStr) ? chroma(colorStr).brighten(level).toString() : undefined;

/**
 * Creates a new <code>Color</code> that is a darker version of this
 * <code>Color</code>.
 * <p>
 * This method applies an arbitrary scale factor to each of the three RGB
 * components of this <code>Color</code> to create a darker version of
 * this <code>Color</code>.  Although <code>brighter</code> and
 * <code>darker</code> are inverse operations, the results of a series
 * of invocations of these two methods might be inconsistent because
 * of rounding errors.
 * @param colorStr
 * @param [level]
 * @return {*} a new <code>Color</code> object that is
 *                    a darker version of this <code>Color</code>.
 * @since      JDK1.0
 */
export const darker = (colorStr,level=1) =>
    chroma.valid(colorStr) ? chroma(colorStr).darken(level).toString() : undefined;

/**
 * Make a simple color map array of no more than 10 entries.  Map goes from darker to brighter.
 * @param baseColor
 * @param mapSize
 * @param reverse
 * @return {*}
 */
function makeSimpleColorMap(baseColor, mapSize=10, reverse=false) {
    const c1= chroma(baseColor).brighten(2);
    const c2= chroma(baseColor).darken(1);
    const cAry= reverse ? [c2,c1] : [c1,c2];
   return chroma.scale(cAry).colors(mapSize);
}

const [R, G, B, A] = [0, 1, 2, 3];
const [LUMI, CONTRAST, HSP] = [0, 1, 2];



export function rateOpacity(color, ratio) {
    const rgba = getRGBA(color);
    if (!rgba) {
        return 'rgba(255, 255, 255,' + (1.0*ratio) + ')';
    }
    const newAlpha = Math.max(Math.min(rgba[3] * ratio, 1.0), 0.0);
    const newColor = rgba.slice(R, A);
    newColor.push(newAlpha);

    return toRGBAString(newColor);
}

export const maximizeOpacity= (color) => chroma.valid(color) ? chroma(color).alpha(1).toString() : 'white';

/**
 * @summary get contrast color in terms of black or white, based on various algorithms, relative luminance, contrast, or HSP color model.
 * @param {string} color
 * @param {Number} method
 * @returns {*}
 */
export function getBWBackground(color, method = LUMI) {
    let weight;
    let th;
    let luma;

    const rgba = getRGBA(color);
    if (!rgba) {
        return 'rgba(255, 255, 255, 1.0)';
    }

    const linearLuma = (w) => {
        return [R, G, B].reduce((lum, c) => {
            lum += w[c] * rgba[c];
            return lum;
        }, 0.0);
    };
    const squareLuma = (w) => {
        return [R, G, B].reduce((lum, c) => {
            lum += w[c] * rgba[c] * rgba[c];
            return lum;
        }, 0.0);
    };


    switch (method) {
        case CONTRAST:
        case HSP:
            weight = [0.299, 0.587, 0.114];
            th = 128;

            if (method === CONTRAST) {
                luma =  linearLuma(weight);
            } else {
                luma = Math.sqrt(squareLuma(weight));

            }
            break;
        case LUMI:
        default:
            weight = [0.2126, 0.7152, 0.0722];
            th = 165;

            luma = linearLuma(weight);
            break;
    }
    const bkGrey = luma < th ? parseInt('ff', 16) : 0;  // dark color -> get bright, bright color -> get dark

    return `rgba(${bkGrey}, ${bkGrey}, ${bkGrey}, 1.0)`;
}

/**
 * @summary get complementary color
 * @param {string} color
 * @returns {*}
 */
function getComplementaryColor(color) {
    const rgba = getRGBA(color);

    if (!rgba)  return 'rgba(201, 201, 201, 1.0)';   //'#e3e3e3'
    let rgb = rgba.slice(0, 3);

    const RGB2HSV = (rgb) =>
    {
        const hsv = {};
        const maxV = Math.max(...rgb);
        const minV = Math.min(...rgb);

        const dif = maxV - minV;

        hsv.saturation = (maxV === 0.0) ? 0 : (100.0 * dif / maxV);
        if (hsv.saturation === 0) {
            hsv.hue = 0;
        } else if (rgb[R] === maxV) {
            hsv.hue = 60.0 * (rgb[G] - rgb[B]) / dif;
        } else if (rgb[G] === maxV) {
            hsv.hue = 120.0 + 60.0 * (rgb[B] - rgb[R]) / dif;
        } else if (rgb[B] === maxV) {
            hsv.hue = 240.0 + 60.0 * (rgb[R] - rgb[G]) / dif;
        }
        if (hsv.hue < 0.0) hsv.hue += 360.0;
        hsv.value = Math.round(maxV * 100 / 255);
        hsv.hue = Math.round(hsv.hue);
        hsv.saturation = Math.round(hsv.saturation);
        return hsv;
    };

    const HSV2RGB = (hsv) => {
        let rgb = [0, 0, 0];
        const toRGB = (rgb, r, g, b) => {
            rgb[R] = r;
            rgb[G] = g;
            rgb[B] = b;
            return rgb.map((v) => (Math.round(v*255)));
        };

        if (hsv.saturation === 0) {
            let v = Math.round(hsv.value*2.55);

            if (hsv.hue === 180.0) {
                v = 255 - v;
            }
            rgb = toRGB(rgb, v, v, v);
        } else {
            hsv.hue /= 60.0;
            hsv.saturation /= 100.0;
            hsv.value /= 100.0;


            const i = Math.floor(hsv.hue);
            const f = hsv.hue - i;
            const a = hsv.value * (1.0 - hsv.saturation);
            const b = hsv.value * (1.0 - hsv.saturation * f);
            const c = hsv.value * (1.0 - hsv.saturation * (1-f));
            const d = hsv.value;
            switch(i) {
                case 0:
                    rgb = toRGB(rgb, d, c, a);
                    break;
                case 1:
                    rgb = toRGB(rgb, b, d, a);
                    break;
                case 2:
                    rgb = toRGB(rgb, a, d, c);
                    break;
                case 3:
                    rgb = toRGB(rgb, a, b, d);
                    break;
                case 4:
                    rgb = toRGB(rgb, c, a, d);
                    break;
                case 5:
                    rgb = toRGB(rgb, d, a, b);
                    break;
                default:
                    rgb = toRGB(rgb, 0, 0, 0);
                    break;
            }
        }
        return rgb;
    };

    const HueShift  = (h, s)  => {
        h += s;

        while (h >= 360.0) {
            h -= 360.0;
        }
        while (h < 0.0) {
            h+= 360.0;
        }
        return h;
    };

    const tmphsv = RGB2HSV(rgb);
    tmphsv.hue = HueShift(tmphsv.hue, 180.0);
    rgb = HSV2RGB(tmphsv);

    return `rgba(${rgb[R]}, ${rgb[G]}, ${rgb[B]}, ${rgba[3]})`;
}

export function toRGBAString(rgba) {
    const len = (!rgba || !isArray(rgba)) ? 0 : rgba.length;
    const val = len === 0 ? 255 : rgba[len-1];

    if (len <= A) {
        if (len === 0) rgba = [];
        for (let i = 0; i < A - len; i++ ) {
            rgba.push(val);
        }
        rgba.push(1.0);
    }

    return `rgba(${rgba[R]}, ${rgba[G]}, ${rgba[B]}, ${rgba[A]})`;
}

/*
 * @param {String} color - hex color, exactly seven characters log, starting with '#'
 * @param {Number} percentage (0.1 means 10 percent lighter, -0.1 - 10 percent darker)
 * @return {String} lighter or darker shade of the given hex color
 * from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-or-rgb-and-blend-colors
 */
function shadeColor(color, percent) {
    const [R, G, B] = toRGB(color.slice(1));
    const t=percent<0?0:255,p=percent<0?percent*-1:percent;
    return `#${(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1)}`;
}

