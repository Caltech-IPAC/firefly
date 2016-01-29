/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

public class Gtjul2 {


public static RaDecRetval gtjul2( double eqxin, double ra, double dec, 
                                  double tobsin, int ieflg, double raout, 
                                  double decout)
//int ieflg;
//double eqxin,ra,dec,tobsin,*raout,*decout;
{
/*
* 31jul92 - changes to jrmvet,jaddet; substituted more general purpose 
*     subs jgtobq,ecleqx,equec2 for limited- purpose subs gtoblq,ecleq2,equec2
* 23jul92 to call nwcprc/nwcprp instead of jprecj.
* 02jul92 - re; routines with propermotions:
*    gtjulp will assume eqx,epoch B1950 in and yield eqx,epoch J2000 out.
*    unjulp will assume eqx,epoch J2000 in and yield eqx,epoch B1950 out.
* 30jun92 to apply fk5-fk4 sys. corr. to eqx B1950 position before any other
*   corrections.
* 05,06mar92 to use astron. suppl 1984 & 1961 e-term removal equations
*    (in subs gtjul2,gtjulp,unjul,unjulp)
* 13-15jan92 updated to reflect method described in FK5 Part I. Catalog
* author:   judy bennett
*
* to precess old besselian equinox positions to equator and equinox of 
*   J2000.0.
*
*   Calls subroutines nwcprc ("old" Newcomb constants) and fk5prc and fk5prp
*      ("new" Julian precession formulas).
*
*  Source of method:
*    Fifth Fundamental Catalogue (FK5) Part I (1988, but not avail. till 1992)
*     Section 8. (page 9)  
*     Summary of order:
*  1.  Precess each object (using precessional values adopted in that cat-
*        i.e. usually Newcomb) from the catalogue equinox to the mean equinox
*        at the object's mean epoch (i.e. observation date).
*  2.  The mean positions in all star catalogues published prior to 1984
*        contain the terms of elliptic aberration (E-terms). These terms need
*        to be removed from the star's mean position.
*  3.  The equinox correction and the systematic correction FK5-FK4 
*       (given in the FK5 cat), computed for the object's mean epoch, need to
*       be applied. Note: program applies fk5-fk4 sys.corr. at B1950 which
*       seems consistent with procedure actually described in the FK5.
*  4.  The corrected position has to be precessed to the new standard equinox
*       J2000.0 using the precessional quantities as adopted in the IAU(1976)
*       System of Astronomical Constants (also see Astronomical Almanac 1984,
*       pages S34,S36)
*
*  NOTE: these routines apply the fk5-fk4 correction at B1950.
*        if no fk5-fk4 systematic correction required, the user should
*        include the following statements in his program:
*  (a value of 0 for japply specifies no corrections; anything else uses corr.)
*        external jfk5bd
*        common /fkappl/ japply
*        japply = 0
*
* ****************************************************************************
*   All arguments are double precision, except ieflg which is integer.
*  Input values:
* eqxin is the equinox of the input position (if negative, absolute val.used)
* ra    is right ascension (decimal degrees) at eqx
* dec   is declination (decimal degrees) at eqx
* tobsin is the year of observation (i.e. when the object was observed at 
*         this position (e.g. 1983.5d0) ; if 0.0d0, value of eqxin is used.
* ieflg is flag allowing removal of E-terms of aberration if any (usually
*        they are present, therefore ieflg should = +1):
*         ieflg = -1 do not remove E-terms (there none).
*         ieflg = any value except -1 indicates  E-terms are present and
*                 are to be removed.
*  Returned values:
* raout  is right ascension at equator and equinox of J2000.0
* decout is declination at equator and equinox of J2000.0
*
* ****************************************************************************
*
* Files required: gtjul2.f (includes gtjul2,gtjulp,unjul2,unjulp,gtecle,
*                           gtetrm,itere,iterec,jaddet,jclpos,jrmvet)
*                 ecleqx.f (includes ecleqx,equecx,jgtobq)
*                 fk5prc.f (includes fk5prc,fk5prp)
*                 nwcprc.f (includes nwcprc,nwcprp)
*                 jgtfkc.f (includes jgtfkc,junfkc,jnitfk.jfk5bd,dintrp,dintr2)
*
* ****************************************************************************
*/
      double tobs=0.0, tobsj=0.0, jde=0.0, eqx=0.0, rat50=0.0, dect50=0.0;
      double rat=0.0, dect=0.0, delt=0.0, dela=0.0;
      double corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;

      eqx  = Math.abs(eqxin);
      tobs = Math.abs(tobsin);
      if(tobs == 0.0) tobs = eqx;

      if(Jgtfkc.japply == 0) 
      {
         rat50 = ra;
         dect50 = dec;
      }
      else
      {
/*   determine fk5-fk4 systematic correction using eqx B1950 postion */

         if(eqx != 1950.0) {
             Nwcprc.NwcprcRetval ret= Nwcprc.nwcprc(eqx,ra,dec,1950.0,
                                                    rat50,dect50);
             rat50 = ret._raou;
             dect50= ret._decou;
         }
         else
         {
            rat50 = ra;
            dect50 = dec;
         }
         Jgtfkc.JgtfkcRetval r1= Jgtfkc.jgtfkc(rat50,dect50,0.0,tobs,
                                         corra,corrd,corrpa,corrpd);
         corra=  r1._corra;
         corrd=  r1._corrd;
         corrpa= r1._corrpa;
         corrpd= r1._corrpd;
         rat50 = rat50 + corra;
         dect50 = dect50 + corrd;
         Gtjul2.RaDecRetval r3= Gtjul2.jclpos(rat50,dect50);
         rat50 = r3._ra;
         dect50= r3._dec;
         eqx = 1950.0;
      }

      if (tobs != eqx) {
/*                     use old newcomb formula to prec. to tobs */
          
             Nwcprc.NwcprcRetval ret= Nwcprc.nwcprc( eqx, rat50, dect50, 
                                                     tobs, rat, dect);
             rat = ret._raou;
             dect= ret._decou;
      }
      else
      {
          rat  = rat50;
          dect = dect50;
      }
/*
* compute the correction for right ascension at the mean epoch of observations.
*   note: this correction will generally be of order +0.06s for most modern 
*         catalogs.
* delt is fraction of Julian century; adjust tobs in besselian time frame
*   to be correct
*   in terms of Julian. use jde of b1950 and tropical year in days
*   to get Julian day of observations. Then compute equivalent year for 
*   Julian prec. prog. so fraction of Julian year will be correct.
*   B1950 = JDE 2433282.4235  365.2421988 tropical days per year.
*   J2000 = JDE 2451545.00    365.25 Julian days per year. 
*
*/
      jde = ((tobs - 1950.0) * 365.2421988) + 2433282.4235;     
      tobsj = 2000.0 + ((jde - 2451545.0)/365.25);
/*
* Remove E-terms if necessary:
*/
      if(ieflg != -1) {
          RaDecRetval ret= jrmvet(tobsj,rat,dect);
          rat=  ret._ra;
          dect= ret._dec;
       }
/*
* Apply the equinox correction (use tobs not tobsj):
*
*/
      delt = (tobs - 1950.0) * 0.01;
      dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

      rat = rat + dela;
      Gtjul2.RaDecRetval jcl1= Gtjul2.jclpos(rat,dect);
      rat= jcl1._ra;
      dect= jcl1._dec;
      
      
/*
* now compute mean place of source at 2000 Jan. 1.5 using the mean place 
* at the mean epoch of observations tobs (with dela applied) and the
* new precession formulas (i.e. Julian prec. formulas)
*
*/
      Fk5prc.Fk5prcRetval fk5prcRet= Fk5prc.fk5prc( tobsj, rat, dect, 
                                                  2000.0, raout, decout);
      raout=  fk5prcRet._raou;
      decout= fk5prcRet._decou;
     

      return new RaDecRetval(raout,decout);
}

public static RaDecRetval unjul2(double rain, double decin, double tobsin, 
                                 int ieflg, double eqxbou, 
                                 double raout, double decout)
//int ieflg;
//double rain,decin,tobsin,eqxbou,*raout,*decout;
{
/*
* author: judy bennett
* 01jul92
* 26jun92 to allow removal of fk5-fk4 systematic corrections
* 17jan92
* 05feb92 to use fk5prc instead of hcprec.
*
* unjul2 reverses what gtjul2  did - i.e. unjul2 precesses accurately
*    back to the original besselian input.
*
* to precess new Julian  equinox positions (J2000.0)back to input besselian
*   equinox positions (B1950.0).
*
*   Calls subroutines nwcprc ("old" Newcomb constants) and fk5prc ("new"
*      Julian prec.formulas).
*
*  Source of method:
*   See comments in gtjul2.
*
* ****************************************************************************
*   All arguments are double precision, except ieflg which is integer.
*  Input values:
* rain is right ascension (decimal degrees) at J2000.0
* decin is declination (decimal degrees) at J2000.0
* tobsin is the year of observation (i.e. when the object was assigned this pos)
*         if 0.0d0, eqxbou is used.
* ieflg is flag allowing restore of E-terms of aberration if any (usually
*        they are present; ieflg should usually = + 1).
*         ieflg =-1 do not replace E-terms (there none).
*         ieflg = anything but -1 indicates  E-terms are to be present
*             and are to be replaced. Usually they are to be replaced.
* eqxbou is output equinox (besselian): if 0.0d0, 1950.0d0 is used.
*  Returned values:
* raout  is right ascension at besselian equinox of eqxbou.
* decout is declination at besselian equinox of eqxbou.
*
* ****************************************************************************
*/

      double ra=0.0, dec=0.0;
      double tobsj=0.0, jde=0.0, tobsb=0.0;
      double rat=0.0, dect=0.0, delt=0.0, dela=0.0;
      double eqx1=2000.0,eqx2=0.0,rat50=0.0,dect50=0.0;
      double corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;

      if(eqxbou != 0.0) 
         eqx2 = Math.abs(eqxbou);
      else
         eqx2 = 1950.0;

      if(tobsin != 0.0) 
         tobsb = Math.abs(tobsin);
      else
         tobsb = eqx2;
      ra  = rain;
      dec = decin;
/*
* tobsj is year in terms of Julian years; tobsb is year in terms of tropical.
*
*/
       jde = ((tobsb - 1950.0) * 365.2421988) + 2433282.4235;     
       tobsj = 2000.0 + ((jde - 2451545.0)/365.25);

       Fk5prc.Fk5prcRetval r0= Fk5prc.fk5prc( eqx1, ra, dec, tobsj, rat, dect);
       rat = r0._raou;
       dect= r0._decou;
       
/*
* remove the equinox correction (use tobsb not tobsj):
*
*/
      delt = (tobsb - 1950.0) * 0.01;
      dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

       rat = rat - dela;
       if(rat >= 360.0) rat = rat - 360.0;
       if(rat <   0.0)  rat = rat + 360.0;
/*
* Add back  E-terms if necessary:
*/
      if(ieflg != -1)  {
            RaDecRetval ret= jaddet(tobsj,rat,dect);
            rat = ret._ra;
            dect= ret._dec;
      }

      if(Jgtfkc.japply == 0) 
      {
         if(tobsb != eqx2)  {
             Nwcprc.NwcprcRetval ret= Nwcprc.nwcprc( tobsb,rat, dect, 
                                               eqx2, raout, decout);
             raout = ret._raou;
             decout= ret._decou;
          }
         else
	 {
            raout = rat;
            decout = dect;
	 }     
      }
      else
      {
/*                find and remove fk5-fk4 systematic corrections: */
          if(tobsb == 1950.0) 
	  {
             rat50 = rat;
             dect50 = dect;
	  }
          else {
             Nwcprc.NwcprcRetval ret= Nwcprc.nwcprc(tobsb,rat,dect,1950.0,
                                                    rat50,dect50);
             rat50 = ret._raou;
             dect50= ret._decou;
          }

          Jgtfkc.JgtfkcRetval r12= Jgtfkc.junfkc(rat50,dect50,0.0,tobsb,
                                        corra,corrd,corrpa,corrpd);
          corra=  r12._corra;
          corrd=  r12._corrd;
          corrpa= r12._corrpa;
          corrpd= r12._corrpd;

          rat50 = rat50 - corra;
          dect50 = dect50 - corrd;
          Gtjul2.RaDecRetval r5= Gtjul2.jclpos(rat50,dect50);
          rat50=  r5._ra;
          dect50= r5._dec;
          if(eqx2 != 1950.0) {
             Nwcprc.NwcprcRetval ret= Nwcprc.nwcprc(1950.0,rat50,dect50,
                                                      eqx2,raout,decout);
             raout = ret._raou;
             decout= ret._decou;
          }
          else
	  {
             raout = rat50;
             decout = dect50;
	  }
      }

      return new RaDecRetval(raout,decout);
}

public static RaDecPMRetval gtjulp( double eqxin, double rain,  double decin, 
                                    double pmain, double pmdin, double pin, 
                                    double vin,   int ieflg,    double raout, 
                                    double decout,double pmaout,double pmdout)
//int ieflg;
//double eqxin,rain,decin,pmain,pmdin,pin,vin,
//       *raout,*decout,*pmaout,*pmdout;
{
/*
* 27jul92 to allow pmain,pmdin =0.0d0 to be processed as any other
*         source with given proper motions (User must use gtjul2
*         if proper motions are "intrinsically" zero (e.g. radio source)
*         or if no proper motions are given for the object).
* 02jul92 to apply fk5-fk4 systematic corrections at B1950.
*  eqxin will be ignored; jgtjulp assumes equinox,epoch B1950 for inputs.
* 24jun92 to apply fk5-fk4 systematic corrections if desired.
* 05feb92 like gtjul2, except for objects with proper motions etc.
*
* author: judy bennett
*
* to precess old besselian equinox positions to equator, equinox and epoch
*   of J2000.0.
*
*  Source of method:
*    Fifth Fundamental Catalogue (FK5) Part I (1988, but not avail. till 1992)
*     Section 8. (page 9) + discussion in FK5
*
* ****************************************************************************
*   All arguments are double precision, except ieflg which is integer.
*  Input values:
* eqxin is ignored (assumes 1950.0d0);
*       is the equinox & epoch of input position 
* rain  is right ascension (decimal degrees) at equinox & epoch of B1950 
* decin is declination (decimal degrees) at equinox & epoxh of B1950
* pmain is proper motion in ra in seconds of time per tropical century
* pmdin is proper motion in dec in seconds of arc per tropical century
* pin is parallax in arc seconds.
* vin is radial velocity in km/sec
* ieflg is flag allowing removal of E-terms of aberration if any (usually
*        E-terms are present; therefore, ieflg should = +1)
*         ieflg = -1 do not remove E-terms (there none).
*         ieflg = anything except -1 indicates  E-terms are present and
*                 are to be removed.
*  Returned values:
* raout  is right ascension at epoch, equator and equinox of J2000.0
* decout is declination at epoch, equator and equinox of J2000.0
* pmaout is proper motion in seconds of time per Julian century.
* pmdout is proper motion in seconds of arc per Julian century.
*
* ****************************************************************************
*
*/

      double pma=0.0, pmd=0.0, corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;
      double tobs=1950.0, tobsj=0.0, jde=0.0, eqx = 1950.0;
      double rar=0.0,decr=0.0, work=0.0;
      double rat=0.0, dect=0.0, delt=0.0, dela=0.0;
      double pmat= 0.0, pmdt=0.0, dtor=0.0;

      dtor =  Math.atan(1.0) / 45.0;

      if(Jgtfkc.japply == 0)
      {
         rat = rain;
         dect = decin;
         pma = pmain;
         pmd = pmdin;
      }
      else
/*          determine fk5-fk4 correction & apply */
      {
         rat = rain;
         dect = decin;
         Jgtfkc.JgtfkcRetval r2= Jgtfkc.jgtfkc(rat,dect,0.0,tobs,
                                    corra,corrd,corrpa,corrpd);
         corra=  r2._corra;
         corrd=  r2._corrd;
         corrpa= r2._corrpa;
         corrpd= r2._corrpd;
         
         rat = rat + corra;
         dect = dect + corrd;
         Gtjul2.RaDecRetval r1= Gtjul2.jclpos(rat,dect);
         rat= r1._ra;
         dect=r1._dec;
         pma = pmain + corrpa;
         pmd = pmdin + corrpd;
      }
/*
* compute the correction for right ascension at the mean epoch of observations.
*   note: this correction will generally be of order +0.06s for most modern 
*         catalogs.
* delt is fraction of Julian century; adjust tobs in besselian time frame
*   to be correct
*   in terms of Julian. use jde of b1950 and tropical year in days
*   to get Julian day of observations. Then compute equivalent year for 
*   Julian prec. prog. so fraction of Julian year will be correct.
*   B1950 = JDE 2433282.4235  365.2421988 tropical days per year.
*   J2000 = JDE 2451545.00    365.25 Julian days per year. 
*
*/
      jde = ((tobs - 1950.0) * 365.2421988) + 2433282.4235;    
      tobsj = 2000.0 + ((jde - 2451545.0)/365.25);
/*
*
*  Remove E-terms if necessary:
*/
      if(ieflg != -1) {
         RaDecRetval ret= jrmvet(tobsj,rat,dect);
         rat = ret._ra;
         dect= ret._dec;
      }
/*
* Apply the equinox correction (use tobs not tobsj):
*      equinox correction from fk5 cat (Section 3 (page 6).
*/
      delt = (tobs - 1950.0) * 0.01;
      dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

      rat = rat + dela;
      Gtjul2.RaDecRetval r2= Gtjul2.jclpos(rat,dect);
      rat= r2._ra;
      dect=r2._dec;
/*
* convert proper motions from units per tropical century to units per Julian
*   century per p.S35 of Supp. to Astron.Alman.
* apply time-dependent portion of eqx corr to ra proper motion (0.0850 secs of
*   time per Julian century) per FK5 Catalog (Section 3 (page 6)).
* also adjust for change in precession constant per p. S35 of Supp. to
*   Astron. Alman. (note; misprint in Supp.: 0.6912 should be 0.06912)
*
*/
       rar = rat*dtor;
       decr = dect*dtor;
       if(Math.abs(dect) > 89.9999) 
          work = 0.0;
       else
          work = 0.0291*Math.sin(rar)*Math.tan(decr); 
       pmat = (pma * 1.00002136) - 0.06912 - work + 0.0850;
       pmdt = (pmd * 1.00002136) - 0.436*Math.cos(rar);
/*
* now compute mean place of source at 2000 Jan. 1.5 using the mean place 
* at at the mean epoch of observations tobs (with dela applied) and the
* new precession formulas (i.e. Julian prec. formulas)
*
*
*/
      Fk5prc.Fk5prpRetval r4= Fk5prc.fk5prp( tobsj, rat, dect, pmat, pmdt, 
                                 pin, vin, 2000.0, raout, decout, 
                                 pmaout, pmdout);
      raout=  r4._raou;
      decout= r4._decou;
      pmaout= r4._pmaou;
      pmdout= r4._pmdou;

      return new RaDecPMRetval(raout,decout,pmaout,pmdout);
}

public static RaDecPMRetval unjulp(double rain,  double decin, double pmain, 
                                   double pmdin, double pin,   double vin, 
                                   int ieflg,    double eqxbou,double raout, 
                                   double decout,double pmaout,double pmdout)
//int ieflg;
//double rain,decin,pmain,pmdin,pin,vin,
//       eqxbou,*raout,*decout,*pmaout,*pmdout;
{
/*
* 27jul92 to allow pmain,pmdin =0.0d0 to be processed as any other
*         source with given proper motions (User must use unjul2
*         if proper motions are "intrinsically" zero (e.g. radio source)
*         or if no proper motions are given for the object).
* 02jul92-eqxbou is ignored (assumed to be equinox,epoch B1950.0d0 for outputs)
* 26jun92 to allow removal for fk5-fk4 correction.
* author:  judy bennett
*
* unjulp reverses what gtjulp  did - i.e. unjulp precesses accurately
*    back to the original besselian input ( B1950).
*
* to precess new Julian  equinox positions (J2000.0)back to input besselian
*   equinox positions (B1950.0).
*
*  Source of method:
*   See comments in gtjul2.
*
* ****************************************************************************
*   All arguments are double precision, except ieflg which is integer.
*  Input values:
* rain is right ascension (decimal degrees) at epoch & equinox of J2000.0
* decin is declination (decimal degrees) at epoch & equinox of J2000.0
* pmain is proper motion in ra in seconds of time per Julian century.
* pmdin is proper motion in dec in seconds of arc per Julian century.
* pin is parallax in arc seconds.   (0.0d0 if unknown)
* vin is radial velocity in km/sec. (0.0d0 if unknown)
* ieflg is flag allowing restore of E-terms of aberration. Usually ieflg
*   should = +1 (to restore the E-terms to the B1950 position).
*         ieflg =-1 do not replace E-terms (there none).
*         ieflg = anything but -1 indicates  E-terms are to be present
*             and are to be replaced.
* eqxbou is ignored (assumed to be 1950.0d0);
*        is output equinox (besselian):
*  Returned values:
* raout  is right ascension (decimal degrees) at equinox,epoch B1950.
* decout is declination (decimal degrees) at equinox,epoch B1950.
* pmaout  is proper motion in ra in seconds of time per tropical century.
* pmdout  is proper motion in dec in seconds of arc per tropical century.
*
* ****************************************************************************
*
*/

      double ra=0.0, dec=0.0, tobsb=1950.0;
      double rat=0.0, dect=0.0, delt=0.0, dela=0.0, pmat=0.0;
      double pmdt=0.0, rar=0.0, decr=0.0;
      double eqx1 = 2000.0, eqx2 = 1950.0, dtor=0.0;
      double corra=0.0, corrd=0.0, corrpa=0.0, corrpd=0.0;
      double jde=0.0, tobsj=0.0, work=0.0;

      dtor = Math.atan(1.0)/45.0;

      ra = rain;
      dec = decin;

       jde = ((tobsb - 1950.0) * 365.2421988) + 2433282.4235;     
       tobsj = 2000.0 + ((jde - 2451545.0)/365.25);

       Fk5prc.Fk5prpRetval r1=Fk5prc.fk5prp( eqx1, ra, dec, pmain, 
                                             pmdin, pin, vin,
                                             tobsj, rat, dect, pmat, pmdt);
       rat=  r1._raou;
       dect= r1._decou;
       pmat= r1._pmaou;
       pmdt= r1._pmdou;
/*
* re: proper motions:  remove adjustment for precession constant;
*     remove equinox corr. for proper motion in ra;
*     convert from units per Julian centry to units per tropical century:
*/
       rar  = dtor*rat;
       decr = dtor*dect;
       if(Math.abs(dect) > 89.9999) {
          work = 0.0;
       }
       else {
          work = 0.0291*Math.sin(rar)*Math.tan(decr);
       }
       pmat = pmat + 0.06912 + work - 0.0850;
       pmaout = pmat / 1.00002136;
       pmdt = pmdt + 0.436 * Math.cos(rar);
       pmdout = pmdt / 1.00002136;
/*
* remove the equinox correction (use tobsb not tobsj):
*
*/
      delt = (tobsb - 1950.0) * 0.01;
      dela = ((0.035 + 0.085*delt) * 15.0) / 3600.0;

      rat = rat - dela;
      if(rat >= 360.0)
         rat = rat - 360.0;
      else if(rat < 0.0) 
         rat = rat + 360.0;
/*
* Add back  E-terms if necessary:
*/
      if(ieflg != -1) {
          RaDecRetval ret= jaddet(tobsj,rat,dect);
          rat=  ret._ra;
          dect= ret._dec;
      }

      if(Jgtfkc.japply != 0)
      {
/*           remove the fk5-fk4 systematic corrections: */
          
          Jgtfkc.JgtfkcRetval r14= Jgtfkc.junfkc(rat,dect,0.0,tobsb,
                                     corra,corrd,corrpa,corrpd);
          corra=  r14._corra;
          corrd=  r14._corrd;
          corrpa= r14._corrpa;
          corrpd= r14._corrpd;
          rat = rat - corra;
          dect = dect - corrd;
          pmaout = pmaout - corrpa;
          pmdout = pmdout - corrpd;
          RaDecRetval ret= jclpos(rat,dect);
          rat= ret._ra;
          dect=ret._dec;
      }

      raout = rat;
      decout = dect;

      return new RaDecPMRetval(raout,decout,pmaout,pmdout);
}

public static RaDecRetval itere(double ra,   double dec,
                                double edela,double edeld)
//double ra, dec, *edela, *edeld;
{
/* for adding E-term back (note: w/ supp. formulas, edela,edeld are added to
*   ra and dec to remove the E-term; therefore subtract here.
*/
      int i,iend1;
      double rwork,dwork;
      rwork = ra;
      dwork = dec;
      iend1 = 3;

      Gtjul2.RaDecRetval loopRet;
      for (i=0; i<iend1;i++)
      {
          
          loopRet= gtetrm(rwork,dwork,edela,edeld);
          edela= loopRet._ra;
          edeld= loopRet._dec;
	  if (i == 2) return new RaDecRetval(edela,edeld);
          rwork = ra    - edela;
          dwork = dec   - edeld;
          loopRet= jclpos(rwork,dwork);
          rwork= loopRet._ra;
          dwork= loopRet._dec;
      }
      return new RaDecRetval(edela,edeld);
}

