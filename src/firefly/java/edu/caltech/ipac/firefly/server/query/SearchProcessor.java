/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.util.DataType;

import java.io.OutputStream;
import java.util.List;

/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: SearchProcessor.java,v 1.3 2012/06/21 18:23:53 loi Exp $
 */
public interface SearchProcessor<Type> {

    ServerRequest inspectRequest(ServerRequest request);
    String getUniqueID(ServerRequest request);
    Type getData(ServerRequest request) throws DataAccessException;
    void writeData(OutputStream out, ServerRequest request) throws DataAccessException;
    boolean doCache();
    void onComplete(ServerRequest request, Type results) throws DataAccessException;
    boolean doLogging();
    void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request);
    QueryDescResolver getDescResolver();
}
