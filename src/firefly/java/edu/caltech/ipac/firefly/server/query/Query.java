/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.table.DataGroup;
import org.springframework.jdbc.core.JdbcTemplate;

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
        JdbcTemplate jdbc = JdbcFactory.getTemplate(getDbInstance());
        DataGroup dg = (DataGroup) jdbc.query(getSql(request), getSqlParams(request), rs -> {
            return dbToDataGroup(rs, getDbInstance());
        });
        String ddSql = getDDSql(request);
        if (ddSql != null) {
            try {
                jdbc.query(ddSql, (ddrs, i) -> (Object) EmbeddedDbUtil.dbToDD(dg, ddrs));
            } catch (Exception e) {
                // ignore.. may not have DD table
            }
        }
        return dg;
    }

}
