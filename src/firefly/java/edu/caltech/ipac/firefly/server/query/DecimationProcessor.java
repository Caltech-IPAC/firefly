/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.expr.Expression;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@SearchProcessorImpl(id = "DecimateTable")
public class DecimationProcessor extends TableFunctionProcessor {
    public static final String DECIMATE_INFO = "decimate";


    protected String getResultSetTablePrefix() {
        return "deci";
    }

    protected DataGroup fetchData(TableServerRequest treq, File dbFile, DbAdapter dbAdapter) throws DataAccessException {

        DecimateInfo decimateInfo = getDecimateInfo(treq);
        TableServerRequest sreq = getSearchRequest(treq);
        sreq.setPageSize(Integer.MAX_VALUE);

        // only read in the required columns
        Expression xColExpr = new Expression(decimateInfo.getxColumnName(), null);
        Expression yColExpr = new Expression(decimateInfo.getyColumnName(), null);
        List<String> requestedCols = new ArrayList<>();
        if (xColExpr.isValid() && yColExpr.isValid()) {
            requestedCols.addAll(xColExpr.getParsedVariables());
            requestedCols.addAll(yColExpr.getParsedVariables());
        }
        sreq.setInclColumns(requestedCols.toArray(new String[requestedCols.size()]));
        DataGroup dg = new SearchManager().getDataGroup(sreq).getData();


        if (decimateInfo != null) {
            DataGroup retval = QueryUtil.doDecimation(dg, decimateInfo);
            return retval;
        } else {
            return dg;
        }
    }

    public static DecimateInfo getDecimateInfo(ServerRequest req) {
        return req.containsParam(DECIMATE_INFO) ? DecimateInfo.parse(req.getParam(DECIMATE_INFO)) : null;
    }

    public static void setDecimateInfo(ServerRequest req, DecimateInfo decimateInfo) {
        if (decimateInfo == null) {
            req.removeParam(DECIMATE_INFO);
        } else {
            req.setParam(DECIMATE_INFO, decimateInfo.toString());
        }
    }

}



