package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 14, 2012
 * Time: 5:54:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileGroupProcessorUtils {
    private static String DECIMAL_PRECISION = "%.6f";

    /**
     *
     * @param wpt
     * @param decimal
     * @return
     */
    public static String createRaDecString(WorldPt wpt, int decimal) {
        String decimalFormat = "%."+decimal+"f";
        return (String.format(decimalFormat, wpt.getLon())+"+"+String.format(decimalFormat, wpt.getLat())).
                replaceAll("\\+\\-","\\-");
    }
}
