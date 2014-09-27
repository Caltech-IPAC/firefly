package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.FileRetrieverFactory;
import edu.caltech.ipac.firefly.server.visualize.ServiceRetriever;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Mar 30, 2012
 * Time: 2:37:32 PM
 * To change this template use File | Settings | File Templates.
 */

@SearchProcessorImpl(id = "FinderChartQueryArtifact", params = {
        @ParamDoc(name = WiseRequest.HOST, desc = "(optional) the hostname, including port"),
        @ParamDoc(name = WiseRequest.SCHEMA_GROUP, desc = "the name of the schema group"),
        @ParamDoc(name = "UserTargetWorldPt", desc = "the target (in WorldPt)"),
        @ParamDoc(name = "subsize", desc = "the radius"),
        @ParamDoc(name = WiseRequest.SCHEMA, desc = "the name of the schema within the schema group")
})

public class QueryFinderChartArtifact extends DynQueryProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String RA = "ra";
    private static final String DEC = "dec";



    @Override
    protected File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {
        long start = System.currentTimeMillis();

        //it happens while AbstractDatasetQueryWorker handling onRowHighlightChange event
//        if (!req.containsParam("UserTargetWorldPt") && !req.containsParam("subsize")) {
//            _log.debug("Warning: TableServerRequest does not contain UserTargetWorldPt and subsize.");
//            return null;
//        }

        if (!req.containsParam("UserTargetWorldPt")) {
            if (req.containsParam("ra") && req.containsParam("dec")) {
                WorldPt wp= new WorldPt(Double.parseDouble(req.getParam("ra")),
                                        Double.parseDouble(req.getParam("dec"))
                                        );
                req= new TableServerRequest(req.getRequestId(),req);
                req.setParam("UserTargetWorldPt", wp);
            }
        }

        String fromCacheStr = "";
        StringKey key = new StringKey(QueryFinderChartArtifact.class.getName(), getUniqueID(req));
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        File retFile = (File) cache.get(key);

        if (retFile == null) {
            retFile = getFinderChartArtifact(req);
            cache.put(key, retFile);
        } else {
            fromCacheStr = "   (from Cache)";
        }
        if (retFile!=null) {
            long elaspe = System.currentTimeMillis() - start;
            String sizeStr = FileUtil.getSizeAsString(retFile.length());
            String timeStr = UTCTimeUtil.getHMSFromMills(elaspe);

            _log.info("catalog: " + timeStr + fromCacheStr,
                    "filename: " + retFile.getPath(),
                    "size:     " + sizeStr);
            
        }

        return retFile;

    }

    public File getFinderChartArtifact(TableServerRequest req) throws IOException, DataAccessException {
        String service = req.getParam("service");

        if (service==null) throw new IOException("Missing service param in xml file.");

        File retFile = null;
        if (service.equalsIgnoreCase("wise")) {
            retFile = getWiseArtifact(req);
        } else if (service.equalsIgnoreCase("2mass")) {
            retFile = get2MassArtifact(req);
        } else {
            //throw new DataAccessException(
            //        "getFinderChartArtifact() unable to process TableServerRequest with service="+service);
            _log.error("getFinderChartArtifact() unable to process TableServerRequest with service="+service);
        }

        return retFile;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        TableMeta.LonLatColumns llc;

        String lonCol = null, latCol = null;
        for (DataType col : columns) {

            if (col.getKeyName().equalsIgnoreCase(RA)) lonCol = col.getKeyName();
            if (col.getKeyName().equalsIgnoreCase(DEC)) latCol = col.getKeyName();

            if (!StringUtils.isEmpty(lonCol) && !StringUtils.isEmpty(latCol)) {
                llc = new TableMeta.LonLatColumns(lonCol, latCol, CoordinateSys.EQ_J2000);
                meta.setLonLatColumnAttr(MetaConst.CENTER_COLUMN, llc);
                break;
            }
        }
        super.prepareTableMeta(meta, columns, request);
    }

    private File getWiseArtifact(final TableServerRequest req) throws IOException, DataAccessException {
        Circle c = new Circle(WorldPt.parse(req.getParam("UserTargetWorldPt")), Double.valueOf(req.getParam("subsize")));
        String band = req.getParam("band");
        File retFile = null;

        //Step 1: get Coadd ID from WISE.
        String coadd= getWiseCoaddId(c, ServiceRetriever.WISE_3A, band);
        //Step 2: modify TableServerRequest
        if (coadd != null) {
            req.setSafeParam("ProductLevel",ServiceRetriever.WISE_3A);
            req.setSafeParam("coadd_id",coadd);
            req.setSafeParam(WiseRequest.SCHEMA, WiseRequest.ALLWISE_MULTIBAND);
            req.setSafeParam(WiseRequest.SCHEMA_GROUP, ServiceRetriever.WISE_SCHEMA_GROUP);
            req.setSafeParam(WiseRequest.HOST, ServiceRetriever.WISE_HOST);

            //Step 3: use Coadd ID to get artifact.
            QueryWiseArtifact wiseArtifactQuery = new QueryWiseArtifact();

            retFile= wiseArtifactQuery.loadDynDataFile(req);
        }
        return retFile;
    }

    private File get2MassArtifact(final TableServerRequest req) throws IOException, DataAccessException {
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        String urlStr=null;
        TableServerRequest req2Mass = new TableServerRequest("2MassQuery");
        TableServerRequest reqArtifact = new TableServerRequest();

        req2Mass.setSafeParam("DoSearch","true");
        req2Mass.setSafeParam("type", req.getParam("type"));
        req2Mass.setSafeParam("DemoSearch2MassPosCmd.field.radius", get2MassImageSize(req.getParam("subsize")));
        req2Mass.setSafeParam("UserTargetWorldPt", req.getParam("UserTargetWorldPt"));
        req2Mass.setSafeParam("projectId","2mass");
        req2Mass.setSafeParam("searchName", "2mass_3");
        req2Mass.setSafeParam("queryId", "2MassQuery" );

        Query2Mass query2Mass = new Query2Mass();
        StringKey key = new StringKey(QueryFinderChartArtifact.class.getName(), getUniqueID(req2Mass));
        File f= (File)cache.get(key);
        if (f==null) {
            f = query2Mass.loadDynDataFile(req2Mass);
            cache.put(key, f);
        }

        try {
            int ScanId = getScanId(req);
            DataGroup resTable= IpacTableReader.readIpacTable(f, "");
            if (resTable.values().size()>0) {
                String aKey = "pers_art";
                if (req2Mass.getParam("type").equals("glint")) aKey= "glint_art";

                for (DataObject o: resTable.values()) {
                    if (Integer.parseInt((String.valueOf(o.getDataElement("scanscan"))))==ScanId) {
                        urlStr= (String) o.getDataElement(aKey+aKey);
                        break;
                    }
                }
                if (urlStr!=null) {
                    reqArtifact.setSafeParam("DemoSearch2MassPosCmd.field.radius", get2MassImageSize(req.getParam("subsize")));
                    reqArtifact.setSafeParam("UserTargetWorldPt", req.getParam("UserTargetWorldPt"));
                    reqArtifact.setParam("type", req.getParam("type"));
                    reqArtifact.setParam(aKey, urlStr );
                }
            }
        } catch (FitsException e) {
            throw new IOException(e.getMessage(), e.getCause());
        } catch (FailedRequestException  e) {
            throw new IOException(e.getDetailMessage(), e.getCause());
        } catch (GeomException e) {
            throw new IOException(e.getMessage(), e.getCause());
        } catch (SecurityException  e) {
            throw new IOException(e.getMessage(), e.getCause());
        } catch (IpacTableException  e) {
            throw new IOException(e.getMessage(), e.getCause());
        }

        if (urlStr==null) {
            throw new IOException("Unable to find artifact files.");
        }
        Query2MassArtifact queryArtifact = new Query2MassArtifact();
        key = new StringKey(QueryFinderChartArtifact.class.getName(), getUniqueID(reqArtifact));
        File retFile = (File)cache.get(key);

        if (retFile==null) {
            retFile = queryArtifact.loadDynDataFile(reqArtifact);
            cache.put(key, retFile);
        }

        return retFile;
    }

    private static int getScanId(final TableServerRequest req) throws FitsException, FailedRequestException,
            IOException, GeomException, SecurityException{
        WebPlotRequest wpReq= WebPlotRequest.make2MASSRequest(WorldPt.parse(req.getParam("UserTargetWorldPt")),
                            req.getParam("band"), Float.parseFloat(req.getParam("subsize")));
        FileRetriever retrieve= FileRetrieverFactory.getRetriever(wpReq);

        FileData fileData = retrieve.getFile(wpReq);
        File f= fileData.getFile();

        FitsRead frAry[] = readFits(f);

        return frAry[0].getHDU().getHeader().getIntValue("SCANNO");
    }

    private static FitsRead [] readFits(File fitsFile) throws FitsException, FailedRequestException, IOException {
        Fits fits= new Fits(fitsFile.getPath());
        FitsRead fr[];
        try {
            fr = FitsRead.createFitsReadArray(fits);
        } finally {
            fits.getStream().close();
        }
        return fr;
    }

    private static String get2MassImageSize(String v) {
        //use ServiceRetriever->get2MassPlot(...)
        double sizeInArcSec = MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, Double.parseDouble(v));
        if (sizeInArcSec > 500) sizeInArcSec = 500;
        if (sizeInArcSec < 50) sizeInArcSec = 50;
        return Float.toString((float) MathUtil.convert(MathUtil.Units.ARCSEC, MathUtil.Units.DEGREE, sizeInArcSec));
    }

    private static String getWiseCoaddId(Circle c, String levelStr, String band) throws IOException, DataAccessException {
        String coaddID = null;
        FileData retval= null;
        WiseRequest sr= new WiseRequest();
        sr.setPageSize(1000);
        sr.setParam(ReqConst.USER_TARGET_WORLD_PT, c.getCenter());
        sr.setIntersect("CENTER");
        sr.setParam("mcenter", WiseRequest.MCEN); // get the most centered images
        sr.setParam("optLevel", "1b4,3a4");// todo, what is this?
        sr.setSchema(ServiceRetriever.WISE_SCHEMA);
        sr.setSchemaGroup(ServiceRetriever.WISE_SCHEMA_GROUP);
        sr.setHost(ServiceRetriever.WISE_HOST);
        //sr.setParam("band", request.getSurveyBand());
        sr.setParam("band", band);
        setWiseParams(sr,levelStr,c);

        SearchManager sman= new SearchManager();
        DataGroupPart primaryData= sman.getDataGroup(sr);
        DataGroup resTable= primaryData.getData();

        if (resTable.values().size()==1) {
            DataObject rowData= resTable.get(0);
            coaddID= (String) rowData.getDataElement("coadd_id");
        }
        return coaddID;
    }


    private static void setWiseParams(WiseRequest sr, String levelStr, Circle c) {
        sr.setParam("subsize", c.getRadius()+"");
        sr.setParam(WiseRequest.PRODUCT_LEVEL,levelStr);
        if (levelStr.equals(ServiceRetriever.WISE_1B)) {
//            sr.setParam("table", WISE_4BAND_L1_TABLE);
            sr.setSchema(WiseRequest.ALLSKY_4BAND);
        }
        else if (levelStr.equals(ServiceRetriever.WISE_3A)) {
//            sr.setParam("table", WISE_4BAND_L3_TABLE);
            sr.setSchema(WiseRequest.ALLWISE_MULTIBAND);
        }

    }

    private static File makeFileName(TableServerRequest req) throws IOException {
        return File.createTempFile("Artifacts-"+req.getRequestId(), ".tbl", ServerContext.getPermWorkDir());
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