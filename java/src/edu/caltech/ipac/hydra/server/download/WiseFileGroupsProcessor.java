package edu.caltech.ipac.hydra.server.download;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.wise.WiseFileRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.CollectionUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@SearchProcessorImpl(id = "WiseDownload")
public class WiseFileGroupsProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private final int L1B_FITS_SIZE = 4167360;
    private final int L1B_FITS_SIZE_W4 = 1071360;
    private final int L1B_FITS_SIZE_ART = 4096;

    private final int L3_FITS_SIZE = 67080960;
    private final int L3_FITS_SIZE_ART = 4096;

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        // create unique list of filesystem-based and url-based files
        Set<String> zipFiles = new HashSet<String>();

        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;

        String allBands = request.getParam("allBands");
        boolean allFlag = false;
        if (allBands.equalsIgnoreCase("yes")) {
            allFlag = true;
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

        String artFiles = request.getParam("artFiles");
        if (artFiles != null && artFiles.length() > 0 && !artFiles.equalsIgnoreCase("_none_")) {
            String[] artArr = artFiles.split(",");
            for (String art : artArr) {
                if (art.equalsIgnoreCase("M")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.MASK);
                } else if (art.equalsIgnoreCase("U")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.UNCERTAINTY);
                } else if (art.equalsIgnoreCase("C")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.COVERAGE);
                } else if (art.equalsIgnoreCase("D")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.DIFF_SPIKES);
                } else if (art.equalsIgnoreCase("H")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.HALOS);
                } else if (art.equalsIgnoreCase("O")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.OPT_GHOSTS);
                } else if (art.equalsIgnoreCase("P")) {
                    types.add(WiseFileRetrieve.IMG_TYPE.LATENTS);
                }
            }
        }

        String baseFilename = WiseFileRetrieve.WISE_FILESYSTEM_BASEPATH;
        String schema = request.getSearchRequest().getSafeParam(WiseRequest.SCHEMA);
        request.setParam(WiseRequest.SCHEMA, schema);
        // use merge schema for multiple image sets
        if (schema.contains(",")) {
            schema = WiseRequest.MERGE;
        }
        request.setParam(WiseRequest.SCHEMA, schema);


        String productLevel = request.getSafeParam("ProductLevel");
        if (productLevel.equalsIgnoreCase("1b")) {
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                    selectedRows, "scan_id", "frame_num", "band", "in_ra", "in_dec", "ra_obj", "dec_obj", "crval1", "crval2");

            // create a unique collection
            LinkedHashSet<String> lhs = new LinkedHashSet<String>();
            for (int rowIdx : selectedRows) {
                String scanId = (String) dgData.get(rowIdx, "scan_id");
                Integer frameNum = (Integer) dgData.get(rowIdx, "frame_num");

                // look for ra_obj first - moving object search
                Double ra = (Double) dgData.get(rowIdx, "ra_obj");
                if (ra == null || ra == 0) {
                    // next look for in_ra (IBE returns this)
                    ra = (Double) dgData.get(rowIdx, "in_ra");
                    if (ra == null || ra == 0) {
                        // all else fails, try using crval1
                        ra = (Double) dgData.get(rowIdx, "crval1");
                    }
                }

                // look for dec_obj first - moving object search
                Double dec = (Double) dgData.get(rowIdx, "dec_obj");
                if (dec == null || dec == 0) {
                    // next look for in_dec (IBE returns this)
                    dec = (Double) dgData.get(rowIdx, "in_dec");
                    if (dec == null || dec == 0) {
                        // all else fails, try using crval2
                        dec = (Double) dgData.get(rowIdx, "crval2");
                    }
                }

                if (allFlag) {
                    List<Integer> availableBands = CollectionUtil.asList(WiseRequest.getAvailableBandsForScanID(scanId, schema));
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
                } else {
                    Integer band = (Integer) dgData.get(rowIdx, "band");
                    if (cutFlag) {
                        lhs.add(scanId + ":" + frameNum + ":" + band + ":" + ra + ":" + dec);
                    } else {
                        // ra/dec does not matter
                        lhs.add(scanId + ":" + frameNum + ":" + band + ":0:0");
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
                            String fileName = WiseFileRetrieve.createFilepath_1b(schema, productLevel, scanId, frameNum, band, t);
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
                        String baseUrl = WiseFileRetrieve.getBaseURL(request);
                        String filenameCutoutInfo = "";

                        String url;
                        int estSize;
                        if ((t == WiseFileRetrieve.IMG_TYPE.INTENSITY || t == WiseFileRetrieve.IMG_TYPE.MASK ||
                                t == WiseFileRetrieve.IMG_TYPE.UNCERTAINTY)) {

                            if (cutFlag) {
                                String size = request.getParam("subsize");
                                double sizeD = Double.parseDouble(size);
                                filenameCutoutInfo = "_ra" + ra + "_dec" + dec + "_asec" + String.format("%.3f", sizeD * 3600);

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

                        String zipPath = null;
                        if (zipFolders) {
                            zipPath = WiseFileRetrieve.createZipPath_1b(productLevel, scanId, frameNum);
                        }

                        if (!zipFiles.contains(url)) {
                            zipFiles.add(url);
                            FileInfo fi = new FileInfo(url, new CutoutFilenameResolver(filenameCutoutInfo, zipPath), estSize);
                            fiArr.add(fi);
                            fgSize += estSize;
                        }
                    }
                }
            }

            StopWatch.getInstance().printLog("Wise download per file");

            FileGroup fg = new FileGroup(fiArr, null, fgSize, "WISE Download Files"); // don't use base filename - it's in the name already
            fgArr.add(fg);

        } else if (productLevel.equalsIgnoreCase("3o") || productLevel.equalsIgnoreCase("3a")) {
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                    selectedRows, "coadd_id", "band", "in_ra", "in_dec");

            // create a unique collection
            LinkedHashSet<String> lhs = new LinkedHashSet<String>();
            for (int rowIdx : selectedRows) {
                String coaddId = (String) dgData.get(rowIdx, "coadd_id");

                // look for ra_obj first - moving object search (should not be the case for moving object - L3)
                Double ra = (Double) dgData.get(rowIdx, "ra_obj");
                if (ra == null || ra == 0) {
                    // next look for in_ra (IBE returns this)
                    ra = (Double) dgData.get(rowIdx, "in_ra");
                    if (ra == null || ra == 0) {
                        // all else fails, try using crval1
                        ra = (Double) dgData.get(rowIdx, "crval1");
                    }
                }

                // look for dec_obj first - moving object search (should not be the case for moving object - L3)
                Double dec = (Double) dgData.get(rowIdx, "dec_obj");
                if (dec == null || dec == 0) {
                    // next look for in_dec (IBE returns this)
                    dec = (Double) dgData.get(rowIdx, "in_dec");
                    if (dec == null || dec == 0) {
                        // all else fails, try using crval2
                        dec = (Double) dgData.get(rowIdx, "crval2");
                    }
                }

                if (allFlag) {
                    for (int i = 1; i < 5; i++) {
                        if (cutFlag) {
                            lhs.add(coaddId + ":" + i + ":" + ra + ":" + dec);
                        } else {
                            // ra/dec does not matter
                            lhs.add(coaddId + ":" + i + ":0:0");
                        }
                    }
                } else {
                    Integer band = (Integer) dgData.get(rowIdx, "band");
                    if (cutFlag) {
                        lhs.add(coaddId + ":" + band + ":" + ra + ":" + dec);
                    } else {
                        // ra/dec does not matter
                        lhs.add(coaddId + ":" + band + ":0:0");
                    }
                }
            }

            // iterate the unique collection
            for (String item : lhs) {
                String[] parts = item.split(":");
                String coaddId = parts[0];
                String band = parts[1];
                String ra = parts[2];
                String dec = parts[3];

                for (WiseFileRetrieve.IMG_TYPE t : types) {
                    StopWatch.getInstance().start("Wise download per file");

                    String retrievalType = WiseFileRetrieve.WISE_DATA_RETRIEVAL_TYPE;
                    if (cutFlag && (t == WiseFileRetrieve.IMG_TYPE.INTENSITY || t == WiseFileRetrieve.IMG_TYPE.COVERAGE ||
                            t == WiseFileRetrieve.IMG_TYPE.UNCERTAINTY)) {

                        retrievalType = "url";
                    }

                    if (retrievalType.equalsIgnoreCase("filesystem")) {
                        if (baseFilename == null || baseFilename.length() == 0) {
                            // if not configured, default to URL retrieval
                            retrievalType = "url";

                        } else {
                            String fileName = WiseFileRetrieve.createFilepath_3(schema, productLevel, coaddId, band, t);

                            File f = new File(fileName);
                            if (f != null && f.exists()) {
                                if (zipFolders) {
                                    String zipName = WiseFileRetrieve.createZipPath_3(productLevel, coaddId) + f.getName();
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
                                    if ((t == WiseFileRetrieve.IMG_TYPE.INTENSITY || t == WiseFileRetrieve.IMG_TYPE.COVERAGE ||
                                            t == WiseFileRetrieve.IMG_TYPE.UNCERTAINTY)) {
                                        estSize = L3_FITS_SIZE;
                                    }

                                    if (zipFolders) {
                                        String zipName = WiseFileRetrieve.createZipPath_3(productLevel, coaddId) + f.getName();
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
                        String baseUrl = WiseFileRetrieve.getBaseURL(request);
                        String filenameCutoutInfo = "";

                        String url;
                        int estSize;
                        if ((t == WiseFileRetrieve.IMG_TYPE.INTENSITY || t == WiseFileRetrieve.IMG_TYPE.COVERAGE ||
                                t == WiseFileRetrieve.IMG_TYPE.UNCERTAINTY)) {

                            if (cutFlag) {
                                String size = request.getParam("subsize");
                                double sizeD = Double.parseDouble(size);
                                filenameCutoutInfo = "_ra" + ra + "_dec" + dec + "_asec" + String.format("%.3f", sizeD * 3600);

                                url = WiseFileRetrieve.createCutoutURLString_3(baseUrl, coaddId, band, t, ra, dec, size);

                                // estSize = max cutout size; actual size could be smaller depending on location of crval1/2
                                double ratio = sizeD / ((4095 * 1.375) / 3600);  // pixel length * asec/pixel / 3600
                                if (ratio < 1.0) {
                                    estSize = (int) (((L3_FITS_SIZE - 4860) * Math.pow(ratio, 2.0)) + 4860);  // 4860 = L3 header size
                                } else {
                                    estSize = L3_FITS_SIZE;
                                }

                            } else {
                                url = WiseFileRetrieve.createURLString_3(baseUrl, coaddId, band, t);
                                estSize = L3_FITS_SIZE;
                            }

                        } else {
                            // artifact
                            url = WiseFileRetrieve.createURLString_3(baseUrl, coaddId, band, t);
                            estSize = L3_FITS_SIZE_ART;
                        }

                        String zipPath = null;
                        if (zipFolders) {
                            zipPath = WiseFileRetrieve.createZipPath_3(productLevel, coaddId);
                        }

                        if (!zipFiles.contains(url)) {
                            zipFiles.add(url);
                            FileInfo fi = new FileInfo(url, new CutoutFilenameResolver(filenameCutoutInfo, zipPath), estSize);
                            fiArr.add(fi);
                            fgSize += estSize;
                        }
                    }
                }
            }

            StopWatch.getInstance().printLog("Wise download per file");

            FileGroup fg = new FileGroup(fiArr, null, fgSize, "WISE Download Files"); // don't use base filename - it's in the name already
            fgArr.add(fg);
        }

        return fgArr;
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
