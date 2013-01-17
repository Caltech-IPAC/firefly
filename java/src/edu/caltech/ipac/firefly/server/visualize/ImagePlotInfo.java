package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 3:56 PM
 */


import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.visualize.plot.ImagePlot;

import java.util.Map;

/**
* @author Trey Roby
*/
public class ImagePlotInfo {
    private final PlotState state;
    private final ImagePlot plot;
    private final Map<Band,WebFitsData> wfDataMap;
    private final Map<Band,ModFileWriter> fileWriterMap;
    private final String dataDesc;

    public ImagePlotInfo(PlotState state,
                         ImagePlot plot,
                         String    dataDesc,
                         Map<Band, WebFitsData> wfDataMap,
                         Map<Band, ModFileWriter> fileWriterMap) {
        this.state = state;
        this.plot = plot;
        this.wfDataMap = wfDataMap;
        this.fileWriterMap = fileWriterMap;
        this.dataDesc = dataDesc;
    }

    public PlotState getState() { return state; }
    public ImagePlot getPlot() { return plot; }
    public Map<Band, WebFitsData> getWebFitsDataMap() { return wfDataMap; }
    public Map<Band, ModFileWriter> getFileWriterMap() { return fileWriterMap; }
    public String getDataDesc() { return dataDesc; }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
