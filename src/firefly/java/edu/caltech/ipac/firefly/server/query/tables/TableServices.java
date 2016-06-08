package edu.caltech.ipac.firefly.server.query.tables;

import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.util.StringUtils;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TableServices {


    @SearchProcessorImpl(id = "Table__SelectedValues", params ={
            @ParamDoc(name = "filePath", desc = "The path of the file on the server."),
            @ParamDoc(name = "selectedRows", desc = "Selected row indices separated by comma."),
            @ParamDoc(name = "columnName", desc = "The name of the column to get the data from."),
    })
    static public class SelectedValues extends JsonDataProcessor {
        public String getData(ServerRequest request) throws DataAccessException {
            String filePath = request.getParam("filePath");
            try {
                String selRows = request.getParam("selectedRows");
                String columnName = request.getParam("columnName");
                List<Integer> rows = StringUtils.convertToListInteger(selRows, ",");
                List<String> values =  new SearchManager().getDataFileValues(ServerContext.convertToFile(filePath), rows, columnName);
                return toJsonArray(values).toJSONString();
            } catch (IOException e) {
                throw new DataAccessException("Fail to retrieve data for file:" + filePath);
            }
        }

    }

}
