package edu.caltech.ipac.astro.conv;

public class Jcnvp2{

public static Jcnvp2Retval jcnvp2(int jsysin, double epokpa, 
       double distin, double pain,
       double epoki, double xlonin, double xlatin, double tobsin,
       int jsysou, double epoko, double paou, double xlonou, 
       double xlatou, int ierr)
//int jsysin,jsysou,*ierr;
//double epokpa,distin,pain,epoki,xlonin,xlatin,tobsin,
//       epoko,*paou,*xlonou,*xlatou;
{
/*
* comments updated 24aug92.
* j.bennett
* jcnvp2 version of jcnvpa that can handle julian prec., etc. - 
*      has tobsin arg. that jcnvpa does not have.
*      calls jcnvc2 instead of jcnvcd (still needs subs in jcnvcd though)
*
*
*       ALL FLOATING POINT ARGS ARE DOUBLE PRECISION
*
* jsysin is coord. system of input:
*          1 = equ(Bess); 2 = gal; 3 = ecl(Bess); 4 = sgl.
*          0 = equ(Jul); 11 = equ(Jul)=same as 0; 13 = ecl(Jul)
*                                    
* epokpa is equinox of pain (ignored if jsysin > 2 or 4)- if 0.0d0, then
*         routine sets epokpa = to equinox of input position.
* distin = semi=major axis of error ellipse (really used only as flag-
*        if distin <= 0.d0, then position angle comp. not done).
* pain = input position angle (0 to 180.0d0 deg.)
* epoki is equinox of xlonin,xlatin (ignored if jsysin = 2 or 4; If 0.0d0
*         1950.0d0 is assumed if jsysin 1 or 3;  2000.0d0 if jsysin
*         0, 11, or 13)
* xlonin,xlatin  coordinates of input position
* tobsin is observation epoch (needed for B1950 <==> J2000 system conversions)
*          if tobsin = 0.0d0, then 1950.0d0 is used.
* jsysou is coord system of output (see jsysin for values).
* epoko is equinox of output. Same default rules as for epoki, but is function
*       of jsysou.
* paou is output position angle in jsysou, equinox=epoko if
*         distin > 0.0d0  and ierr = 0 ( garbage otherwise).
* xlonou,xlatou is output position in jsysou, equinox=epoko.
* ierr = 0 for normal return. -1 if paou could not be computed when
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
      int iflg=0, ipf=0;
      double pokpa,pokin,pokou,tobs;
      double plonp0=0.0,platp0=0.0,plonp1=0.0,platp1=0.0,
	     plon0=0.0, plat0=0.0, plon1=0.0, plat1=0.0,dumd=0.0;
      double dist = 0.1;

      ierr = 0;

      tobs = tobsin;
/*
*                         get central position:
*                         note: for central pos., jcnvc2 takes care of
*                               non-stated (e.g. 0.0d0) values)
*
*/
      Jcnvc2.Jcnvc2Retval r1=Jcnvc2.jcnvc2(jsysin,epoki,xlonin,xlatin,
                             jsysou,epoko,xlonou,xlatou,tobs);
      xlonou= r1._xnew;
      xlatou= r1._ynew;

      if(distin <= 0.0)
      {
         paou = pain;
         return new Jcnvp2Retval(paou, xlonou, xlatou, ierr);
      }
/*
*
* Now take care of position angle conversions, if any:
*
*/
      pokin = epoki;
      if(jsysin == 2 || jsysin == 4) 
         pokin = 1950.0;
      else
      {
         if(pokin == 0.0) 
	 {
            if(jsysin == 0  || jsysin >= 11)
                pokin = 2000.0;
            else
                pokin = 1950.0;
         }
      }

      pokou = epoko;
      pokpa = epokpa;
      if(pokpa <= 0.0 || jsysin == 2 || jsysin == 4)
         pokpa = pokin;

      if(Math.abs(pokpa-pokin) > 0.5) 
      {
/* precess central pt to equinox of pos angle (implies equatorial)
*
*/
        Jcnvc2.Jcnvc2Retval ret= Jcnvc2.jcnvc2(jsysin,pokin,xlonin,xlatin,
                                        jsysin,pokpa,plonp0,platp0,tobs);
        plonp0= ret._xnew;
        platp0= ret._ynew;
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
      Jbposa.JbelptRetval r3= Jbposa.jbelpt(plonp0,platp0,dist,pain,
                                            plonp1,platp1,iflg);
      iflg=   r3._iflg;
      plonp1= r3._xlonp;
      platp1= r3._xlatp;

      if(iflg == 3) 
      {
         plonp0 = 0.0;
         if(platp0 < 0.0)
            platp0 = -90.0;
         else
            platp0 = 90.0;
      }

      if(iflg == 4)
      {
         plonp1 = 0.0;
         if(platp1 < 0.0)
            platp1 = -90.0;
         else
            platp1 = 90.0;
      }
/*
* precess / convert both points to output equinox:
*/
      Jcnvc2.Jcnvc2Retval r2;
      r2= Jcnvc2.jcnvc2(jsysin,pokpa,plonp0,platp0,
                        jsysou,pokou,plon0,plat0,tobs);
      plon0= r2._xnew;
      plat0= r2._ynew;

      r2= Jcnvc2.jcnvc2(jsysin,pokpa,plonp1,platp1,
                        jsysou,pokou,plon1,plat1,tobs);
      plon1= r2._xnew;
      plat1= r2._ynew;

/*
*
* get new pos angle
*
*/
      Jbposa.JbposaRetval r5=  Jbposa.jbposa(plon1,plat1,plon0,
                                             plat0,dumd,paou,ipf);
      dumd= r5._distd;
      paou= r5._pa;
      ipf=  r5._ipf;

      if(ipf == 0)
      {
        ierr = -1;
        paou = 0.0;
      }

      return new Jcnvp2Retval(paou, xlonou, xlatou, ierr);
}
    public static class Jcnvp2Retval { 
          public double _paou;
          public double _xlonou;
          public double _xlatou;
          public int    _ierr;
          public Jcnvp2Retval(double paou, double xlonou, 
                              double xlatou, int ierr) {
             _paou= paou;
             _xlonou= xlonou;
             _xlatou= xlatou;
             _ierr= ierr;
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
