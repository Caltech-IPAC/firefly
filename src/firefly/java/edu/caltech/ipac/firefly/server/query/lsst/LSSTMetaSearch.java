package edu.caltech.ipac.firefly.server.query.lsst;


import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.List;

/**
 * Created by zhang on 10/12/16.
 * This search processor is searching the MetaData (or Data Definition from DAX database, then save to a
 * IpacTable file.
 */
@SearchProcessorImpl(id = "LSSTMetaSearch")
public class LSSTMetaSearch  extends IpacTablePartProcessor{
     private static final Logger.LoggerImpl _log = Logger.getLogger();

     private static String DATA_ACCESS_URI = AppProperties.getProperty("lsst.dataAccess.uri", "lsst.dataAccess.uri");
     private static final String TEST_DATA_ROOT = "firefly_test_data/";
     private static final String TEST_DATA_PATH= TEST_DATA_ROOT+"/DAXTestData/";

    private static String classPath =LSSTCataLogSearch.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static String rootPath = classPath.split("firefly")[0];
    private static  String dataPath = rootPath+TEST_DATA_PATH;


    /* private  static TableMeta getMeta(TableServerRequest request) throws IOException, ParseException {
        //TODO return the search data and then process it to a TableMeta data
         return null;
     }*/

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        //String ddTable = request.getParam(CatalogRequest.CATALOG);
        // output file will be in json format

