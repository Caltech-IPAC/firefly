/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe.datasource;

import edu.caltech.ipac.astro.ibe.BaseIbeDataSource;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeQueryParam;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Date: 4/18/14
 *
 * @author loi
 * @version $Id: $
 */
public class WiseIbeDataSource extends BaseIbeDataSource {
    public static final String WISE = "wise";
    public static final String FTYPE = "type";

    public final static String SOURCE_ID_PATTERN_1B = "[0-9]{5}[abcderstuvw][0-9]{3}-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A = "[0-9]{4}[pm][0-9]{3}_a[abc][1-9]{2}-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3O = "[0-9]{4}[pm][0-9]{3}_[^a].*-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PASS1 = "[0-9]{4}[pm][0-9]{3}_aa[1-9]{2}-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PASS2_4B = "[0-9]{4}[pm][0-9]{3}_ab4[1-9]-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PASS2_3B = "[0-9]{4}[pm][0-9]{3}_ab3[1-9]-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_ALLWISE = "[0-9]{4}[pm][0-9]{3}_ac5[1-9]-[0-9]{6}";
    public final static String SOURCE_ID_PATTERN_3A_PRELIM = "[0-9]{4}[pm][0-9]{3}_aa[1-9]{2}-[0-9]{6}";

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
    public static enum DATA_TYPE {
        INTENSITY, MASK, UNCERTAINTY, COVERAGE, DIFF_SPIKES, HALOS, OPT_GHOSTS, LATENTS
    }

    private static String getSchemaFromSourceId(String sourceId) {
            if (StringUtils.isEmpty(sourceId)) {
                return null;
            }
            if (sourceId.matches(SOURCE_ID_PATTERN_3O)) {
                return PRELIM;
            } else if (sourceId.matches(SOURCE_ID_PATTERN_3A)) {
                if (sourceId.matches(SOURCE_ID_PATTERN_3A_PASS1)) {
                    return PRELIM;
                } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_PASS2_4B))) {
                    return ALLSKY_4BAND;
                } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_PASS2_3B))) {
                    return CRYO_3BAND;
    // TODO: uncomment when needed
    //            } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_PASS2_2B))) {
    //                return publicRelease ? PRELIM_POSTCRYO : PASS2_2BAND;
                } else if ((sourceId.matches(SOURCE_ID_PATTERN_3A_ALLWISE))) {
                    return ALLWISE_MULTIBAND;
                } else {
                    //assert(false);
                    return null;
                }
            } else {
                return null;
            }
        }

    private DataProduct wds;
    private String mergeImageSet;

    public WiseIbeDataSource() {}

    public WiseIbeDataSource(DataProduct ds) {
        this(null, ds);
    }

    public WiseIbeDataSource(String ibeHost, DataProduct ds) {
        setupDS(ibeHost, null, ds);
    }

