/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.GeomException;

/**
 * @author Trey Roby
 */
public class BlankFileRetriever implements FileRetriever {

    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        return new FileData(null,"BLANK", true);
    }
}

