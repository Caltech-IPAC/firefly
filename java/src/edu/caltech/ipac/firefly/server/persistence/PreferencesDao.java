package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.userdata.Preference;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tatianag
 * @version $Id: PreferencesDao.java,v 1.1 2011/11/10 16:38:19 tatianag Exp $
 */
public class PreferencesDao {

    private static PreferencesDao instance;

    public PreferencesDao() {}

    public static PreferencesDao getInstance() {
        if (instance == null) instance = new PreferencesDao();
        return instance;
    }


    public Status updatePreference(final String loginName, String prefname, String prefvalue) {
        Status status = null;
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        if (jdbcTemplate == null) {
            return new Status(1,"No database defined.  Failed to update preference loginid/prefname/prefvalue = \"+id+\"/\"+prefname+\"/\"+prefvalue");
        }

        try {
            int n = jdbcTemplate.queryForInt(
                    "select count(prefid) from preferences where loginname = ? and prefname = ?",
                    new Object[]{loginName, prefname});
            String updateSql;
            int affected = 1;
            if (n == 0) {
                if ((prefvalue != null && prefvalue.length()>0)) {
                    updateSql = "insert into preferences (loginname, prefname, prefvalue) values (?, ?, ?)";
                    affected = jdbcTemplate.update(updateSql, new Object[]{loginName, prefname, prefvalue});
                }
            } else if (n > 0) {
                if (prefvalue != null && prefvalue.length()>0) {
                    updateSql = "update preferences set prefvalue=? where loginname=? and prefname=?";
                    affected = jdbcTemplate.update(updateSql, new Object[]{prefvalue, loginName, prefname});
                } else {
                    updateSql = "delete from preferences where loginname=? and prefname=?";
                    affected = jdbcTemplate.update(updateSql, new Object[]{loginName, prefname});
                }
            }
            if (affected < 1) {
                Logger.error("Failed to update preference loginid/prefname/prefvalue = "+loginName+"/"+prefname+"/"+prefvalue);
                status = new Status(1,"Failed to update preference loginid/prefname/prefvalue = \"+id+\"/\"+prefname+\"/\"+prefvalue");
            } else {
                status = new Status();
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to update preference loginid/prefname/prefvalue = "+loginName+"/"+prefname+"/"+prefvalue);
        }

        return status;
    }

    public Status updatePreferences(final String loginName, final Map<String, String> prefmap) {
        Status status = null;
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        if (jdbcTemplate == null) {
            return new Status(1,"No database defined.  Failed to update preferences.");
        }
        try {
            final Map<String, String> prefs = getPreferences(loginName);
            final List<String> toUpdate = new ArrayList<String>();
            final List<String> toInsert = new ArrayList<String>();
            final List<String> toDelete = new ArrayList<String>();

            // if the value is null - delete preference
            for (String s : prefmap.keySet()) {
                if (prefs.containsKey(s)) {
                    String val = prefmap.get(s);
                    if (val==null || val.trim().length()==0) {
                        toDelete.add(s);
                    } else {
                        toUpdate.add(s);
                    }
                } else {
                    String val = prefmap.get(s);
                    if (val != null && val.trim().length()>0) {
                        toInsert.add(s);
                    }
                }
            }

            //int [] nInserted, nUpdated;
            if (toInsert.size() > 0) {
                //nInserted =
                jdbcTemplate.batchUpdate("insert into preferences (loginName, prefname, prefvalue) values (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        String prefname = toInsert.get(i);
                        String prefvalue = prefmap.get(prefname);
                        ps.setString(1, loginName);
                        ps.setString(2, prefname);
                        ps.setString(3, prefvalue);
                    }

                    public int getBatchSize() {
                        return toInsert.size();
                    }

                });
            }

            if (toUpdate.size() > 0) {
                //nUpdated =
                    jdbcTemplate.batchUpdate("update preferences set prefname = ?, prefvalue = ? where loginName = ? and prefname = ?",
                    new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        String prefname = toUpdate.get(i);
                        String prefvalue = prefmap.get(prefname);
                        ps.setString(1, prefname);
                        ps.setString(2, prefvalue);
                        ps.setString(3, loginName);
                        ps.setString(4, prefname);
                    }

                    public int getBatchSize() {
                        return toUpdate.size();
                    }

                });
            }

            if (toDelete.size() > 0) {
                //nUpdated =
                    jdbcTemplate.batchUpdate("delete from preferences where loginName = ? and prefname = ?",
                    new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, loginName);
                        String prefname = toDelete.get(i);
                        ps.setString(2, prefname);
                    }

                    public int getBatchSize() {
                        return toDelete.size();
                    }

                });
            }

            status = new Status();

        } catch (Exception e) {
            Logger.error(e, "Failed to update preferences.");
        }

        return status;
    }


    public Map<String,String> getPreferences(final String loginName) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        if (jdbcTemplate == null) return null;

        HashMap<String, String> prefs;
        Collection col = jdbcTemplate.query(
                "select prefname, prefvalue from preferences where loginname = ?",
                new Object[]{loginName},
                new RowMapper() {
                    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Preference(rs.getString("prefname"), rs.getString("prefvalue"), loginName);
                    }
                });
        if  (col != null && col.size()>0) {
            final Preference [] lst = (Preference[]) col.toArray(new Preference[col.size()]);
            prefs = new HashMap<String, String>(col.size());
            for (Preference p : lst) {
                prefs.put(p.getPrefname(), p.getPrefvalue());
            }
        }
        else {
            prefs = new HashMap<String, String>(0);
        }
        return prefs;
    }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
