/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

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
        return null;
    }

    public void setIbeHost(String ibeHost) {
        if (!StringUtils.isEmpty(ibeHost) &&
                !ibeHost.toLowerCase().startsWith("http")) {
            ibeHost = "http://" + ibeHost.trim();
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
        return getSearchUrl() + "?" + convertToUrl(param);
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
        String s = convertToUrl(param);
        String[] pp = s.split("&");
        for (String keyval : pp) {
            if (!StringUtils.isEmpty(keyval)) {
                String[] parts = keyval.split("=", 2);
                if (!StringUtils.isEmpty(parts[0])) {
                    String v = parts.length > 1 && !StringUtils.isEmpty(parts[1]) ? parts[1].trim() : "";
                    params.put(parts[0], v);
                }
            }
        }
        return params;
    }


    private String convertToUrl(IbeQueryParam param) {
        String s = "";
        if (param == null) return "";

        if (!StringUtils.isEmpty(param.getRefBy())) {
            s =addUrlParam(s, REF_BY, param.getRefBy());
        } else if (!StringUtils.isEmpty(param.getPos())) {
            s = addUrlParam(s, POS, param.getPos());
            s = addUrlParam(s, INTERSECT, param.getIntersect());
            if (param.isMcen()) {
                s = addUrlParam(s, null, MCEN);
            } else {
                s = addUrlParam(s, SIZE, param.getSize());
            }
        }

        s = addUrlParam(s, COLUMNS, param.getColumns());
        s = addUrlParam(s, WHERE, param.getWhere(), true);
        return s;
    }

    public static String addUrlParam(String url, String key, Object value) {
        return addUrlParam(url, key, value, false);
    }

    public static String addUrlParam(String url, String key, Object value, boolean doEncode) {
        try {
            if (!StringUtils.isEmpty(value)) {
                if (!StringUtils.isEmpty(url)) {
                    url = url + "&";
                }
                value = doEncode ? URLEncoder.encode(value.toString(), "UTF-8") : value;
                key = StringUtils.isEmpty(key) ? "" : key + "=";
                url = url + key + value;
            }
        } catch (UnsupportedEncodingException e) {
        }
        return url;
    }

}
