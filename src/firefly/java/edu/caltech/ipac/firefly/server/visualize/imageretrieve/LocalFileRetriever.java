/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;

import java.io.File;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
@FileRetrieverImpl(id ="FILE")
public class LocalFileRetriever implements FileRetriever {

    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        return getFileByName(request.getFileName());
    }

    public FileInfo getFileByName(String fileName) throws FailedRequestException, GeomException, SecurityException {
        String fStr= StringUtils.crunch(fileName);
        if (fStr!=null) {
            Cache sessionCache= CacheManager.getUserCache();
            UploadFileInfo uFI= (UploadFileInfo)(sessionCache.get(new StringKey(fStr)));
            File f= ServerContext.convertToFile(fStr);
            if (f==null || !f.canRead()) {
                f = (uFI!=null)? uFI.getFile() : null;
            }
            if (f==null) {
                if (fStr.charAt(0)==File.separatorChar) {
                    if (new File(fStr).canRead()) {
                        throw new FailedRequestException("Could read file, "+
                                fileName +
                                " is not in the path specified by " +
                                "visualize.fits.search.path in the configuration file");
                    }
                    else {
                        throw new FailedRequestException("File not found", "Could not find your requested file, "+
                                "the file: " + fileName + " is was not found");
                    }
                }
                else {
                    throw new FailedRequestException("File not found", "Could not find your requested file, "+
                            "the file: " + fileName +
                            " could not be converted to an absolute path");
                }
            }
            if (f.canRead()) {
                return new FileInfo(f, (uFI!=null && uFI.getFileName()!=null)? uFI.getFileName(): f.getName() );
            }
            else {
                throw new FailedRequestException("No read access for file",
                        "the file: " + fileName);
            }
        }
        else {
            throw new FailedRequestException("No file specified");
        }
    }
}

