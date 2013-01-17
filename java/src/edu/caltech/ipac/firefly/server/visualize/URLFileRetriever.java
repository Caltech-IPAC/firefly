package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.net.AnyUrlParams;
import edu.caltech.ipac.visualize.plot.GeomException;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
public class URLFileRetriever implements FileRetriever {

    public static final long EXPIRE_IN_SEC = 60*60*4; // 4 hours
    public static final String SECURITY_COOKIE_NAME = "JOSSO_SESSIONID"; // 4 hours
    public static final String FITS = "fits";
    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        File fitsFile;
        String urlStr= request.getURL();
        if (urlStr==null) throw new FailedRequestException("Could not find file", "request.getURL() returned null");
        if (urlStr.contains("+")) { // i think this is a hack for IRSA image that have a plus in files names as ra+dec
            try {
                urlStr= urlStr.replaceAll("\\+", URLEncoder.encode("+","UTF-8"));
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
            AnyUrlParams params= new AnyUrlParams(new URL(urlStr), request.getProgressKey());
            RequestOwner ro= ServerContext.getRequestOwner();
            Cookie cookies[]= ro.getCookies();
            if (cookies!=null) {
                for(Cookie c : cookies) {
                    params.addCookie(c.getName(), c.getValue());
                }
            }
            if (!ro.getUserInfo().isGuestUser()) {
                params.setCacheLifespanInSec(EXPIRE_IN_SEC);
                params.setLoginName(ro.getUserInfo().getLoginName());
                params.setSecurityCookie(SECURITY_COOKIE_NAME);
            }
            params.setCheckForNewer(true);
            params.setLocalFileExtensions(Arrays.asList(FileUtil.FITS,FileUtil.GZ)); //assuming WebPlotRequest ONLY expect FITS or GZ file.
            params.setMaxSizeToDownload(VisContext.FITS_MAX_SIZE);
            if (request.getUserDesc()!=null) params.setDesc(request.getUserDesc()); // set file description

            PlotServUtils.updateProgress(request, PlotServUtils.READ_PERCENT_MSG);

            fitsFile=  LockingVisNetwork.getImage(params);
        } catch (MalformedURLException e) {
            throw new FailedRequestException("Bad URL",null,e);
        }  catch (FailedRequestException e) {
            throw e;
        }  catch (Exception e) {
            throw new FailedRequestException("No data",null,e);
        }
        return new FileData(fitsFile,urlStr);
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

