/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

/**
 * User: roby
 * Date: 1/9/14
 * Time: 10:48 AM
 */
public interface Readout {

    public void setEnabled(boolean enabled);
    public void hideMouseReadout();
    public void addPlotView(WebPlotView pv);
    public void removePlotView(WebPlotView pv);
    public void setValue(int row, String labelText, String valueText);
    public void setValue(int row, String labelText, String valueText, boolean valueIsHtml);
    public void setValue(int row, String labelText, String valueText, String valueStyle);
    public void setValue(int row, String labelText, String valueText,
                         String valueStyle,
                         boolean valueIsHtml,
                         boolean setOnlyIfActive);
    public void setTitle(String valueText, boolean valueIsHtml);



}
