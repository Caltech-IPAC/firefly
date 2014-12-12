package edu.caltech.ipac.astro.conv;

public class Jbposa {

   static int nthru = 0;
   static double d2r, r2d;

public static JbposaRetval jbposa( double xlon1i, double xlat1i, 
                                   double xlon0i, double xlat0i, 
                                   double distd,  double pa, int ipf)
//int    *ipf;
//double xlon1i,xlat1i,xlon0i,xlat0i,*distd, *pa;
{
/*
*
* 23feb91 updated some comments.
* 22feb91 to make sure pa is 0 to 360 (i.e.never < 0 )in sub jbelpt.
* 19feb91 jbposa2.f  to allow 0 to 360 deg pa
*  26,29oct90 to add pole test to jbposa sub.
*
* 28sep90 to include slightly modified ipac prec. routines
* 26sep90 to improve comments & use smaller value for value variable small.
* 10sep90 jbposa modification of ned version of jbdist to return pa
*   (0 - 180 deg. E of N) if distd < 90 deg.
* jbposa finds distance between 2 positions & (if distd <90.deg) finds
*   position angle (pa) of (xlon1,xlat1) with respect to (xlon0,xlat0)
*
* all non-integer args in double precision degrees.distd,pa,ipf returned.
*
* for discussion of xlon1, etc. see pa explanation below.
*
* distd is distance between 2 positions on sky - results in degrees.
*
* pa is position angle (degrees E of N) -  0 to 180 deg. returned and
*  is valid only if distd < 90. deg.
*  pa is sensitive to what is called xlon1,xlat1 and what is called
*  xlon0,xlat0.
*   for use w/ ellipse  xlon1,xlat1 is pt on ellipse;
*                       xlon0,xlat0 is center of ellipse.
*
* ipf is integer flag for pa compution: 0 = not computed; 1 = computed;
*      2=computed and one of points treated as though exactly at pole.
*      if ipf = 0, and distd .ne. 0, distd was computed.
*
* NOTE: reference for the equations used:
*   A Compendium of Spherical Astronomy by Simon Newcomb
*    Section 56 (page 111)
*
*  where s is distance (in degrees), p is position angle (in degrees)
*        dela = alpha' - alpha
*     sin(s)*sin(p) =                           cos(d')*sin(dela)
*     sin(s)*cos(p) = cos( d)*sin(d') - sin( d)*cos(d')*cos(dela)
*            cos(s) = sin( d)*sin(d') + cos( d)*cos(d')*cos(dela)
*        Therefore:
*          s = acos(cos(s))
*          p = atan2(sin(p) , cos(p))
*      p is counted from the meridian passing north through position
*        at (alpha,d)  toward the east.
*
* NOTE: maximum precision in distd is .01" on machine with a 64-bit
*       double precision word. On cyber, with 128-bit double precision
*       word, precision may exceed .00001" .  The worst case for distd
*       is near the poles.
*       Precision for posangle is within .5 degree which is usually
*       considered, astronomically speaking, more than satisfactory.
*       Actual test cases have indicated pa is within 1.2d-06" on
*       a UNIX machine (i.e. 64-bit double precision)- however not
*       all possible cases have been tested.
*
*/

   int ipole, itest = 2, itesti;
   double xlon1,xlat1,xlon0,xlat0,xlon1r,xlon0r, xlat1r,xlat0r,costh;
   double ds,delar,sinp,cosp,p;
   double dellat,dellon,dist,dwork;
   double small = 1.0e-10, tfac = 36000000.0; 

      if (nthru == 0)
      {
         d2r = Math.atan(1.0)/45.0;
         r2d = 1.0/d2r;
         nthru = 1;
      }

      pa  = 0.0;
      ipf = 0;

/* ipole=1 if xlat0 at a pole; ipole=2 if xlat1 is at a pole. */
      ipole = 0;

      if((Math.abs(xlat0i) == 90.0) && (xlat1i == xlat0i)) 
      {
          distd = 0.0;
          return new JbposaRetval(ipf,distd,pa);
      }

      xlat0 = xlat0i;
      xlon0 = xlon0i;
      xlat1 = xlat1i;
      xlon1 = xlon1i;

/* if lat is within .0002" of pole, treat as pole: */
      if(Math.abs(xlat0i) > 89.9999) 
      {
         itesti = (int)((90.0-Math.abs(xlat0i)) * tfac);
         if(Math.abs(itesti) <= itest)
         {
	    xlat0 = 90.0*xlat0i/Math.abs(xlat0i);
            xlon0 = 0.0;
            ipole = 1;
         }
      }

      if(Math.abs(xlat1i) > 89.9999) 
      {
         itesti = (int)((90.0-Math.abs(xlat1i)) * tfac);
         if(Math.abs(itesti) <= itest) 
         {
	    xlat1 = 90.0*xlat1i/Math.abs(xlat1i);
            xlon1 = 0.0;
            ipole = 2;
         }
      }

      if(Math.abs(xlat0) == 90.0 && xlat1 == xlat0) 
      {
         distd = 0.0;
         return new JbposaRetval(ipf,distd,pa);
      }

      xlon1r = d2r * xlon1;
      xlon0r = d2r * xlon0;
      xlat1r = d2r * xlat1;
      xlat0r = d2r * xlat0;
      delar = xlon1r - xlon0r;
      costh = Math.cos(xlat1r)*Math.cos(xlat0r)*Math.cos(delar) + 
                  Math.sin(xlat1r)*Math.sin(xlat0r);

      if(costh < -1.0) costh = -1.0;
      if(costh >  1.0) costh =  1.0;

      ds = Math.acos(costh);
      distd = r2d * ds;

      if(Math.abs(distd) >= 90.0 ) return new JbposaRetval(ipf,distd,pa);
      if(xlat1==xlat0 && xlon1==xlon0) return new JbposaRetval(ipf,distd,pa); 
      if(Math.abs(distd) < 1.667e-02)
      {
/* alternate method okay for short distances and avoids using so many
   trig functions that may exceed precision of machine being used. */
         dellat = xlat1 - xlat0;
         dellon = Math.abs(xlon1 - xlon0);
         if(dellon > 180.0) dellon = 360.0 - dellon;
         dellon = dellon * Math.cos(xlat0r);
         dist = Math.sqrt(dellat*dellat + dellon*dellon);
         distd = dist;
         ds = d2r * distd;
         if(dist <= small) return new JbposaRetval(ipf,distd,pa);
      }

/* compute position angle if possible */
      ipf = 1;
      if(ipole > 0) ipf = 2;
      sinp = Math.cos(xlat1r)*Math.sin(delar) / Math.sin(ds);
      if(Math.abs(sinp) < 1.0e-09)
      {
         pa = 0.0;
         if(90.0-Math.abs(xlat0) >= distd)
         {
            if(xlat1 < xlat0) 
	       pa = 180.0;
         }
         else
	 {
            dwork = Math.abs(xlon0 - xlon1);
            if(dwork > 180.0) 
	       dwork = Math.abs(dwork -360.0);
            if(xlat0 >   0.0) 
	    {
/*                                          north pole: */
               if(dwork <= 1.0 && ipole != 2) pa = 180.0;
	    }
	    else
	    {
/*                                          south pole: */	       
	       if(dwork >= 179.0 || ipole == 2) pa = 180.0;
	    }
         }
         return new JbposaRetval(ipf,distd,pa);
      }
      cosp = (Math.cos(xlat0r)*Math.sin(xlat1r)-
                        Math.sin(xlat0r)*Math.cos(xlat1r)*
                        Math.cos(delar)) / Math.sin(ds);
      if(Math.abs(cosp) < 1.0e-09) 
      {
         pa = 90.0;
         dwork = xlon0 - xlon1;
         if(Math.abs(dwork) < 180.0)
	 {
            if(dwork > 0.0) 
	       pa = 270.0;
	 }
         else 
	 {
	    if(dwork < 0.0) 
	       pa = 270.0;
	 }
         return new JbposaRetval(ipf,distd,pa);
      } 
      p = Math.atan2(sinp, cosp);
      pa = r2d * p;
      if (pa <    0.0) pa = 360.0 + pa;
      if (pa >= 360.0) pa = pa - 360.0;

      return new JbposaRetval(ipf,distd,pa);
}

