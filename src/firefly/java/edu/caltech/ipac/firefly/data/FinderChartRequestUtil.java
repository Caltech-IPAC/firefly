/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 9/2/14
 * Time: 10:36 AM
 */


import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.HashMap;

/**
 * @author Trey Roby
 */
public class FinderChartRequestUtil {
    // --------  input field names and possible values
    public static final String FD_CAT_BY_BOUNDARY = "catalog_by_img_boundary";
    public static final String FD_CAT_BY_RADIUS = "catalog_by_radius";
    public static final String FD_ONE_TO_ONE = "one_to_one";
    public static final String FD_OVERLAY_CAT = "overlay_catalog";
    public static final String FD_SOURCES = "sources";
    public static final String FD_FILENAME = "filename";
    public static final String FD_SUBSIZE = "subsize";
    private static final String DEF = Character.toString('\0');
    /**
     * Finder Chart services
     * combo string format: option;title
     */
    private final static String dssCombo[]={
            "poss1_blue;DSS1 Blue",
            "poss1_red;DSS1 Red",
            "poss2ukstu_blue;DSS2 Blue",
            "poss2ukstu_red;DSS2 Red",
            "poss2ukstu_ir;DSS2 IR",
            /*"quickv;Quick-V Survey",
            "phase2_gsc2;HST Phase 2 Target Positioning(GSC 2)",
            "phase2_gsc1;HST Phase 1 Target Positioning(GSC 1)",
            "all;The best of a combined list of all plates"*/};
    private final static String twoMassCombo[] = {
            "j;J",
            "h;H",
            "k;K", };
    private final static String issaCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};
    private final static String irisCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};
    private final static String msxCombo[] = {
            "3;A (8.28 microns)",
            "4;C (12.13 microns)",
            "5;D (14.65 microns)",
            "6;E (21.3 microns)"};
    // --------
    private final static String wiseCombo[]={
            "3a.1;w1",
            "3a.2;w2",
            "3a.3;w3",
            "3a.4;w4"};


    /**
     * TODO For FC4, need to use config table and new image search, see IRSA-794 and IRSA-816
     * Add reader to parse IRSA configuration table and return a combo depending on the imageset
     * For FC3: SEIP + AKARI, we use fixed values for the combos below:
     *
     * SEIP metadata sent from client (or TODO read from master table)
     * thru request so it can be split into schema.table and band, and title.
     * THis is passed from the client
     * Expected format :  metadata expected to be as "schema.table-band;title"
     */
    private final static String seipCombo[]={
            "spitzer.seip_science:IRAC1;SEIP IRAC1 (2.4 microns);file_type='science' and fname like '%.mosaic.fits'",
            "spitzer.seip_science:IRAC2;SEIP IRAC2 (2.4 microns);file_type='science' and fname like '%.mosaic.fits'",
            "spitzer.seip_science:IRAC3;SEIP IRAC3 (2.4 microns);file_type='science' and fname like '%.mosaic.fits'",
            "spitzer.seip_science:IRAC4;SEIP IRAC4 (2.4 microns);file_type='science' and fname like '%.mosaic.fits'",
            "spitzer.seip_science:MIPS24;SEIP MIPS (2.4 microns);file_type='science' and fname like '%.mosaic.fits'"
    };

    /**
     * AKARI metadata sent from client thru request so it can be split into schema.table, band on one hand, label title on another hand, all separated by ';'.
     * THis is passed from the client
     * Expected format :  metadata expected to be as "schema.table-band;title"
     */
    private final static String akariCombo[]={
            "akari.akari_images:N60;FIS N60 (65 micron);file_type='science'",
            "akari.akari_images:WideS;FIS WideS (90 micron);file_type='science'",
            "akari.akari_images:WideL;FIS WideL (140 micron);file_type='science'",
            "akari.akari_images:N160;FIS N160 (160 micron);file_type='science'"
    };
    private final static String sDssCombo[]={
            "u;u","g;g","r;r","i;i","z;z"
    };
    private static HashMap<String, Integer> thumbnailSizeMap = new HashMap<String, Integer>() {
        {
            put("small",128);
            put("medium",192);
            put("large",256);
        }
    };
    private static String regExAtlasKey = ":";
    private static String regExSplitKey = ";";

    /**
     * @param pt
     * @param radius
     * @param width
     * @param key string combination of value to be splitted for convenience
     * @param expandedTitle
     * @param service
     * @return
     */
    public static WebPlotRequest makeWebPlotRequest(WorldPt pt,
                                                    float radius,
                                                    int width,
                                                    String key,
                                                    String expandedTitle,
                                                    WebPlotRequest.ServiceType service ) {

        WebPlotRequest wpReq= getWebPlotRequest(service, key, pt, radius);
        if (!StringUtils.isEmpty(expandedTitle)) wpReq.setExpandedTitle(expandedTitle);
        wpReq.setExpandedTitleOptions(WebPlotRequest.ExpandedTitleOptions.PREFIX);
        wpReq.setZoomType(ZoomType.TO_WIDTH);
        wpReq.setZoomToWidth(width);
        wpReq.setZoomToHeight(width); // set width and height the same
        wpReq.setPostCropAndCenter(true);
        wpReq.setRotateNorth(true);
        wpReq.setSaveCorners(true);
        wpReq.setInitialColorTable(1);
        wpReq.setHideTitleDetail(true);
        wpReq.setPreferenceColorKey("FcColorKey");
        wpReq.setTitleOptions(WebPlotRequest.TitleOptions.SERVICE_OBS_DATE);
        wpReq.setTitle(getComboTitle(key));
        return wpReq;
    }

    private static WebPlotRequest getWebPlotRequest(WebPlotRequest.ServiceType service, String key, WorldPt pt, Float radius) {
        WebPlotRequest wpReq=null;
        switch (service) {
            case DSS:
                wpReq= WebPlotRequest.makeDSSRequest(pt, getComboValue(key),radius);
                break;
            case IRIS:
                wpReq= WebPlotRequest.makeIRISRequest(pt, getComboValue(key), radius);
                break;
            case ISSA:
                wpReq= WebPlotRequest.makeISSARequest(pt, getComboValue(key),radius);
                break;
            case MSX:
                wpReq= WebPlotRequest.makeMSXRequest(pt, getComboValue(key),radius);
                break;
            case SDSS:
                wpReq= WebPlotRequest.makeSloanDSSRequest(pt, getComboValue(key), radius);
                break;
            case TWOMASS:
                wpReq= WebPlotRequest.make2MASSRequest(pt, getComboValue(key),radius);
                break;
            case AKARI:
            case SEIP:
            case ATLAS:
                String surveyKey = extractSurveyKey(getComboValue(key));
                String surveyKeyBand = extractSurveyKeyBand(getComboValue(key));
                String filter = extractFilter(key);
                wpReq = WebPlotRequest.makeAtlasRequest(pt, surveyKey, surveyKeyBand, filter, radius);
//                if (wpReq != null)
//                    wpReq.setDrawingSubGroupId(surveyKey.split("\\.")[1]);// Set dataset (table) name as subgroup
                break;
            case WISE:
                String[] pair= getComboValue(key).split("\\.");
                wpReq= WebPlotRequest.makeWiseRequest(pt, pair[0], pair[1], radius);
                break;
        }
        if (wpReq != null)
            wpReq.setDrawingSubGroupId(ImageSet.lookup(service).subgroup);
        return wpReq;
    }

    /**
     * Get ATLAS values from schema.table-band (out of the {@link #seipCombo} for example)
     *
     * @param metadata expected to be as "schema.table:band;title"
     * @return the surveyKey, i.e schema.table
     */
    public static String extractSurveyKey(String metadata) {
        String sAry[] = metadata.split(regExAtlasKey);
        return sAry[0];
    }

    /**
     * @param metadata key expected to be as "schema.table:band;title"
     * @return the surveyKeyBand, i.e irac1 for SEIP
     */
    public static String extractSurveyKeyBand(String metadata) {
        String sAry[] = metadata.split(regExAtlasKey);
        return sAry[1];
    }

    /**
     * @param metadata expected to be as "schema.table:band;title;filter"
     * @return filter value
     */
    public static String extractFilter(String metadata) {
        String sAry[] = metadata.split(regExSplitKey);
        return sAry.length > 1 ? sAry[2] : "";
    }

    /**
     * Value part is the left side of "datax;datay" combo
     *
     * @param combo string value expected to be of a form "datax;datay"
     * @return the first element of the array after splitting the combo into ";" pieces
     */
    public static String getComboValue(String combo) {
        String sAry[] = combo.split(regExSplitKey);
        return sAry.length > 0 ? sAry[0] : combo;
    }

    /**
     * Title part is the right side of "datax;datay" combo
     *
     * @param combo string value expected to be of the form  "datax;datay"
     * @return the second element of the array after splitting the combo into ";" pieces
     */
    public static String getComboTitle(String combo) {
        String sAry[] = combo.split(regExSplitKey);
        return sAry.length > 1 ? sAry[1] : combo;
    }

    public static int getPlotWidth(String sizeKey) {
        if (sizeKey!=null && thumbnailSizeMap.containsKey(sizeKey))  {
            return thumbnailSizeMap.get(sizeKey);
        }
        else {
            return thumbnailSizeMap.get("medium");
        }
    }




    public static enum ImageSet {DSS(WebPlotRequest.ServiceType.DSS, DEF, "dss", "dss_bands", dssCombo, null, DEF ),
                                 IRIS(WebPlotRequest.ServiceType.IRIS,"IRAS (IRIS)", "iris", "iras_bands", irisCombo, "iraspsc", "IRAS"),
                                 ISSA(WebPlotRequest.ServiceType.ISSA, DEF, "issa", null, issaCombo, null, DEF),
                                 MSX(WebPlotRequest.ServiceType.MSX, DEF, "msx", null, msxCombo, null, DEF),
                                 TWOMASS(WebPlotRequest.ServiceType.TWOMASS,"2MASS", "2mass","twomass_bands", twoMassCombo, "fp_psc", DEF),
                                 WISE(WebPlotRequest.ServiceType.WISE, "WISE (AllWISE)", "wise", "wise_bands", wiseCombo, "allwise_p3as_psd", DEF),
                                 SEIP(WebPlotRequest.ServiceType.ATLAS, "Spitzer SEIP","seip","seip_bands",seipCombo,"slphotdr4",DEF),
                                 AKARI(WebPlotRequest.ServiceType.ATLAS, "AKARI","akari","akari_bands",akariCombo,"slphotdr4",DEF),
                                 SDSS(WebPlotRequest.ServiceType.SDSS, "SDSS (DR7)", "sdss", "sdss_bands",sDssCombo, null, "SDSS (DR10)");

        public WebPlotRequest.ServiceType srvType;
        public String title;
        public String subgroup;
        public String band;
        public String[] comboAry;
        public String catalog;
        public String catalogTitle;

        ImageSet(WebPlotRequest.ServiceType serviceType, String title, String subgroup, String band, String[] comboAry, String catalog, String catalogTitle) {
            srvType = serviceType;
            this.title = title.equals(DEF) ? srvType.toString() : title;
            this.subgroup = subgroup;
            this.band = band;
            this.comboAry = comboAry;
            this.catalog = catalog;
            this.catalogTitle = catalogTitle.equals(DEF) ? this.title : catalogTitle;
        }

        public static ImageSet lookup(WebPlotRequest.ServiceType srvType) {
            return valueOf(srvType.name());
        }
        public static ImageSet match(String word) {
            for (ImageSet s: values()){
                if(word.toLowerCase().contains(s.name().toLowerCase())){
                    return s;
                }
            }
            return null;
        }
    }

    public static void main(String[] args) {
        String[] split = new String("schema.table").split("\\.");

        for (String s:split){
            System.out.println(s);
        }
    }

    public static enum Artifact {
        diff_spikes_3("WISE Diffraction Spikes (dots)", "Wise.Artifact.Spikes.level3.Selected"),
        halos("WISE Halos (squares)", "Wise.Artifact.halos.Selected"),
        ghost("WISE Optical Ghosts (diamonds)", "Wise.Artifact.ghost.Selected"),
        latents("WISE Latents (x's)", "Wise.Artifact.latents.Selected"),
        pers_arti("2MASS Persistence Artifacts (crosses)", "2Mass.Artifact.Pers.Selected"),
        glint_arti("2MASS Glints Artifacts (diamonds)", "2Mass.Artifact.Glints.Selected");

        public String desc;
        public String enablePref;

        Artifact(String desc, String enablePref) {
            this.desc = desc;
            this.enablePref = enablePref;
        }

        public static boolean isArtifacts(String desc) {
            for (Artifact art : Artifact.values()) {
                if (art.desc.equals(desc)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * TODO remove that if not needed, i couldn't find any reference but please double check before rmeoving it!
     * @deprecated
     */
    public static enum Source {DSS, IRIS, twomass, WISE, ATLAS, SDSS}
    /**
     * TODO remove that if not needed, i couldn't find any reference but please double check before rmeoving it!
     * @deprecated
     */
    public static enum Band {dss_bands, iras_bands, twomass_bands, wise_bands, spitzer_bands, SDSS_bands}
    /**
     * TODO remove that if not needed, i couldn't find any reference but please double check before rmeoving it!
     * @deprecated
     */
    public static enum Radius {iras_radius, twomass_radius, wise_radius, sdss_radius}

}

