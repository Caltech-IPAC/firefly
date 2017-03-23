/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.util.DataSetParser.*;
import static edu.caltech.ipac.util.DataGroup.ROWID_NAME;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class TableDbUtil {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    /**
     * ingest the given datagroup into a database file using the provided DbAdpater.
     * @param dbFile  the file to save the database to.
     * @param dg the datagroup containing the data
     * @param dbAdapter DbAdapter to use.. ie sqlite, h2, etc.
     * @return  a FileInfo with sizeInBytes representing to the number of rows.
     */
    public static FileInfo createDbFile(File dbFile, DataGroup dg, DbAdapter dbAdapter) {
        int rowCount = createDataTbl(dbFile, dg, dbAdapter);
        createDDTbl(dbFile, dg, dbAdapter);
        createMetaTbl(dbFile, dg, dbAdapter);
        FileInfo finfo = new FileInfo(dbFile);
        finfo.setSizeInBytes(rowCount);
        return finfo;
    }

    public static int insertMeta(DataGroup dg, ResultSet rs) {
        try {
            do {
                dg.addAttribute(rs.getString("key"), rs.getString("value"));
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static int insertDD(DataGroup dg, ResultSet rs) {
        try {
            do {
                String cname = rs.getString("cname");
                String label = rs.getString("label");
                String units = rs.getString("units");
                String format = rs.getString("format");
                int width = rs.getInt("width");
                String visibility = rs.getString("visibility");
                String desc = rs.getString("desc");
                boolean sortable = rs.getBoolean("sortable");
                boolean filterable = rs.getBoolean("filterable");

                DataType dtype = dg.getDataDefintion(cname, true);
                if (dtype != null) {
                    dtype.setKeyName(cname);
                    if (!StringUtils.areEqual(label, cname)) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.LABEL_TAG, cname);
                        dg.addAttribute(attr, label);
                    }
                    if (!StringUtils.isEmpty(units)) {
                        dtype.setUnits(units);
                    }
                    if (!StringUtils.isEmpty(format)) {
                        dtype.getFormatInfo().setDataFormat(format);
                    }
                    if (width > 0) {
                        dtype.getFormatInfo().setWidth(width);
                    }
                    if (!StringUtils.areEqual(visibility, VISI_SHOW)) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, cname);
                        dg.addAttribute(attr, visibility);
                    }
                    if (!StringUtils.isEmpty(desc)) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, cname);
                        dg.addAttribute(attr, desc);
                    }
                    if (!sortable) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.SORTABLE_TAG, cname);
                        dg.addAttribute(attr, String.valueOf(false));
                    }
                    if (!filterable) {
                        String attr = DataSetParser.makeAttribKey(DataSetParser.FILTERABLE_TAG, cname);
                        dg.addAttribute(attr, String.valueOf(false));
                    }
                }
            } while (rs.next());
        } catch (SQLException e) {
            logger.error(e);
        }
        return 0;
    }

    public static String setupDatasetTable(TableServerRequest treq) {
        String datasetID = getDatasetID(treq);
        if (StringUtils.isEmpty(datasetID)) return "data";

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance = dbAdapter.getDbInstance(getDbFile(treq));

        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(String.format("select count(*) from %s", datasetID));
        } catch (Exception e) {
            // does not exists.. create table from orignal 'data' table
            String wherePart = dbAdapter.wherePart(treq);
            String orderBy = dbAdapter.orderByPart(treq);

            String datasetSql = String.format("select * from data %s %s", wherePart, orderBy);
            String sql = dbAdapter.createTableFromSelect(getDatasetID(treq), datasetSql);
            JdbcFactory.getSimpleTemplate(dbInstance).update(sql);
        }

        return datasetID;
    }

    public static File getDbFile(TableServerRequest treq) {
        return ServerContext.convertToFile(treq.getMeta(TBL_FILE_PATH));
    }

    public static File getStorageFile(TableServerRequest treq) {
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        return dbAdapter.getStorageFile(getDbFile(treq));
    }

    public static void setDbMetaInfo(TableServerRequest treq, DbAdapter dbAdapter, File dbFile) {
        treq.setMeta(TBL_FILE_PATH, ServerContext.replaceWithPrefix(dbFile));
        treq.setMeta(TBL_FILE_TYPE, dbAdapter.getName());
    }

    @NotNull
    public static String getDatasetID(TableServerRequest treq) {
        String id = StringUtils.toString(treq.getDataSetParam(), "|");
        return StringUtils.isEmpty(id) ? "" : "ds_" + DigestUtils.md5Hex(id);
    }

    public static DataGroupPart getResults(TableServerRequest treq, String sql) {
        DataGroup dg = runQuery(DbAdapter.getAdapter(treq), getDbFile(treq), sql);
        TableDef tm = new TableDef();
        tm.setStatus(DataGroupPart.State.COMPLETED);
        tm.setRowCount(dg.size());
        return new DataGroupPart(tm, dg, treq.getStartIndex(), dg.size());
    }

    public static int createDataTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter) {
        return createDataTbl(dbFile, dg, dbAdapter, null);
    }

    public static int createDataTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String tblName) {

        boolean insertRowId = insertRowIdIfNeeded(dg);
        int totalRows = dg.size();

        String createDataSql = dbAdapter.createDataSql(dg.getDataDefinitions(), tblName);
        logger.briefDebug("createDataSql:" + createDataSql);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDataSql);
        DataType rowid = dg.getDataDefintion(ROWID_NAME);
        for(int i = 0; i < dg.size(); i++) {
            DataObject row = dg.get(i);
            if (insertRowId) {
                row.setDataElement(rowid, i);
            }
        }
        String[] var = new String[dg.getDataDefinitions().length];
        Arrays.fill(var , "?");

        String insertDataSql = dbAdapter.insertDataSql(dg.getDataDefinitions(), tblName);
        logger.briefDebug("insertDataSql:" + insertDataSql);


        JdbcTemplate jdbc = JdbcFactory.getTemplate(dbAdapter.getDbInstance(dbFile));
        if (dbAdapter.useTxnDuringLoad()) {
            TransactionTemplate txnJdbc = JdbcFactory.getTransactionTemplate(jdbc.getDataSource());
            txnJdbc.execute(new TransactionCallbackWithoutResult() {
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    doTableLoad(jdbc, insertDataSql, dg);
                }
            });
        } else {
            doTableLoad(jdbc, insertDataSql, dg);
        }

        return totalRows;
    }

    public static DataGroup runQuery(DbAdapter dbAdapter, File dbFile, String sql) {
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
        sql = dbAdapter.translateSql(sql);

        DataGroup dg = (DataGroup)JdbcFactory.getTemplate(dbInstance).query(sql, rs -> {
            return DataGroupUtil.processResults(rs);
        });

        SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile));

        // insert DD info into the results
        try {
            String ddSql = dbAdapter.getDDSql();
            jdbc.query(ddSql, (rs, i) -> TableDbUtil.insertDD(dg, rs));
        } catch (Exception e) {
            // ignore.. may not have DD table
        }

        // insert table meta info into the results
        try {
            String metaSql = dbAdapter.getMetaSql();
            jdbc.query(metaSql, (rs, i) -> TableDbUtil.insertMeta(dg, rs));
        } catch (Exception e) {
            // ignore.. may not have meta table
        }

        return dg;
    }

