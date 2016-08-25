/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;

/**
 * Request class to transport and gets the parameters related to LC
 * @author ejoliet
 */
public class PeriodogramAPIRequest extends TableServerRequest {

    public final static String OUTPUT_MODE = "output_`mode";

    public final static String TABLE_NAME = "table_name";
    // API url
    public final static String URL = "url";

    public final static String LC_FILE = "original_table";
    // Periodogram table
    public static final String RESULT_TABLE = "result_table";

    public static final String FOLDED_TABLE = "folded_table";
    public final static String PERIOD_IN_DAYS = "period_days";
    public final static String TIME_COLUMN_NAME = "time_col_name";

    // Input table raw LC flux vs time, should at least contain flux and time!
    public static final String TIME_DEPENDENT_TABLE = "input_table";

    public final static String ALGO_NAME = "algo_name";

    // number of peaks (default = 50)
    public final static String NUMBER_PEAKS = "n_peaks";
    public final static String MINIMUM_PERIOD = "min_period";
    public final static String MAXIMUM_PERIOD = "max_period";

    public final static String STEP_METHOD_NAME = "step_method";
    public final static String STEP_SIZE = "step_size";


    public PeriodogramAPIRequest() {
        super(PeriodogramAPIRequest.class.getSimpleName());
    }

    public String getUrl() {
        return getParam(URL);
    }

    /**
     * Period value
     *
     * @return period in days
     */
    public float getPeriod() {
        return getFloatParam(PERIOD_IN_DAYS);
    }

    /**
     * @return lc original table file path
     */
    public String getLcSource() {
        return getParam(LC_FILE);
    }

    /**
     * Usually 'mjd', but who knows...
     *
     * @return name of the time column
     */
    public String getTimeColName() {
        return getParam(TIME_COLUMN_NAME);
    }

    public float getMinimumPeriod() {
        return getFloatParam(MINIMUM_PERIOD);
    }

    public float getMaximumPeriod() {
        return getFloatParam(MAXIMUM_PERIOD);
    }

    public int getNumberPeaks() {
        return getIntParam(NUMBER_PEAKS);
    }

    public String getResultTable() {
        return getParam(RESULT_TABLE);
    }
}
