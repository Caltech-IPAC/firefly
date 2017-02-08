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


/**
 * Created by zhang on 10/10/16.
 * This is the Catalog search processor.  For any given target (ra and dec, except in polygon), it searches based on
 * the search method.  It supports four search methods:
 *   1.  Cone
 *   2.  Box
 *   3.  Elliptical
 *   4.  Polygon
 *
 *   For cone, box and polygon searches, all input have to be in degree unit
 *   For Elliptical, ra and dec are in degree and the semi axis are in arcsec.
 *
 */
@SearchProcessorImpl(id = "LSSTCataLogSearch")
public class LSSTCataLogSearch extends LSSTQuery {

    /**
     * This method will return the search method string based on the method.  If the method is not supported, the exception
     * will be thrown
     *
     * @param req table request
     * @param catalog catalog
     * @return a string defining area constraint for the where clause of the query
     * @throws Exception
     */

    protected String getSearchMethodCatalog(TableServerRequest req, String catalog)throws Exception { //, String raCol, String decCol) throws Exception {

        String method = req.getParam("SearchMethod");
        String[]  radec = req.getParam("UserTargetWorldPt")!=null? req.getParam("UserTargetWorldPt").split(";"):null;
        String ra =radec!=null? radec[0]:"";
        String dec = radec!=null?radec[1]:"";

        boolean isRunDeep = (catalog != null && catalog.contains("RunDeep"));
        switch (method.toUpperCase()) {
            case "ALLSKY":
                return "";
            case "BOX":
                //The unit is degree for all the input
                String side = req.getParam(CatalogRequest.SIZE);
                WorldPt wpt = new WorldPt(Double.parseDouble(ra), Double.parseDouble(dec));
                //getCorners using arcsec in radius unit
                VisUtil.Corners corners  = VisUtil. getCorners(wpt, Double.parseDouble(side) /2.0*3600.0);

                String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
                String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());

                if (isRunDeep) {
                    return "qserv_areaspec_box(" +  lowerRight + "," +upperLeft + ")";
                }
                else {
                    return "scisql_s2PtInBox("+ getRA(catalog)+"," + getDEC(catalog)+"," +  lowerRight + "," +upperLeft + ")=1";
                }

            case "CONE":
                //The unit is degree for all the input
                String radius = req.getParam(CatalogRequest.RADIUS);
                if (isRunDeep) {
                    return "qserv_areaspec_circle(" + ra + "," + dec + "," + radius + ")";
                }
                else {
                    return "scisql_s2PtInCircle("+ getRA(catalog)+"," + getDEC(catalog)+","+ra +","+dec+","+radius +")=1";
                }
           case "ELIPTICAL":
               //RA (degree), DEC (degree), positionAngle (degree), semi-majorAxis (arcsec), semi-minorAxis(arcsec),
               double semiMajorAxis = Double.parseDouble(req.getParam("radius")) * 3600;
               double ratio = Double.parseDouble(req.getParam(CatalogRequest.RATIO));
               Double semiMinorAxis = semiMajorAxis*ratio;
               String positionAngle = req.getParam("posang");
               if (isRunDeep) {
                   return "qserv_areaspec_ellipse(" + ra + "," + dec + "," + semiMajorAxis + "," +
                           semiMinorAxis + "," + positionAngle + ")";
               }
               else {
                   return  "scisql_s2PtInEllipse("+ getRA(catalog)+"," + getDEC(catalog)+"," + ra + "," + dec + "," + semiMajorAxis + "," +
                           semiMinorAxis + "," + positionAngle + ")=1";
               }
            case "POLYGON":
                //The unit is degree for all the input
                String radecList = req.getParam(CatalogRequest.POLYGON);
                String[] sArray = radecList.split(",");
                String polygoneStr;
                if (isRunDeep) {
                     polygoneStr = "qserv_areaspec_poly(";
                }
                else {
                    polygoneStr = "scisql_s2PtInCPoly("+ getRA(catalog)+"," + getDEC(catalog)+",";
                }
                for (int i=0; i<sArray.length; i++){
                    String[] radecPair = sArray[i].trim().split("\\s+");
                    if (radecPair.length!=2){
                        throw new Exception("wrong data entered");
                    }
                    if (i==sArray.length-1) {
                        if (isRunDeep) {
                            polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ")";
                        }
                        else {
                            polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ")=1";
                        }
                    }
                    else {
                        polygoneStr = polygoneStr + radecPair[0] + "," + radecPair[1] + ",";
                    }
                }
                return polygoneStr;

            case "TABLE":
                //TODO what to do in multi-obj
                throw new EndUserException("Could not do Multi Object search, internal configuration wrong.",
                        "table should be a post search not a get");
            default:
               // should only be happened if a new method was added and not added here
                throw new EndUserException("The search method:"+method+ " is not supported", method);
        }

    }

    protected String getMethodOnSearchType(TableServerRequest req)throws Exception { //, String raCol, String decCol) throws Exception {

       /*
        String method = req.getParam("SearchMethod");
        if (!method.equalsIgnoreCase("box")) {
                throw new DataAccessException("Inout Error:" + method  + " is not supported for now");
        }*/
        String searchType = req.getParam("intersect");
        String[]  radec = req.getParam("UserTargetWorldPt")!=null? req.getParam("UserTargetWorldPt").split(";"):null;
        String ra =radec!=null? radec[0]:"";
        String dec = radec!=null?radec[1]:"";

        VisUtil.Corners corners=null;
        if (!searchType.equalsIgnoreCase("center")) {
            WorldPt wpt = new WorldPt(Double.parseDouble(ra), Double.parseDouble(dec));
            //getCorners using arcsec in radius unit
            String side = req.getParam(CatalogRequest.SIZE);
            corners = VisUtil.getCorners(wpt, Double.parseDouble(side) / 2.0 * 3600.0);
        }

        switch (searchType.toUpperCase()) {
            case "CENTER":
                return "(scisql_s2PtInCPoly(" + ra + "," + dec + ",corner1Ra, corner1Decl, corner2Ra, corner2Decl, " +
                        "corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1)";

            case "COVERS":
                assert corners != null;
                String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
                String upperRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperRight().getLon(), corners.getUpperRight().getLat());
                String lowerLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerLeft().getLon(), corners.getLowerLeft().getLat());
                String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());

                return "(scisql_s2PtInCPoly(" + lowerRight + ", corner1Ra, corner1Decl, corner2Ra, corner2Decl, " +
                        "corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) AND" +
                        "(scisql_s2PtInCPoly(" + lowerLeft + ", corner1Ra, corner1Decl, corner2Ra, corner2Decl, " +
                        "corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) AND" +
                        "(scisql_s2PtInCPoly(" + upperLeft + ", corner1Ra, corner1Decl, corner2Ra, corner2Decl, " +
                        "corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) AND" +
                        "(scisql_s2PtInCPoly(" + upperRight + ", corner1Ra, corner1Decl, corner2Ra, corner2Decl, " +
                        "corner3Ra, corner3Decl, corner4Ra, corner4Decl)=1) ";

            case "ENCLOSED":

                assert corners != null;
                double minRa = corners.getLowerRight().getLon();
                double minDec = corners.getLowerRight().getLat();
                double maxRa = corners.getUpperLeft().getLon();
                double maxDec = corners.getUpperLeft().getLat();

                return
                        "(scisql_s2PtInBox(corner1Ra, corner1Decl," + minRa + "," + minDec + "," + maxRa + "," + maxDec + ")=1) AND " +
                                "(scisql_s2PtInBox(corner2Ra, corner2Decl," + minRa + "," + minDec + "," + maxRa + "," + maxDec + ")=1) AND " +
                                "(scisql_s2PtInBox(corner3Ra, corner3Decl," + minRa + "," + minDec + "," + maxRa + "," + maxDec + ")=1) AND " +
                                "(scisql_s2PtInBox(corner4Ra, corner4Decl, " + minRa + "," + minDec + "," + maxRa + "," + maxDec + ")=1)";


            default:
                // should only be happened if a new method was added and not added here
                throw new EndUserException("The search intersect: "+ searchType + "  is not supported", searchType);
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
        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            //throw new RuntimeException(CatalogRequest.CATALOG + " parameter is required");
            catTable = DATABASE_NAME.length()==0?tableName: DATABASE_NAME+"."+ tableName;
        }


        boolean isCatalogTable = (tableName != null && tableName.contains("RunDeep"));

        String columns = request.getParam(CatalogRequest.SELECTED_COLUMNS);
        if (columns==null){
            columns = "*";
        }

        //get all the constraints
        String constraints =  getConstraints(request);
        //get the search method

        String searchMethod = isCatalogTable ? getSearchMethodCatalog( request, tableName):getMethodOnSearchType(request);

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


    @Override
    protected String getFilePrefix(TableServerRequest request) {
        String catTable = request.getParam(CatalogRequest.CATALOG);
        if (catTable == null) {
            return request.getRequestId();
        } else {
            return catTable+"-dd-";
        }

    }
    private String getRA(String catalog) {
        if (catalog != null && catalog.contains("RunDeep")) {
            return "coord_ra";
        } else {
            return "ra";
        }
    }

    private String getDEC(String catalog) {
        if (catalog != null && catalog.contains("RunDeep")) {
            return "coord_decl";
        } else {
            return "decl";
        }
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        super.prepareTableMeta(meta, columns, request);
        String catTable = request.getParam("table_name");
        String tUp=catTable.toUpperCase();

        String RA = getRA(catTable);
        String DEC = getDEC(catTable);
        TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns(RA, DEC, CoordinateSys.EQ_J2000);
        meta.setCenterCoordColumns(llc);

        if (tUp.equals("SCIENCE_CCD_EXPOSURE") || tUp.equals("DEEPCOADD")) {
            TableMeta.LonLatColumns c1= new TableMeta.LonLatColumns("corner1Ra", "corner1Decl", CoordinateSys.EQ_J2000);
            TableMeta.LonLatColumns c2= new TableMeta.LonLatColumns("corner2Ra", "corner2Decl", CoordinateSys.EQ_J2000);
            TableMeta.LonLatColumns c3= new TableMeta.LonLatColumns("corner3Ra", "corner3Decl", CoordinateSys.EQ_J2000);
            TableMeta.LonLatColumns c4= new TableMeta.LonLatColumns("corner4Ra", "corner4Decl", CoordinateSys.EQ_J2000);
            meta.setCorners(c1, c2, c3, c4);
            meta.setAttribute(MetaConst.DATASET_CONVERTER, "lsst_sdss");
        }
        else {
            meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "LSST");
        }
        super.prepareTableMeta(meta, columns, request);
    }

}
