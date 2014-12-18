package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.plot.GeomException;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:42:15 AM
 */


/**
 * @author Trey Roby
 */
public interface FileRetriever {

    public FileData getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException;


}

