/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

export const Jgtfkc= {iway: 1, japply: 1};


function make2dArray(x,y) {
    const a= new Array(x);
    for(var i=0; i<x; i++) a[i]= new Array(y);
    return a;
}



const idad= [], idpmad= [];
const idd= [], idpmdd= [];
const idaa= make2dArray(19,25), idpmaa= make2dArray(19,25);
const idda= make2dArray(19,25), idpmda= make2dArray(19,25);
const idamm= make2dArray(5,7),  idamam= make2dArray(5,7);

const dad= [], dpmad= [];
const dd= [], dpmdd= [];
const daa= make2dArray(19,25), dpmaa= make2dArray(19,25);
const dda= make2dArray(19,25), dpmda= make2dArray(19,25);
const dam= make2dArray(5,7),   dpmam= make2dArray(5,7);

var nthru = 0;
var dtor;


// var p= [ new Array(3), new Array(3), new Array(3) ];



const decs=[85.0,80.0,70.0,60.0,50.0,40.0,       //[19]
            30.0,20.0,10.0,  0.0,-10.0,-20.0,
            -30.0,-40.0,-50.0,-60.0,-70.0,-80.0,
            -85.0];

const rads=[0.0, 15.0, 30.0, 45.0, 60.0, 75.0,  //[25]
            90.0,105.0,120.0,135.0,150.0,165.0,
            180.0,195.0,210.0,225.0,240.0,255.0,
            270.0,285.0,300.0,315.0,330.0,345.0,
            360.0];

/** 18sep92 just to remove redundant type spec. for dintrp in func. dintrp and
 *     for dintr2 in func. dintr2 so it can be compile on apc3 also.
 * 27jul92 to add   common/fk5way/
 * updated 23jul92 to clean up.
 *  j.bennett  jun92
 * gets systematic fk5-fk4 corrections for b1950 to j2000 conversions
 *  @param ain is b1950 ra in degrees;
 *  @param d is b1950 dec in degrees;
 *  @param dmag is photometric magnitude (used only if between 1.0 and 7.0
 *  @param epoch is normally 1950.0 and provides for an adjustment if it is not
 *     1950.  At 1950, there is no additional adjustment.
 *  @param corra,corrd are the corrections to ra and dec, in degrees, such that they
 *      may be added to a and d to obtain the corrected position.
 *   @param corrd
 *  @param corrpa is correction for proper motion in ra in seconds of time per century.
 *  @param corrpd is correction for proper motion in dec in seconds of arc per century.
 *
 */
