package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 9/6/13
 * Time: 2:05 PM
 */


import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * @author Trey Roby
 */
public class MovingTargetContext {

    private WorldPt positionOnImage;
    private String name;


    public MovingTargetContext(WorldPt positionOnImage, String name) {
        this.positionOnImage = positionOnImage;
        this.name = name;
    }

    public WorldPt getPositionOnImage() {
        return positionOnImage;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) return true;

        boolean retval= false;
        if (obj instanceof MovingTargetContext) {
            MovingTargetContext mtc= (MovingTargetContext)obj;
            retval= ComparisonUtil.equals(positionOnImage,mtc.positionOnImage) &&
                    ComparisonUtil.equals(name,name);
        }
        return retval;
    }
}

