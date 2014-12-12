package edu.caltech.ipac.astro.conv;

public class Nwcprc {

public static NwcprcRetval nwcprc( double eqx1, double rain, double decin, 
                                   double eqx2, double raou, double decou)
//double eqx1,rain,decin,eqx2,*raou,*decou;
{
/* 22jul92: nwcprc and nwcprp.
* j.bennett
* for Besselian (aka Newcomb) precession.
*  method of fk5prc, but with constants from herget general precession(jprecj;
*              also see p.6 of Publ.Cincinnati Obs. No.24)
*  For object without proper motion, nwcprc yields essentially the same
*  results as jprecj (within +/-0.2e-08 arcseconds);  nwcprcp allows
*  rotation matrix computation method of precessing position and propermotions
*  for objects with proper motions. With jprecj propermotions had to be
*  applied outside of the jprecj routine.
*
* All arguments are double precision:
*  eqx1 is equinox of input position (in year - e.g. 1950.0d0)
*  rain, decin is input position in decimal degrees at eqx1.
*  eqx2 is eqinox of output position (in year - e.g. 1992.0d0)
*  raou, decou is position in decimal degrees at eqx2.
*
*/
      double dumpma= 0.0,dumpmd= 0.0;
      NwcprpRetval ret= nwcprp(eqx1,rain,decin,0.0,0.0,0.0,0.0,
                     	     eqx2,raou,decou,dumpma,dumpmd);
      return new NwcprcRetval(ret._raou,ret._decou); 
}
    public static class NwcprcRetval {
        public double _raou;
        public double _decou;
        public NwcprcRetval(double raou, double decou) {
             _raou= raou;
             _decou= decou;
        }
    }
    static double rtod,dtor,delt,f,leqx1 = -1.0, leqx2 = -1.0;
    static double p[][]= new double[3][3];

public static NwcprpRetval nwcprp(double eqx1, double rain,   double decin, 
                                  double pmain, double pmdin, double pin, 
                                  double vin,   double eqx2,  double raou,
                                  double decou, double pmaou, double pmdou)
//double eqx1,rain,decin,pmain,pmdin,pin,vin,
//       eqx2,*raou,*decou,*pmaou,*pmdou;
{
/*  j.bennett - for Besselian (aka Newcomb) precession:
*              using constants from herget general precession (jprecj;
*              also see p.6 of Publ.Cincinnati Obs. No.24)
*
*  allows precession of proper motion values (also will make use of
*    parallax and radial velocity if not zero.
*
* All arguments are double precision:
*  eqx1 is equinox of input position (in year - e.g. 1950.0d0)
*  rain, decin is input position in decimal degrees at eqx1.
*  pmain is proper motion in ra in seconds of time per Tropical century.
*  pmdin is proper motion in dec in seconds of arc per Tropical century.
*  pin is parallax in arc seconds (0.0d0 if none)
*  vin is radial velocity in km/sec (0.0d0 if none)
*  eqx2 is eqinox of output position (in year - e.g. 1992.0d0)
*  raou, decou is position in decimal degrees at eqx2.
*  pmaou is proper motion in ra in seconds of time per Tropical century for eqx2
*  pmdou is proper motion in dec in seconds of arc per Tropical century for eqx2
*
*******************************************************************************
* method from Fricke,W.,Schwan,H.,Lederle,T.,1988,Fifth Fundamental Catalogue
*     (FK5) Part I. (pages 10-11)
*******************************************************************************
*/

      int i;
      double  zetar,zr,thetar,zeta,z,theta;
      double  tau, t;
      double  r0[]= new double[3], rdot0[]= new double[3];
      double  r[] = new double[3], rdot[] = new double[3];
      double  pivelf;
      double  czet,cthet,cz,szet,sthet,sz;
      double  cosa,sina,cosd,sind,rar,decr;
      double  cosao,sinao,cosdo,sindo;
      double  pmas,pmds,rdiv,raour,decour,pmaous,pmdous;
      double  duda[]= new double[3],dudd[]= new double[3];

      if(eqx1 == eqx2)
      {
         raou = rain;
         decou = decin;
         pmaou = pmain;
         pmdou = pmdin;
         return new NwcprpRetval(raou,decou,pmaou,pmdou);
      }

      if(eqx1 != leqx1 || eqx2 != leqx2) 
      {
      dtor = Math.atan(1.0) / 45.0;
      rtod = 1.0 /dtor;
/* tau, t, delt in Tropical centuries: */
      tau =  (eqx1 - 1900.0) * 0.01;
      t    = (eqx2 - eqx1) * 0.01;
      delt = t;
/*                   f is value for 1 arcsec in radian measure. */
      f    = 4.8481368e-06;
      leqx1 = eqx1;
      leqx2 = eqx2;
/*
*    zeta, theta, z in seconds of arc:
*
*/
      zeta = (2304.2530 + 1.39750*tau + 0.000060*tau*tau)*t +
             (0.30230  - 0.000270*tau)*t*t + 0.018000*t*t*t;

      z = (2304.2530 + 1.39750*tau + 0.000060*tau*tau)*t +
          (1.09500 + 0.000390*tau)*t*t + 0.018320*t*t*t;

      theta = (2004.6850 - 0.85330*tau - 0.000370*tau*tau)*t -
              (0.42670 + 0.000370*tau)*t*t - 0.041800*t*t*t;
      zetar = (zeta/3600.0)*dtor;
      zr   = (z/3600.0)*dtor;
      thetar = (theta/3600.0)*dtor;
      czet = Math.cos(zetar);
      szet = Math.sin(zetar);
      cz   = Math.cos(zr);
      sz   = Math.sin(zr);
      cthet= Math.cos(thetar);
      sthet= Math.sin(thetar);
/*
* p matrix from Green,Robin M.,1985,Spherical Astronomy, Cambridge University
*   Press, p. 221-222.
*
*/
      p[0][0] = czet*cthet*cz - szet*sz;
      p[1][0] = czet*cthet*sz + szet*cz;
      p[2][0] = czet*sthet;
      p[0][1] = -szet*cthet*cz - czet*sz;
      p[1][1] = -szet*cthet*sz + czet*cz;
      p[2][1] = -szet*sthet;
      p[0][2] = -sthet*cz;
      p[1][2] = -sthet*sz;
      p[2][2] = cthet;

      }

      rar = dtor*rain;
      decr = dtor*decin;
      cosa = Math.cos(rar);
      sina = Math.sin(rar);
      cosd = Math.cos(decr);
      sind = Math.sin(decr);
      r0[0] = cosd*cosa;
      r0[1] = cosd*sina;
      r0[2] = sind;

      pmas = pmain * 15.0;
      pmds = pmdin;
      if(vin == 0.0 || pin == 0.0) 
      {
         rdot0[0] = f*(pmas*(-cosd)*sina + pmds*(-sind)*cosa);
         rdot0[1] = f*(pmas*cosd*cosa + pmds*(-sind)*sina);
         rdot0[2] = f*(pmds*cosd);
      }
      else
      {
         pivelf = 21.094953 * pin *vin;
         rdot0[0] = f*(pmas*(-cosd)*sina + pmds*(-sind)*cosa+pivelf*r0[0]);
         rdot0[1] = f*(pmas*cosd*cosa + pmds*(-sind)*sina + pivelf*r0[1]);
         rdot0[2] = f*(pmds*cosd + pivelf*r0[2]);
      } 
      for (i=0; i<3; i++)
      {
         rdot[i] = p[i][0]*rdot0[0] + p[i][1]*rdot0[1] + p[i][2]*rdot0[2];
         r[i] = p[i][0]*(r0[0]+rdot0[0]*delt) +
                p[i][1]*(r0[1]+rdot0[1]*delt) +
                p[i][2]*(r0[2]+rdot0[2]*delt);
      }

      raour = Math.atan2(r[1],r[0]);
      decour =Math.atan2(r[2], Math.sqrt(r[0]*r[0] + r[1]*r[1]));
      rdiv =  Math.sqrt(r[0]*r[0] + r[1]*r[1] + r[2]*r[2]);
      cosdo = Math.cos(decour);
      sindo = Math.sin(decour);
      cosao = Math.cos(raour);
      sinao = Math.sin(raour);

      duda[0] = -cosdo*sinao;
      duda[1] =  cosdo*cosao;
      duda[2] =  0.0;
      dudd[0] = -sindo*cosao;
      dudd[1] = -sindo*sinao;
      dudd[2] =  cosdo;
      rdot[0] = rdot[0] / rdiv;
      rdot[1] = rdot[1] / rdiv;
      rdot[2] = rdot[2] / rdiv;
      pmaous = (rdot[0]*duda[0] + rdot[1]*duda[1] + rdot[2]*duda[2])/
               (f*cosdo*cosdo);
      pmdous = (rdot[0]*dudd[0] + rdot[1]*dudd[1] + rdot[2]*dudd[2])/f;

      raou = raour * rtod;
      if(raou > 360.0) 
         raou = raou - 360.0;
      else if (raou < 0.0) 
         raou = raou + 360.0;

      decou = decour * rtod;
/*
* since decour is output of datan2(y,x) with x always positive, if dec
*  .gt. 90. it is due to rounding when being converted to degrees.
*/
      if(decou > 90.0) 
         decou = 90.0;
      else if (decou < -90.0) 
         decou = -90.0;

      pmaou = pmaous / 15.0;
      pmdou = pmdous;

      return new NwcprpRetval(raou,decou,pmaou,pmdou);
}
    public static class NwcprpRetval {
        public double _raou;
        public double _decou;
        public double _pmaou;
        public double _pmdou;
        public NwcprpRetval(double raou,  double decou, 
                            double pmaou, double pmdou) {
             _raou= raou;
             _decou= decou;
             _pmaou= pmaou;
             _pmdou= pmdou;
        }
    }

}


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
