/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.decimate.DecimateKey;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class DbCustomFunctions {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static String getDecimateKey(double xVal, double yVal, double xMin, double yMin, int nX, int nY, double xUnit, double yUnit) {
        return new DecimateKey(xMin, yMin, nX, nY, xUnit, yUnit).getKey(xVal, yVal);
    }

}
