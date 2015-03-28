/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.data.fuse;



import edu.caltech.ipac.firefly.data.fuse.config.SelectedRowData;
import edu.caltech.ipac.firefly.data.fuse.provider.AbstractDataSetInfoConverter;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter.DataVisualizeMode.FITS;

/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */

/**
 * @author Trey Roby
 */
public class LsstCCDDataSetInfoConverter extends AbstractDataSetInfoConverter {

    public enum ID {A0, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15 }
    private static final String ampStr[]= {
        "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12", "A13", "A14", "A15"
    };


    private static List<String> idList= Arrays.asList(
            ID.A0.name(), ID.A1.name(), ID.A2.name(), ID.A3.name(),
            ID.A4.name(), ID.A5.name(), ID.A6.name(), ID.A7.name(),
            ID.A8.name(), ID.A9.name(), ID.A10.name(), ID.A11.name(),
            ID.A12.name(), ID.A13.name(),  ID.A14.name(), ID.A15.name()
    );


    private BaseImagePlotDefinition imDef= null;


    public LsstCCDDataSetInfoConverter() {
        super(Arrays.asList(FITS), new PlotData(new CCDResolver(),false,false,false), null);

        PlotData pd= getPlotData();

    }

    public ImagePlotDefinition getImagePlotDefinition() {
        if (imDef==null) {
            HashMap<String,List<String>> vToDMap= new HashMap<String,List<String>> (7);
        }
        return imDef;
    }


    private static class LsstCcdPlotDefinitionBase extends BaseImagePlotDefinition {

        public LsstCcdPlotDefinitionBase(int imageCount,
                                         List<String> viewerIDList,
                                         List<String> threeColorViewerIDList,
                                         Map<String, List<String>> viewerToDrawingLayerMap) {
            super(imageCount, viewerIDList, threeColorViewerIDList, viewerToDrawingLayerMap, AUTO_GRID_LAYOUT);
        }

        @Override
        public List<String> getAllBandOptions(String viewerID) {
            return idList;
        }

    }

    private static String getBandStr(ID id) {
        return id.name();
    }

    private static class CCDResolver implements PlotData.Resolver {
        static Map<String,ID> bandToID= new HashMap<String, ID>(5);
        private CCDResolver() {
        }

        public WebPlotRequest getRequestForID(String id, SelectedRowData selData, boolean useWithThreeColor) {
            WebPlotRequest r= new WebPlotRequest();
            r.setRequestType(RequestType.TRY_FILE_THEN_URL);
            r.setZoomType(ZoomType.TO_WIDTH);
            r.setInitialRangeValues(new RangeValues(RangeValues.PERCENTAGE,0,RangeValues.PERCENTAGE,100,RangeValues.STRETCH_LINEAR));
            r.setMultiImageIdx(convertToIdx(StringUtils.getEnum(id,ID.A0)));
            return r;
        }


        public List<String> getIDsForMode(PlotData.GroupMode mode, SelectedRowData selData) {
            return Arrays.asList(ampStr);
        }

        public List<String> get3ColorIDsForMode(SelectedRowData selData) {
            return null;
        }

        private static int convertToIdx(ID id) {
            switch (id) {
                case A0: return 0;
                case A1: return 1;
                case A2: return 2;
                case A3: return 3;
                case A4: return 4;
                case A5: return 5;
                case A6: return 6;
                case A7: return 7;
                case A8: return 8;
                case A9: return 9;
                case A10: return 10;
                case A11: return 11;
                case A12: return 12;
                case A13: return 13;
                case A14: return 14;
                case A15: return 15;
                default: return 0;
            }
        }
    }
}

