/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 *
 * @param {number} iaus
 * @param {number} eqx
 * @param {number} ep
 * @return {number}
 */
export function jgtobq( iaus, eqx, ep) {
   var t, t2, t3;

   if(iaus !== 1) {
      t = (eqx - 2000.0) * 0.01;
      t2 = t*t;
      t3 = t*t2;

      ep = (84381.448 - 46.8150*t - 0.00059*t2 + 0.001813*t3)/
          3600.0;

   }
   else {
      t = (eqx - 1950.0) * 0.01;
      t2 = t*t;
      t3 = t*t2;

      ep = (84404.84  - 46.850*t  - 0.0033*t2  + 0.00182*t3)/
          3600.0;
   }
   return ep;
}

var nthru = 0, laus = -99;
var dtor, rtod, leqx = -1.0, cose, sine;

/**
 * 
 * @param {number} iaus
 * @param {number} eqx
 * @param {number} xlam
 * @param {number} beta
 * @param {number} rad
 * @param {number} decd
 * @return {{rad: number, decd: number}}
 */
export function ecleqx(iaus,eqx,xlam, beta,rad,decd) {

   var decr=0.0,e=0.0;

   if(nthru===0) {
      dtor = Math.atan(1.0) / 45.0;
      rtod = 1.0 / dtor;
      nthru = 1;
   }

   if(eqx !== leqx || iaus !== laus) {
      e= jgtobq(iaus,eqx,e);
      e = e * dtor;
      cose = Math.cos(e);
      sine = Math.sin(e);
      leqx = eqx;
      laus = iaus;
   }

   var xlamr = xlam*dtor;
   var betar = beta*dtor;

   const cosb = Math.cos(betar);
   const cosl = Math.cos(xlamr);
   const sinb = Math.sin(betar);
   const sinl = Math.sin(xlamr);

   const xl = sinb;
   const yl = -(cosb*sinl);
   const zl = cosb*cosl;

   const xe = cose*xl - sine*yl;
   const ye = sine*xl + cose*yl;
   const ze = zl;

   const rar = Math.atan2(-ye,ze);
   rad = rar * rtod;
   if(rad < 0.0) rad = 360.0 + rad;

   /* try to catch pole on any machine (& return ra=0 then) */

   if(Math.abs(xe) > 1.0) {
      decd = 90.0*xe/Math.abs(xe);
      rad = 0.0;
   }
   else {
      decr =  Math.asin(xe);
      decd = decr * rtod;
      if(Math.abs(decd) >= 90.0) {
         rad = 0.0;
         if(decd >  90.0) decd =  90.0;
         if(decd < -90.0) decd = -90.0;
      }
   }
   return {rad, decd};
}

//   private static int nthru = 0, laus = -99;
//   private static double dtor, rtod, leqx = -0.1, cose, sine;
var laus2 = -99;
var leqx2 = -1.0, cose2, sine2;

/**
 * 
 * @param {number} iaus
 * @param {number} eqx
 * @param {number} rad
 * @param {number} decd
 * @param {number} xlam
 * @param {number} beta
 * @return {{xlam:number, beta:number}}
 */
export function equecx(iaus, eqx, rad, decd, xlam, beta) {

   var xlamr=0.0,betar=0.0,e=0.0;

      if(nthru===0) {
         dtor = Math.atan(1.0) / 45.0;
         rtod = 1.0 / dtor;
         nthru = 1;
      }

      if(eqx !== leqx2 || iaus !== laus2) {
         e= jgtobq(iaus,eqx,e);
         e = e * dtor;
         cose2 = Math.cos(e);
         sine2 = Math.sin(e);
         leqx2 = eqx;
         laus2 = iaus;
      }

      var rar = rad*dtor;
      var decr = decd*dtor;

      const cosd = Math.cos(decr);
      const cosr = Math.cos(rar);
      const sind = Math.sin(decr);
      const sinr = Math.sin(rar);

      const xe = sind;
      const ye = -(cosd*sinr);
      const ze = cosd*cosr;

      const xl = cose2*xe + sine2*ye;
      const yl = -sine2*xe + cose2*ye;
      const zl = ze;

      if(Math.abs(xl) > 1.0) {
         beta = 90.0*xl/Math.abs(xl);
         xlam = 0.0;
         return {xlam, beta};
      }
      else {
         betar =  Math.asin(xl);
         xlamr = Math.atan2(-yl,zl);
         xlam = xlamr * rtod;
         if(xlam < 0.0) xlam = 360.0 + xlam;
         beta = betar * rtod;
         if(Math.abs(beta) >= 90.0) {
            if(beta >  90.0) beta =  90.0;
            if(beta < -90.0) beta = -90.0;
            xlam = 0.0;
         }
      }
      return {xlam, beta};
}
