/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
     * @param glon GALACTIC longitude
     * @param glat GALACTIC latitude
     * @param fileType
     * @return
     * @throws Exception
     */
    public static long getHealPixelForPlanckImageCutout(double glon, double glat, FileType fileType) throws Exception{
        long pix = -1;
        switch (fileType) {
            case LFI:
                pix = getHealPixel(glon, glat, 1024, Type.RING);
                break;
            case HFI:
                pix = getHealPixel(glon, glat, 2048, Type.RING);
                break;
        }
        return pix;
    }

    /**
     *
     * @param glon GALACTIC longitude
     * @param glat GALACTIC latitude
     * @param nside
     * @param type
     * @return
     * @throws Exception
     */
    public static long getHealPixel(double glon, double glat, int nside, Type type) throws Exception {
        long pix = -1;

        if ( glon < 0.0 || glon > 360.0 ) {
            throw new Exception(glon+" is out of range (0..360).");
        }
        if ( glat < -90.0 || glat > 90.0 ) {
            throw new Exception(glat+" is out of range (-90..90).");
        }

        /* Convert to radians and theta is co-latitude (0 down from N.)
         */

        glon = glon/360.0*2.0*PI;
        glat = glat/360.0*2.0*PI;
        glat = PI/2.0 - glat;
        HealpixIndex hi = new HealpixIndex(nside);
        if (type.equals(Type.RING))
            pix = hi.ang2pix_ring(glat, glon);
        else
            pix = hi.ang2pix_nest(glat, glon);

        return pix;
    }
}