export function jgtfkc(ain, d, dmag, epoch,  corra, corrd, corrpa, corrpd) {

    var loc,loc1,loc2, locx1,locx2;
    var n1,n3;
    var dec1,dec2,dtest, fkpdec= 89.999;
    var xmag1, xmag2, a;
    var delepk, delras, deldas, delpma, delpmd,dcosd;
    var cdadec,cdpmad,cdd,cdpmdd,cdaa,cdpmaa,cdda,cdpmda,cdam,cdpmam;

    if(nthru == 0) {
        dtor = Math.atan(1.0) / 45.0;
        jnitfk();
        nthru = 1;
    }

    corra  = 0.0;
    corrd  = 0.0;
    corrpa = 0.0;
    corrpd = 0.0;

    if(Math.abs(d) > fkpdec) return {corra,corrd,corrpa,corrpd};

    a = ain;
    if(a < 0.0)         a = a + 360.0;
    else if (a > 360.0) a = a - 360.0;

    loc1 = Math.floor(91.0 - d);
    if(loc1 > 180) loc1 = 180;
    if(loc1 < 1)   loc1 = 1;
    loc2 = loc1 + 1;

    dec1 = 91 - loc1;
    dec2 = 91 - loc2;

    cdadec = dintrp(  dad[loc1-1],  dad[loc2-1],dec1,dec2,d);
    cdpmad = dintrp(dpmad[loc1-1],dpmad[loc2-1],dec1,dec2,d);
    cdd    = dintrp(   dd[loc1-1],   dd[loc2-1],dec1,dec2,d);
    cdpmdd = dintrp(dpmdd[loc1-1],dpmdd[loc2-1],dec1,dec2,d);

    for (n1=1; n1<19; n1++) {
        if(d >= decs[n1]) break;
    }
    if (n1 > 18) n1 = 18;
    loc2 = n1;
    loc1 = loc2 - 1;
    for (n3=1; n3<25; n3++) {
        if(a <= rads[n3]) break;
    }
    if (n3 > 24) n3 = 24;

    locx2 = n3;
    locx1 = locx2 - 1;

    cdaa= dintr2(daa[loc1][locx1],  daa[loc1][locx2],
                 daa[loc2][locx1],  daa[loc2][locx2],
                 rads[locx1],rads[locx2],decs[loc1],decs[loc2],a,d);

    cdpmaa= dintr2(dpmaa[loc1][locx1],dpmaa[loc1][locx2],
                   dpmaa[loc2][locx1],dpmaa[loc2][locx2],
                   rads[locx1],rads[locx2],decs[loc1],decs[loc2],a,d);

    cdda= dintr2(dda[loc1][locx1],  dda[loc1][locx2],
                 dda[loc2][locx1],  dda[loc2][locx2],
                 rads[locx1],rads[locx2],decs[loc1],decs[loc2],a,d);

    cdpmda = dintr2(dpmda[loc1][locx1],dpmda[loc1][locx2],
                    dpmda[loc2][locx1],dpmda[loc2][locx2],
                    rads[locx1],rads[locx2],decs[loc1],decs[loc2],a,d);

    cdam   = 0.0;
    cdpmam = 0.0;
    if(dmag >= 1.0 && dmag <= 7.0) {
        if( d >= 60.0) loc = 1;
        else if ( d >= 0.0) loc = 2;
        else if ( d >= -30.0) loc = 3;
        else if ( d >= -60.0) loc = 4;
        else loc = 5;

        loc1 = Math.trunc(dmag);
        loc2 = loc1 + 1;
        if(loc2 > 7) loc2 = 7;
        xmag1 = loc1;
        xmag2 = loc2;

        cdam   = dintrp(dam[loc-1][loc1-1],  dam[loc-1][loc2-1], xmag1, xmag2, dmag);
        cdpmam = dintrp(dpmam[loc-1][loc1-1],dpmam[loc-1][loc2-1], xmag1, xmag2, dmag);
    }

    dcosd = Math.cos(d * dtor);
    delras = (cdadec  + cdaa   + cdam)   / dcosd;
    delpma = (cdpmad + cdpmaa + cdpmam) / dcosd;
    deldas =  cdd    + cdda;
    delpmd =  cdpmdd + cdpmda;
    if(epoch > 0.0 && epoch != 1950.0) {
        delepk = (epoch - 1950.0) * 0.01;
        delras = delras + delpma*delepk;
        deldas = deldas + delpmd*delepk;
    }

    dtest = deldas / 3600.0;
    if(Jgtfkc.iway >= 0) {
         if(Math.abs(d+dtest) > fkpdec) return {corra,corrd,corrpa,corrpd};
    }
    else {
         if(Math.abs(d-dtest) > fkpdec) return {corra,corrd,corrpa,corrpd};
    }

    corrd = dtest;
    corra = (delras * 15.0) / 3600.0;
    corrpa = delpma;
    corrpd = delpmd;

    return {corra,corrd,corrpa,corrpd};
}


/**
 * find, by iteration, fk5-fk4 systematic corrections to subtract from
 *   ra and dec when returning position back to B1950/FK4 system.
 *
 */
export function junfkc(ra,dec,xmag,tobs, corra,corrd,corrpa, corrpd) {
//double ra,dec,xmag,tobs,*corra,*corrd,*corrpa,*corrpd;

    var lway, n10e, n10;
    var rat, dect, fkpdec = 89.999;

    if(Math.abs(dec) > fkpdec) {
        corra  = 0.0;
        corrd  = 0.0;
        corrpa = 0.0;
        corrpd = 0.0;
        return {corra,corrd,corrpa,corrpd};
    }

    lway = Jgtfkc.iway;
    Jgtfkc.iway = -1;
    rat = ra;
    dect = dec;
    n10e = 3;
    for (n10=1; n10<=n10e; n10++) {
        const r0 = jgtfkc(rat,dect,xmag,tobs,corra,corrd,corrpa,corrpd);
        corra = r0.corra;
        corrd = r0.corrd;
        corrpa = r0.corrpa;
        corrpd = r0.corrpd;

         if(n10 == n10e) { //return value of iway to previous value:
            Jgtfkc.iway = lway;
            return {corra,corrd,corrpa,corrpd};
         }
         rat = ra - corra;
         dect = dec - corrd;
         if(rat < 0.0)         rat = rat + 360.0;
         else if (rat > 360.0) rat = rat - 360.0;
    }

    Jgtfkc.iway = lway;
    return {corra,corrd,corrpa,corrpd};
}

