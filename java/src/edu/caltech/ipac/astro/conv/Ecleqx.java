/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

public class Ecleqx {
/* ecleqx.f  contains subs jgtobq,ecleqx,equecx
   30jul92  j.bennett
*/
public static double jgtobq( int iaus, double eqx, double ep)
//int iaus;
//double eqx, *ep;
{   
   double t, t2, t3;

      if(iaus != 1)
      {
         t = (eqx - 2000.0) * 0.01;
         t2 = t*t;
         t3 = t*t2;

         ep = (84381.448 - 46.8150*t - 0.00059*t2 + 0.001813*t3)/
                3600.0;

      }
      else
      {
         t = (eqx - 1950.0) * 0.01;
         t2 = t*t;
         t3 = t*t2;

         ep = (84404.84  - 46.850*t  - 0.0033*t2  + 0.00182*t3)/
                3600.0;

      }
      return ep;
}

private static int nthru = 0, laus = -99;
private static double dtor, rtod, leqx = -1.0, cose, sine;

public static EcleqxRetval ecleqx(int iaus,double eqx,double xlam,
                              double beta,double rad,double decd)
//int iaus;
//double eqx, xlam, beta, *rad, *decd;
{

   double cosb=0.0,cosl=0.0,sinb=0.0,sinl=0.0,xl=0.0,yl=0.0,zl=0.0,
          xe=0.0,ye=0.0,ze=0.0,xlamr=0.0,betar=0.0,rar=0.0,decr=0.0,e=0.0;
   
      if(nthru == 0)
      {
         dtor = Math.atan(1.0) / 45.0;
         rtod = 1.0 / dtor;
         nthru = 1;
      }

      if(eqx != leqx || iaus != laus) 
      {
	 e= jgtobq(iaus,eqx,e);
         e = e * dtor;
         cose = Math.cos(e);
         sine = Math.sin(e);
         leqx = eqx;
         laus = iaus;
      }

      xlamr = xlam*dtor;
      betar = beta*dtor;

      cosb = Math.cos(betar);
      cosl = Math.cos(xlamr);
      sinb = Math.sin(betar);
      sinl = Math.sin(xlamr);

      xl = sinb;
      yl = -(cosb*sinl);
      zl = cosb*cosl;

      xe = cose*xl - sine*yl;
      ye = sine*xl + cose*yl;
      ze = zl;

      rar = Math.atan2(-ye,ze);
      rad = rar * rtod;
      if(rad < 0.0) rad = 360.0 + rad;

/* try to catch pole on any machine (& return ra=0 then) */

      if(Math.abs(xe) > 1.0)
      {
	 decd = 90.0*xe/Math.abs(xe);
         rad = 0.0;
      }
      else
      {
         decr =  Math.asin(xe);
         decd = decr * rtod;
         if(Math.abs(decd) >= 90.0)
	 {
            rad = 0.0;
            if(decd >  90.0) decd =  90.0;
            if(decd < -90.0) decd = -90.0;
	 }
      }
      return new EcleqxRetval(rad, decd);
}

    public static class EcleqxRetval { 
          public double _rad;
          public double _decd;
          public EcleqxRetval(double rad, double decd) {
             _rad= rad;
             _decd= decd;
          }
    }

//   private static int nthru = 0, laus = -99;
//   private static double dtor, rtod, leqx = -0.1, cose, sine;
private static int laus2 = -99;
private static double leqx2 = -1.0, cose2, sine2;

public static EquecxRetval equecx(int iaus, double eqx, double rad, 
                                  double decd, double xlam, double beta)
//int iaus;
//double eqx, rad, decd, *xlam, *beta;
{

   double cosd=0.0,cosr=0.0,sind=0.0,sinr=0.0,xl=0.0,yl=0.0,
          zl=0.0, xe=0.0,ye=0.0,ze=0.0,
	  xlamr=0.0,betar=0.0,rar=0.0,decr=0.0,e=0.0;

      if(nthru == 0) 
      {
         dtor = Math.atan(1.0) / 45.0;
         rtod = 1.0 / dtor;
         nthru = 1;
      }

      if(eqx != leqx2 || iaus != laus2) 
      {
         e= jgtobq(iaus,eqx,e);
         e = e * dtor;
         cose2 = Math.cos(e);
         sine2 = Math.sin(e);
         leqx2 = eqx;
         laus2 = iaus;
      }

      rar = rad*dtor;
      decr = decd*dtor;

      cosd = Math.cos(decr);
      cosr = Math.cos(rar);
      sind = Math.sin(decr);
      sinr = Math.sin(rar);

      xe = sind;
      ye = -(cosd*sinr);
      ze = cosd*cosr;

      xl = cose2*xe + sine2*ye;
      yl = -sine2*xe + cose2*ye;
      zl = ze;

      if(Math.abs(xl) > 1.0)
      {
	 beta = 90.0*xl/Math.abs(xl);
         xlam = 0.0;
         return new EquecxRetval (xlam, beta);
      }
      else
      {
         betar =  Math.asin(xl);
         xlamr = Math.atan2(-yl,zl);
         xlam = xlamr * rtod;
         if(xlam < 0.0) xlam = 360.0 + xlam;
         beta = betar * rtod;
         if(Math.abs(beta) >= 90.0) 
	 {
            if(beta >  90.0) beta =  90.0;
            if(beta < -90.0) beta = -90.0;
            xlam = 0.0;
	 }
      }
      return new EquecxRetval (xlam, beta);
}

    public static class EquecxRetval { 
          public double _xlam;
          public double _beta;
          public EquecxRetval(double xlam, double beta) {
             _xlam= xlam;
             _beta= beta;
          }
    }

}


