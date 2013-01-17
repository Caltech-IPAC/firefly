package edu.caltech.ipac.firefly.server.util;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 12/13/12
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */

import java.io.Serializable;
import java.util.Arrays;

public class HealpixIndex implements Serializable {
    public enum Type {RING, NESTED}
    public enum FileType {LFI, HFI}

    public static final double PI = Math.PI;
    public static final double TWO_PI = 2.*PI;
    public static final double TWO_THIRD = 2. / 3.;
    public static final double PI_OVER_2 = PI / 2.;

    public static final int NS_MAX = 536870912;// 1048576;
    /**
     * Default serial version
     */
    private static final long serialVersionUID = 2L;


    /** Available nsides ..always power of 2 ..**/
    public static int[] nsidelist = { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048,
            4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288,
            1048576, 2097152, 4194304, 8388608, 16777216, 33554432,
            67108864, 134217728,  268435456, 536870912 };

    // coordinate of the lowest corner of each face
    int jrll[] = {  2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4 };
    int jpll[] = {  1, 3, 5, 7, 0, 2, 4, 6, 1, 3, 5, 7 };

    /** The Constant z0. */
    public static final double z0 = TWO_THIRD; // 2/3

    static short ctab[], utab[];

    protected int order;
    /** The nside. */
    public int nside = 1024;
    /** The ncap. */
    protected long  nl2, nl3, nl4, npface, npix, ncap;
    /** The fact2. */
    protected double fact1, fact2;

    /**
     *
     * @param glon
     * @param glan
     * @param fileType
     * @return
     * @throws Exception
     */
    public static long getHealPixelForPlanckImageCutout(double glon, double glan, FileType fileType) throws Exception{
        long pix = -1;
        switch (fileType) {
            case LFI:
                pix = getHealPixel(glon, glan, 1024, Type.NESTED);
                break;
            case HFI:
                pix = getHealPixel(glon, glan, 2048, Type.RING);
                break;
        }
        return pix;
    }


    /**
     * Retr nieve Healpix Index for Planck Image Cutout Search
     * @param glon longitude (RA)
     * @param glan latitude (Dec)
     * @param freq Planck Image Frequency
     * @return HealPix Index
     * @throws Exception
     */
    public static long getHealPixelForPlanckImageCutout(double glon, double glan, int freq) throws Exception{
        long pix = -1;
        switch (freq) {
            case 30:
            case 44:
            case 70:
                pix = getHealPixel(glon, glan, 1024, Type.NESTED);
                break;
            case 100:
            case 143:
            case 217:
            case 353:
            case 545:
            case 857:
                pix = getHealPixel(glon, glan, 2048, Type.RING);
                break;
            default:
                throw new Exception(freq+"GHz is unsupported for Planck Image Cutout HealPix Index Search.");
        }
        return pix;
    }

    /**
     *
     * @param glon longitude (RA)
     * @param glan latitude (Dec)
     * @param nside
     * @param type
     * @return
     * @throws Exception
     */
    public static long getHealPixel(double glon, double glan, int nside, Type type) throws Exception {
        long pix = -1;
        double pi = PI;
        if ( glon < 0.0 || glon > 360.0 ) {
            throw new Exception(glon+" is out of range (0..360).");
        }
        if ( glan < -90.0 || glan > 90.0 ) {
            throw new Exception(glan+" is out of range (-90..90).");
        }

        /* Convert to radians and theta is co-latitude (0 down from N.)
         */

        glon = glon/360.0*2.0*pi;
        glan = glan/360.0*2.0*pi;
        glan = pi/2.0 - glan;
        HealpixIndex hi = new HealpixIndex(nside);
        if (type.equals(Type.RING))
            pix = hi.ang2pix_ring(glan, glon);
        else
            pix = hi.ang2pix_nest(glan, glon);

        return pix;
    }

    /**
     * Inits the.
     */
    protected void init() {
        // tablefiller
        int tabmax=0x100;
        ctab=new short[tabmax];
        utab=new short[tabmax];
        for (int m=0; m<0x100; ++m)
        {
            ctab[m] =(short)(
                    (m&0x1 )        | ((m&0x2 ) << 7) | ((m&0x4 ) >> 1) | ((m&0x8 ) << 6) |
                    ((m&0x10) >> 2) | ((m&0x20) << 5) | ((m&0x40) >> 3) | ((m&0x80) << 4));
            utab[m] = (short)(
                    (m&0x1 )        | ((m&0x2 ) << 1) | ((m&0x4 ) << 2) | ((m&0x8 ) << 3) |
                    ((m&0x10) << 4) | ((m&0x20) << 5) | ((m&0x40) << 6) | ((m&0x80) << 7));
        }
        // end tablefiller
        nl2 = 2 * nside;
        nl3 = 3 * nside;
        nl4 = 4 * nside;
        npface = (long)nside * (long)nside;
        ncap = 2 * (long)nside * ( (long)nside - 1 );// points in each polar cap, =0 for
        // nside =1
        npix =  12 * npface ;
        fact2 = 4.0 / npix;
        fact1 = (nside << 1) * fact2;

        order = nside2order(nside);
    }

