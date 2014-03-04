package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.wise.WiseFileRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@SearchProcessorImpl(id = "WiseQueryArtifact", params = {
        @ParamDoc(name = WiseRequest.HOST, desc = "(optional) the hostname, including port"),
        @ParamDoc(name = WiseRequest.SCHEMA_GROUP, desc = "the name of the schema group"),
        @ParamDoc(name = WiseRequest.SCHEMA, desc = "the name of the schema within the schema group"),
        @ParamDoc(name = WiseRequest.SCAN_GROUP, desc = "the scan group"),
        @ParamDoc(name = WiseRequest.SCAN_ID, desc = "the scan id of the scan group"),
        @ParamDoc(name = WiseRequest.FRAME_NUM, desc = "the frame number of the scan"),
        @ParamDoc(name = WiseRequest.COADD_ID, desc = "the coadd id of the level 3 image"),
        @ParamDoc(name = WiseRequest.BAND, desc = "the band number"),
        @ParamDoc(name = WiseRequest.TYPE, desc = "the artifact type.  values: D (diffraction spikes), H (halos), O (optical ghosts), P (latents)")
})
public class QueryWiseArtifact extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    @Override
    public boolean doCache() {
        return true;
    }

    @Override
    public File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        long start = System.currentTimeMillis();

        String fromCacheStr = "";

        File retFile = getArtifact(req);  // all the work is done here

        if (retFile != null) {
            long elaspe = System.currentTimeMillis() - start;
            String sizeStr = FileUtil.getSizeAsString(retFile.length());
            String timeStr = UTCTimeUtil.getHMSFromMills(elaspe);

            _log.info("WISE artifact: " + timeStr + fromCacheStr,
                    "filename: " + retFile.getPath(),
                    "size:     " + sizeStr);
        }

        return retFile;
    }

    private static File getArtifact(TableServerRequest req) throws IOException, DataAccessException {
        String t = req.getSafeParam(WiseRequest.TYPE);
        WiseFileRetrieve.IMG_TYPE type = null;
        if (t.equalsIgnoreCase("D")) {
            type = WiseFileRetrieve.IMG_TYPE.DIFF_SPIKES;
        } else if (t.equalsIgnoreCase("H")) {
            type = WiseFileRetrieve.IMG_TYPE.HALOS;
        } else if (t.equalsIgnoreCase("O")) {
            type = WiseFileRetrieve.IMG_TYPE.OPT_GHOSTS;
        } else if (t.equalsIgnoreCase("P")) {
            type = WiseFileRetrieve.IMG_TYPE.LATENTS;
        }

        String retrievalType = WiseFileRetrieve.WISE_DATA_RETRIEVAL_TYPE;
        if (retrievalType.equalsIgnoreCase("filesystem")) {
            String baseFilename = WiseFileRetrieve.WISE_FILESYSTEM_BASEPATH;
            if (baseFilename == null || baseFilename.length() == 0) {
                // if not configured, default to URL retrieval
                retrievalType = "url";

            } else {
                String fileName = WiseFileRetrieve.getFilename(req, type);

                File f = new File(fileName);
                if (f != null && f.exists()) {
                    _log.info("retrieving local file:" + fileName);
                    return f;

                } else {
                    fileName += ".gz";
                    f = new File(fileName);
                    if (f != null && f.exists()) {
                        _log.info("retrieving local file:" + fileName);
                        return f;

                    } else {
                        retrievalType = "url";
                    }
                }
            }
        }

        if (retrievalType.equalsIgnoreCase("url")) {
            URL url = WiseFileRetrieve.getURL(req, type);
            if (url == null) throw new MalformedURLException("computed url is null");

            // set authenticator for password-protected http requests
            if (WiseFileRetrieve.USE_HTTP_AUTHENTICATOR) {
                WiseFileRetrieve.setAuthenticator();
            }

            File outFile;
            try {
                HttpURLConnection artConn = (HttpURLConnection)URLDownload.makeConnection(url);
                artConn.setRequestProperty("Accept", "text/plain");

                // quick test to see if url is valid
                URLDownload.getSugestedFileName(artConn);
                int respCode = artConn.getResponseCode();
                if (respCode == 200) {
                    outFile = makeFileName(req);
                    _log.info("retrieving URL:" + url.toString());
                    URLDownload.getDataToFile(artConn, outFile);

                } else {
                    outFile = null;
                    _log.info("Failed to find artifact URL:" + url.toString());
                }

            } catch (MalformedURLException e) {
                IOException eio = new IOException("WISE Query Failed - bad url");
                eio.initCause(e);
                throw eio;
            } catch (IOException e) {
                IOException eio = new IOException("WISE Query Failed - network Error");
                eio.initCause(e);
                throw eio;
            } catch (Exception e) {
                DataAccessException eio = new DataAccessException("WISE Query Failed - " + e.toString());
                eio.initCause(e);
                throw eio;
            }

            // remove authenticator from http requests
            if (WiseFileRetrieve.USE_HTTP_AUTHENTICATOR) {
                WiseFileRetrieve.removeAuthenticator();
            }

            return outFile;
        }

        return null;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        meta.setCenterCoordColumns(new TableMeta.LonLatColumns("ra", "dec", CoordinateSys.EQ_J2000));
        super.prepareTableMeta(meta, columns, request);
    }

    private static File makeFileName(TableServerRequest req) throws IOException {
        return File.createTempFile("WISE-artifact-", ".tbl", ServerContext.getPermWorkDir());
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
