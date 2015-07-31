/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
abstract public class QueryFileInfoProcessor extends BaseFileInfoProcessor implements Query {
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();

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

