/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 4/18/14
 *
 * @author loi
 * @version $Id: $
 */
public class BaseIbeDataSource implements IbeDataSource {

    private String ibeHost;
    private String mission;
    private String dataset;
    private String tableName;
    private boolean useFileSystem = false;
    private String baseFilesystemPath;

    public BaseIbeDataSource() {
    }

    public BaseIbeDataSource(String ibeHost, String mission, String dataset, String tableName) {
        this.ibeHost = ibeHost;
        this.mission = mission;
        this.dataset = dataset;
        this.tableName = tableName;
    }

    public void initialize(Map<String, String> sourceInfo) {}

    public String getIbeHost() {
        return ibeHost;
    }

    public IbeDataParam makeDataParam(Map<String, String> pathInfo) {
        return null;
    }

    public IbeQueryParam makeQueryParam(Map<String, String> queryInfo) {

        // handles only the common position search params
        IbeQueryParam queryParam = new IbeQueryParam();

        String userTargetWorldPt = queryInfo.get("UserTargetWorldPt");
        String upload = queryInfo.get("filename");
        if (userTargetWorldPt != null || !isEmpty(upload) ) {
            // search by position
            if (userTargetWorldPt != null) {
                WorldPt pt = WorldPt.parse(userTargetWorldPt);
                if (pt != null) {
                    pt = VisUtil.convert(pt, CoordinateSys.EQ_J2000);
                    queryParam.setPos(pt.getLon() + "," + pt.getLat());
                }
            } else {
                queryParam.setPos(queryInfo.get("filename"));
            }
        }
        
        if (!StringUtils.isEmpty(queryInfo.get("intersect"))) {
            queryParam.setIntersect(IbeQueryParam.Intersect.valueOf(queryInfo.get("intersect")));
        }
        String mcen = queryInfo.get("mcenter");
        if (mcen != null && (mcen.equalsIgnoreCase(MCEN) || Boolean.parseBoolean(mcen))) {
            queryParam.setMcen(true);
        } else {
            queryParam.setSize(queryInfo.get("size"));
        }

        return queryParam;
    }

    public void setIbeHost(String ibeHost) {
        if (!StringUtils.isEmpty(ibeHost) &&
                !ibeHost.toLowerCase().startsWith("http")) {
            ibeHost = "https://" + ibeHost.trim();
        }
        this.ibeHost = ibeHost;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public boolean useFileSystem() {
        return useFileSystem;
    }

    public void setUseFileSystem(boolean useFileSystem) {
        this.useFileSystem = useFileSystem;
    }

    public String getBaseFilesystemPath() {
        return baseFilesystemPath;
    }

    @Override
    public String getDataUrl(IbeDataParam param) {
        String dataUrl = this.getIbeHost() + "/data/" +
                this.getMission() + "/" + this.getDataset() +
                "/" + this.getTableName() + "/" ;

        String fpath = param.getFilePath();

        String url = dataUrl+ fpath;
        return url;
    }

    @Override
    public String getMetaDataUrl() {
        return getSearchUrl()+ "?FORMAT=METADATA";
    }

    @Override
    public String getQueryUrl(IbeQueryParam param) {
        String url = getSearchUrl();
        String qStr = convertToUrl(param);
        qStr = isEmpty(qStr) ? "" : (url.contains("?") ? "&" : "?") + convertToUrl(param);
        return url + qStr;
    }

    @Override
    public Map<String, String> getMulipleQueryParam(IbeQueryParam param) {
        return asMap(param);
    }

    @Override
    public String getSearchUrl() {
        return this.getIbeHost() + "/search/" +
                this.getMission() + "/" + this.getDataset() +
                "/" + this.getTableName();
    }

    @Override
    public String getCorners() {
        return "ra1;dec1;EQ_J2000,ra2;dec2;EQ_J2000,ra3;dec3;EQ_J2000,ra4;dec4;EQ_J2000";
    }

    @Override
    public String getCenterCols() {
        return "crval1;crval2;EQ_J2000";
    }

    @Override
    public String[] getColsToHide() {
        return new String[]{"in_row_id", "in_ra", "in_dec",
                "crval1", "crval2",
                "ra1", "ra2", "ra3", "ra4",
                "dec1", "dec2", "dec3", "dec4"
        };
    }

    public void setBaseFilesystemPath(String baseFilesystemPath) {
        this.baseFilesystemPath = baseFilesystemPath;
    }

    private Map<String, String> asMap(IbeQueryParam param) {

        HashMap<String, String> params = new HashMap<>();

        if (param == null) return params;

        if (!StringUtils.isEmpty(param.getRefBy())) {
            params.put(REF_BY, param.getRefBy());
        } else if (!StringUtils.isEmpty(param.getPos())) {
            params.put(POS, param.getPos());
            applyIfNotEmpty(param.getIntersect(), (v) -> params.put(INTERSECT, v.toString()));
            if (param.isMcen()) {
                params.put(MCEN, null);
            } else {
                applyIfNotEmpty(param.getSize(), (v) -> params.put(SIZE, v));
            }
        }

        applyIfNotEmpty(param.getColumns(), (v) -> params.put(COLUMNS, v));
        applyIfNotEmpty(param.getWhere(), (v) -> params.put(WHERE, v));

        return params;
    }

    private String convertToUrl(IbeQueryParam param) {
        Map<String, String> params = asMap(param);
        if (params.size() == 0) return "";
        return params.keySet().stream().reduce("",
                (ac, e) -> addUrlParam(ac, e, params.get(e), true));
    }

    public static String addUrlParam(String url, String key, Object value) {
        return addUrlParam(url, key, value, false);
    }

    public static String addUrlParam(String url, String key, Object value, boolean doEncode) {
        try {
            if (!StringUtils.isEmpty(url)) {
                url = url + "&";
            }
            if (!StringUtils.isEmpty(value)) {
                value = doEncode ? URLEncoder.encode(value.toString(), "UTF-8") : value;
            }
            value = StringUtils.isEmpty(value) ? "" : "=" + value;
            url = url + key + value;
        } catch (UnsupportedEncodingException ignored) {}
        return url;
    }

}
