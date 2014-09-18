package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.QueryDescResolver;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
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

    // keys used by finderchart search
    private static final String OBJ_ID = "id";
    private static final String OBJ_NAME = "objname";
    private static final String RA = "ra";
    private static final String DEC = "dec";
    private static final String MAX_SEARCH_TARGETS = "maxSearchTargets";

    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {

        File retFile = null;
        setXmlParams(request);
        retFile = getFinderChart(request);
        return retFile;
    }

    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        meta.setAttribute("datasetInfoConverterId", "FINDER_CHART");
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new FinderChartDescResolver());
    }

    private File getFinderChart(TableServerRequest request) throws IOException, DataAccessException {

        int maxSearchTargets = Integer.MAX_VALUE;
        if (request.containsParam(MAX_SEARCH_TARGETS)) {
            maxSearchTargets = Integer.parseInt(request.getParam(MAX_SEARCH_TARGETS));
        }

        List<Target> targets = getTargetsFromRequest(request);

        if (targets.size() > maxSearchTargets) {
            throw QueryUtil.createEndUserException(
                "There are "+targets.size()+" targets. "+
                        "Finder Chart only supports "+ maxSearchTargets +" targets or less.");
        }
        File f = createTargetFile(targets, request);

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


    private File createTargetFile(List<Target> targets, TableServerRequest request)
            throws IOException, DataAccessException {

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
            row.setDataElement(ra, pt.getLon());
            row.setDataElement(dec, pt.getLat());
            table.add(row);
        }

        //create and write IPAC table into a file
        table.shrinkToFitData();
        File f = createFile(request);
        IpacTableWriter.save(f, table);

        if (request.containsParam("filename")) {
            // update the uploaded file with resolved coordinates so gator can use it.
            String uploadedFile = request.getParam("filename");
            File ufile = VisContext.convertToFile(uploadedFile);
            FileUtil.copyFile(f, ufile);
        }
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