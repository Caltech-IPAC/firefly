package edu.caltech.ipac.firefly.fuse.data.provider;
/**
 * User: roby
 * Date: 8/20/14
 * Time: 1:32 PM
 */


import edu.caltech.ipac.firefly.fuse.data.BaseImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.PlotData;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class SpitzerDataSetConverter extends AbstractDataSetInfoConverter {

    public final static String SEIP= "seip";

    public SpitzerDataSetConverter() {
        super(Arrays.asList(DataVisualizeMode.FITS), new PlotData(new SResolver(),false,false,false), "target");
    }

    @Override
    public ImagePlotDefinition getImagePlotDefinition() {
        return new BaseImagePlotDefinition(SEIP, Arrays.asList("target"));
    }



    private static class SResolver implements PlotData.Resolver {
        public WebPlotRequest getRequestForID(String id, SelectedRowData selData, boolean useWithThreeColor) {
            String path= selData.getSelectedRow().getValue("fname");
            WebPlotRequest r= WebPlotRequest.makeURLPlotRequest("http://irsa.ipac.caltech.edu/data/SPITZER/Enhanced/SEIP/" + path, "SEIP");
            r.setTitle("Spitzer: SEIP");
            r.setZoomType(ZoomType.TO_WIDTH);
            if (useWithThreeColor) r.setTitle("3 Color");
            return r;
        }

        public List<String> getIDsForMode(PlotData.GroupMode mode, SelectedRowData selData) {
            return Arrays.asList(SEIP);
        }

        public List<String> get3ColorIDsForMode(SelectedRowData selData) { return null; }
    }


}

