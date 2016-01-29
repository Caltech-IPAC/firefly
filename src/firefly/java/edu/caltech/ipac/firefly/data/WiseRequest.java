/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class WiseRequest extends TableServerRequest {

    public final static String WISE_PROCESSOR  = "WiseQuery";

    public final static String HOST            = "host";
    public final static String SERVICE         = "service";
    public final static String SCHEMA_GROUP    = "schemaGroup";
    public final static String SCHEMA          = "schema";
//    public final static String TABLE           = "table";
    public final static String FILENAME        = "filename";
    public final static String POS             = "POS";
    public final static String SIZE            = "SIZE";
    public final static String MCEN            = "mcen";
    public final static String INTERSECT       = "intersect";
    public final static String RA_DEC_J2000    = "RaDecJ2000";
    public final static String PRODUCT_LEVEL   = "ProductLevel";

    public final static String SCAN_GROUP      = "scangrp";
    public final static String SCAN_ID         = "scan_id";
    public final static String FRAME_NUM       = "frame_num";
    public final static String BAND            = "band";
    public final static String TYPE            = "type";
    public final static String COADD_ID        = "coadd_id";
    public final static String REF_BY          = "refby";
    public final static String OPT_LEVEL       = "optLevel";

    public final static String SOURCE_ID_PATTERN_1B = "[0-9]{5}[abcde][0-9]{3}-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A = "[0-9]{4}[pm][0-9]{3}_a[abc][1-9]{2}-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3O = "[0-9]{4}[pm][0-9]{3}_[^a].*-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PASS1 = "[0-9]{4}[pm][0-9]{3}_aa[1-9]{2}-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PASS2_4B = "[0-9]{4}[pm][0-9]{3}_ab4[1-9]-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PASS2_3B = "[0-9]{4}[pm][0-9]{3}_ab3[1-9]-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_ALLWISE = "[0-9]{4}[pm][0-9]{3}_ac5[1-9]-[0-9]{6}";

    //public final static String SOURCE_ID_PATTERN_3A_PASS2_2B = "[0-9]{4}[pm][0-9]{3}_ab2[1-9]-[0-9]{6}";


    // Image sets (public)
    public final static String PRELIM = "prelim";
    public final static String PRELIM_POSTCRYO = "prelim_postcryo";
    public final static String ALLWISE_MULTIBAND = "allwise-multiband";
    public final static String ALLSKY_4BAND = "allsky-4band";
    public final static String CRYO_3BAND = "cryo_3band";
    public final static String POSTCRYO = "postcryo";
    public final static String NEOWISER = "neowiser";
    public final static String MERGE = "merge";
    public final static String MERGE_INT = "merge_int";


    // Image sets (internal)
    public final static String PASS1 = "pass1";
    public final static String NEOWISER_PROV = "neowiser_prov";
    public final static String NEOWISER_YR1 = "neowiser_yr1";
    public final static String NEOWISER_YR2 = "neowiser_yr2";
    public final static String NEOWISER_YR3 = "neowiser_yr3";       //this is the on-going yr3 internal neowiser
    public final static String PASS2_4BAND = "pass2-4band";
    public final static String PASS2_3BAND = "pass2-3band";
    public final static String PASS2_2BAND = "pass2-2band";

    public static final String PUBLIC_RELEASE = "wise_public_release";

    private final static Map<String,String> IMAGE_SET_DESC = new HashMap<String,String>(){
        {
            put(PRELIM,"Preliminary Release");
            put(PRELIM_POSTCRYO,"Post-Cryo(Preliminary)");
            put(ALLWISE_MULTIBAND,"AllWISE (multi-band)");
            put(ALLSKY_4BAND,"All-Sky Release");
            put(CRYO_3BAND,"3-band Cryo");
            put(POSTCRYO,"2-band Post-Cryo");
            put(MERGE,"Merged All-Sky, 3-band Cryo, 2-band Post-Cryo and NEOWISE-R");
            put(MERGE_INT,"Internal MERGE");
            put(PASS1,"Pass 1");
            put(NEOWISER_PROV,"NEOWISER PROV");
            put(NEOWISER_YR1,"NEOWISER YR1 (2 Bands)");
            put(NEOWISER,"NEOWISER PUBLIC (2 Bands)");
            put(NEOWISER_YR2,"NEOWISER YR2 (2 Bands)");
            put(NEOWISER_YR3,"NEOWISER YR3 (2 Bands)");
            put(PASS2_4BAND,"Pass 2 (4 Bands)");
            put(PASS2_3BAND,"Pass 2 (3 Bands)");
            put(PASS2_2BAND, "Pass 2 (2 Bands)");
        }
    };


    // mappings of the dataset selected to the wise table and table source values.
    // table name is in the first entry, table source is in the 2nd
    private static HashMap<String, String[]> TABLE_MAP = new HashMap<String, String[]>(){
        {
            put(PRELIM+"|1b", new String[]{"p1bm_frm", "p1bs_psd"});
            put(PRELIM+"|3a", new String[]{"p3am_cdd", "p3as_psd"});
            put(PRELIM_POSTCRYO +"|1b",  new String[]{"p1bm_frm", "p1bs_psd"});
            put(ALLWISE_MULTIBAND+"|3a", new String[]{"p3am_cdd", "p3as_psd"}); // TODO: change for production, changed XW
            put(ALLSKY_4BAND+"|1b", new String[]{"4band_p1bm_frm", "4band_p1bs_psd"});
            put(ALLSKY_4BAND+"|3a", new String[]{"4band_p3am_cdd", "4band_p3as_psd"});
            put(CRYO_3BAND+"|1b",   new String[]{"3band_p1bm_frm", "p1bs_psd"});
            put(CRYO_3BAND+"|3a",   new String[]{"3band_p3am_cdd", "p3as_psd"});  // currently they are different: p1bm_frm and p3am_cdd
            put(POSTCRYO+"|1b",  new String[]{"2band_p1bm_frm", "2band_p1bs_psd"});
            put(MERGE+"|1b", new String[]{"merge_p1bm_frm", "merge_p1bs_psd"});
            put(MERGE+"|3a", new String[]{"merge_p3am_cdd", "merge_p3as_psd"});
            put(NEOWISER +"|1b", new String[]{"p1bm_frm", "p1bs_psd"});

            put(PASS1+"|1b", new String[]{"i1bm_frm", "i1bs_psd"});
            put(PASS1+"|3a", new String[]{"i3am_cdd", "i3as_psd"});
            put(PASS1+"|3o", new String[]{"i3om_cdd", "i3os_psd"});
            put(NEOWISER_PROV +"|1b", new String[]{"i1bm_frm", "i1bs_psd"});
            put(NEOWISER_YR1 +"|1b", new String[]{"yr1_p1bm_frm", "yr1_p1bs_psd"});
            put(NEOWISER_YR2 +"|1b", new String[]{"yr2_p1bm_frm", "yr2_p1bs_psd"});
            put(NEOWISER_YR3 +"|1b", new String[]{"yr3_i1bm_frm", "yr3_i1bs_psd"});  // TODO: check
            put(MERGE_INT+"|1b", new String[]{"merge_i1bm_frm", "merge_i1bs_psd"});
            put(MERGE_INT+"|3a", new String[]{"merge_p3am_cdd", "merge_p3as_psd"});
            put(PASS2_4BAND+"|1b", new String[]{"4band_i1bm_frm", "4band_i1bs_psd"});
            put(PASS2_4BAND+"|3a", new String[]{"4band_i3am_cdd", "4band_i3as_psd"});
            put(PASS2_3BAND+"|1b", new String[]{"3band_i1bm_frm", "3band_i1bs_psd"});
            put(PASS2_3BAND+"|3a", new String[]{"3band_i3am_cdd", "3band_i3as_psd"});
            put(PASS2_2BAND+"|1b",  new String[]{"2band_i1bm_frm", "2band_i1bs_psd"});
          //put(PASS2_2BAND+"|3a",  new String[]{"notknown", "notknown"});
        }
    };

    // Scan ID ranges from Roc Cutri:
    // prelim 00936a - 04125a
    // pass 1 all 00712a - 12514a
    // pass 2 4-bands 00712a - 07101a
    // pass 2 3-bands 07101b - 08744a
    // post-cryo 2-bands 08745a - 12514a
    private static HashMap<String, Integer[]> SCANID_MAP = new HashMap<String, Integer[]>(){
        {
            put(PRELIM, new Integer[]{936, 4125});
            put(PRELIM_POSTCRYO, new Integer[]{8745, 12514});
            put(ALLSKY_4BAND, new Integer[]{712, 7101});
            put(CRYO_3BAND, new Integer[]{7101, 8744});
            put(POSTCRYO, new Integer[]{8745, 12514});
            put(NEOWISER, new Integer[]{44212, 55289});

            put(PASS1, new Integer[]{712, 12514});
            put(PASS2_4BAND, new Integer[]{712, 7101});
            put(PASS2_3BAND, new Integer[]{7101, 8744});
            put(PASS2_2BAND, new Integer[]{8745, 12514});
            put(NEOWISER_PROV, new Integer[]{44212, 55289});
            put(NEOWISER_YR1, new Integer[]{44212, 55289});
            put(NEOWISER_YR2, new Integer[]{55290, 66418});
            put(NEOWISER_YR3, new Integer[]{66419, 999999}); // TODO: which scan is the first for yr3?
        }
    };

    // MOS Catalogs
    // It's easier to remember updating them if we keep all dataset dependent stuff here
    private final static Map<String,String> MOS_CATALOGS = new HashMap<String,String>(){
        {
            put(PRELIM,"wise_prelim");
            put(PRELIM_POSTCRYO,"wise_prelim_2band");
            put(ALLWISE_MULTIBAND,"wise_allwise");
            put(ALLSKY_4BAND,"wise_allsky_4band");
            put(CRYO_3BAND,"wise_allsky_3band");
            put(POSTCRYO,"wise_allsky_2band");
            put(MERGE,"wise_merge");
            put(MERGE_INT,"wise_merge_int");
            put(NEOWISER,"wise_neowiser");
            put(PASS1,"wise_pass1");
            put(PASS2_4BAND,"wise_pass2_4band");
            put(PASS2_3BAND,"wise_pass2_3band");
            put(PASS2_2BAND, "wise_pass2_2band");
            put(NEOWISER_YR1,"wise_neowiser_yr1");
            put(NEOWISER_YR2,"wise_neowiser_yr2");
            put(NEOWISER_YR3,"wise_neowiser_yr3");

        }
    };

    public final static Map<String,String> CATALOG_MAP = new HashMap<String,String>(){
        {
            put(PRELIM,"wise_prelim_p1bs_psd");
            put(PRELIM_POSTCRYO,"wise_prelim_2band_p1bs_psd");
            put(ALLWISE_MULTIBAND,"wise_allwise_p3as_psd");
            put(ALLSKY_4BAND,"wise_allsky_4band_p1bs_psd");
            put(CRYO_3BAND,"wise_allsky_3band_p1bs_psd");
            put(POSTCRYO,"wise_allsky_2band_p1bs_psd");
            put(NEOWISER,"neowiser_p1bs_psd");
            put(PASS1,"wise_pass1_i1bs_psd_view");
            put(PASS2_4BAND,"wise_pass2_4band_i1bs_psd_view");
            put(PASS2_3BAND,"wise_pass2_3band_i1bs_psd_view");
            put(PASS2_2BAND, "wise_pass2_2band_i1bs_psd_view");
            put(NEOWISER_YR1,"neowiser_yr1_p1bs_psd");
            put(NEOWISER_YR2,"neowiser_yr2_p1bs_psd");
            put(NEOWISER_YR3,"neowiser_i1bs_psd");
        }
    };

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public WiseRequest() {
        super(WISE_PROCESSOR);
    }

    public void setHost(String value) {
        setParam(HOST, value);
    }

    public String getHost() { return getParam(HOST); }


    public void setFilename(String value) {
        setParam(FILENAME, value);
    }

    public String getFilename() { return getParam(FILENAME); }


    public void setSchemaGroup(String value) {
        setParam(SCHEMA_GROUP, value);
    }

    public String getSchemaGroup() { return getParam(SCHEMA_GROUP); }


    public void setSchema(String value) {
        setParam(SCHEMA, value);
    }


    public String getSchema() {
        return getParam(SCHEMA);
    }


    /**
     * return the schema value sent to the backend service(ibe)
     * @return
     */
    public String getServiceSchema() {
        return getServiceSchema(getParam(SCHEMA));
    }

    public static String getServiceSchema(String imageSet) {
        String schema = imageSet;
        schema = schema.contains("-") ? schema.split("-")[0] : schema;
        return schema;
    }

    public static boolean useMergedTable(String imageSet) {
        // using merged table same for all ALLSKY_4BAND, CRYO_3BAND, and POSTCRYO and NEOWISER
        return imageSet.contains(",");
    }

    public static String getTableSchema(ServerRequest req, String imageSet) {
        if (useMergedTable(imageSet)) {
            return req.getBooleanParam(PUBLIC_RELEASE, true) ? MERGE : MERGE_INT;
        } else {
            String schema = imageSet;
            schema = schema.contains("-") ? schema.split("-")[0] : schema;
            return schema;
        }
    }


    public String getTable() {
        String imageSet = getParam(SCHEMA);
        if (useMergedTable(imageSet)) {
            imageSet = getBooleanParam(PUBLIC_RELEASE, true) ? MERGE : MERGE_INT;
        }
        String[] names = TABLE_MAP.get(imageSet + "|" + getParam("ProductLevel"));
        return names == null || names.length < 2 ? null : names[0];
    }


    public String getSourceTable() {
        String[] names = TABLE_MAP.get(getParam(SCHEMA) + "|" + getParam("ProductLevel"));
        return names == null || names.length < 2 ? null : names[1];
    }


    public String getSourceTable(String sourceProductLevel) {
        String[] names = TABLE_MAP.get(getParam(SCHEMA) + "|" + sourceProductLevel);
        return names == null || names.length < 2 ? null : names[1];
    }

    public String getSourceTable(String sourceProductLevel, String schema) {
        String[] names = TABLE_MAP.get(schema + "|" + sourceProductLevel);
        return names == null || names.length < 2 ? null : names[1];
    }


    public void setPos(String value) {
        setParam(POS, value);
    }

    public String getPos() { return getParam(POS); }


    public void setSize(String xValue) {
        setSize(xValue, null);
    }

    public void setSize(String xValue, String yValue) {
        setParam(SIZE, (yValue==null) ? xValue : xValue + "," + yValue);
    }

    public String getSize() { return getParam(SIZE); }


    public void setIntersect(String value) {
        setParam(INTERSECT, value);
    }

    public String getIntersect() { return getParam(INTERSECT); }


    public void setWorldPtJ2000(WorldPt pt) {
        assert pt.getCoordSys()== CoordinateSys.EQ_J2000;
        setParam(RA_DEC_J2000, pt.getLon() + " " + pt.getLat());
    }

    public WorldPt getWorldPtJ2000() {
        String ptStr= getParam(RA_DEC_J2000);
        String s[]= ptStr.split(" ");
        return new WorldPt(asDouble(s[0]), asDouble(s[1]));
    }

    public static String getMosCatalog(TableServerRequest req) {
        String imageSet = req.getParam(SCHEMA);
        if (useMergedTable(imageSet)) {
            imageSet = req.getBooleanParam(PUBLIC_RELEASE, true) ? MERGE : MERGE_INT;
        }
        return MOS_CATALOGS.get(imageSet);
    }

    /*
     * Return true, if imageSet was processed with pass1 data processing system
     * @param imageSet
     * @return true, if imageSet was obtained with pass1 processing
     */
    public static boolean isPass1ImageSet(String imageSet) {
        return !StringUtils.isEmpty(imageSet) && (imageSet.equals(PRELIM) || imageSet.equals(PRELIM_POSTCRYO) || imageSet.equals(PASS1));
    }

    /*
     * Given a source id, return the image set that has an image from which this source was extracted
     * For single exposure sources, use userImageSet, if known, as a hint:
     * return pass1 image set, if userImageSet is pass1, otherwise a pass2 image set.
     * Return null, if image set can not be derived unambiguously.
     * @param sourceId source id
     * @param publicRelease if true, use public image sets, otherwise internal
     * @param userImageSet, if not null, use it to choose between pass1 and pass2
     * @return the image set that has an image from which this source was extracted
     */
    public static String getImageSetFromSourceId(String sourceId, boolean publicRelease, String userImageSet) {
        if (StringUtils.isEmpty(sourceId)) {
            return null;
        }
        if (sourceId.matches(SOURCE_ID_PATTERN_3O)) {
            return publicRelease ? PRELIM : PASS1;
        } else if (sourceId.matches(SOURCE_ID_PATTERN_3A)) {
            if (sourceId.matches(SOURCE_ID_PATTERN_3A_PASS1)) {
                return publicRelease ? PRELIM : PASS1;
            } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_PASS2_4B))) {
                return publicRelease ? ALLSKY_4BAND : PASS2_4BAND;
            } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_PASS2_3B))) {
                return publicRelease ? CRYO_3BAND : PASS2_3BAND;
// TODO: uncomment when needed
//            } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_PASS2_2B))) {
//                return publicRelease ? PRELIM_POSTCRYO : PASS2_2BAND;
            } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_ALLWISE))) {
                return ALLWISE_MULTIBAND;
            } else {
                //assert(false);
                return null;
            }
        } else if (sourceId.matches(SOURCE_ID_PATTERN_1B)) {
            // first 6 characters (5 digits and a letter) are scan ID
            String[] possibleSets = getImageSetsForScanID(sourceId.substring(0,6), publicRelease);
            if (possibleSets.length == 1) {
                return possibleSets[0];
            } else if (possibleSets.length == 0) {
                return null;
            } else {
                if (!StringUtils.isEmpty(userImageSet)) {
                    if (isPass1ImageSet(userImageSet)||userImageSet.equals(NEOWISER_PROV)) {
                        return possibleSets[0];
                    } else {
                        return possibleSets[1];
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /*
     * Return product level (single exposure, level 3o or atlas 3a)
     * @param sourceId source ID
     * @return product level string
     */
    public static String getProductLevelFromSourceId(String sourceId) {
        if (StringUtils.isEmpty(sourceId)) {
            return null;
        }
        if (sourceId.matches(SOURCE_ID_PATTERN_1B)) {
            return "1b";
        } else if (sourceId.matches(SOURCE_ID_PATTERN_3A)) {
            return "3a";
        } else if (sourceId.matches(SOURCE_ID_PATTERN_3O)) {
            return "3o";
        } else {
            return null;
        }
    }

    /*
     * Get an array of possible bands for a given scan ID.
     * All 4 bands were taken during the full cryogenic mission phase, 7 January 2010 to 6 August 2010.
     * The 3-band data in the 3.4, 4.6 and 12 ?m WISE bands were taken during the "3-Band Cryo" mission
     * phase, 6 August 2010 to 29 September 2010, following the exhaustion of solid Hydrogen in
     * the WISE secondary cryogen tank.
     * The 2-band data in 3.4 and 4.6 ?m were taken during Postcryo phase, from October 1, 2011
     * to the end of mission.
     * @param scanID scan id
     * @return an array of bands taken during the given scan
     */
    public static int[] getAvailableBandsForScanID(String scanID, String imageSet) {
        try{
            int scanNum = Integer.parseInt(scanID.trim().substring(0,5));
            // pass1 single exposure data have the following pattern:
            // 4 bands are available for scans 00712a - 08764a
            // 2 bands are available for scans 08765a - 12514a
            if (!StringUtils.isEmpty(imageSet) && isPass1ImageSet(imageSet)) {
                if (scanNum < 8765) {
                    return new int[]{1,2,3,4};
                } else {
                    return new int[]{1,2};
                }
            // pass2 scan ranges are defined by SCANID_MAP    
            } else if (scanNum < SCANID_MAP.get(PASS2_4BAND)[1] ||
                    (scanNum==SCANID_MAP.get(PASS2_4BAND)[1] && scanID.trim().endsWith("a")) ) {
                return new int[]{1,2,3,4};
            } else if (scanNum <= SCANID_MAP.get(PASS2_3BAND)[1]) {
                return new int[]{1,2,3};
            } else {
                return new int[]{1,2};
            }
        } catch (Exception e) {
            return new int[]{1,2,3,4};
        }
    }

    /**
     * Returns possible image sets for the given scan ID.
     * When both pass1 and pass2 image sets are possible, pass 1 image set comes first.
     * @param scanIdStr scan id
     * @param publicRelease true if the release is public, false if the release is internal
     * @return an array of possible image sets
     */
    public static String[] getImageSetsForScanID(String scanIdStr, boolean publicRelease) {

        String scanID = scanIdStr.trim();
        if (StringUtils.isEmpty(scanID) || scanID.length()<6) {
            return new String[]{};
        }

        int scanNum = Integer.parseInt(scanID.substring(0,5));
        if (publicRelease) {
            if (scanNum < SCANID_MAP.get(PRELIM)[1]) {
                return new String[]{PRELIM,ALLSKY_4BAND};
            } else if (scanNum < SCANID_MAP.get(ALLSKY_4BAND)[1] ||
                    (scanNum==SCANID_MAP.get(ALLSKY_4BAND)[1] && scanID.trim().endsWith("a")) ) {
                return new String[]{ALLSKY_4BAND};
            } else if (scanNum <= SCANID_MAP.get(CRYO_3BAND)[1]) {
                return new String[]{CRYO_3BAND};
            } else if (scanNum >= SCANID_MAP.get(NEOWISER)[0] &&
                    scanNum <= SCANID_MAP.get(NEOWISER)[2]) {
                return new String[]{NEOWISER};
            } else {
                // these 2 have the same range..
                // getImageSetFromSourceId() will determine which one to select
                return new String[]{PRELIM_POSTCRYO, POSTCRYO};
            }

        } else {
            if (scanNum < SCANID_MAP.get(PASS2_4BAND)[1] ||
                    (scanNum == SCANID_MAP.get(PASS2_4BAND)[1] && scanID.trim().endsWith("a"))) {
                return new String[]{PASS1, PASS2_4BAND, ALLSKY_4BAND};
            } else if (scanNum <= SCANID_MAP.get(PASS2_3BAND)[1]) {
                return new String[]{PASS1, PASS2_3BAND, CRYO_3BAND};
            } else if (scanNum <= SCANID_MAP.get(PASS2_2BAND)[1]) {
                return new String[]{PASS1, PASS2_2BAND, POSTCRYO};
            } else if (scanNum >= SCANID_MAP.get(NEOWISER_YR2)[0] &&
                    scanNum <= SCANID_MAP.get(NEOWISER_YR2)[1]) {
                return new String[]{NEOWISER_YR2};
            } else if (scanNum >= SCANID_MAP.get(NEOWISER_YR3)[1] &&
                    scanNum <= SCANID_MAP.get(NEOWISER_YR3)[2]) {
                return new String[]{NEOWISER_YR3};
            }else {
                // these 2 have the same range..
                // getImageSetFromSourceId() will determine which one to select
                return new String[]{NEOWISER_PROV,NEOWISER_YR1};
            }
        }
    }

    /**
     * Get image set description
     * @param imageSet image set
     * @return short description
     */
    public static String getImageSetDescription(String imageSet) {
        return IMAGE_SET_DESC.get(imageSet);
    }

    private static double asDouble(String dStr) {
        double retval;
        try {
            retval= Double.parseDouble(dStr);
        } catch (NumberFormatException e) {
            retval= 0.0;
        }
        return retval;

    }
}

