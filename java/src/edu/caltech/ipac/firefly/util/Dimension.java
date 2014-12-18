package edu.caltech.ipac.firefly.util;

/**
 * Date: Sep 9, 2011
*
* @author loi
* @version $Id: Dimension.java,v 1.1 2011/09/09 22:04:51 loi Exp $
*/
public class Dimension {
    private final int width;
    private final int height;
    public Dimension(int width, int height) {
        this.width= width;
        this.height= height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean equals(Object o) {
        boolean retval= false;
        if (o!=null && o instanceof Dimension) {
            Dimension other= (Dimension)o;
            retval= (width==other.width && height==other.height);
        }
        return retval;
    }

    @Override
    public String toString() {
        return width+"x"+height;
    }
}
