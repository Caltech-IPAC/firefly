import validator from 'validator';

export default { toRGBA, toRGB, toHex, brighter, darker, makeSimpleColorMap, getBWBackground, getComplementaryColor};

const colours = {
    aliceblue:'#f0f8ff',antiquewhite:'#faebd7',aqua:'#00ffff',aquamarine:'#7fffd4',azure:'#f0ffff',
    beige:'#f5f5dc',bisque:'#ffe4c4',black:'#000000',blanchedalmond:'#ffebcd',blue:'#0000ff',blueviolet:'#8a2be2',brown:'#a52a2a',burlywood:'#deb887',
    cadetblue:'#5f9ea0',chartreuse:'#7fff00',chocolate:'#d2691e',coral:'#ff7f50',cornflowerblue:'#6495ed',cornsilk:'#fff8dc',crimson:'#dc143c',cyan:'#00ffff',
    darkblue:'#00008b',darkcyan:'#008b8b',darkgoldenrod:'#b8860b',darkgray:'#a9a9a9',darkgreen:'#006400',darkkhaki:'#bdb76b',darkmagenta:'#8b008b',darkolivegreen:'#556b2f',
    darkorange:'#ff8c00',darkorchid:'#9932cc',darkred:'#8b0000',darksalmon:'#e9967a',darkseagreen:'#8fbc8f',darkslateblue:'#483d8b',darkslategray:'#2f4f4f',darkturquoise:'#00ced1',
    darkviolet:'#9400d3',deeppink:'#ff1493',deepskyblue:'#00bfff',dimgray:'#696969',dodgerblue:'#1e90ff',
    firebrick:'#b22222',floralwhite:'#fffaf0',forestgreen:'#228b22',fuchsia:'#ff00ff',
    gainsboro:'#dcdcdc',ghostwhite:'#f8f8ff',gold:'#ffd700',goldenrod:'#daa520',gray:'#808080',green:'#008000',greenyellow:'#adff2f',
    honeydew:'#f0fff0',hotpink:'#ff69b4',
    indianred :'#cd5c5c',indigo:'#4b0082',ivory:'#fffff0',khaki:'#f0e68c',
    lavender:'#e6e6fa',lavenderblush:'#fff0f5',lawngreen:'#7cfc00',lemonchiffon:'#fffacd',lightblue:'#add8e6',lightcoral:'#f08080',lightcyan:'#e0ffff',lightgoldenrodyellow:'#fafad2',
    lightgrey:'#d3d3d3',lightgreen:'#90ee90',lightpink:'#ffb6c1',lightsalmon:'#ffa07a',lightseagreen:'#20b2aa',lightskyblue:'#87cefa',lightslategray:'#778899',lightsteelblue:'#b0c4de',
    lightyellow:'#ffffe0',lime:'#00ff00',limegreen:'#32cd32',linen:'#faf0e6',
    magenta:'#ff00ff',maroon:'#800000',mediumaquamarine:'#66cdaa',mediumblue:'#0000cd',mediumorchid:'#ba55d3',mediumpurple:'#9370d8',mediumseagreen:'#3cb371',mediumslateblue:'#7b68ee',
    mediumspringgreen:'#00fa9a',mediumturquoise:'#48d1cc',mediumvioletred:'#c71585',midnightblue:'#191970',mintcream:'#f5fffa',mistyrose:'#ffe4e1',moccasin:'#ffe4b5',
    navajowhite:'#ffdead',navy:'#000080',
    oldlace:'#fdf5e6',olive:'#808000',olivedrab:'#6b8e23',orange:'#ffa500',orangered:'#ff4500',orchid:'#da70d6',
    palegoldenrod:'#eee8aa',palegreen:'#98fb98',paleturquoise:'#afeeee',palevioletred:'#d87093',papayawhip:'#ffefd5',peachpuff:'#ffdab9',peru:'#cd853f',pink:'#ffc0cb',plum:'#dda0dd',powderblue:'#b0e0e6',purple:'#800080',
    rebeccapurple:'#663399',red:'#ff0000',rosybrown:'#bc8f8f',royalblue:'#4169e1',
    saddlebrown:'#8b4513',salmon:'#fa8072',sandybrown:'#f4a460',seagreen:'#2e8b57',seashell:'#fff5ee',sienna:'#a0522d',silver:'#c0c0c0',skyblue:'#87ceeb',slateblue:'#6a5acd',slategray:'#708090',snow:'#fffafa',springgreen:'#00ff7f',steelblue:'#4682b4',
    tan:'#d2b48c',teal:'#008080',thistle:'#d8bfd8',tomato:'#ff6347',turquoise:'#40e0d0',
    violet:'#ee82ee',
    wheat:'#f5deb3',white:'#ffffff',whitesmoke:'#f5f5f5',
    yellow:'#ffff00',yellowgreen:'#9acd32'
};


