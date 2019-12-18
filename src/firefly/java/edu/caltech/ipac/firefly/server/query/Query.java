/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.table.DataGroup;

import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.dbToDataGroup;

/**
 * @author tatianag
 * $Id: Query.java,v 1.4 2010/04/13 16:59:25 roby Exp $
 */
public interface Query {

    /**
     * @return the type of data returned.  this is used to generate headers information.
     */
    String getTemplateName();

    DbInstance getDbInstance();
    /*
     * @return sql to construct prepared statement
     */
    String getSql(TableServerRequest request);

    /*
     * @return sql parameters for this query
     */
    Object [] getSqlParams(TableServerRequest request);

    /**
     * @return the query string to gather data definition info
     */
    default String getDDSql(TableServerRequest request) {
        return null;
    }

    /**
     * @param request  a table request
     * @return the results of this request in the form of a DataGroup table.
     */
    default DataGroup executeQuery(TableServerRequest request) {
        return (DataGroup) JdbcFactory.getTemplate(getDbInstance()).query(getSql(request), getSqlParams(request), rs -> {
            return dbToDataGroup(rs, getDbInstance(), getDDSql(request));
        });
    }

}
