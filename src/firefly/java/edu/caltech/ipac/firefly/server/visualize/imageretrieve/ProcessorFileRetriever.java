/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.GeomException;

import java.io.File;
import java.net.SocketException;

/**
 * @author tatianag
 *         $Id: ProcessorFileRetriever.java,v 1.8 2012/07/27 22:23:29 tatianag Exp $
 */
public class ProcessorFileRetriever implements FileRetriever {
    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        try {
            TableServerRequest sreq;
            String reqStr = request.getParam(ServerParams.REQUEST);
            if (reqStr != null) {
                sreq = TableServerRequest.parse(reqStr);
            } else {
                sreq= QueryUtil.assureType(TableServerRequest.class, request);
            }
//            sreq= makeDataOnlyRequestString(sreq);
            FileInfo fi = new SearchManager().getFileInfo(sreq);
            if (fi == null) {
                throw new FailedRequestException("Unable to get file location info");
            }
            if (fi.getInternalFilename()== null) {
                throw new FailedRequestException("File not available");
            }
            if (!fi.hasAccess()) {
                throw new SecurityException("Access is not permitted.");
            }

            int responseCode= fi.getResponseCode();
            if (responseCode>=300) {
                throw new FailedRequestException(fi.getResponseCodeMsg());
            }
            File f= new File(fi.getInternalFilename());
            return new FileInfo(f, f.getName());
        } catch (DataAccessException dae) {
            if (dae.getCause() instanceof SocketException) {
                throw new FailedRequestException("FITS URL server unavailable", "Details in exception", dae);
            }
            else {
                throw new FailedRequestException(dae.getMessage(), dae.getMessage(), dae);
            }
        } catch (FailedRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new FailedRequestException("Unable to get file location info", e.getMessage(), e);
        }
    }




    private TableServerRequest makeDataOnlyRequestString(TableServerRequest r) {
        TableServerRequest newR= new TableServerRequest();
        newR.copyFrom(r);
        r.setParam(WebPlotRequest.ZOOM_TO_WIDTH, "");
        r.setParam(WebPlotRequest.ZOOM_TO_HEIGHT, "");
        r.setParam(WebPlotRequest.PROGRESS_KEY, "");
        r.setParam(WebPlotRequest.PLOT_ID, "");
        r.setParam(WebPlotRequest.INIT_RANGE_VALUES, "");
        r.setParam(WebPlotRequest.INIT_COLOR_TABLE, "");
        return r;
    }

}

