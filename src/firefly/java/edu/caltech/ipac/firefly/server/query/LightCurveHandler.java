/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import java.io.File;

/**
 * Class should handle the input user parameter to
 * call an API to
 * get at least the periodogram and peaks table
 * out of a raw time changin flux curve
 *
 * @author ejoliet
 * @see PeriodogramAPIRequest
 */
public interface LightCurveHandler {
    /**
     * return a periodogram table from a request
     *
     * @return periodogram (power vs period) file
     */
    public File getPeriodogramTable(PeriodogramAPIRequest request);

    /**
     * Return the table which contains N peaks, N integer from request object
     *
     * @return peaks table
     * @see PeriodogramAPIRequest#getNumberPeaks()
     */
    public File getPeaksTable(PeriodogramAPIRequest request);

    /**
     * TODO add extra output parameters getter that might be interesting
     */
}
