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
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.*;
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
    private static final String DATABASE_NAME =AppProperties.getProperty("lsst.database" , "gapon_sdss_stripe92_patch366_0");


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
             _log.error("load table failed");
             e.printStackTrace();
         }
         return null;
    }



    protected String getSearchMethod(TableServerRequest req) throws Exception {

        if (req.getParam("SearchMethod").equalsIgnoreCase("polygon")) {
            String radecList = req.getParam(CatalogRequest.POLYGON);
            String[] sArray = radecList.split(",");
            String polygoneStr = "scisql_s2PtInCPoly(coord_ra,coord_decl,";
            for (int i=0; i<sArray.length; i++){
                String[] radecPair = sArray[i].trim().split("\\s+");
                if (radecPair.length!=2){
                    throw new Exception("wrong data entered");
                }
                if (i==sArray.length-1) {
                    polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ")=1;";
                }
                else {
                    polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ",";
                }
            }
            return polygoneStr;
        }
        else{
            String[]  radec = req.getParam("UserTargetWorldPt").split(";");
            String ra = radec[0];
            String dec = radec[1];
            if (req.getParam("SearchMethod").equalsIgnoreCase("box")) {
                String side = req.getParam(CatalogRequest.SIZE);
                WorldPt wpt = new WorldPt(new Double(ra).doubleValue(), new Double(dec).doubleValue());
                VisUtil.Corners corners  = VisUtil. getCorners(wpt, new Double(side).doubleValue());
                String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
                String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());
                return "scisql_s2PtInBox(coord_ra,coord_decl," +  lowerRight + "," +upperLeft + ")=1;";

            }
            else if (req.getParam("SearchMethod").equalsIgnoreCase("cone")) {
                String radius = req.getParam(CatalogRequest.RADIUS);
                //TODO remove it after the unit is degree
                if (!radius.equalsIgnoreCase("0.01")){
                    radius="0.01";
                }
                return "scisql_s2PtInCircle(coord_ra,coord_decl,"+ra +","+dec+","+radius +")=1;";

            } else if (req.getParam("SearchMethod").equalsIgnoreCase("Eliptical")) {
                double semiMajorAxis = new Double( req.getParam("radius")).doubleValue();
                double ratio = new Double(req.getParam(CatalogRequest.RATIO)).doubleValue();
                Double semiMinorAxis = semiMajorAxis*ratio;
                String positionAngle = req.getParam("posang");
                return  "scisql_s2PtInEllipse(coord_ra,coord_decl," + ra + "," + dec + "," + semiMajorAxis + "," +
                        semiMinorAxis + "," + positionAngle + ")=1;";
            }
        }
        return null;

    }

    String buildSqlQueryString(TableServerRequest request) throws Exception {

        String tableName = request.getParam("table_name");
        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            //throw new RuntimeException(CatalogRequest.CATALOG + " parameter is required");
            catTable = DATABASE_NAME+"."+ tableName;
        }

        String sql = "SELECT "+request.getParam(CatalogRequest.SELECTED_COLUMNS) + " FROM " + catTable +
                " WHERE " + getSearchMethod( request);
        return sql;
    }

    private DataGroup  getDataFromURL(TableServerRequest request) throws Exception {

        String sql = "query=" + URLEncoder.encode(buildSqlQueryString(request),"UTF-8");

        try{
            long cTime = System.currentTimeMillis();
            _log.briefDebug("Executing SQL query: " + sql);
            String url = "http://"+HOST +":"+PORT+"/db/v0/tap/sync";

            File file = createFile(request, ".json");
            Map<String, String> requestHeader=new HashMap<>();
            requestHeader.put("Accept", "application/json");

             URLDownload.getDataToFileUsingPost(new URL(url),sql,null,  requestHeader, file, null);
             DataGroup dg =  getTableDataFromJson( request,file);
            _log.briefDebug("SHOW COLUMNS took " + (System.currentTimeMillis() - cTime) + "ms");

            return dg;

        }
        catch (Exception e) {
            _log.error(e);
            throw new DataAccessException("Query failed: " + e.getMessage());

        }

    }


    /**
     * This method convert the json data file to data group
     * @param jsonFile
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private DataGroup getTableDataFromJson(TableServerRequest request,  File jsonFile) throws IOException, ParseException {

        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(new FileReader(jsonFile));
        JSONArray data =  (JSONArray) ((JSONObject) ((JSONObject) obj.get("result")).get("table")).get("data");

        JSONArray columns = (JSONArray) ( (JSONObject) ( (JSONObject)( (JSONObject) obj.get("result")).get("table")).get("metadata")).get("elements");
        DataType[] dataType = getTypeDef(request, columns);

        DataGroup dg = new DataGroup("result", dataType  );

        for (int i=0; i<dataType.length; i++){
            dg.addAttribute(DataSetParser.makeAttribKey(DataSetParser.DESC_TAG, dataType[i].getKeyName()),
                    dataType[i].getShortDesc());
        }

        if (data.size()==0) return dg; //empty dataGroup
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
                   row.setDataElement(dataType[j],d );
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
    private  DataType[] getTypeDef(TableServerRequest request, JSONArray columns){


        TableServerRequest metaRequest = new TableServerRequest("LSSTMetaSearch");
        metaRequest.setParam("table_name",request.getParam("meta_table") );
        metaRequest.setPageSize(Integer.MAX_VALUE);
        //call LSSTMetaSearch processor to get the meta data as a DataGroup
        DataGroup metaData = getMeta(metaRequest);

        DataType[] dataTypes = new DataType[columns.size()];
        DataObject[] dataObjects=metaData.values().toArray(new DataObject[0]);
        for (int k=0; k<columns.size(); k++) {
            JSONObject col = (JSONObject) columns.get(k);
            for (int i = 0; i < dataObjects.length; i++) {
                 String keyName = ((String) dataObjects[i].getDataElement("Field")).trim();
                 if (keyName.equalsIgnoreCase( col.get("name").toString().trim() ) ){
                      boolean maybeNull = dataObjects[i].getDataElement("Null").toString().equalsIgnoreCase("yes") ? true : false;
                      dataTypes[k] = new DataType(keyName, keyName,
                            getDataClass((String) dataObjects[i].getDataElement("Type")),
                            DataType.Importance.HIGH,
                            (String) dataObjects[i].getDataElement("Unit"),
                            maybeNull
                    );
                    dataTypes[k].setShortDesc((String) dataObjects[i].getDataElement("Description"));
                    break;
                }
            }
        }
        return dataTypes;
    }

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
