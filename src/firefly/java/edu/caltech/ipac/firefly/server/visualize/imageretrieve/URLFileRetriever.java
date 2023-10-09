/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.ProgressStat;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.ResponseMessage;
import edu.caltech.ipac.visualize.net.AnyUrlParams;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.isEmpty;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
@FileRetrieverImpl(id ="URL")
public class URLFileRetriever implements FileRetriever {

    public static final long EXPIRE_IN_SEC = 60 * 60 * 4; // 4 hours
    public static final String FITS = "fits";
    private static List<String> extsList=
            Arrays.asList(FileUtil.FITS, FileUtil.GZ, "tar", FileUtil.PDF, "votable", "tbl", "csv", "tsv");

    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
       return getFile(request,true);
    }

    public FileInfo getFile(WebPlotRequest request, boolean handleAllErrors) throws FailedRequestException, GeomException, SecurityException {
        FileInfo fitsFileInfo;
        String urlStr = request.getURL();
        if (urlStr == null) throw new FailedRequestException("Could not find file", "request.getURL() returned null");
        if (urlStr.toLowerCase().startsWith("file:///")) {
            return new LocalFileRetriever().getFileByName(urlStr.substring(7));
        }
        if ((urlStr.toLowerCase().contains("irsa") || urlStr.toLowerCase().contains("ceres.ipac"))) { // this is a hack for IRSA images that have a plus in files names as ra+dec
            int plusIdx= urlStr.indexOf("+");
            if (plusIdx>-1 && plusIdx+1 < urlStr.length()) { // if there is a plus and is in the form of num+num such as 4+5
                try {
                    String before= urlStr.charAt(plusIdx-1)+"";
                    String after= urlStr.charAt(plusIdx+1)+"";
                    Integer.parseInt(before);
                    Integer.parseInt(after);
                    urlStr = urlStr.replaceAll("\\+", URLEncoder.encode("+", "UTF-8"));
                } catch (NumberFormatException|UnsupportedEncodingException  ignore) {}
            }
        }
        try {
            String progressKey= !isEmpty(request.getProgressKey()) ? request.getProgressKey() : urlStr;
            AnyUrlParams params = new AnyUrlParams(new URL(urlStr), progressKey,request.getPlotId());
            RequestOwner ro = ServerContext.getRequestOwner();
            Map<String, String> cookies = ro.getCookieMap();
            if (cookies != null) {
                for (String name : cookies.keySet()) {
                    params.addCookie(name, cookies.get(name));
                }
            }
            params.setCheckForNewer(request.getUrlCheckForNewer());
            params.setLocalFileExtensions(extsList);
            params.setMaxSizeToDownload(VisContext.FITS_MAX_SIZE);
            if (request.getUserDesc() != null) params.setDesc(request.getUserDesc()); // set file description

            PlotServUtils.updateProgress(request, ProgressStat.PType.READING, PlotServUtils.READ_PERCENT_MSG);

            fitsFileInfo = LockingVisNetwork.retrieveURL(params);

            int responseCode= fitsFileInfo.getResponseCode();
            if (responseCode>=400 && handleAllErrors) {
                throw new FailedRequestException(fitsFileInfo.getResponseCodeMsg());
            }

        } catch (Exception e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        }
        fitsFileInfo.setDesc(urlStr);
        return fitsFileInfo;
    }

}
