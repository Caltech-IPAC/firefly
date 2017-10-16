/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;


/**
 * Subclasses of this processor return JSONObject.  This helper class provides SearchProcessor's
 * supported features and helper functions.
 *
 * @author loi
 * @version $Id: IpacTablePartProcessor.java,v 1.33 2012/10/23 18:37:22 loi Exp $
 */
abstract public class JsonDataProcessor implements SearchProcessor<String> {

    public static final Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public ServerRequest inspectRequest(ServerRequest request) { return request; };

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {}

    public QueryDescResolver getDescResolver() { return null;}

    public void onComplete(ServerRequest request, String results) throws DataAccessException {}

    public FileInfo writeData(OutputStream out, ServerRequest request) throws DataAccessException { return null; }

    public boolean doCache() { return false; }

    public boolean doLogging() { return false; }

    public String getUniqueID(ServerRequest request) {
        return SearchProcessor.getUniqueIDDef((TableServerRequest) request);
    }

    abstract public String getData(ServerRequest request) throws DataAccessException;


    protected JSONArray toJsonArray(List values) {
        JSONArray jAry = new JSONArray();
        for (Object v : values) {
            if (v instanceof List) {

            } else if(v instanceof Map) {
                jAry.add(toJsonObject((Map) v));
            } else {
                jAry.add(v);
            }
        }
        return jAry;
    }

    protected JSONObject toJsonObject(Map values) {

        JSONObject jObj = new JSONObject();
        for (Object k : values.keySet()) {
            String name = String.valueOf(k);
            Object v = values.get(k);
            if (v instanceof List) {
                jObj.put(name, toJsonArray((List) v));
            } else if(v instanceof Map) {
                jObj.put(name, toJsonObject((Map) v));
            } else {
                jObj.put(name, v);
            }
        }
        return jObj;
    }

}

