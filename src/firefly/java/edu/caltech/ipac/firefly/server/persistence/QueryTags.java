/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.firefly.server.query.Query;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.rpc.UserServicesImpl;
import edu.caltech.ipac.table.DataGroup;

/**
 * @author tatianag
 *         $Id: QueryTags.java,v 1.8 2011/06/07 20:31:19 loi Exp $
 */
@SearchProcessorImpl(id ="tags")
public class QueryTags  extends EmbeddedDbProcessor implements Query {

    public String getTemplateName() {
        return "tags";
    }

    public DbInstance getDbInstance() {
        return DbInstance.operation;
    }

    public String getSql(TableServerRequest request) {
        return "select tagid, tagname, historytoken, istag, numhits, timecreated, timeused, description from tags where createdBy = ? and appname = ? order by timecreated desc";
    }

    public Object[] getSqlParams(TableServerRequest request) {
        String createdBy = UserServicesImpl.getCreatedBy();
        return new Object[]{createdBy, ServerContext.getAppName()};
    }

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        return executeQuery(req);
    }
}
