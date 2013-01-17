package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.catquery.CatMasterTableQuery;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

@SearchProcessorImpl(id = "planckCatalogQuery")
public class QueryPlanck extends DynQueryProcessor {

    private static final int MAX_ERROR_LEN = 200;
    private static final String PLANCK_URL = AppProperties.getProperty("planck.url.catquery");
    private static final Logger.LoggerImpl _log = Logger.getLogger();


    private static final String ERR_START = "[struct ";
    private static final String ANY_ERR_STR = "error";
    private static final String HTML_ERR = "<html>";
    public final static String COLUMN = "column";
    private static final int STAT_IDX = 0;
    private static final int MSG_IDX = 1;


    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {

        long start = System.currentTimeMillis();
        CatalogRequest req = QueryUtil.assureType(CatalogRequest.class, request);

        String fromCacheStr = "";


        StringKey key = new StringKey(CatMasterTableQuery.class.getName(), getUniqueID(request));
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        File retFile = (File) cache.get(key);
        if (retFile == null) {
            retFile = getCatalog(req);
            cache.put(key, retFile);
        } else {
            fromCacheStr = "   (from Cache)";
        }

        long elaspe = System.currentTimeMillis() - start;
        String sizeStr = FileUtil.getSizeAsString(retFile.length());
        String timeStr = UTCTimeUtil.getHMSFromMills(elaspe);

        _log.info("catalog: " + timeStr + fromCacheStr,
                "filename: " + retFile.getPath(),
                "size:     " + sizeStr);

        return retFile;
    }


    private static File getCatalog(CatalogRequest req) throws IOException, DataAccessException {
        File retFile;
        String query = PLANCK_URL + getParams(req);
        _log.info("planck gator query: " + query);

        File outFile;
        URL url = new URL(query);
        try {
            outFile = makeFileName(req);

            URLDownload.getDataToFile(url, outFile);
            evaluateData(outFile);
            retFile = outFile;

        } catch (FailedRequestException e) {
            IOException eio = new IOException("Catalog Query Failed");
            eio.initCause(e);
            throw eio;
        } catch (MalformedURLException e) {
            IOException eio = new IOException("Catalog Query Failed - bad url");
            eio.initCause(e);
            throw eio;
        } catch (IOException e) {
            IOException eio = new IOException("Catalog Query Failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (EndUserException e) {
            DataAccessException eio = new DataAccessException("Catalog Query Failed - network Error");
            eio.initCause(e);
            throw eio;
        }
        return retFile;
    }

    private static File makeFileName(CatalogRequest req) throws IOException {
        return File.createTempFile("wise-catalog-original", ".tbl", ServerContext.getPermWorkDir());
    }

    private static String getParams(CatalogRequest req) throws UnsupportedEncodingException {
        String objStr = "";

        List<Param> paramList = req.getParams();
        for (Param p : paramList) {
            if (p.getName().startsWith("HYDRA_SRV_")) {
                objStr += "&" + p.getName().replaceFirst("HYDRA_SRV_", "") + "=" + p.getValue();
            }
        }

        if (req.containsParam("UserTargetWorldPt")) {
            String val = req.getParam("UserTargetWorldPt");
            String[] vals = val.split(";");
            objStr += "&objstr=" + URLEncoder.encode(vals[0] + " " + vals[1], "UTF-8");
        }

        if (req.containsParam("radius")) {
            objStr += "&spatial=Cone&radius=" + req.getParam("radius") + "&radunits=degree";
        } else {
            objStr += "&spatial=NONE";
        }

        objStr += "&selcols=idfile,name,frequency,ra,dec";

        return objStr;
    }


    private static void evaluateData(File outFile) throws IOException, EndUserException {
        BufferedReader fr = new BufferedReader(new FileReader(outFile), MAX_ERROR_LEN);
        String errStr = fr.readLine();
        FileUtil.silentClose(fr);
        if (errStr.startsWith(ERR_START) && errStr.charAt(errStr.length() - 1) == ']') {
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
                    "Receiving unreconized errors from Gator, Error: " + errStr);
        } else if (errStr.toLowerCase().contains(HTML_ERR) &&
                outFile.length() > MAX_ERROR_LEN) {
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unreconized errors from Gator, html send by mistake");
        } else if (errStr.toLowerCase().contains(HTML_ERR)) { // maybe send the html here
            handleErr(outFile,
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unreconized errors from Gator, html send by mistake");
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