   static int  nthrue=0;

   static double e1, e2, e3, e4, dtor;

public static RaDecRetval gtetrm(double ra,  double dec,
                                 double dela,double deld)
//double ra, dec, *dela, *deld;
{
/*
*  from Suppl. to Astron. Alman. 1984 (also 1961 supp. page 144)
*    see also, Standish, A&A 115, 20-22 (1982)
*
* compute E-terms to be removed for  object at ra and dec.
*  all args. double precision and in decimal degrees)
*
*  Since the E-terms (terms of elliptic aberration) change so slowly
*    (Smart,Textbook on Spherical Astronomy, Sixth Ed. Section 108,p186)
*     these values do not require t as input and will be valid in the
*     1950 to 2000 time span we are dealing with.
*
*  The 1961 supp called these equations an approximation and stated that
*  small errors in this procedure are usually negligible. However, they
*  did not explain what lead up to the procedure:  "The form of the equations
*  of condition and their solution are not discussed here."
*
*/

      double dcosd, alplus; 

      if(nthrue == 0)
      {
         dtor = Math.atan(1.0) / 45.0;
/* note:      e1 = (0.0227 * 15.0) / 3600.0 = 0.341/3600 = e3 */
         e2 = 11.25 * 15.0;
         e3 = 0.341 / 3600.0;
         e4 = 0.029 / 3600.0;
         e1 = e3;
         nthrue = 1;
      }

      alplus = ra    + e2;
      if(alplus >= 360.0) alplus = alplus - 360.0;
      alplus = alplus * dtor;
 
      dcosd = Math.cos(dtor * dec);
      if(Math.abs(dec) >= 90.0 || Math.abs(dcosd) < 1.0e0-27)
      {
         dela = 0.0;
         deld = 0.0;
      }
      else
         dela = (e1 * Math.sin(alplus)) / dcosd;

      deld = (e3 * Math.cos(alplus) * Math.sin(dec*dtor)) + (e4 * dcosd);

      return new RaDecRetval(dela,deld);
}