    /**
     * Gets the order from the nside
     * @param nside
     * @return order
     */
    public static int nside2order(int nside) {
        int ord=0;
        assert (nside > 0);
        if ( ((nside)&(nside-1)) > 0 ) {
            return -1;
        }
        // ok c++ uses a a log - lookup should be better and
        // we do not have iog2 in java
        // the posiiton in the array of nsides is the order !
        ord = Arrays.binarySearch(nsidelist,nside);
        ord = (int) log2(nside);
        return ord;
    }

    /**
     * Log base two
     * @param num
     * @return log2
     */
    public static double log2(double num) {
        return (Math.log(num) / Math.log(2));
    }

    /**
     * Default constructor nside = 1024.
     */
    public HealpixIndex() {
        init();
    }

    /**
     * Construct healpix routines tied to a given nside
     *
     * @param nSIDE2
     *            resolution number
     * @throws Exception
     */
    public HealpixIndex(int nSIDE2) throws Exception {
        if ( nSIDE2 > NS_MAX || nSIDE2 < 1 ) {
            throw new Exception("nsides must be between 1 and " + NS_MAX);
        }
        this.nside = nSIDE2;
        init();
    }

    /**
     * Set nside field.
     * @param nSide
     */
    public void setNside(int nSide) {
        this.nside = nSide;
    }

    /**
     * renders the pixel number ipix (RING scheme) for a pixel which contains a
     * point on a sphere at coordinates theta and phi, given the map resolution
     * parametr nside the computation is made to the highest resolution
     * available (nside=8192) and then degraded to that required (by integer
     * division) this doesn't cost more, and it makes sure that the treatement
     * of round-off will be consistent for every resolution
     *
     * @param theta
     *            angle (along meridian), in [0,Pi], theta=0 : north pole
     * @param phi
     *            angle (along parallel), in [0,2*Pi]
     * @return pixel index number
     * @throws Exception
     */
    public long ang2pix_ring(double theta, double phi) throws Exception {

        if ( nside < 1 || nside > NS_MAX)
            throw new Exception("nside out of range");
        if ( theta < 0.0 || theta > PI )
            throw new Exception("theta out of range");

        long ipix;
        long jp, jm, ir, ip;
        double z, za, tt, tp, tmp, temp1, temp2;
        int  kshift;

        // -----------------------------------------------------------------------
        z = Math.cos(theta);
        za = Math.abs(z);
        if ( phi >= TWO_PI)
            phi = phi - TWO_PI;
        if ( phi < 0. )
            phi = phi + TWO_PI;
        tt = phi / PI_OVER_2;// in [0,4)

        if ( za <= z0 ) {
            temp1 = nside*(0.5+tt);
            temp2 = nside*z*0.75;
            jp = (long)(temp1-temp2); // index of  ascending edge line
            jm = (long)(temp1+temp2); // index of descending edge line

            ir = nside + 1 + jp - jm;// in {1,2n+1} (RING number counted from
            // z=2/3)
            kshift = 1 - (int)(ir&1);

            ip = (long) ( (jp+jm-(long)nside+(long)kshift+1L)/2L);// in {1,4n}
            ip = ip % nl4;

            ipix = ncap + ( ir - 1 )* nl4 + ip;
            return ipix;

        }
        tp = tt - (int)( tt );// MOD(tt,1.0)
        tmp = (long)nside * Math.sqrt(3.0 * ( 1.0 - za ));

        jp = (long) (  tp * tmp );// increasing edge line index
        jm = (long) ( ( 1.0 - tp ) * tmp );// decreasing edge line index

        ir = jp + jm + 1L;// RING number counted from the closest pole
        ip = (long) ( tt * ir );// in {1,4*ir})
        ip = ip % (4L * ir);
        if ( z > 0.0 ) {
            ipix = 2L * ir * ( ir - 1L ) + ip;
        } else {
            ipix = npix - 2L * ir * ( ir + 1L ) + ip;
        }
        return ipix;
    }

