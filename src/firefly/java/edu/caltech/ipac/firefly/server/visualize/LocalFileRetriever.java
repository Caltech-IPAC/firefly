/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.GeomException;

import java.io.File;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
public class LocalFileRetriever implements FileRetriever {

    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        String fStr= StringUtils.crunch(request.getFileName());
        if (fStr!=null) {
            Cache sessionCache= UserCache.getInstance();
            UploadFileInfo uFI= (UploadFileInfo)(sessionCache.get(new StringKey(fStr)));
            File f= ServerContext.convertToFile(fStr);
            if (f==null || !f.canRead()) {
                f = (uFI!=null)? uFI.getFile() : null;
            }
            if (f==null) {
                if (fStr.charAt(0)==File.separatorChar) {
                    throw new FailedRequestException("Could not find your requested file, "+
                                                     "the file: " + request.getFileName() +
                                                             " is not in the path specified by " +
                                                             "visualize.fits.search.path"+
                                                             " in the configuration file");
                }
                else {
                    throw new FailedRequestException("Could not find your requested file, "+
                                                     "the file: " + request.getFileName() +
                                                             " could not be converted to an absolute path");
                }
            }
            if (f.canRead()) {
                return new FileData(f, (uFI!=null && uFI.getFileName()!=null)? uFI.getFileName(): f.getName() );
            }
            else {
                throw new FailedRequestException("Could not read ",
                                                 "the file: " + request.getFileName());
            }
        }
        else {
            throw new FailedRequestException("You did not requests a file, request.getFileName() is returning null");
        }
    }
}

