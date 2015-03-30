/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize;

/**
 * @author Trey Roby
 */
public class PlotCmdExtension {

    public enum ExtType { AREA_SELECT, LINE_SELECT, POINT, NONE }

    private final ExtType extType;
    private final String id;
    private final String imageUrl;
    private final String title;
    private final String toolTip;

    public PlotCmdExtension(String id, ExtType extType, String imageUrl, String title, String toolTip) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.toolTip = toolTip;
        this.extType= extType;
    }

    public String getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getTitle() { return title; }
    public String getToolTip() { return toolTip; }

    public ExtType getExtType() { return extType; }
}
