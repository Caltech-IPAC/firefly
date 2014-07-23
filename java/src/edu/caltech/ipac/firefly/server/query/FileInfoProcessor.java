package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: FileInfoProcessor.java,v 1.9 2012/06/21 18:23:53 loi Exp $
 */
abstract public class FileInfoProcessor implements SearchProcessor<FileInfo>, Query {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    public FileInfo getData(ServerRequest sr) throws DataAccessException {
        try {
            TableServerRequest request= (TableServerRequest)sr;
            FileInfo fi = null;
            if (doCache()) {
                StringKey key = new StringKey(FileInfoProcessor.class.getName(), getUniqueID(request));
                Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
                fi = (FileInfo) cache.get(key);
            }
            if (fi == null) {
                fi = loadData(request);
            }
            onComplete(request, fi);
            return fi;
        } catch (Exception e) {
            LOGGER.error(e, "Error while processing request:" + sr);
            throw new DataAccessException("Request failed due to unexpected exception: ", e);
        }
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
    }

    public ServerRequest inspectRequest(ServerRequest request) {
        return request;
    }

    public String getUniqueID(ServerRequest request) {
        return request.getRequestId() + "-" + StringUtils.toString(request.getParams());
    }

    public void writeData(OutputStream out, ServerRequest request) throws DataAccessException {
        /* does not apply.. do nothing */
    }

    public boolean doCache() {
        /* does not apply.. do nothing */
        return false;
    }

    public void onComplete(ServerRequest request, FileInfo results) throws DataAccessException {
    }

    public boolean doLogging() {
        return false;
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        /* this only applies to table-based results... do nothing here */
    }

//====================================================================
//  implementing Query
//====================================================================

    protected FileInfo loadData(ServerRequest sr) throws IOException, DataAccessException {

        if (!(sr instanceof TableServerRequest)) {
            throw new IllegalArgumentException("FileInfoProcessor.loadData Requires an TableServerRequest");
        }
        TableServerRequest request= (TableServerRequest)sr;
        SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(getDbInstance());

        String sql = getSql(request);
        Object[] params = getSqlParams(request);

        final FileInfoRowMapper fim = makeRowMapper(request);
        ParameterizedRowMapper<FileInfo> mapper = new ParameterizedRowMapper<FileInfo>() {
            public FileInfo mapRow(ResultSet resultSet, int i) throws SQLException {
                return fim.mapRow(resultSet, i);
            }
        };

        LOGGER.info("Executing SQL query: " + sql,
                 "         Parameters: " + "{" + CollectionUtil.toString(params) + "}");
        FileInfo val = jdbc.queryForObject(sql, mapper, params);

        return val;

    }

    public String getTemplateName() {
        // this is not used.. for table-based only
        return null;
    }

    abstract protected FileInfoRowMapper makeRowMapper(TableServerRequest request);

    public static interface FileInfoRowMapper {
             FileInfo mapRow(ResultSet rs, int rowNum);
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