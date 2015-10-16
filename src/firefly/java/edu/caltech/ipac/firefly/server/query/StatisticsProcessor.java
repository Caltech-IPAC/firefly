package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.download.URLDownload;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhang on 10/14/15.
 */

@SearchProcessorImpl(id = "StatisticsProcessor")

public class StatisticsProcessor extends IpacTablePartProcessor {
    public static final String  SEARCH_PARAMETER = "searchRequest";
    public static final String TBL_TYPE = "tblType";
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
    public StatisticsProcessor(){};

    /**
     * This method is defined as an abstract in the IpacTablePartProcessor and it is implemented here.
     * The TableServerRequest is passed here and processed.  Only when the "searchRequest" is set, the request
     * is processed.
     *
     * @param request
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        JSONObject searchRequestJSON = (JSONObject) JSONValue.parse(request.getParam(SEARCH_PARAMETER));
        File inf=null;
        String source=null;
        if (searchRequestJSON != null) {
            for (Object param : searchRequestJSON.keySet()) {
                String name = (String) param;

                if (name.equalsIgnoreCase(ServerParams.ID)) {
                    source = (String) searchRequestJSON.get(param);
                    inf = getSourceFile(source, request);

                }
            }
        }

        if (inf == null) {
            throw new DataAccessException("Unable to read the source[alt_source] file:" + source );
        }

        if (!ServerContext.isFileInPath(inf)) {
            throw new SecurityException("Access is not permitted.");
        }
        return convertToIpacTable(inf, request);

    }

      /**
     * resolve the file given a 'source' string.  it could be a local path, or a url.
     * if it's a url, download it into the application's workarea
     * @param source
     * @param request
     * @return
     */
    private File getSourceFile(String source, TableServerRequest request) {
        File inf = null;
        try {
            URL url = makeUrl(source);
            if (url == null) {
                inf = ServerContext.convertToFile(source);
            } else {
                HttpURLConnection conn = (HttpURLConnection) URLDownload.makeConnection(url);
                int rcode = conn.getResponseCode();
                if (rcode >= 200 && rcode < 400) {
                    String sfname = URLDownload.getSugestedFileName(conn);
                    if (sfname == null) {
                        sfname = url.getPath();
                    }
                    String ext = sfname == null ? null : FileUtil.getExtension(sfname);
                    ext = StringUtils.isEmpty(ext) ? ".ul" : "." + ext;
                    inf = createFile(request, ext);
                    URLDownload.getDataToFile(conn, inf, null, false, true, true, Long.MAX_VALUE);
                }
            }
        } catch (Exception ex) {
            inf = null;
        }
        if (inf != null && inf.canRead()) {
            return inf;
        }

        return null;
    }
    private URL makeUrl(String source) {
        try {
            return new URL(source);
        } catch (MalformedURLException e) {
            return null;
        }


    }

      /**
     * This method process the input IpacTable and find the coumnNames, min, max etc and store in a new IpacTable, ie, a DataGroup.
     * @return
     * @throws IpacTableException
     * @throws IOException
     * @throws DataAccessException
     */
    private  DataGroup  createTableStatistic(File file) throws IpacTableException, IOException, DataAccessException  {

        DataGroup dg = IpacTableReader.readIpacTable(file, null, false, "inputTable" );
        List<DataObject> dgjList= dg.values();
        DataType[] inColumns = dg.getDataDefinitions();
        DataType[] numericColumns = getNumericColumns(inColumns);

        String[] columnNames = getDataTypeField(numericColumns, "name");
        String[] columnDescription = getDataTypeField(numericColumns, "description");
        String[] unit = getDataTypeField(numericColumns, "unit");

        Object[] retArrays = getDataArrays (dgjList,numericColumns );
        double[] minArray = (double[]) retArrays[0];
        double[] maxArray = (double[]) retArrays[1];
        int[] numPointsArray =(int[]) retArrays[2];
        DataGroup statisticsTable = new DataGroup("statisticsTable", columns);
        for (int i=0; i<minArray.length; i++){
           DataObject row = new DataObject(statisticsTable);
           row.setDataElement(columns[0], columnNames[i]);
           row.setDataElement(columns[1], columnDescription[i]);
           row.setDataElement(columns[2], unit[i]);
           row.setDataElement(columns[3], minArray[i]);
           row.setDataElement(columns[4], maxArray[i]);
           row.setDataElement(columns[5], numPointsArray [i]);
            row.setDataElement(columns[2], numPointsArray[i]);
            statisticsTable.add(row);
        }

        return statisticsTable;
    }

    /**
     * Calculate three numerical arrays in the same loop to improve the performance.  Each array could be calculated individually
     * in each corresponding method.  But It takes three loops to get the results
     * @param dgjList
     * @param numericColumns
     * @return
     */
    private Object[] getDataArrays(List<DataObject> dgjList,  DataType[] numericColumns){
        double[] minArray=new double[numericColumns.length];
        double[] maxArray=new double[numericColumns.length];
        int[] numPointsArray=new int[numericColumns.length];
        for (int icol=0; icol<numericColumns.length; icol++) {
            double min=Double.MAX_VALUE;
            double max=Double.MIN_VALUE;
            int numPoints=0;
            for (int i = 0; i < dgjList.size(); i++) {
                double val = convertToADoubleValue(dgjList.get(i).getDataElement(numericColumns[icol].getKeyName()), numericColumns[icol]);
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
    private double convertToADoubleValue(Object data, DataType numericColumn) {
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
     * This method willl read in the DataDef columns and find and return all the numerical columns because none numerical
     * columns have to minimum and maximum values
     * @param columns
     * @return
     */
    private DataType[] getNumericColumns(DataType[] columns ){
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
    private String[] getDataTypeField (DataType[] columns, String field){
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
     * This method is used for testing the class.  It split the input file to path and file name.
     *
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
    public  DataGroup  getStatisticsTable(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {
        File file = loadDataFile(request);
        return createTableStatistic(file);
    }

    public static void main(String args[]) throws IOException, DataAccessException {

        if (args.length > 0) {
            String path = getInputFilePath(args[0])[0];
            String inFileName = getInputFilePath(args[0])[1];
            if (args.length > 0) {
                try {
                    File inFile = new File(args[0]);
                    StatisticsProcessor sp = new StatisticsProcessor();
                    DataGroup outDg = sp.createTableStatistic(inFile);
                    String outFileName = path+"output_"+inFileName;
                    File outFile = new File(outFileName);
                    IpacTableWriter.save(outFile, outDg);

                } catch (IpacTableException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
