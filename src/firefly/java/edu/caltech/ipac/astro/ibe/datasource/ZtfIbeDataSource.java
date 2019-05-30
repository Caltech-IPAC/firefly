/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe.datasource;

import edu.caltech.ipac.astro.ibe.BaseIbeDataSource;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.firefly.server.query.ztf.ZtfRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Date: 3/20/2018
 *
 * @author wmi
 * @version $Id: $
 */
public class ZtfIbeDataSource extends BaseIbeDataSource {
    public static final String ZTF     = "ztf";
    private final static String HOST    = "host";
    private final static String SCHEMA  = "schema";
    private final static String TABLE   = "table";

    public static enum DATA_TYPE {
        INTENSITY, MASK, UNCERTAINTY, COVERAGE
    }

    public enum DataProduct {
        SCI("products","sci"),
        REF("products","ref");

        private String dataset;
        private String table;


        DataProduct(String dataset, String imageTable) {
            this.dataset = dataset;
            this.table = imageTable;
        }

        public String getDataset() { return dataset;}
        public String getTable() { return table;}
    }

    public ZtfIbeDataSource() {}

    public ZtfIbeDataSource(DataProduct ds) {
        this(null, ds);
    }

    public ZtfIbeDataSource(String ibeHost, DataProduct ds) { setupDS(ibeHost, ds.getDataset(), ds.getTable());
    }

//====================================================================
//  ZTF implementation of IBE services
//====================================================================

    /**
     * use the dsInfo to define this datasource.  all values in DataProduct must be populated.
     * @param dsInfo data set information
     */
    @Override
    public void initialize(Map<String, String> dsInfo) {

        String host = dsInfo.get(HOST);
        String schema = dsInfo.get(SCHEMA);
        String table = dsInfo.get(TABLE);
        setupDS(host, schema, table);
    }

    @Override
    public IbeDataParam makeDataParam(Map<String, String> pathInfo) {
        IbeDataParam dataParam = new IbeDataParam();

        String filepath;

        if (getTableName().equalsIgnoreCase(DataProduct.SCI.name())) {
            String field = pathInfo.get("field");
            String filtercode = pathInfo.get("filtercode");
            String qid = pathInfo.get("qid");
            String ccdid = pathInfo.get("ccdid");
            String filefracday = pathInfo.get("filefracday");
            String mjd = pathInfo.get("mjd");
            String expid = pathInfo.get("expid");
            String YYYY = filefracday.substring(0,4);
            String MMDD = filefracday.substring(4,8);
            String dddddd = filefracday.substring(8,14);
            String formatccdid = ("00" + ccdid).substring(ccdid.length());
            String formatfield =  ("000000" + field).substring(field.length());
            String baseDir = YYYY + "/" + MMDD+ "/" + dddddd +"/";
            String baseFile = "ztf_" + filefracday + "_" + formatfield +"_" + filtercode +"_c" + formatccdid + "_o_" + "q" + qid + ZtfRequest.SCIIMAGE;
            //String rawbaseDir = "raw/" + YYYY + "/" + MMDD+ "/" + dddddd +"/";
            //String rawbaseFile = rawbaseDir + "ztf_" + filefracday + "_" + formatfield +"_" + filtercode +"_c" + formatccdid + "_o.fits.fz";


            dataParam.setFilePath(baseDir + baseFile);
        } else if (getTableName().equalsIgnoreCase(DataProduct.REF.name())) {

            String field = pathInfo.get("field");
            String filtercode = pathInfo.get("filtercode");
            String qid = pathInfo.get("qid");
            String ccdid = pathInfo.get("ccdid");
            String formatccdid = ("00" + ccdid).substring(ccdid.length());
            String formatfield =  ("000000" + field).substring(field.length());
            String fff = formatfield.substring(0,3);
            String refbaseDir = fff + "/" + "field" + formatfield + "/" + filtercode +"/" + "ccd" +formatccdid +"/" + "q" + qid +"/";
            String refbaseFile = "ztf_" + formatfield + "_" + filtercode +"_c" + formatccdid + "_q" + qid + ZtfRequest.REFIMAGE;
            dataParam.setFilePath(refbaseDir + refbaseFile);
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

        // look for dec_obj first - moving osetupDS(host, schema, table) object search
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
                if (mcen != null && mcen.equalsIgnoreCase(MCEN)) {
                    queryParam.setMcen(true);

                } else {
                    queryParam.setSize(queryInfo.get("size"));
                }
            }
        }
        // process constraints
        String constraints = processConstraints(queryInfo);
        if (!StringUtils.isEmpty(constraints)) {
            queryParam.setWhere(constraints);
        }
        return queryParam;
    }


