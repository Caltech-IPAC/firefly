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
import edu.caltech.ipac.firefly.server.query.ztf.ZtfSciimsFileRetrieve;
import edu.caltech.ipac.firefly.server.query.ztf.ZtfFileRetrieve;
import edu.caltech.ipac.firefly.server.query.ztf.ZtfRefimsFileRetrieve;
import edu.caltech.ipac.firefly.server.query.ztf.ZtfIbeResolver;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;



@SearchProcessorImpl(id = "ZtfLcDownload")
public class ZtfLCFileGroupsProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private final int SCI_FITS_SIZE = 37872000;
    private final int MASK_SIZE = 18944640;

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

        String sciImage = request.getParam("sciImage");
        boolean dlSciImage = false;
        if (sciImage.equalsIgnoreCase("yes")) {
            dlSciImage = true;
        }

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean doCutout = dlCutouts != null && dlCutouts.equalsIgnoreCase("cut");

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders = zipType != null && zipType.equalsIgnoreCase("folder");

        // build file types list
        String artFiles = request.getParam("anciFiles");

        ArrayList<ZtfSciimsFileRetrieve.FILE_TYPE> types = new ArrayList<ZtfSciimsFileRetrieve.FILE_TYPE>();

     //   List<String> types = new ArrayList<String>();
        if (dlSciImage) {
            types.add(ZtfSciimsFileRetrieve.FILE_TYPE.SCI);
        }
        if (artFiles != null && artFiles.length() > 0 && !artFiles.equalsIgnoreCase("_none_")) {
            String[] artArr = artFiles.split(",");
            for (String art : artArr) {
                if (art.equalsIgnoreCase("M")) {
                    types.add(ZtfSciimsFileRetrieve.FILE_TYPE.MASK);
                } else if (art.equalsIgnoreCase("R")) {
                    types.add(ZtfSciimsFileRetrieve.FILE_TYPE.RAW);
                } else if (art.equalsIgnoreCase("S")) {
                    types.add(ZtfSciimsFileRetrieve.FILE_TYPE.SEXCATL);
                } else if (art.equalsIgnoreCase("P")) {
                    types.add(ZtfSciimsFileRetrieve.FILE_TYPE.PSFCATL);
                }
            }
        }

        String baseFilename = ZtfSciimsFileRetrieve.ZTF_FILESYSTEM_BASEPATH;

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "field","filtercode","ccdid","qid","filefracday","imgtypecode", "in_ra", "crval1", "in_dec", "crval2");

        String baseUrl = ZtfSciimsFileRetrieve.getBaseURL(request);
        String subSize = request.getSafeParam("subsize");
        double sizeD = StringUtils.isEmpty(subSize) ? 0 : Double.parseDouble(subSize);
        String sizeAsecStr = String.valueOf((int) (sizeD * 3600));

        Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();

        for (int rowIdx : selectedRows) {


            String filefracday = String.valueOf(dgData.get(rowIdx, "filefracday"));
            String field = String.valueOf(dgData.get(rowIdx, "field"));
            String filtercode = (String) dgData.get(rowIdx, "filtercode");
            String ccdid = String.valueOf(dgData.get(rowIdx, "ccdid"));
            String imgtypcode = (String) dgData.get(rowIdx, "imgtypecode");
            String qid = String.valueOf(dgData.get(rowIdx, "qid"));

            //for (String col : types) {
            for (ZtfSciimsFileRetrieve.FILE_TYPE t : types) {
                FileInfo fi = null;

                String fName = ZtfSciimsFileRetrieve.createFilepath_l1(filefracday,field,filtercode,ccdid,imgtypcode,qid,t);

                File f = new File("", fName);

                String extName = doFolders ? fName : f.getName();

                //long estSize = 5000;
                int estSize;
                // ZTF pixscal = 1.01 arcsec/pix

                double ratiol = sizeD / ((2048 * 1.01) / 3600);  // pixel length * asec/pixel / 3600
                double ratioh = 0.5 * ratiol; // ratioh = sizeD / ((4096 * 1.01) / 3600);pixel height * asec/pixel / 3600
                if (ratiol < 1.0) {
                    estSize = (int) (((SCI_FITS_SIZE - 31750) * (ratiol * ratioh)) + 31750);  // 31750 = SCI header size
                } else {
                    estSize = SCI_FITS_SIZE;
                }
                if (types.equals(ZtfSciimsFileRetrieve.FILE_TYPE.MASK)) {
                    estSize = (int) (0.5 * estSize);
                }

                if (doCutout && (t == ZtfSciimsFileRetrieve.FILE_TYPE.SCI || t == ZtfSciimsFileRetrieve.FILE_TYPE.MASK)) {

                    // look for in_ra and in_dec returned by IBE
                    String subLon = String.format("%.4f", (Double) dgData.get(rowIdx, "in_ra"));
                    if (StringUtils.isEmpty(subLon)) {
                        subLon = String.format("%.4f", (Double) dgData.get(rowIdx, "crval1"));
                    }

                    // look for in_ra and in_dec returned by IBE
                    String subLat = String.format("%.4f", (Double) dgData.get(rowIdx, "in_dec"));
                    if (StringUtils.isEmpty(subLat)) {
                        // if it fails, try using crval2
                        subLat = String.format("%.4f", (Double) dgData.get(rowIdx, "crval2"));
                    }
                    String cutoutInfo = "_ra" + subLon + "_dec" + subLat + "_asec" + sizeAsecStr;

                    String url = ZtfSciimsFileRetrieve.createCutoutURLString_l1(baseUrl, filefracday,field,filtercode,ccdid,imgtypcode,qid, t, subLon, subLat, subSize);
                    // strip out filename when using file resolver
                    if (doFolders) {
                        int idx = extName.lastIndexOf("/");
                        idx = idx < 0 ? 0 : idx;
                        extName = extName.substring(0, idx) + "/";
                    } else {
                        extName = null;
                    }

                    fi = new FileInfo(url, extName + cutoutInfo, estSize);
                } else {
                    String url = baseUrl + fName;
                    fi = new FileInfo(url, extName, estSize);
                }
                if (fi != null) {
                    fi.setCookies(cookies);
                    fiArr.add(fi);
                    fgSize += fi.getSizeInBytes();
                }
            }
        }
        FileGroup fg = new FileGroup(fiArr, null, fgSize, "ZTF Download Files");
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        fgArr.add(fg);
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
