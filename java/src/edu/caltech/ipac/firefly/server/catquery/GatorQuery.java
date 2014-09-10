package edu.caltech.ipac.firefly.server.catquery;


import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import static edu.caltech.ipac.firefly.util.DataSetParser.DESC_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;
/**
 * User: roby
 * Date: Jul 29, 2010
 * Time: 2:24:20 PM
 */


/**
 * @author Trey Roby
 */
@SearchProcessorImpl(id = "GatorQuery", params =
        {@ParamDoc(name = CatalogRequest.SEARCH_METHOD, desc = "values: Cone, Eliptical, Box, Polygon, Table, AllSky"),
                @ParamDoc(name = CatalogRequest.RADIUS, desc = "float, the radius of the code search"),
                @ParamDoc(name = CatalogRequest.SIZE, desc = "float, the length of a side for a box search"),
                @ParamDoc(name = CatalogRequest.RAD_UNITS, desc = "the units for the radius or side, must be arcsec,arcmin,degree, default arcsec"),
                @ParamDoc(name = CatalogRequest.CATALOG, desc = "the catalog name to search"),
                @ParamDoc(name = CatalogRequest.RA_DEC_J2000, desc = "the ra and dec in j2000 separated by a space"),
                @ParamDoc(name = CatalogRequest.FILE_NAME, desc = "for upload, not yet used"),
                @ParamDoc(name = CatalogRequest.PA, desc = "pa for elliptical request"),
                @ParamDoc(name = CatalogRequest.RATIO, desc = "ratio for elliptical request"),
                @ParamDoc(name = CatalogRequest.POLYGON, desc = "a set of coordinate pairs (up to 15), eg. 20.5 21.5, 20.5 20.5, 21.5 20.5"),
                @ParamDoc(name = CatalogRequest.SELECTED_COLUMNS, desc = "a comma separated list of columns to return, empty gives the default list"),
                @ParamDoc(name = CatalogRequest.CONSTRAINTS, desc = "a where fragment of the column constrains"),
                @ParamDoc(name = CatalogRequest.DATABASE, desc = "the database to search against"),
                @ParamDoc(name = CatalogRequest.SERVER, desc = "i am not sure what this one means"),
                @ParamDoc(name = CatalogRequest.DD_FILE, desc = "the dd file to use"),
                @ParamDoc(name = CatalogRequest.DD_ONLIST, desc = "search catalog that is on list, optional: default true"),
                @ParamDoc(name = CatalogRequest.XPF_FILE, desc = "the xpf file to be used on the server when SearchMethod is Table, " +
                        "not used for other SearchMethods, optional when onlist is true, " +
                        "required when onlist is false.  example xpf file: /xpf/catupd.fp_psc.xpf"),
                @ParamDoc(name = CatalogRequest.GATOR_HOST, desc = "The hostname for the gator URL. optional: almost never used"),
                @ParamDoc(name = CatalogRequest.SERVICE_ROOT, desc = "the part of the URL string that specifies the service and first params. " +
                        "optional: almost never used"),
                @ParamDoc(name = CatalogRequest.USE, desc = "how this catalog will be used on the client: value: primary or overlay; default overlay.  " +
                        "if overlay then the meta value CatalogOverlayType is set otherwise it is not")
        })
public class GatorQuery extends BaseGator {

    private final static double ARCMIN_TO_DEG = .01666666667;
    private final static double ARCSEC_TO_DEG = .00027777778;
    private final static double DEG_TO_ARCMIN = 60.0;
    private final static double DEG_TO_ARCSEC = 3600.0;
    private final static double ARCMIN_TO_ARCSEC = 60.0;
    private final static double ARCSEC_TO_ARCMIN = .01666666667;

    protected static final String GP_SPATIAL_CONE = "&spatial=Cone";
    protected static final String GP_SPATIAL_BOX = "&spatial=Box";
    protected static final String GP_SPATIAL_POLY = "&spatial=Polygon";
    protected static final String GP_SPATIAL_NONE = "&spatial=NONE";
    protected static final String GP_SPATIAL = "spatial";
    protected static final String GP_DBMS20 = "&dbms=20";
    protected static final String UPLOAD = "Upload";

