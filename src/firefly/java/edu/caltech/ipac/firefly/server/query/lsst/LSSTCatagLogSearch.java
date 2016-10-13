package edu.caltech.ipac.firefly.server.query.lsst;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.Iterator;
import java.util.List;


/**
 * Created by zhang on 10/10/16.
 */
@SearchProcessorImpl(id = "LSSTCatagLogSearch")
public class LSSTCatagLogSearch extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private static String DATA_ACCESS_URI = AppProperties.getProperty("lsst.dataAccess.uri", "lsst.dataAccess.uri");
    private static final String TEST_DATA_ROOT = "firefly_test_data/";
    private static final String TEST_DATA_PATH= TEST_DATA_ROOT+"/DAXTestData/";

    private static String testTreePath =LSSTCatagLogSearch.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static String  rootPath = testTreePath.split("firefly")[0];
    private static  String dataPath = rootPath+TEST_DATA_PATH;

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        File file = createFile(request, ".json");
        try {
            request = new TableServerRequest("DummyDD");
            request.setParam(ServerRequest.ID_KEY, "LSSTMetaSearch");

            TableDef tableDef= getMeta(request);
            DataGroup dg = getTableData(file, tableDef);
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg, 0);
            return  outFile;
        }
        catch (ParseException e){
            e.getStackTrace();
        }
       return null;


    }

    private File loadDataFileDummy(TableServerRequest request) throws IOException, DataAccessException {
        //File file = createFile(request, ".json");
        File file = new File(request.getParam("inFileName")+".json");
        try {
            //TableDef tableDef= getMeta(requestMeta);
            DataGroup dg = getTableData(file);
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg, 0);
            return outFile;
        }
        catch (ParseException e){
            e.getStackTrace();
        }

       return null;
    }

    private DataGroup getTableData(File jsonFile) throws IOException, ParseException {


        JSONArray tableData = getData( jsonFile);


        JSONObject object = (JSONObject) tableData.get(0);
        String[] colsName = (String[]) object.keySet().toArray(new String[0]);
        DataType[]  dataTypes  = new DataType[colsName.length];
        for (int i=0; i<colsName.length;  i++){
            Object keyvalue = object.get(colsName[i]);
            Class cls= new Object().getClass();
            if (keyvalue!=null){
                cls = keyvalue.getClass();
            }
            dataTypes [i]= new DataType(colsName[i], cls);

        }



        // DataType[]  dataType =  tableDef.getCols().toArray(new DataType[0]);


        DataGroup dg = new DataGroup("result", dataTypes  );

        for (int i=0; i<tableData.size(); i++){
            JSONObject rowTblData = (JSONObject) tableData.get(i);
            DataObject row = new DataObject(dg);
            for (int j=0; j<dataTypes.length; j++){
                Object value = rowTblData.get(colsName[j]);
                row.setDataElement(dataTypes[j],value);

            }
            dg.add(row);
        }


        return null;//dg;
    }
    private DataGroup getTableData(File jsonFile, TableDef tableDef) throws IOException, ParseException {


        JSONArray tableData = getData( jsonFile);
        DataType[]  dataType =  tableDef.getCols().toArray(new DataType[0]);

        DataGroup dg = new DataGroup("result", dataType  );

        for (int i=0; i<tableData.size(); i++){
            JSONObject rowTblData = (JSONObject) tableData.get(i);
            DataObject row = new DataObject(dg);
            for (int j=0; j<dataType.length; j++){

                if (dataType[j].getDataType()==Boolean.class){
                    System.out.println("debug");
                }

                Object value = getValue(dataType[j].getKeyName(), rowTblData);
                if (value!=null) {
                    row.setDataElement(dataType[j],value);
                }
                else {
                    System.out.println("No data found in this table");
                }
                System.out.println(dataType[j].getKeyName());
            }
            dg.add(row);
        }


        return dg;
    }
    private TableDef getMeta(TableServerRequest request){
        SearchManager sm = new SearchManager();

        try {
            DataGroupPart dgp = sm.getDataGroup(request);
            return dgp.getTableDef();
        } catch (Exception e) {
            e.getStackTrace();
        }
        return null;
    }
    private static JSONArray getData(File jsonFile) throws IOException, ParseException {


        JSONParser parser = new JSONParser();

        Object obj = parser.parse(new FileReader(jsonFile)); //meta json file
        return (JSONArray)obj;

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

    public static void main(String[] args) throws IOException, ParseException, DataAccessException {

        String jsonFileName = "RunDeepSource_ra_btw";

        TableServerRequest request = new TableServerRequest("DummyTable");
        request.setParam("inFileName",dataPath +jsonFileName );
        request.setParam(ServerRequest.ID_KEY, "RunDeepSource_ra_btw");
       /* String outFileName = dataPath + "output_" + jsonFileName;
        request.setParam("outFileName", outFileName);
        request.setParam("MetaID", "DummyDD");

        TableServerRequest requestMeta = new TableServerRequest("DummyDD");
        requestMeta.setParam(ServerRequest.ID_KEY, "LSSTMetaSearch");
*/
        LSSTCatagLogSearch lssCat = new LSSTCatagLogSearch();
        File file = lssCat.loadDataFileDummy(request);

       System.out.println("done" + file.getAbsolutePath());



    }
}
