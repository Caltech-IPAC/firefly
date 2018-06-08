package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.JsonDataProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import org.json.simple.JSONArray;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * This search processor returns LSST projects/subprojects/tables information as a JSON string
 * @author tatianag
 */
@SearchProcessorImpl(id="LSSTTables")
public class LSSTTables extends JsonDataProcessor {
    @Override
    public String getData(ServerRequest request) throws DataAccessException {
        JSONArray arr = LSSTQuery.getJsonTables();
        if (arr == null) {
            throw new DataAccessException("Unable to retrieve general information on LSST data sets");
        } else {
            return arr.toJSONString();
        }
    }
}