    public final static String URADIUS = "uradius";
    public final static String URAD_UNITS = "uradunits";

    private final static String DEFAULT_TNAME_OPTIONS[] = {
            "name",         // generic
            "pscname",      // IRAS
            "target",       //  our own table output
            "designation",  // 2MASS
            "starid"        // PCRS
    };
    private static final String DEF_QUERY_SERVICE = AppProperties.getProperty("irsa.gator.service.query",
            "/cgi-bin/Gator/nph-query");

    private final static String GP_OUTFMT = "outfmt";
    private final static String FMT_TYPE = "1";
    private final static String START_PARAM_OUTFMT_1 = GP_OUTFMT + "=" + FMT_TYPE;
    private static final String RA = "ra";
    private static final String DEC = "dec";

    protected String getDefService() {
        return DEF_QUERY_SERVICE;
    }

    protected boolean isPost(CatalogRequest req) {
        return req.getMethod() == CatalogRequest.Method.TABLE;
    }

    protected String getParams(CatalogRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);

        sb.append("?");
        sb.append(START_PARAM_OUTFMT_1);
        requiredParam(sb, CatalogRequest.CATALOG, req.getQueryCatName());
        switch (req.getMethod()) {
            case ALL_SKY:
                sb.append(GP_SPATIAL_NONE);
                break;
            case BOX:
                sb.append(makeObjStr(req));
                sb.append(GP_SPATIAL_BOX);
                double side = convert(req.getRadUnits(), CatalogRequest.RadUnits.ARCSEC, req.getSide());
                requiredParam(sb, CatalogRequest.SIZE, side);
                break;
            case CONE:
                sb.append(makeObjStr(req));
                sb.append(GP_SPATIAL_CONE);
                // use arcseconds (int) - Gator stores max limits in arcseconds
                double radius =  convert(req.getRadUnits(), CatalogRequest.RadUnits.ARCSEC, req.getRadius());
                requiredParam(sb, CatalogRequest.RADIUS, radius);
                //requiredParam(sb, CatalogRequest.RAD_UNITS, req.getRadUnits().getDesc());
                //requiredParam(sb, CatalogRequest.RADIUS, req.getRadius());
                break;
            case ELIPTICAL:
                sb.append(makeObjStr(req));
                sb.append(GP_SPATIAL_CONE);
                requiredParam(sb, CatalogRequest.RAD_UNITS, req.getRadUnits().getDesc());
                requiredParam(sb, CatalogRequest.RADIUS, req.getRadius());
                requiredParam(sb, CatalogRequest.PA, req.getPA());
                requiredParam(sb, CatalogRequest.RATIO, req.getRatio());
                break;
            case POLYGON:
                sb.append(GP_SPATIAL_POLY);
                sb.append(param(CatalogRequest.POLYGON, urlEncode(req.getPolygon())));
                break;
            case TABLE:
                throw new EndUserException("Could not do Multi Object search, internal configuration wrong.",
                        "table should be a post search not a get");
            default:
                assert false; // should only happend if a new method was added and not added here
                break;
        }


        optionalParam(sb, CatalogRequest.SELECTED_COLUMNS, req.getSelectedColumns());
        optionalParam(sb, CatalogRequest.CONSTRAINTS, encodeParams(req.getConstraints()));
        optionalParam(sb, CatalogRequest.DD_ONLIST, req.getDDOnList());
        optionalParam(sb, CatalogRequest.GATOR_MISSION, req.getGatorMission());

        boolean ddOnList = req.getDDOnList();

        if (!ddOnList) {
            requiredParam(sb, CatalogRequest.SERVER, urlEncode(req.getServer()));
            requiredParam(sb, CatalogRequest.DATABASE, urlEncode(req.getDatabase()));
            optionalParam(sb, CatalogRequest.DD_FILE, urlEncode(req.getDDFile()));
            sb.append(GP_DBMS20);
        }

