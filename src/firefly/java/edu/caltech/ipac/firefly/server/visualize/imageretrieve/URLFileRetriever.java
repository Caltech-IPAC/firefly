/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.ProgressStat;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.net.AnyUrlParams;
import edu.caltech.ipac.visualize.plot.GeomException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        File fitsFile;
        String urlStr = request.getURL();
        if (urlStr == null) throw new FailedRequestException("Could not find file", "request.getURL() returned null");
        if (urlStr.contains("+")) { // i think this is a hack for IRSA image that have a plus in files names as ra+dec
            try {
                urlStr = urlStr.replaceAll("\\+", URLEncoder.encode("+", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // do nothing
            }
        }
//        try {
//            urlStr= URLDecoder.decode(urlStr, "UTF-16");
//        } catch (UnsupportedEncodingException e) {
//            // do nothing, just leave reqStr
//            Logger.warn("oops");
//        }
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
                params.setCacheLifespanInSec(EXPIRE_IN_SEC);
                params.setLoginName(ro.getUserInfo().getLoginName());
                params.setSecurityCookie(ro.getRequestAgent().getAuthKey());
            }
            params.setCheckForNewer(request.getUrlCheckForNewer());
            params.setLocalFileExtensions(Arrays.asList(FileUtil.FITS, FileUtil.GZ)); //assuming WebPlotRequest ONLY expect FITS or GZ file.
            params.setMaxSizeToDownload(VisContext.FITS_MAX_SIZE);
            if (request.getUserDesc() != null) params.setDesc(request.getUserDesc()); // set file description

            PlotServUtils.updatePlotCreateProgress(request, ProgressStat.PType.READING, PlotServUtils.READ_PERCENT_MSG);

            fitsFile = LockingVisNetwork.getImage(params);
        } catch (MalformedURLException e) {
            throw new FailedRequestException("Bad URL", null, e);
        } catch (FailedRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new FailedRequestException("No data", null, e);
        }
        return new FileData(fitsFile, urlStr);
    }

}
