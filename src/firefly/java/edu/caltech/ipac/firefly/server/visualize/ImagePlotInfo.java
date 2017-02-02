/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 3:56 PM
 */


import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;

import java.util.List;
import java.util.Map;

/**
* @author Trey Roby
*/
public class ImagePlotInfo {
    private final PlotState state;
    private final ImagePlot plot;
    private final ActiveFitsReadGroup frGroup;
    private final Map<Band,WebFitsData> wfDataMap;
    private final Map<Band,ModFileWriter> fileWriterMap;
    private final List<RelatedData> relatedData;
    private final String dataDesc;

    public ImagePlotInfo(PlotState state,
                         ImagePlot plot,
                         ActiveFitsReadGroup frGroup,
                         String    dataDesc,
                         List<RelatedData> relatedData,
                         Map<Band, WebFitsData> wfDataMap,
                         Map<Band, ModFileWriter> fileWriterMap) {
        this.state = state;
        this.plot = plot;
        this.frGroup = frGroup;
        this.wfDataMap = wfDataMap;
        this.relatedData= relatedData;
        this.fileWriterMap = fileWriterMap;
        this.dataDesc = dataDesc;
    }

    public PlotState getState() { return state; }
    public ImagePlot getPlot() { return plot; }
    public Map<Band, WebFitsData> getWebFitsDataMap() { return wfDataMap; }
    public Map<Band, ModFileWriter> getFileWriterMap() { return fileWriterMap; }
    public String getDataDesc() { return dataDesc; }
    public ActiveFitsReadGroup getFrGroup() { return frGroup; }
    public List<RelatedData> getRelatedData() {return relatedData;}
}

