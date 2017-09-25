package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhang on 10/14/15.
 * This class calculates the statistics of a IpacTable Data.
 */

@SearchProcessorImpl(id = "StatisticsProcessor")
public class StatisticsProcessor extends EmbeddedDbProcessor {
    private static final String SEARCH_REQUEST = "searchRequest";
    private static DataType[] columns = new DataType[]{
            new DataType("columnName", String.class),
            new DataType("description", String.class),
            new DataType("unit", String.class),
            new DataType("min", Double.class),
            new DataType("max", Double.class),
            new DataType("numPoints", Integer.class),
    };

    /**
     *  recreate the table database if does not exists.  otherwise, return the table's database
     */
    @Override
    public FileInfo createDbFile(TableServerRequest treq) throws DataAccessException {
        TableServerRequest sreq = getSearchRequest(treq);
        File dbFile = EmbeddedDbUtil.getDbFile(sreq);
        if (dbFile == null || !dbFile.canRead()) {
            sreq.setPageSize(1);
            sreq.setStartIndex(0);
            new SearchManager().getDataGroup(sreq).getData();
            dbFile = EmbeddedDbUtil.getDbFile(sreq);
        }
        EmbeddedDbUtil.setDbMetaInfo(treq, DbAdapter.getAdapter(treq), dbFile);
        return new FileInfo(dbFile);
    }

    /**
     * generate stats for the given search request if not exists.  otherwise, return the stats
     */
    @Override
    protected DataGroupPart getDataset(TableServerRequest treq, File dbFile) throws DataAccessException {

        TableServerRequest sreq = getSearchRequest(treq);
        String dsTblName = EmbeddedDbUtil.getDatasetID(sreq);
        dsTblName = StringUtils.isEmpty(dsTblName) ? "data" : dsTblName;
        String statsTblName = dsTblName + "_stats";

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance =  dbAdapter.getDbInstance(dbFile);
        String tblExists = String.format("select count(*) from %s", statsTblName);
        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(tblExists);
        } catch (Exception e) {
            // does not exists.. fetch data and populate
            generateStatsTable(dsTblName, dbFile, dbAdapter, statsTblName);
        }

        treq.setParam(TableServerRequest.SQL_FROM, statsTblName);
        treq.setPageSize(Integer.MAX_VALUE);
        treq.setStartIndex(0);
        String sql = String.format("%s %s %s", dbAdapter.selectPart(treq), dbAdapter.fromPart(treq), dbAdapter.wherePart(treq));
        sql = dbAdapter.translateSql(sql);

        DataGroup dg = EmbeddedDbUtil.runQuery(dbAdapter, dbFile, sql);
        TableDef tm = new TableDef();
        tm.setStatus(DataGroupPart.State.COMPLETED);
        return new DataGroupPart(tm, dg, treq.getStartIndex(), dg.size());
    }

    private void generateStatsTable(String dsTblName, File dbFile, DbAdapter dbAdapter, String statsTblName) {

        // check to see if a dataset table exists... if not, use orginal data table.
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);
        String tblExists = String.format("select count(*) from %s", dsTblName);
        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(tblExists);
        } catch (Exception e) {
            dsTblName = "data";
        }

        // get all cols from dd table
        DataGroup stats = null;
        DataGroup dd = EmbeddedDbUtil.runQuery(dbAdapter, dbFile, String.format("select * from DD"));
        if (dd.size() > 0) {
            String statSql = "select '%s' as \"columnName\", '%s' as \"description\", '%s' as \"unit\", min(%5$s) as \"min\", max(%5$s) as \"max\", count(%5$s) as \"numPoints\" from %s";
            for (int i =0; i < dd.size(); i++) {
                DataObject row = dd.get(i);
                String type = (String) row.getDataElement("TYPE");
                String visi = (String) row.getDataElement("VISIBILITY");
                if (DataType.NUMERIC_TYPES.contains(type) && !StringUtils.areEqual(visi, "hidden")) {
                    String cname = (String) row.getDataElement("CNAME");
                    String desc = (String) row.getDataElement("DESC");
                    String units = (String) row.getDataElement("UNITS");
                    DataGroup aStat = EmbeddedDbUtil.runQuery(dbAdapter, dbFile, String.format(statSql, cname, desc, units, dsTblName, cname));
                    if (stats == null) {
                        stats = aStat;
                    } else {
                        stats.add(aStat.get(0));
                    }
                }
            }
        }
        EmbeddedDbUtil.createDataTbl(dbFile, stats, dbAdapter, statsTblName);
    }

    private TableServerRequest getSearchRequest(TableServerRequest treq) throws DataAccessException {
        String searchRequestJson = treq.getParam(SEARCH_REQUEST);
        if (searchRequestJson == null) {
            throw new DataAccessException("Unable to get statistics: " + SEARCH_REQUEST + " is missing");
        }
        TableServerRequest sreq = QueryUtil.convertToServerRequest(searchRequestJson);
        if (sreq.getRequestId() == null) {
            throw new DataAccessException("Unable to get statistics: " + SEARCH_REQUEST + " must contain " + ServerParams.ID);
        }
        return sreq;
    }
}











