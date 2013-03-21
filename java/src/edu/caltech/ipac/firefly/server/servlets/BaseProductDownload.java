package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.query.SearchProcessorFactory;
import edu.caltech.ipac.firefly.server.util.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 3/20/13
 * Time: 6:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class BaseProductDownload extends BaseHttpServlet {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String DOWNLOAD = "download";
    private static final String QUERY = "query";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        LOG.debug("Query string", req.getQueryString());
        Map origParamMap = req.getParameterMap();
        Map<String, String> paramMap = new HashMap<String,String>();
        // parameters could be upper or lower case
        for (Object p : origParamMap.keySet()) {
            if (p instanceof String) {
                paramMap.put((String)p, (((String[])origParamMap.get(p))[0]).trim());
            }
        }
        try {
            DownloadRequest downloadRequest = new DownloadRequest(getRequest(paramMap),"","");
            if (paramMap.containsKey("file_type"))downloadRequest.setParam("file_type", paramMap.get("file_type"));
            List<FileGroup>  data = (List<FileGroup>)
                    SearchProcessorFactory.getProcessor(paramMap.get(DOWNLOAD)).getData(downloadRequest);
            //sendTable(res, paramMap, dgPart);
        } catch (Exception e) {
            LOG.error(e);

        }

    }

    private TableServerRequest getRequest(Map<String, String> paramMap) {
            TableServerRequest searchReq = new TableServerRequest(paramMap.get(QUERY));
            for (Map.Entry<String, String> e: paramMap.entrySet()) {
                searchReq.setParam(e.getKey(),e.getValue());
            }
            return searchReq;
        }



}
