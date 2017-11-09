/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe.datasource;

import edu.caltech.ipac.astro.ibe.BaseIbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

/**
 * Generic Atlas (IBE2) source definition, api has the URL form of (see IRSA-750):
 * https://irsadev.ipac.caltech.edu/IBE?table=@schema@.@table@&POS=@ra@,@dec@
 * <p>
 * See schemas(mission) / tables value available: https://irsadev.ipac.caltech.edu/ibe
 * <br>and<p>
 * https://irsadev.ipac.caltech.edu/TAP/sync?query=select+c.schema_name,+c.table_name+from+TAP_SCHEMA.columns+c+JOIN+TAP_SCHEMA.tables+t+ON+c.table_name=t.table_name+where+c.column_name='ra4'+AND+t.irsa_dbms='POSTGRES'
 * </p>
 * <br>
 * Plus, the <i>where</i> clause still apply, i.e. SEIP:
 * https://irsadev.ipac.caltech.edu/IBE?table=spitzer.seip_science&responseformat=votable&POS=280,-8&where=instrument_name%3d%27IRAC%27+and+band_name%3d%27IRAC4%27
 * <br><p>
 * Using cutout:
 * http://irsadev.ipac.caltech.edu/ibe/data/@chema@/@table@/@fname?center=@ra@,@dec@&size=@n@px
 * <p>
 * Note for Spitzer:
 * there are two types of image that are considered "science": *mosaic.fits and median_mosaic.fits.
 * Filtering by file-type: science will remove the unc and cov files, but not the median_mosaic.fits files.
 * need specific filter: <em>fname like '%.mosaic.fits'</em>
 *
 * @author ejoliet
 */
public class AtlasIbeDataSource extends BaseIbeDataSource {
    public static final String ATLAS = "atlas";

    //Keeping the logic
    public static final String DS_KEY = "ds"; // example: seip
    public static final String BAND_KEY = "band"; // example: IRAC1
    public static final String INSTRUMENT_KEY = "instrument"; //example: IRAC or MIPS
    public static final String FILE_TYPE_KEY = "file_type";// example: 'science' for seip (see princiapl column for prefered image to display = '1'
    public static final String TABLE_KEY = "table";// example: seip_science
    public static final String DATASET_KEY = "dataset"; //Example:spitzer
    public static final String XTRA_KEY = "filter"; //Example:fname like '%.mosaic.fits'
    private static final String PRINCIPAL_KEY = "principal";

    private DS ds;

    /**
     * ATLAS has hundreds of dataproducts: herschel, spitzer, planck, dss, cosmos, ETC.
     * TODO Have this 2 datasets as example but the information has to come from the client itself
     */
    public enum DS {
        SEIP("SEIP", "spitzer", "seip_science", FILE_TYPE_KEY + "='science' and fname like \'%.mosaic.fits\'"),
        MSX("MSX", "msx", "msx_images", "");
        private final String extraFilter;
        private String name;
        private String schema;
        private String table;


        DS(String name, String schema, String table, String xtraFilter) {
            this.name = name;
            this.schema = schema;
            this.table = table;
            this.extraFilter = xtraFilter;
        }

        /**
         * DataSet name
         *
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Schema = Mission, see https://irsadev.ipac.caltech.edu/ibe
         *
         * @return schema name which is the mission in IBE2/ATLAS
         */
        public String getSchema() {
            return schema;
        }

        /**
         * Table name in IRSA archives, see https://irsadev.ipac.caltech.edu/TAP/sync?query=select+c.schema_name,+c.table_name+from+TAP_SCHEMA.columns+c+JOIN+TAP_SCHEMA.tables+t+ON+c.table_name=t.table_name+where+c.column_name='ra4'+AND+t.irsa_dbms='POSTGRES'
         *
         * @return
         */
        public String getTable() {
            return table;
        }

        public String getSpecificFilter() {
            return extraFilter;
        }

        public static DS getDS(String name) {
            for (DS ds : values()) {
                if (ds.getName().equalsIgnoreCase(name)) {
                    return ds;
                }
            }
            return null;
        }
    }

    public AtlasIbeDataSource() {
    }

    /**
     * Convenient constructor to build a datasource by using a combination of schema name and table names in more generic way than ENUM string
     *
     * @param schema schema name, i.e. 'spitzer'
     * @param table  table name, i.e 'seip_science'
     */
    public AtlasIbeDataSource(String schema, String table) {
        this(null, schema, table);
    }

    public AtlasIbeDataSource(DS ds) {
        this(null, ds);
    }


