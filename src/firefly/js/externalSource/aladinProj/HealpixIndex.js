

// This file contains a javascript implementation of HealpixIndex written by CDS.
// This code is from a minified version (healpix.min.js) in the Aladin distribution.
// I unminified it and made it an ES5 javascript module.

// The original healpix code is on: http://healpix.sourceforge.net/
// it was written in C the ported to other languages including java.
// The java Healpix code was expanded to include a class called healpix.core.HealpixIndex
// This is the javascript for of HealpixIndex.
// it has the following objects: HealpixIndex, SpatialVector, LongRangeSetBuilder, Utils
// in java they are all part of the healpix.code package.
// docs:
// https://healpix.jpl.nasa.gov/html/java/healpix/core/HealpixIndex.html
// https://healpix.jpl.nasa.gov/html/java/healpix/tools/SpatialVector.html
// https://healpix.jpl.nasa.gov/html/java/healpix/core/base/set/LongRangeSet.html




const Constants = {
    PI : Math.PI,
    C_PR : Math.PI / 180,
    VLEV : 2,
    EPS : 1e-7,
    c : .105,
    LN10 : Math.log(10),
    PIOVER2 : Math.PI / 2,
    TWOPI : 2 * Math.PI,
    TWOTHIRD : 2 / 3,
    ARCSECOND_RADIAN : 484813681109536e-20
};



export class SpatialVector {
    constructor(x, y, z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ra_ = 0;
        this.dec_ = 0;
        this.okRaDec_ = false;
    }
    setXYZ(t, s, i) {
        this.x = t;
        this.y = s;
        this.z = i;
        this.okRaDec_ = false;
    }

    length() {
        return Math.sqrt(this.lengthSquared());
    }

    lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    normalized() {
        const vectorLength = this.length();
        this.x /= vectorLength;
        this.y /= vectorLength;
        this.z /= vectorLength;
    }

    set(lon, lat) {
        this.ra_ = lon;
        this.dec_ = lat;
        this.okRaDec_ = true;
        this.updateXYZ();
    }

    angle(v1) {
        const xx = this.y * v1.z - this.z * v1.y;
        const yy = this.z * v1.x - this.x * v1.z;
        const zz = this.x * v1.y - this.y * v1.x;
        const cross = Math.sqrt(xx * xx + yy * yy + zz * zz);
        return Math.abs(Math.atan2(cross, this.dot(v1)));
    }

    get() { return [this.x, this.y, this.z]; }

    toString() { return 'SpatialVector[' + this.x + ', ' + this.y + ', ' + this.z + ']'; }

    cross(v) {
        return new SpatialVector(this.y * v.z - v.y * this.z, this.z * v.x - v.z * this.x, this.x * v.y - v.x() * this.y);
    }

    equal(other) {
        return Boolean(this.x===other.x && this.y===other.y && this.z===other.z);
    }

    mult(s) {
        return new SpatialVector(s * this.x, s * this.y, s * this.z);
    }

    dot(v1) {
        return this.x * v1.x + this.y * v1.y + this.z * v1.z;
    }

    add(s) {
        return new SpatialVector(this.x + s.x, this.y + s.y, this.z + s.z);
    }

    sub(s) {
        return new SpatialVector(this.x - s.x, this.y - s.y, this.z - s.z);
    }

    dec() {
        if (this.okRaDec_) return this.dec_;
        this.normalized();
        this.updateRaDec();
        return this.dec_;
    }

    ra() {
        if (this.okRaDec_) return this.ra_;
        this.normalized();
        this.updateRaDec();
        return this.ra_;
    }

    updateXYZ() {
        const t = Math.cos(this.dec_ * Constants.C_PR);
        this.x = Math.cos(this.ra_ * Constants.C_PR) * t;
        this.y = Math.sin(this.ra_ * Constants.C_PR) * t;
        this.z = Math.sin(this.dec_ * Constants.C_PR);
    }

    updateRaDec() {
        this.dec_ = Math.asin(this.z) / Constants.C_PR;
        const t = Math.cos(this.dec_ * Constants.C_PR);
        this.ra_ = (t > Constants.EPS || -Constants.EPS > t) ?
            this.y > Constants.EPS || this.y < -Constants.EPS ? 0 > this.y ?
                360 - Math.acos(this.x / t) / Constants.C_PR : Math.acos(this.x / t) / Constants.C_PR : 0 > this.x ?
                180 :
                0 :
            0;
        this.okRaDec_ = true;
    }
}

class LongRangeSetBuilder {

    constructor() { this.items = []; }

    appendRange(first, last) {
        for (let i = first; last >= i; i++) i in this.items || this.items.push(i);
    };
}


