package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Nov 16, 2009
 *
 * @author loi
 * @version $Id: TemplateGenerator.java,v 1.7 2011/10/11 15:40:40 xiuqin Exp $
 */
public class TemplateGenerator {


    public static enum Tag {LABEL_TAG("col.@.Label"),
                      VISI_TAG("col.@.Visibility"),
                      DESC_TAG("col.@.ShortDescription"),
                      ITEMS_TAG("col.@.Items"),
                      UNIT_TAG("col.@.Unit");
        public static final String VISI_SHOW = "show";
        public static final String VISI_HIDDEN = "hidden";
        public static final String VISI_INVISIBLE = "invisible";
        String name;
        Tag(String name) { this.name = name;}

        public String getName() {
            return name;
        }
        public String generateKey(String col) {
            return getName().replaceFirst("@", col);
        }
    }

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final Map<String, String[]> enumColValues =  loadEnumColValues();


    public static DataGroup generate(String templateName, String querySql, DataSource dataSource) {

        if (StringUtils.isEmpty(templateName)) {
            return null;
        }
        try {
            CacheKey cacheKey = new StringKey("TemplateGenerator", templateName);
            Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
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

    public static DataGroup.Attribute createAttribute(Tag tag, String col, String value) {
        return new DataGroup.Attribute(tag.getName().replaceFirst(
                        "@", col), value);
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

                        // create format info
                        if (!dt.hasFormatInfo()) {
                            DataType.FormatInfo fInfo = dt.getFormatInfo();
                            String format = null;
                            if (colClass == Float.class || colClass == Double.class) {
                                int scale = Math.max(meta.getScale(i), 6);
                                int prec = Math.max(meta.getPrecision(i), cwidth);
                                format = "%" + prec + "." + scale + "f"; // double or float
                            } else if (Date.class.isAssignableFrom(colClass)) {
                                format = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS"; // date
                            }
                            if (format != null) {
                                fInfo.setDataFormat(format);
                            }
                        }

                        dt.getFormatInfo().setWidth(cwidth);

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

                // temporarily using Importance to indicate hidden status
                // HIGH is not hidden, MEDIUM is hidden, LOW is not visible(data transfer only)
                DataType.Importance isHidden = DataType.Importance.LOW;
                if (!StringUtils.isEmpty(sel)) {
                    if (sel.equals("y")) {
                        isHidden = DataType.Importance.HIGH;
                    } else if (sel.equals("n")) {
                        isHidden = DataType.Importance.MEDIUM;
                    }
                }


                DataType dt = new DataType(name, label, String.class, isHidden);
                dt.setShortDesc(desc);
                if (!StringUtils.isEmpty(format)) {
                    dt.getFormatInfo().setDataFormat(format);
                }

                return dt;
            }
        });

        DataGroup template = new DataGroup(templateName, headers);
        for (DataType dt : headers) {
            String visi = Tag.VISI_INVISIBLE;
            if (dt.getImportance() == DataType.Importance.HIGH) {
                visi = Tag.VISI_SHOW;
            } else if (dt.getImportance() == DataType.Importance.MEDIUM) {
                visi = Tag.VISI_HIDDEN;
            }

            template.addAttributes(createAttribute(Tag.VISI_TAG, dt.getKeyName(), visi));

            if (!StringUtils.isEmpty(dt.getShortDesc())) {
                template.addAttributes(createAttribute(Tag.DESC_TAG, dt.getKeyName(), dt.getShortDesc()));
            }

            dt.setImportance(DataType.Importance.HIGH);
            template.addAttributes(createAttribute(Tag.LABEL_TAG, dt.getKeyName(), dt.getDefaultTitle()));

            if ( enumColValues.containsKey(dt.getKeyName()) ) {
                template.addAttributes(createAttribute(Tag.ITEMS_TAG, dt.getKeyName(),
                        StringUtils.toString(enumColValues.get(dt.getKeyName()), ",")));
            }

            if (dt.hasFormatInfo()) {
                String fi = dt.getFormatInfo().getDataFormatStr();
                if (fi.equals("RA") || fi.equals("DEC")) {
                    dt.setFormatInfo(null);
                    template.addAttributes(createAttribute(Tag.UNIT_TAG, dt.getKeyName(), fi));
                }
            }

        }
        return template;
    }

    private static Map<String, String[]> loadEnumColValues() {
        HashMap<String, String[]> map = new HashMap<String, String[]>();

        map.put("wavelength",  new String[] {"IRAC 3.6um", "IRAC 4.5um", "IRAC 5.8um", "IRAC 8.0um",
                                 "IRS LH 18.7-37.2um", "IRS LL 14.0-21.7um", "IRS LL 14.0-38.0um", "IRS LL 19.5-38.0um",
                                 "IRS PU Blue 13.3-18.7um", "IRS PU Red 18.5-26.0um", 
                                 "IRS SH 9.9-19.6um", "IRS SL 5.2-14.5um", "IRS SL 5.2-8.7um", "IRS SL 7.4-14.5um",
                                 "MIPS 24um", "MIPS 70um", "MIPS 160um"}
                );
        map.put("modedisplayname", new String[] {"IRAC Map", "IRAC Map PC", "IRS Map", "IRS Stare", "IRS Peakup Image",
                         "MIPS Phot", "MIPS SED", "MIPS Scan", "MIPS TP", "IRAC IER","IRAC Post-Cryo IER", "IRS IER","MIPS IER"}
                );
        map.put("filetype", new String[] {"Image", "Table"});

        return map;
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
