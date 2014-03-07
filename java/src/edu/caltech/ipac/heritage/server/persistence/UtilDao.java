package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.ImageCornersMapper;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.ImageCorners;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility queries
 *
 * @author tatianag $Id: UtilDao.java,v 1.21 2011/10/11 15:40:40 xiuqin Exp $
 */
public class UtilDao {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();


    public static Map<String, String> getAbstractInfo(int progId) {

        SimpleJdbcTemplate openedJdbc = JdbcFactory.getSimpleTemplate(DbInstance.archive);
        String sql = "select first 1 progid, progtitle, progname, pi, sciencecat, abstract " +
                "from requestinformation r where progid = ?";

        LOGGER.info("Executing SQL query: " + sql + "progid=" + progId);
        Map<String, Object> hm = openedJdbc.queryForMap(sql, progId);
        Map<String, String> rval = new HashMap<String, String>();
        for (Map.Entry<String, Object> e : hm.entrySet()) {
            rval.put(e.getKey(), String.valueOf(e.getValue()));
        }
        return rval;
    }

    public static List<ImageCorners> getBcdCornersByRequestID(int requestID) {
        try {
            SimpleJdbcTemplate simpleTemplate = JdbcFactory.getSimpleTemplate(DbInstance.archive);

            String sql = "select ra1, dec1, ra2, dec2, ra3, dec3, ra4, dec4 from bcdproducts where reqkey = ?";

            LOGGER.info("Executing SQL query: " + sql);
            return simpleTemplate.query(sql, new ImageCornersMapper(), requestID);
        } catch (Exception e) {
            LOGGER.error(e, "getBcdCornersRequestID failed");
            return null;
        }
    }

    public static List<String> getObservers() {
        StringKey key = new StringKey("getObservers");
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        List<String> observers = (List<String>) cache.get(key);
        if (observers == null) {

            try {
                SimpleJdbcTemplate simpleTemplate = JdbcFactory.getSimpleTemplate(DbInstance.archive);
                String sql = "select unique pi from requestinformation";

                LOGGER.info("Executing SQL query: " + sql);
                observers = simpleTemplate.query(sql, new ParameterizedRowMapper<String>() {
                    public String mapRow(ResultSet rs, int i) throws SQLException {
                        return rs.getString("pi");
                    }
                });
                cache.put(key, observers);

            } catch (Exception e) {
                Logger.error(e, "getObservers failed");
                return null;
            }
        }
        return observers;
    }

    public static List<Integer> getBcdIds(Integer[] pbcdids) {
        SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(DbInstance.archive);

        String sql = "select bcdid from bcdproducts b, postbcdproducts p, dcesets s" +
                " where b.reqkey = p.reqkey" +
                " and   b.dceid = s.dceid" +
                " and   s.dcesetid = p.dcesetid";
        if (pbcdids.length == 1) {
            sql += " and   p.pbcdid = " + pbcdids[0];
        } else {
            sql += " and   p.pbcdid in (" + StringUtils.toString(pbcdids, ",") + ")";
        }

        LOGGER.info("Executing SQL query: " + sql);
        return jdbc.query(sql, new ParameterizedRowMapper<Integer>() {
            public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
                return resultSet.getInt("bcdid");
            }
        });

    }

    public static ProprietaryInfo getPropriertaryInfo() {
        ProprietaryInfo proprietary;

        JdbcTemplate openedJdbc = JdbcFactory.getTemplate(DbInstance.archive);
        String sql = "select reqkey, releasedate, progid " +
                "from requestinformation";

        LOGGER.info("Executing SQL query: " + sql);
        proprietary = (ProprietaryInfo) openedJdbc.query(sql, new ResultSetExtractor() {

            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                ProprietaryInfo proprietary = new ProprietaryInfo();
                while (rs.next()) {
                    proprietary.addData(rs.getString(1).trim(), rs.getString(3), rs.getDate(2));
                }
                return proprietary;
            }
        });
        return proprietary;
    }

    //    public static Map<String, List<String>> getProgramReqkeys() {
//        JdbcTemplate jdbc = JdbcFactory.getTemplate(DbInstance.archive);
//        String sql = "select progid, reqkey " +
//                "from requestinformation r where releasedate > current";
//
//        LOGGER.info("Executing SQL query: " + sql);
//        final Map<String, List<String>> retval = new HashMap<String, List<String>>();
//        jdbc.query(sql, new ResultSetExtractor() {
//                    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
//                        while (rs.next()) {
//                            String progid = rs.getString("progid").trim();
//                            List<String> l = retval.get(progid);
//                            if (l == null) {
//                                l = new ArrayList<String>();
//                                retval.put(progid, l);
//                            }
//                            l.add(rs.getString("reqkey").trim());
//                        }
//                        return null;
//                    }
//                });
//        return retval;
//    }
//
    /*
     * Get paths to the wavsamp calibration files for a given bcd
     * @param bcdid BCD ID
     * @return heritagefilenames of the wavsamp files
     */
    public static Collection<String> getBcdWavsamp(int bcdid) {
        String sql = "select heritagefilename from cal2bcd cp, calibrationproducts p " +
                "where cp.bcdid = " + bcdid + " and cp.cpid = p.cpid and heritagefilename like \"%wavsamp%fits\"";
        return getHeritageFilenames(sql);
    }

    /*
     * Get paths to the wavsamp calibration files for a given pbcd
     * @param bcdid PBCD ID
     * @return heritagefilenames of the wavsamp files
     */
    public static Collection<String> getPbcdWavsamp(int pbcdid) {
        String sql = "select heritagefilename from cal2postbcd cp, calibrationproducts p " +
                "where cp.pbcdid = " + pbcdid + " and cp.cpid = p.cpid and heritagefilename like \"%wavsamp%fits\"";
        return getHeritageFilenames(sql);
    }

    private static Collection<String> getHeritageFilenames(String sql) {
        try {
            SimpleJdbcTemplate simpleTemplate = JdbcFactory.getSimpleTemplate(DbInstance.archive);
            return simpleTemplate.query(sql, new ParameterizedRowMapper<String>() {
                public String mapRow(ResultSet rs, int i) throws SQLException {
                    return rs.getString("heritagefilename");
                }
            });
        } catch (Exception e) {
            Logger.error(e, "getHeritageFilenames: (" + sql + ") failed");
            return null;
        }
    }

}
