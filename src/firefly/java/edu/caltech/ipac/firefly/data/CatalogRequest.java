/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
/**
 * User: roby
 * Date: Oct 30, 2009
 * Time: 12:29:21 PM
 */


/**
 * @author Trey Roby
 */
public class CatalogRequest extends TableServerRequest {

    public final static String SEARCH_METHOD = "SearchMethod";
    public final static String RADIUS = "radius";
    public final static String ONE_TO_ONE = "one_to_one";
    public final static String SIZE = "size";
    public final static String RAD_UNITS = "radunits";
    public final static String DISPLAY_UNITS = "displayUnits";
    public final static String CATALOG = "catalog";
    public final static String RA_DEC_J2000 = "RaDecJ2000";
    public final static String FILE_NAME = "filename";
    public final static String PA = "posang";
    public final static String RATIO = "ratio";
    public final static String POLYGON = "polygon";
    public final static String SELECTED_COLUMNS = "selcols";
    public final static String REQUIRED_COLUMNS = "reqcols";
    public final static String CONSTRAINTS = "constraints";
    public final static String SERVER = "server";
    public final static String DATABASE = "database";
    public final static String DBMS = "dbms";
    public final static String DD_FILE = "ddfile";
    public final static String DD_SHORT = "short";
    public final static String DD_ONLIST = "onlist";
    public final static String GATOR_HOST = "GatorHost";
    public final static String SERVICE_ROOT = "Service";
    public final static String XPF_FILE = "xpffile";
    public final static String PROJ_INDEX = "projIndex";
    public final static String CAT_INDEX = "catIndex";
    public final static String CAT_ROW = "catRow";
    public final static String USE = "use";
    public final static String GATOR_MISSION = "mission";
    public final static String CATALOG_PROJECT = "catalogProject";
    public static final String UPDLOAD_ROW_ID = "in_row_id";

    // how do we separate multiple constraints -
    // used in catalog dd panels, where each field can be constrained separately
    // if we use comma, where clause can not contain comma:
    //    "col in (1,2,3)" constraint is not possible
    // " and " is not perfect either, we might think of a better separator later
    public final static String CONSTRAINTS_SEPARATOR = ";";

    public enum RequestType {
        GATOR_QUERY("GatorQuery"),
        GATOR_DD("GatorDD");

        private final String _searchProcessor;

        RequestType(String searchProcessor) {
            _searchProcessor = searchProcessor;
        }

        public String getSearchProcessor() {
            return _searchProcessor;
        }
    }

    public enum Method {
        CONE("Cone", 0),
        ELIPTICAL("Eliptical", 1),
        BOX("Box", 2),
        POLYGON("Polygon", 3),
        TABLE("Table", 4),
        ALL_SKY("AllSky", 5);

        private final int _idx;
        private final String _desc;

        Method(String desc, int idx) {
            _desc = desc;
            _idx = idx;
        }

        public int getIdx() {
            return _idx;
        }

        public String getDesc() {
            return _desc;
        }
    }


    public enum RadUnits {
        ARCSEC("arcsec", 0),
        ARCMIN("arcmin", 1),
        DEGREE("degree", 2);

        private final int _idx;
        private final String _desc;

        RadUnits(String desc, int idx) {
            _desc = desc;
            _idx = idx;
        }

        public int getIdx() {
            return _idx;
        }

        public String getDesc() {
            return _desc;
        }

        public String getGatorUploadParam() {
            String retval;
            switch (this) {
                case ARCSEC:
                    retval = getDesc();
                    break;
                case ARCMIN:
                    retval = getDesc();
                    break;
                case DEGREE:
                    retval = "deg";
                    break;
                default:
                    retval = null;
                    break;
            }
            return retval;
        }
    }


    public enum Use {
        CATALOG_OVERLAY("catalog_overlay", 0),
        CATALOG_PRIMARY("catalog_primary", 1),
        DATA_PRIMARY("data_primary", 2);

        private final int _idx;
        private final String _desc;

        Use(String desc, int idx) {
            _desc = desc;
            _idx = idx;
        }

        public String getDesc() {
            return _desc;
        }
    }


    private static final Method _allMethods[] = {Method.CONE,
                                                 Method.ELIPTICAL,
                                                 Method.BOX,
                                                 Method.POLYGON,
                                                 Method.TABLE,
                                                 Method.ALL_SKY};

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public CatalogRequest() {
    }

    public CatalogRequest(RequestType rtype) {
        super(rtype.getSearchProcessor());
        this.setPageSize(0);
        if (rtype == RequestType.GATOR_QUERY) setUse(Use.CATALOG_OVERLAY);
    }


    @Override
    public ServerRequest newInstance() {
        return new CatalogRequest();
    }

    public void setMethod(Method method) {
        setParam(SEARCH_METHOD, method.getDesc());
    }


    public Method getMethod() {
        String mStr = getParam(SEARCH_METHOD);
        Method retval = null;
        for (Method m : _allMethods) {
            if (m.getDesc().equals(mStr)) {
                retval = m;
                break;
            }
        }
        return retval;
    }

    public void setRadUnits(RadUnits radUnits) {
        setParam(RAD_UNITS, radUnits.getDesc());
    }


