/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
@FileRetrieverImpl(id ="TRY_FILE_THEN_URL")
public class TryFileThenURLRetriever implements FileRetriever {

    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        FileInfo retval;
        try {
            retval= new LocalFileRetriever().getFile(request);
        } catch (Exception e) {
            try {
                retval= new URLFileRetriever().getFile(request);
            } catch (Exception e1) {
                e1.initCause(e);
                throw new FailedRequestException("File not found", "Both file and URL search failed",e1);
            }
        }
        return retval;
    }
}