       // File file = createFile(request, ".json");
        File file = new File(request.getParam("table_path")+request.getParam("table_name")+".json");
        try {
            DataGroup dg = getMetaData(file);
            File outFile = createFile(request, ".tbl");
            dg.shrinkToFitData();
            DataGroupWriter.write(outFile, dg, 0);
            return outFile;

        }
        catch (ParseException e){
            e.getStackTrace();
        }
        return null;

    }
    File loadDataFileDummy(TableServerRequest request) throws IOException, DataAccessException {


        //String ddTable = request.getParam(CatalogRequest.CATALOG);
        // output file will be in json format
       // File file = new File(request.getParam("outFileName"));

        try {
            File file = new File(request.getParam("inFileName"));
            DataGroup dg = getMetaData(file);
           // File outFile = createFile(request, ".json");
            File oFile = new File(request.getParam("outFileName"));
            dg.shrinkToFitData();
            DataGroupWriter.write(oFile, dg, 0);
            return oFile;
        }
        catch (ParseException e){
            e.getStackTrace();
        }

        return null;
    }
    private  DataType[] getMetaDummy(File file) throws IOException, ParseException {


        JSONParser parser = new JSONParser();

        Object obj = parser.parse(new FileReader(file )); //meta json file
        JSONArray metaData = (JSONArray) obj;
        int len =metaData.size();



        //Testing the data here
        DataType[] dataTypes = new DataType[len];
        for (int i=0; i<len; i++){
            JSONObject value = (JSONObject) metaData.get(i);
            if (value.get("Field").toString().equalsIgnoreCase("flags_pixel_interpolated_center")){

                String name = value.get("Field").toString();
                System.out.println("debug");
            }

            dataTypes[i] = new DataType(value.get("Field").toString(), getDataClass(value.get("Type").toString()) );
            if (value.get("Null").toString().equalsIgnoreCase("yes")){
                dataTypes[i].setMayBeNull(true);
            }
            else {
                dataTypes[i].setMayBeNull(false);
            }
        }


        return  dataTypes;
    }
    private DataGroup getMetaData(File file) throws IOException, ParseException {


        DataType[] dataTypes = getMetaDummy(file);
        DataGroup dg = new DataGroup(file.getName(), dataTypes);

        return dg;
    }

    static Class getDataClass(String classType){

        if (classType.equalsIgnoreCase("double")){
            return Double.class;
        }
        else if (classType.equalsIgnoreCase("float") || classType.equalsIgnoreCase("real") ){
            return Float.class;
        }
        else if (classType.equalsIgnoreCase("int(11)")){
            return Integer.class;
        }
        else if (classType.equalsIgnoreCase("BigInt(20)") ){//|| classType.equalsIgnoreCase("int(11)") ){
            return Long.class;
        }
        else if (classType.equalsIgnoreCase("bit(1)")){
            return Boolean.class;
        }
        else if (classType.equalsIgnoreCase("TINYINT")){
            return Byte.class;
        }
        else if (classType.equalsIgnoreCase("SMALLINT")){
            return Short.class;
        }
        else if (classType.equalsIgnoreCase("string") ) {

            return String.class;

        }
        else {
            System.out.println(classType + "is not supported");
        }
        return null;

    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            return request.getRequestId();
        } else {
            return catTable+"-dd-";
        }

    }
    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);

    }


    private static Object getValue(String colName,JSONObject rowTblData){

        for (Object key : rowTblData.keySet()) {
            //based on you key types
            String keyStr = (String)key;
            if (keyStr.equalsIgnoreCase(colName) ) {
                return rowTblData.get(keyStr);

            }
        }
        return null;
    }

    private static String getTypeInMeta(String key, String[] names, String[] types){
        for (int i=0; i<names.length; i++){
            if (names[i].equalsIgnoreCase(key)){
                return types[i];
            }
        }
        return null;
    }
    public static void main(String[] args) throws IOException, ParseException, DataAccessException {


        String jsonFileName = "RunDeepSourceDD";

        TableServerRequest request = new TableServerRequest("DummyDD");
        request.setParam("inFileName",dataPath +jsonFileName+".json" );
        String outFileName = dataPath + "output_" + jsonFileName+".tbl";
        request.setParam("outFileName", outFileName);
        LSSTMetaSearch lsstMeta = new LSSTMetaSearch();


        File file = lsstMeta.loadDataFileDummy(request);
        System.out.println("done" + file.getAbsolutePath());



       /* TableServerRequest request = new TableServerRequest("DummyDD");
        request.setParam("inFileName",dataPath +jsonFileName );
        String outFileName = dataPath + "output_" + jsonFileName;
        request.setParam("outFileName", outFileName);
        LSSTMetaSearch lsstMeta = new LSSTMetaSearch();


        File file = lsstMeta.loadDataFileDummy(request);
        System.out.println("done" + file.getAbsolutePath());
*/


      //  BufferedWriter writer = new BufferedWriter(new FileWriter(dataPath+"deepSourceDD.txt"));

     /*   JSONParser parser = new JSONParser();
        File file = new File(dataPath+jsonFileName);
        Object obj = parser.parse(new FileReader(file )); //meta json file
        JSONArray metaData = (JSONArray) obj;
        int len =metaData.size();
        String[] types = new String[len];
        String[] fields = new String[len];

        //Testing the data here
        for (int i=0; i<len; i++){

            JSONObject value = (JSONObject) metaData.get(i);
            types[i]=value.get("Type").toString();
            fields[i]=value.get("Field").toString();
            String line = value.get("Field").toString() + ":"+ types[i];
           // writer.write(line+"\n");
            //System.out.println(line);
        }
       // writer.close();



        String dataFile = "RunDeepSource_ra_btw.json";
        file = new File(dataPath+dataFile);
        obj = parser.parse(new FileReader(file )); //meta json file
        BufferedWriter  writer = new BufferedWriter(new FileWriter(dataPath+"RunDeepSource_ra_btw.txt"));
        JSONArray tableData = (JSONArray) obj;
        //Testing the data here
        JSONObject rowTblData = (JSONObject) tableData.get(0);
        for (int i=0; i<fields.length; i++){

            Object value = getValue(fields[i], rowTblData);
            String type=value!=null?value.getClass().getTypeName():"null";

            String line =fields[i] + "\t:" + type;

            writer.write(line+"\n");
            System.out.println(line);
        }


        writer.close();*/

       /* String[] keys = (String[]) rowTblData.keySet().toArray(new String[0]);
        for (int i=0; i<len; i++){
            rowTblData = (JSONObject)tableData.get(i);

            writer.write("============row"+i+"=========\n");
            for (int j=0; j<keys.length; j++){
                Object value = getValue(keys[j], rowTblData);
                String type=value!=null?value.getClass().getTypeName():"null";

                String line = keys[j] + "\t:" + type;

                String ddInMeta = getTypeInMeta(keys[j],fields, types);

                if (ddInMeta==null){
                    line=line+" \t DD does no have this field";
                }
                else {
                    if (value != null) {
                      if (!ddInMeta.equalsIgnoreCase(value.getClass().getTypeName())) {
                            line = line + " \t the data type does not match what is in DD";
                        }
                    }
                }
                writer.write(line+"\n");
                System.out.println(line);
            }

        }
        writer.close();
*/

    }
}