const powerOf2=  new Array(53).fill(0).map((val,idx) => Math.pow(2,idx));

const Utils = {
    radecToPolar(ra, dec) {
        return { theta: Math.PI / 2 - dec / 180 * Math.PI, phi: ra / 180 * Math.PI };
    },
    polarToRadec(t, s) {
        return { ra: 180 * s / Math.PI, dec: 180 * (Math.PI / 2 - t) / Math.PI };
    },
    castToInt(t) {
        return t > 0 ? Math.floor(t) : Math.ceil(t);
    },

    shiftRight(v,bits) {
        return Math.trunc(v / powerOf2[bits]);
    },
    shiftLeft(v,bits) {
        return Math.trunc(v * powerOf2[bits]);
    },


    bigAnd(v1, v2) {
        const hi = 0x80000000;
        const low = 0x7fffffff;
        const hi1 = ~~(v1 / hi);
        const hi2 = ~~(v2 / hi);
        const low1 = (v1 & low) >>> 0;
        const low2 = (v2 & low) >>> 0;
        const h = (hi1 & hi2) >>> 0;
        const l = (low1 & low2) >>> 0;
        return h*hi + l;
    },
    bigOr(v1, v2) {
        const hi = 0x80000000;
        const low = 0x7fffffff;
        const hi1 = ~~(v1 / hi);
        const hi2 = ~~(v2 / hi);
        const low1 = (v1 & low) >>> 0;
        const low2 = (v2 & low) >>> 0;
        const h = (hi1 | hi2) >>> 0;
        const l = (low1 | low2) >>> 0;
        return h*hi + l;
    },

    orAll(...args) {
        return args.reduce( (prev,curr) => {
            return Utils.bigOr(prev,curr);
        },0);
    }

};


// const FACEARRAY = [
//     [8, 9, 10, 11, -1, -1, -1, -1, 10, 11, 8, 9],
//     [5, 6, 7, 4, 8, 9, 10, 11, 9, 10, 11, 8],
//     [-1, -1, -1, -1, 5, 6, 7, 4, -1, -1, -1, -1],
//     [4, 5, 6, 7, 11, 8, 9, 10, 11, 8, 9, 10],
//     [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
//     [1, 2, 3, 0, 0, 1, 2, 3, 5, 6, 7, 4],
//     [-1, -1, -1, -1, 7, 4, 5, 6, -1, -1, -1, -1],
//     [3, 0, 1, 2, 3, 0, 1, 2, 4, 5, 6, 7],
//     [2, 3, 0, 1, -1, -1, -1, -1, 0, 1, 2, 3]
// ];
// const SWAPARRAY = [
//     [0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3],
//     [0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6],
//     [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
//     [0, 0, 0, 0, 0, 0, 0, 0, 5, 5, 5, 5],
//     [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
//     [5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0],
//     [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
//     [6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0],
//     [3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0]
// ];
//
// const XOFFSET = [-1, -1, 0, 1, 1, 1, 0, -1];
// const YOFFSET = [0, 1, 1, 1, 0, -1, -1, -1];
// const ORDER_MAX = 13;
// const NSIDELIST = [1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536,
//     131072,  262144, 524288]; //level 19

export const ORDER_MAX = 21;
const NSIDELIST =  new Array(ORDER_MAX).fill(0).map((val,idx) => Math.pow(2,idx));
const JPLL = [1, 3, 5, 7, 0, 2, 4, 6, 1, 3, 5, 7];
const JRLL = [2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4];
const NS_MAX = NSIDELIST[NSIDELIST.length-1];
const Z0 = Constants.TWOTHIRD;

const TAB_SIZE = 256;
const CTAB= new Array(TAB_SIZE).fill(0).map( (v,i) =>
               1 & i | (2 & i) << 7 | (4 & i) >>> 1 | (8 & i) << 6 | (16 & i) >>> 2 | (32 & i) << 5 | (64 & i) >>> 3 | (128 & i) << 4);
const UTAB= new Array(TAB_SIZE).fill(0).map( (v,i) =>
               1 & i | (2 & i) << 1 | (4 & i) << 2 | (8 & i) << 3 | (16 & i) << 4 | (32 & i) << 5 | (64 & i) << 6 | (128 & i) << 7);


export class HealpixIndex {

