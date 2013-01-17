package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbInstance;

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

}
