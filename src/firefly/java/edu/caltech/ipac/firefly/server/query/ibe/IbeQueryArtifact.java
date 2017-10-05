/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.query.ibe;

import edu.caltech.ipac.astro.ibe.BaseIbeDataSource;
import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.datasource.TwoMassIbeDataSource;
import edu.caltech.ipac.astro.ibe.datasource.WiseIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WiseRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.persistence.IbeFileRetrieve;
import edu.caltech.ipac.firefly.server.persistence.QueryIBE;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ServiceRetriever;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query the IBE for artifacts. Copied and renamed FinderChartQueryArtifact (which is now deprecated).
 */
@SearchProcessorImpl(id = "IbeQueryArtifact", params = {
        @ParamDoc(name = "service", desc = "right now: wise or 2mass, might have more options later"),
        @ParamDoc(name = "band", desc = "wise band: 1,2,3 or 4, only necessary for wise"),
        @ParamDoc(name = "type", desc = "artifact type:  wise: P,O,H or D, 2mass: glint or pers"),
        @ParamDoc(name = "UserTargetWorldPt", desc = "target string, example- 2.3;4.5;EQJ2000 or 6.7;8.9;GAL"),
        @ParamDoc(name = "subsize", desc = "the radius in degrees"),

        @ParamDoc(name = WiseRequest.HOST, desc = "todo add support for: (optional) the hostname, including port"),
        @ParamDoc(name = WiseRequest.SCHEMA_GROUP, desc = "todo add support for: the name of the schema group"),
        @ParamDoc(name = WiseRequest.SCHEMA, desc = "todo add support for: the name of the schema within the schema group")
})
public class IbeQueryArtifact extends IpacTablePartProcessor {
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final String RA = "ra";
    private static final String DEC = "dec";
    public static final String ID= "IbeQueryArtifact";



    @Override
    protected File loadDataFile(TableServerRequest req) throws IOException, DataAccessException {
        return getFinderChartArtifact(req);
    }

    private File getFinderChartArtifact(TableServerRequest req) throws IOException, DataAccessException {
        String service = req.getParam("service");

        if (service==null) throw new IOException("Missing service param");

        File retFile = null;
        if (service.equalsIgnoreCase("wise")) {
            retFile = getWiseArtifact(req);
        } else if (service.equalsIgnoreCase("2mass")) {
            retFile = get2MassArtifact(req);
        } else {
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
        sreq.setParam(ServerParams.USER_TARGET_WORLD_PT, req.getParam("UserTargetWorldPt"));
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


    private static double max2massSize = MathUtil.convert(MathUtil.Units.ARCSEC, MathUtil.Units.DEGREE, 500);
    private static double min2massSize = MathUtil.convert(MathUtil.Units.ARCSEC, MathUtil.Units.DEGREE, 50);

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
        req.setParam(QueryIBE.MOST_CENTER, BaseIbeDataSource.MCEN);
        req.setPageSize(1);

        SearchManager sman= new SearchManager();
        DataGroupPart primaryData= sman.getDataGroup(req);
        DataGroup resTable= primaryData.getData();

        if (resTable.values().size() > 0) {
            return resTable.get(0);
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


    public static  List<RelatedData> getWiseRelatedData(WorldPt wp, String sizeInDeg, String band) {

        List<RelatedData> rdList= new ArrayList<>();
        rdList.add(makeWiseRelatedDataItem(wp,sizeInDeg,band,"P", "latents","WISE Latents"));
        rdList.add(makeWiseRelatedDataItem(wp,sizeInDeg,band,"O", "ghost","WISE Optical Ghosts"));
        rdList.add(makeWiseRelatedDataItem(wp,sizeInDeg,band,"H", "halos","WISE Halos"));
        rdList.add(makeWiseRelatedDataItem(wp,sizeInDeg,band,"D", "diff_spikes","WISE Diffraction Spikes"));
        return rdList;
    }

    public static  List<RelatedData> get2MassRelatedData(WorldPt wp, String sizeInDeg) {
        List<RelatedData> rdList= new ArrayList<>();
        rdList.add(get2MassRelatedDataItem(wp,sizeInDeg,"glint","glint_arti","2MASS Glints Artifacts"));
        rdList.add(get2MassRelatedDataItem(wp,sizeInDeg,"pers","pers_arti","2MASS Persistence Artifacts"));
        return rdList;
    }


    private static RelatedData makeWiseRelatedDataItem(WorldPt wp, String sizeInDeg, String band,
                                                String type, String dataKey, String desc) {
        Map<String,String> params= new HashMap<>();
        params.put("id", IbeQueryArtifact.ID);
        params.put("service", "wise");
        params.put("band", band);
        params.put("type", type);
        params.put("subsize", sizeInDeg);
        params.put(ServerParams.USER_TARGET_WORLD_PT, wp.toString());
        params.put(WiseRequest.PRODUCT_LEVEL, ServiceRetriever.WISE_3A);
        return RelatedData.makeTabularRelatedData(params, dataKey, desc);
    }

    private static RelatedData get2MassRelatedDataItem(WorldPt wp, String sizeInDeg,
                                                String type, String dataKey, String desc) {
        Map<String,String> params= new HashMap<>();
        params.put("id", IbeQueryArtifact.ID);
        params.put("service", "2mass");
        params.put("type", type);
        params.put("subsize", sizeInDeg);
        params.put(ServerParams.USER_TARGET_WORLD_PT, wp.toString());
        return RelatedData.makeTabularRelatedData(params, dataKey, desc);
    }




}






