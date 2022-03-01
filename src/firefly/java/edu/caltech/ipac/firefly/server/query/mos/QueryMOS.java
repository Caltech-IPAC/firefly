/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.mos;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.table.TableMeta.LABEL_TAG;
import static edu.caltech.ipac.table.TableMeta.makeAttribKey;


@SearchProcessorImpl(id = "MOSQuery")
public class QueryMOS extends IpacTablePartProcessor {

    String MOST_HOST_URL = AppProperties.getProperty("most.host", "default_most_host_url");

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String RESULT_TABLE_NAME = "imgframes_matched_final_table.tbl";
    private static final String ORBITAL_PATH_TABLE_NAME = "orbital_path.tbl";
    private static final String HEADER_ONLY_PARAM = "header_only";


    @Override
    public boolean doCache() {
        return false;
    }

    @Override
    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        try {
            MOSRequest req = QueryUtil.assureType(MOSRequest.class, request);
            String tblType = req.getParam(MOSRequest.TABLE_NAME);
            boolean headerOnly = isHeaderOnlyRequest(req);

            String tblName = (tblType != null && tblType.equalsIgnoreCase(MOSRequest.ORBITAL_PATH_TABLE))
                    ? ORBITAL_PATH_TABLE_NAME : RESULT_TABLE_NAME;

            DataGroup dg = doSearch(req, tblName, headerOnly);
            return headerOnly ? getOrbitalElements(dg) : dg;

        } catch (Exception e) {
            throw new DataAccessException("MOS Query Failed.", e);
        }
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        return loadDataFileImpl(request);
    }

    private DataGroup doSearch(final MOSRequest req, String tblName, boolean headerOnly) throws IOException, DataAccessException, EndUserException {

        URL url;
        try {
            url = createURL(req, false);
        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw new EndUserException(e.getEndUserMsg(), e.getMoreDetailMsg());
        }
        URLConnection conn = null;
        try {
            _log.info("querying MOS:" + url);

            final Ref<File> catOverlayFile = new Ref<File>(null);
            Thread catSearchTread = null;
            // pre-generate gator upload file for catalog overlay
            if (req.getBooleanParam(MOSRequest.CAT_OVERLAY)) {
                catOverlayFile.setSource(File.createTempFile("mosCatOverlayFile-", ".tbl", QueryUtil.getTempDir(req)));
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            URLDownload.getDataToFile(createURL(req, true), catOverlayFile.getSource());
                        } catch (Exception e) {
                            _log.error(e);
                        }
                    }
                };
                catSearchTread = new Thread(r);
                catSearchTread.start();
            }
            // workaround for MOS service bug when launching 2 simultaneously.
            Thread.sleep(1000);
            File votable = makeFileName(req);
            URLDownload.getDataToFile(url, votable);

            if (catSearchTread != null) {
                catSearchTread.join();
            }

            DataGroup[] groups = VoTableReader.voToDataGroups(votable.getAbsolutePath(), headerOnly);
            if (groups != null) {
                for (DataGroup dg : groups) {
                    if (dg.getTitle().equalsIgnoreCase(RESULT_TABLE_NAME) && catOverlayFile.getSource() != null) {
                            // save the generated file as ipac table headers
                            dg.addAttribute(MOSRequest.CAT_OVERLAY_FILE, catOverlayFile.getSource().getPath());
                    }

                    if (dg.getTitle().equals(tblName)) {
                        return dg;
                    }
                }
            }

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "MOS Query Failed - bad url.");

        } catch (IOException e) {
            _log.error(e, e.toString());
            throw makeException(e, "MOS Query Failed - network error.");

        } catch (Exception e) {
            throw makeException(e, "MOS Query Failed.");
        }

        return null;
    }

    private String parseMessageFromServer(String response) {
        // no html, so just return
        return response.replaceAll("<br ?/?>", "");
    }

    private URL createURL(MOSRequest req, boolean forGator) throws EndUserException, IOException {
        MOSRequest request = (MOSRequest) req.cloneRequest();
        String url = request.getUrl();
        if (url == null || url.length() < 5) {
            url = MOST_HOST_URL;
        }

        if (forGator) {
            request.setParam(MOSRequest.OUTPUT_MODE, "Gator");
        }

        String paramStr = getParams(request);
        if (paramStr.startsWith("&")) {
            paramStr = paramStr.substring(1);
        }
        url += "?" + paramStr;

        return new URL(url);
    }


    protected String getParams(MOSRequest req) throws EndUserException, IOException {
        StringBuffer sb = new StringBuffer(100);
        
        String catalog = req.getParam(MOSRequest.CATALOG);
        if (StringUtils.isEmpty(catalog)) {
            catalog = getMosCatalog(req);
        }

        String outputMode = req.getParam(MOSRequest.OUTPUT_MODE);
        if (StringUtils.isEmpty(outputMode)) {
            outputMode = "VOTable";
        }

        requiredParam(sb, MOSRequest.CATALOG, catalog);
        requiredParam(sb, MOSRequest.OUTPUT_MODE, outputMode);

        // object name
        String objectName = req.getParam(MOSRequest.OBJ_NAME);
        String naifID = req.getNaifID();
        if (!StringUtils.isEmpty(naifID)) {
            requiredParam(sb, MOSRequest.INPUT_TYPE, "naifid_input");
            requiredParam(sb, MOSRequest.OBJ_TYPE, "all");
            requiredParam(sb, MOSRequest.OBJ_NAIF_ID, naifID);
        }
        else if (!StringUtils.isEmpty(objectName)) {
            requiredParam(sb, MOSRequest.INPUT_TYPE, "name_input");
            requiredParam(sb, MOSRequest.OBJ_TYPE, req.getParam(MOSRequest.OBJ_TYPE ));
            requiredParam(sb, MOSRequest.OBJ_NAME, URLEncoder.encode(objectName.trim(), "ISO-8859-1"));

        } else {
            // mpc 1-line input
            String mpcData = req.getParam(MOSRequest.MPC_DATA);
            if (!StringUtils.isEmpty(mpcData)) {
                requiredParam(sb, MOSRequest.INPUT_TYPE, "mpc_input");
                requiredParam(sb, MOSRequest.OBJ_TYPE, req.getParam(MOSRequest.OBJ_TYPE));
                requiredParam(sb, MOSRequest.MPC_DATA, URLEncoder.encode(mpcData.trim(), "ISO-8859-1"));

            } else {
                // manual input
                requiredParam(sb, MOSRequest.INPUT_TYPE, "manual_input");
                requiredParam(sb, MOSRequest.BODY_DESIGNATION,
                        URLEncoder.encode(req.getParam(MOSRequest.BODY_DESIGNATION).trim(), "ISO-8859-1"));
                requiredParam(sb, MOSRequest.EPOCH, req.getParam(MOSRequest.EPOCH));
                requiredParam(sb, MOSRequest.ECCENTRICITY, req.getParam(MOSRequest.ECCENTRICITY));
                requiredParam(sb, MOSRequest.INCLINATION, req.getParam(MOSRequest.INCLINATION));
                requiredParam(sb, MOSRequest.ARG_PERIHELION, req.getParam(MOSRequest.ARG_PERIHELION));
                requiredParam(sb, MOSRequest.ASCEND_NODE, req.getParam(MOSRequest.ASCEND_NODE));

                String objType = req.getParam(MOSRequest.OBJ_TYPE);
                requiredParam(sb, MOSRequest.OBJ_TYPE, objType);
                if (objType.equalsIgnoreCase("asteroid")) {
                    requiredParam(sb, MOSRequest.SEMIMAJOR_AXIS, req.getParam(MOSRequest.SEMIMAJOR_AXIS));
                    requiredParam(sb, MOSRequest.MEAN_ANOMALY, req.getParam(MOSRequest.MEAN_ANOMALY));

                } else if (objType.equalsIgnoreCase("comet")) {
                    requiredParam(sb, MOSRequest.PERIH_DIST, req.getParam(MOSRequest.PERIH_DIST));
                    requiredParam(sb, MOSRequest.PERIH_TIME, req.getParam(MOSRequest.PERIH_TIME));
                }
            }
        }

        String obsBegin = req.getParam(MOSRequest.OBS_BEGIN);
        if (!StringUtils.isEmpty(obsBegin)) {
            optionalParam(sb, MOSRequest.OBS_BEGIN, URLEncoder.encode((obsBegin.trim()), "ISO-8859-1"));
        }

        String obsEnd = req.getParam(MOSRequest.OBS_END);
        if (!StringUtils.isEmpty(obsEnd)) {
            optionalParam(sb, MOSRequest.OBS_END, URLEncoder.encode((obsEnd.trim()), "ISO-8859-1"));
        }

        // no longer part of hydra interface
        //optionalParam(sb, MOSRequest.EPHEM_STEP, req.getParam(MOSRequest.EPHEM_STEP));
        //optionalParam(sb, MOSRequest.SEARCH_REGION_SIZE, req.getParam(MOSRequest.SEARCH_REGION_SIZE));

        return sb.toString();
    }

    protected String getMosCatalog(MOSRequest req) {
        // TG is it even used? return req.getServiceSchema();
        return req.getParam(MOSRequest.CATALOG);
    }

    private String convertDate(String msecStr) {
        long msec = Long.parseLong(msecStr);
        Date resultdate = new Date(msec);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy M d HH:mm:ss.SSS");

        return sdf.format(resultdate);
    }

    private static File makeFileName(MOSRequest req) throws IOException {
        return File.createTempFile("mos-result", ".xml", QueryUtil.getTempDir(req));
    }

    protected static void requiredParam(StringBuffer sb, String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            requiredParam(sb, name, value + "");

        } else {
            throw new EndUserException("MOS search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));

        } else {
            throw new EndUserException("MOS search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void optionalParam(StringBuffer sb, String name) {
        sb.append(param(name));
    }

    protected static void optionalParam(StringBuffer sb, String name, String value) {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));
        }
    }

    protected static void optionalParam(StringBuffer sb, String name, boolean value) {
        sb.append(param(name, value));
    }


    protected static String param(String name) {
        return "&" + name;
    }

    protected static String param(String name, String value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, int value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, double value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, boolean value) {
        return "&" + name + "=" + (value ? "1" : "0");
    }

    protected static boolean isHeaderOnlyRequest(TableServerRequest req) {
        return req.getBooleanParam(HEADER_ONLY_PARAM);
    }


    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        String tblType = request.getParam(MOSRequest.TABLE_NAME);
        if (tblType == null || tblType.equalsIgnoreCase(MOSRequest.RESULT_TABLE)) {
            meta.setCenterCoordColumns(new TableMeta.LonLatColumns("ra_obj", "dec_obj"));
        } else {
            meta.setCenterCoordColumns(new TableMeta.LonLatColumns("RA_obs", "Dec_obs"));
        }

        TableMeta.LonLatColumns c1= new TableMeta.LonLatColumns("ra1", "dec1", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c2= new TableMeta.LonLatColumns("ra2", "dec2", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c3= new TableMeta.LonLatColumns("ra3", "dec3", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c4= new TableMeta.LonLatColumns("ra4", "dec4", CoordinateSys.EQ_J2000);
        meta.setCorners(c1, c2, c3, c4);

        meta.setAttribute("DataType", "MOS");
    }


    protected DataGroup getOrbitalElements(DataGroup inData) {
        final List<String> namesLst = Arrays.asList("object_name", "element_epoch", "eccentricity", "inclination",
                                                    "argument_perihelion", "ascending_node", "semimajor_axis", "semimajor_axis", "mean_anomaly",
                                                    "perihelion_distance", "perihelion_time");
        try {
            inData.setTitle("Result Table");
            Map<String, DataGroup.Attribute> attrMap = inData.getTableMeta().getAttributes();

            List<DataType> newDT = new ArrayList<DataType>();
            for (String s : attrMap.keySet()) {
                if (namesLst.contains(s)) {
                    DataType dt = new DataType(s, String.class);
                    newDT.add(dt);
                }
            }
            DataGroup newDG = new DataGroup("Orbital Elements", newDT);
            DataObject obj = new DataObject(newDG);
            for (DataType dt : newDT) {
                String col = dt.getKeyName();
                obj.setDataElement(dt, attrMap.get(col).getValue());
                newDG.addAttribute(makeAttribKey(LABEL_TAG, col.toLowerCase()), getOrbitalElementLabel(col));
            }
            newDG.add(obj);
            return newDG;

        } catch (Exception e) {
            _log.error(e);
        }
        return null;
    }

    private String getOrbitalElementLabel(String key) {
        if (key.equals("object_name")) {
            return "Object Name";
        } else if (key.equals("element_epoch")) {
            return "Epoch (MJD)";
        } else if (key.equals("eccentricity")) {
            return "Eccentricity";
        } else if (key.equals("inclination")) {
            return "Inclination";
        } else if (key.equals("argument_perihelion")) {
            return "Argument of Perihelion (deg)";
        } else if (key.equals("ascending_node")) {
            return "Ascending Node  (deg)";
        } else if (key.equals("semimajor_axis")) {
            return "Semi-major Axis (AU)";
        } else if (key.equals("mean_anomaly")) {
            return "Mean Anomaly (deg)";
        } else if (key.equals("perihelion_distance")) {
            return "Perihelion Distance (AU)";
        } else if (key.equals("perihelion_time")) {
            return "Perihelion Time (JD)";
        }
        return key;
    }


}

