/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe.datasource;

import edu.caltech.ipac.astro.ibe.BaseIbeDataSource;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * @author tatianag
 *         $Id: $
 */
public class TwoMassIbeDataSource extends BaseIbeDataSource {
    public static final String TWOMASS = "twomass";

    public static final String DS_KEY = "ds";
    public static final String BAND_KEY = "band";
    public static final String HEM_KEY = "hem";

    public static final String XDATE_KEY = "xdate";
    public static final String SCAN_KEY = "scan";

    public static final String XTRA_CONSTRAINT = "xtraConstraint"; // IRSA-2742

    private DS ds;

    public enum DS {
        ASKY("asky", "allsky","allsky"),
        ASKYW("askyw","full","full"),
        SX("sx","sixxcat","sixxcat"),
        SXW("sxw","sixxfull","sixxfull"),
        MOSAIC("mosaic", "mosaic", "sixdeg"), // not ready yet
        CAL("cal", "calibration", "calibration"),
        LH("lh", "lh","lh");

        private String name;
        private String schema;
        private String table;


        DS(String name, String schema, String table) {
            this.name = name;
            this.schema = schema;
            this.table = table;
        }

        public String getName() { return name;}
        public String getSchema() { return schema;}
        public String getTable() { return table;}

        public static DS getDS(String name) {
            for (DS ds : values()) {
                if (ds.getName().equalsIgnoreCase(name)) {
                    return ds;
                }
            }
            return null;
        }
    }

    @Override
    public void initialize(Map<String, String> dsInfo) {

        ds = DS.getDS(dsInfo.get(DS_KEY));
        setupDS(null, ds.getSchema(), ds.getTable());
    }

    private void setupDS(String ibeHost, String dataset, String table) {

        if (StringUtils.isEmpty(ibeHost)) {
            ibeHost = AppProperties.getProperty("twomass.ibe.host", "https://irsa.ipac.caltech.edu/ibe");
        }
        setIbeHost(ibeHost);
        setMission(TWOMASS);
        setDataset(dataset);
        setTableName(table);
    }

    public DS getDS() { return ds; }

    @Override
    public IbeDataParam makeDataParam(Map<String, String> pathInfo) {
        IbeDataParam dataParam = new IbeDataParam();

        String ordate = pathInfo.get("ordate");
        String hemisphere = pathInfo.get("hemisphere");
        int scanno = StringUtils.getInt(pathInfo.get("scanno"), -1);
        NumberFormat ne = NumberFormat.getInstance();
        ne.setMinimumIntegerDigits(3);
        String fname = pathInfo.get("fname");

        String band = pathInfo.get("band");
        if (StringUtils.isEmpty(band)) {
            // IBE's filter column is equivalent to band
            band = pathInfo.get("filter");
        }

        if (!fname.endsWith(".tbl")) {
            fname =  "image/" + fname;
        }

        if(this.ds.equals(DS.MOSAIC)){
            //ex https://<hostname>/ibe/data/twomass/mosaic/sixdeg/j/01381/mosaic_6deg_j01381_1asec.fits
            int seqnum = StringUtils.getInt(pathInfo.get("seqnum"));
            dataParam.setFilePath( pathInfo.get("fname") );
        }else {
            // sample path
            // http://<hostname>:8000/ibe/data/twomass/allsky/allsky/980623n/s076/image/hi0760126.fits.gz
            dataParam.setFilePath(ordate + hemisphere + "/" + "s" + ne.format(scanno) + "/" + fname);
        }

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
        if(!StringUtils.isEmpty(subSize)){
            if(subSize.equals("null")){
                subSize = null;
            }
        }

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

        // common position search params
        IbeQueryParam queryParam = super.makeQueryParam(queryInfo);

        // process constraints
        String constraints = processConstraints(queryInfo);
        if (!StringUtils.isEmpty(constraints)) {
            queryParam.setWhere(constraints);
        }

        return queryParam;
    }

    //http://<hostname>:8000/search/twomass/full/full?POS=302.6,38.7&columns=atlasdir,ordate,hemisphere,scanno,fname,filter&WHERE=filter%20in%20%28%27j%27%29%20and%20scanno=76%20and%20hemisphere=%27n%27
    private String processConstraints(Map<String, String> queryInfo) {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";

        //Add proper filter for mosaic in IBE (IRSA-2742)
        String filter = queryInfo.get(XTRA_CONSTRAINT);
        String filterStr = "";
        String band = queryInfo.get(BAND_KEY);
        if (!StringUtils.isEmpty(band) && !band.startsWith("A")) {
            filterStr += "filter = \'" + band.toLowerCase() + "\'";
            if (!StringUtils.isEmpty(filter)) {
                filterStr += " and " + filter;
            }
            constraints.add(filterStr);
        } else if (!StringUtils.isEmpty(filter)) {
            filterStr += "filter = " + filter;
            constraints.add(filterStr);
        }

        String hem = queryInfo.get(HEM_KEY);
        if (!StringUtils.isEmpty(hem)&&!hem.startsWith("a"))  {
            constraints.add("hemisphere=\'"+hem+"\'");
        }

        Date xdate = StringUtils.getDate(queryInfo.get(XDATE_KEY));
        if (xdate != null)  {
            constraints.add("ordate=\'"+(new SimpleDateFormat("yyMMdd")).format(xdate)+"\'");
        }

        int scan = StringUtils.getInt(queryInfo.get(SCAN_KEY),-1);
        if (scan != -1) {
            constraints.add("scanno="+scan);
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

        return constrStr;
    }

}
