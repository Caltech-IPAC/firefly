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
import java.util.Arrays;
import java.util.List;

/**
 * Created by zhang on 10/16/15.
 */
public class HistogramProcessor  extends IpacTablePartProcessor {
    private static final String  SEARCH_PARAMETER = "searchRequest";
    private static DataType[] columns = new DataType[]{
                                                              new DataType("numInBin", Integer.class),
                                                              new DataType("binMin", Double.class),
                                                              new DataType("binMax",  Double.class)
    };
    private final String  ALGORITHM = "fixedSizeBins";
    private final String  LINEAR_SCALE = "linear";
    private final String  BINSIZE = "binSize";
    private final String  COLUMN = "column";
    private final String  MIN = "min";
    private final String  MAX = "max";

    private String algorithm = ALGORITHM;
    private String scale = LINEAR_SCALE;
    private double binSize = 20;
    private double min=Double.NaN;
    private double max=Double.NaN;
    private String columnName=null;
    /**
     * empty constructor
     */
    public HistogramProcessor (){};

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

    public DataGroup getHistogramTable(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {
        File file = loadDataFile(request);
        JSONObject searchRequestJSON = (JSONObject) JSONValue.parse(request.getParam(SEARCH_PARAMETER));

        if (searchRequestJSON != null) {
            for (Object param : searchRequestJSON.keySet()) {
                String name = (String) param;
                Object value =  searchRequestJSON.get(param);
                if (name.equalsIgnoreCase(ALGORITHM)) {
                    algorithm=  (String) value;
                }
                else if (name.equalsIgnoreCase(LINEAR_SCALE)){
                    scale=  (String) value;
                }
                else if (name.equalsIgnoreCase(COLUMN)){
                    columnName = (String) value;
                }
                else if (name.equalsIgnoreCase(MIN)){
                    min= (( Integer) value).intValue();
                }
                else if (name.equalsIgnoreCase(MAX)){
                    max= (( Integer) value).intValue();
                }
                else if (name.equalsIgnoreCase(BINSIZE)){
                    binSize = ( (Double) value).doubleValue();
                }
            }
        }
       return  createHistogramTable(file);

    }
    private  DataGroup  createHistogramTable(File file) throws IpacTableException, IOException, DataAccessException  {

        DataGroup dg = IpacTableReader.readIpacTable(file, null, false, "inputTable");

        DataType  column = getColumn(dg);
        if (column==null){
            throw new DataAccessException(columnName + " is not found in the input table" );
        }
        double[] columnData = getColumnData(dg, column);
        return createHistogramTable(columnData);
    }
    private double[] scaleData(double[] columnData){
        double[] rData = new double[columnData.length];

        for(int i=0; i<columnData.length; ){
            if (scale.equalsIgnoreCase("log")){
                rData[i]=Math.log(columnData[i]);
            }
            else if (scale.equalsIgnoreCase("log10")){
                rData[i]=Math.log10(columnData[i]);
            }
            else if (scale.equalsIgnoreCase("sqrt")){
                rData[i]=Math.sqrt(columnData[i]);
            }
        }
        return rData;
    }
    private int getBinSize(double[] columnData){

        int nBin = (int) ((columnData[columnData.length-1] -columnData[0])/binSize);
        if (min!=Double.NaN && max!=Double.NaN ){
            nBin = (int) ((max - min) / binSize);
        }
        else if (min==Double.NaN && max!=Double.NaN){
            nBin = (int) ((columnData[columnData.length-1] - min) / binSize);
        }
        else if (min!=Double.NaN && max==Double.NaN){
            nBin = (int) ((max - columnData[0]) / binSize);
        }
        return nBin;
    }
    private DataGroup createHistogramTable( double[] columnData){
        DataGroup HistogramTable = new DataGroup("histogramTable", columns);
        if (!scale.equalsIgnoreCase(LINEAR_SCALE)){
            columnData = scaleData(columnData);
        }
        Arrays.sort(columnData);
        int nBin=getBinSize(columnData);

        Object[] obj = getBinInfo(columnData, nBin);
        int[] numInBins = (int[]) obj[0];
        double[] binMin = (double[]) obj[1];
        double[] binMax = (double[]) obj[2];
        for (int i=0; i<nBin; i++){
            DataObject row = new DataObject(HistogramTable);
            row.setDataElement(columns[0], numInBins[i]);
            row.setDataElement(columns[1], binMin[i]);
            row.setDataElement(columns[2], binMax[i]);
            HistogramTable.add(row);
        }
        return HistogramTable;
    }
    private Object[] getBinInfo(double[] columnData, int nBin){
        int[] numInBins = new int[nBin];
        double[] binMin = new double[nBin];
        if (min==Double.NaN){
            min= (int) columnData[0];
        }
        if (max==Double.NaN){
            max =columnData[columnData.length-1];
        }
        Arrays.fill(binMin, min);
        double[] binMax = new double[nBin];
        Arrays.fill(binMax, max);

        for (int i=0; i<columnData.length; i++){
           if (columnData[i]>=min && columnData[i]<=max){
               int iBin =  (int)( ( columnData[i]-min)/ binSize );
               if (columnData[i]<binMin[iBin]) binMin[iBin]=columnData[i];
               if (columnData[i]>binMax[iBin]) binMax[iBin]=columnData[i];
               numInBins[iBin]++;

           }
        }
        Object[] obj={numInBins, binMin, binMax};
        return obj;
    }
    private DataType getColumn( DataGroup dg){
        DataType[] inColumns = dg.getDataDefinitions();
        for (int i=0; i<inColumns.length; i++){
            if (inColumns[i].getKeyName().equalsIgnoreCase(columnName)){
                return inColumns[i];
            }
        }
        return null;
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

    private double[] getColumnData(DataGroup dg, DataType column){
        List<DataObject> objList= dg.values();
        int nRow = objList.size();
        double[] data= new double[nRow];
        for (int i=0; i<nRow; i++){
            Object value = objList.get(i).getDataElement(column);
            data[i] = convertToADoubleValue(value, column);
        }

        return data;
    }
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
                    DataGroup dg = IpacTableReader.readIpacTable(inFile, null, false, "inputTable");

                    HistogramProcessor hp = new HistogramProcessor();
                    hp.columnName  = "f_x";
                    DataType  column = hp.getColumn(dg);

                    if (column==null){
                        throw new DataAccessException(hp.columnName + " is not found in the input table" );
                    }
                    double[] columnData = hp.getColumnData(dg, column);

                    DataGroup outDg = hp.createHistogramTable(columnData);
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