    public AtlasIbeDataSource(String ibeHost, String schema1, String table1) {
        setupDS(ibeHost, schema1, table1);
    }

    public AtlasIbeDataSource(String ibeHost, DS ds) {
        setupDS(ibeHost, ds.getSchema(), ds.getTable());
    }

    @Override
    public void initialize(Map<String, String> dsInfo) {

        //For example using key
        // TODO this should me removed in favor of survey = schema.table
        ds = DS.getDS(dsInfo.get(DS_KEY));
        String schema = null, table = null;
        if (ds != null) {
            schema = ds.getSchema();
            table = ds.getTable();
        } else {
            if (dsInfo.get(DATASET_KEY) != null && dsInfo.get(TABLE_KEY) != null) {
                // When info comes from edu.caltech.ipac.visualize.net.BaseIrsaParams
                // WebPlotRquest should also have the information from master table (see IRSA-816)
                schema = dsInfo.get(DATASET_KEY);
                table = dsInfo.get(TABLE_KEY);
            } else {
                if (!StringUtils.isEmpty(dsInfo.get(DS_KEY))) {
                    String[] defs = dsInfo.get(DS_KEY).split("\\.");
                    if (defs.length == 2) {
                        schema = defs[0];
                        table = defs[1];
                    }
                }

            }
        }
        setupDS(null, schema, table);
    }

    private void setupDS(String ibeHost, String dataset, String table) {

        if (StringUtils.isEmpty(ibeHost)) {
            ibeHost = AppProperties.getProperty("atlas.ibe.host", "https://irsa.ipac.caltech.edu");
        }
        setIbeHost(ibeHost);
        setMission(ATLAS);
        setDataset(dataset);
        setTableName(table);
    }

    public DS getDS() {
        return ds;
    }

    //this method encode the file name in case it has + or some other sign
    private String getEncodedFileName(String fname){

            String[] fpArray =fname.split("/");

            try {
                String str=new String();
                for (int i=0; i<fpArray.length; i++){
                    str += URLEncoder.encode(fpArray[i], "UTF-8") + "/";
                }
                return str;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
    }
    @Override
    public IbeDataParam makeDataParam(Map<String, String> pathInfo) {
        IbeDataParam dataParam = new IbeDataParam();

        String fname = getEncodedFileName( pathInfo.get("fname") );

        String root = getImageRoot();

        dataParam.setFilePath(root + "/" + fname);

        // check cutout params
        // look for ra_obj first - moving object search
        String subLon = pathInfo.get("ra_obj");
        if (StringUtils.isEmpty(subLon)) {
            // next look for in_ra (IBE returns this)
            subLon = pathInfo.get("in_ra");
            if (StringUtils.isEmpty(subLon)) {
                // all else fails, try using crval1
                subLon = pathInfo.get("crval1");
            }
        }

        // look for dec_obj first - moving object search
        String subLat = pathInfo.get("dec_obj");
        if (StringUtils.isEmpty(subLat)) {
            // next look for in_dec (IBE returns this)
            subLat = pathInfo.get("in_dec");
            if (StringUtils.isEmpty(subLat)) {
                // all else fails, try using crval2
                subLat = pathInfo.get("crval2");
            }
        }
        String subSize = pathInfo.get("subsize");

        if (!StringUtils.isEmpty(subLon) && !StringUtils.isEmpty(subLat) && !StringUtils.isEmpty(subSize)) {
            dataParam.setCutout(true, subLon + "," + subLat, subSize);
        }

        if (dataParam.getFilePath() != null && dataParam.getFilePath().endsWith(".gz")) {
            dataParam.setDoZip(true);
        }

        return dataParam;
    }

    @Override
    public IbeQueryParam makeQueryParam(Map<String, String> queryInfo) {

        // source search
        IbeQueryParam queryParam = new IbeQueryParam();

        // process POS - target search
        String userTargetWorldPt = queryInfo.get("UserTargetWorldPt");
        if (userTargetWorldPt != null) {
            WorldPt pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = Plot.convert(pt, CoordinateSys.EQ_J2000);
                queryParam.setPos(pt.getLon() + "," + pt.getLat());
                if (!StringUtils.isEmpty(queryInfo.get("intersect"))) {
                    queryParam.setIntersect(IbeQueryParam.Intersect.valueOf(queryInfo.get("intersect")));
                }
                String mcen = queryInfo.get("mcenter");
                if (mcen != null && (mcen.equalsIgnoreCase(MCEN) || Boolean.parseBoolean(mcen))) {
                    queryParam.setMcen(true);

                } else {
                    // workaround:
                    if (StringUtils.isEmpty(queryInfo.get("radius"))) {
                        queryParam.setSize(queryInfo.get("size"));
                    } else {
                        queryParam.setSize(queryInfo.get("radius"));
                    }
                }
            }
        }
        // process constraints
        String constraints = processAtlasConstraints(queryInfo);
        if (!StringUtils.isEmpty(constraints)) {
            queryParam.setWhere(constraints);
        }

        return queryParam;
    }

