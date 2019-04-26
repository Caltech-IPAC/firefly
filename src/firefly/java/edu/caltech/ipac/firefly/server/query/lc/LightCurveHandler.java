/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.table.DataGroup;

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
     * TODO check ordinals when API call is implemented
     * Represent the tables in votable result from API: 0 for periodogram, 1 for peaks
     */
    enum RESULT_TABLES_IDX {
        PERIODOGRAM, PEAKS
    }


    /**
     * return a periodogram table from a request
     *
     * @return periodogram (power vs period) file
     */
    DataGroup getPeriodogramTable(PeriodogramAPIRequest request);

    /**
     * Return the table which contains N peaks, N integer from request object
     *
     * @return peaks table
     * @see PeriodogramAPIRequest#getNumberPeaks()
     */
    DataGroup getPeaksTable(PeriodogramAPIRequest request);

    /**
     * TODO add extra output parameters getter that might be interesting
     */
}
