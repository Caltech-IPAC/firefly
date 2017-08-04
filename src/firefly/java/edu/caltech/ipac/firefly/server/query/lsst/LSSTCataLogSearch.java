package edu.caltech.ipac.firefly.server.query.lsst;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
import java.util.List;
import java.util.Locale;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * Feb-9-2017
 * DM-9247
 *   Implement search processor(s) to support All Sky mode
 *
 *
 * Created by zhang on 10/10/16.
 * This is the Catalog search processor.  It searches based on search method for for catalog database and searches based on
 * search type in image database.
 * For catlog database, the search method.  It supports four search methods:
 *   1.  Cone
 *   2.  Box
 *   3.  Elliptical
 *   4.  Polygon
 *
 *
 *   For cone, box and polygon searches, all input have to be in degree unit
 *   For Elliptical, ra and dec are in degree and the semi axis are in arcsec.
 *
 *  For image data base, it supports:
 *     1. CENTER
 *     2. COVER
 *     3. ENCLOSED
 *
 */
@SearchProcessorImpl(id = "LSSTCataLogSearch")
public class LSSTCataLogSearch extends LSSTQuery {

    /**
     * This method will return the search method string based on the method.  If the method is not supported, the exception
     * will be thrown
     *
     * @param req table request
     * @return a string defining area constraint for the where clause of the query
     * @throws Exception
     */