function jnitfk() {
/* initializes values for use by jgtfkc
*
*  values from Fricke,W.,Schwan,H.,Lederle,T,Fifth Fundamental Catalogue (FK5),
*      1988.
*/

    var i,j,k,m,n;

/*
* idad   is delta(ra(dec))* cos(dec):
*       idad is in units of 0.001s; make dad seconds
* idpmad is delta(propermotion_in_ra(dec)) * cos(dec):
*       idpmad is in units of 0.001s/century; make dpmad seconds/century
* idd    is delta(dec(dec)):
*       idd is in units of 0.01"; make dd "
* idpmdd is delta(propermotion_in_dec(dec)):
*        idpmdd is in units of 0.01"/century; make dpmdd "/century
* for dad,dpmad,dd, and dpmdd values are given for each degree of declination:
*                              location   1 is associated with dec = +90deg;
*                              location  91 is associated with dec =   0deg;
*                              location 181 is associated with dec = -90deg.
*/
      /* J.L. Aug 23, 1995: add a new routine to initialize the jfk5bd */

    block_data_jfk5bd();

    for (i=0; i<181; i++) {
        dad[i]   = 0.001 * idad[i];
        dpmad[i] = 0.001 * idpmad[i];
        dd[i]    = 0.01  * idd[i];
        dpmdd[i] = 0.01  * idpmdd[i];
    }
/*
* idaa   is delta(ra(ra)) * cos(dec):
*        idaa is in units of 0.001s: make daa seconds.
* idpmaa is delta(propermotion_in_ra(ra) * cos(dec):
*        idpmaa is in units of 0.001s/century; make dpmaa s/century.
* idda   is delta(dec(ra)):
*        idda is in units of 0.01"; make dda ".
* idpmda is delta(propermotion_in_dec(ra)):
*        idpmda is in units of 0.01"/century; make dpmda "/century.
*
* for idaa,idpma,idda and idpmd values given for each hour of ra 
*  (m=1 for 0 hrs; m=25 for 24 hrs) and for dec +/- 85 and for +80 thru
*  -80 in steps of 10 deg. (n=1 for +85 deg;, n=2 for +80 deg., n=10 for 0deg;
*  n=18 for -80deg; n=19 for -85deg).
*/
    for (n=0; n<19; n++) {
        for (m=0; m<25; m++) {
            daa[n][m] =   idaa[n][m] * 0.001;
            dpmaa[n][m] = idpmaa[n][m] * 0.001;
            dda[n][m] =   idda[n][m] * 0.01;
            dpmda[n][m] = idpmda[n][m] * 0.01;
        }
    }
/*
* idamm is delta(ra(m)) * cos(dec):
*   idamm is in units of 0.001s; make dam seconds.
* idamam is delta(propermotion_in_ra(m))*cos(dec):
*    idamam is in units of 0.001s/century; make dpmam s/century.
* m is magnitude in this context. values are given for the following 
*  dec zones:  for k = 1: +90 >= dec >= +60
*                  k = 2: +60 >  dec >=   0
*                  k = 3    0 >  dec >= -30
*                  k = 4: -30 >  dec >= -60
*                  k = 5: -60 >  dec >= -90
* and the following magnitudes : j = magnitude = 1 thru 7
*
*/
    for (k=0; k<5; k++) {
        for (j=0; j<7; j++) {
            dam[k][j]   =  idamm[k][j] * 0.001;
            dpmam[k][j] = idamam[k][j] * 0.001;
        }
    }
}

function dintrp( y1, y2, x1, x2, x0) {
/* linear interpolation function  01jun92  jdb */
   return ((y2-y1)/(x2-x1)*(x0-x1) + y1);
}

function dintr2( za1d1, za2d1, za1d2, za2d2,  a1, a2, d1, d2, a, d) {
/*    Given a table,
*                   a1      a2
*                 -----   ------
*         d1    | za1d1 | za2d1 |
*         d2    | za1d2 | za2d2 |
*      find z value where a is value between a1 and a2; d is value
*      between d1 and d2; za2d1 is zvalue at a1,d1,  etc.
*
*/
    const za1d = dintrp(za1d1,za1d2,d1,d2,d);
    const za2d = dintrp(za2d1,za2d2,d1,d2,d);

    return ( dintrp(za1d,za2d,a1,a2,a) );
}

const idad1 = [    // [181]
    -3, -3, -2, -2, -1, -1,  0,  0,  1,  1,  1,  1,  1,  1,  1,
    1,  1,  1,  1,  1,  1,  1,  2,  2,  2,  2,  1,  1,  1,  1,
    0,  0,  0,  0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1,  0,  0,  0,  1,  1,  1,  1,  2,  2,  1,  1,  1,  1,
    0,  0,  0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1,  0,  0,  0,  1,  1,  1,  1,
    1,  1,  1,  2,  2,  2,  2,  3,  3,  3,  4,  4,  4,  4,  4,
    4,  4,  3,  2,  2,  1,  0, -1, -1, -2, -3, -3, -3, -3, -3,
    -3, -3, -2, -1, -1,  0,  1,  2,  2,  3,  3,  3,  2,  2,  1,
    0,  0, -1, -2, -2, -3, -3, -3, -3, -3, -3, -3, -3, -4, -5,
    -5, -7, -8, -9,-11,-12,-13,-14,-14,-14,-14,-13,-13,-12,-11,
    -10, -9, -8, -7, -7, -6, -6, -6, -6, -6, -6, -6, -6, -6, -7,
    -7
];