    constructor(nside) {
        this.nside = nside;
        this.nl2 = 2 * nside;
        this.nl3 = 3 * nside;
        this.nl4 = 4 * nside;
        this.npface = nside * nside;
        this.ncap = 2 * nside * (nside - 1);
        this.npix = 12 * this.npface;
        this.fact2 = 4 / this.npix;
        this.fact1 = (nside << 1) * this.fact2;
        this.order = HealpixIndex.nside2order(nside);
    }
    static calculateNSide(pixsize) {
        let i = 0;
        const n = pixsize * pixsize;
        const a = 180 / Constants.PI;
        const e = 3600 * 3600 * 4 * Constants.PI * a * a;
        const h = Utils.castToInt(e / n);
        const r = h / 12;
        const o = Math.sqrt(r);
        let c = NS_MAX;
        let u = 0;
        for (let  p = 0; NSIDELIST.length > p; p++) {
            if (c >= Math.abs(o - NSIDELIST[p])) {
                c = Math.abs(o - NSIDELIST[p]);
                i = NSIDELIST[p];
                u = p;
            }
            if (o > i && NS_MAX > o) {
                i = NSIDELIST[u + 1];
            }
            if (o > NS_MAX) {
                console.log('nside cannot be bigger than ' + NS_MAX);
                return NS_MAX;
            }
        }
        return i;
    }
    static nside2order(nside) {
        return (nside & nside - 1) > 0 ? -1 : Utils.castToInt(Math.log2(nside));
    }

    ang2pix_nest(theta, phi) {
        let  tp, o, c, jp, jm, ntt, face_num, ix, iy;
        phi >= Constants.TWOPI && (phi -= Constants.TWOPI);
        0 > phi && (phi += Constants.TWOPI);
        if (theta > Constants.PI || 0 > theta) {
            throw {
                name: 'Illegal argument',
                message: 'theta must be between 0 and ' + Constants.PI
            };
        }
        if (0 > phi) {
            throw {
                name: 'Illegal argument',
                message: 'phi must be between 0 and ' + Constants.TWOPI
            };
        }
        const z = Math.cos(theta);
        const za = Math.abs(z);
        const tt = phi / Constants.PIOVER2;
        if (Z0 >= za) { //Equatorial region
            const M = this.nside * (.5 + tt);
            const y = this.nside * .75 * z;
            const u = M - y;
            const p = M + y;
            o = u >>> this.order;
            c = p >>> this.order;
            face_num = o===c ?
                4===o ?
                    4 :
                    o + 4 :
                c > o ?
                    o :
                    c + 8;
            ix = Utils.castToInt(p & this.nside - 1);
            iy = Utils.castToInt(this.nside - (u & this.nside - 1) - 1);
        } else { // polar region, za > 2/3
            ntt = Utils.castToInt(tt);
            if (ntt >= 4) ntt = 3;
            tp = tt - ntt;
            const tmp = this.nside * Math.sqrt(3 * (1 - za));
            //   (the index of edge lines increase when distance from the closest pole goes up)
            jp = Utils.castToInt(tp * tmp);
            jm = Utils.castToInt((1 - tp) * tmp);
            jp = Math.min(NS_MAX - 1, jp);
            jm = Math.min(NS_MAX - 1, jm);
            // finds the face and pixel's (x,y)
            if (z>=0) {
                face_num = ntt; // in {0,3}
                ix = Utils.castToInt(this.nside - jm - 1);
                iy = Utils.castToInt(this.nside - jp - 1);
            }
            else {
                face_num = ntt + 8; // in {8,11}
                ix = jp;
                iy = jm;
            }
        }
        return this.xyf2nest(ix, iy, face_num);
    }

    xyf2nestORGIN_A(ix, iy, face_num) {
        return (face_num << 2 * this.order) + (UTAB[255 & ix] | UTAB[255 & ix >> 8] << 16 | UTAB[255 & ix >> 16] << 32 | UTAB[255 & ix >> 24] << 48 | UTAB[255 & iy] << 1 | UTAB[255 & iy >> 8] << 17 | UTAB[255 & iy >> 16] << 33 | UTAB[255 & iy >> 24] << 49);
    }


