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
import java.util.ArrayList;
import java.util.List;


/**
 * Created by zhang on 10/10/16.
 */
@SearchProcessorImpl(id = "LSSTCataLogSearch")
public class LSSTCataLogSearch extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private static String DATA_ACCESS_URI = AppProperties.getProperty("lsst.dataAccess.uri", "lsst.dataAccess.uri");
    private static final String TEST_DATA_ROOT = "firefly_test_data/";
    private static final String TEST_DATA_PATH= TEST_DATA_ROOT+"/DAXTestData/";

    private static String testTreePath =LSSTCataLogSearch.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    private static String  rootPath = testTreePath.split("firefly")[0];
    private static  String dataPath = rootPath+TEST_DATA_PATH;

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        //File file = createFile(request, ".json");
        File dataFile = new File(request.getParam("table_path")+request.getParam("table_name")+".cvs");
       // File dataFile = new File(request.getParam("table_path")+request.getParam("table_name")+".json");
        File metaFile = new File(request.getParam("table_path")+request.getParam("meta_table")+".json");



         TableDef tableDef= IpacTableUtil.getMetaInfo(metaFile);// getMeta(request);
        // DataGroup dg = getTableDataFromJson(dataFile,tableDef);
         DataGroup dg = null;
         try {
             dg = getTableDataFromCsv(dataFile,tableDef);
         } catch (Exception e) {
             e.printStackTrace();
         }
         File outFile = createFile(request, ".tbl");
         DataGroupWriter.write(outFile, dg, 0);
         return  outFile;
   }


    private File loadDataFileDummy(TableServerRequest request) throws IOException, DataAccessException {
        //File file = createFile(request, ".json");
        File file = new File(request.getParam("inFileName")+".json");
        try {
            //TableDef tableDef= getMeta(requestMeta);
            DataGroup dg = getTableDataFromJson(file);
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg, 0);
            return outFile;
        }
        catch (ParseException e){
            e.getStackTrace();
        }

       return null;
    }

    private DataGroup getTableDataFromJson(File jsonFile) throws IOException, ParseException {


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


    private DataType[] getDataTypes(JSONArray tableData){



        //Object value=null;
        int lineCount=0;
        //using the first row to get all the column names
        JSONObject rowData = (JSONObject) tableData.get(0);
        String[] keys = (String[]) rowData.keySet().toArray(new String[0]);
        DataType[] types = new DataType[keys.length];

        for (int i=0; i<keys.length; i++) {

            Object value = getValue(keys[i], rowData);

           /* while (value == null && lineCount < tableData.size()) {
                lineCount++;
                rowData = (JSONObject) tableData.get(lineCount);
                value = getValue(keys[i], rowData);
            }*/
            Class cls = value == null ? (new String("s")).getClass() :value.getClass();
                    //LSSTMetaSearch.getDataClass(value.getClass().getTypeName());
            types[i]=new DataType(keys[i],cls );

        }

     /*   String[] keys = (String[]) rowData.keySet().toArray(new String[0]);
        DataType[] types = new DataType[keys.length];
        for (int i=0; i<keys.length; i++){
            Object value = getValue(keys[i], rowData);
            if (value!=null){
                types[i]=new DataType(keys[i], value.getClass());
            }
            else {
                Class cls = (new String()).getClass();
                *//*int j=1;
                while(value==null){
                    JSONObject row = (JSONObject) tableData.get(j);
                    value = getValue(keys[i], row);
                    j++;
                }
                if (value!=null){
                    cls = value.getClass();
                }*//*

                types[i]=new DataType(keys[i],cls );

            }
        }*/
       return types;
    }

    private DataType getDataType(String colName, DataType[] dataType){
        for (int i=0; i<dataType.length; i++){
            if (dataType[i].getKeyName().equalsIgnoreCase(colName)){
                return dataType[i];
            }
        }
        return null;
    }
    private DataObject getRow(DataType[] dataType, String[] cols,  String csvLine, DataGroup dg ) throws Exception {

        String[] strVaues = csvLine.split(",");
        DataObject row = new DataObject(dg);
        for (int i=0; i<strVaues.length; i++){
            DataType type = getDataType(cols[i], dataType);
            if (type==null){
                throw new Exception("no data type define");
            }

            if (strVaues[i].equalsIgnoreCase("null")) {
                type.setMayBeNull(true);
                row.setDataElement(type, null);
            }
            else{
                if (type.getDataType().getTypeName().equalsIgnoreCase("double")){
                    row.setDataElement(type, new Double(strVaues[i]));
                }
                else  if (type.getDataType().getTypeName().equalsIgnoreCase("float")){
                    row.setDataElement(type, new Float(strVaues[i]));
                }  if (type.getDataType().getTypeName().equalsIgnoreCase("bit(1)")){
                    row.setDataElement(type, new Boolean(strVaues[i]));
                } if (type.getDataType().getTypeName().equalsIgnoreCase("bigint")){
                    row.setDataElement(type, new Long(strVaues[i]));
                } if (type.getDataType().getTypeName().equalsIgnoreCase("int(11")){
                    row.setDataElement(type, new Integer(strVaues[i]));
                } if (type.getDataType().getTypeName().equalsIgnoreCase("SMALLINT")){
                    row.setDataElement(type, new Short(strVaues[i]));
                } if (type.getDataType().getTypeName().equalsIgnoreCase("TINYINT")){
                    row.setDataElement(type, new Byte(strVaues[i]));
                }  if (type.getDataType().getTypeName().equalsIgnoreCase("string")){
                    row.setDataElement(type, strVaues[i]);
                }
                else {
                    System.out.println("the type "+type.getDataType().getTypeName() + "is not supported");
                }

            }




        }
        return row;

    }
    private DataGroup getTableDataFromCsv(File  csvFile, TableDef tableDef) throws Exception {

        DataType[] dataType = tableDef.getCols().toArray(new DataType[0]);

        DataGroup dg = new DataGroup("result", dataType);
        BufferedReader reader = new BufferedReader(new FileReader(csvFile));
       //get the first line and then convert to column names
        String line = reader.readLine();
        String[] cols = line.split(",");
        while ((line = reader.readLine()) != null) {
              dg.add(getRow(dataType, cols, line,dg));

        }
        return dg;
    }

    /**
     * This method convert the json data file to data group
     * @param jsonFile
     * @param tableDef
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private DataGroup getTableDataFromJson(File jsonFile, TableDef tableDef) throws IOException, ParseException {
        JSONArray tableData = getData( jsonFile);
       // DataType[]  dataType =  tableDef.getCols().toArray(new DataType[0]);

        DataType[] dataType = getDataTypes( tableData);

        DataGroup dg = new DataGroup("result", dataType  );

        //TODO database has issue with bit value is false.  It has '\0' instead of 0
        for (int i=0; i<tableData.size(); i++){
            JSONObject rowTblData = (JSONObject) tableData.get(i);
            DataObject row = new DataObject(dg);
            for (int j=0; j<dataType.length; j++){
                Object value = getValue(dataType[j].getKeyName(), rowTblData);
                if (j>59){//row 2 and col 65, value is double, but the first row is double
                    System.out.println(dataType[j]);
                    if (value!=null) System.out.println(value.getClass());

                }
               /* if (dataType[j].getDataType()==Boolean.class){
                    boolean b =  ((String) value ).equalsIgnoreCase("\0")? false:true;
                    row.setDataElement(dataType[j],b);
               }
                else {*/

                    if (value==null){
                        dataType[j].setMayBeNull(true);
                        row.setDataElement(dataType[j], null);
                    }
                else {
                        row.setDataElement(dataType[j], value);

                    }
               // }

            }
            System.out.println(row.size());
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
        LSSTCataLogSearch lssCat = new LSSTCataLogSearch();
        File file = lssCat.loadDataFileDummy(request);

       System.out.println("done" + file.getAbsolutePath());



    }
}
