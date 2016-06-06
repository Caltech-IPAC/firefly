/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {ecleqx,equecx} from './Ecleqx.js';
import {nwcprc} from './Nwcprc.js';
import {fk5prc} from './Fk5prc.js';
import {galeqd,galsgl,equgad,sglgal} from './Galeqd.js';
import {unjul2,gtjul2} from './Gtjul2.js';

/**
 *  26mar-01apr92 j.bennett
 *  This updated version, aug92, is totally restructured in order to
 *    handle the addition of ecliptic of date; also problems in the pre-release
 *    version have been fixed. (Pre-release version failed to produce
 *    accurate results if input was Bess. equ (isys1 =1) with equinox other
 *    than 1950.0d0 and output was Julian equatorial (isys2 =0) with equinox
 *    2000.0d0 (e.g. B1975 to J2000 conversion was wrong).  Also,
 *    when isys1  was 2,3,or 4 (galactic,ecliptic,supergalactic) and
 *    isys2  =1 (Bess. equ) and equinox not 1950.0d0, result was wrong).
 *
 *  jcnvc2 is version of jcnvcd that can handle J2000 input and output and
 *         do appropriate conversions before/after calling ecl,gal,sgl conv.
 *         routines.
 *         E-term removal/replacement assumed in B1950 <==>J2000 conversions.
 *
 *  16Sep92 to comment out write on unit 0
 *  24aug92 updated comments only.
 *  11-13,20aug92 to implement ecliptic of date capability.
 *
 *    calls gtjul2, unjul2, nwcprc, fk5prc
 *    calls ecleqx,equecx,equgad,galeqd,galsgl,sglgal
 *     the new subroutines compute(the first time thru & store) values for
 *     the galactic <==> equatorial conversion matrices from the
 *     actual angles so that maximum precision may be maintained for
 *     the machine being used for computation. Also better agreement is
 *     obtained when returning to input coord system from output values.
 *
 *    Also radian to deg., etc. computed using machine functions (i.e.
 *     radian-to-deg = datan(1.0d0) / 45.0d0 )
 *    If |dec| or |lat| = 90.0d0, the corresponding ra or lon = 0.0d0.
 *
 * @param x0,y0     = input lon and lat in degrees,
 * @param y0
 * @param xnew,ynew = DUMMY FROM CONVERSION output lon and lat in degrees.
 * @param ynew
 * @param epoki,epoko input output equinox (used if jsys1,jsys2 0, 1, 3, 11,or 13).
 * @param epoko
 *    epok_ ignored for galactic (jsys_=2) and supergalactic (isys_=4).
 *    If epok_ input as 0.0d0, then 1950.0d0 used if isys_ is 1 or 3;
 *                                  2000.0d0 used if isys_ is 0,11 or 13.
 * @param isys1  = input coordinate system#;  isys2 = output system#.
 *     0 = equ(Jul. ),     1 = equ(Bess),
 *     2 = gal,            3 = ecl(Bess),     4 = supergalactic (sgl)
 *     11= equ(Jul.),      13= ecl(Jul.)
 * @param isys2
 * @param tobsin is observation epoch used when B1950 <==> J2000 conversions required.
 *   Note:gal and sgl conversions require B1950 as input, give B1950 as output
 *   when working with the original subroutines. jcnvc2 will convert between
 *   B1950 and J2000 as needed and as appropriate.
 *
 */