    xyf2nestORGIN_B(ix, iy, face_num) {
        return (face_num << 2 * this.order) +
            (UTAB[255 & ix] |
             UTAB[255 & ix >>> 8] << 16 |
             UTAB[255 & ix >>> 16] << 32 |
             UTAB[255 & ix >>> 24] << 48 |
             UTAB[255 & iy] << 1 |
             UTAB[255 & iy >>> 8] << 17 |
             UTAB[255 & iy >>> 16] << 33 |
             UTAB[255 & iy >>> 24] << 49);
    }
    xyf2nest(ix, iy, face_num) {
        // if (ix>0x7FFFFFF || iy>0x7FFFFFF) {
        //     console.log('xyf2nest: ix or iy greater');
        // }
        // const nest= (face_num << 2 * this.order) +
        //     (this.utab[255 & ix] |
        //         this.utab[255 & ix >>> 8] * Math.pow(2,16) |
        //         this.utab[255 & ix >>> 16] * Math.pow(2,32)|
        //         this.utab[255 & ix >>> 24] * Math.pow(2,48)|
        //         this.utab[255 & iy] << 1 |
        //         this.utab[255 & iy >>> 8] * Math.pow(2,17) |
        //         this.utab[255 & iy >>> 16] * Math.pow(2,33) |
        //         this.utab[255 & iy >>> 24] * Math.pow(2,49));
        const nest= Utils.shiftLeft(face_num, 2 * this.order) +
            Utils.orAll(UTAB[255 & ix] ,
                UTAB[255 & ix >>> 8] * Math.pow(2,16) ,
                UTAB[255 & ix >>> 16] * Math.pow(2,32),
                UTAB[255 & ix >>> 24] * Math.pow(2,48),
                UTAB[255 & iy] << 1 ,
                UTAB[255 & iy >>> 8] * Math.pow(2,17) ,
                UTAB[255 & iy >>> 16] * Math.pow(2,33) ,
                UTAB[255 & iy >>> 24] * Math.pow(2,49));
        // if (nest>0x7FFFFFF) {
        //     console.log('xyf2nest: nest greater');
        // }
        return nest;
    }

