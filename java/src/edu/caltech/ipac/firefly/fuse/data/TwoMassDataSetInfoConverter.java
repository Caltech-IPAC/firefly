package edu.caltech.ipac.firefly.fuse.data;
/**
 * User: roby
 * Date: 7/25/14
 * Time: 12:45 PM
 */


import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.fuse.data.provider.AbstractDataSetInfoConverter;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter.DataVisualizeMode.FITS;
import static edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter.DataVisualizeMode.FITS_3_COLOR;

/**
 * @author Trey Roby
 */
public class TwoMassDataSetInfoConverter extends AbstractDataSetInfoConverter {

    public enum ID {TWOMASS_J, TWOMASS_H, TWOMASS_K, }
    public static final String TWOMASS_3C= "TWOMASS_3C";
    private static final String bandStr[]= {"j", "h", "k"};

    private BaseImagePlotDefinition imDef= null;


    public TwoMassDataSetInfoConverter() {
        super(Arrays.asList(FITS, FITS_3_COLOR), new PlotData(new TMResolver(),true,false,true), "2mass_target");

        PlotData pd= getPlotData();

        pd.set3ColorIDOfIDs(TWOMASS_3C, Arrays.asList(ID.TWOMASS_J.name(),
                                                      ID.TWOMASS_H.name(),
                                                      ID.TWOMASS_K.name()));
        pd.setTitle(TWOMASS_3C, "2MASS 3 Color");
        pd.setTitle(ID.TWOMASS_J.name(), "2MASS J");
        pd.setTitle(ID.TWOMASS_H.name(), "2MASS H");
        pd.setTitle(ID.TWOMASS_K.name(), "2MASS K");
    }

    public ImagePlotDefinition getImagePlotDefinition() {
        if (imDef==null) {
            HashMap<String,List<String>> vToDMap= new HashMap<String,List<String>> (7);
            vToDMap.put(ID.TWOMASS_J.name(), makeOverlayList("J"));
            vToDMap.put(ID.TWOMASS_H.name(), makeOverlayList("H"));
            vToDMap.put(ID.TWOMASS_K.name(), makeOverlayList("K"));

            List<String> idList= Arrays.asList(
                    ID.TWOMASS_J.name(),
                    ID.TWOMASS_H.name(),
                    ID.TWOMASS_K.name());
            imDef= new TwoMassPlotDefinitionBase(3,idList, Arrays.asList(TWOMASS_3C), vToDMap);
        }
        return imDef;
    }

    private static List<String> makeOverlayList(String b) {
        return Arrays.asList("target");
    }



    private static class TwoMassPlotDefinitionBase extends BaseImagePlotDefinition {

        public TwoMassPlotDefinitionBase(int imageCount,
                                         List<String> viewerIDList,
                                         List<String> threeColorViewerIDList,
                                         Map<String, List<String>> viewerToDrawingLayerMap) {
            super(imageCount, viewerIDList, threeColorViewerIDList, viewerToDrawingLayerMap, AUTO_GRID_LAYOUT);
        }

        @Override
        public List<String> getAllBandOptions(String viewerID) {
            return Arrays.asList(
                    ID.TWOMASS_J.name(),
                    ID.TWOMASS_H.name(),
                    ID.TWOMASS_K.name());
        }

    }

    private static String getBandStr(ID id) {
        switch (id) {
            case TWOMASS_J:
                return "j";
            case TWOMASS_K:
                return "h";
            case TWOMASS_H:
                return "k";
        }
        return null;
    }

    private static class TMResolver implements PlotData.Resolver {
        private ServerRequestBuilder builder= new ServerRequestBuilder();
        static Map<String,ID> bandToID= new HashMap<String, ID>(5);
        private TMResolver() {
            builder.setColumnsToUse(Arrays.asList("filter", "scanno", "fname", "ordate", "hemisphere", "in_ra", "in_dec", "image_set"));
            builder.setHeaderParams(Arrays.asList("mission", "ds", "subsize"));
            builder.setColorTableID(1);
            builder.setRangeValues(new RangeValues(RangeValues.SIGMA, -2, RangeValues.SIGMA, 10, RangeValues.STRETCH_LINEAR));
            bandToID.put("j", ID.TWOMASS_J);
            bandToID.put("h", ID.TWOMASS_H);
            bandToID.put("k", ID.TWOMASS_K);
        }

        public WebPlotRequest getRequestForID(String id, SelectedRowData selData, boolean useWithThreeColor) {
            String inter= selData.getRequest().getParam("intersect");
            if (inter!=null && inter.equals("OVERLAPS")) {
                builder.setHeaderParams(Arrays.asList("mission", "ImageSet", "ProductLevel"));
            }
            else {
                builder.setHeaderParams(Arrays.asList("mission", "ImageSet", "ProductLevel", "subsize"));
            }

            String b= getBandStr(ID.valueOf(id));
            WebPlotRequest r = builder.makeServerRequest("ibe_file_retrieve", id, selData, Arrays.asList(new Param("band", b)));

            if (useWithThreeColor) r.setTitle("2MASS: 3 Color");
            else r.setTitle("2MASS: "+b);

            r.setZoomType(ZoomType.FULL_SCREEN);
            return r;
        }


        public List<String> getIDsForMode(PlotData.GroupMode mode, SelectedRowData selData) {
            String b= selData.getSelectedRow().getValue("filter");
            if (b!=null && Arrays.asList(bandStr).contains(b.toLowerCase())) {
                if (mode== PlotData.GroupMode.TABLE_ROW_ONLY) {
                    return Arrays.asList(bandToID.get(b).name());
                }
                else {
                    return Arrays.asList(ID.TWOMASS_J.name(), ID.TWOMASS_H.name(), ID.TWOMASS_K.name());
                }

            }
            return null;
        }

        public List<String> get3ColorIDsForMode(SelectedRowData selData) {
            return Arrays.asList(TWOMASS_3C);
        }
    }
}

