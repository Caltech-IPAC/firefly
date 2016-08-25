package edu.caltech.ipac.firefly.server.query;

import java.io.File;

/**
 * Created by ejoliet on 8/22/16.
 * API handler to deal with result from calling LC API.
 * Result is a file or several that contain the power vs period and peaks table
 * The prefered output is votable containing those 2 tables
 */
public abstract class LightCurveAPIHandler implements LightCurveHandler {

    /**
     * return a periodogram table
     *
     * @return
     */
    public abstract File getPeriodogramTable();

    /**
     * @return
     */
    public abstract File getPeaksTable();

}