//====================================================================
//
//====================================================================

    private static void doTableLoad(JdbcTemplate jdbc, String insertDataSql, DataGroup data) {
        jdbc.batchUpdate(insertDataSql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] r = data.get(i).getData();
                for (int cidx = 0; cidx < r.length; cidx++) ps.setObject(cidx+1, r[cidx]);
            }
            public int getBatchSize() {
                return data.size();
            }
        });
    }

    public static void createMetaTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter) {
        if (dg.getAttributeKeys().size() == 0) return;

        String createMetaSql = dbAdapter.createMetaSql(dg.getDataDefinitions());
        logger.debug("createMetaSql:" + createMetaSql);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createMetaSql);

        List<Object[]> data = new ArrayList<>();
        Map<String, DataGroup.Attribute> meta = dg.getAttributes();
        for (String key : meta.keySet()) {
            String val = meta.get(key).getValue();
            data.add(new Object[]{key, val});
        }
        String insertDDSql = dbAdapter.insertMetaSql(dg.getDataDefinitions());
        logger.debug("insertDDSql:" + insertDDSql);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }


    public static void createDDTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter) {

        insertRowIdIfNeeded(dg);
        String createDDSql = dbAdapter.createDDSql(dg.getDataDefinitions());
        logger.debug("createDDSql:" + createDDSql);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDDSql);

        Map<String, DataGroup.Attribute> meta = dg.getAttributes();
        List<Object[]> data = new ArrayList<>();
        for(DataType dt : dg.getDataDefinitions()) {
            int width = getIntVal(meta, WIDTH_TAG, dt, 0);
            width = width > 0 ? width : dt.getFormatInfo().getWidth();
            String format = getStrVal(meta, FORMAT_TAG, dt, null);
            format = format == null ? dt.getFormatInfo().getDataFormatStr() : format;
            String visi = dt.getKeyName().equals(ROWID_NAME) ? VISI_HIDDEN :
                            getStrVal(meta, VISI_TAG, dt, VISI_SHOW);

            data.add( new Object[]
                    {
                            dt.getKeyName(),
                            getStrVal(meta, LABEL_TAG, dt, dt.getKeyName()),
                            dt.getTypeDesc(),
                            dt.getDataUnit(),
                            format,
                            width,
                            visi,
                            Boolean.valueOf(getStrVal(meta, SORTABLE_TAG, dt, "true")),
                            Boolean.valueOf(getStrVal(meta, FILTERABLE_TAG, dt, "true")),
                            getStrVal(meta, DESC_TAG, dt, "")
                    }
            );
        }
        String insertDDSql = dbAdapter.insertDDSql(dg.getDataDefinitions());
        logger.debug("insertDDSql:" + insertDDSql);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    private static boolean insertRowIdIfNeeded(DataGroup dg) {
        if (dg.getDataDefintion(ROWID_NAME) == null) {
            dg.addDataDefinition(DataGroup.makeRowId());
            return true;
        }
        return false;
    }

    private static String getStrVal(Map<String, DataGroup.Attribute> meta, String tag, DataType col, String def) {
        DataGroup.Attribute val = meta.get(makeAttribKey(LABEL_TAG, col.getKeyName()));
        return val == null ? def : val.getValue();
    }

    private static int getIntVal(Map<String, DataGroup.Attribute> meta, String tag, DataType col, int def) {
        String v = getStrVal(meta, tag, col, null);
        return v == null ? def : Integer.parseInt(v);
    }

}
