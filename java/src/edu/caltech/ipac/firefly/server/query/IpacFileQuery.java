package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.IpacTableExtractor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataGroup;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: IpacFileQuery.java,v 1.22 2011/01/10 19:36:00 tatianag Exp $
 */
abstract public class IpacFileQuery extends IpacTablePartProcessor implements Query {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public String getTemplateName() {
        return null;
    }

    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File file = null;
        DataSource ds = getDataSource();
        if (ds == null) throw new DataAccessException("Unable to connect to database");
        try {
            boolean proceed = onBeforeQuery(request, ds);
            String sql = getSql(request);
            if (proceed && sql != null) {
                Object [] sqlParams = getSqlParams(request);
                DataGroup template = TemplateGenerator.generate(getTemplateName(), sql, ds);
                file = createFile(request);
                IpacTableExtractor.query(template, ds, file, request.getPageSize(), sql, sqlParams);
            } else {
                closeConnection(ds);  // make sure connection is close
            }
        } catch (Exception ex) {
            closeConnection(ds);
            // if no exception, IpacTableExtractor will close it upon complete.
            // do not close connection in finally{} clause.
            // IpacTableExtractor may return after spinning off another thread to complete the
            // query.. this will cause the connection to close prematurely.
            if (file != null) {
                try {
                    file.delete();
                } catch (Exception e) {
                    LOGGER.error(e, "Unable to delete file");
                }
            }
            throw new DataAccessException("Unable to get data", ex);
        }
        return file;
    }

    private void closeConnection(DataSource ds) {
        if (ds != null) {
            try {
                //SingleConnectionDataSource is always used
                Connection conn = ds.getConnection();
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e);
            }
        }
    }

//    private File createEmptyFile(TableServerRequest request) {
//        // TODO: better way to deal with empty results
//        File file = null;
//        try {
//            file = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getPermWorkDir());
//            DataGroupWriter.write(file, new DataGroup("Empty Set", new DataType[0]), request.getPageSize());
//        } catch (IOException e) {
//            LOGGER.error(e);
//        }
//        return file;
//    }

    /**
     * This method is called by loadData before any query related calls
     * @param request server request
     * @param datasource
     * @throws IOException on io problem
     * @throws DataAccessException on any other problem
     */
    @SuppressWarnings("unused")
    protected boolean onBeforeQuery(TableServerRequest request, DataSource datasource) throws IOException, DataAccessException {
        return true;
    }

    /**
     * @return a single connection datasource. this allow sub-routine to share
     * the same connection.
     */
    private DataSource getDataSource() {
        return JdbcFactory.getSingleConnectionDS(getDbInstance());
    }

}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/

