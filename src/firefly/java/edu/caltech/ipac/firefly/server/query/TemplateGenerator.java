/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Date: Nov 16, 2009
 *
 * @author loi
 * @version $Id: TemplateGenerator.java,v 1.7 2011/10/11 15:40:40 xiuqin Exp $
 */
public class TemplateGenerator {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public static DataGroup generate(String templateName, String querySql, DataSource dataSource) {

        if (StringUtils.isEmpty(templateName)) {
            return null;
        }
        try {
            CacheKey cacheKey = new StringKey("TemplateGenerator",  java.lang.System.currentTimeMillis() );
            Cache cache = CacheManager.getLocal();
            DataGroup template = (DataGroup) cache.get(cacheKey);
            if (template == null) {
                template = loadTemplate(templateName, dataSource);
                setupFormat(template, querySql, dataSource);
                cache.put(cacheKey, template);
            }
            return template;
        } catch (Exception e) {
            LOGGER.warn(e, "Unable to generate template for query:" + templateName);
        }
        return null;
    }

    private static void setupFormat(DataGroup template, String querySql, DataSource dataSource) throws SQLException {
            Connection conn = null;
            PreparedStatement ps = null;
            try {
                conn = dataSource.getConnection();
                ps = conn.prepareStatement(querySql);
                ResultSetMetaData meta = ps.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String cname = meta.getColumnName(i);
                    DataType dt = template.getDataDefintion(cname);
                    if (dt != null) {

                        Class colClass = String.class;
                        try {
                            colClass = Class.forName(meta.getColumnClassName(i));
                        } catch (ClassNotFoundException ex) {
                        }
                        dt.setDataType(colClass);

                        int cwidth = meta.getColumnDisplaySize(i);
                        cwidth = Math.max(cwidth, 6);
                        cwidth = Math.max(cwidth, cname.length());
                    }
                }
            } finally {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    DataSourceUtils.releaseConnection(conn, dataSource);
                }
            }
    }

    private static DataGroup loadTemplate(String templateName, DataSource dataSource) {

        String sql = "select name, display_name, description, sel, format  from " + templateName + " order by cntr asc";
        SimpleJdbcTemplate jdbc = new SimpleJdbcTemplate(dataSource);
        List<DataType> headers = jdbc.query(sql, new ParameterizedRowMapper<DataType>() {
            public DataType mapRow(ResultSet rs, int i) throws SQLException {
                String name = rs.getString("name");
                String label = rs.getString("display_name");
                String desc = rs.getString("description");
                String sel = rs.getString("sel");
                String format = rs.getString("format");

                if (StringUtils.isEmpty(name)) {
                    return null;
                }

                label = StringUtils.isEmpty(label) ? name : label;
                desc = StringUtils.isEmpty(desc) ? label : desc;

                DataType dt = new DataType(name, label, String.class);
                dt.setDesc(desc);

                if (StringUtils.areEqual(sel, "n")) {
                    dt.setVisibility(DataType.Visibility.hide);
                }
                if (!StringUtils.isEmpty(format)) {
                    if (format.equals("RA") || format.equals("DEC")) {
                        // this is weird.. should see how it's used.
                        dt.setUnits(format);
                    } else {
                        dt.setFormat(format);
                    }
                }
                return dt;
            }
        });

        return new DataGroup(templateName, headers);
    }
}
