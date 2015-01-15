/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

public class Gtjpa {


/*
* \ned\gtjpa.f  12-13mar92
*   03aug92; updated comments only.
*   23jul92 to call nwcprc/nwcprp instead of jprecj. Also, precesses
*     to 1950 (if necessary) before call to gtjulp or from 1950 (if
*     necessary) after call to unjulp (i.e. for propermotion types only).
*
*        contains gtjpa, unjpa, gtjpap, unjpap
*/

public static GtjpaRetval gtjpa (double eqxin, double ra, double dec, 
                                 double pab,   double tobsin, int ieflg, 
                                 double raout, double decout, double paj,
                                 int ierr)
//int ieflg, *ierr;
//double eqxin,ra,dec,pab,tobsin,*raout,*decout,*paj;
{
/*
*
*       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
*
* eqxin is the equinox(besselian) of the input position.
* ra,dec is input position in decimal degrees at equinox eqxin.
* pab  = input position angle (0 to 360.0d0 deg.) at eqxin.
* tobsin is year of observation (e.g. 1983.5 for iras sources)
*         if 0.0d0, then tobsin is treated as though equal to eqxin.
* ieflg  of -1 indicate to not remove E-terms from position in computing
*             position angle (any other value allows E-term removal)
* raout,decout is output position in decimal degrees at J2000.0
* paj is output position angle (0 to 360.0d0 deg.) for J2000.0
*         if ierr = 0 ( garbage otherwise).
* ierr = 0 for normal return. -1 if paj could not be computed.
*
* NOTE:
* This routine assumes input pos angle & input position are in the equatorial
*    coordinate system.
*
* calls jbposa,jbelpt,gtjul2
*
*/

   int    iflg=0,ipf=0;
   double plonp0=0.0,platp0=0.0,plonp1=0.0,platp1=0.0,plon0=0.0;
   double plat0=0.0,plon1=0.0,plat1=0.0,dumd=0.0;
   double pain=0.0,paou=0.0,eqx=0.0,tobs=0.0;
   double dist = 0.1;

   ierr = 0;
   eqx = eqxin; 
   if(eqxin == 0.0) eqx = 1950.0;
   tobs = tobsin;
   if(tobsin == 0.0) tobs = eqxin;

   plonp0 = ra;
   platp0 = dec;
   pain = pab;
   
   Gtjul2.RaDecRetval r34= Gtjul2.gtjul2(eqx, ra, dec, tobs, 
                                        ieflg, raout, decout);
   raout = r34._ra;
   decout= r34._dec;

/*
*
* find end point at equinox eqxin
* note: plonp0 and platp0 are used for pos angle proc. and may
*        be slightly adjusted here.  To get proper central position 
*        one must precess ra and dec.
*
*/
      plon0 = raout;
      plat0 = decout;

      Jbposa.JbelptRetval r1= Jbposa.jbelpt(plonp0,platp0,dist,pain,
                                            plonp1,platp1,iflg);
      plonp1= r1._xlonp;
      platp1= r1._xlatp;
      iflg  = r1._iflg;
      if(iflg == 3)
      {
/*          recompute plonp0,platp0 for pos angle processing if near a pole:
*/
         plonp0 = 0.0;
         if(platp0 < 0.0)
            platp0 = -90.0;
         else
            platp0 = 90.0;
         Gtjul2.RaDecRetval ret= Gtjul2.gtjul2(eqx,plonp0,platp0,tobs,
                                               ieflg,plon0,plat0);
         plon0= ret._ra;
         plat0= ret._dec;
      }

      if(iflg == 4)
      {
         plonp1 = 0.0;
         if(platp1 < 0.0)
            platp1 = -90.0;
         else
            platp1 = 90.0;
      }

/* precess pt at apr2ex of ellipse:*/
         
         Gtjul2.RaDecRetval r3= Gtjul2.gtjul2(eqx,plonp1,platp1,tobs,
                                              ieflg,plon1,plat1);
         plon1= r3._ra;
         plat1= r3._dec;
/*
*   get new pos angle
*/
   Jbposa.JbposaRetval r5= Jbposa.jbposa(plon1,plat1,plon0,plat0,
                                         dumd,paou,ipf);
   dumd= r5._distd;
   paou= r5._pa;
   ipf=  r5._ipf;
   if(ipf == 0) 
   {
      ierr = -1;
      paou = 0;
   }

   paj = paou;

   return new GtjpaRetval(ierr,raout,decout,paj);
}
    public static class GtjpaRetval{
        public int    _ierr;
        public double _raout;
        public double _decout;
        public double _paj;
        public GtjpaRetval(int ierr, double raout, double decout, double paj) {
             _ierr= ierr;
             _raout= raout;
             _decout= decout;
             _paj= paj;
        }
    }

public static UnjpaRetval unjpa ( double ra, double dec, double paj, 
                                  double tobsin, int ieflg, double eqxbou, 
                                  double raout, double decout, double pab,
                                  int    ierr)
//int ieflg, *ierr;
//double ra,dec,paj,tobsin,eqxbou,*raout,*decout,*pab;
{

/*
*
*       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
*
* ra,dec is input position in decimal degrees at J2000.0
* paj  = input position angle (0 to 360.0d0 deg.) at J2000.0
* tobsin is year of observation (e.g. 1983.5 for iras sources)
*         if 0.0d0, then tobsin is treated as though equal to eqxin.
* ieflg  of -1 indicates to not add back E-terms
*           +1  (any val other than -1) indicates E-terms are to be replaced.
* eqxbou is the equinox(besselian) of the output position & position angle.
* raout,decout is output position in decimal degrees at eqxbou.
* pab is output position angle (0 to 360.0d0 deg.) for eqxbou;
*         if ierr = 0 ( garbage otherwise).
* ierr = 0 for normal return. -1 if paou could not be computed.
*
* NOTE:
* This routine assumes input pos angle & input position are in the equatorial
*    coordinate system.
*
* calls jbposa,jbelpt,unjul2
*
*/

   int iflg=0, ipf=0;
   double plonp0=0.0,platp0=0.0,plonp1=0.0,platp1=0.0, plon0=0.0;
   double plat0=0.0, plon1=0.0, plat1=0.0,dumd=0.0;
   double dist = 0.1,pain=0.0,paou=0.0,eqx=0.0,tobs=0.0;

      ierr = 0;
      eqx = eqxbou;
      if(eqxbou == 0.0) eqx = 1950.0;
      tobs = tobsin;
      if(tobsin == 0.0) tobs = eqx;

      plonp0 = ra;
      platp0 = dec;
      pain = paj;
      
      Gtjul2.RaDecRetval r7 = Gtjul2.unjul2(ra, dec,tobs,ieflg,eqx,
                                            raout,decout);
      raout=  r7._ra;
      decout= r7._dec;
      plon0 = raout;
      plat0 = decout;


/* find end point at equinox J2000.0 */

      Jbposa.JbelptRetval r1= Jbposa.jbelpt(plonp0,platp0,dist,pain,
                                          plonp1,platp1,iflg);
      plonp1= r1._xlonp;
      platp1= r1._xlatp;
      iflg  = r1._iflg;
      if(iflg == 3)
      {
/*    recompute plonp0,platp0 for pos angle processing if near a pole: */
         plonp0 = 0.0;
         if(platp0 < 0.0)
            platp0 = -90.0;
         else
            platp0 = 90.0;
         
         Gtjul2.RaDecRetval r55 = Gtjul2.unjul2(plonp0,platp0,tobs,ieflg,
                                               eqx,plon0,plat0);
         plon0= r55._ra;
         plat0= r55._dec;
      }
      if(iflg == 4)
      {
         plonp1 = 0.0;
         if(platp1 < 0.0) 
            platp1 = -90.0;
          else
            platp1 = 90.0;
      }

       
      Gtjul2.RaDecRetval r8 = Gtjul2.unjul2(plonp1,platp1,tobs,ieflg,
                                            eqx,plon1,plat1);
       plon1= r8._ra;
       plat1= r8._dec;

/* get new pos angle */

      Jbposa.JbposaRetval r5= Jbposa.jbposa(plon1,plat1,plon0,plat0,
                                            dumd,paou,ipf);
      dumd= r5._distd;
      paou= r5._pa;
      ipf = r5._ipf;
      if(ipf == 0)
      {
         ierr = -1;
         paou = 0;
      }
      pab = paou;

      return new UnjpaRetval(ierr,raout,decout,pab);
}
    public static class UnjpaRetval {
        public int    _ierr;
        public double _raout;
        public double _decout;
        public double _pab;
        public UnjpaRetval(int ierr, double raout, double decout, double pab) {
             _ierr= ierr;
             _raout= raout;
             _decout= decout;
             _pab= pab;
        }
    }

public static GtjpapRetval gtjpap( double eqxin, double ra, double dec, 
                                   double pab, double pma, double pmd, 
                                   double pin, double vin, int ieflg,
                                   double raout, double decout, double paj, 
                                   double pmaout, double pmdout, int ierr)
//int ieflg, *ierr;
//double eqxin,ra,dec,pab,pma,pmd,pin,vin,
//       *raout,*decout,*paj,*pmaout,*pmdout;
{
/*
*
*       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
*
* eqxin is the equinox(besselian) of the input position & position angle.
* ra,dec is input position in decimal degrees at equinox eqxin.
* pab  = input position angle (0 to 360.0d0 deg.) at eqxin.
* pma is proper motion in ra in seconds per tropical century.
* pmd is proper motion in dec in seconds of arc per tropical century.
* pin is parallax in arc seconds.
* vin is radial velocity in km/sec
* ieflg  of -1 indicate to not remove E-terms from position in computing
*             position angle (any other value allows E-term removal)
* raout,decout is output position in decimal degrees at J2000.0
* paj is output position angle (0 to 360.0d0 deg.) for J2000.0
*         if ierr = 0 ( garbage otherwise).
* pmaout is proper motion in seconds of time per Julian century.
* pmdout is proper motion in seconds of arc per Julian century.
* ierr = 0 for normal return. -1 if paj could not be computed.
*
* NOTE:
* This routine assumes input pos angle & input position are in the equatorial
*    coordinate system.
*
* calls jbposa,jbelpt,gtjulp,gtjul2
*
*/
      int    iflg=0,ipf=0;
      double plonp0=0.0,platp0=0.0,plonp1=0.0,platp1=0.0,plon0=0.0;
      double plat0=0.0, plon1=0.0, plat1=0.0, dumd=0.0;
      double ra50=0.0,dec50=0.0,pma50=0.0,pmd50=0.0;
      double dist=0.1,pain=0.0,paou=0.0,eqx=0.0,tobs=0.0;

      ierr = 0;
      eqx = eqxin; 
      if(eqxin == 0.0) eqx = 1950.0;
      tobs = eqx;

      plonp0 = ra;
      platp0 = dec;
      pain = pab;

/* get central position (w/ proper motions) at J2000.0: */

      if(eqx == 1950.0)
      {
         ra50  = ra;
         dec50 = dec;
         pma50 = pma;
         pmd50 = pmd;
      }
      else {
         Nwcprc.NwcprpRetval r20= Nwcprc.nwcprp(eqx,ra,dec,pma,pmd,pin,
                                                vin,1950.0,
                                                ra50,dec50,pma50,pmd50);
         ra50 = r20._raou;
         dec50= r20._decou;
         pma50= r20._pmaou;
         pmd50= r20._pmdou;
      }
      Gtjul2.RaDecPMRetval r54= Gtjul2.gtjulp(1950.0,ra50,dec50,pma50,pmd50,
                                              pin,vin,ieflg,
                                              raout,decout,pmaout,pmdout);
      raout=  r54._ra; 
      decout= r54._dec; 
      pmaout= r54._pmra;
      pmdout= r54._pmdec;
/*
* find end point at equinox eqxin - ignore proper motion for pos.ang. calc.
*  note: plonp0 and platp0 are used for pos angle proc. and may
*        be slightly adjusted here. 
*
*/
      Jbposa.JbelptRetval r1= Jbposa.jbelpt(plonp0,platp0,dist,pain,
                                          plonp1,platp1,iflg);
      plonp1= r1._xlonp;
      platp1= r1._xlatp;
      iflg  = r1._iflg;
      if(iflg == 3)
      {
/*       recompute plonp0,platp0 for pos angle processing if near a pole: */
         plonp0 = 0.0;
         if(platp0 < 0.0) 
            platp0 = -90.0;
         else
            platp0 = 90.0;
      }

      Gtjul2.RaDecRetval r3= Gtjul2.gtjul2(eqx,plonp0,platp0,tobs,
                                           ieflg,plon0,plat0);
      plon0= r3._ra;
      plat0= r3._dec;

      if(iflg == 4)
      {
         plonp1 = 0.0;
         if(platp1 < 0.0)
            platp1 = -90.0;
         else
            platp1 = 90.0;
      }

/* precess pt at apex of ellipse: */
      Gtjul2.RaDecRetval r4= Gtjul2.gtjul2(eqx,plonp1,platp1,tobs,
                                               ieflg,plon1,plat1);
      plon1= r4._ra;
      plat1= r4._dec;
/*
* get new pos angle
*/
      Jbposa.JbposaRetval r5= Jbposa.jbposa(plon1,plat1,plon0,plat0,
                                         dumd,paou,ipf);
      dumd= r5._distd;
      paou= r5._pa;
      ipf = r5._ipf;
      if(ipf == 0) 
      {
        ierr = -1;
        paou = 0;
      }
      paj = paou;

      return new GtjpapRetval(ierr,raout,decout,paj,pmaout,pmdout);
}
    public static class GtjpapRetval {
        public int    _ierr;
        public double _raout;
        public double _decout;
        public double _paj;
        public double _pmaout;
        public double _pmdout;
        public GtjpapRetval(int ierr, double raout, double decout, double paj,
                            double pmaout, double pmdout) {
             _ierr= ierr;
             _raout= raout;
             _decout= decout;
             _paj= paj;
             _pmaout= pmaout;
             _pmdout= pmdout;
        }
    }

public static UnjpapRetval unjpap( double ra, double dec, double paj, 
                                   double pma, double pmd, double pin, 
                                   double vin, int ieflg, double eqxbou,
                                   double raout, double decout, double pab, 
                                   double pmaout, double pmdout, int ierr)
//int ieflg,*ierr;
//double ra,dec,paj,pma,pmd,pin,vin,eqxbou,
//       *raout,*decout,*pab,*pmaout,*pmdout;
{
/*
*       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION 
*
* ra,dec is input position in decimal degrees at J2000.0
* paj  = input position angle (0 to 360.0d0 deg.) at J2000.0
* pma is proper motion in ra in seconds per Julian century.
* pmd is proper motion in dec in seconds of arc per Julian century.
* pin is parallax in arc seconds.
* vin is radial velocity in km/sec
* ieflg  of -1 indicates to not add back E-terms
*           +1  (any val other than -1) indicates E-terms are to be replaced.
* eqxbou is the equinox(besselian) of the output position & position angle.
* raout,decout is output position in decimal degrees at eqxbou.
* pab is output position angle (0 to 360.0d0 deg.) for eqxbou;
*         if ierr = 0 ( garbage otherwise).
* pmaout is proper motion in seconds of time per tropical century.
* pmdout is proper motion in seconds of arc per tropical century.
* ierr = 0 for normal return. -1 if paou could not be computed.
*
* NOTE:
* This routine assumes input pos angle & input position are in the equatorial
*    coordinate system.
*
* calls jbposa,jbelpt,unjulp,unjul2
*
*/
      int iflg=0, ipf=0;
      double plonp0=0.0,platp0=0.0,plonp1=0.0,platp1=0.0, 
             plon0=0.0, plat0=0.0, plon1=0.0, plat1=0.0, dumd=0.0;
      double ra50=0.0,dec50=0.0,pma50=0.0,pmd50=0.0;
      double dist=0.1,pain=0.0,paou=0.0,eqx=0.0,tobs=0.0;

      ierr = 0;
      eqx = eqxbou;
      if(eqxbou == 0.0) eqx = 1950.0;
      tobs = eqx;

      plonp0 = ra;
      platp0 = dec;
      pain = paj;
      Gtjul2.RaDecPMRetval r31= Gtjul2.unjulp( ra, dec,pma,pmd,pin,vin,ieflg,
                                              eqx,ra50,dec50,pma50,pmd50);
      ra50 = r31._ra;
      dec50= r31._dec;
      pma50= r31._pmra;
      pmd50= r31._pmdec;
      if(eqx == 1950.0)
      {
         raout  = ra50;
         decout = dec50;
         pmaout = pma50;
         pmdout = pmd50;
      }
      else {
         Nwcprc.NwcprpRetval r21=Nwcprc.nwcprp(1950.0,ra50,dec50,
                                         pma50,pmd50,pin,vin,eqx,
		                         raout,decout,pmaout,pmdout);
         raout = r21._raou;
         decout= r21._decou;
         pmaout= r21._pmaou;
         pmdout= r21._pmdou;
      }
/*
* find end point at equinox J2000.0
*/
      Jbposa.JbelptRetval r32= Jbposa.jbelpt(plonp0,platp0,dist,pain,
                                            plonp1,platp1,iflg);
      plonp1= r32._xlonp;
      platp1= r32._xlatp;
      iflg  = r32._iflg;
      if(iflg == 3)
      {
/*       recompute plonp0,platp0 for pos angle processing if near a pole: */
         plonp0 = 0.0;
         if(platp0 < 0.0)
            platp0 = -90.0;
         else
            platp0 = 90.0;
      }
      
      Gtjul2.RaDecRetval r9 = Gtjul2.unjul2(plonp0,platp0,tobs,ieflg,
                                            eqx,plon0,plat0);
      plon0= r9._ra;
      plat0= r9._dec;

      if(iflg == 4)
      {
         plonp1 = 0.0;
         if(platp1 < 0.0) 
            platp1 = -90.0;
         else
            platp1 = 90.0;
      }

      Gtjul2.RaDecRetval r6 = Gtjul2.unjul2(plonp1,platp1,tobs,ieflg,eqx,
                                           plon1,plat1);
      plon1= r6._ra;
      plat1= r6._dec;

/* get new pos angle
*
*/
      Jbposa.JbposaRetval r5= Jbposa.jbposa(plon1,plat1,plon0,plat0,
                                            dumd,paou,ipf);
      dumd= r5._distd;
      paou= r5._pa;
      ipf = r5._ipf;
      if(ipf == 0)
      {
        ierr = -1;
        paou = 0;
      }
      pab = paou;

      return new UnjpapRetval(ierr,raout,decout,pab,pmaout,pmdout);
}
    public static class UnjpapRetval {
        public int    _ierr;
        public double _raout;
        public double _decout;
        public double _pab;
        public double _pmaout;
        public double _pmdout;
        public UnjpapRetval(int ierr, double raout, double decout, double pab,
                            double pmaout, double pmdout ) {
             _ierr= ierr;
             _raout= raout;
             _decout= decout;
             _pab= pab;
             _pmaout= pmaout;
             _pmdout= pmdout;
        }
    }
}


