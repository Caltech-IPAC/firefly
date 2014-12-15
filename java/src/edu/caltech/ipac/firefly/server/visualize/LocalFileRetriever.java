package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;
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
        String fStr= StringUtil.crunch(request.getFileName());
        if (fStr!=null) {
            Cache sessionCache= UserCache.getInstance();
            UploadFileInfo uFI= (UploadFileInfo)(sessionCache.get(new StringKey(fStr)));
            File f= VisContext.convertToFile(fStr);
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
