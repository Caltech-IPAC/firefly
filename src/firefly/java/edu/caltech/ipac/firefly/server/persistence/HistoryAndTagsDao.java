/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.SearchInfo;
import edu.caltech.ipac.firefly.data.TagInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * @author tatianag
 * $Id: HistoryAndTagsDao.java,v 1.10 2011/06/07 00:53:02 loi Exp $
 */
public class HistoryAndTagsDao {
    private static final int MAX_DESC_COL_WIDTH = 2000;
    private static HistoryAndTagsDao instance;
    public static final String ADS_TAG_PREFIX = AppProperties.getProperty("ads.tag.prefix", "ADS/ADSArchiveName");

    public HistoryAndTagsDao() {

    }

    public static HistoryAndTagsDao getInstance() {
        if (instance == null) instance = new HistoryAndTagsDao();
        return instance;
    }

    public TagInfo addTag(final String createdBy, final String queryString, final String desc) {
        final JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        TransactionTemplate txTemplate = JdbcFactory.getTransactionTemplate(jdbcTemplate.getDataSource());
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    final String ndesc = desc != null && desc.length() > MAX_DESC_COL_WIDTH ?
                                    desc.substring(0, MAX_DESC_COL_WIDTH) : desc;
                    final String updateSql1 = "insert into tags (tagname, historytoken, description, istag, numhits, timecreated, createdby, appname) values (?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(new PreparedStatementCreator() {
                        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                            PreparedStatement stmt = connection.prepareStatement(updateSql1);
                            stmt.setString(1, UUID.randomUUID().toString());
                            stmt.setString(2, queryString);
                            stmt.setString(3, ndesc);
                            stmt.setBoolean(4, true);
                            stmt.setInt(5, 0);
                            stmt.setTimestamp(6, new Timestamp(new java.util.Date().getTime()));
                            stmt.setString(7, createdBy);
                            stmt.setString(8, ServerContext.getAppName());
                            return stmt;
                        }
                    }, keyHolder);
                    int generatedKey = keyHolder.getKey().intValue();
                    String updateSql2 = "update tags set tagname = ? where tagid = ?";
                    jdbcTemplate.update(updateSql2, new Object[]{getTagName(generatedKey), generatedKey});
                } catch (RuntimeException e) {
                    status.setRollbackOnly();
                    throw e;
                }
            }
        });
        String querySql = "select tagid, tagname, historytoken, description, istag, numhits, timecreated, timeused from tags where tagid = ?";
        return (TagInfo)jdbcTemplate.queryForObject(querySql, new Object[]{keyHolder.getKey().intValue()}, getTagsRowMapper());
    }

    public TagInfo getTag(String tagName) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        String querySql = "select tagid, tagname, historytoken, description, istag, numhits, timecreated, timeused from tags where tagname = ?";
        TagInfo tagInfo =  (TagInfo)jdbcTemplate.queryForObject(querySql, new Object[]{tagName}, getTagsRowMapper());
        try {
            String updateSql = "update tags set numhits = ?, timeused = ? where tagname = ?";
            jdbcTemplate.update(updateSql, new Object[]{(tagInfo.getNumHits()+1), new Timestamp(new java.util.Date().getTime()), tagName});
        } catch (Exception e) {
            Logger.error(e, "Unable to update tag statistics for "+tagInfo.getTagName());
        }
        return tagInfo;
    }

    public void removeTag(String createdBy, String tagName) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        String updateSql = "update tags set createdBy = concat('r_', createdBy) where tagname = ? and createdby like ?";
        jdbcTemplate.update(updateSql, new Object[]{tagName, createdBy});
    }

    public List<TagInfo> getTags(String createdBy) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        String querySql = "select tagid, tagname, historytoken, description, istag, numhits, timecreated, timeused from tags where createdBy = ?  order by timecreated desc";
        return (List<TagInfo>) jdbcTemplate.query(querySql, new Object[]{createdBy}, getTagsRowMapper());
    }

    public SearchInfo addSearchHistory(final String loginname, final String queryString, final String desc, final boolean isFavorite) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        final String ndesc = desc != null && desc.length() > MAX_DESC_COL_WIDTH ?
                            desc.substring(0, MAX_DESC_COL_WIDTH) : desc;
        final String updateSql = "insert into queryhistory (loginname, historytoken, description, favorite, timeadded, appname) values (?,?,?,?,?,?)";
        final java.util.Date now = new java.util.Date();
        jdbcTemplate.update(new PreparedStatementCreator(){
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                PreparedStatement stmt = connection.prepareStatement(updateSql);
                stmt.setString(1, loginname);
                stmt.setString(2, queryString);
                stmt.setString(3, ndesc);
                stmt.setBoolean(4, isFavorite);
                stmt.setTimestamp(5, new Timestamp(now.getTime()));
                stmt.setString(6, ServerContext.getAppName());
                return stmt;
            }
        }, keyHolder);
        return new SearchInfo(keyHolder.getKey().intValue(), loginname, queryString, desc, isFavorite, now);
    }

    public SearchInfo getSearch(int searchId) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        String querySql = "select * from queryhistory where queryid = ?";
        return (SearchInfo)jdbcTemplate.queryForObject(querySql, new Object[]{searchId}, getQueriesRowMapper());
    }

    public void removeSearch(final String loginname, final int... searchIds) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        String updateSql = "delete from queryhistory where loginname = ? and queryid = ?";
        jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, loginname);
                ps.setInt(2, searchIds[i]);
            }

            public int getBatchSize() {
                return searchIds.length;
            }
        });

    }

    public void updateSearch(int searchId, boolean isFavorite, String desc) {
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        desc = desc != null && desc.length() > MAX_DESC_COL_WIDTH ?
                            desc.substring(0, MAX_DESC_COL_WIDTH) : desc;
        String updateSql = "update queryhistory set favorite = ?, description = ? where queryid = ?";
        jdbcTemplate.update(updateSql, new Object[]{isFavorite, desc, searchId});
    }

    public List<SearchInfo> getSearchHistory(String loginName) {
        final String appName = ServerContext.getAppName();
        JdbcTemplate jdbcTemplate = JdbcFactory.getTemplate(DbInstance.operation);
        String querySql = "select * from queryhistory where loginname = ? and appname = ? order by timeadded desc";
        return (List<SearchInfo>) jdbcTemplate.query(querySql, new Object[]{loginName, appName}, getQueriesRowMapper());
    }

    private String getTagName(long id) {
        //ex. ADS/Sa.Spitzer#2006/0630/Tid
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MMdd/");
        return ADS_TAG_PREFIX+"#"+df.format(new java.util.Date())+"T"+id;
    }


    private RowMapper getTagsRowMapper() {
        return new RowMapper() {

            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new TagInfo(rs.getInt("tagid"),
                        rs.getString("tagname"),
                        rs.getString("historytoken"),
                        rs.getString("description"),
                        rs.getBoolean("istag"),
                        rs.getInt("numhits"),
                        new java.util.Date(rs.getTimestamp("timecreated").getTime()),
                        rs.getDate("timeused") == null ? null : new java.util.Date(rs.getTimestamp("timeused").getTime()));
            }
        };
    }

    private RowMapper getQueriesRowMapper() {
        return new RowMapper() {

            public Object mapRow(ResultSet rs, int i) throws SQLException {
                return new SearchInfo(
                        rs.getInt("queryid"),
                        rs.getString("loginname"),
                        rs.getString("historyToken"),
                        rs.getString("description"),
                        rs.getBoolean("favorite"),
                        rs.getTimestamp("timeadded"));

            }
        };
    }


}