    /**
     * renders the pixel number ipix ( scheme as defined for object)
     * for a pixel which contains
     * a point on a sphere at coordinates theta and phi, given the map
     * resolution parameter nside
     *
     * @param theta
     *            angle (along meridian), in [0,Pi], theta=0 : north pole
     * @param phi
     *            angle (along parallel), in [0,2*Pi]
     * @return pixel index number
     * @throws Exception
     */
    public long ang2pix_nest(double theta, double phi) throws Exception {
        long ipix;
        double z, za, tt, tp;
        long ifp, ifm;
        long jp, jm;
        int  ntt, face_num, ix, iy;

        if ( phi >= TWO_PI)
            phi = phi - TWO_PI;
        if ( phi < 0. )
            phi = phi + TWO_PI;
        if ( theta > PI || theta < 0 ) {
            throw new Exception("theta must be between 0 and " + PI);
        }
        if ( phi > TWO_PI || phi < 0 ) {
            throw new Exception("phi must be between 0 and " + TWO_PI);
        }
        // Note exception thrown means method does not get further.

        z = Math.cos(theta);
        za = Math.abs(z);
        tt = phi / PI_OVER_2;// in [0,4]


        // System.out.println("Za:"+za +" z0:"+z0+" tt:"+tt+" z:"+z+"
        // theta:"+theta+" phi:"+phi);
        if ( za <= z0 ) { // Equatorial region
            // System.out.println("Equatorial !");
            // (the index of edge lines increase when the longitude=phi goes up)
            double temp1 = nside*(0.5+tt);
            double temp2 = nside*(z*0.75);

            jp = (long) (temp1 - temp2);
            // ascending edge line index
            jm = (long) (temp1 + temp2);
            // descending edge line index

            // finds the face
            ifp = jp >> order; // in {0,4}
            ifm = jm >> order;
            if ( ifp == ifm ) { // faces 4 to 7
                face_num = (int)( ifp == 4 ?  4 : ifp+4);
            } else {
                if ( ifp < ifm ) { // (half-)faces 0 to 3
                    face_num = (int)ifp ;
                } else { // (half-)faces 8 to 11
                    face_num = (int) ifm   + 8;
                };
            };

            ix = (int)( jm & (nside -1));
            iy = (int) (nside - ( jp &  (nside -1 )) - 1);
        } else { // polar region, za > 2/3

            ntt = (int) ( tt );
            if ( ntt >= 4 )
                ntt = 3;
            tp = tt - ntt;
            double tmp = nside * Math.sqrt(3.0 * ( 1.0 - za ));

            // (the index of edge lines increase when distance from the closest
            // pole goes up)
            jp = (long) ( tp * tmp);// line going toward the
            // pole as phi increases
            jm = (long) (( 1.0 - tp ) * tmp); // that one goes
            // away of the closest pole
            jp = Math.min(NS_MAX - 1, jp);
            // for points too close to the boundary
            jm = Math.min(NS_MAX - 1, jm);

            // finds the face and pixel's (x,y)
            if ( z >= 0 ) { // North Pole
                // System.out.println("Polar z>=0 ntt:"+ntt+" tt:"+tt);
                face_num = ntt; // in {0,3}
                ix = (int) (nside - jm - 1);
                iy = (int) (nside - jp - 1);
            } else {
                // System.out.println("Polar z<0 ntt:"+ntt+" tt:"+tt);
                face_num = ntt + 8;// in {8,11}
                ix = (int)jp;
                iy = (int)jm;
            };
        };

        ipix = xyf2nest(ix,iy,face_num);

        return ipix;
    }

    protected  long xyf2nest(int ix, int iy, int face_num) {
        return ((long)(face_num)<<(2*order)) +
                    ( ((long)(utab[ ix     &0xff]))
                    | ((long)(utab[(ix>> 8)&0xff])<<16)
                    | ((long)(utab[(ix>>16)&0xff])<<32)
                    | ((long)(utab[(ix>>24)&0xff])<<48)
                    | ((long)(utab[ iy     &0xff])<<1)
                    | ((long)(utab[(iy>> 8)&0xff])<<17)
                    | ((long)(utab[(iy>>16)&0xff])<<33)
                    | ((long)(utab[(iy>>24)&0xff])<<49) );
    }

    public long nest2ring(long ipnest) throws Exception {
            Xyf xyf = nest2xyf(ipnest);
            long ipring = xyf2ring(xyf.ix,xyf.iy,xyf.face_num);
            return ipring;
        }

        private long xyf2ring(int ix, int iy, int face_num) {
            long jr = ((long)jrll[face_num]*(long)nside) - (long)ix - (long)iy  - 1L;

            long nr, kshift, n_before;
            if (jr<(long)nside)
              {
              nr = jr;
              n_before = 2*nr*(nr-1);
              kshift = 0;
              }
            else if (jr > 3*(long)nside)
              {
              nr = nl4-jr;
              n_before = npix - 2*(nr+1)*nr;
              kshift = 0;
              }
            else
              {
              nr = (long)nside;
              n_before = ncap + (jr-(long)nside)*nl4;
              kshift = (jr-(long)nside)&1;
              }

            long jp = ((long)jpll[face_num]*nr + (long)ix - (long)iy + 1L + (long)kshift) / 2L;
            if (jp>nl4)
              jp-=nl4;
            else
              if (jp<1) jp+=nl4;

            return n_before + jp - 1L;
        }

