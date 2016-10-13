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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    private static String classPath =LSSTCatagLogSearch.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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

        File file = createFile(request, ".json");
        try {
            DataGroup dg = getMetaData(file);
            File outFile = createFile(request, ".tbl");
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
        File file = new File(request.getParam("outFileName"));
        try {
            DataGroup dg = getMetaData(file);
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg, 0);
            return outFile;
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

    private static Class getDataClass(String classType){

        if (classType.equalsIgnoreCase("double")){
            return Double.class;
        }
        else if (classType.equalsIgnoreCase("float") || classType.equalsIgnoreCase("real") ){
            return Float.class;
        }
        else if (classType.equalsIgnoreCase("int(11)")){
            return Integer.class;
        }
        else if (classType.equalsIgnoreCase("BigInt(20)")){
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
        else {

            return String.class;

        }

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


    public static void main(String[] args) throws IOException, ParseException, DataAccessException {


        String jsonFileName = "RunDeepSourceDD";

        TableServerRequest request = new TableServerRequest("DummyDD");
        request.setParam("inFileName",dataPath +jsonFileName );
        String outFileName = dataPath + "output_" + jsonFileName;
        request.setParam("outFileName", outFileName);
        LSSTMetaSearch lsstMeta = new LSSTMetaSearch();


        File file = lsstMeta.loadDataFileDummy(request);
        System.out.println("done" + file.getAbsolutePath());



    }
}
