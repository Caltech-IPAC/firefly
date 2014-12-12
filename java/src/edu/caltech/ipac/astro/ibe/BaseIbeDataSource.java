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
