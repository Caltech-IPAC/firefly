package edu.caltech.ipac.hydra.server.download;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: Jul 24, 2012
 * Time: 3:04:56 PM
 * To change this template use File | Settings | File Templates.
 */
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.hydra.data.LsstRequest;
import edu.caltech.ipac.hydra.server.query.LsstFileRetrieve;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

@SearchProcessorImpl(id = "LsstDownload")
public class LsstFileGroupProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private final int FITS_SIZE =  45521280;
//    private final int icSrc_SIZE = 253440;
//    private final int icMatch_SIZE = 11520;


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

        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;

        String procImage = request.getParam("Image");
        boolean dlpImage = false;
        if (procImage.equalsIgnoreCase("yes")) {
            dlpImage = true;
        }

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean doCutout = dlCutouts != null && dlCutouts.equalsIgnoreCase("cut");

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders =  zipType != null && zipType.equalsIgnoreCase("folder");

        // build file types list
        String artFiles = request.getParam("anciFiles");
        ArrayList<LsstFileRetrieve.DL_TYPE> types = new ArrayList<LsstFileRetrieve.DL_TYPE>();

        if (dlpImage){
            types.add(LsstFileRetrieve.DL_TYPE.IMAGE);
        }

//        if (artFiles != null && artFiles.length() > 0 && !artFiles.equalsIgnoreCase("_none_")) {
//            String[] artArr = artFiles.split(",");
//            for (String art : artArr) {
//                if (art.equalsIgnoreCase("C")) {
//                    types.add(LsstFileRetrieve.DL_TYPE.icMatch);
//                } else if (art.equalsIgnoreCase("S")) {
//                    types.add(LsstFileRetrieve.DL_TYPE.icSrc);
//                }
//            }
//        }

        String baseFilename = LsstFileRetrieve.LSST_FILESYSTEM_BASEPATH;


        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "path", "filterName", "tract", "patch", "in_ra", "crval1", "in_dec", "crval2");

        String baseUrl = LsstFileRetrieve.getBaseURL(request);
        String subSize = request.getSafeParam("subsize");
        double sizeD = StringUtils.isEmpty(subSize) ? 0 : Double.parseDouble(subSize);
        String sizeAsecStr = String.valueOf((int)(sizeD * 3600));
        int estSize = FITS_SIZE;

        for (int rowIdx : selectedRows) {

             for(LsstFileRetrieve.DL_TYPE t : types) {
                FileInfo fi = null;
                String fName = (String) dgData.get(rowIdx, "path");
                String filter = (String) dgData.get(rowIdx, "filterName");
                String tract = (String) dgData.get(rowIdx, "tract");
                String patch = (String) dgData.get(rowIdx, "patch");

                if (StringUtils.isEmpty(fName)) {
                    logger.warn("No file name.  column=" + fName);
                    continue;
                }

                File f = new File(baseFilename, fName);

                String extName = doFolders ? fName : f.getName();
                //if (doCutout && ( col.equals(LsstRequest.PIMAGE) || col.equals(LsstRequest.MIMAGE) ) ) {
                 if (doCutout && (t.equals(LsstFileRetrieve.DL_TYPE.IMAGE) ) ) {
                    //long estSize = 5000;
                    //int estSize;
                    // LSST pixscal = 1.01 arcsec/pix

                    double ratiol = sizeD / ((2048 * 1.01) / 3600);  // pixel length * asec/pixel / 3600
                    double ratioh = 0.5 * ratiol; // ratioh = sizeD / ((4096 * 1.01) / 3600);pixel height * asec/pixel / 3600
                    if (ratiol < 1.0) {
                         estSize = (int) (((FITS_SIZE - 31750) * (ratiol*ratioh)) + 31750);  // 31750 = L1 header size
                    } else {
                         estSize = FITS_SIZE;
                    }

                    // look for in_ra and in_dec returned by IBE
                    String subLon = String.format("%.4f", (Double)dgData.get(rowIdx, "in_ra"));
                    if (StringUtils.isEmpty(subLon)) {
                      subLon = String.format("%.4f", (Double)dgData.get(rowIdx, "crval1"));
                    }

                    // look for in_ra and in_dec returned by IBE
                    String subLat = String.format("%.4f", (Double)dgData.get(rowIdx, "in_dec"));
                    if (StringUtils.isEmpty(subLat)) {
                      // if it fails, try using crval2
                        subLat = String.format("%.4f", (Double)dgData.get(rowIdx, "crval2"));
                    }
                    String cutoutInfo = "_ra" + subLon + "_dec" + subLat + "_asec" + sizeAsecStr;

                    String url = LsstFileRetrieve.createCutoutURLString_coadd(baseUrl, filter, tract, patch, t, subLon, subLat, subSize);
                    // strip out filename when using file resolver
                    if (doFolders) {
                        int idx = extName.lastIndexOf("/");
                        idx = idx < 0 ? 0 : idx;
                        extName = extName.substring(0, idx) + "/";
                    } else {
                        extName = null;
                    }

                    fi = new FileInfo(url, new CutoutFilenameResolver(cutoutInfo, extName), estSize);
                } else {
                    if (t == LsstFileRetrieve.DL_TYPE.IMAGE) {
                        estSize = FITS_SIZE;
                    //}else if(t == LsstFileRetrieve.DL_TYPE.icMatch){
                    //    estSize = icMatch_SIZE;
                    //}else if (t == LsstFileRetrieve.DL_TYPE.icSrc){
                    //    estSize = icSrc_SIZE;
                    //}else if (t == LsstFileRetrieve.DL_TYPE.JPEG){
                    //    estSize =JPEG_SIZE;
                    }
                    //String url =LsstFileRetrieve.createFileURLString(baseUrl, filter, tract, patch, t);
                    fi = new FileInfo(f.getAbsolutePath(), extName, f.length());
//                    if (doFolders) {
//                        extName = LsstFileRetrieve.getFileName(filter, tract, patch, t);
//                    } else {
//                        extName = url.substring(url.lastIndexOf("/")+1);
//                    }

                    //fi = new FileInfo(url, extName, estSize);
                }
                if (fi != null) {
                    fiArr.add(fi);
                    fgSize += fi.getSizeInBytes();
                }
            }
        }
        FileGroup fg = new FileGroup(fiArr, null, fgSize, "LSST Download Files");
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        fgArr.add(fg);
        return fgArr;
    }

}
