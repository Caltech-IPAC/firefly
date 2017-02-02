package edu.caltech.ipac.firefly.server.query.lc;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.wise.WiseFileRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * WARNING / TODO Dealing only with WISE download for now, client should use the downalod based on the mission name
 */
@SearchProcessorImpl(id = "LightCurveFileGroupsProcessor")
public class LightCurveFileGroupsProcessor extends FileGroupsProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private final int L1B_FITS_SIZE = 4167360;
    private final int L1B_FITS_SIZE_W4 = 1071360;
    private final int L1B_FITS_SIZE_ART = 4096;

    private final int L3_FITS_SIZE = 67080960;
    private final int L3_FITS_SIZE_ART = 4096;

    private final String IBE_HOST = AppProperties.getProperty("wise.ibe.host");

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        try {
            DownloadRequest dlReq = (DownloadRequest) request;
            TableServerRequest searchReq = dlReq.getSearchRequest();
            Collection<Integer> selectedRows = dlReq.getSelectedRows();
            return computeFileGroup(dlReq);


//            // temporary mock data instead of using searchReq
//            File voRes = new File(ServerContext.getTempWorkDir(), "p3am_cdd.vot");
//            int statusCode = HttpServices.getDataViaUrl(new URL("http://irsa.ipac.caltech.edu/ibe/sia/wise/allwise/p3am_cdd?POS=10.6,40.2&SIZE=10"), voRes);
//            if (statusCode != 500) {
//                // should be 248 images found.
//                DataGroup dataWithUrl = DataGroupReader.readAnyFormat(voRes);
//                return computeFileGroup(dataWithUrl, selectedRows);
//            }
//            return null;
        } catch (Exception e) {
            LOGGER.error(e);
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * givent the results from a search, compute the files to be packaged from the selectedRows.
     *
     * @param data         the tabular data from a search
     * @param selectedRows selected rows from the data above.  index starts from zero.
     * @return
     * @throws Exception
     */
    private List<FileGroup> computeFileGroup(DataGroup data, Collection<Integer> selectedRows) throws Exception {
        ArrayList<FileInfo> images = new ArrayList<>();
        for (int idx : selectedRows) {
            String url = String.valueOf(data.get(idx).getDataElement("sia_url"));
            images.add(new FileInfo(url, "/3a/img" + idx, 67 * 1024 * 1024));
        }
        FileGroup fgroup = new FileGroup(images, new File("/lightcurve_data"), selectedRows.size() * 640 * 1024, "Test lightcurve data");
        return Arrays.asList(fgroup);
    }


    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        // create unique list of filesystem-based and url-based files
        Set<String> zipFiles = new HashSet<String>();

        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;
//
        // TODO this for WISE!
        // TODO Either we keep this and update DownloadDialog.jsx with passing the option from UI (flux variable ~ band)  :: messy!
        // TODO or we just decide to download all bands
        String allBands = request.getParam("allBands");
        boolean allFlag = true;

        // TODO for now, downalod all bands

        if (allBands != null) {
            allFlag = allBands.equalsIgnoreCase("yes");
        }

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean cutFlag = false;
        if (dlCutouts != null && dlCutouts.equalsIgnoreCase("cut")) {
            cutFlag = true;
        }

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean zipFolders = true;
        if (zipType != null && zipType.equalsIgnoreCase("flat")) {
            zipFolders = false;
        }

        // build file types list
        ArrayList<WiseFileRetrieve.IMG_TYPE> types = new ArrayList<WiseFileRetrieve.IMG_TYPE>();
        types.add(WiseFileRetrieve.IMG_TYPE.INTENSITY);

        String baseFilename = WiseFileRetrieve.WISE_FILESYSTEM_BASEPATH;

        //TODO this should come from the request but how dealing with public and internal here
        String schemaGroup = request.getBooleanParam("wise_public_release", true) ? "merge" : "merge_int";//WiseRequest.getTrueSchema(request.getSearchRequest());

        String tableSchema = schemaGroup.equals("merge") ? "merge_p1bm_frm" : "merge_i1bm_frm";

        request.setParam(WiseRequest.SCHEMA, schemaGroup);

        // For LC viewer we only want single exposure - product level = 1b
        String pl = request.getSafeParam("ProductLevel");
        String productLevel = pl != null ? pl : "1b";
        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "source_id_mf", "frame_id", "ra", "dec");


        String frameidRegex = "(\\d+)([0-9][a-z])(\\w+)";

        Pattern pattern = Pattern.compile(frameidRegex);

        String srcId = "";
        // create a unique collection
        LinkedHashSet<String> lhs = new LinkedHashSet<String>();
        for (int rowIdx : selectedRows) {
            srcId = (String) dgData.get(rowIdx, "source_id_mf");// should be only one source!
            String frameId = (String) dgData.get(rowIdx, "frame_id");

// find scan id , scan group and frame num
            Matcher matcher = pattern.matcher(frameId);
            String scanId = null, scanGrp = null;
            Integer frameNum = null;

            if (matcher.matches()) {
                scanId = matcher.group(1) + matcher.group(2);
                scanGrp = matcher.group(2);
                frameNum = Integer.parseInt(matcher.group(3));
            }

            Double ra = (Double) dgData.get(rowIdx, "ra");
            Double dec = (Double) dgData.get(rowIdx, "dec");

            List<Integer> availableBands = CollectionUtil.asList(WiseRequest.getAvailableBandsForScanID(scanId, schemaGroup));
            for (int i = 1; i < 5; i++) {
                if (availableBands.contains(i)) {
                    if (cutFlag) {
                        lhs.add(scanId + ":" + frameNum + ":" + i + ":" + ra + ":" + dec);
                    } else {
                        // ra/dec does not matter
                        lhs.add(scanId + ":" + frameNum + ":" + i + ":0:0");
                    }
                }
            }
        }

        // iterate the unique collection
        for (String item : lhs) {
            String[] parts = item.split(":");
            String scanId = parts[0];
            String frameNum = parts[1];
            String band = parts[2];
            String ra = parts[3];
            String dec = parts[4];

            for (WiseFileRetrieve.IMG_TYPE t : types) {
                StopWatch.getInstance().start("Wise download per file");

                String retrievalType = WiseFileRetrieve.WISE_DATA_RETRIEVAL_TYPE;
                if (cutFlag && (t == WiseFileRetrieve.IMG_TYPE.INTENSITY || t == WiseFileRetrieve.IMG_TYPE.MASK ||
                        t == WiseFileRetrieve.IMG_TYPE.UNCERTAINTY)) {

                    retrievalType = "url";
                }

                if (retrievalType.equalsIgnoreCase("filesystem")) {
                    if (baseFilename == null || baseFilename.length() == 0) {
                        // if not configured, default to URL retrieval
                        retrievalType = "url";

                    } else {
                        String fileName = WiseFileRetrieve.createFilepath_1b(schemaGroup, productLevel, scanId, frameNum, band, t);
                        File f = new File(fileName);
                        if (f != null && f.exists()) {
                            if (zipFolders) {
                                String zipName = WiseFileRetrieve.createZipPath_1b(productLevel, scanId, frameNum) + f.getName();
                                if (!zipFiles.contains(zipName)) {
                                    zipFiles.add(zipName);
                                    FileInfo fi = new FileInfo(fileName, zipName, f.length());
                                    fiArr.add(fi);
                                    fgSize += f.length();
                                }

                            } else {
                                String zipName = f.getName();
                                if (!zipFiles.contains(zipName)) {
                                    zipFiles.add(zipName);
                                    FileInfo fi = new FileInfo(fileName, zipName, f.length());
                                    fiArr.add(fi);
                                    fgSize += f.length();
                                }
                            }

                        } else {
                            fileName += ".gz";
                            f = new File(fileName);
                            if (f != null && f.exists()) {
                                long estSize = f.length();
                                if ((t == WiseFileRetrieve.IMG_TYPE.INTENSITY || t == WiseFileRetrieve.IMG_TYPE.MASK ||
                                        t == WiseFileRetrieve.IMG_TYPE.UNCERTAINTY)) {
                                    if (band.equalsIgnoreCase("4")) {
                                        estSize = L1B_FITS_SIZE_W4;
                                    } else {
                                        estSize = L1B_FITS_SIZE;
                                    }
                                }

                                if (zipFolders) {
                                    String zipName = WiseFileRetrieve.createZipPath_1b(productLevel, scanId, frameNum) + f.getName();
                                    if (!zipFiles.contains(zipName)) {
                                        zipFiles.add(zipName);
                                        FileInfo fi = new FileInfo(fileName, zipName, estSize);
                                        fiArr.add(fi);
                                        fgSize += estSize;
                                    }

                                } else {
                                    String zipName = f.getName();
                                    if (!zipFiles.contains(zipName)) {
                                        zipFiles.add(zipName);
                                        FileInfo fi = new FileInfo(fileName, zipName, estSize);
                                        fiArr.add(fi);
                                        fgSize += estSize;
                                    }
                                }
                            }
                        }
                    }
                }

                if (retrievalType.equalsIgnoreCase("url")) {
                    // TODO Can't use WiseFileRetrieve.getBaseURL(request) because request doesn't have the paramters needed
                    // TODO AND the method is based on expecting parameters that came form a different WISE table (AllWISE with band, scan_id,etc.);

                    String baseUrl = QueryUtil.makeUrlBase(IBE_HOST) + "/data/wise/" + schemaGroup + "/" + tableSchema;
                    String filenameInfo = "";

                    String url;
                    int estSize;
                    if (t == WiseFileRetrieve.IMG_TYPE.INTENSITY) {
                        filenameInfo = scanId + StringUtils.pad(3, frameNum, StringUtils.Align.RIGHT, '0') + "-w" + band + "-int-1b";
                        if (cutFlag) {
                            String size = "2";// TODO need to pass it.//request.getParam("subsize");
                            double sizeD = Double.parseDouble(size);
                            filenameInfo += "_ra" + ra + "_dec" + dec + "_asec" + String.format("%.3f", sizeD * 3600);

                            url = WiseFileRetrieve.createCutoutURLString_1b(baseUrl, scanId, frameNum, band, t, ra, dec, size);

                            // estSize = max cutout size; actual size could be smaller depending on location of crval1/2
                            if (band.equalsIgnoreCase("4")) {
                                double ratio = sizeD / ((508 * 5.52) / 3600);  // pixel length * asec/pixel / 3600
                                if (ratio < 1.0) {
                                    estSize = (int) (((L1B_FITS_SIZE_W4 - 38336) * Math.pow(ratio, 2.0)) + 38336);  // 38336 = L1b header size
                                } else {
                                    estSize = L1B_FITS_SIZE_W4;
                                }

                            } else {
                                double ratio = sizeD / ((1016 * 2.76) / 3600);
                                if (ratio < 1.0) {
                                    estSize = (int) (((L1B_FITS_SIZE - 38336) * Math.pow(ratio, 2.0)) + 38336);
                                } else {
                                    estSize = L1B_FITS_SIZE;
                                }
                            }

                        } else {
                            url = WiseFileRetrieve.createURLString_1b(baseUrl, scanId, frameNum, band, t);

                            if (band.equalsIgnoreCase("4")) {
                                estSize = L1B_FITS_SIZE_W4;
                            } else {
                                estSize = L1B_FITS_SIZE;
                            }
                        }

                    } else {
                        // artifact
                        url = WiseFileRetrieve.createURLString_1b(baseUrl, scanId, frameNum, band, t);
                        estSize = L1B_FITS_SIZE_ART;
                    }
                    String zipPath = "";
                    filenameInfo = srcId + "-" + filenameInfo;
                    if (zipFolders) {
                        zipPath = WiseFileRetrieve.createZipPath_1b(productLevel, scanId, frameNum);
                    }

                    if (!zipFiles.contains(url)) {
                        zipFiles.add(url);

                        FileInfo fi = new FileInfo(url, zipPath + filenameInfo + ".fits", estSize);
                        fiArr.add(fi);
                        fgSize += estSize;
                    }
                }
            }
        }

        StopWatch.getInstance().printLog("Wise download per file");

        FileGroup fg = new FileGroup(fiArr, null, fgSize, "WISE Download Files"); // don't use base filename - it's in the name already
        fgArr.add(fg);


        return fgArr;
    }

    class CutoutFilenameResolver implements FileInfo.FileNameResolver {
        private String _cutoutData;
        private String _pathStructure;

        public CutoutFilenameResolver(String data, String path) {
            _cutoutData = data;
            _pathStructure = path;
        }

        public String getResolvedName(String input) {
            String fileName;
            String gzExt = "";
            String path = StringUtils.isEmpty(_pathStructure) ? "" : _pathStructure;

            if (FileUtil.isExtension(input, FileUtil.GZ)) {
                gzExt = "." + FileUtil.GZ;
                input = FileUtil.getBase(input);
            }

            String base = FileUtil.getBase(input);
            if (!StringUtils.isEmpty(base)) {
                fileName = path + base + _cutoutData + "." + FileUtil.getExtension(input) + gzExt;
            } else {
                fileName = path + input + _cutoutData + gzExt;
            }

            fileName = fileName.replaceAll(",", "_");
            return fileName;
        }
    }
}

