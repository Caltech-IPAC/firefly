/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

var nthru = 0;
var dtor, rtod;

/**
 *
 * updated 09jun92 to test costh ( to prevent acos error message when
 *   round off near theta = 0. or 180.deg creates a dabs(cos(theta)) slightly
 *   greater than 1.0d0)
 * \ned\jbdist.f  28feb91  like prev. versions except does not call
 *     fdtor2/frtod2
 * f77 -c jbdist.f
 * for vax, dacos is used instead of darcos.
 * all args in double precision degrees.
 *
 * @param {number} xlon1
 * @param {number} xlat1
 * @param {number} xlon2
 * @param {number} xlat2
 * @return {number} distd is distance between 2 positions on sky - results in degrees
 *
 */
export function jbdist( xlon1, xlat1, xlon2, xlat2) {

    if(nthru===0) {
        dtor = Math.atan(1.0) / 45.0;
        rtod = 1.0 / dtor;
        nthru = 1;
    }

    const xlon1r =  dtor * xlon1;
    const xlon2r =  dtor * xlon2;
    const xlat1r =  dtor * xlat1;
    const xlat2r =  dtor * xlat2;

    var costh = Math.cos(xlat1r)*Math.cos(xlat2r)*Math.cos(xlon1r-xlon2r) +
        Math.sin(xlat1r)*Math.sin(xlat2r);

    if(Math.abs(costh) > 1.0) costh = costh/Math.abs(costh);
    const distd =  rtod*( Math.acos(costh));
    return distd;
}
