package edu.caltech.ipac.visualize.plot;

/**
 * This class contains a image point.
 * A image point is the point in the image data.
 * 0,0 is at the lower left corner of the lower left pixel
 */
public final class ImagePt extends Pt {
    public ImagePt() { this(0,0); }
    public ImagePt(double fsamp, double fline) { super(fsamp,fline); }

    private ImagePt(Pt p) { this(p.getX(),p.getY()); }

    public double getFsamp() { return getX(); }
    public double getFline() { return getY(); }

    public static ImagePt parse(String serString) {
        Pt p= Pt.parse(serString);
        return p==null ? null : new ImagePt(p);
    }

}
