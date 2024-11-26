/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.util.FileUtil;

import java.io.IOException;

/**
 * Date: 11/3/24
 *
 * @author loi
 * @version : $
 */
public class DuckDbUDF {
    public static String deg2pix = importDeg2Pix();
    public static final String decimate_key = """
        CREATE FUNCTION decimate_key(xVal, yVal, xMin, yMin, nX, nY, xUnit, yUnit) AS
        TRUNC((xVal-xMin)/xUnit)::INT || ':' || TRUNC((yVal-yMin)/yUnit)::INT
    """;         // make sure this matches DecimateKey.getKey()
    public static final String lg = "CREATE FUNCTION lg(val) AS LOG10(val)";
    public static final String nvl2 = """
        CREATE FUNCTION nvl2(expr1, expr2, expr3) AS
        CASE
        WHEN expr1 IS NOT NULL THEN expr2
        ELSE expr3
        END
    """;

    private static String importDeg2Pix() {
        try {
            String deg2pixFile = "/edu/caltech/ipac/firefly/resources/healpix-java.sql";
            return FileUtil.readFile(DuckDbUDF.class.getResourceAsStream(deg2pixFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
