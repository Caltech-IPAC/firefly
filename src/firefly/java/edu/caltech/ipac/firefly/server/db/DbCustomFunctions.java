/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.decimate.DecimateKey;

import java.io.File;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class DbCustomFunctions {

    private static final String [] customFunctions = {
            "CREATE FUNCTION decimate_key(xVal DOUBLE, yVal DOUBLE, xMin DOUBLE, yMin DOUBLE, nX INT, nY INT, xUnit DOUBLE, yUnit DOUBLE)\n" +
                    "RETURNS CHAR VARYING(20)\n" +
                    "RETURNS NULL ON NULL INPUT\n"+
                    "LANGUAGE JAVA DETERMINISTIC NO SQL\n" +
                    "EXTERNAL NAME 'CLASSPATH:edu.caltech.ipac.firefly.server.db.DbCustomFunctions.getDecimateKey'\n"
            ,
            "CREATE FUNCTION lg(val DOUBLE)\n" +
                    "RETURNS DOUBLE\n" +
                    "RETURNS NULL ON NULL INPUT\n" +
                    "LANGUAGE JAVA DETERMINISTIC NO SQL\n" +
                    "EXTERNAL NAME 'CLASSPATH:java.lang.Math.log10'\n"
    };

    private static final Logger.LoggerImpl logger = Logger.getLogger();

    public static String getDecimateKey(double xVal, double yVal, double xMin, double yMin, int nX, int nY, double xUnit, double yUnit) {
        return new DecimateKey(xMin, yMin, nX, nY, xUnit, yUnit).getKey(xVal, yVal);
    }


    /**
     * This is where you create stored function or procedure.
     * @param dbFile
     * @param dbAdapter
     */
    public static void createCustomFunctions(File dbFile, DbAdapter dbAdapter) {

        for (String cf : customFunctions) {
            try {
                JdbcFactory.getTemplate(dbAdapter.getDbInstance(dbFile)).execute(cf);
            } catch (Exception ex) {
                logger.error("Fail to create custom function:" + cf);
            }
        }
    }

}