//====================================================================
//  supporting internal logic..
//====================================================================

    private void setupDS(String ibeHost, String dataset, String table) {

        if (StringUtils.isEmpty(ibeHost)) {
            ibeHost = AppProperties.getProperty("ztf.ibe.host", "https://irsa.ipac.caltech.edu/ibe");
        }
        setIbeHost(ibeHost);
        setMission(ZTF);
        setDataset("products");
        setTableName(table);

    }

    private String processConstraints(Map<String, String> queryInfo) {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";

        //if (getTableName().equalsIgnoreCase(DataProduct.REF.name())) {
            // process DATE RANGE
            String timeStart = queryInfo.get("timeStart");
            if (!StringUtils.isEmpty(timeStart)) {
                constraints.add("obsjd>='" + IBE.convertUnixToMJD(timeStart) + "'");
            }
            String timeEnd = queryInfo.get("timeEnd");
            if (!StringUtils.isEmpty(timeEnd)) {
                constraints.add("obsjd<='" + IBE.convertUnixToMJD(timeEnd) + "'");
            }

            // process Ztf Field IDs (support multiple field IDs)
            String ztfFields = queryInfo.get("ztfField");
            if (!StringUtils.isEmpty(ztfFields)) {
                String[] ztffieldArray = ztfFields.split("[,; ]+");
                String ztffieldConstraint = "field";
                if (ztffieldArray.length == 1) {
                    ztffieldConstraint += "='" + ztffieldArray[0] + "'";
                } else {
                    ztffieldConstraint += " IN (";
                    int cnt = 0;
                    for (String ztfField : ztffieldArray) {
                        if (StringUtils.isEmpty(ztfField)) {
                            continue;
                        }

                        if (cnt > 0) {
                            ztffieldConstraint += ",";
                        }
                        ztffieldConstraint += "'" + ztfField + "'";
                        cnt++;
                    }

                    ztffieldConstraint += ")";
                }

                constraints.add(ztffieldConstraint);
            }

            //process CCD IDs (support multiple ccdids)
            String ccdIds = queryInfo.get("ccdId");
            if (!StringUtils.isEmpty(ccdIds)) {
                String[] ccdIdArray = ccdIds.split("[,; ]+");
                String ccdIdConstraint = "ccdid";
                if (ccdIdArray.length == 1) {
                    ccdIdConstraint += "='" + ccdIdArray[0] + "'";
                } else {
                    ccdIdConstraint += " IN (";
                    int cnt = 0;
                    for (String ccdId : ccdIdArray) {
                        if (StringUtils.isEmpty(ccdId)) {
                            continue;
                        }

                        if (cnt > 0) {
                            ccdIdConstraint += ",";
                        }
                        ccdIdConstraint += "'" + ccdId + "'";
                        cnt++;
                    }

                    ccdIdConstraint += ")";
                }

                constraints.add(ccdIdConstraint);
            }

            //process filter constriant, this is from image search panel
            String filtercodeConstr = queryInfo.get("filtercode");
            if (!StringUtils.isEmpty(filtercodeConstr)) {
                String ztffilterConstraint = "filtercode='" + filtercodeConstr + "'";
                constraints.add(ztffilterConstraint);
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