const idpmad1 = [   // [181]
     -12,-12,-12,-12,-11,-10, -9, -8, -7, -6, -5, -4, -3, -2, -1,
       0,  0,  1,  1,  1,  2,  2,  2,  2,  3,  3,  3,  4,  4,  5,
       5,  5,  5,  6,  6,  6,  6,  6,  6,  5,  5,  5,  5,  4,  4,
       3,  3,  2,  2,  1,  1,  0,  0, -1, -2, -3, -4, -5, -6, -7,
      -9,-10,-11,-12,-14,-14,-15,-16,-16,-16,-16,-15,-15,-14,-13,
     -12,-10, -9, -7, -6, -5, -4, -3, -2, -1,  0,  1,  2,  3,  3,
       4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 13, 14, 14, 13, 13,
      12, 11,  9,  7,  5,  3,  1, -1, -3, -4, -5, -6, -7, -7, -6,
      -5, -4, -2, -1,  1,  4,  6,  7,  9, 10, 11, 12, 12, 11, 10,
       9,  7,  5,  3,  0, -3, -5, -8,-11,-13,-16,-19,-21,-23,-26,
     -28,-30,-33,-35,-38,-41,-43,-46,-48,-51,-53,-54,-56,-56,-57,
     -56,-55,-54,-52,-49,-46,-43,-39,-35,-32,-29,-26,-23,-22,-20,
     -20
];

const idd1 = [   // [181]
       0,  0,  1,  1,  1,  1,  1,  1,  0,  0, -1, -2, -3, -4, -5,
      -5, -5, -5, -5, -5, -4, -4, -4, -3, -3, -3, -3, -2, -2, -2,
      -1, -1, -1, -1,  0,  0,  0,  0,  0, -1, -1, -1, -1, -1, -1,
       0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1,
      -1, -2, -3, -3, -3, -3, -3, -2, -2, -1,  0,  0,  1,  1,  0,
       0, -1, -1, -2, -2, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
      -3, -3, -2, -2, -2, -1, -1,  0,  0,  1,  1,  1,  1,  1,  1,
       1,  1,  0,  0, -1, -1, -1, -2, -2, -1, -1,  0,  0,  1,  2,
       2,  3,  3,  3,  3,  2,  2,  2,  2,  2,  2,  3,  3,  3,  3,
       3,  2,  2,  2,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
       1,  1,  1,  2,  3,  4,  5,  7,  8,  8,  8,  8,  6,  4,  2,
      -1, -3, -6, -8, -9,-10,-10, -9, -7, -5, -3,  0,  2,  3,  5,
       5
];

const idpmdd1 = [   // [181]
       1,  1,  2,  3,  3,  4,  4,  3,  2,  0, -2, -5, -8,-11,-13,
     -15,-16,-16,-16,-15,-13,-12,-11,-10, -9, -9, -9, -9, -8, -7,
      -6, -4, -2,  0,  2,  4,  5,  5,  5,  4,  3,  2,  1,  0,  0,
       0,  0,  1,  2,  3,  4,  5,  6,  7,  7,  7,  7,  6,  4,  3,
       1, -1, -2, -3, -4, -3, -2,  0,  2,  5,  7,  8,  9,  9,  8,
       6,  4,  1, -1, -3, -4, -5, -5, -5, -5, -4, -4, -4, -3, -3,
      -3, -2, -1,  1,  3,  5,  7,  9, 11, 13, 15, 16, 16, 16, 16,
      15, 14, 12, 11, 10,  9,  8,  7,  6,  6,  5,  5,  4,  4,  3,
       2,  2,  1,  1,  1,  1,  1,  1,  0,  0, -1, -2, -4, -5, -7,
      -9,-11,-12,-13,-14,-15,-15,-15,-14,-14,-13,-13,-11,-10, -8,
      -5, -3,  0,  3,  6,  9, 11, 13, 15, 16, 16, 16, 14, 12,  8,
       3, -2, -8,-14,-20,-24,-26,-25,-22,-17,-11, -3,  4, 10, 14,
      16
];

const idaa1= [ // [5][25]
     [  1,  0, -2, -3, -4, -3, -2,  0,  1,  1,  0, -1, -1,  0,  2,
        4,  4,  4,  2,  0, -1, -1,  0,  1,  1],
     [  1,  0, -2, -3, -4, -3, -1,  0,  1,  1,  0, -1, -1,  0,  2,
        4,  4,  4,  2,  0, -1, -1,  0,  0,  1],
     [  0, -1, -2, -3, -3, -2, -1,  0,  1,  1,  0, -1, -1,  0,  2,
        3,  4,  4,  2,  0, -1, -1, -1,  0,  0],
     [  0, -1, -2, -2, -2, -2, -1,  0,  1,  0,  0, -1, -1,  0,  1,
        3,  4,  3,  2,  1,  0, -1, -1,  0,  0],
     [ -1, -1, -2, -2, -2, -1,  0,  0,  0,  0,  0, -1, -1,  0,  1,
        2,  3,  3,  2,  1,  0, -1, -1, -1, -1]
];

