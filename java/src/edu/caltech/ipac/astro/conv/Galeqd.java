package edu.caltech.ipac.astro.conv;

public class Galeqd{

      static int nthru = 0;
      static double dtor, rtod; 
      static double a[]= new double[3], b[]= new double[3], c[]= new double[3];


public static Retval galeqd( double xl, double xb, double rad, double decd)
//double xl, xb, *rad, *decd;
{     
/*
* galeqd,equgad,sglgal,galsgl subroutines moved from jcnvcd.f file to galeqd.f
*   on 14sep92 - no changes to code, itself, made (code same as 17jan91)
*
* galeqd (modifcation of galequ) requires double prec. args (22oct90)
*
****  entry equcor(xl,xb,rad,decd)   obsolete entry pt equcor.
* 22mar85 for hepvax (changed darsin to dasin;removed entry equcor)
* 17mar82 updated so has more sensible name- galequ, but still
*   retains alias for equcor.
* 02oct80  jdb   29jan81 to input dtor,rtod to more places. 22apr81
* subroutine to compute earth equatorial coords from galactic coords
* inputs: xl l in degrees, xb b in degrees.
* b = galactic latitude (-90. to + 90. deg);l=galactic longitude(0-360)
* outputs: rad ra in degrees, decd dec in degrees.
*  x = cos(b)*cos(l)
*  y = sin(l)*cos(b)
*  z = sin(b)
*  gal <==> equ equations from tom soifer.  see also:
*   Classical Mechanics by Herbert Goldstein (Addison-Wesley Publ.Co.,
*     c.1950, 7th printing 1965) Section 4-4 on Eulerian Angles (p.107-109)
*
* assumes gal north pole at 12h 49.0m, +27d24' (192.25d,+27.4d)
*         gal (0, 0) at     17h 42.4m, -28d55' (265.36d,-28.91d)
*   also: delta=lat=0 at gal (33d,0d) ; equ (282.25d,0d).
*
*/


      double xlr,xbr,cosb,cosl,sinl,x,y,z,sind,
             cosd,decr,rar,eq2,eq3,cosa,sina;
      double cosph,sinph,cosps,sinps,costh,sinth;
      double psi=-33.0, theta=62.6, phi=282.25;

      if(nthru == 0) 
      {
         dtor = Math.atan(1.0) / 45.0;
         rtod = 1.0 / dtor;


/*                         compute matrix for gal ==> equ conversion:*/

         cosps = Math.cos(psi*dtor);
         sinps = Math.sin(psi*dtor);
         cosph = Math.cos(phi*dtor);
         sinph = Math.sin(phi*dtor);
         costh = Math.cos(theta*dtor);
         sinth = Math.sin(theta*dtor);

/* |  b[0]  b[1]  b[2]  |
 * |  c[0]  c[1]  c[2]  |
 * |  a[0]  a[1]  a[2]  |
 * */
         b[0] = cosps*cosph - costh*sinph*sinps;
         b[1] = -(sinps*cosph) - costh*sinph*cosps;
         b[2] = sinth*sinph;

         c[0] = cosps*sinph + costh*cosph*sinps;
         c[1] = -(sinps*sinph) + costh*cosph*cosps;
         c[2] = -(sinth*cosph);

         a[0] = sinth*sinps;
         a[1] = sinth*cosps;
         a[2] = costh;

         nthru = 1;
      }

      xlr = xl * dtor;
      xbr = xb * dtor;
      cosb = Math.cos(xbr);
      cosl = Math.cos(xlr);
      sinl = Math.sin(xlr);
      z    = Math.sin(xbr);
      x = cosb*cosl;
      y = sinl*cosb;
      sind = a[0]*x + a[1]*y + a[2]*z;
      if(Math.abs(sind)>=1.0)
      {
         sind = sind/Math.abs(sind);
         decr = Math.asin(sind);
         rar = 0.0;
      }

      else
      {
	 eq2  = b[0]*x + b[1]*y + b[2]*z;
         eq3  = c[0]*x + c[1]*y + c[2]*z;
         decr = Math.asin(sind);
         cosd = Math.sqrt(1.0-sind*sind);
         cosa = eq2/cosd;
         sina = eq3/cosd;
         if(Math.abs(cosa)>1.0) cosa = cosa/Math.abs(cosa);
         if(Math.abs(sina)>1.0) sina = sina/Math.abs(sina);
         rar = Math.atan2(sina,cosa);
      }

      rad  = rar * rtod;
      if(rad < 0.0) rad = 360.0 + rad;
      decd = decr * rtod;
      if(Math.abs(decd) >= 90.0) 
      {
         rad = 0.0;
         if(decd >  90.0) decd =  90.0;
         if(decd < -90.0) decd = -90.0;
      }

      return new Retval(rad,decd);

}

static int nthru2 = 0;
static double a2[]= new double[3], b2[]= new double[3], c2[]= new double[3];
public static Retval equgad( double rad, double decd, double xl, double xb)
//double rad, decd, *xl, *xb;
{
/*
* equgad (modification of equgal) requires double prec. args (22oct90)
*
* 22mar85 for hepvax (darsin changed to dasin;entry galcor removed)
**    entry galcor(rad,decd,xl,xb)
* 17mar82 updated for more sensible name- equgal, while retaining
*     alias for old name of galcor.
* 02oct80 jdb:subroutine to compute galactic coords from earth
*  equatorial coords.29jan81 for more places in dtor,rtod.22apr81
*  gal <==>equ equations from tom soifer.  see also:
*   Classical Mechanics by Herbert Goldstein (Addison-Wesley Publ.Co.,
*     c.1950, 7th printing 1965) Section 4-4 on Eulerian Angles (p.107-109)
* assumes gal north pole at 12h 49.0m, +27d24' (192.25d,+27.4d)
*         gal (0, 0) at     17h 42.4m, -28d55' (265.36d,-28.91d)
*   also: delta=lat=0 at gal (33d,0d) ; equ (282.25d,0d).
*
* inputs: rad ra in degrees, decd is dec in degrees.
* b = galactic latitude (-90. to + 90. deg);l=galactic longitude(0-360)
*
*  x = cos(alpha)*cos(delta)
*  y = sin(alpha)*cos(delta)
*  z = sin(delta)
*  eq1 = sin(b) = -.8676*x - .1884*y + .4602*z
*  eq2 = cos(b)*cos(l) = -.0669*x - .8728*y - .4835*z
*  eq3 = cos(b)*sin(l) = +.4927*x - .4503*y + .7446*z
*    b = arsin(sin(b))
*  cos(b) = sqrt(1.-sin(b)*sin(b))
*  cos(l) = eq2/cos(b)
*  sin(l) = eq3/cos(b)
*  l = atan2(sin(l)/cos(l): if(l.lt.0) l = l+2*pi if radians.
*
*/

   double rar,decr,cosa,sina,cosd,x,y,z,cosb,
          sinb,eq2,eq3,xbr,xlr,cosl,sinl;
   double cosph,sinph,cosps,sinps,costh,sinth;
   double psi=-33.0, theta=62.6, phi=282.25;

   if(nthru2 == 0)
   {
      dtor = Math.atan(1.0) / 45.0;
      rtod = 1.0 / dtor;

/*                         compute matrix for equ ==> gal conversion: */

      cosps = Math.cos(psi*dtor);
      sinps = Math.sin(psi*dtor);
      cosph = Math.cos(phi*dtor);
      sinph = Math.sin(phi*dtor);
      costh = Math.cos(theta*dtor);
      sinth = Math.sin(theta*dtor);
/*
* |  b[1]  b[2]  b[3]  |
* |  c[1]  c[2]  c[3]  |
* |  a[1]  a[2]  a[3]  |
*
*/
      b2[0] = cosps*cosph - costh*sinph*sinps;
      b2[1] = cosps*sinph + costh*cosph*sinps;
      b2[2] = sinps*sinth;

      c2[0] = -(sinps*cosph) - costh*sinph*cosps;
      c2[1] = -(sinps*sinph) + costh*cosph*cosps;
      c2[2] =  cosps*sinth;

      a2[0] = sinth*sinph;
      a2[1] = -(sinth*cosph);
      a2[2] = costh;

      nthru2 = 1;
   }


      rar  = rad * dtor;
      decr = decd * dtor;
      cosa = Math.cos(rar);
      sina = Math.sin(rar);
      cosd = Math.cos(decr);
      z    = Math.sin(decr);
      x = cosa*cosd;
      y = sina*cosd;
      sinb = a2[0]*x+ a2[1]*y + a2[2]*z;
      if(Math.abs(sinb)>=1.0) 
      {
         sinb = sinb/Math.abs(sinb);
         xbr =  Math.asin(sinb);
         xlr = 0.0;
      }
      else
      {
         eq2  = b2[0]*x + b2[1]*y + b2[2]*z;
         eq3  = c2[0]*x + c2[1]*y + c2[2]*z;
         xbr =  Math.asin(sinb);
         cosb = Math.sqrt(1.0-sinb*sinb);
         cosl = eq2/cosb;
         sinl = eq3/cosb;
         if(Math.abs(cosl)>1.0) cosl = cosl/Math.abs(cosl);
         if(Math.abs(sinl)>1.0) sinl = sinl/Math.abs(sinl);
         xlr = Math.atan2(sinl,cosl);
      }

      xl = xlr * rtod;
      if(xl < 0.0) xl = 360.0 + xl;
      xb = xbr * rtod;
      if(Math.abs(xb) >= 90.0) 
      {
         xl = 0.0;
         if(xb >  90.0) xb =  90.0;
         if(xb < -90.0) xb = -90.0;
      }
      return new Retval(xl,xb);

}

static int nthru3 = 0;
static double a3[]= new double[3], b3[]= new double[3], c3[]= new double[3];
public static Retval galsgl( double rad, double decd, double xl, double xb)
//double rad,decd,*xl,*xb;
{
/*
*
* galsgl (modification of equgad) to compute supergalactic coordinates
*  from galactic coordinates.  j.bennett  30oct-05nov90
*
* inputs: rad =  gal lon (l) in degrees. double precision.
*         decd = gal lat (b) in degrees. double precision.
* returned:
* xb = supergalactic latitude  (SGB)  (-90. to + 90. deg). double precision.
* xl = supergalactic longitude (SGL)  (0-360. deg). double precision.
*
*  Computed values of rotation used for the Eulerian angles:
*   phi = 137.37 deg.;  theta = 83.68 deg.;  psi = 0. deg.
*    Based on gal l,b = 137.37, 0 deg. at sg SGL,SGB = 0., 0. and
*         SG North Pole (0,+90) at gal l,b = 47.37, +6.32 deg.
*
* References:
* de Vaucouleurs,G., A. de Vaucouleurs, and G.H.Corwin,
*  Second Reference Catalog of Bright Galaxies, (1976), p.8
*
* Tully,B., Nearby Galaxies Catalog, (1988) p. 1,4-5
* 
*    Note: de Vaucouleurs gives gal l,b 137.29, 0 for SGL,SGB= 0,0;
*          this is not 90 degrees from Galactic North Pole.- used
*          Tully's value of 137.37, 0 deg.
*
*  gal <==> equ equations from tom soifer.  see also:
*   Classical Mechanics by Herbert Goldstein (Addison-Wesley Publ.Co.,
*     c.1950, 7th printing 1965) Section 4-4 on Eulerian Angles
*     (p.107-109)
*    note: such equations also appropriate for gal <==> sgl .
*          See code below for def. of arrays a,b,c.
*  x = cos(lon)*cos(lat)
*  y = sin(lon)*cos(lat)
*  z = sin(lat)
*  eq1 = sin(sgb)          =   a(1)*x +  a(2)*y +  a(3)*z
*  eq2 = cos(sgb)*cos(sgl) =   b(1)*x +  b(2)*y +  b(3)*z
*  eq3 = cos(sgb)*sin(sgl) =   c(1)*x +  c(2)*y +  c(3)*z
*  sgb = arsin(sin(sgb))
*  cos(sgb) = sqrt(1.-sin(sgb)*sin(sgb))
*  cos(sgl) = eq2/cos(sgb)
*  sin(sgl) = eq3/cos(sgb)
*  sgl = atan2(sin(sgl)/cos(sgl): if(sgl.lt.0) sgl = sgl+2*pi if radians.
*
*/

   double rar,decr,cosa,sina,cosd,x,y,z,cosb,
          sinb,eq2,eq3,xbr,xlr,cosl,sinl;
   double cosph,sinph,cosps,sinps,costh,sinth;
   double psi= 0.0, theta = 83.68, phi = 137.37;

   if(nthru3 == 0) 
   {
      dtor = Math.atan(1.0) / 45.0;
      rtod = 1.0 / dtor;

/*                         compute matrix for equ ==> gal conversion: */
      cosps = Math.cos(psi*dtor);
      sinps = Math.sin(psi*dtor);
      cosph = Math.cos(phi*dtor);
      sinph = Math.sin(phi*dtor);
      costh = Math.cos(theta*dtor);
      sinth = Math.sin(theta*dtor);
/*
* |  b[0]  b[1]  b[2]  |
* |  c[0]  c[1]  c[2]  |
* |  a[0]  a[1]  a[2]  |
*/
      b3[0] = cosps*cosph - costh*sinph*sinps;
      b3[1] = cosps*sinph + costh*cosph*sinps;
      b3[2] = sinps*sinth;

      c3[0] = -(sinps*cosph) - costh*sinph*cosps;
      c3[1] = -(sinps*sinph) + costh*cosph*cosps;
      c3[2] =  cosps*sinth;

      a3[0] = sinth*sinph;
      a3[1] = -(sinth*cosph);
      a3[2] = costh;

      nthru3 = 1;
   }


      rar  = rad * dtor;
      decr = decd * dtor;
      cosa = Math.cos(rar);
      sina = Math.sin(rar);
      cosd = Math.cos(decr);
      z    = Math.sin(decr);
      x = cosa*cosd;
      y = sina*cosd;
      sinb = a3[0]*x + a3[1]*y + a3[2]*z;
      if(Math.abs(sinb)>=1.0)
      {
         sinb = sinb/Math.abs(sinb);
         xbr =  Math.asin(sinb);
         xlr =  0.0;
      }
      else
      {
         eq2  = b3[0]*x + b3[1]*y + b3[2]*z;
         eq3  = c3[0]*x + c3[1]*y + c3[2]*z;
         xbr  =  Math.asin(sinb);
         cosb = Math.sqrt(1.0-sinb*sinb);
         cosl = eq2/cosb;
         sinl = eq3/cosb;
         if(Math.abs(cosl)>1.0) cosl = cosl/Math.abs(cosl);
         if(Math.abs(sinl)>1.0) sinl = sinl/Math.abs(sinl);
         xlr = Math.atan2(sinl,cosl);
      }

      xl  = xlr * rtod;
      if(xl < 0.0) xl = 360.0 + xl;
      xb = xbr * rtod;
      if(Math.abs(xb) >= 90.0)
      {
         xl = 0.0;
         if(xb >  90.0) xb =  90.0;
         if(xb < -90.0) xb = -90.0;
      }
   return new Retval(xl,xb);
}

static int nthru4 = 0;
static double a4[]= new double[3], b4[]= new double[3], c4[]= new double[3];
public static Retval sglgal( double xl, double xb, double rad, double decd)
//double xl, xb, *rad, *decd;
{
/*
*
* sglgal (modification of galeqd) to compute galactic coordinates
*  from supergalactic coordinates.  j.bennett  30oct-05nov90
*
* inputs:
* xb = supergalactic latitude  (SGB)  (-90. to + 90. deg). double precision.
* xl = supergalactic longitude (SGL)  (0-360. deg). double precision.
* returned: rad =  gal lon (l) in degrees. double precision.
*           decd = gal lat (b) in degrees. double precision.
*
*  Computed values of rotation used for the Eulerian angles:
*   phi = 137.37 deg.;  theta = 83.68 deg.;  psi = 0. deg.
*    Based on gal l,b = 137.37, 0 deg. at sg SGL,SGB = 0., 0. and
*         SG North Pole (0,+90) at gal l,b = 47.37, +6.32 deg.
*
* References:
* de Vaucouleurs,G., A. de Vaucouleurs, and G.H.Corwin,
*  Second Reference Catalog of Bright Galaxies, (1976), p.8
*
* Tully,B., Nearby Galaxies Catalog, (1988) p. 1,4-5
* 
*    Note: de Vaucouleurs gives gal l,b 137.29, 0 for SGL,SGB= 0,0;
*          this is not 90 degrees from Galactic North Pole.- used
*          Tully's value of 137.37, 0 deg.
*
*  gal <==> equ equations from tom soifer.  see also:
*   Classical Mechanics by Herbert Goldstein (Addison-Wesley Publ.Co.,
*     c.1950, 7th printing 1965) Section 4-4 on Eulerian Angles
*     (p.107-109)
*    note: such equations also appropriate for gal <==> sgl .
*          See code below for def. of arrays a,b,c.
*  x = cos(sgb)*cos(sgl)
*  y = sin(sgl)*cos(sgb)
*  z = sin(sgb)
*  eq1 = sin(lat)          =   a(1)*x +  a(2)*y +  a(3)*z
*  eq2 = cos(lat)*cos(lon) =   b(1)*x +  b(2)*y +  b(3)*z
*  eq3 = cos(lat)*sin(lon) =   c(1)*x +  c(2)*y +  c(3)*z
*  sgb = arsin(sin(lat))
*  cos(lat) = sqrt(1.-sin(lat)*sin(lat))
*  cos(lon) = eq2/cos(lat)
*  sin(lon) = eq3/cos(lat)
*  lon = atan2(sin(lon)/cos(lon): if(lon.lt.0) lon = lon+2*pi if radians.
*
*/
   double xlr,xbr,cosb,cosl,sinl,x,y,z,sind,
          cosd,decr,rar,eq2,eq3,cosa,sina;
   double cosph,sinph,cosps,sinps,costh,sinth;
   double psi=0.0, theta=83.68, phi=137.37;

   if(nthru4 == 0)
   {
      dtor = Math.atan(1.0) / 45.0;
      rtod = 1.0 / dtor;

/*                         compute matrix for gal ==> equ conversion: */

      cosps = Math.cos(psi*dtor);
      sinps = Math.sin(psi*dtor);
      cosph = Math.cos(phi*dtor);
      sinph = Math.sin(phi*dtor);
      costh = Math.cos(theta*dtor);
      sinth = Math.sin(theta*dtor);
/*
* |  b[0]  b[1]  b[2]  |
* |  c[0]  c[1]  c[2]  |
* |  a[0]  a[1]  a[2]  |
*/
      b4[0] = cosps*cosph - costh*sinph*sinps;
      b4[1] = -(sinps*cosph) - costh*sinph*cosps;
      b4[2] = sinth*sinph;

      c4[0] = cosps*sinph + costh*cosph*sinps;
      c4[1] = -(sinps*sinph) + costh*cosph*cosps;
      c4[2] = -(sinth*cosph);

      a4[0] = sinth*sinps;
      a4[1] = sinth*cosps;
      a4[2] = costh;

      nthru4 = 1; 
   }


      xlr = xl * dtor;
      xbr = xb * dtor;
      cosb = Math.cos(xbr);
      cosl = Math.cos(xlr);
      sinl = Math.sin(xlr);
      z    = Math.sin(xbr);
      x = cosb*cosl;
      y = sinl*cosb;
      sind = a4[0]*x + a4[1]*y + a4[2]*z;
      if(Math.abs(sind)>=1.0)
      {
         sind = sind/Math.abs(sind);
         decr =  Math.asin(sind);
         rar = 0.0;
      }

     else
     {
        eq2  = b4[0]*x + b4[1]*y + b4[2]*z;
        eq3  = c4[0]*x + c4[1]*y + c4[2]*z;
        decr = Math.asin(sind);
        cosd = Math.sqrt(1.0-sind*sind);
        cosa = eq2/cosd;
        sina = eq3/cosd;
        if(Math.abs(cosa)>1.0) cosa = cosa/Math.abs(cosa);
        if(Math.abs(sina)>1.0) sina = sina/Math.abs(sina);
        rar = Math.atan2(sina,cosa);
     }
     rad  = rar * rtod;
     if(rad < 0.0) rad = 360.0 + rad;
     decd = decr * rtod;
     if(Math.abs(decd) == 90.0) 
     {
        rad = 0.0;
        if(decd >  90.0) decd =  90.0;
        if(decd < -90.0) decd = -90.0;
     }
     return new Retval(rad,decd);
}

    public static class Retval{
          public double _ra;
          public double _dec;
          public Retval(double ra, double dec) {
             _ra= ra;
             _dec= dec;
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
