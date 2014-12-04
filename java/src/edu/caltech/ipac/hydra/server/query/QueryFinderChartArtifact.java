package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.datasource.TwoMassIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.form.FloatFieldDef;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.persistence.IbeFileRetrieve;
import edu.caltech.ipac.firefly.server.persistence.QueryIBE;
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
import edu.caltech.ipac.util.IpacTableUtil;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

        if (!req.containsParam("UserTargetWorldPt")) {
            if (req.containsParam("ra") && req.containsParam("dec")) {
                WorldPt wp= new WorldPt(Double.parseDouble(req.getParam("ra")),
                                        Double.parseDouble(req.getParam("dec"))
                                        );
                req= new TableServerRequest(req.getRequestId(),req);
                req.setParam("UserTargetWorldPt", wp);
            }
        }
        return getFinderChartArtifact(req);
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

        WiseRequest sreq= new WiseRequest();
        sreq.setParam(QueryIBE.MISSION, WiseIbeDataSource.WISE);
        sreq.setParam(ReqConst.USER_TARGET_WORLD_PT, req.getParam("UserTargetWorldPt"));
        sreq.setParam("optLevel", "1b4,3a4");// todo, what is this?
        sreq.setParam("band", req.getParam("band"));
        sreq.setParam("subsize", req.getParam("subsize"));
        sreq.setParam(WiseRequest.PRODUCT_LEVEL, ServiceRetriever.WISE_3A);
        sreq.setSchema(WiseRequest.ALLWISE_MULTIBAND);

        try {
            DataObject row = queryIBE(sreq);
            //Step 2: modify TableServerRequest
            if (row != null) {
                sreq.setParams(req.getParams());
                sreq.setParams(IpacTableUtil.asMap(row));

                return getIBEData(sreq);
            }
        } catch (Exception e) {
            // some may not have artifacts
        }
        return null;
    }

    private File get2MassArtifact(final TableServerRequest req) throws IOException, DataAccessException {

        try {
            TableServerRequest sreq = new TableServerRequest();
            sreq.setParam(QueryIBE.MISSION, TwoMassIbeDataSource.TWOMASS);
            sreq.setParam(TwoMassIbeDataSource.DS_KEY, TwoMassIbeDataSource.DS.ASKY.getName());
            sreq.setParam(QueryIBE.POS_WORLDPT, req.getParam("UserTargetWorldPt"));
            sreq.setParam(QueryIBE.RADIUS, get2MassImageSize(req.getParam("subsize")));

            DataObject row = queryIBE(sreq);

            String fname = req.getParam("type").equals("glint") ? "glint.tbl" : "pers.tbl";
            sreq.setParams(IpacTableUtil.asMap(row));
            sreq.setParam("fname", fname);

            return getIBEData(sreq);

        } catch (SecurityException  e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }


    static double max2massSize = MathUtil.convert(MathUtil.Units.ARCSEC, MathUtil.Units.DEGREE, 500);
    static double min2massSize = MathUtil.convert(MathUtil.Units.ARCSEC, MathUtil.Units.DEGREE, 50);

    private static String get2MassImageSize(String v) {
        //use ServiceRetriever->get2MassPlot(...)
        double sizeInArcSec = Double.parseDouble(v);
        if (sizeInArcSec > max2massSize) {
            return Double.toString(max2massSize);
        } else if (sizeInArcSec < min2massSize) {
            return Double.toString(min2massSize);
        } else {
            return v;
        }
    }

    private static DataObject queryIBE(TableServerRequest req) throws IOException, DataAccessException {
        req.setRequestId(QueryIBE.PROC_ID);
        req.setParam(QueryIBE.MOST_CENTER, IBE.MCEN);
        req.setPageSize(1);

        SearchManager sman= new SearchManager();
        DataGroupPart primaryData= sman.getDataGroup(req);
        DataGroup resTable= primaryData.getData();

        if (resTable.values().size() > 0) {
            DataObject rowData= resTable.get(0);
            return rowData;
        } else {
            throw new DataAccessException("Cannot find IBE data:" + req.toString());
        }
    }

    private static File getIBEData(TableServerRequest req) throws IOException, DataAccessException {
        req.setRequestId(IbeFileRetrieve.PROC_ID);

        SearchManager sman= new SearchManager();
        FileInfo fileInfo= sman.getFileInfo(req);
        if (fileInfo != null) {
            File f = new File(fileInfo.getInternalFilename());
            return f.canRead() ? f : null;
        } else {
            throw new DataAccessException("Cannot find IBE data:" + req.toString());
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