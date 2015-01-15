/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Date: Apr 28, 2009
 *
 * @author loi
 * @version $Id: ImageCorners.java,v 1.4 2009/06/15 22:29:33 booth Exp $
 */
public class ImageCorners implements Serializable {

    private ArrayList<WorldPt> corners;

    public ImageCorners() {
    }

    public ImageCorners(WorldPt... corners) {
        addCorners(corners);
    }

    /**
     * Adds all of the corners
     * @param points
     * @return Returns true if at least one is added.
     */
    public boolean addCorners(WorldPt... points) {
        if (corners == null) {
            corners = new ArrayList<WorldPt>();
        }
        return corners.addAll(Arrays.asList(points));
    }

    /**
     * Returns a list of unique corners
     * @return Returns a list of unique corners
     */
    public List<WorldPt> getCorners() {
        return Collections.unmodifiableList(corners);
    }

    public int size() {
        return corners == null ? 0 : corners.size();
    }
}
