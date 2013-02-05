package edu.caltech.ipac.firefly.server.util;

import healpix.core.HealpixIndex;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 2/4/13
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class HealpixWrapper {
    public enum Type {RING, NESTED}
    public enum FileType {LFI, HFI}
    public static final double PI = Math.PI;
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
                pix = getHealPixel(glon, glan, 1024, Type.RING);
                break;
            case HFI:
                pix = getHealPixel(glon, glan, 2048, Type.RING);
                break;
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

        if ( glon < 0.0 || glon > 360.0 ) {
            throw new Exception(glon+" is out of range (0..360).");
        }
        if ( glan < -90.0 || glan > 90.0 ) {
            throw new Exception(glan+" is out of range (-90..90).");
        }

        /* Convert to radians and theta is co-latitude (0 down from N.)
         */

        glon = glon/360.0*2.0*PI;
        glan = glan/360.0*2.0*PI;
        glan = PI/2.0 - glan;
        HealpixIndex hi = new HealpixIndex(nside);
        if (type.equals(Type.RING))
            pix = hi.ang2pix_ring(glan, glon);
        else
            pix = hi.ang2pix_nest(glan, glon);

        return pix;
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