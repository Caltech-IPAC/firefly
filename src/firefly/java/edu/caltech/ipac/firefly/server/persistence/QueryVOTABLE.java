/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynServerUtils;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

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
public abstract class QueryVOTABLE extends IpacTablePartProcessor {

    public static final String TITLE_KEY = "title";
    public static final String USE_KEY = "use";

    private static final Logger.LoggerImpl _log = Logger.getLogger();


    @Override
    public boolean doCache() {
        return true;
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
            return queryVOSearchService(request);  // all the work is done here
    }

    protected abstract String getQueryString(TableServerRequest req) throws DataAccessException;

    private File queryVOSearchService(TableServerRequest req) throws IOException, DataAccessException {

        try {
            File votable = getSearchResult(getQueryString(req), getFilePrefix(req));
            DataGroup[] groups = VoTableUtil.voToDataGroups(votable.getAbsolutePath(), false);
            DataGroup dg;
            if (groups.length<1) {
                dg = new DataGroup("empty",new DataType[]{new DataType("empty", String.class)});
                //throw new EndUserException("cone search query failed", "no results");
            } else {
                dg = groups[0];
            }
            DataGroup.Attribute raColAttr = dg.getAttribute("POS_EQ_RA_MAIN");
            DataGroup.Attribute decColAttr = dg.getAttribute("POS_EQ_DEC_MAIN");
            String use = req.getParam(USE_KEY); // tells how the table will be used - DM-6902
            if (raColAttr != null && decColAttr != null) {
                TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns(raColAttr.getValue(), decColAttr.getValue(), CoordinateSys.EQ_J2000);
                dg.addAttribute(MetaConst.CENTER_COLUMN, llc.toString());

                if (use != null && use.startsWith("catalog")) {
                    dg.addAttribute(MetaConst.CATALOG_COORD_COLS, llc.toString());
                    String title = req.getParam(TITLE_KEY);
                    dg.addAttribute(MetaConst.CATALOG_OVERLAY_TYPE, title == null ? "VO Catalog" : title);
                }
            }
            if (use != null) {
                if (use.equals("catalog_overlay")) {
                    dg.addAttribute(MetaConst.DATA_PRIMARY, "False");
                } else if (use.equals("data_primary") || use.equals("catalog_primary")) {
                    dg.addAttribute(MetaConst.DATA_PRIMARY, "True");
                }
            }

            if (dg.getDataDefinitions().length > 0) {
                String name, desc;
                for (DataType col : dg.getDataDefinitions()) {
                    name = col.getKeyName();
                    desc = col.getShortDesc();
                    if (desc != null) {
                        dg.addAttribute(makeAttribKey(DESC_TAG, name.toLowerCase()), desc);
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
        URLConnection conn = null;

        //File outFile = createFile(req, ".xml");
        File outFile = File.createTempFile(filePrefix, ".xml", ServerContext.getPermWorkDir());
        try {
            conn = URLDownload.makeConnection(url);
            conn.setRequestProperty("Accept", "*/*");

            URLDownload.getDataToFile(conn, outFile);

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


    private String parseMessageFromServer(String response) {
        // no html, so just return
        return response.replaceAll("<br ?/?>", "");
    }

}
