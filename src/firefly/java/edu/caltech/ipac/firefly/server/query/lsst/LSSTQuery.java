/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.util.FileUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * This is a base class for LSSTCatlogSearch and LSSTLightCurveQuery
 * DM-9964:
 *   Since the MetaSeaver is not ready the meta data(columns) are from the serach result.  Therefore, the empty table can not be
 *   displayed since there is no column information.  In order to let multi-object search work, the set the DataGroup=null when
 *   the empty result is returned.  The loadFile will check and display the error message if the dg==null.
 *
 *   In Mutli-Objet search, it the dg==null, it skip this data group.
 *
 */
public abstract class LSSTQuery extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String PORT = "5000";
    private static final String HOST = AppProperties.getProperty("lsst.dd.hostname","lsst-qserv-dax01.ncsa.illinois.edu");
    //TODO how to handle the database name??
    //protected static final String DATABASE_NAME =AppProperties.getProperty("lsst.database" , "");
    //set default timeout to 180 seconds
    private int timeout  = Integer.parseInt(AppProperties.getProperty("lsst.database.timeoutLimit", "180"));

    abstract String buildSqlQueryString(TableServerRequest request) throws Exception;

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        try {
            DataGroup dg = getDataFromURL(request);
            //DM-9964 : TODO this is a tempoary solution until the meta server is up
            if (dg==null){
                throw new DataAccessException("No data is found in the search range");
            }
            dg.shrinkToFitData();
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg);
            _log.info("table loaded");
            return  outFile;

         } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }
    }

    DataGroup  getDataFromURL(TableServerRequest request) throws Exception {

        String sql = "query=" + URLEncoder.encode(buildSqlQueryString(request),"UTF-8");


        long cTime = System.currentTimeMillis();
        _log.briefDebug("Executing SQL query: " + sql);
        String url = "http://"+HOST +":"+PORT+"/" + LSSTQuery.getDatasetInfo(request.getParam("database"),
                                                                            request.getParam("table_name"),
                                                                            new String[]{"meta"});

        File file = createFile(request, ".json");
        Map<String, String> requestHeader=new HashMap<>();
        requestHeader.put("Accept", "application/json");
        FileInfo fileData = URLDownload.getDataToFileUsingPost(new URL(url),sql,null,  requestHeader, file, null, timeout);

        if (fileData.getResponseCode()>=500) {
            throw new DataAccessException("DAX Error: "+ LSSTMetaSearch.getErrorMessageFromFile(file));

        }

        DataGroup dg =  getTableDataFromJson( request,file);
        _log.briefDebug("SHOW COLUMNS took " + (System.currentTimeMillis() - cTime) + "ms");

        return dg;

    }

    /**
     * This method convert the json data file to data group
     * @param request table request
     * @param jsonFile JSON file with the result
     * @return DataGroup
     * @throws IOException
     * @throws ParseException
     */
    private DataGroup getTableDataFromJson(TableServerRequest request,  File jsonFile) throws Exception {



        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(new FileReader(jsonFile));
        JSONArray data =  (JSONArray) ((JSONObject) ((JSONObject) obj.get("result")).get("table")).get("data");


        //search returns empty, throw no data exception
 /*       if (data.size()==0) {
            throw new DataAccessException("No data is found in the search range");

        }
*/
        //no data found, return the meta data only
        if (data.size()==0){
            return  null;
        }

        //TODO this should NOT be needed when the MetaServer is running
        JSONArray metaInData = (JSONArray) ( (JSONObject) ( (JSONObject)( (JSONObject) obj.get("result")).get("table")).get("metadata")).get("elements");
        DataType[] dataType = getTypeDef(request, metaInData);
        DataGroup dg = new DataGroup("result", dataType  );

        //add column description as the attribute so that it can be displayed
        for (DataType dt : dataType) {
            dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, dt.getKeyName()),
                    dt.getShortDesc());
        }

        for (Object jsonRow : data) {
            JSONArray rowTblData = (JSONArray) jsonRow;
            DataObject row = new DataObject(dg);
            for (int j = 0; j < dataType.length; j++) {
                Object d = rowTblData.get(j);
                if (d == null) {
                    dataType[j].setMayBeNull(true);
                    row.setDataElement(dataType[j], null);
                    continue;
                }
                if (d instanceof Number) {
                    Number nd = (Number) d;
                    addNumberToRow(dataType[j], nd, row);
                } else if (d instanceof String) {
                    String sd = (String) d;
                    addStringToRow(dataType[j], sd, row);
                } else {
                    throw new Exception(d.getClass() + "to " + dataType[j].getDataType().getSimpleName() + " is not supported");
                }
            }
            dg.add(row);
        }

        return dg;
    }

    /**
     * This method add a number to a DataObject
     * @param dataType data type
     * @param nd       number object
     * @param row      row object
     */
    private void addNumberToRow(DataType dataType, Number nd,DataObject row ) {
        switch (dataType.getDataType().getSimpleName()){
            case "Byte":
                row.setDataElement(dataType, nd.byteValue());
                break;
            case "Short":
                row.setDataElement(dataType, nd.shortValue());
                break;
            case "Integer":
                row.setDataElement(dataType, nd.intValue());
                break;
            case "Long":
                row.setDataElement(dataType, nd.longValue());
                break;
            case "Float":
                row.setDataElement(dataType, nd.floatValue());
                break;
            case "Double":
                row.setDataElement(dataType, nd.doubleValue());
                break;

        }
    }

    /**
     * This method adds a String to a DataObject
     * @param dataType data type
     * @param sd       string to be added
     * @param row      row object
     */
    private void addStringToRow(DataType dataType, String sd,DataObject row){
        switch (dataType.getDataType().getSimpleName()) {
            case "Boolean":
                char c = sd.toCharArray()[0];
                if (c == '\u0000') { // null control character
                    System.out.println(c);
                }
                if (sd.equalsIgnoreCase("\u0000") || sd.length() == 0) {//\u0000 is "", an empty string
                    row.setDataElement(dataType, false);
                } else {
                    row.setDataElement(dataType, true);
                }
                break;
            case "String":
                row.setDataElement(dataType, sd);
                break;
        }
     }


    //TODO this method will be used when the MetaServer is working
    private  DataType[] getTypeDef(TableServerRequest request) throws IOException, DataAccessException {

        DataType[] allColumns = geDataTypeFromMetaSearch(request);
        String selColumnsStr = request.getParam(CatalogRequest.SELECTED_COLUMNS);
        if (selColumnsStr == null) {
            return allColumns;
        } else {
            String[] selColumns = selColumnsStr.split(",");
            DataType[] dataTypes = new DataType[selColumns.length];

            for (int i = 0; i < selColumns.length; i++) {
                dataTypes[i]=getDataType(allColumns, selColumns[i]);
                if (dataTypes[i]==null){
                    throw new IOException(selColumns[i]+ " Is not found");
                }
            }
            return dataTypes;
        }
    }

    //TODO this method will not needed when the MetaServer is running and the data types are consistent
    public static  DataType[] getTypeDef(TableServerRequest request, JSONArray columns)  throws  DataAccessException {

        DataType[] dataTypes = new DataType[columns.size()];

        if (request.getParam("meta_table") != null) {
            TableServerRequest metaRequest = new TableServerRequest("LSSTMetaSearch");
            metaRequest.setParam("table_name", request.getParam("meta_table"));
            metaRequest.setParam("database", request.getParam("database"));
            metaRequest.setPageSize(Integer.MAX_VALUE);
            //call LSSTMetaSearch processor to get the meta data as a DataGroup
            DataGroup metaData = getMeta(metaRequest);
            DataObject[] dataObjects = metaData.values().toArray(new DataObject[metaData.size()]);

            //all columns are selected, the default
            if (columns.size() == dataObjects.length) {
                for (int i = 0; i < columns.size(); i++) {
                    JSONObject col = (JSONObject) columns.get(i);
                    boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes");
                    //TODO always get the data type from the data meta
                    Class cls = getDataClass(col.get("datatype").toString());
                    if (cls == null) {
                        cls = getDataClass((String) dataObjects[i].getDataElement("Type"));
                    }
                    String colName = col.get("name").toString().trim();
                    dataTypes[i] = new DataType(colName, colName,
                            cls,
                            DataType.Importance.HIGH,
                            (String) dataObjects[i].getDataElement("Unit"),
                            maybeNull
                    );
                    dataTypes[i].setShortDesc((String) dataObjects[i].getDataElement("Description"));
                }


            } else {
                for (int k = 0; k < columns.size(); k++) {
                    JSONObject col = (JSONObject) columns.get(k);
                    for (DataObject dataObject : dataObjects) {
                        String keyName = ((String) dataObject.getDataElement("Field")).trim();
                        if (keyName.equalsIgnoreCase(col.get("name").toString().trim())) {
                            boolean maybeNull = dataObject.getDataElement("Null").toString().equalsIgnoreCase("yes");
                            //TODO always get the data type from the data meta unless it is null
                            Class cls = getDataClass(col.get("datatype").toString());
                            if (cls == null) {
                                cls = getDataClass((String) dataObject.getDataElement("Type"));
                            }
                            dataTypes[k] = new DataType(keyName, keyName,
                                    cls,
                                    DataType.Importance.HIGH,
                                    (String) dataObject.getDataElement("Unit"),
                                    maybeNull
                            );
                            dataTypes[k].setShortDesc((String) dataObject.getDataElement("Description"));
                            break;
                        }
                    }
                }
            }
        } else {
            // no meta info except the one that came with the result
            for (int i = 0; i < columns.size(); i++) {
                JSONObject col = (JSONObject) columns.get(i);
                String keyName = col.get("name").toString().trim();
                Class cls = getDataClass(col.get("datatype").toString());
                dataTypes[i] = new DataType(keyName, cls);
            }
        }

        return dataTypes;
    }


    /**
     * This method is calling the LSSTMetaSearch processor to search the data type definitions
     * @param request table request
     * @return DataGroup with metadata
     */
    public static DataGroup getMeta(TableServerRequest request) throws DataAccessException {

        SearchManager sm = new SearchManager();
        try {
            DataGroupPart dgp = sm.getDataGroup(request);
            return dgp.getData();

        } catch (Exception e) {
            throw new DataAccessException("Unable to get metadata", e);
        }
    }


    private DataType[] geDataTypeFromMetaSearch(TableServerRequest request) throws DataAccessException {
        TableServerRequest metaRequest = new TableServerRequest("LSSTMetaSearch");
        metaRequest.setParam("table_name", request.getParam("meta_table"));
        metaRequest.setPageSize(Integer.MAX_VALUE);
        //call LSSTMetaSearch processor to get the meta data as a DataGroup
        DataGroup metaData = getMeta(metaRequest);
        DataObject[] dataObjects = metaData.values().toArray(new DataObject[metaData.size()]);
        DataType[] dataTypes = new DataType[dataObjects.length];
        for (int i = 0; i < dataObjects.length; i++) {
            boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes");
            String colName = dataObjects[i].getDataElement("Field").toString();
            dataTypes[i] = new DataType(colName, colName,
                    getDataClass((String) dataObjects[i].getDataElement("Type")),
                    DataType.Importance.HIGH,
                    (String) dataObjects[i].getDataElement("Unit"),
                    maybeNull
            );
            dataTypes[i].setShortDesc((String) dataObjects[i].getDataElement("Description"));
        }
        return dataTypes;
    }

    private DataType  getDataType(DataType[] allColumns, String colName){
        for (DataType col : allColumns) {
            if (col.getKeyName().equalsIgnoreCase(colName)) {
                return col;
            }
        }
        return null;
    }

    /**
     * This method translates the mySql data type to corresponding java data type
     * @param classType data type from the database
     * @return corresponding Java class
     */
    public static Class getDataClass(String classType) throws DataAccessException {

        if (classType.equalsIgnoreCase("double")){
            return Double.class;
        }
        else if (classType.equalsIgnoreCase("float") || classType.equalsIgnoreCase("real") ){
            return Float.class;
        }
        else if (classType.equalsIgnoreCase("int(11)") || classType.equalsIgnoreCase("int")){
            return Integer.class;
        }
        else if (classType.equalsIgnoreCase("BigInt(20)") ||  classType.equalsIgnoreCase("long")){
            return Long.class;
        }
        else if (classType.equalsIgnoreCase("TINYINT") || classType.equalsIgnoreCase("byte")){
            return Byte.class;
        }
        else if (classType.equalsIgnoreCase("SMALLINT") || classType.equalsIgnoreCase("short)")){
            return Short.class;
        }
        else if (classType.equalsIgnoreCase("string") || classType.equalsIgnoreCase("text") ||
                classType.equalsIgnoreCase("character") ||   classType.equalsIgnoreCase("varchar") ||
                classType.equalsIgnoreCase("longchar") | classType.equalsIgnoreCase("binary")) {

            return String.class;

        }
        else if (classType.equalsIgnoreCase("bit(1)") || classType.equalsIgnoreCase("boolean")){
            return Boolean.class;
        }
        /*else if (classType.equalsIgnoreCase("binary") || classType.equalsIgnoreCase("varbinary")
                                                      || classType.equalsIgnoreCase("longvarbinary")){
            return Byte[].class;
        }*/
        else if ( classType.equalsIgnoreCase("date") ){
            return java.sql.Date.class;
        }
        else if ( classType.equalsIgnoreCase("time") ){
            return java.sql.Time.class;
        }
        else if ( classType.equalsIgnoreCase("timestamp") ){
            return java.sql.Timestamp.class;
        }
        else if ( classType.equalsIgnoreCase("NUMERIC") || classType.equalsIgnoreCase("DECIMAL")){
            return java.math.BigDecimal.class;
        }
        else {
            System.out.println(classType + "is not supported");
            throw new DataAccessException(classType + "is not handled");
        }

    }

    private static JSONObject jsonDataSet = LSSTQuery.getDataSet();
    private static JSONObject getDataSet() {
        JSONObject obj = new JSONObject();

        try {
            InputStream lsstMetaInfo = LSSTQuery.class.getResourceAsStream("/edu/caltech/ipac/firefly/resources/LSSTMetaInfo.json");
            String lsstMetaStr = FileUtil.readFile(lsstMetaInfo);
            obj = (JSONObject) new JSONParser().parse(lsstMetaStr);
        } catch (ParseException e) {
            LOGGER.error(e);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return obj;
    }

    public static Object getDatasetInfo(String databaseName, String tableName, String[] pathAry) {
        for (Object key : jsonDataSet.keySet()) {     // test SDSS or WISE
            JSONObject missionObj = (JSONObject)jsonDataSet.get(key);
            String[]   dataSet = {"catalog", "imagemeta"};

            for (Object dataType : dataSet) {      // test imagemeta or catalog
                JSONArray missionTableSet = (JSONArray)missionObj.get(dataType);

                for (Object missionData : missionTableSet) {   // find table group

                    if (!(missionData instanceof JSONObject)) continue;
                    if (!(((JSONObject)missionData).get("database")).equals(databaseName)) {
                        continue;
                    }
                    Object tables = ((JSONObject)missionData).get("tables");
                    if ((tables instanceof JSONArray) &&
                        ((JSONArray)tables).contains(tableName)) {

                        Object pathObj = missionData;

                        for (String path : pathAry) {
                            if (pathObj instanceof JSONObject) {
                                pathObj = ((JSONObject) pathObj).get(path);
                            } else {
                                pathObj = null;
                                break;
                            }
                        }
                        return pathObj;
                    }
                }
            }
        }

        return null;
    }

    public static Object getImageMetaSchema(String database, String tableName) {
        for (Object key : jsonDataSet.keySet()) {
            JSONObject missionObj = (JSONObject)jsonDataSet.get(key);
            Object   imageDataSets = missionObj.get("imagemeta");
            Object   foundImageSet = null;

            if (imageDataSets == null || !(imageDataSets instanceof JSONArray)) {
                continue;
            }

            for (Object imageDataset : (JSONArray)imageDataSets) {
                if (database.equals(((JSONObject)imageDataset).get("database"))) {
                    Object tables = ((JSONObject) imageDataset).get("tables");
                    if ((tables instanceof JSONArray) &&
                        ((JSONArray)tables).contains(tableName)) {
                            foundImageSet = imageDataset;
                            break;
                    }
                }
            }

            if (foundImageSet == null) continue;

            Object schemaObj = ((JSONObject)foundImageSet).get("schema");

            if (schemaObj instanceof JSONObject) {
                for (Object schemaKey : ((JSONObject)schemaObj).keySet()) {
                    Object schema = ((JSONObject)schemaObj).get(schemaKey);
                    JSONArray tables = (JSONArray)((JSONObject) schema).get("tables");

                    if (tables.contains(tableName)) {
                        return ((JSONObject)schema).get("params");
                    }
                }
                break;   // no schema group is found
            }
        }
        return null;
    }

    // get table depenent column name
    // columnType: "objectColumn", "filterColumn"
    public static String getTableColumn(String database, String tableName, String columnType) {
        Object tables = getDatasetInfo(database, tableName, new String[]{"tables"});
        if (tables instanceof JSONArray) {
            JSONArray jTables = (JSONArray)tables;
            int tableIdx = -1;

            for (int i = 0; i < jTables.size(); i++) {
                if (jTables.get(i).toString().equals(tableName)) {
                    tableIdx = i;
                    break;
                }
            }

            if (tableIdx >= 0) {
                Object cols = getDatasetInfo(database, tableName, new String[]{columnType});

                if ((cols instanceof JSONArray) && (((JSONArray)cols).size() > tableIdx) ) {
                    return ((JSONArray)cols).get(tableIdx).toString();
                }
            }
        }

        return null;
    }

    static String[] getDBTableNameFromRequest(TableServerRequest request) {
        String tableName = request.getParam("table_name");
        String catTable = request.getParam(CatalogRequest.CATALOG);
        String database = request.getParam(CatalogRequest.DATABASE);

        if (catTable == null || catTable.length() == 0) {
            catTable = tableName;
        }

        return new String[]{database, catTable};
    }

    static Boolean isCatalogTable(String database, String catTable) {
        String type = (String)LSSTQuery.getDatasetInfo(database, catTable, new String[]{"datatype"});
        return (type != null)&&type.startsWith("catalog");
    }
}
