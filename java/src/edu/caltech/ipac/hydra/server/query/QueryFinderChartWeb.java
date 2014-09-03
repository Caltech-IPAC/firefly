package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.QueryDescResolver;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.hydra.core.FinderChartDescResolver;
import edu.caltech.ipac.target.Fixed;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.target.TargetFixedSingle;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Date: Sept 2, 2014
 *
 * @author loi
 * @version $Id: BaseEventWorker.java,v 1.14 2012/09/21 23:35:38 roby Exp $
 */

@SearchProcessorImpl(id = "QueryFinderChartWeb")
public class QueryFinderChartWeb extends DynQueryProcessor {
    public static final String PROC_ID = QueryFinderChartWeb.class.getAnnotation(SearchProcessorImpl.class).id();
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static final String EVENTWORKER = "ew";
    private static final String ALL_EVENTWORKER = "all_ew";
    public static final String OBJ_ID = "id";
    public static final String OBJ_NAME = "objname";
    public static final String RA = "ra";
    public static final String DEC = "dec";
    public static final String OBS_DATE="Obs date";
    public static final String MID_OBS="Mid obs";
    public static final String MAX_SEARCH_TARGETS = "maxSearchTargets";
    public static final String USER_TARGET_WORLDPT = "UserTargetWorldPt";

    private int maxSearchTargets = Integer.MAX_VALUE;
    private List<Target> targets = null;

    @Override
    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
        super.onComplete(request, results);
        // now.. we prefetch the images so the page will load faster.

//        TableServerRequest treq = (TableServerRequest) request;
//        if (results.getData().size() == 0 || treq.getFilters() == null || treq.getFilters().size() == 0) return;
//
//        String spid = request.getParam("searchProcessorId");
//        String mst = request.getParam("maxSearchTargets");
//        String flt = StringUtils.toString(treq.getFilters());
//
//        if ( StringUtils.isEmpty(spid) && (!StringUtils.isEmpty(mst) || flt.startsWith("id =")) ) {
//            ExecutorService executor = Executors.newFixedThreadPool(results.getData().size());
//            StopWatch.getInstance().start("QueryFinderChart: prefetch images");
//            try {
//                for (int i = results.getData().size() - 1; i >= 0; i--) {
//                    DataObject row = results.getData().get(i);
//                    final WebPlotRequest webReq = WebPlotRequest.parse(String.valueOf(row.getDataElement(ImageGridSupport.COLUMN.THUMBNAIL.name())));
//                    Runnable worker = new Runnable() {
//                        public void run() {
//                            try {
//                                StopWatch.getInstance().start(webReq.getUserDesc());
//                                FileRetrieverFactory.getRetriever(webReq).getFile(webReq);
//                                StopWatch.getInstance().printLog(webReq.getUserDesc());
//                            } catch (Exception e) {}
//                        }
//                    };
//                    executor.execute(worker);
//                }
//                executor.shutdown();
//                executor.awaitTermination(10, TimeUnit.SECONDS);
//                StopWatch.getInstance().printLog("QueryFinderChart: prefetch images");
//            } catch (Exception e) { e.printStackTrace();}
//        }
    }

    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {

        setXmlParams(request);
        if (request.containsParam(MAX_SEARCH_TARGETS)) {
            maxSearchTargets = Integer.parseInt(request.getParam(MAX_SEARCH_TARGETS));
        }

        File retFile = getFinderChart(request);

        return retFile;
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new FinderChartDescResolver());
    }

    private File getFinderChart(TableServerRequest request) throws IOException, DataAccessException {
        File f;

        targets = getTargetsFromRequest(request);

        if (targets.size() > maxSearchTargets) {
            throw QueryUtil.createEndUserException(
                "There are "+targets.size()+" targets. "+
                        "Finder Chart only supports "+ maxSearchTargets +" targets or less.");
        }
        f = handleTargets(request);

        return f;
    }

    private List<Target> getTargetsFromRequest(TableServerRequest request) throws IOException, DataAccessException {
        List<Target> list;

        if (request.containsParam("filename")) {
            String uploadedFile = request.getParam("filename");
            list = getTargetList(uploadedFile);
        } else {
            String userTargetWorldPt = request.getParam(ReqConst.USER_TARGET_WORLD_PT);
            list= new ArrayList<Target>();
            //parse position
            if (userTargetWorldPt != null) {
                WorldPt pt = WorldPt.parse(userTargetWorldPt);
                if (pt!=null) {
                    pt = VisUtil.convertToJ2000(pt);
                    String name = request.containsParam("TargetPanel.field.targetName")?
                            request.getParam("TargetPanel.field.targetName"):"";
                    Target t= new TargetFixedSingle(name, new PositionJ2000(pt.getLon(), pt.getLat()));
                    list.add(t);
                }
            }
        }
        return list;
    }


    private File handleTargets(TableServerRequest request)
            throws IOException, DataAccessException {

        ArrayList<DataType> defs = new ArrayList<DataType>();

        //create an IPAC table with default attributes.
        DataType objId = new DataType(OBJ_ID, Integer.class);
        DataType objName = new DataType(OBJ_NAME, String.class);

        DataType ra = new DataType(RA, Double.class);
        ra.setFormatInfo(DataType.FormatInfo.createFloatFormat(ra.getFormatInfo().getWidth(), 6));

        DataType dec = new DataType(DEC, Double.class);
        dec.setFormatInfo(DataType.FormatInfo.createFloatFormat(dec.getFormatInfo().getWidth(), 6));

        DataGroup table = new DataGroup("targets", Arrays.asList(objId, objName, ra, dec));

        for (int i = 0; i < targets.size(); i++) {
            Target tgt = targets.get(i);
            WorldPt pt = getTargetWorldPt(tgt);

            DataObject row = new DataObject(table);
            row.setDataElement(objId, i);
            row.setDataElement(objName, getTargetName(tgt));
            row.setDataElement(ra, pt.getLat());
            row.setDataElement(dec, pt.getLon());
            table.add(row);
        }

        //create and write IPAC table into a file
        table.shrinkToFitData();
        File f = createFile(request);
        IpacTableWriter.save(f, table);
        return f;
    }

    private static String getTargetName(Target t) {
        String retval = t.getName();
        if (retval==null) retval="";
        return retval;
    }

    private static WorldPt getTargetWorldPt(Target t) throws IOException {
        WorldPt pt = null;
        if (!t.isFixed()) {
            throw new IOException("Table upload cannot support moving targets.");
        }
        Fixed ft = (Fixed) t;

        pt = new WorldPt(ft.getPosition().getRa(), ft.getPosition().getDec());

        return pt ;
    }

    public static List<Target> getTargetList(String uploadedFile) throws DataAccessException, IOException {

        File ufile = VisContext.convertToFile(uploadedFile);
        if (ufile == null || !ufile.canRead()) {
            LOGGER.error("Unable to read uploaded file:" + uploadedFile);
            throw QueryUtil.createEndUserException("Finder Chart search failed.",
                    "Unable to read uploaded file.");
        }

        if (FileUtil.isBinary(ufile)) {
            throw QueryUtil.createEndUserException("Unable to parse binary file.");
        }

        return QueryUtil.getTargetList(ufile);

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