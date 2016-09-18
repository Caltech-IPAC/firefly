import validator from 'validator';

export default { toRGBA, toRGB, toHex, brighter, darker, makeSimpleColorMap};


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
