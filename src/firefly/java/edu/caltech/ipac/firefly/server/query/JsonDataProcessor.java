/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.util.Logger;

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

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {}

    public QueryDescResolver getDescResolver() { return null;}

    public void onComplete(ServerRequest request, String results) throws DataAccessException {}

    public FileInfo writeData(OutputStream out, ServerRequest request, String format) throws DataAccessException { return null; }

    public boolean doCache() { return false; }

    public boolean doLogging() { return false; }

    public String getUniqueID(ServerRequest request) {
        return SearchProcessor.getUniqueIDDef((TableServerRequest) request);
    }

    abstract public String getData(ServerRequest request) throws DataAccessException;
}

