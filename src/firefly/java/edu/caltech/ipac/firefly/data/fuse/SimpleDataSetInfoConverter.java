/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.fuse;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import edu.caltech.ipac.firefly.data.fuse.config.SelectedRowData;
import edu.caltech.ipac.firefly.data.fuse.provider.AbstractDataSetInfoConverter;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter.DataVisualizeMode.FITS;

/**
 * @author Trey Roby
 */
public class SimpleDataSetInfoConverter extends AbstractDataSetInfoConverter {

    private BaseImagePlotDefinition imDef= null;


    public SimpleDataSetInfoConverter() {
        super(Arrays.asList(FITS), new PlotData(new SimpleResolver(),false,false,true), "target");

        PlotData pd= getPlotData();

        pd.setTitle("simple", "Data Image");
    }

    public ImagePlotDefinition getImagePlotDefinition() {
        if (imDef==null) imDef= new SimplePlotDefinitionBase();
        return imDef;
    }

    private static List<String> makeOverlayList(String b) {
        return Arrays.asList("target");
    }



    private static class SimplePlotDefinitionBase extends BaseImagePlotDefinition {

        public SimplePlotDefinitionBase() {
            super("simple", Collections.<String>emptyList());
        }

        @Override
        public List<String> getAllBandOptions(String viewerID) {
            return Arrays.asList("simple");
        }

    }

    private static class SimpleResolver implements PlotData.Resolver {
        private SimpleResolver() {
        }

        public WebPlotRequest getRequestForID(String id, SelectedRowData selData, boolean useWithThreeColor) {
            WebPlotRequest r= null;
            TableData.Row<String>row= selData.getSelectedRow();
            String fileName= row.getValue("file");
            if (fileName!=null) {
                r= WebPlotRequest.makeFilePlotRequest(fileName);
            }
            else {
                String url= row.getValue("url");
                if (url!=null)  r= WebPlotRequest.makeURLPlotRequest(url);
            }
            if (r!=null) {
                r.setTitleOptions(WebPlotRequest.TitleOptions.FILE_NAME);
                r.setZoomType(ZoomType.FULL_SCREEN);
            }

            return r;
        }


        public List<String> getIDsForMode(PlotData.GroupMode mode, SelectedRowData selData) {
            return Arrays.asList("simple");
        }

        public List<String> get3ColorIDsForMode(SelectedRowData selData) {
            return null;
        }
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
