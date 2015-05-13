/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;
/**
 * User: roby
 * Date: 3/13/12
 * Time: 3:49 PM
 */


import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynConfigManager;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.TableResultSearch;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartData;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ShowResult extends BaseHttpServlet {

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String BASE_URL= AppProperties.getProperty("ResultViewer.baseURL", "http://localhost:8080/");
    private static final String TABLE_LOAD_URL = BASE_URL + "applications/resultViewer/Hydra.html#projectId=resultViewer" +
                                                     "&id=Hydra_resultViewer_UploadResultsSearch" +
                                                     "&searchName=UploadResultsSearch" +
                                                     "&isSearchResult=true" +
                                                     "&DoSearch=true";
    private static final boolean DO_DBG= false;
    private static final String DBG_STR = DO_DBG ? "?gwt.codesvr=134.4.61.149:9997" : "";
    private static final String FITS_LOAD_URL = BASE_URL + "applications/resultViewer/Hydra.html" +
                                                           DBG_STR +
                                                           "#projectId=resultViewer" +
                                                           "&id=FitsInput" +
                                                           "&isSearchResult=true" +
                                                           "&VisOnly=t" +
                                                           "&DoPlot=t";

    private static final String TABLE_URL= "tableURL";
    private static final String TABLE_FILE= "tableFile";
//    private final String TABLE_LOAD_URL= BASE_URL + "applications/resultViewer/Hydra.html";


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        boolean image= false;
        File f= null;
        MultiPartData data= null;
        try {
            data= parseParams(req);
            TableResultSearch.addToCache(data);
            TableServerRequest sr= getPopulatedRequest(data.getCacheKey().toString());
            TableResultSearch.addSearchParmsToData(sr,false);
            String smallIconUrl= findSmallIcon(data);
            String url;
            String cmd= data.getParams().get(ServerParams.COMMAND);
            if (cmd!=null && cmd.equals(ServerParams.PLOT_EXTERNAL)) {
                url= makePlotURL(data, smallIconUrl);
                image= true;
            }
            else if (data.getParams().containsKey(CommonParams.DO_PLOT)) { // deprecated
                url= makePlotURLOldStyle(data,smallIconUrl);
                image= true;
            }
            else {
                f= findTable(data);
                String fileParam= ServerContext.replaceWithPrefix(f);
                url= makeTableURL(data.getCacheKey().toString(), fileParam, smallIconUrl);
            }


            res.setContentType("text/html");
            PrintStream out = new PrintStream(res.getOutputStream());
            writeTransferHTML(out, url);
            out.close();

            // do log after we have close the transfer
            if (image) logImageRequest(data, url);
            else logTableRequest(data, f, url);

        } catch (Exception e) {
            sendReturnMsg(res, 500, e.getMessage(), null);
            logFail(data,req,e);
        }

    }

    private static MultiPartData parseParams(HttpServletRequest req) throws Exception {
        MultiPartData data;
        StringKey key= new StringKey("ShowResult", System.currentTimeMillis());
        if (req.getContentType()==null) {
            data =  new MultiPartData(key);
            Enumeration<String> e= req.getParameterNames();
            String name;
            while(e.hasMoreElements()) {
                name= e.nextElement();
                data.addParam(name, req.getParameter(name));
            }
        }
        else {
            data= MultipartDataUtil.handleRequest(key, req);
        }
        return data;
    }

    private static String makeTableURL(String key, String fileParam, String smallIcon) {
        String retval= TABLE_LOAD_URL + "&"+ CommonParams.CACHE_KEY +"="+key +
                               "&"+ TableResultSearch.LOAD_TABLE+"="+ fileParam;
        if (smallIcon!=null) {
            retval+= "&"+LayoutManager.SMALL_ICON_REGION+"="+smallIcon;
        }
        return retval;
    }

    private static String makePlotURL(MultiPartData data,String smallIcon) {
        String baseURL= FITS_LOAD_URL + "&"+ CommonParams.CACHE_KEY +"="+data.getCacheKey().toString();
        Map<String,String> params= data.getParams();
        if (params.containsKey(ServerParams.REQUEST)) {
            baseURL+= "&"+ ServerParams.REQUEST+"="+params.get(ServerParams.REQUEST);
        }
        StringBuilder url= new StringBuilder(500);
        url.append(baseURL);
        if (smallIcon!=null) {
            url.append("&").append(LayoutManager.SMALL_ICON_REGION).append("=").append(smallIcon);
        }
        return url.toString();

    }

    private static String makePlotURLOldStyle(MultiPartData data,String smallIcon) {
        String baseURL= FITS_LOAD_URL + "&"+ CommonParams.CACHE_KEY +"="+data.getCacheKey().toString();
        Map<String,String> params= data.getParams();
        if (params.containsKey(CommonParams.RESOLVE_PROCESSOR)) {
            baseURL+= "&"+ CommonParams.RESOLVE_PROCESSOR+"="+params.get(CommonParams.RESOLVE_PROCESSOR);
        }

        StringBuilder url= new StringBuilder(500);
        url.append(baseURL);
        url.append("&").append(LayoutManager.SMALL_ICON_REGION).append("=").append(smallIcon);
        for(String k: WebPlotRequest.getAllKeys()) {
            if (params.containsKey(k)) {
                url.append("&").append(k).append("=").append(params.get(k));
            }
        }
        return url.toString();
    }

    private void writeTransferHTML(PrintStream out, String url) {

        String transFunc= "function TransferToURL(){  window.location=\"" + url + "\";}";
        String body= "<BODY onLoad=\"TransferToURL()\"></BODY>";
        out.print("<!doctype html>");
        out.print("<html>");
        out.print("<head>");
        out.print("<script type=\"text/javascript\">");
        out.print(transFunc);
        out.print("</script>");
        out.print("</head>");
        out.print(body);
        out.print("</html>");

    }

    private String findSmallIcon(MultiPartData data) {
        Map<String,String> params= data.getParams();
        String retval= null;
        if (params.containsKey(DynUtils.HYDRA_PROJECT_ID)) {
            String projectId = params.get(DynUtils.HYDRA_PROJECT_ID);
            ProjectTag obj = DynConfigManager.getInstance().getCachedProject(projectId);
            if (obj!=null) {
                retval= obj.getPropertyValue(LayoutManager.SMALL_ICON_REGION);
            }
        }
        return retval;
    }

    private void logTableRequest(MultiPartData data, File tableFile, String url) {

        List<String> outList= new ArrayList<String>(20);
        outList.add("ShowResult table load call results:");
        outList.add("  Table File: "+tableFile.getPath());
        populateLog(data,url,outList);
        LOG.info(outList.toArray(new String[outList.size()]));
    }

    private void logImageRequest(MultiPartData data, String url) {

        List<String> outList= new ArrayList<String>(20);
        outList.add("ShowResult FITS view call results:");
        populateLog(data,url, outList);
        LOG.info(outList.toArray(new String[outList.size()]));
    }

    private void logFail(MultiPartData data, HttpServletRequest req, Exception e) {

        List<String> outList= new ArrayList<String>(20);
        outList.add("ShowResult call results failed:");
        Map<String, String[]> map = req.getParameterMap();
        String s;
        String vAry[];
        for(Map.Entry<String, String[]> entry : map.entrySet()) {
            outList.add("  URL params from user:");
            s= "    -- "+ entry.getKey()+ " : ";
            vAry= entry.getValue();
            for(int i=0; i<vAry.length; i++) {
                s+=vAry[i];
                if (vAry.length<i-1) s+=",";
            }
            outList.add(s);
        }
        populateLog(data, null, outList);
        LOG.warn(e,outList.toArray(new String[outList.size()]));
    }

    private void populateLog(MultiPartData data, String url,List<String> outList) {

        String s;
        if (url!=null) outList.add("  URL: "+url);
        if (data!=null) {
            if (data.getParams().size()>0) outList.add("  Params:");
            for(Map.Entry<String,String> entry : data.getParams().entrySet()) {
                s= "    -- "+ entry.getKey()+ " : "+ entry.getValue();
                outList.add(s);
            }
            if (data.getFiles().size()>0) outList.add("  File:");
            for(UploadFileInfo flInfo : data.getFiles()) {
                s= "    -- "+ flInfo.getPname()+ " : "+ flInfo.getFile();
                outList.add(s);
            }
        }
    }

    private static File findTable(MultiPartData data) throws IOException{
        File f = File.createTempFile("uploadTable-", "."+FileUtil.TBL,
                                     ServerContext.getPermWorkDir());
        try {
            Map<String,String> params= data.getParams();
            if (data.getFiles().size()>0) {
                FileUtil.copyFile(data.getFiles().get(0).getFile(), f);
            }
            else if (params.containsKey(TABLE_URL)) {
                URL url= new URL(params.get(TABLE_URL));
                URLDownload.getDataToFile(url, f, false);
            }
            else if (params.containsKey(TABLE_FILE)) {
                File fromFile= ServerContext.convertToFile(params.get(TABLE_FILE), true);
                FileUtil.copyFile(fromFile,f);
            }
        } catch (FailedRequestException e) {
            throw new IOException("Could not retrieve file",e);
        }
        return f;
    }

    private TableServerRequest getPopulatedRequest(String key) {
        TableServerRequest sr= new TableServerRequest("x");
        sr.setParam(CommonParams.CACHE_KEY,key);
        sr.setParam(DynUtils.HYDRA_PROJECT_ID, "resultViewer");
        sr.setParam(DynUtils.SEARCH_NAME,"UploadResultsSearch");
        sr.setParam(DynUtils.QUERY_ID, "tableResults");
        DynQueryProcessor.setXmlParams(sr);
        return sr;
    }

}

