/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {jcnvc2} from './Jcnvc2.js';
import {jbposa, jbelpt} from './Jbposa.js';

/**
* comments updated 24aug92.
* j.bennett
* jcnvp2 version of jcnvpa that can handle julian prec., etc. -
*      has tobsin arg. that jcnvpa does not have.
*      calls jcnvc2 instead of jcnvcd (still needs subs in jcnvcd though)
*
*
*       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
*
* @param {number} jsysin is coord. system of input:
*          1 = equ(Bess); 2 = gal; 3 = ecl(Bess); 4 = sgl.
*          0 = equ(Jul); 11 = equ(Jul)=same as 0; 13 = ecl(Jul)
*
* @param {number} epokpa is equinox of pain (ignored if jsysin > 2 or 4)- if 0.0d0, then
*         routine sets epokpa = to equinox of input position.
* @param {number} distin = semi=major axis of error ellipse (really used only as flag-
*        if distin <= 0.d0, then position angle comp. not done).
* @param {number} pain = input position angle (0 to 180.0d0 deg.)
* @param {number} epoki is equinox of xlonin,xlatin (ignored if jsysin = 2 or 4; If 0.0d0
*         1950.0d0 is assumed if jsysin 1 or 3;  2000.0d0 if jsysin
*         0, 11, or 13)
* @param {number} xlonin,xlatin  coordinates of input position
 * @param {number} xlatin
* @param {number} tobsin is observation epoch (needed for B1950 <==> J2000 system conversions)
*          if tobsin = 0.0d0, then 1950.0d0 is used.
* @param {number} jsysou is coord system of output (see jsysin for values).
* @param {number} epoko is equinox of output. Same default rules as for epoki, but is function
*       of jsysou.
* @param {number} paou is output position angle in jsysou, equinox=epoko if
*         distin > 0.0d0  and ierr = 0 ( garbage otherwise).
* @param {number} xlonou,xlatou is output position in jsysou, equinox=epoko.
 * @param {number} xlatou
* @param {number} ierr = 0 for normal return. -1 if paou could not be computed when
*   distin > 0.d0
*
* NOTE:
* paou computed only if distin > 0.0d0 deg. (however 0.1 deg. is used
*      for distance in recomputation of pa.
* This routine will enforce the assumption that equinox = 1950. for
    *    galactic and supergalactic coordinate systems.
* This routine assumes input pos angle & input position are in the same
*    coordinate system (however, if equ, not necessarily the same equinox).
*
*
*  NOTE:  result in position angle not same when going in two steps
*         from J2000 to B1950 to galactic as when going in one step
*         from J2000 to galactic because an intermediate position angle
*         is computed for B1950 and used to create point to convert
*         to galactic in two-step process.
*
* calls jbposa,jbelpt,jcnvc2
*/
export function jcnvp2(jsysin, epokpa, distin, pain, epoki, xlonin, xlatin, tobsin,
                       jsysou, epoko, paou, xlonou, xlatou, ierr) {

                         // get central position:
                         // note: for central pos., jcnvc2 takes care of
                         //       non-stated (e.g. 0.0d0) values)

   var iflg=0, ipf=0;
   var pokpa,pokin,pokou,tobs;
   var plonp0=0.0,platp0=0.0,plonp1=0.0,platp1=0.0,
       plon0=0.0, plat0=0.0, plon1=0.0, plat1=0.0,dumd=0.0;
   var dist = 0.1;

   ierr = 0;

   tobs = tobsin;

   var r1=jcnvc2(jsysin,epoki,xlonin,xlatin, jsysou,epoko,xlonou,xlatou,tobs);

   if(distin <= 0.0) {
      paou = pain;
      return {paou, xlonou:r1.xnew, xlatou:r1.ynew, ierr};
   }
/*
*
* Now take care of position angle conversions, if any:
*
*/
   pokin = epoki;
   if(jsysin===2 || jsysin===4) {
      pokin = 1950.0;
   }
   else {
      if(pokin == 0.0) {
         if(jsysin == 0  || jsysin >= 11) {
            pokin = 2000.0;
         }
         else {
            pokin = 1950.0;
         }
      }
   }

   pokou = epoko;
   pokpa = epokpa;
   if(pokpa <= 0.0 || jsysin=== 2 || jsysin=== 4) {
      pokpa = pokin;
   }

   if(Math.abs(pokpa-pokin) > 0.5) {
/* precess central pt to equinox of pos angle (implies equatorial)
*
*/
        var ret= jcnvc2(jsysin,pokin,xlonin,xlatin,
                                        jsysin,pokpa,plonp0,platp0,tobs);
        plonp0= ret.xnew;
        platp0= ret.ynew;
      }
      else
      {
         plonp0 = xlonin;
         platp0 = xlatin;
      }
/*
* find end point in equinox of pa
*
*/
      var r3= jbelpt(plonp0,platp0,dist,pain, plonp1,platp1,iflg);
      iflg=   r3.iflg;
      plonp1= r3.xlonp;
      platp1= r3.xlatp;

      if(iflg===3) {
         plonp0 = 0.0;
         if(platp0 < 0.0) platp0 = -90.0;
         else             platp0 = 90.0;
      }

      if(iflg=== 4) {
         plonp1 = 0.0;
         if(platp1 < 0.0) platp1 = -90.0;
         else             platp1 = 90.0;
      }
/*
* precess / convert both points to output equinox:
*/
      var r2;
      r2= jcnvc2(jsysin,pokpa,plonp0,platp0,
                        jsysou,pokou,plon0,plat0,tobs);
      plon0= r2.xnew;
      plat0= r2.ynew;

      r2= jcnvc2(jsysin,pokpa,plonp1,platp1, jsysou,pokou,plon1,plat1,tobs);
      plon1= r2.xnew;
      plat1= r2.ynew;

/*
*
* get new pos angle
*
*/
      var r5=  jbposa(plon1,plat1,plon0, plat0,dumd,paou,ipf);
      // dumd= r5.distd;
      paou= r5.pa;
      ipf=  r5.ipf;

      if(ipf===0) {
        ierr = -1;
        paou = 0.0;
      }

      return {paou, xlonou, xlatou, ierr};
}
