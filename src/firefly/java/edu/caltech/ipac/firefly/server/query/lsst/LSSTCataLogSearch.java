package edu.caltech.ipac.firefly.server.query.lsst;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.List;


/**
 * Created by zhang on 10/10/16.
 */
@SearchProcessorImpl(id = "LSSTCataLogSearch")
public class LSSTCataLogSearch extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    private static String DATA_ACCESS_URI = AppProperties.getProperty("lsst.dataAccess.uri", "lsst.dataAccess.uri");


    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        //File file = createFile(request, ".json");
        File dataFile = new File(request.getParam("table_path")+request.getParam("table_name")+".csv");
       // File dataFile = new File(request.getParam("table_path")+request.getParam("table_name")+".json");

       // File metaFile = new File(request.getParam("table_path")+request.getParam("meta_table")+".tbl");

        TableServerRequest metaRequest = new TableServerRequest("LSSTMetaSearch");
        metaRequest.setParam("table_path",request.getParam("table_path") );
        metaRequest.setParam("table_name",request.getParam("meta_table") );

       // TableDef tableDef=getMeta(metaRequest);

        // TableDef tableDef= IpacTableUtil.getMetaInfo(metaFile);// getMeta(request);
        // DataGroup dg = getTableDataFromJson(dataFile,tableDef);
         DataGroup dg = null;
         try {
             dg = getTableDataFromCsv(dataFile,getMeta(metaRequest));//tableDef);
         } catch (Exception e) {
             _log.error("load table failed");
             e.printStackTrace();
         }
         File outFile = createFile(request, ".tbl");
         dg.shrinkToFitData();
         DataGroupWriter.write(outFile, dg, 0);
        _log.info("table loaded");
         return  outFile;
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
        if (strVaues.length!=cols.length){
            throw new Exception("data length does not match");
        }
        DataObject row = new DataObject(dg);
        for (int i=0; i<strVaues.length; i++){
            DataType type = getDataType(cols[i], dataType);
            if (type==null){
                System.out.println("****name="+cols[i]);
                throw new Exception("no data type define");
            }
            String tname = type.getDataType().getTypeName();
            String pkg="java.lang.";
            if (strVaues[i].equalsIgnoreCase("null")) {
                type.setMayBeNull(true);
                row.setDataElement(type, null);
            }
            else{
                if (tname.equalsIgnoreCase(pkg+"double")){
                    row.setDataElement(type, new Double(strVaues[i]));
                }
                else  if (tname.equalsIgnoreCase(pkg+"float")){
                    row.setDataElement(type, new Float(strVaues[i]));
                }
                else if (tname.equalsIgnoreCase(pkg+"long")){
                    row.setDataElement(type, new Long(strVaues[i]));
                }
                else if (tname.equalsIgnoreCase(pkg+"byte")){
                    row.setDataElement(type, new Byte(strVaues[i]));
                }
                else if (tname.equalsIgnoreCase(pkg+"short")){
                    row.setDataElement(type, new Short(strVaues[i]));
                }
                else if (tname.equalsIgnoreCase(pkg+"int")){
                    row.setDataElement(type, new Integer(strVaues[i]));
                }
                else if (tname.equalsIgnoreCase(pkg+"boolean")){
                    row.setDataElement(type, new Boolean(strVaues[i]));
                }
                else if (tname.equalsIgnoreCase(pkg+"string")){
                    row.setDataElement(type, strVaues[i]);
                }
                else {
                    System.out.println("the type "+type.getDataType().getTypeName() + "is not supported");
                }

            }

        }

        return row;

    }
    private DataGroup getTableDataFromCsv(File  csvFile, DataType[] dataType ) throws Exception {////TableDef tableDef)

       // DataType[] dataType = tableDef.getCols().toArray(new DataType[0]);

        DataGroup dg = new DataGroup("result", dataType);
        BufferedReader reader = new BufferedReader(new FileReader(csvFile));
        try {
            //get the first line and then convert to column names
            String line = reader.readLine();
            String[] cols = line.split(",");
            while ((line = reader.readLine()) != null) {

                DataObject row = getRow(dataType, cols, line, dg);
                dg.add(row);

            }
        }
        finally {
            // Free up file descriptor resources
            reader.close();
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


         JSONArray tableData = getArrayData( jsonFile);
         DataType[]  dataType =  tableDef.getCols().toArray(new DataType[0]);

        DataGroup dg = new DataGroup("result", dataType  );

        for (int i=0; i<tableData.size(); i++){
            JSONObject rowTblData = (JSONObject) tableData.get(i);
            DataObject row = new DataObject(dg);
            for (int j=0; j<dataType.length; j++){
                Object value = getValue(dataType[j].getKeyName(), rowTblData);
                 if (value==null){
                        dataType[j].setMayBeNull(true);
                        row.setDataElement(dataType[j], null);
                 }
                 else {
                       //data stored in the cell should be the correct type as defined
                       row.setDataElement(dataType[j], value);
                  }

            }
            dg.add(row);
        }


        return dg;
    }
    /**
     * This method translates the mySql data type to corresponding java data type
     * @param classType
     * @return
     */
    private Class getDataClass(String classType){

        if (classType.equalsIgnoreCase("double")){
            return Double.class;
        }
        else if (classType.equalsIgnoreCase("float") || classType.equalsIgnoreCase("real") ){
            return Float.class;
        }
        else if (classType.equalsIgnoreCase("int(11)")){
            return Integer.class;
        }
        else if (classType.equalsIgnoreCase("BigInt(20)") ){
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
    private  DataType[] getMetaFromMetatable(DataGroup dataGroup){

        DataType[] dataTypes = new DataType[dataGroup.size()];
        DataObject[] dataObjects=dataGroup.values().toArray(new DataObject[0]);
        for (int i=0; i<dataObjects.length; i++){
            dataTypes[i] = new DataType((String) dataObjects[i].getDataElement("Field"),
                    getDataClass( (String) dataObjects[i].getDataElement("Type") ) );
            boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes")?true:false;
            dataTypes[i] = new DataType( (String) dataObjects[i].getDataElement("Field"),
                    (String) dataObjects[i].getDataElement("description"),
                    getDataClass( (String) dataObjects[i].getDataElement("Type")),
                    DataType.Importance.HIGH,
                    (String) dataObjects[i].getDataElement("unit"),
                    maybeNull
            );

        }

        return dataTypes;
    }
    private DataType[] getMeta(TableServerRequest request){
        SearchManager sm = new SearchManager();

        try {
            FileInfo fi = sm.getFileInfo(request);
            if (fi == null) {
                throw new DataAccessException("Unable to get file location info");
            }
            if (fi.getInternalFilename() == null) {
                throw new DataAccessException("File not available");
            }
            if (!fi.hasAccess()) {
                throw new SecurityException("Access is not permitted.");
            }
            DataGroup sourceDataGroup = DataGroupReader.readAnyFormat(new File(fi.getInternalFilename()));
            return getMetaFromMetatable(sourceDataGroup);

        } catch (Exception e) {
            e.getStackTrace();
        }
        return null;
    }


    private  JSONArray getArrayData(File jsonFile) throws IOException, ParseException {


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

    private  Object getValue(String colName,JSONObject rowTblData){

        for (Object key : rowTblData.keySet()) {
            //based on you key types
            String keyStr = (String)key;
            if (keyStr.equalsIgnoreCase(colName) ) {
                return rowTblData.get(keyStr);

            }
        }
        return null;
    }

  /*  private File loadDataFileDummy(TableServerRequest request) throws IOException, DataAccessException {
        //File file = createFile(request, ".json");
        File dataFile = new File(request.getParam("table_path")+request.getParam("table_name")+".csv");
        // File dataFile = new File(request.getParam("table_path")+request.getParam("table_name")+".json");
        File metaFile = new File(request.getParam("table_path")+request.getParam("meta_table")+".tbl");



        TableDef tableDef= IpacTableUtil.getMetaInfo(metaFile);// getMeta(request);
        // DataGroup dg = getTableDataFromJson(dataFile,tableDef);
        DataGroup dg = null;
        try {
            dg = getTableDataFromCsv(dataFile,tableDef);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // File outFile = createFile(request, ".tbl");
        File outFile = new File(request.getParam("table_path")+ "out_"+request.getParam("table_name")+".json");
        dg.shrinkToFitData();
        DataGroupWriter.write(outFile, dg, 0);
        return  outFile;
    }

    public static void main(String[] args) throws IOException, ParseException, DataAccessException {

        String jsonFileName = "RunDeepSource_ra_btw";

        TableServerRequest request = new TableServerRequest("DummyTable");
        request.setParam("table_name","RunDeepSource_ra_btw");
        request.setParam("table_path", "/hydra/cm/firefly_test_data/DAXTestData/");
        request.setParam("meta_table","output_RunDeepSourceDD");
        request.setParam("inFileName",dataPath +jsonFileName );
        request.setParam(ServerRequest.ID_KEY, "RunDeepSource_ra_btw");

        LSSTCataLogSearch lssCat = new LSSTCataLogSearch();
        File file = lssCat.loadDataFileDummy(request);

       System.out.println("done" + file.getAbsolutePath());



    }*/
}
