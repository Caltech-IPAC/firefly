package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ImageGridSupport;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.hydra.data.PlanckCutoutRequest;
import edu.caltech.ipac.target.Fixed;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.target.TargetFixedSingle;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
/**
 * Created for Planck.
 * User: wmi
 * 
*/


@SearchProcessorImpl(id = "planckCutoutQuery")
public class QueryPlanckCutout extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    //private static final String PLANCK_CUTOUT_HOST= "http://***REMOVED***:9072/cgi-bin/Heal2Tan/nph-heal2tan";
    private static final double FWHM [] ={32.65,27.00,13.01,9.94,7.04,4.66,4.41,4.47,4.23};
    private static final int planckBands[] = {30,44,70,100,143,217,353,545,857};
    private static final String wmapBands[] = {"K","Ka","Q","V","W"};
    private static final int irasBands[] = {100,60,25,12};
    private static final int width = 96;

    public static final String RA = "ra";
    public static final String DEC = "dec";
    public static final String OBJ_NAME = "objname";
    public static final String MAX_SEARCH_TARGETS = "maxSearchTargets";

    private int maxSearchTargets = Integer.MAX_VALUE;

    private List<Target> targets = null;
    private Target curTarget = null;
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        long start = System.currentTimeMillis();
        //***REMOVED***:9072/cgi-bin/Heal2Tan/nph-heal2tan?locstr=199.38,61.095&size=128&pixsize=1.5&hmap=I&mission=planck&planckfreq=30&wmapfreq=&submit=

        setXmlParams(request);
        if (request.containsParam(MAX_SEARCH_TARGETS)) {
            maxSearchTargets = Integer.parseInt(request.getParam(MAX_SEARCH_TARGETS));
        }

        String fromCacheStr = "";
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        StringKey key = new StringKey(QueryPlanckCutout.class.getName(), getUniqueID(request));
        File retFile = (File) cache.get(key);
        if (retFile == null) {
            retFile = getPlanckImageCutouts(request);
            cache.put(key, retFile);
        } else {
            fromCacheStr = "   (from Cache)";
        }
        if (request.containsParam("FilterColumn") && request.containsParam("columns")) {
            retFile = getFilterPanelTable(request, retFile);
        } else {
            // QueryPlanckCutout returns a complete table, but only shows filtered results.
            // Thus set page size to 0 at initial stage.
            if (request.containsParam("filename") && (request.getFilters()==null || request.getFilters().size()==0)) {
                request.setPageSize(0);
            }
        }

        if (!request.containsParam("FilterColumn")) {
            request.setFilters(getFilterList(request, retFile));
        }

        long elaspe = System.currentTimeMillis() - start;
        String sizeStr = FileUtil.getSizeAsString(retFile.length());
        String timeStr = UTCTimeUtil.getHMSFromMills(elaspe);

        _log.info("catalog: " + timeStr + fromCacheStr,
                "filename: " + retFile.getPath(),
                "size:     " + sizeStr);

        return retFile;
    }

    private List<String> getFilterList(TableServerRequest request, File f) throws IOException, DataAccessException {
        String[] columns= {/*OBJ_NAME,*/RA,DEC}; //columns=objname,ra,dec
        ArrayList<String> retval = null;
        try {
            DataGroup table = IpacTableReader.readIpacTable(f, columns, false, "");
            String filter, value;
            DataObject dObj = table.get(0);
            for (String key: columns) {
                value = String.valueOf(dObj.getDataElement(key));
                if (value!=null && value.length()>0) {
                    filter= key+" = "+value;
                    if (retval==null) retval=new ArrayList<String>();
                    retval.add(filter);
                }
            }

        } catch (IpacTableException e) {
            throw new IOException("IpacTableException: "+e.getMessage());
        }
        return retval;
    }

    private File getPlanckImageCutouts(TableServerRequest request) throws IOException, DataAccessException {
        File f;

        targets = getTargetsFromRequest(request);

        if (targets.size()>maxSearchTargets) {throw QueryUtil.createEndUserException(
                "There are " + targets.size() + " targets. " +
                        "Planck Image Service only supports " + maxSearchTargets + " targets or less.");}
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

    private File handleTargets(TableServerRequest request) throws IOException, DataAccessException {
        String baseUrl = request.getParam(PlanckCutoutRequest.CUTOUT_HOST);
        String subsizeStr = request.getParam("subsize");
        String map_type = request.getParam("maptype");
        String map_scale = request.getParam("mapscale");

        //convert cutout angular size to pixelsize for Heal2Tan
        //String pixsize = "1.5";
        //Float pixelscale = new Float(pixsize);
        Float subsize = new Float(subsizeStr);
        //int sizePix = (int)((subsize*60)/pixelscale);
        //String sizeStr = Integer.toString(sizePix);

        //todo: calculate angular size 4*FWHM

        WorldPt pt;
        String pos = null;
        /*String userTargetWorldPt = request.getParam(ReqConst.USER_TARGET_WORLD_PT);
        if (userTargetWorldPt != null) {
            pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                pos = pt.getLon() + "," + pt.getLat();
            }
        } else {
            throw new DataAccessException("No Name or Position found");
        }*/

        ArrayList<DataType> defs = ImageGridSupport.createBasicDataDefinitions();
        DataType dt;
        defs.add(new DataType(OBJ_NAME, String.class));
        dt = new DataType(RA, Double.class);
        dt.setFormatInfo(DataType.FormatInfo.createFloatFormat(dt.getFormatInfo().getWidth(), 6));
        defs.add(dt);
        dt = new DataType(DEC, Double.class);
        dt.setFormatInfo(DataType.FormatInfo.createFloatFormat(dt.getFormatInfo().getWidth(), 6));
        defs.add(dt);

        DataGroup table = ImageGridSupport.createBasicDataGroup(defs, true);
        WebPlotRequest wpReq;
        String ExpandedDesc,desc;
        DataObject row;
        String groupName, url, planckurl, sizeStr, pixsize;
        for (Target tgt: targets) {
            curTarget = tgt;
            pt = getTargetWorldPt(curTarget);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                pos = pt.getLon() + "," + pt.getLat();
            }

            //For Planck
            for (String mType: map_type.split(",")) {
                url = createCutoutURLString(baseUrl, pos, mType);
                table.addAttributes(new DataGroup.Attribute("TitleDesc.PLANCK-"+mType, "(Planck Cutouts)"));
                for(int j = 0; j < planckBands.length; j++){
                    row = new DataObject(table);

                    //skip Q and U for 545 and 857
                    if (mType.equals("Q") && planckBands[j]==545){
                        continue;
                    } else if (mType.equals("U") && planckBands[j]==545){
                        continue;
                    } else if (mType.equals("Q") && planckBands[j]==857){
                        continue;
                    } else if (mType.equals("U") && planckBands[j]==857){
                        continue;
                    }

                    if (planckBands[j]==30 || planckBands[j]==44 || planckBands[j]==70){
                        pixsize ="2.0";
                    } else {
                        pixsize = "1.0";
                    }

                    sizeStr = getSizeStr(pixsize, subsizeStr);

                    planckurl= url+"&pixsize="+pixsize+"&mission=planck&planckfreq="+planckBands[j]+"&wmapfreq=&submit=";

                    if (map_scale.equals("yes")){
                        String fscaleStr= request.getParam("sfactor");
                        Double fscale= new Double(fscaleStr);
                        int subscaledsize= (int)(fscale*FWHM[j]);
                        planckurl +=("&size="+subscaledsize);
                    } else {
                        planckurl +=("&size="+sizeStr);
                    }
                    groupName= "PLANCK-" + mType;
                    desc= planckBands[j] + "GHz";
                    ExpandedDesc= groupName+"-"+desc;
                    wpReq= WebPlotRequest.makeURLPlotRequest(planckurl,ExpandedDesc);
                    wpReq.setWorldPt(pt);
                    wpReq.setSizeInDeg(subsize);
                    wpReq.setZoomType(ZoomType.TO_WIDTH);
                    wpReq.setZoomToWidth(width);
                    wpReq.setHasMaxZoomLevel(false);
                    addWebPlotRequest(table, row, groupName, ExpandedDesc, desc, wpReq);
                }

                //for WMAP
                if (map_scale.equals("yes")){
                    table.addAttributes(new DataGroup.Attribute("TitleDesc.WMAP-"+mType,
                            "(WMAP Cutouts: Cutout Image Size="+toDegString(subsizeStr)+")"));
                } else {
                    table.addAttributes(new DataGroup.Attribute("TitleDesc.WMAP-"+mType, "(WMAP Cutouts)"));
                }

                for (String band: wmapBands) {
                    row = new DataObject(table);
                    pixsize ="2.0";
                    sizeStr = getSizeStr(pixsize,subsizeStr);
                    String wmapurl = url + "&pixsize=" + pixsize + "&size=" + sizeStr + "&mission=wmap" + "&wmapfreq=" +
                                    band + "&planckfreq=&submit=";
                    groupName= "WMAP-"+mType;
                    desc= band;
                    ExpandedDesc= groupName+"-"+desc;
                    wpReq= WebPlotRequest.makeURLPlotRequest(wmapurl, ExpandedDesc);
                    wpReq.setWorldPt(pt);
                    wpReq.setSizeInDeg(subsize);
                    wpReq.setZoomType(ZoomType.TO_WIDTH);
                    wpReq.setZoomToWidth(width);

                    addWebPlotRequest(table, row, groupName, ExpandedDesc, desc, wpReq);
                }
            }

            if (map_scale.equals("yes")){
                table.addAttributes(new DataGroup.Attribute("TitleDesc.IRAS",
                        "(IRAS Cutouts from ISSA plates: Cutout Image Size="+toDegString(subsizeStr)+")"));
            } else {
                table.addAttributes(new DataGroup.Attribute("TitleDesc.IRAS", "(IRAS Cutouts from ISSA plates)"));
            }
            //for IRAS
            //Padding the cutout size for glactic north up rotation first
            //and crop the image
            for (int band: irasBands) {
                row = new DataObject(table);
                groupName= "IRAS";
                ExpandedDesc= groupName+" "+band+" microns";
                desc= band+" microns";
                wpReq= WebPlotRequest.makeISSARequest(pt,Integer.toString(band),subsize);
                wpReq.setRotateNorth(true);
                wpReq.setRotateNorthType(CoordinateSys.GALACTIC);
                wpReq.setPostCropAndCenter(true);
                wpReq.setPostCropAndCenterType(CoordinateSys.GALACTIC);
                wpReq.setZoomType(ZoomType.TO_WIDTH);
                wpReq.setZoomToWidth(width);

                addWebPlotRequest(table, row, groupName, ExpandedDesc, desc, wpReq);
            }
        }
        // write out table into ipac-table format..
        if (table.size()==0) {
            table.addAttributes(new DataGroup.Attribute("INFO", "Image data not found!"));
        }
        File f = createFile(request);
        table.shrinkToFitData();
        IpacTableWriter.save(f, table);
        return f;
    }


    private File getFilterPanelTable(TableServerRequest request, File f) throws IOException, DataAccessException {
        File retFile;
        String[] columns= request.getParam("columns").split(";"); //columns=objname,ra,dec
        try {
            DataGroup table = IpacTableReader.readIpacTable(f, columns, false, "TableFiltering");
            DataGroup newTable = new DataGroup(table.getTitle(), table.getDataDefinitions());
            DataObject last = null;
            boolean same = false;
            for (DataObject o: table) {
                if (last== null) {
                    same = false;
                } else {
                    if (o.getData()[0]!=null && o.getData()[0].toString().length()>0) {
                        same =  ComparisonUtil.equals(last.getData()[0], o.getData()[0]);
                    } else { //compare ra and dec
                        same = ComparisonUtil.equals(last.getData()[1],o.getData()[1])&&
                               ComparisonUtil.equals(last.getData()[2],o.getData()[2]);
                    }
                }
                if (!same) newTable.add(o);
                last = o;
            }
            retFile = createFile(request);
            newTable.shrinkToFitData();
            IpacTableWriter.save(retFile, newTable);
        } catch (IpacTableException e) {
            throw new IOException("IpacTableException: "+e.getMessage());
        }
        return retFile;
    }

    private void addWebPlotRequest(DataGroup table, DataObject row, String groupName,
                                          String ExpandedDesc, String desc, WebPlotRequest wpReq) {
        RangeValues rv= new RangeValues(RangeValues.PERCENTAGE,1.0,RangeValues.PERCENTAGE,99.0,
                RangeValues.STRETCH_LINEAR);

        wpReq.setInitialColorTable(4);
        wpReq.setInitialRangeValues(rv);
        wpReq.setExpandedTitle(ExpandedDesc);
        wpReq.setHideTitleDetail(true);
        wpReq.setTitle(desc);

        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.TYPE.toString()), "req");
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.THUMBNAIL.toString()), wpReq.toString());
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.DESC.toString()), wpReq.getTitle());
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.GROUP.toString()), groupName);

        row.setDataElement(table.getDataDefintion(OBJ_NAME), curTarget.getName());
        row.setDataElement(table.getDataDefintion(RA), wpReq.getWorldPt().getLon());
        row.setDataElement(table.getDataDefintion(DEC),wpReq.getWorldPt().getLat());

        table.add(row);
    }

    public static String createCutoutURLString(String baseUrl,String pos,String mtypes) {
        //String url = baseUrl + "?locstr=" + pos + "&pixsize=" + pixsize + "&hmap=" + mtypes;
        String url = baseUrl + "?locstr=" + pos + "&hmap=" + mtypes;
        return url;
    }

    private String getSizeStr (String pixsize, String subsizeStr) {

        Float pixelscale = new Float(pixsize);
        Float subsize = new Float(subsizeStr);
        int sizePix = (int)((subsize*60)/pixelscale);
        String sizeStr = Integer.toString(sizePix);
        return sizeStr;
    }

    private String processGridGroup (String map_type) {
        // create grid group attribute String
        String GrdattributeStr = "keywords=";

        if (!StringUtils.isEmpty(map_type)) {
            String[] mtypes = map_type.split(",");
            //for Planck
            for (int i = 0; i < map_type.split(",").length; i++) {
                GrdattributeStr += "Planck-" + mtypes[i] + ",";
            }
            //add WMAP
            for (int i = 0; i < map_type.split(",").length; i++) {
                GrdattributeStr += "WMAP-" + mtypes[i] + ",";
            }
        }
        GrdattributeStr = GrdattributeStr + "IRAS&show_in_a_row=T&show_label=TOP";
        return GrdattributeStr;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        meta.setAttribute("GRID_THUMBNAIL_COLUMN", "thumbnail");
        meta.setAttribute("PLOT_REQUEST_COLUMN", "req");

    }

    //------------------------------------ static methods ------------------------------------
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

    private static WorldPt getTargetWorldPt(Target t) throws IOException {
        WorldPt pt = null;
        if (!t.isFixed()) {
            throw new IOException("Table upload cannot support moving targets.");
        }
        Fixed ft = (Fixed) t;

        pt = new WorldPt(ft.getPosition().getRa(), ft.getPosition().getDec());

        return pt ;
    }

    private static String toDegString(String s) {
        float sv = StringUtils.getFloat(s);

        return NumberFormat.getNumberInstance().format(sv) + " deg";
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
