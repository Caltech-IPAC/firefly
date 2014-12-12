package edu.caltech.ipac.astro.conv;

public class Jbdist {

   static int nthru = 0;
   static double dtor, rtod;

public static double jbdist( double xlon1, double xlat1, double xlon2, 
                             double xlat2, double distd)
//double xlon1,xlat1,xlon2,xlat2,*distd;
{
/*
*
* updated 09jun92 to test costh ( to prevent acos error message when
*   round off near theta = 0. or 180.deg creates a dabs(cos(theta)) slightly
*   greater than 1.0d0)
* \ned\jbdist.f  28feb91  like prev. versions except does not call
*     fdtor2/frtod2
* f77 -c jbdist.f
* for vax, dacos is used instead of darcos.
* all args in double precision degrees.  distd returned.
*
* distd is distance between 2 positions on sky - results in degrees
*
*/


   double xlon1r,xlon2r, xlat1r,xlat2r,costh;

   if(nthru == 0)
   {
      dtor = Math.atan(1.0) / 45.0;
      rtod = 1.0 / dtor;
      nthru = 1;
   }

   xlon1r =  dtor * xlon1;
   xlon2r =  dtor * xlon2;
   xlat1r =  dtor * xlat1;
   xlat2r =  dtor * xlat2;

   costh = Math.cos(xlat1r)*Math.cos(xlat2r)*Math.cos(xlon1r-xlon2r) +
           Math.sin(xlat1r)*Math.sin(xlat2r);

   if(Math.abs(costh) > 1.0) costh = costh/Math.abs(costh);
   distd =  rtod*( Math.acos(costh));
   return distd;
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
