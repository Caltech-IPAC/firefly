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

import java.util.ArrayList;
import java.util.Map;

/**
 * Date: 4/18/14
 *
 * @author loi
 * @version $Id: $
 */
public class PtfIbeDataSource extends BaseIbeDataSource {
    public static final String PTF     = "ptf";
    private final static String HOST    = "host";
    private final static String SCHEMA  = "schema";
    private final static String TABLE   = "table";

    public static enum DATA_TYPE {
        INTENSITY, MASK, UNCERTAINTY, COVERAGE, DIFF_SPIKES, HALOS, OPT_GHOSTS, LATENTS
    }

    public enum DataProduct {
        LEVEL1("images","level1"),
        LEVEL2("images","level2");

        private String dataset;
        private String table;


        DataProduct(String dataset, String imageTable) {
            this.dataset = dataset;
            this.table = imageTable;
        }

        public String getDataset() { return dataset;}
        public String getTable() { return table;}
    }

    public PtfIbeDataSource() {}

    public PtfIbeDataSource(DataProduct ds) {
        this(null, ds);
    }

    public PtfIbeDataSource(String ibeHost, DataProduct ds) {
        setupDS(ibeHost, ds.getDataset(), ds.getTable());
    }

//====================================================================
//  WISE implementation of IBE services
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

        if (getTableName().equalsIgnoreCase(DataProduct.LEVEL2.name())) {
            String filename = pathInfo.get("filename");
            String fieldId = pathInfo.get("ptffield");
            String filterId = pathInfo.get("fid");
            String ccdId = pathInfo.get("ccdid");

            dataParam.setFilePath("/d" + fieldId + "/f" + filterId + "/c" + ccdId + "/" + filename);
        } else {
            dataParam.setFilePath(pathInfo.get("pfilename"));
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
                if (mcen != null && mcen.equalsIgnoreCase(IBE.MCEN)) {
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
            ibeHost = AppProperties.getProperty("ptf.ibe.host", "http://irsasearchops1.ipac.caltech.edu/ibe");
        }
        setIbeHost(ibeHost);
        setMission(PTF);
        setDataset(dataset);
        setTableName(table);
    }

    private String processConstraints(Map<String, String> queryInfo) {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";

//        String productLevel = req.getSafeParam("ProductLevel");
        String productLevel = "l1";

        // process L1 only constraints
        if (productLevel.equalsIgnoreCase("l1")) {
            // process DATE RANGE
            String timeStart = queryInfo.get("timeStart");
            if (!StringUtils.isEmpty(timeStart)) {
                constraints.add("obsmjd>='" + IBE.convertUnixToMJD(timeStart) + "'");
            }
            String timeEnd = queryInfo.get("timeEnd");
            if (!StringUtils.isEmpty(timeEnd)) {
                constraints.add("obsmjd<='" + IBE.convertUnixToMJD(timeEnd) + "'");
            }

            // process PTF Field IDs (support multiple field IDs)
            String ptfFields = queryInfo.get("ptfField");
            if (!StringUtils.isEmpty(ptfFields)) {
                String[] ptffieldArray = ptfFields.split("[,; ]+");
                String ptffieldConstraint = "ptffield";
                if (ptffieldArray.length == 1) {
                    ptffieldConstraint += "='" + ptffieldArray[0] + "'";
                } else {
                    ptffieldConstraint += " IN (";
                    int cnt = 0;
                    for (String ptfField : ptffieldArray) {
                        if (StringUtils.isEmpty(ptfField)) {
                            continue;
                        }

                        if (cnt > 0) {
                            ptffieldConstraint += ",";
                        }
                        ptffieldConstraint += "'" + ptfField + "'";
                        cnt++;
                    }

                    ptffieldConstraint += ")";
                }

                constraints.add(ptffieldConstraint);
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