      static int nthrue2 = 0;
      static double kappa, lepoch=-1.0, e, pirad;

public static RaDecRetval gtecle( double epoch, double lambda, double beta,
                                  double dela,  double deld)
//double epoch, lambda, beta, *dela, *deld;
{
/*
*
* compute E-terms at epoch for ecliptic lambda, beta input (returned in dela,
*  deld (degrees).
*  epoch in years (e.g. 1950.000), lambda,beta,,dela,deld in decimal degrees.
*  All arguments double precision.
*
* E-term formulas from ASTRONOMICAL ALGORITHMS by Jean Meeus (1991) ch.22
*   Note: equations as presented are for computing E-terms from position
*         that does not contain E-terms.  To get better answer (when
*         splitting hairs), iterate to get best E-term (for position that
*         has E-terms) to be removed. Subroutine iterec may be called to
*         to do the iteration.
*   Note 2: these formulas for E-terms, as function of ecliptic lon and lat,
*           also appear in  Spherical Astronomy by R.Green (1985),page 192;
*           and in Textbook on Spherical Astronomy, Sixth Ed.,by Smart (1977),
*           page 186.
*
*   To remove E-terms, subtract dela & deld from lamba & beta, respectively,
*   in the calling program.
*   To add back E-terms, add dela & deld to lambda & beta, respectively, in
*   the calling program.
*   
*
*/

      double t, t2, pi, lrad, brad;

      if(nthrue2 == 0) 
      {
         dtor = Math.atan(1.0) / 45.0;
/*    constant of aberration, kappa = 20.49552" "new" ("old" was 20.496) */
         kappa = 0.0056932;
         nthrue2 = 1;
      }

      dela = 0.0;
      deld = 0.0;

      if(epoch != lepoch) 
      {
         t = (epoch - 2000.0) * 0.01;
         t2 = t*t;
         lepoch = epoch;
/*
* e = eccentricity of the Earth's orbit
* pi= longitude of the perihelion of this orbit
*/
         e = 0.016708617 - 0.000042037*t - 0.0000001236*t2;
         pi= 102.93735 + 0.71953*t + 0.00046*t2;
         pirad = dtor * pi;
      }

      if(Math.abs(beta) > 89.999) return new RaDecRetval(dela,deld);
      lrad = dtor*lambda;
      brad = dtor*beta;

      dela = e * kappa * Math.cos(pirad-lrad) / Math.cos(brad);
      deld = e * kappa * Math.sin(pirad-lrad) * Math.sin(brad);

      return new RaDecRetval(dela,deld);
}

public static RaDecRetval iterec(double tobsj,double lambda,
                                 double beta,double edela,double edeld)
//double tobsj,lambda,beta,*edela,*edeld;
{
      int i, iend1 = 3;
      double lwork,bwork;
      lwork = lambda;
      bwork = beta;
      Gtjul2.RaDecRetval rdRet;
      for (i=0; i<iend1; i++)
      {
         rdRet= gtecle(tobsj,lwork,bwork,edela,edeld);
         edela= rdRet._ra;
         edeld= rdRet._dec;
         lwork = lambda - edela;
         bwork = beta - edeld;
         rdRet= Gtjul2.jclpos(lwork,bwork);
         lwork= rdRet._ra;
         bwork= rdRet._dec;
      }
      return new RaDecRetval(edela,edeld);
}
public static RaDecRetval jrmvet( double tobsj, double rat, double dect)
//double tobsj,*rat,*dect;
{
/*c Remove E-terms:
*  31jul92 update to use equecx and ecleqx instead of equec2 and ecleq2.
*/
      double edela= 0.0,edeld= 0.0;
      double lambda= 0.0, beta= 0.0, pole=89.999;

      if(Math.abs(dect) < pole)
      {
         RaDecRetval r15= gtetrm( rat, dect, edela, edeld);
         edela= r15._ra;
         edeld= r15._dec;
         rat = rat + edela;
         dect = dect + edeld;
         Gtjul2.RaDecRetval ret= Gtjul2.jclpos(rat,dect);
         rat= ret._ra;
         dect=ret._dec;
      }
      else
      {
/*    note: using "Julian system" (iaus=2 for equecx and ecleqx) here -
*     makes no. dif in resulting E-terms and simplifies argument
*     list required for jrmvet.
*/
         Ecleqx.EquecxRetval r18= Ecleqx.equecx(2,tobsj,rat,dect,lambda,beta);
         lambda= r18._xlam;
         beta  = r18._beta;
         RaDecRetval iterRet= iterec(tobsj,lambda,beta,edela,edeld);
         edela= iterRet._ra; 
         edeld= iterRet._dec; 
         lambda = lambda - edela;
         beta = beta - edeld;
         Gtjul2.RaDecRetval ret= Gtjul2.jclpos(lambda, beta);
         lambda= ret._ra;
         beta=   ret._dec;
         Ecleqx.EcleqxRetval r24= Ecleqx.ecleqx(2,tobsj,lambda,beta,rat,dect);
         rat=  r24._rad;
         dect= r24._decd;
      } 

      return new RaDecRetval(rat,dect);
}

public static RaDecRetval jaddet(double tobsj,double rat,double dect)
//double tobsj,*rat,*dect;
{
/* Add back  E-terms:
*  31jul92 update to use equecx and ecleqx instead of equec2 and ecleq2.
*/
      double edela= 0.0, edeld= 0.0, lambda= 0.0, beta= 0.0, pole = 89.999;

      if(Math.abs(dect) < pole) 
      {
         RaDecRetval iterRet= itere( rat, dect, edela, edeld);
         edela= iterRet._ra; 
         edeld= iterRet._dec; 
         rat = rat - edela;
         dect = dect - edeld;
         RaDecRetval ret= Gtjul2.jclpos(rat,dect);
         rat= ret._ra;
         dect=ret._dec;
      }
      else
      {
         
         Ecleqx.EquecxRetval r20= Ecleqx.equecx(2,tobsj, rat, dect, 
                                                 lambda, beta);
         lambda= r20._xlam;
         beta  = r20._beta;
         RaDecRetval r30= gtecle(tobsj, lambda, beta, edela, edeld);
         edela= r30._ra;
         edeld= r30._dec;
         lambda = lambda + edela;
         beta = beta + edeld;
         RaDecRetval ret= jclpos(lambda, beta);
         lambda= ret._ra;
         beta  = ret._dec;
         Ecleqx.EcleqxRetval r26= Ecleqx.ecleqx(2,tobsj, lambda, beta, 
                                               rat, dect);
         rat = r26._rad;
         dect= r26._decd;
      }

      return new RaDecRetval(rat,dect);
}

public static RaDecRetval jclpos(double rat,double dect)
//double *rat, *dect;
{
/* to put ra into 0 to 360 and dec into -90 to +90 ranges.*/

       if(rat > 360.0) 
          rat = rat - 360.0;
       else if(rat < 0.0)
          rat = rat + 360.0;

       if(Math.abs(dect) > 90.0)
       {
          rat = rat + 180.0;
          if(rat >= 360.0) rat = rat - 360.0;
          if(dect >   0.0)
             dect =   180.0 - dect;
          else
             dect = -(180.0 + dect);
      }
      return new RaDecRetval(rat,dect);
}

    public static class RaDecRetval {
        public double _ra;
        public double _dec;
        public RaDecRetval(double ra, double dec) {
             _ra= ra;
             _dec= dec;
        }
    }
    public static class RaDecPMRetval {
        public double _ra;
        public double _dec;
        public double _pmra;
        public double _pmdec;
        public RaDecPMRetval(double ra, double dec, double pmra, double pmdec){
             _ra= ra;
             _dec= dec;
             _pmra= pmra;
             _pmdec= pmdec;
        }
    }

}


