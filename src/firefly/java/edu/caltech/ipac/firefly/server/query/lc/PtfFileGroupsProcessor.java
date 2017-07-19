/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.ptf.PtfFileRetrieve;
import edu.caltech.ipac.firefly.server.query.ptf.PtfIbeResolver;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA. User: wmi Date: 10/8/13 Time: 4:30 PM To change this template use File | Settings | File
 * Templates.
 */

@SearchProcessorImpl(id = "PtfLcDownload")
public class PtfFileGroupsProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private final int L2_FITS_SIZE = 33586560;
    private final int L2_ANSI_SIZE = 16781760;

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

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean doCutout = dlCutouts != null && dlCutouts.equalsIgnoreCase("cut");

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders = zipType != null && zipType.equalsIgnoreCase("folder");

        List<String> types = new ArrayList<String>();

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "oid", "pfilename", "fid", "pid", "ccdid", "ra", "dec");

        if (request.getParam("ProductLevel") == null) {
            request.setParam("ProductLevel", "l1");
        }
        String subSize = request.getSafeParam("cutoutSize");
        double sizeD = StringUtils.isEmpty(subSize) ? 0 : Double.parseDouble(subSize);
        String sizeAsecStr = String.valueOf((int) (sizeD * 3600));

        Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();
        for (int rowIdx : selectedRows) {
            FileInfo fi = null;
            String pidstr = dgData.get(rowIdx, "pid").toString();
            long pid = Long.parseLong(pidstr);

            String pfilename = null;
            if (pidstr != null) {
                try {
                    pfilename = new PtfIbeResolver().getListPfilenames(new long[]{pid})[0];
                } catch (InterruptedException e) {
                    //PID didn't work
                    throw new DataAccessException("PID " + pid + " didn't give any result ", e);
                }
            } else {
                pfilename = (String) dgData.get(rowIdx, "pfilename");
            }

            String baseUrl = getBaseURL(request);

            String fileName = pfilename;//createBaseFileString_l2(fieldDir, fieldId, fId, ccdId);

            logger.briefInfo("filename=" + fileName);

            File f = new File(fileName);
            String extName = doFolders ? fileName : f.getName();
            if (doCutout) {
                // look for in_ra and in_dec returned by IBE
                String subLon = dgData.get(rowIdx, "in_ra") != null ? String.format("%.4f", (Double) dgData.get(rowIdx, "in_ra")) : null;
                if (StringUtils.isEmpty(subLon)) {
                    subLon = String.format("%.4f", (Double) dgData.get(rowIdx, "ra"));
                }

                // look for in_ra and in_dec returned by IBE
                String subLat = dgData.get(rowIdx, "in_dec") != null ? String.format("%.4f", (Double) dgData.get(rowIdx, "in_dec")) : null;
                if (StringUtils.isEmpty(subLat)) {
                    // if it fails, try using crval2
                    subLat = String.format("%.4f", (Double) dgData.get(rowIdx, "dec"));
                }
                String cutoutInfo = "_ra" + subLon + "_dec" + subLat + "_asec" + sizeAsecStr;

                extName = extName.replace(".fits",cutoutInfo+".fits");
                String url = createCutoutURLString_l2(baseUrl, pfilename, subLon, subLat, subSize);
                logger.briefInfo("cutout url: " + url);


//                // strip out filename when using file resolver
//                if (doFolders) {
//                    int idx = extName.lastIndexOf("/");
//                    idx = idx < 0 ? 0 : idx;
//                    extName = extName.substring(0, idx) + "/";
//                } else {
//                    extName = null;
//                }

                fi = new FileInfo(url, extName, 100000);
            } else {
                String url = baseUrl + fileName;
                fi = new FileInfo(url, extName, 100000);
            }
            if (fi != null) {
                fi.setCookies(cookies);
                fiArr.add(fi);
                fgSize += fi.getSizeInBytes();
            }
        }

        FileGroup fg = new FileGroup(fiArr, null, fgSize, "PTF Download Files");
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        fgArr.add(fg);
        return fgArr;
    }

    static String createCutoutURLString_l2(String baseUrl, String baseFile, String lon, String lat, String size) {

        //http://irsa.ipac.caltech.edu/ibe/data/ptf/images/level1/{$pfilename}?center=${ra},${dec}&size=${cutoutSizeInDeg}&gzip=false

        String url = baseUrl + baseFile;
        url += "?center=" + lon + "," + lat;
        if (!StringUtils.isEmpty(size)) {
            url += "&size=" + size;
        }
        url += "&gzip=" + false;

        return url;
    }

//    static String getBaseURL(ServerRequest sr) {
//
//        String host = sr.getSafeParam("host") != null ? sr.getSafeParam("host") : PtfIbeResolver.PTF_IBE_HOST;
//
//        return QueryUtil.makeUrlBase(host) + "/data/ptf/images/level1/";
//    }

    private static String getBaseURL(ServerRequest sr) throws MalformedURLException {
        // build service
        return new PtfFileRetrieve().getBaseURL(sr);

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