const idaa2= [ // [5][25]
     [ -1, -1, -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  1,
        2,  2,  2,  2,  1,  0,  0, -1, -1, -1],
     [ -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,
        1,  1,  1,  1,  1,  0,  0, -1, -1, -1],
     [ -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,
        1,  0,  0,  0,  0,  0, -1, -1, -2, -1],
     [ -1, -1,  0,  1,  1,  1,  1,  1,  1,  0,  0,  0,  0,  1,  0,
        0,  0,  0,  0,  0,  0, -1, -1, -2, -1],
     [ -1, -1,  0,  1,  1,  2,  2,  1,  1,  0,  0,  0,  0,  0,  0,
        0, -1, -1,  0,  0,  0, -1, -2, -2, -1]
];

const idaa3 = [ // [5][25]
     [ -1,  0,  0,  1,  1,  2,  2,  2,  1,  1,  0,  0,  0,  0,  0,
       -1, -1, -1,  0,  0,  0, -1, -2, -2, -1],
     [ -1,  0,  0,  1,  1,  1,  1,  1,  1,  0,  0, -1,  0,  0,  0,
        0, -1,  0,  0,  0,  0, -1, -1, -2, -1],
     [ -1,  0,  0,  0,  0,  0,  1,  1,  1,  0, -1, -1,  0,  0,  0,
        0,  0,  0,  1,  1,  1,  0, -1, -1, -1],
     [ -1,  0,  0,  0, -1, -1,  0,  0,  0, -1, -1, -1,  0,  0,  1,
        0,  0,  1,  1,  2,  1,  1,  0, -1, -1],
     [  0,  0,  0, -1, -1, -1, -1,  0,  0, -1, -1, -1,  0,  0,  1,
        1,  0,  1,  1,  2,  2,  1,  0, -1,  0]
];


const idaa4 = [ // [4][25]
     [ -1,  0,  0,  0, -1, -1,  0,  0,  0, -1, -1, -1,  0,  0,  1,
        0,  0,  1,  1,  2,  2,  1,  0, -1, -1],
     [ -1,  0,  0,  0,  0,  0,  0,  1,  0, -1, -1, -1, -1,  0,  0,
        0,  0,  0,  1,  2,  2,  1,  0, -1, -1],
     [ -1,  0,  0,  0,  0,  1,  1,  1,  1,  0, -1, -1, -1,  0,  0,
       -1, -1,  0,  1,  2,  2,  1, -1, -1, -1],
     [ -1,  0,  0,  1,  1,  1,  1,  1,  1,  0, -1, -1, -1,  0,  0,
       -1, -1,  0,  1,  2,  2,  0, -1, -1, -1]
];

const idpma1 = [ // [5][25]
     [ -8, -9,-10,-11,-10, -8, -4,  2,  7, 10, 10,  6,  2,  2,  6,
       12, 17, 16,  9,  0, -8,-12,-11, -9, -8],
     [ -8, -8,-10,-10,-10, -8, -3,  2,  7, 10, 10,  6,  2,  2,  5,
       12, 16, 15,  9,  0, -8,-11,-11, -9, -8],
     [ -7, -7, -7, -8, -8, -6, -2,  2,  7,  9,  9,  6,  2,  1,  4,
        9, 13, 13,  7, -1, -8,-11,-10, -8, -7],
     [ -5, -4, -4, -5, -4, -3, -1,  2,  6,  8,  7,  5,  2,  1,  3,
        6,  9,  9,  4, -2, -7, -9, -9, -7, -5],
     [ -4, -2, -1, -1, -1, -1,  0,  2,  4,  6,  6,  4,  1,  0,  1,
        3,  5,  5,  2, -2, -6, -8, -8, -6, -4]
];

const idpma2 = [ // [5][25]
     [ -2,  0,  2,  2,  2,  2,  2,  2,  3,  4,  4,  3,  1,  0, -1,
        0,  1,  1,  0, -2, -5, -6, -6, -5, -2],
     [ -2,  1,  4,  5,  5,  4,  3,  2,  2,  2,  2,  1,  0, -1, -2,
       -2, -2, -2, -2, -2, -3, -5, -5, -4, -2],
     [ -1,  2,  5,  6,  6,  5,  4,  2,  1,  1,  0,  0,  0, -1, -2,
       -3, -4, -3, -2, -2, -2, -3, -4, -4, -1],
     [ -2,  1,  5,  7,  7,  7,  5,  3,  2,  0,  0,  0,  0, -1, -2,
       -4, -5, -4, -3, -2, -2, -3, -5, -4, -2],
     [ -3,  0,  4,  7,  8,  8,  7,  5,  3,  1,  0,  0,  0, -1, -3,
       -4, -5, -5, -3, -2, -2, -4, -5, -6, -3]
];