    public static String getSearchMethodCatalog(TableServerRequest req)throws Exception { //, String raCol, String decCol) throws Exception {

        String method = req.getParam("SearchMethod");
        if (method.equalsIgnoreCase("allSky")){
            return "";
        }
        String[]  radec = req.getParam("UserTargetWorldPt")!=null? req.getParam("UserTargetWorldPt").split(";"):null;
        String ra =radec!=null? radec[0]:"";
        String dec = radec!=null?radec[1]:"";

        switch (method.toUpperCase()) {
            case "BOX":
                //The unit is degree for all the input
                String side = req.getParam(CatalogRequest.SIZE);
                WorldPt wpt = new WorldPt(Double.parseDouble(ra), Double.parseDouble(dec));
                //getCorners using arcsec in radius unit
                VisUtil.Corners corners  = VisUtil. getCorners(wpt, Double.parseDouble(side) /2.0*3600.0);

                String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
                String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());

                return "qserv_areaspec_box(" +  lowerRight + "," +upperLeft + ")";
                //return "scisql_s2PtInBox("+ radecCol + "," +  lowerRight + "," +upperLeft + ")=1";

            case "CONE":
                //The unit is degree for all the input
                String radius = req.getParam(CatalogRequest.RADIUS);

                return "qserv_areaspec_circle(" + ra + "," + dec + "," + radius + ")";
                //return "scisql_s2PtInCircle("+ radecCol + ","+ra +","+dec+","+radius +")=1";
           case "ELIPTICAL":
               //RA (degree), DEC (degree), positionAngle (degree), semi-majorAxis (arcsec), semi-minorAxis(arcsec),
               double semiMajorAxis = Double.parseDouble(req.getParam(CatalogRequest.RADIUS)) * 3600;
               double ratio = Double.parseDouble(req.getParam(CatalogRequest.RATIO));
               Double semiMinorAxis = semiMajorAxis*ratio;
               String positionAngle = req.getParam(CatalogRequest.PA);

               return "qserv_areaspec_ellipse(" + ra + "," + dec + "," + semiMajorAxis + "," +
                                               semiMinorAxis + "," + positionAngle + ")";

               // return  "scisql_s2PtInEllipse("+ radecCol + "," + ra + "," + dec + "," + semiMajorAxis + "," +
               //                               semiMinorAxis + "," + positionAngle + ")=1";

            case "POLYGON":
                //The unit is degree for all the input
                String radecList = req.getParam(CatalogRequest.POLYGON);
                String[] sArray = radecList.split(",");
                StringBuilder polygoneStr = new StringBuilder();

                polygoneStr.append("qserv_areaspec_poly(");

                //polygoneStr = "scisql_s2PtInCPoly("+ radecCol +",";

                for (int i=0; i<sArray.length; i++){
                    String[] radecPair = sArray[i].trim().split("\\s+");
                    if (radecPair.length!=2){
                        throw new Exception("wrong data entered");
                    }
                    if (i==sArray.length-1) {
                        polygoneStr.append(radecPair[0] + "," + radecPair[1] + ")");
                        //polygoneStr.append(radecPair[0] + "," + radecPair[1] + ")=1");
                    }
                    else {
                        polygoneStr.append(radecPair[0] + "," + radecPair[1] + ",");
                    }
                }
                return polygoneStr.toString();

            case "TABLE":
                //TODO what to do in multi-obj
                throw new EndUserException("Could not do Multi Object search, internal configuration wrong.",
                        "table should be a post search not a get");
            default:
               // should only be happened if a new method was added and not added here
                throw new EndUserException("The search method:"+method+ " is not supported", method);
        }

    }

    /**
     * image search (only for SDSS)
     * @param req request for image metadata search method
     * @return request based on search method
     * @throws Exception
     */
    protected String getSearchMethodImageMeta(TableServerRequest req)throws Exception {


        String searchType = req.getParam("intersect");
        if (searchType.equalsIgnoreCase("allSky")){
            return "";
        }
        String[]  radec = req.getParam("UserTargetWorldPt")!=null? req.getParam("UserTargetWorldPt").split(";"):null;
        String ra = radec!=null? radec[0]:"";
        String dec = radec!=null?radec[1]:"";

        VisUtil.Corners corners=null;
        if (!searchType.equalsIgnoreCase("center")) {
            WorldPt wpt = new WorldPt(Double.parseDouble(ra), Double.parseDouble(dec));
            //getCorners using arcsec in radius unit
            String side = req.getParam(CatalogRequest.SIZE);
            corners = VisUtil.getCorners(wpt, Double.parseDouble(side) / 2.0 * 3600.0);
        }

        String[] cornerRa = getCorners(req.getParam("database"), req.getParam("table_name"), "ra");
        String[] cornerDec = getCorners(req.getParam("database"), req.getParam("table_name"), "dec");
        String cornerStr = "";

        switch (searchType.toUpperCase()) {
            case "CENTER":
                cornerStr = getCornerStr(cornerRa, cornerDec);
                return "(scisql_s2PtInCPoly(" + ra + "," + dec + "," + cornerStr + ")=1)";

            case "COVERS":
                assert corners != null;
                String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
                String upperRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperRight().getLon(), corners.getUpperRight().getLat());
                String lowerLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerLeft().getLon(), corners.getLowerLeft().getLat());
                String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());

                cornerStr = getCornerStr(cornerRa, cornerDec);
                return "(scisql_s2PtInCPoly(" + lowerRight + ", " + cornerStr + ")=1) AND" +
                        "(scisql_s2PtInCPoly(" + lowerLeft + ", " + cornerStr + ")=1) AND" +
                        "(scisql_s2PtInCPoly(" + upperLeft + ", " + cornerStr + ")=1) AND" +
                        "(scisql_s2PtInCPoly(" + upperRight + ", " + cornerStr + ")=1) ";
            case "ENCLOSED":
                assert corners != null;
                double minRa = corners.getLowerRight().getLon();
                double minDec = corners.getLowerRight().getLat();
                double maxRa = corners.getUpperLeft().getLon();
                double maxDec = corners.getUpperLeft().getLat();

                return
                        "(scisql_s2PtInBox(" + getCornerWithLimit(cornerRa, cornerDec, 0, minRa, minDec, maxRa, maxDec) + ")=1) AND " +
                        "(scisql_s2PtInBox(" + getCornerWithLimit(cornerRa, cornerDec, 1, minRa, minDec, maxRa, maxDec) + ")=1) AND " +
                        "(scisql_s2PtInBox(" + getCornerWithLimit(cornerRa, cornerDec, 2, minRa, minDec, maxRa, maxDec) + ")=1) AND " +
                        "(scisql_s2PtInBox(" + getCornerWithLimit(cornerRa, cornerDec, 3, minRa, minDec, maxRa, maxDec) + ")=1)";

            default:
                // should only be happened if a new method was added and not added here
                throw new EndUserException("The search intersect: "+ searchType + "  is not supported", searchType);
        }

    }


    String getCornerStr(String[] cornerRa, String[] cornerDec) {
        String cornerStr = "";

        if ((cornerRa != null) && (cornerDec != null)) {
            for (int i = 0; i < cornerRa.length; i++) {
                cornerStr +=  cornerRa[i] + ", " + cornerDec[i];
                if (i < cornerRa.length -1) {
                    cornerStr += ", ";
                }
            }
        }
        return cornerStr;
    }

    String[] getCorners(String database, String catalog, String key) {
        Object corners = LSSTQuery.getDatasetInfo(database, catalog, new String[]{key});

        if (corners == null || !(corners instanceof JSONArray))
            return null;

        JSONArray jcorners = (JSONArray)corners;
        String[] strAry = new String[jcorners.size()];
        for (int i = 0; i < jcorners.size(); i++) {
            strAry[i] = jcorners.get(i).toString();
        }
        return strAry;
    }

    String getCornerWithLimit(String[] cornerRa, String[] cornerDec, int idx, double minRa, double minDec, double maxRa, double maxDec) {
        String cornerStr = "";

        if ((cornerRa != null) && (cornerDec != null) && idx < cornerRa.length) {
            return cornerRa[idx] + ", " + cornerDec[idx] + ", " + minRa + "," + minDec + "," + maxRa + "," + maxDec;
        }
        return cornerStr;
    }


    public static String getConstraints(TableServerRequest request) {
        String constraints = request.getParam(CatalogRequest.CONSTRAINTS);
        if (!StringUtils.isEmpty(constraints) && constraints.contains(CatalogRequest.CONSTRAINTS_SEPARATOR)) {
            constraints = constraints.replace(CatalogRequest.CONSTRAINTS_SEPARATOR, " and ");
        }
        return constraints;
    }


    String buildSqlQueryString(TableServerRequest request) throws Exception {

        String[] dbTable = LSSTQuery.getDBTableNameFromRequest(request);
        boolean isCatalogTable = LSSTQuery.isCatalogTable(dbTable[0], dbTable[1]);

        String columns = request.getParam(CatalogRequest.SELECTED_COLUMNS);
        if (columns==null){
            columns = "*";
        }

        //get all the constraints
        String constraints =  getConstraints(request);
        //get the search method

        String searchMethod = isCatalogTable ? getSearchMethodCatalog(request)
                                             : getSearchMethodImageMeta(request);

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

        String sql = "SELECT " + columns + " FROM " + String.join(".", dbTable);
        //add the guard to prevent from seaching the whole database when users do not enter a constrain
        if (whereStr.length()>0){
            return sql +  " WHERE " + whereStr + ";";
        }
        else {
            throw new EndUserException("Error: Search without constrains will cause database hanging!!!", sql);
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
        String catTable = request.getParam("table_name");
        String database = request.getParam("database");

        if (LSSTQuery.isCatalogTable(database, catTable)) {
            Object RA = LSSTQuery.getRA(database, catTable);
            Object DEC = LSSTQuery.getDEC(database, catTable);

            TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns((String) RA, (String) DEC, CoordinateSys.EQ_J2000);
            meta.setCenterCoordColumns(llc);
            meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "LSST");
            String col = LSSTQuery.getTableColumn(database, catTable, "objectColumn");
            if (col != null) {
                meta.setAttribute("objectIdColumn", col);
            }
            col =  LSSTQuery.getTableColumn(database, catTable, "filterColumn");

            if (col != null) {
                meta.setAttribute("filterIdColumn", col);
            }
        } else {
            String[] RAs = getCorners(database, catTable, "ra");
            String[] DECs = getCorners(database, catTable, "dec");
            TableMeta.LonLatColumns[] c = new TableMeta.LonLatColumns[4];

            for (int i = 0; i < 4; i++) {
                c[i] = new TableMeta.LonLatColumns(RAs[i], DECs[i],  CoordinateSys.EQ_J2000);
            }

            meta.setCorners(c[0], c[1], c[2], c[3]);
            // only set for image meta table
            meta.setAttribute(MetaConst.DATASET_CONVERTER,
                    (String)LSSTQuery.getDatasetInfo(database, catTable, new String[]{MetaConst.DATASET_CONVERTER}));
            Object schemaParams = LSSTQuery.getImageMetaSchema(database, catTable);
            if (schemaParams instanceof JSONObject) {
                for (Object key : ((JSONObject)schemaParams).keySet()) {
                    meta.setAttribute((String)key, (String)((JSONObject)schemaParams).get(key));
                }
            }
        }
        meta.setAttribute("database", database);
        meta.setAttribute("tableName", catTable);
        meta.setAttribute("mission",
                          (String)LSSTQuery.getDatasetInfo(database, catTable, new String[]{MetaConst.DATASET_CONVERTER}));
        meta.setAttribute("tableType", (String)LSSTQuery.getTableColumn(database, catTable, "tableType"));
        super.prepareTableMeta(meta, columns, request);
    }

}