class StatisticsProcessorOld extends IpacTablePartProcessor {
    private static final String SEARCH_REQUEST = "searchRequest";
    private static DataType[] columns = new DataType[]{
            new DataType("columnName", String.class),
            new DataType("description", String.class),
            new DataType("unit", String.class),
            new DataType("min", Double.class),
            new DataType("max", Double.class),
            new DataType("numPoints", Integer.class),
    };

    /**
     * empty constructor
     */
    public StatisticsProcessorOld(){}

    /**
     * Add this method to run unit test
     *
     * @param inDataGroup source data group
     * @return DataGroup containing table statistics
     */
    public  static DataGroup createTableStatistic(DataGroup inDataGroup){
        List<DataObject> dgjList= inDataGroup.values();
        DataType[] inColumns =inDataGroup.getDataDefinitions();
        DataType[] numericColumns = getNumericColumns(inColumns);

        String[] columnNames = new String[numericColumns.length];
        String[] unit = new String[numericColumns.length];
        for (int i=0; i<numericColumns.length; i++){
            columnNames[i]=numericColumns[i].getKeyName();
            unit[i]=numericColumns[i].getDataUnit();
        }

        Object[] retArrays = getDataArrays (dgjList,numericColumns );
        double[] minArray = (double[]) retArrays[0];
        double[] maxArray = (double[]) retArrays[1];
        int[] numPointsArray =(int[]) retArrays[2];
        DataGroup statisticsTable = new DataGroup("statisticsTable", columns);
        for (int i=0; i<minArray.length; i++){
            DataObject row = new DataObject(statisticsTable);
            row.setDataElement(columns[0], columnNames[i]);
            row.setDataElement(columns[1], ""); // description not available
            row.setDataElement(columns[2], unit[i]);
            row.setDataElement(columns[3], minArray[i]);
            row.setDataElement(columns[4], maxArray[i]);
            row.setDataElement(columns[5], numPointsArray [i]);
            statisticsTable.add(row);
        }

        // adjust the width of all columns to fit the data
        statisticsTable.shrinkToFitData(true);
        return statisticsTable;
    }

    /**
     *
     * Calculate three numerical arrays in the same loop to improve the performance.  Each array could be calculated individually
     * in each corresponding method.  But It takes three loops to get the results
     * @param dgjList
     * @param numericColumns
     * @return
     */
    private static Object[] getDataArrays(List<DataObject> dgjList,  DataType[] numericColumns){
        double[] minArray=new double[numericColumns.length];
        double[] maxArray=new double[numericColumns.length];
        int[] numPointsArray=new int[numericColumns.length];
        for (int icol=0; icol<numericColumns.length; icol++) {
            double min=Double.MAX_VALUE; //using the maximum double value as a minimal
            double max=-Double.MAX_VALUE; //using the minimum double value as the maximun
            int numPoints=0;
            for (int i = 0; i < dgjList.size(); i++) {
                Object data=dgjList.get(i).getDataElement(numericColumns[icol].getKeyName());
                if (data==null){
                    continue;
                }
                double val = convertToADoubleValue(data, numericColumns[icol]);
                if (Double.isFinite(val)) {
                    numPoints++;
                    if (val < min) min = val;

                    if (val > max) max = val;
                }
            }
            minArray[icol]=min;
            maxArray[icol]=max;
            numPointsArray[icol]=numPoints;
        }
        Object[] ret= {minArray, maxArray, numPointsArray};
        return ret;
    }

    /**
     * This method convert the numerical data of Object type to the type of Double, return a primitive double value.
     * @param data
     * @param numericColumn
     * @return
     */
    private static double convertToADoubleValue(Object data, DataType numericColumn) {
        Class type = numericColumn.getDataType();

          if (type == Double.class) {
            Double doubleData = (Double) data;
            return doubleData.doubleValue();
        } else if (type == Float.class) {
            Float  floatData = (Float) data;

                  return floatData.doubleValue();

        } else if (type == Integer.class) {
            Integer integerData = (Integer) data;
            return integerData.doubleValue();
        } else if (type == Short.class) {
            Short shortData = (Short) data;
            return shortData.doubleValue();
        } else if (type == Long.class) {
            Long longData = (Long) data;
            return longData.doubleValue();

        } else if (type == Byte.class) {
            Byte byteData = (Byte) data;
           return byteData.doubleValue();
        }
        return Double.NaN;
    }