const idpma3 = [ // [5][25]
     [ -5, -1,  3,  6,  8,  9,  9,  7,  5,  3,  1,  0,  0, -1, -3,
       -4, -5, -5, -3, -3, -3, -5, -7, -7, -5],
     [ -6, -3,  2,  6,  8, 10, 10,  9,  7,  5,  3,  1,  0, -1, -3,
       -4, -5, -4, -4, -3, -4, -6, -8, -8, -6],
     [ -8, -4,  0,  4,  8, 10, 10, 10,  8,  6,  4,  2,  1, -1, -3,
       -4, -4, -4, -3, -4, -5, -7, -9, -9, -8],
     [ -8, -6, -2,  3,  6,  9,  9,  9,  8,  7,  5,  3,  1, -1, -2,
       -3, -2, -2, -2, -3, -5, -7, -9, -9, -8],
     [ -9, -7, -4,  0,  4,  6,  7,  7,  6,  6,  6,  4,  2,  0, -1,
       -1,  0,  0,  0, -2, -4, -6, -7, -9, -9]
];

const idpma4 = [ // [4][25]
     [ -9, -8, -6, -2,  1,  3,  3,  3,  4,  5,  6,  5,  3,  1,  0,
        1,  2,  3,  2,  1, -1, -4, -6, -7, -9],
     [ -8, -9, -7, -4, -2,  0,  0,  0,  1,  3,  5,  5,  3,  2,  1,
        3,  5,  6,  5,  3,  1, -1, -4, -6, -8],
     [ -8, -9, -9, -6, -4, -3, -3, -3, -1,  2,  5,  5,  4,  2,  2,
        4,  6,  8,  7,  5,  3,  0, -2, -5, -8],
     [ -8, -9, -9, -7, -4, -3, -4, -4, -2,  1,  4,  5,  4,  2,  2,
        4,  7,  8,  8,  6,  3,  1, -2, -5, -8]
];

const idda1 = [// [5][25]
     [ -4, -3, -2, -2, -2, -2, -1,  0,  2,  4,  5,  5,  4,  2,  2,
        2,  2,  2,  1,  0, -2, -4, -5, -5, -4],
     [ -4, -3, -2, -2, -1, -1, -1,  1,  2,  4,  4,  4,  3,  2,  2,
        2,  2,  2,  1,  0, -2, -4, -4, -5, -4],
     [ -3, -3, -2, -1, -1, -1,  0,  1,  2,  3,  3,  3,  3,  2,  2,
        1,  1,  1,  0,  0, -2, -3, -3, -3, -3],
     [ -3, -2, -2, -1, -1,  0,  1,  1,  2,  2,  2,  2,  2,  2,  2,
        1,  1,  0,  0, -1, -1, -2, -2, -2, -3],
     [ -2, -2, -2, -1,  0,  0,  1,  1,  2,  2,  2,  2,  2,  2,  1,
        1,  1,  0,  0, -1, -1, -2, -2, -2, -2]
];

const idda2 = [// [5][25]
     [ -2, -2, -2, -1,  0,  0,  1,  1,  2,  2,  2,  1,  1,  1,  1,
        1,  1,  0,  0, -1, -1, -2, -2, -2, -2],
     [ -2, -2, -2, -1,  0,  0,  1,  1,  2,  2,  2,  1,  1,  1,  1,
        1,  1,  0,  0, -1, -1, -2, -2, -2, -2],
     [ -2, -2, -1, -1,  0,  1,  1,  1,  2,  2,  1,  1,  1,  1,  1,
        1,  1,  0,  0, -1, -1, -2, -2, -2, -2],
     [ -2, -2, -1,  0,  0,  1,  1,  1,  2,  1,  1,  1,  1,  1,  1,
        0,  0,  0,  0,  0, -1, -1, -2, -2, -2],
     [ -2, -1, -1,  0,  1,  1,  1,  2,  2,  2,  1,  1,  1,  0,  0,
        0,  0,  0,  0, -1, -1, -2, -2, -2, -2]
];

const idda3 = [// [5][25]
     [ -2, -1, -1,  0,  1,  1,  2,  2,  2,  2,  1,  1,  0,  0,  0,
        0,  0,  0,  0, -1, -1, -2, -2, -2, -2],
     [ -2, -1, -1,  0,  1,  2,  2,  3,  2,  2,  1,  1,  0,  0,  0,
        0,  0, -1, -1, -1, -1, -2, -2, -2, -2],
     [ -2, -2, -1,  0,  1,  2,  3,  3,  2,  1,  1,  0,  0,  0,  0,
        0, -1, -1, -1, -1, -1, -1, -2, -2, -2],
     [ -1, -1, -1,  0,  2,  3,  3,  3,  2,  1,  0, -1,  0,  0,  0,
        0, -1, -1, -1, -1, -1, -1, -1, -1, -1],
     [ -1, -1, -1,  0,  2,  3,  3,  3,  2,  0, -1, -1, -1,  0,  0,
        0, -1, -1, -1, -1, -1,  0,  0,  0, -1]
];

