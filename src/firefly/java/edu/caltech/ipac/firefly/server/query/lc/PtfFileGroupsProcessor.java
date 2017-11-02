/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.lc;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
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

        ArrayList<Integer> selectedRows = new ArrayList<>(request.getSelectedRows());

        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean doCutout = dlCutouts != null && dlCutouts.equalsIgnoreCase("cut");

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders = zipType != null && zipType.equalsIgnoreCase("folder");

        List<String> types = new ArrayList<String>();

        IpacTableParser.MappedData dgData = EmbeddedDbUtil.getSelectedMappedData(request.getSearchRequest(),
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
                } catch (Exception e) {
                    //PID didn't work, swallow it
                    logger.briefInfo("PID " + pid + " didn't give any result ");
                    continue;
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

                fi = new FileInfo(url, extName, 0);
            } else {
                String url = baseUrl + fileName;
                fi = new FileInfo(url, extName, 0);
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

    private static String getBaseURL(ServerRequest sr) throws MalformedURLException {
        // build service
        return PtfFileRetrieve.getBaseURL(sr);

    }
}