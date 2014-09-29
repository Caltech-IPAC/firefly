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
        public WebPlotRequest getRequestForID(String id, SelectedRowData selData) {
            String path= selData.getSelectedRow().getValue("fname");
            WebPlotRequest r= WebPlotRequest.makeURLPlotRequest("http://irsa.ipac.caltech.edu/data/SPITZER/Enhanced/SEIP/" + path, "SEIP");
            r.setTitle("Spitzer: SEIP");
            r.setZoomType(ZoomType.TO_WIDTH);
            return r;
        }

        public List<String> getIDsForMode(PlotData.GroupMode mode, SelectedRowData selData) {
            return Arrays.asList(SEIP);
        }

        public List<String> get3ColorIDsForMode(SelectedRowData selData) { return null; }
    }


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
