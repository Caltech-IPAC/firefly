/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.mos;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.DateUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.table.TableMeta.*;


@SearchProcessorImpl(id = "MOSQuery")
public class QueryMOS extends EmbeddedDbProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    protected static final String RESULT_TABLE_NAME = "imgframes_matched_final_table.tbl";
    protected static final String ORBITAL_PATH_TABLE_NAME = "orbital_path.tbl";
    String MOST_HOST_URL = AppProperties.getProperty("most.host", "default_most_host_url");

    /**
     * MOS search returns both tables.  But, we had to call it twice
     * from the client; one for orbital path, the other for MOS results.
     * Therefore, we remove TABLE_NAME param to force duplicate requests locking
     */
    @Override
    public String getUniqueID(ServerRequest request) {
        ServerRequest pReq = request.cloneRequest();
        pReq.removeParam(MOSRequest.TABLE_NAME);
        String uid = super.getUniqueID(pReq);
        return uid;
    }

    /**
     * Override to create new dbFile for Orbital results
     * By creating new dbFile, it will force a fetchDataGroup.
     */
    @Override
    public DbAdapter getDbAdapter(TableServerRequest treq) {
        DbAdapter dbAdapter = super.getDbAdapter(treq);
        if (getTblName(treq).equals(ORBITAL_PATH_TABLE_NAME)) {
            File dbFile = dbAdapter.getDbFile();
            return DbAdapter.getAdapter(new File(dbFile.getParentFile(), "orb-" + dbFile.getName()));
        }
        return dbAdapter;
    }

    @Override
    public DataGroup fetchDataGroup(TableServerRequest request) throws DataAccessException {
        try {
            MOSRequest req = QueryUtil.assureType(MOSRequest.class, request);
            String tblName = getTblName(request);
            DataGroup dg = doSearch(req, tblName, false);
            if (dg != null) addMeta(dg, request);

            return dg;

        } catch (Exception e) {
            throw new DataAccessException("MOS Query Failed.", e);
        }
    }

    protected String getTblName(ServerRequest req) {
        String tblType = req.getParam(MOSRequest.TABLE_NAME);
        return (tblType != null && tblType.equalsIgnoreCase(MOSRequest.ORBITAL_PATH_TABLE))
                ? ORBITAL_PATH_TABLE_NAME : RESULT_TABLE_NAME;
    }

    private void addMeta(DataGroup dg, TableServerRequest request) {
        // adding meta to the table  ==>
        TableMeta meta = dg.getTableMeta();
        if (getTblName(request).equalsIgnoreCase(RESULT_TABLE_NAME)) {
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

            TableServerRequest pReq = (TableServerRequest) req.cloneRequest();
            pReq.removeParam(MOSRequest.TABLE_NAME);
            String uid = DigestUtils.md5Hex(getUniqueID(pReq));

            File votable = new File(QueryUtil.getTempDir(req), uid+".xml");
            if (!votable.canRead()) {
            URLDownload.getDataToFile(url, votable);
            }

            DataGroup[] groups = VoTableReader.voToDataGroups(votable.getAbsolutePath(), headerOnly);
            if (groups != null) {
                for (DataGroup dg : groups) {
                    if (dg.getTitle().equalsIgnoreCase(RESULT_TABLE_NAME) && catOverlayFile.get() != null) {
                            // save the generated file as ipac table headers
                            dg.addAttribute(MOSRequest.CAT_OVERLAY_FILE, catOverlayFile.get().getPath());
                    }

                    if (dg.getTitle().equals(tblName)) {
                        return dg;
                    }
                }
            }

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw new DataAccessException("MOS Query Failed - bad url.", e);

        } catch (IOException e) {
            _log.error(e, e.toString());
            throw new DataAccessException("MOS Query Failed - network error.", e);

        } catch (Exception e) {
            throw new DataAccessException("MOS Query Failed.", e);
        }

        return null;
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

    //Using catch Exception including the empty ObsDate
    private static String getObsDate(String obsDateParam) {
        try {
            DateUtils.parseDate(obsDateParam, new String[]{"yyyy-MM-dd", "yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd'T'hh:mm:ssX"});     // check for valid dates
            return obsDateParam.replaceAll("[^0-9:-]", " ").trim();   // convert to format expected by MOST
        } catch (Exception e) {
            return null;
        }
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

        String obsBegin = getObsDate(req.getParam(MOSRequest.OBS_BEGIN));
        if (!StringUtils.isEmpty(obsBegin)) {
            optionalParam(sb, MOSRequest.OBS_BEGIN, URLEncoder.encode(obsBegin, "ISO-8859-1"));
        }

        String obsEnd = getObsDate(req.getParam(MOSRequest.OBS_END));
        if (!StringUtils.isEmpty(obsEnd)) {
            optionalParam(sb, MOSRequest.OBS_END, URLEncoder.encode(obsEnd, "ISO-8859-1"));
        }

        return sb.toString();
    }

    protected String getMosCatalog(MOSRequest req) {
        // TG is it even used? return req.getServiceSchema();
        return req.getParam(MOSRequest.CATALOG);
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

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));

        } else {
            throw new EndUserException("MOS search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void optionalParam(StringBuffer sb, String name, String value) {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));
        }
    }

    protected static String param(String name, String value) {
        return "&" + name + "=" + value;
    }

}

