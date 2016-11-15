package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.*;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.download.FileData;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.Exception;

import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.firefly.data.table.MetaConst;


/**
 * Created by zhang on 10/10/16.
 * This is the Catalog search processor.  For any given target (ra and dec, except in polygon), it searches based
 * the search method.  It supports four search methods:
 *   1.  cone
 *   2.  box
 *   3.  Elliptical
 *   4.  Polygon
 *
 *   For cone, box and polygon searches, all input have to be in degree unit
 *   For Elliptical, ra and dec are in degree and the semi axis are in arcsec.
 *
 */
@SearchProcessorImpl(id = "LSSTCataLogSearch")
public class LSSTCataLogSearch extends IpacTablePartProcessor {
    private static final String RA = "coord_ra";
    private static final String DEC = "coord_decl";

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String PORT = "5000";
    private static final String HOST = AppProperties.getProperty("lsst.dd.hostname","lsst-qserv-dax01.ncsa.illinois.edu");
    //TODO how to handle the database name??
    private static final String DATABASE_NAME =AppProperties.getProperty("lsst.database" , "");
    //set default timeout to 120seconds
    private int timeout  = new Integer( AppProperties.getProperty("lsst.database.timeoutLimit" , "120")).intValue();
    //define all methods here
    private static final CatalogRequest.Method allSearchMethods[] = {CatalogRequest.Method.CONE,
            CatalogRequest.Method.ELIPTICAL,
            CatalogRequest.Method.BOX,
            CatalogRequest.Method.POLYGON,
            CatalogRequest.Method.TABLE,
            CatalogRequest. Method.ALL_SKY};

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        try {
            DataGroup dg = getDataFromURL(request);
            dg.shrinkToFitData();
            File outFile = createFile(request, ".tbl");
            DataGroupWriter.write(outFile, dg,0);
            _log.info("table loaded");
            return  outFile;

         } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException("ERROR:" + e.getMessage(), e);
        }
    }


    private CatalogRequest.Method getMethod (String searchMethod){

        CatalogRequest.Method retval = null;
        for (CatalogRequest.Method m : allSearchMethods) {
            if (m.getDesc().equals(searchMethod)) {
                retval = m;
                break;
            }
        }
        return retval;

    }
    /**
     * This method will return the search method string based on the method.  If the method is not supported, the exception
     * will be thrown
     *
     * @param req
     * @return
     * @throws Exception
     */
    protected String getSearchMethod(TableServerRequest req, String raCol, String decCol) throws Exception {


        CatalogRequest.Method method = getMethod(req.getParam("SearchMethod"));
        String[]  radec = req.getParam("UserTargetWorldPt")!=null? req.getParam("UserTargetWorldPt").split(";"):null;
        String ra =radec!=null? radec[0]:"";
        String dec = radec!=null?radec[1]:"";

        switch (method) {
            case ALL_SKY:
                return "";
            case BOX:
                //The unit is degree for all the input
                String side = req.getParam(CatalogRequest.SIZE);
                WorldPt wpt = new WorldPt(new Double(ra).doubleValue(), new Double(dec).doubleValue());
                //getCorners using arcsec in radius unit
                VisUtil.Corners corners  = VisUtil. getCorners(wpt, new Double(side).doubleValue()/2.0*3600.0);

                String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
                String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());
                return "scisql_s2PtInBox("+ raCol+"," + decCol+"," +  lowerRight + "," +upperLeft + ")=1";

            case CONE:
                //The unit is degree for all the input
                String radius = req.getParam(CatalogRequest.RADIUS);
                return "scisql_s2PtInCircle("+ raCol+"," + decCol+","+ra +","+dec+","+radius +")=1";
           case ELIPTICAL:
               //RA (degree), DEC (degree), positionAngle (degree), semi-majorAxis (arcsec), semi-minorAxis(arcsec),
               double semiMajorAxis = new Double( req.getParam("radius")).doubleValue()*3600;
               double ratio = new Double(req.getParam(CatalogRequest.RATIO)).doubleValue();
               Double semiMinorAxis = semiMajorAxis*ratio;
               String positionAngle = req.getParam("posang");
               return  "scisql_s2PtInEllipse("+ raCol+"," + decCol+"," + ra + "," + dec + "," + semiMajorAxis + "," +
                       semiMinorAxis + "," + positionAngle + ")=1";
            case POLYGON:
                //The unit is degree for all the input
                String radecList = req.getParam(CatalogRequest.POLYGON);
                String[] sArray = radecList.split(",");
                String polygoneStr = "scisql_s2PtInCPoly("+ raCol+"," + decCol+",";
                for (int i=0; i<sArray.length; i++){
                    String[] radecPair = sArray[i].trim().split("\\s+");
                    if (radecPair.length!=2){
                        throw new Exception("wrong data entered");
                    }
                    if (i==sArray.length-1) {
                        polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ")=1";
                    }
                    else {
                        polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ",";
                    }
                }
                return polygoneStr;

            case TABLE:
                //TODO what to do in multi-obj
                throw new EndUserException("Could not do Multi Object search, internal configuration wrong.",
                        "table should be a post search not a get");
            default:
               // should only be happened if a new method was added and not added here
                throw new EndUserException("The search method is not supported", req.getParam("SearchMethod"));
        }

    }


    String getConstraints(TableServerRequest request) {
        String constraints = request.getParam(CatalogRequest.CONSTRAINTS);
        if (!StringUtils.isEmpty(constraints) && constraints.contains(CatalogRequest.CONSTRAINTS_SEPARATOR)) {
            constraints = constraints.replace(CatalogRequest.CONSTRAINTS_SEPARATOR, " and ");
        }
        return constraints;
    }
    String buildSqlQueryString(TableServerRequest request) throws Exception {

        String tableName = request.getParam("table_name");

        boolean usingRaDec=true;
        if (tableName.equalsIgnoreCase("RunDeepSource") || tableName.equalsIgnoreCase("RunDeepForcedSource")){
           usingRaDec=false;
        }
        String raColumn=usingRaDec?"ra":"coord_ra";
        String decColumn=usingRaDec?"decl":"coord_decl";

        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            //throw new RuntimeException(CatalogRequest.CATALOG + " parameter is required");
            catTable =DATABASE_NAME.length()==0?tableName: DATABASE_NAME+"."+ tableName;
        }



        String columns = request.getParam(CatalogRequest.SELECTED_COLUMNS);
        if (columns==null){
            columns = "*";
        }

        //get all the constraints
        String constraints =  getConstraints(request);
        //get the search method
        String searchMethod = getSearchMethod( request, raColumn,decColumn);

        //build where clause
        String whereStr;
        if (searchMethod.length()>0 && constraints.length()>0){
            whereStr = searchMethod +  " AND " + constraints;
        }
        else if (searchMethod.length()>0 &&constraints.length()==0 ){
            whereStr = searchMethod ;
        }
        else if ( searchMethod.length()==0 &&constraints.length()>0 ){
            whereStr = constraints;
        }
        else {
            whereStr="";
        }



        String sql = "SELECT " + columns + " FROM " + catTable;
        sql =whereStr.length()>0? sql +  " WHERE " + whereStr + ";": sql+ ";";

        return sql;
    }

    private DataGroup  getDataFromURL(TableServerRequest request) throws Exception {


           String sql = "query=" + URLEncoder.encode(buildSqlQueryString(request),"UTF-8");


          long cTime = System.currentTimeMillis();
          _log.briefDebug("Executing SQL query: " + sql);
          String url = "http://"+HOST +":"+PORT+"/db/v0/tap/sync";

          File file = createFile(request, ".json");
          Map<String, String> requestHeader=new HashMap<>();
          requestHeader.put("Accept", "application/json");
          FileData fileData = URLDownload.getDataToFileUsingPost(new URL(url),sql,null,  requestHeader, file, null, timeout);

          if (fileData.getResponseCode()>=500) {
              throw new DataAccessException("DAX Error: "+ LSSTMetaSearch.getErrorMessageFromFile(file));

          }

           DataGroup dg =  getTableDataFromJson( request,file);
          _log.briefDebug("SHOW COLUMNS took " + (System.currentTimeMillis() - cTime) + "ms");

          return dg;


    }

    private Object convertType(Object d, DataType dt) throws Exception {
         Class targetType = dt.getDataType();
         if (targetType.getTypeName().equalsIgnoreCase("java.lang.integer")  && d instanceof Long ){
             return new Long((long) d).intValue();
         }
        else if (targetType.getTypeName().equalsIgnoreCase("java.lang.float")  && d instanceof Double ) {
            return new Double((double) d).floatValue();
        }
       else  if (targetType.getTypeName().equalsIgnoreCase("java.lang.byte")  && d instanceof Short ) {
            return new Short((short) d).byteValue();
        }

       else if (targetType.getTypeName().equalsIgnoreCase("java.lang.short")  && d instanceof Integer ) {
            return new Integer((int) d).shortValue();
        }
       else{
             throw new Exception(d.getClass() + "to " + targetType + "is not supported");
         }
    }

    /**
     * This method convert the json data file to data group
     * @param jsonFile
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private DataGroup getTableDataFromJson(TableServerRequest request,  File jsonFile) throws Exception {

        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(new FileReader(jsonFile));
        JSONArray data =  (JSONArray) ((JSONObject) ((JSONObject) obj.get("result")).get("table")).get("data");

        //search returns empty, throw no data exception
        if (data.size()==0) {
            throw new DataAccessException("No data is found in the search range");

        }

        //TODO this should NOT be needed when the MetaServer is running
        JSONArray metaInData = (JSONArray) ( (JSONObject) ( (JSONObject)( (JSONObject) obj.get("result")).get("table")).get("metadata")).get("elements");
        DataType[] dataType = getTypeDef(request, metaInData);

        DataGroup dg = new DataGroup("result", dataType  );

        //add column description as the attribute so that it can be displayed
        for (int i=0; i<dataType.length; i++){
            dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, dataType[i].getKeyName()),
                    dataType[i].getShortDesc());
        }

        for (int i=0; i<data.size(); i++){
            JSONArray  rowTblData = (JSONArray) data.get(i);
            DataObject row = new DataObject(dg);
            for (int j=0; j<dataType.length; j++){

                Object d = rowTblData.get(j);

                if (d==null){
                    dataType[j].setMayBeNull(true);
                    row.setDataElement(dataType[j], null);
                }
                else  {

                    //if it is a boolean, convert the "text" to boolean
                    if (dataType[j].getDataType().getTypeName().equalsIgnoreCase("java.lang.Boolean")){
                        char c = d.toString().toCharArray()[0];
                        if (c=='\u0000'){
                            System.out.println(c);
                        }
                        if (d.toString().equalsIgnoreCase(new String("\u0000"))  || d.toString().length()==0) {//\u0000 is "", an empty string
                            d=new Boolean(false);
                        }
                        else {
                            d=new Boolean(true);
                        }
                    }
                    try {
                        row.setDataElement(dataType[j], d);
                    }
                    catch (Exception e){
                        //when the parser does not translate the type right, force the type to be converted
                          row.setDataElement(dataType[j], convertType(d,dataType[j]));
                    }
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
    private Class getDataClass(String classType) throws DataAccessException {

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
        else if (classType.equalsIgnoreCase("bit(1)") || classType.equalsIgnoreCase("boolean")){
            return Boolean.class;
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
       // return null;

    }


    private DataType[] geDataTypeFromMetaSearch(TableServerRequest request) throws DataAccessException {
        TableServerRequest metaRequest = new TableServerRequest("LSSTMetaSearch");
        metaRequest.setParam("table_name", request.getParam("meta_table"));
        metaRequest.setPageSize(Integer.MAX_VALUE);
        //call LSSTMetaSearch processor to get the meta data as a DataGroup
        DataGroup metaData = getMeta(metaRequest);
        DataObject[] dataObjects = metaData.values().toArray(new DataObject[0]);
        DataType[] dataTypes = new DataType[dataObjects.length];
        for (int i = 0; i < dataObjects.length; i++) {
            boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes") ? true : false;
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
        for (int i=0; i<allColumns.length; i++){
            if (allColumns[i].getKeyName().equalsIgnoreCase(colName)){
                return allColumns[i];
            }
        }
        return null;
    }

    //TODO this method will be usd when the MetaServer is working
    private  DataType[] getTypeDef(TableServerRequest request) throws IOException, DataAccessException {

        DataType[] allColumns = geDataTypeFromMetaSearch(request);
        String[] selColumns = request.getParam(CatalogRequest.SELECTED_COLUMNS).split(",");
        if (selColumns==null) {

            return allColumns;
        }
        else {
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
    //TODO this method will not needed when teh MetaServer is running and the data types are consistent
    private  DataType[] getTypeDef(TableServerRequest request, JSONArray columns)  throws  DataAccessException {


        TableServerRequest metaRequest = new TableServerRequest("LSSTMetaSearch");
        metaRequest.setParam("table_name", request.getParam("meta_table"));
        metaRequest.setPageSize(Integer.MAX_VALUE);
        //call LSSTMetaSearch processor to get the meta data as a DataGroup
        DataGroup metaData = getMeta(metaRequest);

        DataType[] dataTypes = new DataType[columns.size()];
        DataObject[] dataObjects = metaData.values().toArray(new DataObject[0]);

        //all columns are selected, the default
        if (columns.size() == dataObjects.length) {
            for (int i = 0;i < columns.size(); i++) {
                 JSONObject col = (JSONObject) columns.get(i);
                 boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes") ? true : false;
                //TODO always get the data type from the data meta
                 Class cls = getDataClass(col.get("datatype").toString());
                if (cls==null){
                    cls =  getDataClass( (String) dataObjects[i].getDataElement("Type"));
                }
                 String colName  = col.get("name").toString().trim();
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
                for (int i = 0; i < dataObjects.length; i++) {
                    String keyName = ((String) dataObjects[i].getDataElement("Field")).trim();
                    if (keyName.equalsIgnoreCase(col.get("name").toString().trim())) {
                        boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes") ? true : false;
                        //TODO always get the data type from the data meta unless it is null
                        Class cls = getDataClass(col.get("datatype").toString());
                        if (cls==null){
                            cls =  getDataClass( (String) dataObjects[i].getDataElement("Type"));
                        }
                        dataTypes[k] = new DataType(keyName, keyName,
                                cls,
                                DataType.Importance.HIGH,
                                (String) dataObjects[i].getDataElement("Unit"),
                                maybeNull
                        );
                        dataTypes[k].setShortDesc((String) dataObjects[i].getDataElement("Description"));
                        break;
                    }
                }
            }
        }

        return dataTypes;
    }

    /**
     * This method is calling the LSSTMetaSearch processor to search the data type definitions
     * @param request
     * @return
     */
    private DataGroup getMeta(TableServerRequest request){


        SearchManager sm = new SearchManager();
        try {
            DataGroupPart dgp = sm.getDataGroup(request);

           return dgp.getData();

        } catch (Exception e) {
            e.getStackTrace();
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

        TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns(RA, DEC, CoordinateSys.EQ_J2000);
        meta.setCenterCoordColumns(llc);
        meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "LSST");
        super.prepareTableMeta(meta, columns, request);
    }

}
