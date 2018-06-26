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
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.ztf.ZtfSciimsFileRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.server.query.ztf.ZtfFileRetrieve;
import edu.caltech.ipac.util.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



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

        ArrayList<Integer> selectedRows = new ArrayList<>(request.getSelectedRows());

        //Collection<Integer> selectedRows = request.getSelectedRows();
        //DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;

        boolean dlSciImage = true;

        // values = cut or orig
        String dlCutouts = request.getParam("dlCutouts");
        boolean doCutout = dlCutouts != null && dlCutouts.equalsIgnoreCase("cut");

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders = zipType != null && zipType.equalsIgnoreCase("folder");

        IpacTableParser.MappedData dgData = EmbeddedDbUtil.getSelectedMappedData(request.getSearchRequest(), selectedRows);

        if (request.getParam("ProductLevel") == null) {
            request.setParam("ProductLevel", "sci");
        }
        String subSize = request.getSafeParam("cutoutSize");
        double sizeD = StringUtils.isEmpty(subSize) ? 0 : Double.parseDouble(subSize);
        String sizeAsecStr = String.valueOf((int) (sizeD * 3600));
        String baseUrl = getBaseURL(request);


        Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();

        for (int rowIdx : selectedRows) {
            String filefracday = String.valueOf(dgData.get(rowIdx, "filefracday"));
            String field = String.valueOf(dgData.get(rowIdx, "field"));
            String filtercode = (String) dgData.get(rowIdx, "filtercode");
            String ccdid = String.valueOf(dgData.get(rowIdx, "ccdid"));
            String imgtypcode = "o";
            String qid = String.valueOf(dgData.get(rowIdx, "qid"));

            FileInfo fi = null;

            String fName = ZtfSciimsFileRetrieve.createFilepath_l1(filefracday,field,filtercode,ccdid,imgtypcode,qid, ZtfSciimsFileRetrieve.FILE_TYPE.SCI);

            File f = new File(fName);

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


            if (doCutout) {
                // look for in_ra and in_dec returned by IBE
                // if it fails, try using ra and dec
                String subLon = dgData.get(rowIdx, "in_ra") != null ? String.format("%.4f", (Double) dgData.get(rowIdx, "in_ra")) : null;
                if (StringUtils.isEmpty(subLon)) {
                    subLon = String.format("%.4f", (Double) dgData.get(rowIdx, "ra"));
                }

                String subLat = dgData.get(rowIdx, "in_dec") != null ? String.format("%.4f", (Double) dgData.get(rowIdx, "in_dec")) : null;
                if (StringUtils.isEmpty(subLat)) {
                    subLat = String.format("%.4f", (Double) dgData.get(rowIdx, "dec"));
                }
                String cutoutInfo = "_ra" + subLon + "_dec" + subLat + "_asec" + sizeAsecStr;

                extName = extName.replace(".fits",cutoutInfo+".fits");

                String url = ZtfSciimsFileRetrieve.createCutoutURLString_l1(baseUrl, filefracday,field,filtercode,ccdid,imgtypcode,qid, ZtfSciimsFileRetrieve.FILE_TYPE.SCI, subLon, subLat, subSize);
                logger.briefInfo("cutout url: " + url);
                // strip out filename when using file resolver
                fi = new FileInfo(url, extName, 0);
            } else {
                String url = baseUrl + fName;
                fi = new FileInfo(url, extName,0);
            }
            if (fi != null) {
                fi.setCookies(cookies);
                fiArr.add(fi);
                fgSize += fi.getSizeInBytes();
            }
        }

        FileGroup fg = new FileGroup(fiArr, null, fgSize, "ZTF Download Files");
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        fgArr.add(fg);
        return fgArr;
    }


    private static String getBaseURL(ServerRequest sr) throws MalformedURLException {
            // build service
            return ZtfFileRetrieve.getBaseURL(sr);

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