        return sb.toString();
    }

    @Override
    protected void insertPostParams(CatalogRequest req) throws EndUserException, IOException {

        if (req.getMethod() == CatalogRequest.Method.TABLE) {
            requiredPostParam(GP_OUTFMT, FMT_TYPE);
            requiredPostParam(GP_SPATIAL, UPLOAD);
            // use arcseconds (int) - Gator stores max limits in arcseconds
            double radius =  convert(req.getRadUnits(), CatalogRequest.RadUnits.ARCSEC, req.getRadius());
            requiredPostParam(URADIUS, radius);
            //requiredPostParam(URAD_UNITS, req.getRadUnits().getGatorUploadParam());
            //requiredPostParam(URADIUS, req.getRadius());
            requiredPostParam(CatalogRequest.CATALOG, req.getQueryCatName());
            boolean ddOnList = req.getDDOnList();
            if (!ddOnList) {
                requiredPostParam(CatalogRequest.XPF_FILE, req.getXPFFile());
                requiredPostParam(CatalogRequest.SERVER, req.getServer());
                requiredPostParam(CatalogRequest.DATABASE, req.getDatabase());
                optionalPostParam(CatalogRequest.DD_FILE, req.getDDFile());
                requiredPostParam("dbms", 20);

            }
            optionalPostParam(CatalogRequest.SELECTED_COLUMNS, req.getSelectedColumns());
            optionalPostParam(CatalogRequest.CONSTRAINTS, encodeParams(req.getConstraints()));
            optionalPostParam(CatalogRequest.DD_ONLIST, req.getDDOnList());
            optionalPostParam(CatalogRequest.GATOR_MISSION, req.getGatorMission());

            requiredPostFileCacheParam(CatalogRequest.FILE_NAME, req.getFileName());

        } else {
            throw new EndUserException("Could not do search, internal configuration wrong.",
                    "search should be a get search not a post");
        }
        switch (req.getMethod()) {
            case TABLE:
                break;
            default:
        }
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        CatalogRequest req = QueryUtil.assureType(CatalogRequest.class, request);
        setXmlParams(req);
        TableMeta.LonLatColumns llc = null;

        if (verifyCoordsInTable(columns, RA, DEC)) {
            llc = new TableMeta.LonLatColumns(RA, DEC, CoordinateSys.EQ_J2000);
            meta.setCenterCoordColumns(llc);
        }
        String name = findTargetName(columns);
        if (name != null) meta.setAttribute(MetaConst.CATALOG_TARGET_COL_NAME, name);


        if (req.getUse() == CatalogRequest.Use.CATALOG_OVERLAY ||
            req.getUse() == CatalogRequest.Use.CATALOG_PRIMARY) {
            meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "IRSA");

            if (llc != null) {
                meta.setLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS, llc);
            }

        }

        if (req.getUse() == CatalogRequest.Use.DATA_PRIMARY ||
            req.getUse() == CatalogRequest.Use.CATALOG_PRIMARY) {
            meta.setAttribute(MetaConst.DATA_PRIMARY, "True");
        }

        setColumnTips(meta, req);

        meta.setAttribute(MetaConst.DATASET_CONVERTER, "FINDER_CHART"); //TODO: TEST ONLY, remove or comment out this line
        super.prepareTableMeta(meta, columns, request);
    }

    protected void setColumnTips(TableMeta meta, ServerRequest request) {

        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_DD);
        req.setPageSize(1000);
        for (Param param : request.getParams()) {
            if (param.getName().equals(CatalogRequest.CATALOG)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.DATABASE)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.SERVER)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.DD_FILE)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.DD_SHORT)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.DD_ONLIST)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.GATOR_HOST)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.SERVICE_ROOT)) req.setParam(param);
            if (param.getName().equals(CatalogRequest.CATALOG_PROJECT)) req.setParam(param);
        }

        SearchManager sm = new SearchManager();
        DataGroupPart dgp = new DataGroupPart();

        try {
            dgp = sm.getDataGroup(req);
        } catch (Exception e) {
        }

        DataGroup dg = dgp.getData();
        if (dg != null) {
            for (int i = 0; i < dg.size(); i++) {
                DataObject dObj = dg.get(i);
                String tipStr = "";

                String descStr = (String) dObj.getDataElement("description");
                if (!StringUtils.isEmpty(descStr) && !descStr.equalsIgnoreCase("null")) {
                    tipStr += descStr;
                }

                String unitStr = (String) dObj.getDataElement("units");
                if (!StringUtils.isEmpty(unitStr) && !unitStr.equalsIgnoreCase("null")) {
                    if (tipStr.length() > 0) {
                        tipStr += " ";
                    }
                    tipStr += "(" + unitStr + ")";
                }

                String nameStr = (String) dObj.getDataElement("name");
                meta.setAttribute(makeAttribKey(DESC_TAG, nameStr.toLowerCase()), tipStr);
            }
        }
    }

    private String encodeParams(String params) {
        if (!StringUtils.isEmpty(params)) {
            if (params.contains(",")) {
                params = params.replace(",", " and ");
            }
            params = urlEncode(params);
        }
        return params;
    }

    private static String findTargetName(List<DataType> columns) {
        String cname;
        String finalName = null;
        for (DataType col : columns) {
            cname = col.getKeyName().toLowerCase();
            for (String testName : DEFAULT_TNAME_OPTIONS) {
                if (cname.contains(testName)) {
                    finalName = col.getKeyName();
                    break;
                }

            }
            if (finalName != null) break;
        }
        return finalName;
    }

    private static boolean verifyCoordsInTable(List<DataType> columns, String lonStr, String latStr) {

        boolean lonFound = false;
        boolean latFound = false;

        for (DataType col : columns) {

            if (col.getKeyName().equalsIgnoreCase(lonStr)) lonFound = true;
            if (col.getKeyName().equalsIgnoreCase(latStr)) latFound = true;

            if (lonFound && latFound) break;
        }

        return lonFound && latFound;
    }


    private static String makeObjStr(CatalogRequest req) throws UnsupportedEncodingException, EndUserException {
        WorldPt pt = req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        if (pt == null) pt = req.getWorldPtJ2000();
        pt = VisUtil.convertToJ2000(pt);

        if (pt == null) {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " +
                            CatalogRequest.RA_DEC_J2000);
        }
        String fstr = String.format(Locale.US, "%8.6fd %8.6fd eq j2000", pt.getLon(), pt.getLat());
        return param("objstr", urlEncode(fstr));
    }

    protected String getFileBaseName(CatalogRequest req) throws EndUserException {
        CatalogRequest.Method method = req.getMethod();
        if (method != null) {
            return "gator-catalog-" + req.getMethod().getDesc();
        } else {
            throw new EndUserException("Could not complete IRSA search",
                    "The search method was not specified in the query");
        }
    }


    public static double convert(CatalogRequest.RadUnits fromUnits, CatalogRequest.RadUnits toUnits, double v) {
        double retval;
        if (fromUnits == toUnits) {
            retval = v;
        } else if (Double.isNaN(v)) {
            retval = Double.NaN;
        } else if (fromUnits == CatalogRequest.RadUnits.DEGREE && toUnits == CatalogRequest.RadUnits.ARCMIN) {
            retval = v * DEG_TO_ARCMIN;
        } else if (fromUnits == CatalogRequest.RadUnits.DEGREE && toUnits == CatalogRequest.RadUnits.ARCSEC) {
            retval = Math.round( v * DEG_TO_ARCSEC );
        } else if (fromUnits == CatalogRequest.RadUnits.ARCSEC && toUnits == CatalogRequest.RadUnits.DEGREE) {
            retval = v * ARCSEC_TO_DEG;
        } else if (fromUnits == CatalogRequest.RadUnits.ARCSEC && toUnits == CatalogRequest.RadUnits.ARCMIN) {
            retval = v * ARCSEC_TO_ARCMIN;
        } else if (fromUnits == CatalogRequest.RadUnits.ARCMIN && toUnits == CatalogRequest.RadUnits.DEGREE) {
            retval = v * ARCMIN_TO_DEG;
        } else if (fromUnits == CatalogRequest.RadUnits.ARCMIN && toUnits == CatalogRequest.RadUnits.ARCSEC) {
            retval = Math.round( v * ARCMIN_TO_ARCSEC );
        } else { // this should never happpen
            retval = v;
            WebAssert.tst(false);
        }

        return retval;
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
