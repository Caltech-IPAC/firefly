/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.GeomException;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
public class TryFileThenURLRetriever implements FileRetriever {

    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        FileData retval;
        try {
            retval= new LocalFileRetriever().getFile(request);
        } catch (Exception e) {
            try {
                retval= new URLFileRetriever().getFile(request);
            } catch (Exception e1) {
                e1.initCause(e);
                throw new FailedRequestException("Could not find file", "Both file and URL search failed",e1);
            }
        }
        return retval;
    }
}

