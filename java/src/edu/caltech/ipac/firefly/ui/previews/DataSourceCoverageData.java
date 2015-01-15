/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;

import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.List;


/**
 * User: roby
 * Date: Apr 13, 2010
 * Time: 11:17:40 AM
 */
public interface DataSourceCoverageData {


    public enum CoverageType {X, BOX}

    public String getTitle();
    public String setTitle(String title);
    public String getTip();
    public String getCoverageBaseTitle();
    public List<String> getEventWorkerList();
    public ZoomType getSmartZoomHint();
    public String getGroup();
    public boolean getEnableDetails();
    public boolean getUseBlankPlot();
}
