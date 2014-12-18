package edu.caltech.ipac.visualize.plot;

/**
 * User: roby
 * Date: Apr 30, 2007
 * Time: 11:11:21 AM
 */


/**
 * This class contains a image workspace point.
 * A image workspace point where this images sits in relation to the set
 * of images begin displayed. The points have a one-to-one relationship to
 * a ImagePt but will have different offsets.
 */
public final class ImageWorkSpacePt extends Pt{
   public ImageWorkSpacePt() { super(0,0); }
   public ImageWorkSpacePt(double x, double y) { super(x,y); }
   private ImageWorkSpacePt(Pt p) { this(p.getX(),p.getY()); }

    public static ImageWorkSpacePt parse(String serString) {
        Pt p= Pt.parse(serString);
        return p==null ? null : new ImageWorkSpacePt(p);
    }
}