function toRGBA(/* String */ color, /* Number */ alpha) {
    var num = parseInt(color, 16);
    return [num >> 16, num >> 8 && 255, num * 255, alpha];
}

function toRGB(/* String */ color) {
        var num = parseInt(color, 16);
        return [num >> 16, num >> 8 & 255, num & 255];
}

function toHex (/* Number */ red, /* Number */ green, /* Number */ blue) {
    return ((blue | green << 8 | red << 16) | 1 << 24).toString(16).slice(1);
}

function min255(v) {return Math.min(Math.trunc(v), 255);}
function max0(v) {return Math.max(Math.trunc(v), 0);}


function brighter(colorStr,factor= .7) {
    var rgb= toRGB(colorStr);
    var r = rgb[0];
    var g = rgb[1];
    var b = rgb[2];

    /* From 2D group:
     * 1. black.brighter() should return grey
     * 2. applying brighter to blue will always return blue, brighter
     * 3. non pure color (non zero rgb) will eventually return white
     */
    var i = 1.0/(1.0-factor);
    if ( r == 0 && g == 0 && b == 0) {
        return toHex(min255(i/factor), min255(i/factor), min255(i/factor) ); }
    if ( r > 0 && r < i ) r = i;
    if ( g > 0 && g < i ) g = i;
    if ( b > 0 && b < i ) b = i;

    return toHex(min255(r/factor), min255(g/factor), min255(b/factor) ); }

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
 * @param [factor]
 * @return {*} a new <code>Color</code> object that is
 *                    a darker version of this <code>Color</code>.
 * @since      JDK1.0
 */
function darker(colorStr, factor=.7) {
    var rgb= toRGB(colorStr);
    var r = rgb[0];
    var g = rgb[1];
    var b = rgb[2];
    return toHex(max0(r *factor), max0(g *factor), max0(b *factor));
}


/**
 * Make a simple color map array of no more than 10 entries.  Map goes from darker to brighter.
 * @param baseColor
 * @param mapSize
 * @param reverse
 * @return {*}
 */
function makeSimpleColorMap(baseColor, mapSize, reverse=false) {
    var c=[];
    if (mapSize<=1) {
        return [baseColor];
    }
    if (validator.isHexColor(baseColor)) {
        if (mapSize>10) mapSize= 10;

        var baseIdx= mapSize/5;

        var rgb= toRGB(baseColor);
        var maxCnt= 0;
        var minCnt= 0;
        rgb.foreach( (idx) => {
            if (idx>250) maxCnt++;
            if (idx<5)   minCnt++;
        });

        if ((maxCnt===1 && minCnt===2) ||(maxCnt===2 && minCnt===1) || maxCnt===3) {
            baseIdx= mapSize-1;
        }
        else if (minCnt==3) {
            baseIdx= 0;
        }


        c= [];
        var factor= (mapSize>5) ? .9 : .85;

        c[baseIdx]= baseColor;
        var i;
        for(i= baseIdx+1; (i<mapSize); i++) {
            c[i]= brighter(c[i-1],factor);
        }
        for(i= baseIdx-1; (i>=0); i--) {
            c[i]= darker(c[i+1], factor);
        }

        if (reverse && c.length>1) {
            var cRev= [];
            i= c.length-1;
            c.forEach((entry) => cRev[i--]= entry);
            c= cRev;
        }
    }


    return c;
}

