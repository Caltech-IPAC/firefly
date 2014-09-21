package edu.caltech.ipac.firefly.data;

/**
 * @author tatianag
 *         $Id: $
 */
public class SDSSRequest extends TableServerRequest {

    public final static String RADIUS_ARCMIN = "radiusArcMin";
    public final static String NEAREST_ONLY = "nearest_only";
    public final static String FILE_NAME = "filename";

    public SDSSRequest() {
        super("SDSSQuery");
        this.setPageSize(0);
    }

    public void setRadiusArcmin(double radiusArcMin) {
        setParam(RADIUS_ARCMIN, radiusArcMin + "");
    }

    public void setUserTargetWorldPoint(String wpStr) {
        setParam(ReqConst.USER_TARGET_WORLD_PT, wpStr);
    }

    public void setFilename(String filename) {
        setParam(FILE_NAME, filename);
    }


    public void setNearestOnly(boolean nearestOnly) {
        setParam(NEAREST_ONLY, nearestOnly+"");
    }
}