    public static class JbposaRetval {
        public int    _ipf;
        public double _distd;
        public double _pa;
        public JbposaRetval(int ipf, double distd, double pa) {
             _distd= distd;
             _pa= pa;
             _ipf= ipf;
        }
    }



public static JbelptRetval jbelpt( double xlonin, double xlatin, double distd, 
                                   double pain,   double xlonp,  double xlatp, 
                                   int iflg)
//int    *iflg;
//double xlonin,xlatin,distd,pain,*xlonp,*xlatp;
{
/*
*
* finds pt on end of ellipse given center xlonin & xlatin, posang (pa)
*    and major axis semi-diameter (distd). Returns xlonp,xlatp & iflg).
*    all non-integer args are double precision degrees.
*   iflg indicates processing: 1 completely "normal"
*        0 = distd too small (new pt = old pt on return)
*        2 = posang too small (treated as though pa= 0.)
*            ( note 3 can override iflg=2)
*        3 = treated xlonin,xlatin as pole:  e.g. dabs(xlatin) close
*            to 90.deg, treated as + or - 90 & xlonin was not 0 on
*            input, but was treated as though zero.
*             Note: test for pole used here is that any position with
*                   dabs(xlatin) > 89.99999d0 (89d59'59.964") is at the
*                   pole  and is treated as pole with xlon = 0 deg. and
*                   xlat as -90. or + 90. deg as appropriate.
*        4 = xlonp,xlatp should be treated as pole, however computed
*            pos has not been revised. Calling prog. may want to revise pos.
*
* NOTE: reference for the equations used:
*   A Compendium of Spherical Astronomy by Simon Newcomb
*    Section 56 (page 111)
*
*/

      int donext = 1;
      double xlon0, xlat0, xlat0r, xlatpr=0.0, distr;
      double pa, par, dela=0.0, delar, part, b;
      double cosdp,sindp,sinpar;
      double small = 1.0e-09, test=89.99999;

      if (nthru == 0)
      {
         d2r = Math.atan(1.0)/45.0;
         r2d = 1.0/d2r;
         nthru = 1;
      }

      test = 89.99999;
      if(90.0-test > distd) 
         test= 90.0 - (distd - 0.1*distd);
      iflg = 1;
      xlon0 = xlonin;
      xlat0 = xlatin;

      if(distd < small) 
      {
         xlonp = xlon0;
         xlatp = xlat0;
         iflg = 0;
         return new JbelptRetval(iflg,xlonp,xlatp);
      }

      if(Math.abs(xlat0) >= test) 
      {
         if(xlon0 >= 360.0) xlon0 = xlon0 - 360.0;
         if(Math.abs(xlon0) > small) 
         {
          iflg = 3;
          xlon0 = 0.0;
          if(xlat0 < 0.0) xlat0 = -90.0;
          if(xlat0 > 0.0) xlat0 =  90.0;
         }
      } 

      pa = pain;
      if(pain < 0.0) pa = 360.0 + pain;
      distr = d2r * distd;
      par   = d2r * pa;
      sinpar = Math.sin(par);

/*                                      here pa either near 0 or 180: */
      if(Math.abs(sinpar) < small)
      {
         if(iflg != 3) iflg = 2;
         xlonp = xlon0;
         if( pa <= 90.0 || pa >= 270.0)
         {
            xlatp = xlat0 + distd;
            if(xlatp > 90.0)
	    {
               xlatp = 180.0 - xlatp;
               xlonp = xlon0 + 180.0;
            }
         }
         else
	 {
            xlatp = xlat0 - distd;
            if(xlatp < -90.0)
	    {
               xlatp = -(180.0 + xlatp);
               xlonp = xlon0 + 180.0;
	    }
         }
         if(xlonp >= 360.0) xlonp = xlonp - 360.0;
         if(Math.abs(xlatp) >= test) iflg = 4;
	 return new JbelptRetval(iflg,xlonp,xlatp);
      } 

      part   = Math.sin(distr) * sinpar;
      xlat0r = d2r * xlat0;

      if(distd <= 1.667e-02 && Math.abs(xlat0) <= 89.0)
      {
         xlatpr = xlat0r + distr*Math.cos(par);
         delar  = distr * sinpar/Math.cos(xlatpr);
         dela   = r2d*delar;
         if(dela <= 360.0) 
	    donext=0;
      }

      if (donext == 1)
      {
         if(Math.abs(xlat0) < small)
         {
            b = Math.cos(distr) / part;
            delar = Math.atan(1.0 / b);
            cosdp = part/Math.sin(delar);
            sindp = (Math.sin(distr)*Math.cos(par)+cosdp*
                            Math.sin(xlat0r)*Math.cos(delar))/
                                           Math.cos(xlat0r);
            xlatpr = Math.atan(sindp/cosdp);
         }
         else
         {
            b = (Math.cos(distr)*Math.cos(xlat0r) - 
                 Math.sin(distr)*Math.cos(par)*Math.sin(xlat0r))/part;
            delar = Math.atan(1.0 / b);
            cosdp = part/Math.sin(delar);
            sindp = (Math.cos(distr) - b*Math.cos(xlat0r)*part)
                                  /Math.sin(xlat0r);
            if(Math.abs(cosdp) >= 1.0e-09)
               xlatpr = Math.atan2(sindp,cosdp);
            else
               xlatpr = Math.asin(sindp);
         }

         dela = r2d*delar;
      }

      xlonp = xlon0 + dela;
      if(xlonp >= 360.0) xlonp = xlonp - 360.0;
      if(xlonp <    0.0) xlonp = xlonp + 360.0;

      xlatp = r2d * xlatpr;
      if(Math.abs(xlatp) < test) return new JbelptRetval(iflg,xlonp,xlatp);

      if(Math.abs(xlatp) > 90.0) 
      {
         if(xlatp < 90.0)
            xlatp = -(180.0 + xlatp);
         else
            xlatp = 180.0 - xlatp;
         xlonp = xlonp - 180.0;
         if(xlonp < 0.0) xlonp = xlonp + 360.0;
      }


/* set flag so user may reset xlonp,xlatp to pole val w/ ra=0 */

      return new JbelptRetval(iflg,xlonp,xlatp);
}
    public static class JbelptRetval {
        public int    _iflg;
        public double _xlonp;
        public double _xlatp;
        public JbelptRetval(int iflg, double xlonp, double xlatp) {
             _xlonp= xlonp;
             _xlatp= xlatp;
             _iflg= iflg;
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
