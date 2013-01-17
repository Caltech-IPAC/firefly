package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


@SearchProcessorImpl(id = "2MassQueryArtifact", params=
        {@ParamDoc(name=Query2MassArtifact.PERS_ART,  desc="url of persistence artifacts"),
         @ParamDoc(name=Query2MassArtifact.GLINT_ART, desc="url of glint artifacts"),
         @ParamDoc(name=Query2MassArtifact.TYPE,      desc="glint or pers")
        })
public class Query2MassArtifact extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static final String PERS_ART = "pers_art";
    public static final String GLINT_ART= "glint_art";
    public static final String TYPE     = "type";
    public static final String TYPE_PERS= "pers";
    public static final String TYPE_GLINT= "glint";

    @Override
    public boolean doCache() {
        return true;
    }

    @Override
    public File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        long start = System.currentTimeMillis();

        String fromCacheStr = "";

        File retFile = getArtifact(req);  // all the work is done here

        long elaspe = System.currentTimeMillis() - start;
        String sizeStr = FileUtil.getSizeAsString(retFile.length());
        String timeStr = UTCTimeUtil.getHMSFromMills(elaspe);

        _log.info("2mass artifact: " + timeStr + fromCacheStr,
                "filename: " + retFile.getPath(),
                "size:     " + sizeStr);

        return retFile;
    }


    private static File getArtifact(TableServerRequest req) throws IOException, DataAccessException {
        String type= req.getParam(TYPE);

        String urlStr= null;
        if (type.equals(TYPE_PERS)) {
            urlStr= requiredParam(req,PERS_ART);
        }
        else if (type.equals(TYPE_GLINT)) {
            urlStr= requiredParam(req,GLINT_ART);
        }


        URL url = new URL(urlStr);
        File outFile;
        try {
            String data = URLDownload.getStringFromURL(url, null);
            Query2Mass.evaluateData(data);

            DataGroup dataGroup = Query2Mass.voToDataGroup(data);
            outFile = makeFileName(req);
            IpacTableWriter.save(outFile, dataGroup);


        } catch (FailedRequestException e) {
            IOException eio = new IOException("2mass Query Failed");
            eio.initCause(e);
            throw eio;
        } catch (MalformedURLException e) {
            IOException eio = new IOException("2mass Query Failed - bad url");
            eio.initCause(e);
            throw eio;
        } catch (IOException e) {
            IOException eio = new IOException("2mass Query Failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (EndUserException e) {
            DataAccessException eio = new DataAccessException("2mass Query Failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (Exception e) {
            DataAccessException eio = new DataAccessException("2mass Query Failed - " + e.toString());
            eio.initCause(e);
            throw eio;
        }
        return outFile;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        meta.setCenterCoordColumns(new TableMeta.LonLatColumns("ra","dec", CoordinateSys.EQ_J2000));
        super.prepareTableMeta(meta, columns, request);
    }

    private static File makeFileName(TableServerRequest req) throws IOException {
        return File.createTempFile("2mass-artifact-", ".tbl", ServerContext.getPermWorkDir());
    }

    private static String requiredParam(TableServerRequest req, String param) throws DataAccessException{
        if (!req.containsParam(param)) {
            throw new DataAccessException("could not find the parameter: " + param);
        }
        return req.getParam(param);
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

