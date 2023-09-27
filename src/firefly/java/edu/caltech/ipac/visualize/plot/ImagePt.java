/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

/**
 * This class contains a image point.
 * An image point is the point in the image data.
 * 0,0 is in the lower left corner of the lower left pixel
 */
public final class ImagePt extends Pt {
    public ImagePt(double fsamp, double fline) { super(fsamp,fline); }

    public double getFsamp() { return getX(); }
    public double getFline() { return getY(); }

    public static ImagePt parse(String serString) {
        Pt p= Pt.parse(serString);
        return p==null ? null : new ImagePt(p.getX(),p.getY());
    }

}