//====================================================================
//  WISE implementation of IBE services
//====================================================================

    /**
     * use the dsInfo to define this datasource.  all values in DataProduct must be populated.
     * @param dsInfo data set information
     */
    @Override
    public void initialize(Map<String, String> dsInfo) {
        String host = dsInfo.get("host");
        String baseFsPath = AppProperties.getProperty("wise.filesystem_basepath");
        String mergeSchemaFromProp = AppProperties.getBooleanProperty("ibe.public_release", true) ? WiseRequest.MERGE : WiseRequest.MERGE_INT;

        String imageset = dsInfo.get("ImageSet");
        if (StringUtils.isEmpty(imageset)) {
            // hydra uses schema while fuse uses ImageSet.
            imageset = dsInfo.get("schema");
            if(StringUtils.isEmpty(imageset)) {
                imageset = mergeSchemaFromProp;//WiseRequest.getTrueSchema(request.getSearchRequest());
            }
        }

        String productLevel = dsInfo.get("ProductLevel");

        List<String> imageSets = Arrays.asList(imageset.split(","));

        if (StringUtils.isEmpty(productLevel)) {
            String sourceId = dsInfo.get("sourceId");
            if (!StringUtils.isEmpty(sourceId)) {
                productLevel = getProductLevelFromSourceId(sourceId);
                if (productLevel == null) {
                    throw new IllegalArgumentException("Invalid Source ID: " + sourceId);
                }
            }
            String refsourceId = dsInfo.get("refSourceId");
            if (!StringUtils.isEmpty(refsourceId)) {
                productLevel = getProductLevelFromSourceId(refsourceId);
                if (productLevel == null) {
                    throw new IllegalArgumentException("Invalid Source ID: " + refsourceId);
                } else if (productLevel == "3a" && imageset == "prelim_postcryo") {
                    throw new IllegalArgumentException("There is no coadd (level 3) for Prelim Post-Cryo image set, try a different search for your source ID: " + refsourceId);
                }
            }
        }

        String ds = imageset.replaceAll("-", "_").toUpperCase();
        String dt = productLevel.toUpperCase();
        if (imageset.contains(",")) {
            ds = mergeSchemaFromProp.toUpperCase();
            mergeImageSet = imageset;
        }

        DataProduct dsource = DataProduct.valueOf(ds + "_" + dt);
        setupDS(host, baseFsPath, dsource);
    }

    @Override
    public IbeDataParam makeDataParam(Map<String, String> pathInfo) {
        IbeDataParam dataParam = new IbeDataParam();
        DATA_TYPE dataType = DATA_TYPE.INTENSITY;

        String ftype = pathInfo.get(FTYPE);
        if (!StringUtils.isEmpty(ftype)) {
            if (ftype.equalsIgnoreCase("M")) {
                dataType =  DATA_TYPE.MASK;
            } else if (ftype.equalsIgnoreCase("C")) {
                dataType =  DATA_TYPE.COVERAGE;
            } else if (ftype.equalsIgnoreCase("U")) {
                dataType =  DATA_TYPE.UNCERTAINTY;
            } else if (ftype.equalsIgnoreCase("D")) {
                dataType =  DATA_TYPE.DIFF_SPIKES;
            } else if (ftype.equalsIgnoreCase("H")) {
                dataType = DATA_TYPE.HALOS;
            } else if (ftype.equalsIgnoreCase("O")) {
                dataType = DATA_TYPE.OPT_GHOSTS;
            } else if (ftype.equalsIgnoreCase("P")) {
                dataType = DATA_TYPE.LATENTS;
            } else {
                dataType = DATA_TYPE.valueOf(ftype);
            }
        }
        String productLevel = wds.plevel();
        if (productLevel.equalsIgnoreCase("1B")) {
            String scanId = pathInfo.get("scan_id");
            String frameNum = pathInfo.get("frame_num");
            String band = pathInfo.get("band");

            dataParam.setFilePath(make_l1_filepath(scanId, frameNum, band, dataType));

        } else if (productLevel.equalsIgnoreCase("3O") || productLevel.equalsIgnoreCase("3A")) {
            String coaddId = pathInfo.get("coadd_id");
            String band = pathInfo.get("band");

            dataParam.setFilePath(make_l3_filepath(coaddId, band, dataType));
        }

        // check cutout params
        // look for ra_obj first - moving object search
        String subLon = pathInfo.get("ra_obj");
        if (StringUtils.isEmpty(subLon)) {
            // next look for in_ra (IBE returns this)
            subLon = pathInfo.get("in_ra");
            if (StringUtils.isEmpty(subLon)) {
                // all else fails, try using crval1
                subLon = pathInfo.get("crval1");
            }
        }

        // look for dec_obj first - moving object search
        String subLat = pathInfo.get("dec_obj");
        if (StringUtils.isEmpty(subLat)) {
            // next look for in_dec (IBE returns this)
            subLat = pathInfo.get("in_dec");
            if (StringUtils.isEmpty(subLat)) {
                // all else fails, try using crval2
                subLat = pathInfo.get("crval2");
            }
        }
        String subSize = pathInfo.get("subsize");

        // if no subSize specified by searches, set doCutout flag false
        if (StringUtils.isEmpty(subSize)) {
            dataParam.setCutout(false,null,null);
        } else if (subSize.equals("null")) {
           dataParam.setCutout(false,null,null);
        } else if (subSize != "null" && !StringUtils.isEmpty(subLon) && !StringUtils.isEmpty(subLat)){
            dataParam.setCutout(true, subLon + "," + subLat, subSize);
        }

        if (dataParam.getFilePath() != null && dataParam.getFilePath().endsWith(".gz")) {
            dataParam.setDoZip(true);
        }

        return dataParam;
    }

    @Override
    public IbeQueryParam makeQueryParam(Map<String, String> queryInfo) {

        // common position search params
        IbeQueryParam queryParam = super.makeQueryParam(queryInfo);

        String refSourceId = queryInfo.get("refSourceId");
        String sourceId = queryInfo.get("sourceId");
        String schema = queryInfo.get("schema");
        
        if (sourceId != null) {
            String sourceProductLevel = getProductLevelFromSourceId(sourceId);
            if (sourceProductLevel == null) {
                throw new IllegalArgumentException("Invalid Source ID: " + sourceId);
            }
            if ( sourceProductLevel == "1b") {
                String sourceSpec = WISE + "." + wds.getSourceTable() + "(\"source_id\":\"" + sourceId + "\")";
                queryParam.setPos(sourceSpec);
            }
            else if (sourceProductLevel == "3a") {
                String sourceSchema = getSchemaFromSourceId(sourceId);
                String ss = sourceSchema.replaceAll("-", "_").toUpperCase();
                String tt = sourceProductLevel.toUpperCase();
                DataProduct sourcedt = DataProduct.valueOf(ss + "_" + tt);
                String sourceTable = sourcedt.getSourceTable();
                String sourceSpec = WISE + "." + sourceTable + "(\"source_id\":\"" + sourceId + "\")";
                queryParam.setPos(sourceSpec);
            }
        }
        if (refSourceId != null) {
            String sourceProductLevel = getProductLevelFromSourceId(refSourceId);
            if (sourceProductLevel == null) {
                throw new IllegalArgumentException("Invalid Source ID: " + refSourceId);
            }
            if (sourceProductLevel == "1b") {
                String sourceSpec = WISE + "." + wds.getSourceTable() + "(\"source_id\":\"" + refSourceId + "\")";
                //queryParam.setRefBy(sourceSpec);  commented out not using RefBy search,
                // getting the images with the sourceId identified scanId and frame_num only
                String scanId = refSourceId.trim().substring(0,6);
                String framNum = String.valueOf(Integer.parseInt(refSourceId.trim().substring(6,9)));
                String ref1B =  "scan_id=" + "\'"+ scanId + "\'"+" AND " + "frame_num="+"\'"+framNum+"\'";
                queryParam.setWhere(ref1B);
            } else if (sourceProductLevel == "3a") {
                String coaddId = refSourceId.trim().substring(0,13);
                String sourceSchema = getSchemaFromSourceId(refSourceId);
                String ss = sourceSchema.replaceAll("-", "_").toUpperCase();
                String tt = sourceProductLevel.toUpperCase();
                DataProduct sourcedt = DataProduct.valueOf(ss + "_" + tt);
                String sourceTable = sourcedt.getSourceTable();
                String sourceSpec = WISE + "." + sourceTable + "(\"source_id\":\"" + refSourceId + "\")";
                String ref3A = "coadd_id="+"\'"+coaddId+"\'";  //get the images with the coadd_id given by the source_id
                queryParam.setWhere(ref3A);
            }
        }

        // process constraints
        String constraints = processConstraints(queryInfo);
        if (!StringUtils.isEmpty(constraints)) {
            queryParam.setWhere(constraints);
        }
        return queryParam;
    }

    public DataProduct getDataProduct() { return wds; }

    /**
     * assemble the relative level 1 filepath from the given parameters
     * @param scanId  scan id
     * @param frameNum  frame number
     * @param band  wise band
     * @param type  data type (intensity, mask, uncertainty, etc.
     * @return returns null if type is not recognized
     */
    public static String make_l1_filepath(String scanId, String frameNum, String band, DATA_TYPE type) {

        frameNum = StringUtils.pad(3, frameNum, StringUtils.Align.RIGHT, '0');
        String path = scanId.substring(scanId.length() - 2) + "/" + scanId + "/" + frameNum + "/" + scanId + frameNum;

        if (type == DATA_TYPE.INTENSITY) {
            path += "-w" + band + "-int-1b.fits";
        } else if (type == DATA_TYPE.MASK) {
            path += "-w" + band + "-msk-1b.fits.gz";
        } else if (type == DATA_TYPE.UNCERTAINTY) {
            path += "-w" + band + "-unc-1b.fits.gz";
        } else if (type == DATA_TYPE.DIFF_SPIKES) {
            path += "-art-w" + band + "-D.tbl";
        } else if (type == DATA_TYPE.HALOS) {
            path += "-art-w" + band + "-H.tbl";
        } else if (type == DATA_TYPE.OPT_GHOSTS) {
            path += "-art-w" + band + "-O.tbl";
        } else if (type == DATA_TYPE.LATENTS) {
            path += "-art-w" + band + "-P.tbl";
        } else {
            // not a valid data type
            return null;
        }

        return path;
    }

    /**
     * assemble the relative level 3 filepath from the given parameters
     * @param coaddId  coadd id
     * @param band  wise bands
     * @param type intensity, mask, uncertainty, etc. (returns null if type is not recognized)
     * @return  file path string
     */
    public static String make_l3_filepath(String coaddId, String band, DATA_TYPE type) {

        String filepath = coaddId.substring(0, 2) + "/" + coaddId.substring(0, 4) + "/" + coaddId + "/";

        if (type == DATA_TYPE.INTENSITY) {
            filepath += coaddId + "-w" + band + "-int-3.fits";
        } else if (type == DATA_TYPE.MASK) {
            filepath += coaddId + "-w" + band + "-msk-3.fits.gz";
        } else if (type == DATA_TYPE.COVERAGE) {
            filepath += coaddId + "-w" + band + "-cov-3.fits.gz";
        } else if (type == DATA_TYPE.UNCERTAINTY) {
            filepath += coaddId + "-w" + band + "-unc-3.fits.gz";
        } else if (type == DATA_TYPE.DIFF_SPIKES) {
            filepath += coaddId + "-art-w" + band + "-D.tbl";
        } else if (type == DATA_TYPE.HALOS) {
            filepath += coaddId + "-art-w" + band + "-H.tbl";
        } else if (type == DATA_TYPE.OPT_GHOSTS) {
            filepath += coaddId + "-art-w" + band + "-O.tbl";
        } else if (type == DATA_TYPE.LATENTS) {
            filepath += coaddId + "-art-w" + band + "-P.tbl";
        } else {
            // not a valid data type
            return null;
        }

        return filepath;
    }