    private String processAtlasConstraints(Map<String, String> queryInfo) {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";

        String bands = queryInfo.get(BAND_KEY);
        // process BAND - ENUMSTRING
        if (!StringUtils.isEmpty(bands)) {
            String checkedBands = checkAndConvert2StringSingleQuote(bands);
            constraints.add("band_name IN (" + checkedBands + ")");
        }
        String inst = queryInfo.get(INSTRUMENT_KEY);
        if (!StringUtils.isEmpty(inst)) {
            constraints.add("instrument_name=\'" + inst + "\'");
        }

        // Main image to be displayed is controlled with 'principal' colummn = 1 for each file_type
        // TODO remove that when 'principal' is available in metadata (IRSA-822) and assign to 1.
        String pvalue = queryInfo.get(PRINCIPAL_KEY);
        if (!StringUtils.isEmpty(pvalue)) {
            constraints.add(PRINCIPAL_KEY + "=" + pvalue);
        }

        // extra filter is passed from the request client or the example ENUM defined
        // This can be different for different ATLAS dataset: cosmos, herschel, spitzer, etc.
        String xtraFilter = ds == null ? queryInfo.get(XTRA_KEY) : ds.getSpecificFilter();
        if (!StringUtils.isEmpty(xtraFilter)) {
            constraints.add(xtraFilter);
        }
        // compile all constraints
        if (!constraints.isEmpty()) {

            int i = 0;
            for (String s : constraints) {
                if (i > 0) {
                    constrStr += " AND ";
                }
                constrStr += s;

                i++;
            }
        }
        //Overwrite if where key is set:
        String where = queryInfo.get("where");
        if (where != null) {
            constrStr = where;
        }
        return constrStr;
    }

    /**
     * Check and convert if needed to single quote value to be used in where=band in (...)
     *
     * @param str CSV string
     * @return CSV single quoted values
     */
    private String checkAndConvert2StringSingleQuote(String str) {
        String res = "";
        int i = 0;
        String[] split = str.split(",");
        for (String b : split) {
            b = b.trim();
            if (b.lastIndexOf("'") < 0) {
                b += "\'";
            }
            if (!b.startsWith("'")) {
                b = "\'" + b;
            }
            res += b;
            if (i < split.length - 1) {
                res += ",";
            }
            i++;
        }
        return res;
    }

    @Override
    public String getDataUrl(IbeDataParam param) {
        return this.getIbeHost() + "/ibe/" + param.getFilePath();
    }

    @Override
    public String getSearchUrl() {
        return this.getIbeHost() + "/IBE?table=" +
                this.getDataset() + "." + this.getTableName();
    }

    @Override
    public String getQueryUrl(IbeQueryParam param) {
        return getSearchUrl() + "&" + convertToUrl(param);
    }

    @Override
    public String getMetaDataUrl() {
        return this.getIbeHost() + "/IBE?table=" +
                this.getDataset() +
                "." + this.getTableName() + "&FORMAT=METADATA";
    }

    private String convertToUrl(IbeQueryParam param) {
        String s = "";
        if (param == null) return "";

        if (!StringUtils.isEmpty(param.getRefBy())) {
            s = addUrlParam(s, REF_BY, param.getRefBy());
        } else if (!StringUtils.isEmpty(param.getPos())) {
            s = addUrlParam(s, POS, param.getPos());
            s = addUrlParam(s, INTERSECT, param.getIntersect());
            if (param.isMcen()) {
                s = addUrlParam(s, null, MCEN);
            } else {
                s = addUrlParam(s, SIZE, param.getSize());
            }
        }

        s = addUrlParam(s, COLUMNS, param.getColumns()); // ATLAS IBE2 doesn't have the columns filter, might want to do TAP data source for that purpose?
        s = addUrlParam(s, WHERE, param.getWhere(), true);
        return s;
    }

    public String getImageRoot() {
        return "/data/" + getDataset() + "/" + getTableName();
    }
}
