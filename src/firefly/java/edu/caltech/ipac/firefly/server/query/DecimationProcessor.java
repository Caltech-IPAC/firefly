/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataGroup;

import java.io.File;


@SearchProcessorImpl(id = "DecimateTable")
public class DecimationProcessor extends TableFunctionProcessor {
    public static final String DECIMATE_INFO = "decimate";


    protected String getResultSetTablePrefix() {
        return "deci";
    }

    protected DataGroup fetchData(TableServerRequest treq, File dbFile, DbAdapter dbAdapter) throws DataAccessException {

        DecimateInfo decimateInfo = getDecimateInfo(treq);
        TableServerRequest sreq = getSearchRequest(treq);
        sreq.setPageSize(Integer.MAX_VALUE);        // we want all of the data.  no paging
        // the "inclCols" in search request should be set by caller and handle column expressions if any
        if (sreq.getInclColumns() == null) {
            String x = decimateInfo.getxColumnName();
            String y = decimateInfo.getyColumnName();
            String [] requestedCols = x.equals(y) ? new String[]{"\""+x+"\""} : new String[]{"\""+x+"\",\""+y+"\""};
            sreq.setInclColumns(requestedCols);
        }
        
        DataGroupPart sourceData = new SearchManager().getDataGroup(sreq);
        if (sourceData == null) {
            throw new DataAccessException("Unable to get source data");
        } else if (sourceData.getErrorMsg() != null) {
            throw new DataAccessException(sourceData.getErrorMsg());
        }
        DataGroup dg = sourceData.getData();

        if (decimateInfo != null) {
            DataGroup retval = QueryUtil.doDecimation(dg, decimateInfo);
            retval.mergeAttributes(dg.getKeywords());
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



