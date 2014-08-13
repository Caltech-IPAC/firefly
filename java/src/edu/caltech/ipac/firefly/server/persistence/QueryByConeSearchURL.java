package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static edu.caltech.ipac.firefly.util.DataSetParser.DESC_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * @author tatianag
 *         $Id: $
 */
@SearchProcessorImpl(id = "ConeSearchByURL", params=
        {@ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="accessUrl", desc="access URL")
        })

public class QueryByConeSearchURL extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static final String RADIUS_KEY = "radius";
    public static final String ACCESS_URL = "accessUrl";


    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.CATALOGS;
    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return "conesearch-";
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {

        long start = System.currentTimeMillis();

        String fromCacheStr = "";

        StringKey key = new StringKey(this.getClass().getName(), getUniqueID(request));
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        File retFile = (File) cache.get(key);
        if (retFile == null) {
            retFile = queryVOSearchService(request);  // all the work is done here
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

    protected String getQueryString(TableServerRequest req) throws DataAccessException {
        String accessUrl = req.getParam(ACCESS_URL);
        if (accessUrl == null) {
            throw new DataAccessException("could not find the parameter "+ACCESS_URL);
        }

        WorldPt wpt = req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        if (wpt == null) {
            throw new DataAccessException("could not find the paramater: " + ReqConst.USER_TARGET_WORLD_PT);
        }
        wpt = Plot.convert(wpt, CoordinateSys.EQ_J2000);

        double radVal = req.getDoubleParam(RADIUS_KEY);
        double radDeg = MathUtil.convert(MathUtil.Units.parse(req.getParam(CatalogRequest.RAD_UNITS), MathUtil.Units.DEGREE), MathUtil.Units.DEGREE, radVal);
        return accessUrl + "&RA=" + wpt.getLon() + "&DEC=" +wpt.getLat() + "&SR=" + radDeg;

    }

    private File queryVOSearchService(TableServerRequest req) throws IOException, DataAccessException {

        try {
            File votable = getSearchResult(getQueryString(req), getFilePrefix(req));
            DataGroup[] groups = VoTableUtil.voToDataGroups(votable.getAbsolutePath(), false);
            if (groups == null || groups.length<1) {
                throw new EndUserException("cone search query failed", "unable to convert results to data group");
            }
            DataGroup dg = groups[0];
            DataGroup.Attribute raColAttr = dg.getAttribute("POS_EQ_RA_MAIN");
            DataGroup.Attribute decColAttr = dg.getAttribute("POS_EQ_DEC_MAIN");
            if (raColAttr != null && decColAttr != null) {
                TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns((String)raColAttr.getValue(), (String)decColAttr.getValue(), CoordinateSys.EQ_J2000);
                dg.addAttributes(new DataGroup.Attribute(MetaConst.CENTER_COLUMN, llc.toString()));
            }
            if (dg.getDataDefinitions().length > 0) {
                String name, desc;
                for (DataType col : dg.getDataDefinitions()) {
                    name = col.getKeyName();
                    desc = col.getShortDesc();
                    if (desc != null) {
                        dg.addAttributes(new DataGroup.Attribute(makeAttribKey(DESC_TAG, name.toLowerCase()), desc));
                    }
                }
            }

            File outFile = createFile(req);
            IpacTableWriter.save(outFile, dg);
            return outFile;
        } catch (IOException e) {
            IOException eio = new IOException("query failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (EndUserException e) {
            DataAccessException eio = new DataAccessException("query failed - network error");
            eio.initCause(e);
            throw eio;
        } catch (Exception e) {
            DataAccessException eio = new DataAccessException("query failed - " + e.toString());
            eio.initCause(e);
            throw eio;
        }

    }

    private File getSearchResult(String urlQuery, String filePrefix) throws IOException, DataAccessException, EndUserException {

        URL url;
        try {
            url = new URL(urlQuery);
        } catch (MalformedURLException e) {
            _log.error(e, e.toString());
            throw new EndUserException("query failed - bad url: "+urlQuery, e.toString());
        }
        StringKey cacheKey = new StringKey(url);
        File f = (File) getCache().get(cacheKey);
        if (f != null && f.canRead()) {
            return f;
        } else {
            URLConnection conn = null;

            //File outFile = createFile(req, ".xml");
            File outFile = File.createTempFile(filePrefix, ".xml", ServerContext.getPermWorkDir());
            try {
                conn = URLDownload.makeConnection(url);
                conn.setRequestProperty("Accept", "*/*");

                URLDownload.getDataToFile(conn, outFile);
                getCache().put(cacheKey, outFile, 60 * 60 * 24);    // 1 day

            } catch (MalformedURLException e) {
                _log.error(e, "Bad URL");
                throw makeException(e, "query failed - bad url.");

            } catch (IOException e) {
                _log.error(e, e.toString());
                if (conn != null && conn instanceof HttpURLConnection) {
                    HttpURLConnection httpConn = (HttpURLConnection) conn;
                    int respCode = httpConn.getResponseCode();
                    if (respCode == 400 || respCode == 404 || respCode == 500) {
                        InputStream is = httpConn.getErrorStream();
                        if (is != null) {
                            String msg = parseMessageFromServer(DynServerUtils.convertStreamToString(is));
                            throw new EndUserException("query failed: " + msg, msg);

                        } else {
                            String msg = httpConn.getResponseMessage();
                            throw new EndUserException("query failed: " + msg, msg);
                        }
                    }

                } else {
                    throw makeException(e, "query failed - network error.");
                }

            } catch (Exception e) {
                throw makeException(e, "query failed");
            }
            return outFile;
        }

    }


    private String parseMessageFromServer(String response) {
        // no html, so just return
        return response.replaceAll("<br ?/?>", "");
    }

       /*
        * In the case of error, the service MUST respond with a VOTable
        * that contains a single PARAM element or a single INFO element
        * with name="Error", where the corresponding value attribute
        * should contain some explanation of the nature of the error.
        * If an INFO element is used, it must appear as a direct child
        * of one of the following elements:
        * the root VOTABLE element (which is preferred by this document),
        * or the RESOURCE element
        * If a PARAM element is used, it must appear as a direct child
        * of one of following elements:
        * the RESOURCE element, or
        * a DEFINITION element below the root VOTABLE element (which is discouraged by this document).
        */

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