    nest2xyf(ipix) {
        // if (ipix>0x7FFFFFF) {
        //     console.log('nest2xyf: ipix greater');
        // }
        const s = {};
        // s.face_num = ipix >> 2 * this.order;
        s.face_num = Utils.shiftRight(ipix,2 * this.order);
        // let i = ipix & this.npface - 1;
        let i = Utils.bigAnd(ipix, this.npface - 1);
        // let n = (93823560581120 & i) >>> 16 | (614882086624428e4 & i) >>> 31 | 21845 & i | (1431633920 & i) >>> 15;
        let n = Utils.orAll(Utils.shiftRight(Utils.bigAnd(93823560581120,i),16),
                            Utils.shiftRight(Utils.bigAnd(614882086624428e4, i),31),
                            Utils.bigAnd(21845, i),
                            Utils.shiftRight(Utils.bigAnd(1431633920,i),15));

        // s.ix = this.ctab[255 & n] | this.ctab[255 & n >>> 8] << 4 | this.ctab[255 & n >>> 16] << 16 | this.ctab[255 & n >>> 24] << 20;

        // if (n>0x7FFFFFF) {
        //     console.log('nest2xyf: n greater');
        // }

        s.ix = Utils.orAll(CTAB[Utils.bigAnd(255, n)],
                           Utils.shiftLeft(CTAB[255 & Utils.shiftRight(n,8)], 4),
                           Utils.shiftLeft(CTAB[255 & Utils.shiftRight(n,16)], 16),
                           Utils.shiftLeft(CTAB[255 & Utils.shiftRight(n,24)], 20));

        //i >>= 1;
        i= Utils.shiftRight(i,1);
        // n = (93823560581120 & i) >>> 16 | (614882086624428e4 & i) >>> 31 | 21845 & i | (1431633920 & i) >>> 15;
        n = Utils.orAll(Utils.shiftRight(Utils.bigAnd(93823560581120,i), 16),
                        Utils.shiftRight(Utils.bigAnd(614882086624428e4, i), 31),
                        Utils.bigAnd(21845, i),
                        Utils.shiftRight(Utils.bigAnd(1431633920,i),15));



        // s.iy = this.ctab[255 & n] | this.ctab[255 & n >>> 8] << 4 | this.ctab[255 & n >>> 16] << 16 | this.ctab[255 & n >>> 24] << 20;
        s.iy = Utils.orAll(CTAB[Utils.bigAnd(255, n)],
                           Utils.shiftLeft(CTAB[255 & Utils.shiftRight(n, 8)], 4),
                           Utils.shiftLeft(CTAB[255 & Utils.shiftRight(n, 16)], 16),
                           Utils.shiftLeft(CTAB[255 & Utils.shiftRight(n,24)], 20));
        return  s;
    }
    pix2ang_nest(ipix) {
        if (0 > ipix || ipix > this.npix - 1) {
            throw {
                name: 'Illegal argument',
                message: 'ipix out of range'
            };
        }
        let nr, z, kshift;
        const e = this.nest2xyf(ipix);
        const h = e.ix;
        const r = e.iy;
        const o = e.face_num;
        const jr = (JRLL[o] << this.order) - h - r - 1;
        if (this.nside > jr) {
            nr = jr;
            z = 1 - nr * nr * this.fact2;
            kshift = 0;
        }
        else if (jr > this.nl3 ) {
            nr = this.nl4 - jr;
            z = nr * nr * this.fact2 - 1;
            kshift = 0;
        }
        else {
            nr = this.nside;
            z = (this.nl2 - jr) * this.fact1;
            kshift = 1 & jr - this.nside;

        }
        const theta = Math.acos(z);
        let jp = (JPLL[o] * nr + h - r + 1 + kshift) / 2;
        jp > this.nl4 && (jp -= this.nl4);
        1 > jp && (jp += this.nl4);
        const phi = (jp - .5 * (kshift + 1)) * (Constants.PIOVER2 / nr);
        return { theta, phi };
    }
    static nside2Npix(nside) {
        if (0 > nside || (nside & -nside) !==nside || nside > NS_MAX) {
            throw {
                name: 'Illegal argument',
                message: 'nside should be >0, power of 2, <' + NS_MAX
            };
        }
        const i = 12 * nside * nside;
        return i;
    }
    xyf2ring(ix, iy, face_num) {
        let nr, kshift, startpix;
        const r = JRLL[face_num] * this.nside - ix - iy - 1;

        if (this.nside > r) {
            nr = r;
            startpix = 2 * nr * (nr - 1);
            kshift = 0;
        }
        else if (r > 3 * this.nside) {
            nr = this.nl4 - r;
            startpix = this.npix - 2 * (nr + 1) * nr;
            kshift = 0;
        }
        else {
            nr = this.nside;
            startpix = this.ncap + (r - this.nside) * this.nl4;
            kshift = 1 & r - this.nside;
        }


        let jp = (JPLL[face_num] * nr + ix - iy + 1 + kshift) / 2;
        if (jp > this.nl4) {
            jp -= this.nl4;
        } else if (1 > jp){
            jp += this.nl4;
        }
        return startpix + jp - 1;
    }
    nest2ring(ipnest) {
        const s = this.nest2xyf(ipnest);
        const i = this.xyf2ring(s.ix, s.iy, s.face_num);
        return i;
    }
    corners_nest(ipix, step) {
        const i = this.nest2ring(ipix);
        return this.corners_ring(i, step);
    }
    pix2ang_ring(ipix) {
        let theta, phi, iring, iphi, ip,  fodd, hip, fihip;
        if (0 > ipix || ipix > this.npix - 1) {
            throw {
                name: 'Illegal argument',
                message: 'ipix out of range'
            };
        }
        const ipix1 = ipix + 1; // in {1, npix}



        if (this.ncap >= ipix1) {

            hip = ipix1 / 2;
            fihip = Utils.castToInt(hip);
            iring = Utils.castToInt(Math.sqrt(hip - Math.sqrt(fihip))) + 1;
            iphi = ipix1 - 2 * iring * (iring - 1);
            theta = Math.acos(1 - iring * iring * this.fact2);
            phi = (iphi - .5) * Constants.PI / (2 * iring);
        }
        else {
            if (this.npix - this.ncap > ipix ) {
                ip = ipix - this.ncap;
                iring = ip / this.nl4 + this.nside;
                iphi = ip % this.nl4 + 1;
                fodd = Utils.bigAnd(1, iring + this.nside) > 0 ? 1 : .5;
                theta = Math.acos((this.nl2 - iring) * this.fact1);
                phi = (iphi - fodd) * Constants.PI / this.nl2;
            }
            else {
                ip = this.npix - ipix;
                iring = Utils.castToInt(.5 * (1 + Math.sqrt(2 * ip - 1)));
                iphi = 4 * iring + 1 - (ip - 2 * iring * (iring - 1));
                theta = Math.acos(-1 + Math.pow(iring, 2) * this.fact2);
                phi = (iphi - .5) * Constants.PI / (2 * iring);
            }
        }
        return [theta, phi];
    }
    ring(ipix) {
        let s, i, n = 0;
        const a = ipix + 1;
        let   e = 0;

        if (this.ncap >= a ) {
            i = a / 2;
            e = Utils.castToInt(i);
            n = Utils.castToInt(Math.sqrt(i - Math.sqrt(e))) + 1;
        }
        else if (this.nl2 * (5 * this.nside + 1) >= a ) {
            s = Utils.castToInt(a - this.ncap - 1);
            n = Utils.castToInt(s / this.nl4 + this.nside);
        }
        else {
            s = this.npix - a + 1;
            i = s / 2;
            e = Utils.castToInt(i);
            n = Utils.castToInt(Math.sqrt(i - Math.sqrt(e))) + 1;
            n = this.nl4 - n;
        }
        return n;
    }
    integration_limits_in_costh(i_th) {
        let s, i, n;
        const a = 1 * this.nside;
        
        if (this.nside >= i_th) {
            i = 1 - Math.pow(i_th, 2) / 3 / this.npface;
            n = 1 - Math.pow(i_th - 1, 2) / 3 / this.npface;
            s = (i_th===this.nside) ? 2 * (this.nside - 1) / 3 / a : 1 - Math.pow(i_th + 1, 2) / 3 / this.npface;

        }
        else if (this.nl3 > i_th) {
            i = 2 * (2 * this.nside - i_th) / 3 / a;
            n = 2 * (2 * this.nside - i_th + 1) / 3 / a;
            s = 2 * (2 * this.nside - i_th - 1) / 3 / a;
        }
        else  {
            n = i_th===this.nl3 ?
                2 * (-this.nside + 1) / 3 / a :
                -1 + Math.pow(4 * this.nside - i_th + 1, 2) / 3 / this.npface;
            s = -1 + Math.pow(this.nl4 - i_th - 1, 2) / 3 / this.npface;
            i = -1 + Math.pow(this.nl4 - i_th, 2) / 3 / this.npface;
        }
        return [n, i, s];
    }
    pixel_boundaries(i_th, i_phi, i_zone, cos_theta) {
        let sq3th, factor, jd, ju, ku, kd, phi_l, phi_r;
        const r_n_nside = 1 * this.nside;
        if (Math.abs(cos_theta) >= 1 - 1 / 3 / this.npface) {
            phi_l = i_zone * Constants.PIOVER2;
            phi_r = (i_zone + 1) * Constants.PIOVER2;
            return [phi_l, phi_r];
        }
        if (1.5 * cos_theta >= 1) {
            sq3th = Math.sqrt(3 * (1 - cos_theta));
            factor = 1 / r_n_nside / sq3th;
            jd = i_phi;
            ju = jd - 1;
            ku = i_th - i_phi;
            kd = ku + 1;
            phi_l = Constants.PIOVER2 * (Math.max(ju * factor, 1 - kd * factor) + i_zone);
            phi_r = Constants.PIOVER2 * (Math.min(1 - ku * factor, jd * factor) + i_zone);
        }
        else if (1.5 * cos_theta > -1) {
            const cth34 = .5 * (1 - 1.5 * cos_theta);
            const cth34_1 = cth34 + 1;
            const modfactor = this.nside + i_th % 2;
            jd = i_phi - (modfactor - i_th) / 2;
            ju = jd - 1;
            ku = (modfactor + i_th) / 2 - i_phi;
            kd = ku + 1;
            phi_l = Constants.PIOVER2 * (Math.max(cth34_1 - kd / r_n_nside, -cth34 + ju / r_n_nside) + i_zone);
            phi_r = Constants.PIOVER2 * (Math.min(cth34_1 - ku / r_n_nside, -cth34 + jd / r_n_nside) + i_zone);
        } else {
            sq3th = Math.sqrt(3 * (1 + cos_theta));
            factor = 1 / r_n_nside / sq3th;
            const M = 2 * this.nside;
            jd = i_th - M + i_phi;
            ju = jd - 1;
            ku = M - i_phi;
            kd = ku + 1;
            phi_l = Constants.PIOVER2 * (Math.max(1 - (M - ju) * factor, (M - kd) * factor) + i_zone);
            phi_r = Constants.PIOVER2 * (Math.min(1 - (M - jd) * factor, (M - ku) * factor) + i_zone);
        }
        return [phi_l, phi_r];
    }
    static vector(theta, phi) {
        const x = Math.sin(theta) * Math.cos(phi);
        const y = Math.sin(theta) * Math.sin(phi);
        const z = Math.cos(theta);
        return new SpatialVector(x, y, z);
    }

