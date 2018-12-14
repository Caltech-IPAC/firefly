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
import java.util.Map;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
public class URLFileRetriever implements FileRetriever {

    public static final long EXPIRE_IN_SEC = 60 * 60 * 4; // 4 hours
    public static final String FITS = "fits";

    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        FileInfo fitsFileInfo;
        String urlStr = request.getURL();
        if (urlStr.toLowerCase().startsWith("file:///")) {
            return new LocalFileRetriever().getFileByName(urlStr.substring(7));
        }
        if (urlStr == null) throw new FailedRequestException("Could not find file", "request.getURL() returned null");
        if (urlStr.contains("+")) { // i think this is a hack for IRSA image that have a plus in files names as ra+dec
            try {
                urlStr = urlStr.replaceAll("\\+", URLEncoder.encode("+", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // do nothing
            }
        }
        try {
            AnyUrlParams params = new AnyUrlParams(new URL(urlStr), request.getProgressKey(),request.getPlotId());
            RequestOwner ro = ServerContext.getRequestOwner();
            Map<String, String> cookies = ro.getCookieMap();
            if (cookies != null) {
                for (String name : cookies.keySet()) {
                    params.addCookie(name, cookies.get(name));
                }
            }
            if (!ro.getUserInfo().isGuestUser()) {
                params.setLoginName(ro.getUserInfo().getLoginName());
                params.setSecurityCookie(ro.getRequestAgent().getAuthKey());
            }
            params.setCheckForNewer(request.getUrlCheckForNewer());
            params.setLocalFileExtensions(Arrays.asList(FileUtil.FITS, FileUtil.GZ)); //assuming WebPlotRequest ONLY expect FITS or GZ file.
            params.setMaxSizeToDownload(VisContext.FITS_MAX_SIZE);
            if (request.getUserDesc() != null) params.setDesc(request.getUserDesc()); // set file description

            PlotServUtils.updatePlotCreateProgress(request, ProgressStat.PType.READING, PlotServUtils.READ_PERCENT_MSG);

            fitsFileInfo = LockingVisNetwork.retrieveURL(params);

            int responseCode= fitsFileInfo.getResponseCode();
            if (responseCode>=400) {
                throw new FailedRequestException(fitsFileInfo.getResponseCodeMsg());
            }

        } catch (Exception e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        }
        fitsFileInfo.setDesc(urlStr);
        return fitsFileInfo;
    }

}