        private Xyf nest2xyf(long ipix) {

            Xyf ret = new Xyf();
            ret.face_num =(int)( ipix>>(2*order));
            long pix = ipix& (npface-1);
            // need o check the & here - they were unsigned in cpp ...
            int raw = (int)(((pix&0x555500000000L)>>16)
                         | ((pix&0x5555000000000000L)>>31)
                         | (pix&0x5555)
                         | ((pix&0x55550000)>>15));
              ret.ix =  ctab[raw&0xff]
                 | (ctab[(raw>>8)&0xff]<<4)
                 | (ctab[(raw>>16)&0xff]<<16)
                 | (ctab[(raw>>24)&0xff]<<20);
              pix >>= 1;
              raw = (int)(((pix&0x555500000000L)>>16)
                         | ((pix&0x5555000000000000L)>>31)
                         | (pix&0x5555)
                         | ((pix&0x55550000)>>15));
              ret.iy =  ctab[raw&0xff]
                 | (ctab[(raw>>8)&0xff]<<4)
                 | (ctab[(raw>>16)&0xff]<<16)
                 | (ctab[(raw>>24)&0xff]<<20);
            return ret;
        }

        public long ring2nest(long ipring) throws Exception {
            Xyf xyf=ring2xyf(ipring);
            return xyf2nest (xyf.ix, xyf.iy, xyf.face_num);
        }

        private Xyf ring2xyf(long pix) {
            Xyf ret = new Xyf();
            long iring, iphi, kshift, nr;


              if (pix<ncap) // North Polar cap
                {
                iring = (long)(0.5*(1+Math.sqrt(1L+2L*pix))); //counted from North pole
                iphi  = (pix+1) - 2*iring*(iring-1);
                kshift = 0;
                nr = iring;
                ret.face_num=0;
                long tmp = iphi-1;
                if (tmp>=(2L*iring))
                  {
                  ret.face_num=2;
                  tmp-=2L*iring;
                  }
                if (tmp>=iring) ++ret.face_num;
                }
              else if (pix<(npix-ncap)) // Equatorial region
                {
                long ip = pix - ncap;
                if (order>=0)
                  {
                  iring = (ip>>(order+2)) + (long)nside; // counted from North pole
                  iphi  = (ip&(nl4-1)) + 1;
                  }
                else
                  {
                  iring = (ip/(nl4)) + (long)nside; // counted from North pole
                  iphi  = (ip%(nl4)) + 1L;
                  }
                kshift = (iring+(long)nside)&1;
                nr = (long)nside;
                long ire = iring-(long)nside+1;
                long irm = nl2+2-ire;
                long ifm, ifp;
                if (order>=0)
                  {
                  ifm = (iphi - ire/2 + (long)nside -1) >> order;
                  ifp = (iphi - irm/2 + (long)nside -1) >> order;
                  }
                else
                  {
                  ifm = (iphi - ire/2 + (long)nside -1) / (long)nside;
                  ifp = (iphi - irm/2 + (long)nside -1) / (long)nside;
                  }
                if (ifp == ifm) // faces 4 to 7
                  ret.face_num = (ifp==4) ? 4 : (int)ifp+4;
                else if (ifp<ifm) // (half-)faces 0 to 3
                  ret.face_num = (int)ifp;
                else // (half-)faces 8 to 11
                  ret.face_num = (int)ifm + 8;
                }
              else // South Polar cap
                {
                long ip = npix - pix;
                iring = (long)(0.5*(1+Math.sqrt(2L*ip-1L))); //counted from South pole
                iphi  = 4L*iring + 1 - (ip - 2L*iring*(iring-1L));
                kshift = 0;
                nr = iring;
                iring = 2L*nl2-iring;
                ret.face_num=8;
                long tmp = iphi-1L;
                if (tmp>=(2L*nr))
                  {
                  ret.face_num=10;
                  tmp-=2L*nr;
                  }
                if (tmp>=nr) ++ret.face_num;
                }

              long irt = iring - ((long)jrll[ret.face_num]*(long)nside) + 1L;
              long ipt = 2L*iphi- (long)jpll[ret.face_num]*nr - kshift -1L;
              if (ipt>=nl2) ipt-=8L*(long)nside;

              ret.ix = (int)( (ipt-irt) >>1);
              ret.iy =(int) ((-(ipt+irt))>>1);

            return ret;
        }

        private class Xyf {
            public int ix;
            public int iy;
            public int face_num;
        }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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