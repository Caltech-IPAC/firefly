/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.db.spring.mapper.DataGroupUtil;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_PATH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_FILE_TYPE;
import static edu.caltech.ipac.firefly.util.DataSetParser.*;
import static edu.caltech.ipac.util.DataGroup.ROW_IDX;
import static edu.caltech.ipac.util.DataGroup.ROW_NUM;

/**
 * @author loi
 * @version $Id: DbInstance.java,v 1.3 2012/03/15 20:35:40 loi Exp $
 */
public class EmbeddedDbUtil {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    /**
     * ingest the given datagroup into a database file using the provided DbAdpater.
     * @param dbFile  the file to save the database to.
     * @param dg the datagroup containing the data
     * @param dbAdapter DbAdapter to use.. ie sqlite, h2, etc.
     * @return  a FileInfo with sizeInBytes representing to the number of rows.
     */
    public static FileInfo createDbFile(File dbFile, DataGroup dg, DbAdapter dbAdapter) {

        // remove ROW_IDX or ROW_NUM if exists
        dg.removeDataDefinition(DataGroup.ROW_IDX);
        dg.removeDataDefinition(DataGroup.ROW_NUM);

        int rowCount = createDataTbl(dbFile, dg, dbAdapter);
        createDDTbl(dbFile, dg, dbAdapter, "data");
        createMetaTbl(dbFile, dg, dbAdapter, "data");
        FileInfo finfo = new FileInfo(dbFile);
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
        String resultSetID = getResultSetID(treq);

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance = dbAdapter.getDbInstance(getDbFile(treq));

        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(String.format("select count(*) from %s", resultSetID));
        } catch (Exception e) {
            // does not exists.. create table from orignal 'data' table
            List<String> cols = treq.getInclColumns() == null ? getColumnNames(dbInstance, "DATA")
                                    : StringUtils.asList(treq.getInclColumns(), ",");
            String wherePart = dbAdapter.wherePart(treq);
            String orderBy = dbAdapter.orderByPart(treq);
            String tblName = getResultSetID(treq);

            cols = cols.stream().filter((s) -> !(s.equals(DataGroup.ROW_IDX) || s.equals(DataGroup.ROW_NUM))).collect(Collectors.toList());   // remove this cols because it will be automatically added

            // copy data
            String datasetSql = String.format("select %s, %s from data %s %s", StringUtils.toString(cols), DataGroup.ROW_IDX, wherePart, orderBy);
            String datasetSqlWithIdx = String.format("select b.*, (ROWNUM-1) as %s from (%s) as b", DataGroup.ROW_NUM, datasetSql);
            String sql = dbAdapter.createTableFromSelect(tblName, datasetSqlWithIdx);
            JdbcFactory.getSimpleTemplate(dbInstance).update(sql);

            // copy dd
            String ddSql = "select * from data_dd";
            ddSql = dbAdapter.createTableFromSelect(tblName + "_dd", ddSql);
            JdbcFactory.getSimpleTemplate(dbInstance).update(ddSql);

            // copy meta
            String metaSql = "select * from data_meta";
            metaSql = dbAdapter.createTableFromSelect(tblName + "_meta", metaSql);
            JdbcFactory.getSimpleTemplate(dbInstance).update(metaSql);

        }

