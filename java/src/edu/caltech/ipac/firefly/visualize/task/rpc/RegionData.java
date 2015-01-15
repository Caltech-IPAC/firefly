/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;
/**
 * User: roby
 * Date: 2/15/13
 * Time: 11:08 AM
 */


/**
 * @author Trey Roby
 */
public class RegionData {
    private final String title;
    private final String regionTextData;
    private final String regionParseErrors;

    public RegionData(String title, String regionTextData, String regionParseErrors) {
        this.regionTextData = regionTextData;
        this.regionParseErrors = regionParseErrors;
        this.title = title;
    }

    public String getRegionTextData() { return regionTextData; }
    public String getRegionParseErrors() { return regionParseErrors; }
    public String getTitle() { return title; }
}