    /**
     * This method will read in the DataDef columns and find and return all the numerical columns because none numerical
     * columns have to minimum and maximum values
     * @param columns
     * @return
     */
    private static DataType[] getNumericColumns(DataType[] columns ){
        ArrayList<DataType> numericDataTypeArray = new ArrayList<DataType>();
        for (int i=0; i<columns.length; i++){
            if (columns[i].getDataType().equals(String.class) || columns[i].getDataType().equals(Boolean.class) ){
                continue;
            }
            numericDataTypeArray.add(columns[i]);
        }
        return numericDataTypeArray.toArray(new DataType[0]);
    }

    /**
     * This method will check each of the DataTye def in the columns array and find its name, unit
     * and description.
     * @param columns
     * @param field - a String which tells which field is going to search for.  It can be "name", "unit"
     *              and "description".
     * @return - return the corresponding array of the field
     */
    private static String[] getDataTypeField (DataType[] columns, String field){
        String[] results = new String[columns.length];
        for (int i=0; i<columns.length; i++){
            if (field.equalsIgnoreCase("name")){
                results[i]=columns[i].getKeyName();
            }
            else if (field.equalsIgnoreCase("unit")){
                results[i]=columns[i].getDataUnit();
            }
                else if (field.equalsIgnoreCase("description") ){
                results[i]=columns[i].getShortDesc()!=null?columns[i].getShortDesc():" ";
            }
        }
        return results;
    }

    /**
     * This method splits the input file to path and file name.
     * @param inputFileName
     * @return  the path (or directory)  of the inputFileName is from.
     */
    private static String[] getInputFilePath(String inputFileName){
        String[] dirs= inputFileName.split("/");
        String name = dirs[dirs.length-1];
        String path = inputFileName.substring(0, inputFileName.length()-name.length());
        String[] ret={path, name};
        return ret;
    }

    public static void main(String args[]) throws IOException, DataAccessException {

        if (args.length > 0) {
            String path = getInputFilePath(args[0])[0];
            String inFileName = getInputFilePath(args[0])[1];
            if (args.length > 0) {
                try {
                    File inFile = new File(args[0]);
                    StatisticsProcessorOld sp = new StatisticsProcessorOld();
                    DataGroup outDg = sp.createTableStatistic(inFile);
                    String outFileName = path+"statistics_output_"+inFileName;
                    File outFile = new File(outFileName);
                    IpacTableWriter.save(outFile, outDg);

                } catch (IpacTableException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This method is defined as an abstract in the IpacTablePartProcessor and it is implemented here.
     * The TableServerRequest is passed here and processed.  Only when the "searchRequest" is set, the request
     * is processed.
     *
     * @param request table server request
     * @return File with statistics on a table
     * @throws IOException
     * @throws DataAccessException
     */
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        String searchRequestJson = request.getParam(SEARCH_REQUEST);
        if (searchRequestJson == null) {
            throw new DataAccessException("Unable to get statistics: " + SEARCH_REQUEST + " is missing");
        }
        TableServerRequest sReq = QueryUtil.convertToServerRequest(searchRequestJson);

        if (sReq.getRequestId() == null) {
            throw new DataAccessException("Unable to get statistics: " + SEARCH_REQUEST + " must contain " + ServerParams.ID);
        }
        sReq.setPageSize(Integer.MAX_VALUE);
        sReq.setStartIndex(0);
        DataGroup sourceDataGroup = new SearchManager().getDataGroup(sReq).getData();
        DataGroup statisticsDataGroup = createTableStatistic(sourceDataGroup);
        statisticsDataGroup.addAttribute("searchRequest", sReq.toString());
        File statisticsFile = createFile(request);
        DataGroupWriter.write(statisticsFile, statisticsDataGroup);
        return statisticsFile;
    }

    /**
     *
     * This method process the input IpacTable and find the coumnNames, min, max etc and store in a new IpacTable, ie, a DataGroup.
     * @param file file with the source table
     * @return DataGroup which contains table statistics
     * @throws IpacTableException
     * @throws IOException
     * @throws DataAccessException
     */
    private  DataGroup  createTableStatistic(File file) throws IpacTableException, IOException, DataAccessException  {

        DataGroup dg = IpacTableReader.readIpacTable(file, null, false, "inputTable" );
        return createTableStatistic(dg);

    }
}
