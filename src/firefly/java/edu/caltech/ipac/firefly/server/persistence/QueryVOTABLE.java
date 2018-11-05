/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static edu.caltech.ipac.table.TableMeta.DESC_TAG;
import static edu.caltech.ipac.table.TableMeta.makeAttribKey;

/**
 * @author tatianag
 *         $Id: $
 */
public abstract class QueryVOTABLE extends IpacTablePartProcessor {

    public static final String TITLE_KEY = "title";
    public static final String USE_KEY = "use";

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        try {
            File votable = getSearchResult(getQueryString(req), getFilePrefix(req));
            DataGroup[] groups = VoTableReader.voToDataGroups(votable.getAbsolutePath(), false);
            DataGroup dg;
            if (groups.length < 1) {
                dg = new DataGroup("empty",new DataType[]{new DataType("empty", String.class)});
                //throw new EndUserException("cone search query failed", "no results");
            } else {
                dg = groups[0];
            }
            String raColAttr = dg.getAttribute("POS_EQ_RA_MAIN");
            String decColAttr = dg.getAttribute("POS_EQ_DEC_MAIN");
            String use = req.getParam(USE_KEY); // tells how the table will be used - DM-6902
            if (raColAttr != null && decColAttr != null) {
                TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns(raColAttr, decColAttr, CoordinateSys.EQ_J2000);
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
                    desc = col.getDesc();
                    if (desc != null) {
                        dg.addAttribute(makeAttribKey(DESC_TAG, name.toLowerCase()), desc);
                    }
                }
            }
            return dg;
        } catch (IOException | EndUserException e) {
            DataAccessException eio = new DataAccessException("query failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (Exception e) {
            DataAccessException eio = new DataAccessException("query failed - " + e.toString());
            eio.initCause(e);
            throw eio;
        }
    }

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        return loadDataFileImpl(request);
    }

    protected abstract String getQueryString(TableServerRequest req) throws DataAccessException;

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
            throw makeException(e, "query failed - network error.");

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