    public RadUnits getRadUnits() {
        RadUnits retval = RadUnits.ARCSEC;
        String s = getParam(RAD_UNITS);
        if (s != null) {
            try {
                retval = Enum.valueOf(RadUnits.class, s.toUpperCase());
            } catch (IllegalArgumentException e) {
                retval = RadUnits.ARCSEC;
            }
        }
        return retval;
    }


    public void setUse(Use use) {
        setParam(USE, use.getDesc());
    }

    public Use getUse() {
        Use retval = Use.DATA_PRIMARY;
        String s = getParam(USE);
        if (s != null) {
            try {
                retval = Enum.valueOf(Use.class, s.toUpperCase());
            } catch (IllegalArgumentException e) {
                retval = Use.DATA_PRIMARY;
            }
        }
        return retval;
    }


    public void setQueryCatName(String catName) {
        setParam(CATALOG, catName);
    }

    public String getQueryCatName() {
        return getParam(CATALOG);
    }


    public void setWorldPtJ2000(WorldPt pt) {
        assert CoordinateSys.EQ_J2000.equals(pt.getCoordSys());
        setParam(RA_DEC_J2000, pt.getLon() + " " + pt.getLat());
    }


    public WorldPt getWorldPtJ2000() {
        String ptStr = getParam(RA_DEC_J2000);
        String s[] = ptStr.split(" ");
        return new WorldPt(asDouble(s[0]), asDouble(s[1]));
    }


    public void setRadius(double radiusArcSec) {
        setParam(RADIUS, radiusArcSec + "");
    }

    public double getRadius() {
        return getDoubleParam(RADIUS);
    }

    public void setSide(double sideArcSec) {
        setParam(SIZE, sideArcSec + "");
    }

    public double getSide() {
        return getDoubleParam(SIZE);
    }


    public void setPA(double pa) {
        setParam(PA, pa + "");
    }

    public double getPA() {
        return getDoubleParam(PA);
    }

    public void setRatio(double pa) {
        setParam(RATIO, pa + "");
    }

    public double getRatio() {
        return getDoubleParam(RATIO);
    }

    public void setSelectedColumns(String selectedColumns) {
        setParam(SELECTED_COLUMNS, selectedColumns);
    }

    public String getSelectedColumns() {
        return getParam(SELECTED_COLUMNS);
    }

    public void setRequiredColumns(String requiredColumns) {
        setParam(REQUIRED_COLUMNS, requiredColumns);
    }

    public String getRequiredColumns() {
        return getParam(REQUIRED_COLUMNS);
    }

    public String getConstraints() {
        return getParam(CONSTRAINTS);
    }

    public void setConstraints(String c) {
        setParam(CONSTRAINTS, c);
    }

    public String getServer() {
        return getParam(SERVER);
    }

    public void setServer(String s) {
        setParam(SERVER, s);
    }


    public String getDatabase() {
        return getParam(DATABASE);
    }

    public void setDatabase(String d) {
        setParam(DATABASE, d);
    }


    public String getDDFile() {
        return getParam(DD_FILE);
    }

    public void setDDFile(String f) {
        setParam(DD_FILE, f);
    }

    public boolean getDDShort() {
        return getBooleanParam(DD_SHORT, true);
    }

    public void setDDShort(boolean v) {
        setParam(DD_SHORT, v + "");
    }


    public boolean getDDOnList() {
        return getBooleanParam(DD_ONLIST, true);
    }

    public void setDDOnList(boolean onList) {
        setParam(DD_ONLIST, onList + "");
    }

    public String getGatorHost() {
        return getParam(GATOR_HOST);
    }

    public void setGatorHost(String h) {
        setParam(GATOR_HOST, h);
    }

    public String getProjIndex() {
        return getParam(PROJ_INDEX);
    }

    public void setProjIndex(String h) {
        setParam(PROJ_INDEX, h);
    }

    public String getCatagoryIndex() {
        return getParam(CAT_INDEX);
    }

    public void setCatagoryIndex(String h) {
        setParam(CAT_INDEX, h);
    }

    public String getCatTableRow() {
        return getParam(CAT_ROW);
    }

    public void setCatTableRow(String h) {
        setParam(CAT_ROW, h);
    }

    public String getServiceRoot() {
        return getParam(SERVICE_ROOT);
    }

    public void setServiceRoot(String r) {
        setParam(SERVICE_ROOT, r);
    }

    public String getXPFFile() {
        return getParam(XPF_FILE);
    }

    public void setXPFFile(String f) {
        setParam(XPF_FILE, f);
    }

    public void setFileName(String file) {
        setParam(FILE_NAME, file);
    }

    public void setGatorMission(String gatorMission) {
        setParam(GATOR_MISSION, gatorMission);
    }

    public String getGatorMission() {
        return getParam(GATOR_MISSION);
    }

    public String getFileName() {
        return getParam(FILE_NAME);
    }

    public void setPolygon(String coords) {
        setParam(POLYGON, coords);
    }

    public String getPolygon() {
        return getParam(POLYGON);
    }

    private static double asDouble(String dStr) {
        double retval;
        try {
            retval = Double.parseDouble(dStr);
        } catch (NumberFormatException e) {
            retval = 0.0;
        }
        return retval;

    }
}