const idda4 = [// [4][25]
     [ -1, -2, -2, -1,  1,  3,  4,  3,  1, -1, -2, -2, -1,  0,  1,
        1,  0, -1, -2, -1,  0,  1,  1,  0, -1],
     [ -1, -3, -3, -2,  1,  3,  4,  3,  1, -2, -3, -3, -1,  1,  2,
        2,  0, -1, -2, -1,  0,  2,  2,  1, -1],
     [ -2, -4, -5, -3,  0,  3,  5,  3,  0, -3, -4, -3, -1,  2,  4,
        3,  1, -1, -2, -2,  1,  3,  3,  1, -2],
     [ -2, -5, -5, -4,  0,  3,  5,  4,  0, -3, -4, -3, -1,  3,  4,
        4,  1, -1, -2, -2,  1,  3,  3,  1, -2]
];

const idpmd1 = [// [5][25]
     [-18,-14,-10, -9, -9, -7, -3,  3,  7,  9,  9,  7,  5,  4,  4,
        6, 10, 14, 16, 12,  4, -7,-15,-19,-18],
     [-16,-13,-11, -9, -9, -7, -3,  2,  6,  8,  8,  6,  5,  4,  5,
        7, 10, 13, 14, 11,  4, -5,-13,-17,-16],
     [-13,-12,-11,-10, -8, -6, -2,  2,  4,  4,  4,  4,  4,  5,  6,
        7,  9, 10, 10,  9,  4, -2, -8,-11,-13],
     [ -9,-10,-10, -9, -7, -4, -1,  1,  2,  2,  1,  2,  3,  5,  6,
        7,  7,  7,  7,  6,  4,  1, -3, -6, -9],
     [ -7, -8, -8, -7, -5, -2,  1,  2,  2,  2,  1,  1,  2,  3,  4,
        5,  5,  5,  4,  4,  3,  1, -2, -5, -7]
];

const idpmd2 = [// [5][25]
     [ -7, -8, -7, -5, -2,  0,  3,  4,  4,  4,  3,  2,  1,  2,  2,
        3,  3,  4,  3,  3,  1, -2, -4, -6, -7],
     [ -9, -8, -6, -4, -1,  2,  4,  6,  6,  5,  4,  3,  2,  1,  2,
        2,  3,  3,  3,  2, -1, -3, -6, -8, -9],
     [-10, -9, -7, -4, -2,  2,  4,  6,  7,  6,  4,  3,  2,  2,  3,
        3,  3,  3,  3,  2, -1, -4, -7, -9,-10],
     [ -9, -8, -7, -5, -2,  0,  3,  5,  6,  5,  3,  2,  2,  3,  3,
        4,  4,  4,  4,  3,  0, -3, -7, -9, -9],
     [ -8, -7, -6, -4, -3, -1,  2,  4,  4,  3,  2,  1,  1,  2,  3,
        3,  4,  5,  5,  4,  2, -2, -6, -8, -8]
];

const idpmd3 = [// [5][25]
     [ -7, -6, -5, -3, -2,  0,  2,  3,  3,  1,  0, -1, -1,  0,  2,
        3,  4,  6,  6,  6,  3, -1, -5, -7, -7],
     [ -6, -6, -5, -3, -1,  1,  3,  3,  2,  0, -2, -3, -2,  0,  2,
        3,  4,  5,  6,  5,  3,  1, -3, -5, -6],
     [ -5, -7, -7, -5, -1,  2,  4,  4,  1, -2, -4, -4, -2,  1,  4,
        5,  4,  4,  3,  3,  3,  2,  0, -3, -5],
     [ -4, -7, -8, -6, -2,  1,  3,  3,  0, -4, -5, -3,  0,  5,  7,
        7,  4,  1,  0,  0,  2,  3,  3,  0, -4],
     [ -1, -6, -9, -8, -4,  0,  2,  1, -2, -5, -5, -2,  3,  9, 11,
        8,  3, -2, -4, -3,  1,  5,  6,  3, -1]
];

const idpmd4 = [// [4][25]
     [  1, -5, -9, -9, -5,  0,  3,  2, -2, -6, -7, -2,  5, 12, 14,
        9,  1, -6, -9, -7,  0,  6,  9,  7,  1],
     [  3, -5,-10, -9, -4,  4,  8,  5, -1, -9,-11, -5,  5, 14, 17,
       10, -1,-12,-16,-11, -2,  8, 12, 10,  3],
     [  4, -6,-12,-10, -2,  8, 13, 10,  0,-11,-15, -9,  4, 16, 19,
       11, -4,-17,-21,-15, -2, 10, 16, 13,  4],
     [  4, -7,-13,-10, -1, 10, 15, 12,  0,-11,-16,-10,  4, 16, 20,
       11, -4,-18,-23,-16, -3, 10, 17, 14,  4]
];

const idamm1 = [ // [5][7]
     [  -2, -2, -1, -1,  0,  1,  2],
     [  -2, -1, -1,  0,  0,  0,  1],
     [  -4, -2, -1,  0,  0,  0,  0],
     [  -6, -3, -1,  0,  1,  0,  0],
     [  -8, -5, -2,  0,  1,  1,  1]
];

