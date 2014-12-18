package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.util.StringUtils;

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

    public void setBaseFilesystemPath(String baseFilesystemPath) {
        this.baseFilesystemPath = baseFilesystemPath;
    }
}
