/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.ws.WsServerParams;
import edu.caltech.ipac.firefly.server.ws.WsServerUtils;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;

import java.io.File;
import java.io.IOException;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * @author Trey Roby
 */
@FileRetrieverImpl(id ="WORKSPACE")
public class WorkdspaceImageFileRetriever implements FileRetriever {

    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        return getFileFromWorkspace(request.getFileName());
    }

    public static FileInfo getFileFromWorkspace(String fileName) throws FailedRequestException, GeomException, SecurityException {
        String fStr= StringUtils.crunch(fileName);
        if (fStr!=null) {

            WsServerParams wsParams = new WsServerParams();
            wsParams.set(WsServerParams.WS_SERVER_PARAMS.CURRENTRELPATH, fStr);
            WsServerUtils wsUtil= new WsServerUtils();
            try {
                String s=  wsUtil.upload(wsParams);
                File f= ServerContext.convertToFile(s);
                return new FileInfo(f);
            } catch (IOException e) {
                throw new FailedRequestException("Unable to retrieve file from workspace",e);
            }
        }
        return null;
    }
}