//====================================================================
//  supporting internal logic..
//====================================================================

    private void setupDS(String ibeHost, String baseFsPath, DataProduct ds) {

        if (StringUtils.isEmpty(ibeHost)) {
            ibeHost = AppProperties.getProperty("wise.ibe.host", "https://irsa.ipac.caltech.edu/ibe");
        }

        setIbeHost(ibeHost);
        setMission(WISE);
        if (ds != null) {
            wds = ds;
            setDataset(wds.getDataset());
            setTableName(wds.getImageTable());

//      following code was commented out to force url file retrieval
//            File dir= (baseFsPath!=null) ? new File(baseFsPath) : null;
//
//            if (baseFsPath != null && dir.canRead()) {
//                setUseFileSystem(true);
//                setBaseFilesystemPath(baseFsPath + "/" + wds.getFilesysDatasetPath());
//            }
        }
    }

    private String processConstraints(Map<String, String> queryInfo) {
        // create constraint array
        ArrayList<String> constraints = new ArrayList<String>();
        String constrStr = "";


        if (!StringUtils.isEmpty(mergeImageSet)) {
            String imageSets[] = mergeImageSet.split(",");
            int n = 0;
            String imageSetConstraint = "image_set";
            if (imageSets.length > 1) {
                imageSetConstraint += " IN (";
            } else {
                imageSetConstraint += "=";
            }
            if (mergeImageSet.contains(DataProduct.ALLWISE_MULTIBAND_3A.dataset)) {
                imageSetConstraint += "5";
                n++;
            }
            if (mergeImageSet.contains(DataProduct.ALLSKY_4BAND_3A.dataset)) {
                if (n>0) imageSetConstraint += ",4";
                else imageSetConstraint += "4";
                n++;
            }
            if (mergeImageSet.contains(DataProduct.CRYO_3BAND_1B.dataset)) {
                if (n>0) imageSetConstraint += ",3";
                else imageSetConstraint += "3";
                n++;
            }
            if (mergeImageSet.contains(DataProduct.POSTCRYO_1B.dataset)) {
                if (n>0) imageSetConstraint += ",2";
                else imageSetConstraint += "2";
                n++;
            }

            if (mergeImageSet.contains(DataProduct.NEOWISER_1B.dataset)) {
                if (n>0) imageSetConstraint += ",6,7,8,9,10,11,12,13,14,15,16";
                else imageSetConstraint += ",6,7,8,9,10,11,12,13,14,15,16";
                n++;
            }

            if (imageSets.length > 1) {
                imageSetConstraint += ")";
            }
            if (n>0) {
                constraints.add(imageSetConstraint);
            }
        }

        // process L1b only constraints
        String productLevel = wds.plevel();
        if (productLevel.equalsIgnoreCase("1B")) {
            // process DATE RANGE
            String timeStart = queryInfo.get("timeStart");
            if (!StringUtils.isEmpty(timeStart)) {
                //constraints.add("mjd_obs>='" + IBE.convertUnixToMJD(timeStart) + "'");
                constraints.add("date_obs>='" + timeStart + "'");
            }
            String timeEnd = queryInfo.get("timeEnd");
            if (!StringUtils.isEmpty(timeEnd)) {
                //constraints.add("mjd_obs<='" + IBE.convertUnixToMJD(timeEnd) + "'");
                constraints.add("date_obs<='" + timeEnd + "'");
            }

            // process Scan IDs (support multiple IDs)
            String scanIds = queryInfo.get("scanId");
            if (!StringUtils.isEmpty(scanIds)) {
                String[] scanArray = scanIds.split("[,; ]+");
                String scanConstraint = "scan_id";
                if (scanArray.length == 1) {
                    scanConstraint += "='" + scanArray[0] + "'";
                } else {
                    scanConstraint += " IN (";
                    int cnt = 0;
                    for (String scanId : scanArray) {
                        if (StringUtils.isEmpty(scanId)) {
                            continue;
                        }

                        if (cnt > 0) {
                            scanConstraint += ",";
                        }
                        scanConstraint += "'" + scanId + "'";
                        cnt++;
                    }

                    scanConstraint += ")";
                }

                constraints.add(scanConstraint);
            }

            // process FRAME Numbers
            String frameOp = queryInfo.get("frameOp");
            if (!StringUtils.isEmpty(frameOp)) {
                String frameConstraint = "frame_num";
                if (frameOp.equals("=")) {
                    String frameVal1 = queryInfo.get("frameVal1");
                    if (!StringUtils.isEmpty(frameVal1)) {
                        frameConstraint += "=" + frameVal1.trim();
                    }

                } else if (frameOp.equals(">")) {
                    String frameVal1 = queryInfo.get("frameVal1");
                    if (!StringUtils.isEmpty(frameVal1)) {
                        frameConstraint += ">" + frameVal1.trim();
                    }

                } else if (frameOp.equals("<")) {
                    String frameVal1 = queryInfo.get("frameVal1");
                    if (!StringUtils.isEmpty(frameVal1)) {
                        frameConstraint += "<" + frameVal1.trim();
                    }

                } else if (frameOp.equals("IN")) {
                    String frameVal3 = queryInfo.get("frameVal3");
                    if (!StringUtils.isEmpty(frameVal3)) {
                        String[] frameArray = frameVal3.split("[,; ]+");
                        if (frameArray.length == 1) {
                            frameConstraint += "=" + frameArray[0];
                        } else {
                            frameConstraint += " IN (";
                            int cnt = 0;
                            for (String frameNum : frameArray) {
                                if (StringUtils.isEmpty(frameNum)) {
                                    continue;
                                }

                                if (cnt > 0) {
                                    frameConstraint += ",";
                                }
                                frameConstraint += frameNum;
                                cnt++;
                            }

                            frameConstraint += ")";
                        }
                    }

                } else if (frameOp.equals("BETWEEN")) {
                    String frameVal1 = queryInfo.get("frameVal1");
                    String frameVal2 = queryInfo.get("frameVal2");
                    if (!StringUtils.isEmpty(frameVal1) && StringUtils.isEmpty(frameVal2)) {
                        throw (new IllegalArgumentException("Missing second BETWEEN constraint!"));
                    }
                    if (StringUtils.isEmpty(frameVal1) && !StringUtils.isEmpty(frameVal2)) {
                        throw (new IllegalArgumentException("Missing first BETWEEN constraint!"));
                    }
                    if (!StringUtils.isEmpty(frameVal1) && !StringUtils.isEmpty(frameVal2)) {
                        frameConstraint += " BETWEEN " + frameVal1.trim() + " AND " + frameVal2.trim();
                    }
                }

                if (frameConstraint.length() > "frame_num".length()) {
                    constraints.add(frameConstraint);
                }
            }
        }

        // process L3 only constraints
        if (productLevel.equalsIgnoreCase("3A") || productLevel.equalsIgnoreCase("3O")) {
            // process COADD IDs (support multiple IDs)
            String coaddIds = queryInfo.get("coaddId");
            if (!StringUtils.isEmpty(coaddIds)) {
                String[] coaddArray = coaddIds.split("[,; ]+");
                String coaddConstraint = "coadd_id";
                if (coaddArray.length == 1) {
                    coaddConstraint += "='" + coaddArray[0] + "'";
                } else {
                    coaddConstraint += " IN (";
                    int cnt = 0;
                    for (String coaddId : coaddArray) {
                        if (StringUtils.isEmpty(coaddId)) {
                            continue;
                        }

                        if (cnt > 0) {
                            coaddConstraint += ",";
                        }
                        coaddConstraint += "'" + coaddId + "'";
                        cnt++;
                    }

                    coaddConstraint += ")";
                }

                constraints.add(coaddConstraint);
            }
        }

        // process BAND - ENUMSTRING
        String bands = queryInfo.get("band");
        if (!StringUtils.isEmpty(bands) && bands.split(",").length < 4) {
            constraints.add("band IN (" + bands + ")");
        }

        // compile all constraints
        if (!constraints.isEmpty()) {

            int i = 0;
            for (String s : constraints) {
                if (i > 0) {
                    constrStr += " AND ";
                }
                constrStr += s;

                i++;
            }
        }

        return constrStr;
    }

    /*
     * Return product level (single exposure, level 3o or atlas 3a)
     * @param sourceId source ID
     * @return product level string
     */
    private static String getProductLevelFromSourceId(String sourceId) {
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

    public enum DataProduct {
        PRELIM_1B("prelim","p1bm_frm", "prelim_p1bs_psd", "links-prelim/l1b/"),
        PRELIM_3A("prelim","p3am_cdd", "prelim_p3as_psd", "links-prelim/l3a/"),
        PRELIM_POSTCRYO_1B("prelim_postcryo","p1bm_frm", "prelim_2band_p1bs_psd", "links-postcryo-prelim/l1b-2band/"),
        PRELIM_POSTCRYO_3A("merge", "merge_p3am_cdd", "prelim_2band_p1bs_psd", "links-allwise/l3a-merge/"), //map to merge_3a, should just show no data for post-cryo
        ALLWISE_MULTIBAND_3A("allwise","p3am_cdd", "allwise_p3as_psd", "links-allwise/l3a/"), 
        ALLSKY_4BAND_1B("allsky", "4band_p1bm_frm", "allsky_4band_p1bs_psd", "links-allsky/l1b-4band/"),
        ALLSKY_4BAND_3A("allsky", "4band_p3am_cdd", "allsky_4band_p3as_psd", "links-allsky/l3a-4band/"),
        CRYO_3BAND_1B("cryo_3band", "3band_p1bm_frm", "allsky_3band_p1bs_psd", "links-3band/l1b-3band/"),
        CRYO_3BAND_3A("cryo_3band", "3band_p3am_cdd", "allsky_3band_p3as_psd", "links-3band/l3a-3band/"),  // currently they are different: p1bm_frm and p3am_cdd
        POSTCRYO_1B("postcryo", "2band_p1bm_frm", "allsky_2band_p1bs_psd", "links-postcryo/l1b-2band/"),
        MERGE_1B("merge", "merge_p1bm_frm", "allsky_4band_p1bs_psd", "links-allsky/l1b-merge/"),         // exists under links-allsky
        MERGE_3A("merge", "merge_p3am_cdd", "allsky_4band_p1bs_psd", "links-allwise/l3a-merge/"),       // exists under links-allwise
        NEOWISER_1B("neowiser", "p1bm_frm", "neowiser_p1bs_psd", "links-neowiser/l1b/"),

        // internal dataset
        MERGE_INT_1B("merge_int", "merge_i1bm_frm", "merge_i1bs_psd", "links-merge/l1b/"),
        MERGE_INT_3A("merge_int", "merge_p3am_cdd", "allsky_4band_p3as_psd", "links-merge/l3a/"),
        NEOWISER_PROV_1B("neowiser_prov", "i1bm_frm", "i1bs_psd", "links-nprov/l1b/"),
        NEOWISER_YR1_1B("neowiser_yr1", "yr1_p1bm_frm", "yr1_p1bs_psd", "links-neowiser/l1b-yr1/"),
        PASS1_1B("pass1", "i1bm_frm", "i1bs_psd", "links-pass1/l1b/"),
        PASS1_3A("pass1", "i3am_cdd", "i3as_psd", "links-pass1/l3a/"),
        PASS1_3O("pass1", "i3om_cdd", "i3os_psd", "links-pass1/l3o/"),
        PASS2_4BAND_1B("pass2", "4band_i1bm_frm", "4band_i1bs_psd", "links-pass2/l1b-4band/"),
        PASS2_4BAND_3A("pass2", "4band_i3am_cdd", "4band_i3as_psd", "links-pass2/l3a-4band/"),
        PASS2_3BAND_1B("pass2", "3band_i1bm_frm", "3band_i1bs_psd", "links-pass2/l1b-3band/"),
        PASS2_3BAND_3A("pass2", "3band_i3am_cdd", "3band_i3as_psd", "links-pass2/l3a-3band/"),
        PASS2_2BAND_1B("pass2",  "2band_i1bm_frm", "2band_i1bs_psd", "links-pass2/l1b-2band/");

        private String dataset;
        private String imageTable;
        private String sourceTable;   // this sourceTable is mapped and meant for source Id searches
        private String filesysDatasetPath;


        DataProduct(String dataset, String imageTable, String sourceTable, String filesysDatasetPath) {
            this.dataset = dataset;
            this.imageTable = imageTable;
            this.sourceTable = sourceTable;
            this.filesysDatasetPath = filesysDatasetPath;
        }

        public String getDataset() { return dataset;}
        public String getImageTable() { return imageTable;}
        public String getSourceTable() { return sourceTable;}
        public String getFilesysDatasetPath() { return filesysDatasetPath;}

        public String imageset() {
            return name().substring(0, name().lastIndexOf("_"));
        }
        public String plevel() {
            return name().substring(name().lastIndexOf("_")+1);
        }
    }

}