const idamam1 = [ // [5][7]
     [ -11, -8, -5, -2,  1,  3,  6],
     [  -7, -3, -1,  0,  1,  0, -1],
     [ -14, -8, -3,  0,  1,  1, -1],
     [ -28,-17, -8, -1,  3,  5,  4],
     [ -39,-24,-12, -3,  4,  8,  9]
];

/**
 *      block data jfk5bd
 *
 *  updated 27jul92 to add common/fk5way/iway so jgtfkc can check for "pole"
 *    value when correction will be added in calling program (iway >=0) or
 *    will be removed in calling program (iway < 0) and return fk5-fk4
 *    correction values of 0. when too close to pole for valid corr or
 *    reproducibility problems.
 *  updated 22jun92 to add common/fkappl/japply  so user may change
 *   default application of systematic corrections by including common/fkappl/
 *    and setting japply  (japply .ne. 0 to apply corr - i.e. add when going
 *     from B1950 to J2000, subtract when going from J2000 to b1950;
 *     japply = 0 to not apply corr.-allows calling program a central info.
 *       location to determine application desires (however, if jgtfkc is
 *       called, corr. factors will be returned and calling program
 *       must make decision (based on japply)  whether to apply them.
 *
 * Note: common/fkappl/ is used in gtjulX/unjulX routines and may be used/reset
 *                      in any user subroutine.
 *       common/fk5way/ is set to -1 in junfkc, but returned to previous value
 *                      on return; is used in junfkc and jgtfkc.
 *
 *       See subroutine jnitfk for explanation of values in common/fk5cc2/
 *
 */
function block_data_jfk5bd() {
    var i, j;

    for (i=0; i<181; i++) idad[i] = idad1[i];

    for (i=0; i<181; i++) idpmad[i] = idpmad1[i];

    for (i=0; i<181; i++) idd[i] = idd1[i];

    for (i=0; i<181; i++) idpmdd[i] = idpmdd1[i];

    for(i=0; i<5; i++) {
        for (j=0; j<25; j++) {
            idaa[i][j]    = idaa1[i][j];
        }
    }

    for(i=0; i<5; i++) {
        for (j=0; j<25; j++) {
            idaa[5+i][j]    = idaa2[i][j];
        }
    }

    for(i=0; i<5; i++) {
        for (j=0; j<25; j++) {
            idaa[10+i][j]   = idaa3[i][j];
        }
    }

    for(i=0; i<4; i++) {
        for (j=0; j<25; j++) {
            idaa[15+i][j]   = idaa4[i][j];
        }
    }

    for(i=0; i<5; i++) {
        for (j=0; j<25; j++) {
            idpmaa[i][j]  = idpma1[i][j];
        }
    }

    for(i=0; i<5; i++) {
        for (j = 0; j < 25; j++) {
            idpmaa[5 + i][j] = idpma2[i][j];
        }
    }

     for(i=0; i<5; i++) {
         for (j = 0; j < 25; j++) {
             idpmaa[10 + i][j] = idpma3[i][j];
         }
     }

    for(i=0; i<4; i++) {
        for (j = 0; j < 25; j++) {
            idpmaa[15 + i][j] = idpma4[i][j];
        }
    }

     for(i=0; i<5; i++) {
         for (j = 0; j < 25; j++) {
             idda[i][j] = idda1[i][j];
         }
     }

    for(i=0; i<5; i++) {
        for (j = 0; j < 25; j++) {
            idda[5 + i][j] = idda2[i][j];
        }
    }

    for(i=0; i<5; i++) {
        for (j = 0; j < 25; j++) {
            idda[10 + i][j] = idda3[i][j];
        }
    }

    for(i=0; i<4; i++) {
        for (j = 0; j < 25; j++) {
            idda[15 + i][j] = idda4[i][j];
        }
    }

     for(i=0; i<5; i++) {
         for (j = 0; j < 25; j++) {
             idpmda[i][j] = idpmd1[i][j];
         }
     }

    for(i=0; i<5; i++) {
        for (j = 0; j < 25; j++) {
            idpmda[5 + i][j] = idpmd2[i][j];
        }
    }

    for(i=0; i<5; i++) {
        for (j = 0; j < 25; j++) {
            idpmda[10 + i][j] = idpmd3[i][j];
        }
    }

    for(i=0; i<4; i++) {
        for (j = 0; j < 25; j++) {
            idpmda[15 + i][j] = idpmd4[i][j];
        }
    }

    for (i=0; i<5; i++) {
        for (j = 0; j < 7; j++) {
            idamm[i][j] = idamm1[i][j];
        }
    }

     for (i=0; i<5; i++) {
         for (j = 0; j < 7; j++) {
             idamam[i][j] = idamam1[i][j];
         }
     }
}


