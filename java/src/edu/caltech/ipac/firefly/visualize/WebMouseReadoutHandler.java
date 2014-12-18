package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.List;


/**
 * User: roby
 * Date: May 20, 2008
 */
public interface WebMouseReadoutHandler {
    public int getRows(WebPlot plot);

    public void computeMouseValue(WebPlot plot,
                                    Readout readout,
                                    int row,
                                    ImagePt ipt,
                                    ScreenPt screenPt,
                                    long callID);
    public void computeMouseExitValue(WebPlot plot, Readout readout, int row);

    public List<Integer> getRowsWithOptions();
    public List<String> getRowOptions(int row);
    public void setRowOption(int row, String op);
    public String getRowOption(int row);
}

