/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.DataGroup;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;

/**
 * We have a few search processors, which are accepting the search request as a parameter
 * This class contains the shared code
 * @author tatianag
 */
public class SearchRequestUtils {

    /**
     * Get data group created by the search request
     * @param searchRequestJson search request in JSON format
     * @return DataGroup data group
     * @throws IOException
     * @throws DataAccessException
     */
    public static DataGroup dataGroupFromSearchRequest(String searchRequestJson) throws IOException, DataAccessException {
        FileInfo fi = fileInfoFromSearchRequest(searchRequestJson);
        return DataGroupReader.readAnyFormat(new File(fi.getInternalFilename()));
    }

    /**
     * Get file created by the search request
     * @param searchRequestJson search request in JSON format
     * @return File file with the result table
     * @throws IOException
     * @throws DataAccessException
     **/
    public static File fileFromSearchRequest(String searchRequestJson) throws IOException, DataAccessException {
        FileInfo fi = fileInfoFromSearchRequest(searchRequestJson);
        return new File(fi.getInternalFilename());
    }

    /**
     * Get file info for the results created by the search request
     * @param searchRequestJson search request in JSON format
     * @return File file with the result table
     * @throws IOException
     * @throws DataAccessException
     **/
    static FileInfo fileInfoFromSearchRequest(String searchRequestJson)  throws IOException, DataAccessException {
        if (searchRequestJson == null) {
            throw new DataAccessException("Missing search request");
        }
        JSONObject searchRequestJSON = (JSONObject) JSONValue.parse(searchRequestJson);
        String searchId = (String) searchRequestJSON.get(ServerParams.ID);
        if (searchId == null) {
            throw new DataAccessException("Search request must contain " + ServerParams.ID);
        }
        TableServerRequest sReq = QueryUtil.convertToServerRequest(searchRequestJson);

        FileInfo fi = new SearchManager().getFileInfo(sReq);
        if (fi == null) {
            throw new DataAccessException("Unable to get file location info");
        }
        if (fi.getInternalFilename() == null) {
            throw new DataAccessException("File not available");
        }
        if (!fi.hasAccess()) {
            throw new SecurityException("Access is not permitted.");
        }

       return fi;
    }
}
