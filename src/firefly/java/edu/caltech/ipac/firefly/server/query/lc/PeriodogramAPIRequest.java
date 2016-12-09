/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.firefly.data.TableServerRequest;

/**
 * Request class to transport and gets the parameters related to LC
 *
 * @author ejoliet
 */
public class PeriodogramAPIRequest extends TableServerRequest {

    public final static String OUTPUT_MODE = "output_mode";

    public final static String TABLE_NAME = "table_name";
    // API url
    public final static String URL = "url";

    public final static String LC_FILE = "original_table";
    // Periodogram table
    public static final String RESULT_TABLE = "result_table";

    public static final String FOLDED_TABLE = "folded_table";
    public final static String PERIOD_IN_DAYS = "period_days";
    public final static String TIME_COLUMN_NAME = "x";
    public final static String DATA_COLUMN_NAME = "y";
    public final static String ERROR_COLUMN_NAME = "error_col_name";
    public final static String URL_IMAGE_SERVICE_COLUMN_NAME = "image_col_name";
    // Input table raw LC flux vs time, should at least contain flux and time!
    public static final String TIME_DEPENDENT_TABLE = "input_table";

    public final static String ALGO_NAME = "alg";// optional, default 'ls', ls (Lomb-Scargle), others bls (Box-fitting Least Squares), and plav (Plavchan 2008)

    // number of peaks (default = 50)
    public final static String NUMBER_PEAKS = "peaks";// optional
    public final static String MINIMUM_PERIOD = "pmin";// optional
    public final static String MAXIMUM_PERIOD = "pmax";// optional

    public final static String STEP_METHOD_NAME = "step_method";// optional
    public final static String STEP_SIZE = "step_size";// optional


    public PeriodogramAPIRequest() {
        super(PeriodogramAPIRequest.class.getSimpleName());
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

    /**
     * @return name of the flux or data column
     */
    public String getDataColName() {
        return getParam(DATA_COLUMN_NAME);
    }

    public String getStepMethodName() {
        return getParam(STEP_METHOD_NAME);
    }

    public int getSepSize() {
        return getIntParam(STEP_SIZE);
    }

    public float getMinimumPeriod() {
        return getFloatParam(MINIMUM_PERIOD);
    }

    public float getMaximumPeriod() {
        return getFloatParam(MAXIMUM_PERIOD);
    }

    public int getNumberPeaks() {
        return getIntParam(NUMBER_PEAKS, 50);
    }

    public String getResultTable() {
        return getParam(RESULT_TABLE);
    }

    public String getAlgoName() {
        return getParam(ALGO_NAME);
    }

}