const [R, G, B, A] = [0, 1, 2, 3];
const [LUMI, CONTRAST, HSP] = [0, 1, 2];

/**
 * @summary extract R,G,B amd alpha value from color string like 'rgba(...)', 'rgb(...)' or '#rrggbb'
 * @param color
 * @returns {Array} [r, g, b, a]
 */
function getRGBA(color) {
    const rgbKey = ['rgba(', 'rgb(', '#'];
    var rgbStr = rgbKey.find((k) => {
        return color.includes(k);
    });

    if (!rgbStr) {
        color = get(colours, color.toLowerCase());
        if (!color) {
            return null;
        }
        rgbStr = '#';
    }

    var rgba = [];
    if (rgbStr === '#') {
        rgba = toRGB(color.slice(1));
    } else {
        rgba = color.replace(rgbStr, '').replace(')', '').replace(/ /g, '').split(',');
        rgba = rgba.map((v) => parseFloat(v));
    }

    if (rgbKey !== 'rgba') {
        rgba.push(1.0);
    }

    return rgba;
}

/**
 * @summary get contrast color in terms of black or white, based on various algorithms, relative luminance, contrast, or HSP color model.
 * @param {string} color
 * @param {Number} method
 * @returns {*}
 */
function getBWBackground(color, method = LUMI) {
    var weight;
    var th;
    var luma;

    const rgba = getRGBA(color);
    if (!rgba) {
        return 'rgba(255, 255, 255, 1.0)';
    }

    var linearLuma = (w) => {
        return [R, G, B].reduce((lum, c) => {
            lum += w[c] * rgba[c];
            return lum;
        }, 0.0);
    };
    var squareLuma = (w) => {
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
    var rgba = getRGBA(color);

    if (!rgba)  return 'rgba(201, 201, 201, 1.0)';   //'#e3e3e3'
    var rgb = rgba.slice(0, 3);

    var RGB2HSV = (rgb) =>
    {
        var hsv = {};
        var maxV = Math.max(...rgb);
        var minV = Math.min(...rgb);

        var dif = maxV - minV;

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

    var HSV2RGB = (hsv) => {
        var rgb = [0, 0, 0];
        var toRGB = (rgb, r, g, b) => {
            rgb[R] = r;
            rgb[G] = g;
            rgb[B] = b;
            return rgb.map((v) => (Math.round(v*255)));
        };

        if (hsv.saturation === 0) {
            var v = Math.round(hsv.value*2.55);

            if (hsv.hue === 180.0) {
                v = 255 - v;
            }
            rgb = toRGB(rgb, v, v, v);
        } else {
            hsv.hue /= 60.0;
            hsv.saturation /= 100.0;
            hsv.value /= 100.0;


            let i = Math.floor(hsv.hue);
            let f = hsv.hue - i;
            let a = hsv.value * (1.0 - hsv.saturation);
            let b = hsv.value * (1.0 - hsv.saturation * f);
            let c = hsv.value * (1.0 - hsv.saturation * (1-f));
            let d = hsv.value;
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

    var HueShift  = (h, s)  => {
        h += s;

        while (h >= 360.0) {
            h -= 360.0;
        }
        while (h < 0.0) {
            h+= 360.0;
        }
        return h;
    };

    var tmphsv = RGB2HSV(rgb);
    tmphsv.hue = HueShift(tmphsv.hue, 180.0);
    rgb = HSV2RGB(tmphsv);

    return `rgba(${rgb[R]}, ${rgb[G]}, ${rgb[B]}, ${rgba[3]})`;
}