    corners_ring(pix, step) {
        const n = 2 * step + 2;
        const res = Array(n);
        const e = this.pix2ang_ring(pix);
        let h = Math.cos(e[0]);
        let r = e[0];
        const o = e[1];
        const c = Utils.castToInt(o / Constants.PIOVER2);
        const u = this.ring(pix);
        const p = Math.min(u, Math.min(this.nside, this.nl4 - u));
        const d = Constants.PIOVER2 / p;
        let l = (u >= this.nside && this.nl3 >= u) ?
            Utils.castToInt(o / d + u % 2 / 2) + 1 :
            Utils.castToInt(o / d) + 1;
        l -= c * p;
        const f = n / 2;
        const I = this.integration_limits_in_costh(u);
        const M = Math.acos(I[0]);
        const y = Math.acos(I[2]);
        let g = this.pixel_boundaries(u, l, c, I[0]);
        res[0] = l > p / 2 ? HealpixIndex.vector(M, g[1]) : HealpixIndex.vector(M, g[0]);
        g = this.pixel_boundaries(u, l, c, I[2]);
        res[f] = l > p / 2 ? HealpixIndex.vector(y, g[1]) : HealpixIndex.vector(y, g[0]);
        if (1===step) {
            const P = Math.acos(I[1]);
            g = this.pixel_boundaries(u, l, c, I[1]);
            res[1] = HealpixIndex.vector(P, g[0]);
            res[3] = HealpixIndex.vector(P, g[1]);
        }
        else {
            const x = I[2] - I[0];
            const C = x / (step + 1);
            for (let v = 1; step >= v; v++) {
                h = I[0] + C * v;
                r = Math.acos(h);
                g = this.pixel_boundaries(u, l, c, h);
                res[v] = HealpixIndex.vector(r, g[0]);
                res[n - v] = HealpixIndex.vector(r, g[1]);
            }
        }
        return res;
    }
    static vec2Ang(spatialVector) {
        const s = spatialVector.z / spatialVector.length();
        const i = Math.acos(s);
        let n = 0;
        if (0 !==spatialVector.x || 0 !==spatialVector.y) {
            n = Math.atan2(spatialVector.y, spatialVector.x);
        }
        if (0 > n) {
            n += 2 * Math.PI;
        }
        return [i, n];
    }

