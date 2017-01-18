/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.GeomException;

/**
 * @author Trey Roby
 */
public interface FileRetriever {

    FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException;
}

