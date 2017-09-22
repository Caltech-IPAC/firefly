/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe;

import java.util.Map;

/**
 * Date: 4/17/14
 *
 * This class is a mission specific implementation.
 * It is used to translate mission's specific data into IBE's API.
 *
 * @author loi
 * @version $Id: $
 */
public interface IbeDataSource {

    String getIbeHost();
    String getMission();
    String getDataset();
    String getTableName();

    // use the given information to initialize this data source.
    public void initialize(Map<String, String> sourceInfo);

    /** given the pathInfo map, return the relative path to the file **/
    IbeDataParam makeDataParam(Map<String, String> pathInfo);

    IbeQueryParam makeQueryParam(Map<String, String> queryInfo);

    // Direct filesystem access info
    boolean useFileSystem();
    String getBaseFilesystemPath();


    String getDataUrl(IbeDataParam param);

    String getMetaDataUrl();

    String getQueryUrl(IbeQueryParam param);

    Map<String,String> getMulipleQueryParam(IbeQueryParam param);

    String getSearchUrl();

    String getCorners();

    String getCenterCols();

    String[] getColsToHide();
}