    queryDisc(spatialVector, radius, nest, inclusive) {
        if (0 > radius || radius > Constants.PI) {
            throw {
                name: 'Illegal argument',
                message: 'angular radius is in RADIAN and should be in [0,pi]'
            };
        }
        let r, d, f, y, v;
        const R = new LongRangeSetBuilder;
        let c = radius;
        inclusive && (c += Constants.PI / this.nl4);
        const T = HealpixIndex.vec2Ang(spatialVector);
        const u = T[0];
        const p = T[1];
        const I = this.fact2;
        const M = this.fact1;
        const o = Math.cos(u);
        const _ = 1 / Math.sqrt((1 - o) * (1 + o));
        const g = u - c;
        const P = u + c;
        const l = Math.cos(c);
        const C = Math.cos(g);
        const e = this.ringAbove(C) + 1;
        const x = Math.cos(P);
        let h = this.ringAbove(x);
        if (e > h && 0===h ) h= e;
        if (0 >= g) {
            for (let m = 1; e > m; ++m) {
                this.inRing(m, 0, Math.PI, R);
            }
        }
        for (r = e; h >= r; ++r) {
            v = this.nside > r ?
                1 - r * r * I :
                this.nl3 >= r ?
                    (this.nl2 - r) * M :
                    -1 + (this.nl4 - r) * (this.nl4 - r) * I;
            d = (l - v * o) * _;
            f = 1 - v * v - d * d;
            y = Math.atan2(Math.sqrt(f), d);
            if (isNaN(y)) y = c;
            this.inRing(r, p, y, R);
        }
        if (P >= Math.PI) {
            for (let m = h + 1; this.nl4 > m; ++m) this.inRing(m, 0, Math.PI, R, false);
        }
        let b;
        if (nest) {
            const S = R.items;
            const U = [];
            for (let A = 0; S.length > A; A++) {
                const O = this.ring2nest(S[A]);
                U.indexOf(O) >= 0 || U.push(O);
            }
            b = U;
            // console.log(b);
        }
        else {
            b = R.items;
        }
        return b;
    }

    inRing(iz, phi0, dphi, nest, conservative) {
        let e, h, r, o, c = false, d = 0, f = 0, I = 0, u;
        const p = 1e-12;
        const M = (phi0 - dphi) % Constants.TWOPI - p;
        const y = phi0 + dphi + p;
        const g = (phi0 + dphi) % Constants.TWOPI + p;

        if (p > Math.abs(dphi - Constants.PI)) c = true;

        if (iz >= this.nside && this.nl3 >= iz) {
            d = iz - this.nside + 1;
            r = this.ncap + this.nl4 * (d - 1);
            o = r + this.nl4 - 1;
            e = d % 2;
            h = this.nl4;
        }
        else {
            if (this.nside > iz) {
                d = iz;
                r = 2 * d * (d - 1);
                o = r + 4 * d - 1;
            }
            else {
                d = 4 * this.nside - iz;
                r = this.npix - 2 * d * (d + 1);
                o = r + 4 * d - 1;
            }
            h = 4 * d;
            e = 1;
        }

        if (c) {
            nest.appendRange(r, o);
            return;
        }
        const l= e/2;
        if (conservative) {
            f = Math.round(h * M / Constants.TWOPI - l);
            I = Math.round(h * y / Constants.TWOPI - l); f %= h;
            I > h && (I %= h);
        }
        else {
            f = Math.ceil(h * M / Constants.TWOPI - l);
            I = Utils.castToInt(h * g / Constants.TWOPI - l);
            f > I && 1===iz && (I = Utils.castToInt(h * y / Constants.TWOPI - l));
            f===(I + 1) && (f = I);
            if (1===(f - I) && Constants.PI > dphi * h) {
                console.log('the interval is too small and away from center');
                return undefined;
            }
            f = Math.min(f, h - 1);
            I = Math.max(I, 0);
        }
        f > I && (u = true);
        if (u) {
            f += r;
            I += r;
            nest.appendRange(r, I);
            nest.appendRange(f, o);
        }
        else {
            if (0 > f) {
                f = Math.abs(f);
                nest.appendRange(r, r + I);
                nest.appendRange(o - f + 1, o);
                return;
            }
            f += r;
            I += r;
            nest.appendRange(f, I);
        }
    }

