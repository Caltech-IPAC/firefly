package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.dyn.xstream.CatalogTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.dyn.DynConfigManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Date: Jun 5, 2009
 *
 * @author Trey
 * @version $Id: BaseGator.java,v 1.28 2012/08/21 21:31:12 roby Exp $
 */
public abstract class BaseGator extends DynQueryProcessor {


    private static final int MAX_ERROR_LEN = 200;


    static final String DEF_HOST = AppProperties.getProperty("irsa.gator.hostname", "irsa.ipac.caltech.edu");
//    static final String DEF_GATOR_MISSION = AppProperties.getProperty("irsa.gator.params.mission", "");
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private MultiPartPostBuilder _postBuilder = null;

    private static final String ERR_START = "[struct ";
    private static final String ANY_ERR_STR = "error";
    private static final String HTML_ERR = "<html>";
    private static final String STRANGE_HEADER_ERR = "HTTP/1.1 200 OK";
    private static final int STAT_IDX = 0;
    private static final int MSG_IDX = 1;

    @Override
    public ServerRequest inspectRequest(ServerRequest request) {
        setXmlParams(request);

        CatalogRequest req = QueryUtil.assureType(CatalogRequest.class,request.cloneRequest());
        // if hydra, check against additional catalogs
        try {
            CatalogTag cTag = null;
            String projectId = req.getParam("projectId");
            if (!StringUtils.isEmpty(projectId)) {
                String projectName = req.getParam(CatalogRequest.CATALOG_PROJECT);
                ProjectTag pTag = DynConfigManager.getInstance().getCachedProject(projectId);
                List<CatalogTag> cList = pTag.getCatalogs();
                for (CatalogTag c : cList) {
                    if (projectName.equals(c.getName())) {
                        cTag = c;

                        // set request params from configured catalog info
                        req.setGatorHost(c.getHost());
                        req.setDDOnList(false);

                        Map<String, String> paramMap = c.getSearchParams();
                        if (paramMap.size() > 0) {
                            // set additional request params with data obtained from master catalog query
                            DataGroup dGrp = null;
                                dGrp = CatMasterTableQuery.getBaseGatorData(c.getOriginalFilename());

                            DataGroupQuery.DataFilter filter =
                                    new DataGroupQuery.DataFilter("catname", DataGroupQuery.OpType.EQUALS, req.getQueryCatName());

                            ArrayList<DataObject> dataResults = new ArrayList<DataObject>();
                            CollectionUtil.filter(dGrp.values(), dataResults, filter);

                            DataObject dObj = dataResults.get(0);

                            Iterator iter = paramMap.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<String, String> entry = (Map.Entry) iter.next();
                                req.setParam(entry.getKey(), (String) dObj.getDataElement(entry.getValue()));
                            }

                            if (StringUtils.isEmpty(req.getXPFFile())) {
                                throw new DataAccessException("xpffile does not exist for this catalog!");
                            }
                        }

                        break;
                    }
                }
            }
//            if (!StringUtil.isEmpty(DEF_GATOR_MISSION) && !request.containsParam(CatalogRequest.GATOR_MISSION))  {
//                req.setGatorMission(DEF_GATOR_MISSION);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DataAccessException e) {
            e.printStackTrace();
        }
        return req;
    }

    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.CATALOGS;

    }

    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        CatalogRequest req = QueryUtil.assureType(CatalogRequest.class, request);
        return searchGator(req);
    }

    protected abstract String getDefService();

    protected String getDefHost() {
        return DEF_HOST;
    }

    protected String getParams(CatalogRequest req) throws EndUserException, IOException {
        return "";
    }

    protected void insertPostParams(CatalogRequest req) throws EndUserException, IOException {
    }

    protected File modifyData(File f, CatalogRequest req) throws Exception {
        return f;
    }

    protected abstract String getFileBaseName(CatalogRequest req) throws EndUserException;

    protected boolean isPost(CatalogRequest req) {
        return false;
    }


    private File searchGator(CatalogRequest req) throws IOException, DataAccessException {
        File outFile;
        try {
            outFile = createFile(req);
            validateRequest(req);
            if (isPost(req)) {
                URL url = createURL(req, true);
                _postBuilder = new MultiPartPostBuilder(url.toString());
                insertPostParams(req);
                BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(outFile), 10240);
                _postBuilder.post(writer);
                writer.close();

            } else {
                URL url = createURL(req, false);
                URLDownload.getDataToFile(url, outFile);
            }

            evaluateData(outFile);
            outFile = modifyData(outFile, req);

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "Catalog Query Failed - bad url");
        } catch (IOException e) {
            _log.error(e, e.toString());
            throw makeException(e, "Catalog Query Failed - network Error");
        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw makeException(e, "Catalog Query Failed - network Error");
        } catch (NoDataFoundException e) {
            _log.briefInfo("no data found for search");
            outFile = null;
        } catch (Exception e) {
            _log.error(e, e.toString());
            throw makeException(e, "Catalog Query Failed");
        }
        return outFile;
    }

    protected void validateRequest(CatalogRequest req) {}


    private URL createURL(CatalogRequest req, boolean isPost) throws EndUserException, IOException {
        String host = req.containsParam(CatalogRequest.GATOR_HOST) ? req.getGatorHost() : getDefHost();//DEF_HOST;
        String service = req.getServiceRoot();
        if (service == null) {
            service = getDefService();
        } else if (!service.startsWith("/")) {
            service += "/" + service;
        }

        String urlString = QueryUtil.makeUrlBase(host) + service;

        if (!isPost) urlString += getParams(req);

        return new URL(urlString);
    }

    private static void evaluateData(File outFile) throws IOException, EndUserException, NoDataFoundException {
        BufferedReader fr = new BufferedReader(new FileReader(outFile), MAX_ERROR_LEN);
        String errStr = fr.readLine();
        FileUtil.silentClose(fr);
        if (StringUtils.isEmpty(errStr)) {
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable",
                    "No data returned from search- the output file is zero length");
        } else if (errStr.startsWith(ERR_START) && errStr.charAt(errStr.length() - 1) == ']') {
            String s = errStr.substring(ERR_START.length(), errStr.length() - 1);
            String sParts[] = s.split(", ");

            Entry stat = getEntry(sParts[STAT_IDX]);
            Entry msg = getEntry(sParts[MSG_IDX]);
            if (stat.getName().equals("stat")) {
                handleErr(outFile,
                        "IRSA search error: " + msg.getValue(),
                        "Receiving errors from Gator, stat=" + stat.getValue());
            }

        } else if (errStr.toLowerCase().contains(ANY_ERR_STR)) {
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unrecognized errors from Gator, Error: " + errStr);
        } else if (errStr.toLowerCase().contains(HTML_ERR) &&
                outFile.length() > MAX_ERROR_LEN) {
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unrecognized errors from Gator, html send by mistake");
        } else if (errStr.toLowerCase().contains(HTML_ERR)) { // maybe send the html here
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unrecognized errors from Gator, html send by mistake");
        } else if (errStr.contains(STRANGE_HEADER_ERR) && outFile.length() < 100) { // i don't know why but it is bad
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable or results are too large to process",
                    "Receiving unrecognized errors from Gator, sending html header in the body by mistake");
        }
    }

    private static void handleErr(File f,
                                  String userErr,
                                  String moreDetail) throws EndUserException {
        f.delete();
        throw new EndUserException(userErr, moreDetail);
    }

    private static Entry getEntry(String s) {
        String sAry[] = s.split("=");
        String name = removeQuotes(sAry[0]);
        String val = removeQuotes(sAry[1]);
        return new Entry(name, val);
    }

    private static String removeQuotes(String s) {
        if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    protected static void requiredParam(StringBuffer sb, String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            requiredParam(sb, name, value + "");
        } else {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected static void requiredParam(StringBuffer sb, String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));
        } else {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }


    protected void requiredPostParam(String name, double value) throws EndUserException {
        if (!Double.isNaN(value)) {
            postParam(name, value + "");
        } else {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }


    protected void requiredPostParam(String name, String value) throws EndUserException {
        if (!StringUtils.isEmpty(value)) {
            postParam(name, value);
        } else {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }

    protected void requiredPostParam(String name, File f) throws EndUserException {
        if (f.canRead()) {
            _postBuilder.addFile(name, f);
        } else {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor could not read file: " + f.getPath());
        }
    }

    protected void requiredPostFileCacheParam(String name, String cacheID) throws EndUserException {
        boolean badParam = true;

        if (!StringUtils.isEmpty(cacheID)) {
            File uploadFile = VisContext.convertToFile(cacheID);
            if (uploadFile.canRead()) {
                requiredPostParam(name, uploadFile);
                badParam = false;
            }
        }
        if (badParam) {
            throw new EndUserException("IRSA search failed, Catalog is unavailable",
                    "Search Processor did not find the required parameter: " + name);
        }
    }


    protected static void optionalParam(StringBuffer sb, String name, String value) {
        if (!StringUtils.isEmpty(value)) {
            sb.append(param(name, value));
        }
    }


    protected static void optionalParam(StringBuffer sb, String name, boolean value) {
        sb.append(param(name, value));
    }


    protected void optionalPostParam(String name, String value) {
        if (!StringUtils.isEmpty(value)) postParam(name, value);
    }

    protected void optionalPostParam(String name, boolean value) {
        postParam(name, value ? "1" : "0");
    }


    protected final void postParam(String name, String value) {
        if (_postBuilder != null) {
            _postBuilder.addParam(name, value);
        } else {
            Assert.argTst(false, "attemping to bulid post when isPost() is returning false");
        }
    }

    protected static String param(String name, String value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, int value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, double value) {
        return "&" + name + "=" + value;
    }

    protected static String param(String name, boolean value) {
        return "&" + name + "=" + (value ? "1" : "0");
    }

    protected static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            _log.warn(e, "an exception here should never happen, using UTF-8");
            return s;
        }
    }

    private static class Entry {
        private final String _name;
        private final String _value;

        public Entry(String name, String value) {
            _name = name;
            _value = value;
        }

        public String getName() {
            return _name;
        }

        public String getValue() {
            return _value;
        }
    }

    private static class NoDataFoundException extends Exception {
        public NoDataFoundException() {
            super("no data found");
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
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