        return resultSetID;
    }

    public static List<String> getColumnNames(DbInstance dbInstance, String forTable) {
        List<String> cols = JdbcFactory.getSimpleTemplate(dbInstance).query(String.format("select cname from %s_DD", forTable), (rs, i) -> rs.getString(1));
        cols = cols.stream().map((s) -> "\"" + s.toUpperCase() + "\"").collect(Collectors.toList());      //  internally.. columns are stored as uppercase.  adding quotes to avoid db reserved words.
        return cols;
    }

    /**
     * return the DB file this request mapped to.
     * @param treq
     * @return
     */
    public static File getDbFile(TableServerRequest treq) {
        String fname = String.format("%s_%s.%s", treq.getRequestId(), DigestUtils.md5Hex(getUniqueID(treq)), DbAdapter.getAdapter(treq).getName());
        return new File(ServerContext.getTempWorkDir(), fname);
    }

    public static void setDbMetaInfo(TableServerRequest treq, DbAdapter dbAdapter, File dbFile) {
        treq.setMeta(TBL_FILE_PATH, ServerContext.replaceWithPrefix(dbFile));
        treq.setMeta(TBL_FILE_TYPE, dbAdapter.getName());
    }

    public static String getTblFileType(TableServerRequest treq) {
        return treq.getMeta(TBL_FILE_TYPE);
    }

    @NotNull
    public static String getResultSetID(TableServerRequest treq) {
        String id = StringUtils.toString(treq.getResultSetParam(), "|");
        return StringUtils.isEmpty(id) ? "data" : "data_" + DigestUtils.md5Hex(id);
    }

    @NotNull
    public static String getUniqueID(TableServerRequest treq) {
        return SearchProcessor.getUniqueIDDef(treq);
    }

    public static DataGroupPart getResults(TableServerRequest treq, String sql, String tblName) {
        DataGroup dg = runQuery(DbAdapter.getAdapter(treq), getDbFile(treq), sql, tblName);
        TableDef tm = new TableDef();
        tm.setStatus(DataGroupPart.State.COMPLETED);
        tm.setRowCount(dg.size());
        return new DataGroupPart(tm, dg, treq.getStartIndex(), dg.size());
    }

    public static int createDataTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter) {
        return createDataTbl(dbFile, dg, dbAdapter, null);
    }

    public static int createDataTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String tblName) {

        DataType[] colsAry = makeDbCols(dg);
        int totalRows = dg.size();

        String createDataSql = dbAdapter.createDataSql(colsAry, tblName);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDataSql);
        if (totalRows > 0) {
            String[] var = new String[colsAry.length];
            Arrays.fill(var , "?");

            String insertDataSql = dbAdapter.insertDataSql(colsAry, tblName);

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
        }

        return totalRows;
    }

    public static DataGroup runQuery(DbAdapter dbAdapter, File dbFile, String sql, String tblName) {
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
        sql = dbAdapter.translateSql(sql);

        DataGroup dg = (DataGroup)JdbcFactory.getTemplate(dbInstance).query(sql, rs -> {
            return DataGroupUtil.processResults(rs);
        });

        SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile));

        if (tblName != null) {
            // insert DD info into the results
            try {
                String ddSql = dbAdapter.getDDSql(tblName);
                jdbc.query(ddSql, (rs, i) -> EmbeddedDbUtil.insertDD(dg, rs));
            } catch (Exception e) {
                // ignore.. may not have DD table
            }

            // insert table meta info into the results
            try {
                String metaSql = dbAdapter.getMetaSql(tblName);
                jdbc.query(metaSql, (rs, i) -> EmbeddedDbUtil.insertMeta(dg, rs));
            } catch (Exception e) {
                // ignore.. may not have meta table
            }
        }

        return dg;
    }

//====================================================================
//
//====================================================================

    private static void doTableLoad(JdbcTemplate jdbc, String insertDataSql, DataGroup data) {

        jdbc.batchUpdate(insertDataSql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] row = data.get(i).getData();
                Object[] rowWithIdx = new Object[ row.length + 2];
                System.arraycopy(row, 0, rowWithIdx, 0, row.length);
                rowWithIdx[row.length] = i;
                rowWithIdx[row.length+1] = i;

                for (int cidx = 0; cidx < rowWithIdx.length; cidx++) ps.setObject(cidx+1, rowWithIdx[cidx]);
            }
            public int getBatchSize() {
                return data.size();
            }
        });
    }

    public static void createMetaTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String forTable) {
        if (dg.getAttributeKeys().size() == 0) return;

        String createMetaSql = dbAdapter.createMetaSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createMetaSql);

        List<Object[]> data = new ArrayList<>();
        Map<String, DataGroup.Attribute> meta = dg.getAttributes();
        for (String key : meta.keySet()) {
            String val = meta.get(key).getValue();
            data.add(new Object[]{key, val});
        }
        String insertDDSql = dbAdapter.insertMetaSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }


    public static void createDDTbl(File dbFile, DataGroup dg, DbAdapter dbAdapter, String forTable) {

        makeDbCols(dg);
        String createDDSql = dbAdapter.createDDSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).update(createDDSql);

        Map<String, DataGroup.Attribute> meta = dg.getAttributes();
        List<Object[]> data = new ArrayList<>();
        for(DataType dt : dg.getDataDefinitions()) {
            int width = getIntVal(meta, WIDTH_TAG, dt, 0);
            width = width > 0 ? width : dt.getFormatInfo().getWidth();
            String format = getStrVal(meta, FORMAT_TAG, dt, null);
            format = format == null ? dt.getFormatInfo().getDataFormatStr() : format;
            String visi = dt.getKeyName().equals(ROW_IDX) ? VISI_HIDDEN :
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
        String insertDDSql = dbAdapter.insertDDSql(forTable);
        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).batchUpdate(insertDDSql, data);
    }

    private static DataType[] makeDbCols(DataGroup dg) {
        DataType[] cols = new DataType[dg.getDataDefinitions().length + 2];
        if (dg.getDataDefintion(ROW_IDX) != null) {
            logger.error("Datagroup should not have ROW_IDX in it at the start.");
        }
        System.arraycopy(dg.getDataDefinitions(), 0, cols, 0, cols.length-2);
        cols[cols.length-2] = DataGroup.makeRowIdx();
        cols[cols.length-1] = DataGroup.makeRowNum();
        dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, ROW_IDX), DataSetParser.VISI_HIDDEN);
        dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.VISI_TAG, ROW_NUM), DataSetParser.VISI_HIDDEN);
        return cols;
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