    ringAbove(z) {
        const az = Math.abs(z);
        if (az > Constants.TWOTHIRD) {
            const iring = Utils.castToInt(this.nside * Math.sqrt(3 * (1 - az)));
            return z > 0 ? iring : 4 * this.nside - iring - 1;
        }
        return Utils.castToInt(this.nside * (2 - 1.5 * z));
    }

    ring2nest(ipRing) {
        const xyf = this.ring2xyf(ipRing);
        const nest=  this.xyf2nest(xyf.ix, xyf.iy, xyf.face_num);
        return nest;
    }

    ring2xyf(pix) {
        let iring, iphi, kshift, nr;
        const ret = {}; // Xyf
        if (this.ncap > pix) { // North Polar cap
            iring = Utils.castToInt(.5 * (1 + Math.sqrt(1 + 2 * pix)));
            iphi = pix + 1 - 2 * iring * (iring - 1);
            kshift = 0;
            nr = iring;
             ret.face_num = 0;
            let r = iphi - 1;
            if (r >= 2 * iring) {
                 ret.face_num = 2;
                r -= 2 * iring;
            }
            r >= iring && ++ ret.face_num;
        } else if (this.npix - this.ncap > pix) {  // Equatorial region
            const ip = pix - this.ncap;
            if (this.order >= 0)  {
                iring = Utils.shiftRight(ip,this.order + 2) + this.nside;
                // iphi = (ip & this.nl4 - 1) + 1;
                iphi = Utils.bigAnd(ip, this.nl4 - 1) + 1;
            } else {
                iring = ip / this.nl4 + this.nside;
                iphi = ip % this.nl4 + 1;
            }
            kshift = Utils.bigAnd(1 , iring + this.nside);
            nr = this.nside;
            let c, u;
            const ire = iring - this.nside + 1;
            const irm = this.nl2 + 2 - ire;
            if (this.order>=0) {
                c = Utils.shiftRight(iphi - Utils.castToInt(ire / 2) + this.nside - 1, this.order);
                u = Utils.shiftRight(iphi - Utils.castToInt(irm / 2) + this.nside - 1,this.order);
            }
            else {
                c = (iphi - Utils.castToInt(ire / 2) + this.nside - 1) / this.nside;
                u = (iphi - Utils.castToInt(irm / 2) + this.nside - 1) / this.nside;
            }
            if (u===c) {
                 ret.face_num= 4===u ? 4 : Utils.castToInt(u) + 4;
            }
            else  {
                 ret.face_num=c > u ? Utils.castToInt(u) : Utils.castToInt(c) + 8;
            }
        } else {  // South Polar cap
            const ip = this.npix - pix;
            iring = Utils.castToInt(.5 * (1 + Math.sqrt(2 * ip - 1)));
            iphi = 4 * iring + 1 - (ip - 2 * iring * (iring - 1));
            kshift = 0;
            nr = iring;
            iring = 2 * this.nl2 - iring;
             ret.face_num = 8;
            let r = iphi - 1;
            if (r >= 2 * nr) {
                 ret.face_num = 10;
                r -= 2 * nr;
            }
            r >= nr && ++ ret.face_num;
        }
        const d = iring - JRLL[ ret.face_num] * this.nside + 1;
        let f = 2 * iphi - JPLL[ ret.face_num] * nr - kshift - 1;
        f >= this.nl2 && (f -= 8 * this.nside);
        // ret.ix = f - d >>> 1;
        //  ret.iy = -(f + d) >>> 1;
         ret.ix = Utils.shiftRight(f - d, 1);
         ret.iy = Utils.shiftRight(-(f + d),1);
        return  ret;
    }
}