export function jcnvc2(isys1, epoki, x0, y0,isys2, epoko, xnew, ynew, tobsin) {
      var x=0.0,y=0.0;

      var tobs = tobsin;
      if(tobs == 0.0) tobs = 1950.0;
                    //  iauf1 = 1 for Besselian; iauf1 = 2 for Julian.
                    //  iauf2 = 1 for Besselian; iauf2 = 2 for Julian.
      var iauf1 = 1;
      var iauf2 = 1;
      var jsys1 = isys1;
      var jsys2 = isys2;

      if(isys1=== 11 || isys1=== 0) {
         iauf1 = 2;
         jsys1 = 0;
      }
      else if (isys1=== 13) {
          iauf1 = 2;
      }

      if(isys2=== 11 || isys2=== 0) {
         iauf2 = 2;
         jsys2 = 0;
      }
      else if (isys2=== 13)  {
          iauf2 = 2;
      }

      var poki = epoki;
      var poko = epoko;
      if(jsys1 ===2 || jsys1=== 4) {
          poki = 1950.0;
      }
      else {
         if(poki ===0.0) {
            if(iauf1 ===2) {
                poki = 2000.0;
            }
            else {
                poki = 1950.0;
            }
         }
      }

      if(jsys2 ===2 || jsys2=== 4) {
          poko = 1950.0;
      }
      else {
         if(poko ===0.0) {
            if(iauf2=== 2)  {
                poko = 2000.0;
            }
            else {
                poko = 1950.0;
            }
         }
      }

      if(jsys1=== jsys2 && poki=== poko)
      {
         xnew = x0;
         ynew = y0;
         return {xnew,ynew};
      }

      var xin = x0;
      var yin = y0;
/*
* see if ecliptic is involved in input - if so, convert now to equatorial
*   and reset input flags.
*/
      if(jsys1 ===3 || jsys1=== 13) {
         const r1= ecleqx(iauf1,poki,x0,y0,xin,yin);
         xin= r1.rad;
         yin= r1.decd;
         if(iauf1===1) {
             jsys1 = 1;
         }
         else {
             jsys1 = 0;
         }
         if(jsys2===jsys1 && poki===poko) {
            xnew = xin;
            ynew = yin;
            return {xnew,ynew};
         }
      }

      else if(jsys1===4) {
/*                      see if supergalactic or galactic on input: */
          var r2=  sglgal(xin,yin,x,y);
          x= r2.ra;
          y= r2.dec;
          if(jsys2=== 2) {
             xnew = x;
             ynew = y;
             return {xnew,ynew};
          }
          var r3=galeqd(x,y,xin,yin);
          xin= r3.ra;
          yin= r3.dec;
          jsys1 = 1;
      }

      else if(jsys1===2) {
          if(jsys2===4) {
              ret= galsgl(xin,yin,xnew,ynew);
              xnew= ret.ra;
              ynew= ret.dec;
              return {xnew,ynew};
          }

          var r4=galeqd(xin,yin,x,y);
          x= r4.ra;
          y= r4.dec;
          xin = x;
          yin = y;
          jsys1 = 1;
      }

      if(isys2 == jsys1 && poki == poko) {
         xnew = xin;
         ynew = yin;
         return {xnew,ynew};
      }
/*
* see if ecliptic output desired - first set up to get appropriate
*                intermediate equatorial output:
*/
      if(isys2===3)  {
          jsys2 = 1;
      }
      else if (isys2 == 13) {
          jsys2 = 0;
      }
/*
* see if equatorial (or redefined equatorial on input ( bess or julian )
*/
      if(jsys1===0) {
/*                   input is Julian; is output (iauf2=1) to be bess? */
       if(iauf2===1) {
          if(poki!==2000.0) {
             const ret= fk5prc(poki,xin,yin,2000.0,x,y);
             x= ret.raou;
             y= ret.decou;
             xin = x;
             yin = y;
             poki = 2000.0;
          }
          var r1= unjul2(xin,yin,tobs,1,poko,x,y);
          x= r1.ra;
          y= r1.dec;
          if(isys2===1) {
             xnew = x;
             ynew = y;
             return {xnew,ynew};
          }
          else {
             xin = x;
             yin = y;
             poki = poko;
             iauf1 = 1;
             jsys1 = 1;
          }
       }
       else {
/*            else(iauf2=2) output to be Julian (equ or ecl) */
           if(poki!==poko) {
              var ret= fk5prc(poki,xin,yin, poko,x,y);
              x= ret.raou;
              y= ret.decou;
              xin = x;
              yin = y;
              poki = poko;
           }

           if(isys2===0 || isys2==11) {
              xnew = xin;
              ynew = yin;
              return {xnew,ynew};
           }
       }
      }
      else if(jsys1===1) {
/*                               input Bess; is output Bess ? */
         if(iauf2===1) {
            if(poki !==poko) {
               const ret= nwcprc(poki,xin,yin,poko,x,y);
               x= ret.raou;
               y= ret.decou;
               xin = x;
               yin = y;
               poki = poko;
            }
            if(isys2===1) {
               xnew = xin;
               ynew = yin;
               return {xnew,ynew};
            }
         }
         else {
/*                else(iauf2=2) ouput is to be Julian (equ or ecl) */
            const r1= gtjul2(poki,xin,yin,tobs,1,x,y);
            x= r1.ra;
            y= r1.dec;
            if(poko!==2000.00) {
               xin = x;
               yin = y;
               const ret= fk5prc(2000.0,xin,yin,poko,x,y);
               x= ret.raou;
               y= ret.decou;

            }
            if(isys2===0 || isys2===11) {
               xnew = x;
               ynew = y;
               return {xnew,ynew};
            }
            else {
               xin = x;
               yin = y;
               poki = poko;
               iauf1 = 2;
               jsys1 = 0;
            }
         }
      }

      if(isys2===3 || isys2===13) {
         const ret= equecx(iauf2,poko,xin,yin,xnew,ynew);
         xnew= ret.xlam;
         ynew= ret.beta;
         return {xnew,ynew};
      }
      if(isys2===4 || isys2===2) {
         const ret= equgad(xin,yin,x,y);
         x= ret.ra;
         y= ret.dec;
         if(isys2===4)  {
            const ret1= galsgl(x,y,xnew,ynew);
            xnew= ret1.ra;
            ynew= ret1.dec;
         }
         else {
            xnew = x;
            ynew = y;
         }
         return {xnew,ynew};
      }
      return {xnew,ynew};
}
