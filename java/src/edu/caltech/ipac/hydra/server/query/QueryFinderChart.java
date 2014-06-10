package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ImageGridSupport;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.FileRetrieverFactory;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.hydra.server.servlets.FinderChartApi;
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
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.WorldPt;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 16, 2011
 * Time: 5:04:59 PM
 * To change this template use File | Settings | File Templates.
 */

@SearchProcessorImpl(id = "FinderChartQuery")
public class QueryFinderChart extends DynQueryProcessor {
    public static final String PROC_ID = QueryFinderChart.class.getAnnotation(SearchProcessorImpl.class).id();
    public static final String WEB_MODE = "web";
    public static final String MODE = FinderChartApi.Param.mode.name();

    private static final Logger.LoggerImpl _log = Logger.getLogger();
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
//    private enum Service {DSS, IRIS, ISSA, MSX, SDSS, TWOMASS, WISE}

    private static HashMap<WebPlotRequest.ServiceType, String> serviceTitleMap = null, bandMap = null;
    private static HashMap<WebPlotRequest.ServiceType, String[]> comboMap = null;

    private String wiseEventWorker[]=null, twoMassEventWorker[]=null;

    private String allWiseEventWorker[]={"diff_spikes_3_","halos_","ghosts_","latents_"};
    private String allTwoMassEventWorker[]={"glint_arti_","pers_arti_"};

    private int maxSearchTargets = Integer.MAX_VALUE;

    private List<Target> targets = null;
    private Target curTarget = null;
    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
    }

    @Override
    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
        super.onComplete(request, results);
        // now.. we prefetch the images so the page will load faster.

        if (results.getData().size() == 0) return;
        TableServerRequest treq = (TableServerRequest) request;

        String spid = treq.getParam("searchProcessorId");
        String flt = StringUtils.toString(treq.getFilters());

        if ( flt.startsWith("id =") && StringUtils.isEmpty(spid) ) {
            ExecutorService executor = Executors.newFixedThreadPool(results.getData().size());
            long itime = System.currentTimeMillis();
            try {
                for (DataObject row : results.getData()) {
                    final WebPlotRequest webReq = WebPlotRequest.parse(String.valueOf(row.getDataElement(ImageGridSupport.COLUMN.THUMBNAIL.name())));
                    Runnable worker = new Runnable() {
                        public void run() {
                            try {
                                long stime = System.currentTimeMillis();
                                System.out.println("thread started: " + Thread.currentThread().getName());
                                FileRetrieverFactory.getRetriever(webReq).getFile(webReq);
                                System.out.println("thread finished:"  + Thread.currentThread().getName() + " in " + (System.currentTimeMillis() - stime) + " ms");
                            } catch (Exception e) { e.printStackTrace();}
                        }
                    };
                    executor.execute(worker);
                }
                executor.shutdown();
                executor.awaitTermination(60, TimeUnit.SECONDS);
                System.out.println("!!ALL finished:" + Thread.currentThread().getName() + " in " + (System.currentTimeMillis() - itime) + " ms");
            } catch (Exception e) { e.printStackTrace();};
        }
    }

    @Override
    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {

        setXmlParams(request);
        if (request.containsParam(MAX_SEARCH_TARGETS)) {
            maxSearchTargets = Integer.parseInt(request.getParam(MAX_SEARCH_TARGETS));
        }

        String mode = request.getParam(MODE);
        mode = StringUtils.isEmpty(mode) ? WEB_MODE : mode;

        File retFile = getFinderChart(mode, request);

        if (mode.equals(WEB_MODE)) {
            if (request.containsParam("FilterColumn") && request.containsParam("columns")) {
                retFile = getFilterPanelTable(request, retFile);
            } else {
                // QueryFinderChart returns a complete table, but finder chart only shows filtered results.
                // Thus set page size to 0 at initial stage.
                if (request.containsParam("filename") && (request.getFilters()==null || request.getFilters().size()==0)) {
                    request.setPageSize(0);
                }
            }

            if (!request.containsParam("FilterColumn")) {
                request.setFilters(getFilterList(request, retFile));
            }
        }

        return retFile;
    }

    private File getFinderChart(String mode, TableServerRequest request) throws IOException, DataAccessException {
        File f;

        targets = getTargetsFromRequest(request);

        if (targets.size()>maxSearchTargets) {throw QueryUtil.createEndUserException(
            "There are "+targets.size()+" targets. "+
            "Finder Chart only supports "+ maxSearchTargets +" targets or less.");}
        f = handleTargets(mode, request);

        return f;
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
                        same =  ComparisonUtil.equals(last.getData()[0],o.getData()[0]);
                    } else { //compare ra and dec
                        same = ComparisonUtil.equals(last.getData()[1],o.getData()[1])&&
                               ComparisonUtil.equals(last.getData()[2],o.getData()[2]);
                    }
                }
                if (!same) newTable.add(o);
                last = o;
            }
            retFile = createFile(request);
            IpacTableWriter.save(retFile, newTable);
        } catch (IpacTableException e) {
            throw new IOException("IpacTableException: "+e.getMessage());
        }
        return retFile;
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


    private File handleTargets(String mode, TableServerRequest request)
            throws IOException, DataAccessException {
        String subSizeStr= request.getParam("subsize");
        String sources= request.getParam("sources");
        String artifactsWise = request.getParam("wise_artifacts");
        String artifacts2Mass = request.getParam("twomass_artifacts");
        String thumbnailSize= request.getParam("thumbnail_size");

        // use default if not given
        sources = StringUtils.isEmpty(sources) ? "DSS,SDSS,twomass,IRIS,WISE" : sources;
        subSizeStr = StringUtils.isEmpty(subSizeStr) ? "300" : subSizeStr;
        thumbnailSize = StringUtils.isEmpty(thumbnailSize) ? "medium" : thumbnailSize;


        wiseEventWorker= getCheckboxValue(artifactsWise);
        twoMassEventWorker= getCheckboxValue(artifacts2Mass);

        //convert angular size to pixelsize for Heal2Tan
        Float subSize = new Float(subSizeStr);

        DataType dt;
        DataGroup table = null;
        ArrayList<DataType> defs;
        if (mode.equals(FinderChartApi.SIAP) ||
                mode.equals(FinderChartApi.PROG)) {
            defs = new ArrayList<DataType>();
            dt = new DataType(RA, Double.class);
            dt.setFormatInfo(DataType.FormatInfo.createFloatFormat(dt.getFormatInfo().getWidth(), 6));
            defs.add(dt);
            dt = new DataType(DEC, Double.class);
            dt.setFormatInfo(DataType.FormatInfo.createFloatFormat(dt.getFormatInfo().getWidth(), 6));
            defs.add(dt);
            // HTTP-GET API columns
            defs.add(new DataType("externalname", String.class));
            defs.add(new DataType("wavelength", String.class));
            defs.add(new DataType("accessUrl", String.class));
            defs.add(new DataType("naxis1", Integer.class));
            defs.add(new DataType("naxis2", Integer.class));
            defs.add(new DataType("service", String.class));
            defs.add(new DataType("accessWithAnc1Url", String.class));
            defs.add(new DataType("fitsurl", String.class));
            defs.add(new DataType("jpgurl", String.class));
            defs.add(new DataType("shrunkjpgurl", String.class));
            table = new DataGroup("ImageGrid Table", defs);
            ImageGridSupport.addDataGroupAttribute(table, "datatype", "fitshdr");
            ImageGridSupport.addDataGroupAttribute(table, "fixlen", "T");
        } else {
            //create an IPAC table with default attributes.
            defs = ImageGridSupport.createBasicDataDefinitions();
            defs.add(new DataType(OBJ_ID, Integer.class));
            defs.add(new DataType(OBJ_NAME, String.class));
            dt = new DataType(RA, Double.class);
            dt.setFormatInfo(DataType.FormatInfo.createFloatFormat(dt.getFormatInfo().getWidth(), 6));
            defs.add(dt);
            dt = new DataType(DEC, Double.class);
            dt.setFormatInfo(DataType.FormatInfo.createFloatFormat(dt.getFormatInfo().getWidth(), 6));
            defs.add(dt);
            defs.add(new DataType(EVENTWORKER, String.class));
            dt = new DataType(ALL_EVENTWORKER, String.class);
            dt.getFormatInfo().setWidth(100);
            defs.add(dt);
            //defs.get(defs.size()-1).getFormatInfo().setWidth(100);

            table = ImageGridSupport.createBasicDataGroup(defs, true);
            table.getDataDefinitions();
            ImageGridSupport.addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.EVENTWORKER_COLUMN, EVENTWORKER);
            ImageGridSupport.addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.ALL_EVENTWORKER_COLUMN, ALL_EVENTWORKER);
            // show these message for request.setPageSize(0);
            ImageGridSupport.addDataGroupAttribute(table, "INFO", "Please wait...");
            ImageGridSupport.addDataGroupAttribute(table, ImageGridSupport.ATTRIBUTE.GRID_BACKGROUND, "#f6f6f6");
        }
        //populating finder chart services
        String bands[]=null;
        String bandStr;
        for (Target tgt: targets) {
            curTarget = tgt;
            for (String serviceStr: sources.split(",")) {
                serviceStr = serviceStr.trim().equalsIgnoreCase("2mass") ? WebPlotRequest.ServiceType.TWOMASS.name() : serviceStr.toUpperCase();
                WebPlotRequest.ServiceType service = WebPlotRequest.ServiceType.valueOf(serviceStr);
                String bandKey = getBandKey(service);
                if (bandKey!=null) {
                    bandStr = request.getParam(bandKey);
                    if (bandStr !=null) {
                        bands = bandStr.split(",");
                        for (int i=0;i<bands.length;i++) {
                            bands[i]=getComboPair(service, bands[i]);
                        }
                    } else {
                        bands = getServiceComboArray(service);
                    }
                }
                String tnsize = thumbnailSize == null || !thumbnailSizeMap.containsKey(thumbnailSize) ? "small" : thumbnailSize;
                if (mode.equals(FinderChartApi.SIAP) ||
                        mode.equals(FinderChartApi.PROG)) {
                    addDataServiceProducts(request, table, serviceStr, bands, subSize, thumbnailSizeMap.get(tnsize));
                } else {
                    addWebPlotRequests(table, serviceStr, bands, subSize, thumbnailSizeMap.get(tnsize));
                }
            }
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

    private boolean addDataServiceProducts(TableServerRequest request, DataGroup dg, String serviceStr, String bands[],
                                           Float radius, int width) throws IOException {
        boolean success = true;
        WorldPt pt=getTargetWorldPt(curTarget);
        String dateStr="", expanded="", ew, allEW, name=getTargetName(curTarget);

        WebPlotRequest.ServiceType service = WebPlotRequest.ServiceType.valueOf(serviceStr.toUpperCase());
        for (String band: bands) {
//            switch (service) {
//                case TWOMASS:
//                    dateStr= "ORDATE;"+OBS_DATE;
//                    break;
//                case DSS:
//                    dateStr= "DATE-OBS;"+OBS_DATE;
//                    break;
//                case WISE:
//                    dateStr= "MIDOBS;"+MID_OBS;
//                    break;
//                case SDSS:
//                    dateStr= "DATE-OBS;"+OBS_DATE;
//                    break;
//                case IRIS:
//                    dateStr= "DATEIRIS;"+OBS_DATE;
//                    break;
//            }
            DataObject row = new DataObject(dg);
            //row.setDataElement(dg.getDataDefintion(OBJ_ID), targets.indexOf(curTarget)+1);
            //row.setDataElement(dg.getDataDefintion(OBJ_NAME), name);
            row.setDataElement(dg.getDataDefintion(RA), pt.getLon());
            row.setDataElement(dg.getDataDefintion(DEC),pt.getLat());
            row.setDataElement(dg.getDataDefintion("externalname"), getServiceTitle(service));
            row.setDataElement(dg.getDataDefintion("wavelength"), getComboTitle(band));
            //row.setDataElement(dg.getDataDefintion("naxis1"), width);
            //row.setDataElement(dg.getDataDefintion("naxis2"), width);
            row.setDataElement(dg.getDataDefintion("service"), service.name());
            for (String type: new String[] {"accessUrl", "accessWithAnc1Url", "fitsurl", "jpgurl", "shrunkjpgurl"}) {
                row.setDataElement(
                    dg.getDataDefintion(type), getAccessURL(request, pt.getLon(), pt.getLat(), radius, serviceStr, getComboValue(band), type));
            }
            dg.add(row);
        }

        return success;
    }

    private boolean addWebPlotRequests(DataGroup table, String serviceStr, String bands[],
                                       Float radius, int width)
            throws IOException {
        boolean success = true;
        WebPlotRequest wpReq;
        WorldPt pt=getTargetWorldPt(curTarget);
        String dateStr="", expanded="", ew, allEW, name=getTargetName(curTarget);
        try {
            WebPlotRequest.ServiceType service = WebPlotRequest.ServiceType.valueOf(serviceStr.toUpperCase());
            for (String band: bands) {
                if (service.equals(WebPlotRequest.ServiceType.WISE)) {
                    if (!band.startsWith("3a.")) band = "3a."+band;
                }
                if (curTarget.getName()==null || curTarget.getName().length()==0) {
                    if (curTarget instanceof TargetFixedSingle) {
                        TargetFixedSingle fixedSingle = (TargetFixedSingle)curTarget;
                        expanded = String.format("%.6f",fixedSingle.getPosition().getRa())
                                +"+"+String.format("%.6f",fixedSingle.getPosition().getDec());
                        expanded = expanded.replaceAll("\\+\\-","\\-");
                    }
                } else {
                    expanded= curTarget.getName();
                }
                expanded += (" "+getServiceTitle(service)+" "+getComboTitle(band));
                wpReq= getWebPlotRequest(service, band, pt, radius);
                wpReq.setExpandedTitle(expanded);
                wpReq.setZoomType(ZoomType.TO_WIDTH);
                wpReq.setZoomToWidth(width);
                wpReq.setPostCropAndCenter(true);
                wpReq.setRotateNorth(true);
                wpReq.setSaveCorners(true);
                wpReq.setInitialColorTable(1);
                wpReq.setHideTitleDetail(true);
                wpReq.setPreferenceColorKey("FcColorKey");
                //add date info to 2MASS, DSS, WISE, SDSS:
                //dateStr = getDateInfo(wpReq, service);
//                switch (service) {
//                    case TWOMASS:
//                        dateStr= "ORDATE;"+OBS_DATE;
//                        break;
//                    case DSS:
//                        dateStr= "DATE-OBS;"+OBS_DATE;
//                        break;
//                    case WISE:
//                        dateStr= "MIDOBS;"+MID_OBS;
//                        break;
//                    case SDSS:
//                        dateStr= "DATE-OBS;"+OBS_DATE;
//                        break;
//                    case IRIS:
//                        dateStr= "DATEIRIS;"+OBS_DATE;
//                        break;
//                }
//                wpReq.setTitleOptions(WebPlotRequest.TitleOptions.PLOT_DESC_PLUS_DATE);
                wpReq.setTitleOptions(WebPlotRequest.TitleOptions.SERVICE_OBS_DATE);
//                wpReq.setPlotDescAppend(dateStr);

                wpReq.setTitle(getComboTitle(band)/*+" "+dateStr*/);
                ew = getServiceEventWorkerId(service, band, false);
                allEW = getServiceEventWorkerId(service, band, true);
                addWebPlotRequest(table, wpReq, name, getServiceTitle(service), ew, allEW);
            }
        } catch (Exception e) {
            _log.briefInfo(e.getMessage());
            success=false;
        }
        return success;
    }

    private String getServiceEventWorkerId(WebPlotRequest.ServiceType key, String band, boolean all) {
        String retval = "";
        String bandSuffix = band.substring(band.length()-1).toLowerCase();
        String prefixAry[] = null;

        if (key.equals(WebPlotRequest.ServiceType.WISE)) {
            if (all)
                prefixAry = allWiseEventWorker;
            else
                prefixAry = wiseEventWorker;
        }
        else if (key.equals(WebPlotRequest.ServiceType.TWOMASS)) {
            if (all)
                prefixAry = allTwoMassEventWorker;
            else
                prefixAry = twoMassEventWorker;
        }

        if (prefixAry!=null)
            for (String prefix: prefixAry) {
                if (retval.length()>0) retval += ",";
                retval +=prefix+bandSuffix;
            }
        return retval;
    }

    //------------------------------------ private static methods ------------------------------------
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

    private static String getDateInfo(WebPlotRequest wpReq, WebPlotRequest.ServiceType service) {
        String retval = "";

        try {
            FileRetriever retrieve= FileRetrieverFactory.getRetriever(wpReq);
            FileData fileData = retrieve.getFile(wpReq);
            File f= fileData.getFile();
            FitsRead frAry[] = readFits(f);
            String dateStr;
            switch (service) {
                case TWOMASS:
                    dateStr= frAry[0].getHDU().getHeader().getStringValue("ORDATE");
                    if (dateStr.length()>5) dateStr= dateStr.subSequence(0,2)+"-"+dateStr.subSequence(2,4)+"-"+dateStr.subSequence(4,6);
                    if (dateStr.startsWith("0"))
                        dateStr = "20"+dateStr;
                    else
                        dateStr = "19"+dateStr;

                    retval = OBS_DATE+": "+ dateStr;
                    break;
                case DSS:
                    dateStr= frAry[0].getHDU().getHeader().getStringValue("DATE-OBS").split("T")[0];
                    retval = OBS_DATE+": "+dateStr;
                    break;
                case WISE:
                    dateStr= frAry[0].getHDU().getHeader().getStringValue("MIDOBS").split("T")[0];
                    retval = MID_OBS+": "+dateStr;
                    break;
                case SDSS:
                    dateStr= frAry[0].getHDU().getHeader().getStringValue("DATE-OBS");
                    retval = "";
                    for (String v: dateStr.split("/")) {
                        retval  = retval + "-"+v;
                    }
                    if (retval.startsWith("-0"))
                        retval = retval.replaceFirst("-0","200");
                    else if (retval.startsWith("-9"))
                        retval = retval.replaceFirst("-9","199");
                    retval= OBS_DATE+": "+retval;
                    break;
                case IRIS:
                    dateStr= frAry[0].getHDU().getHeader().getStringValue("DATEIRIS");
                    if (dateStr.startsWith("0"))
                        dateStr = "20"+dateStr;
                    else
                        dateStr = "19"+dateStr;
                    retval = OBS_DATE+": "+dateStr.replaceAll("/","-");
                    break;
            }
        } catch (Exception e) {

        }

        return retval;
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

    private static String[] getCheckboxValue(String cb) {
        String[] retval;
        if (cb==null)
        {
            retval=null;
        } else {
            retval= cb.equals("_none_")?null: cb.split(",");
        }
        return retval;
    }

    private void addWebPlotRequest(DataGroup dg, WebPlotRequest wpReq, String name, String groupName, String ew, String allEW)
            throws Exception {
        DataObject row = new DataObject(dg);
        row.setDataElement(dg.getDataDefintion(OBJ_ID), targets.indexOf(curTarget)+1);
        row.setDataElement(dg.getDataDefintion(ImageGridSupport.COLUMN.TYPE.toString()), "req");
        row.setDataElement(dg.getDataDefintion(ImageGridSupport.COLUMN.THUMBNAIL.toString()), wpReq.toString());
        row.setDataElement(dg.getDataDefintion(ImageGridSupport.COLUMN.DESC.toString()), wpReq.getTitle());
        row.setDataElement(dg.getDataDefintion(ImageGridSupport.COLUMN.GROUP.toString()), groupName);
        row.setDataElement(dg.getDataDefintion(OBJ_NAME), name);
        row.setDataElement(dg.getDataDefintion(RA), wpReq.getWorldPt().getLon());
        row.setDataElement(dg.getDataDefintion(DEC),wpReq.getWorldPt().getLat());
        row.setDataElement(dg.getDataDefintion(EVENTWORKER), allEW);
        row.setDataElement(dg.getDataDefintion(ALL_EVENTWORKER), allEW);
        dg.add(row);
    }


    private static WebPlotRequest getWebPlotRequest(WebPlotRequest.ServiceType service, String band, WorldPt pt, Float radius)
            throws Exception{
        WebPlotRequest wpReq=null;
        switch (service) {
            case DSS:
                wpReq= WebPlotRequest.makeDSSRequest(pt, getComboValue(band),radius);
                break;
            case IRIS:
                wpReq= WebPlotRequest.makeIRISRequest(pt, getComboValue(band),radius);
                break;
            case ISSA:
                wpReq= WebPlotRequest.makeISSARequest(pt, getComboValue(band),radius);
                break;
            case MSX:
                wpReq= WebPlotRequest.makeMSXRequest(pt, getComboValue(band),radius);
                break;
            case SDSS:
                wpReq= WebPlotRequest.makeSloanDSSRequest(pt, getComboValue(band), radius);
                break;
            case TWOMASS:
                wpReq= WebPlotRequest.make2MASSRequest(pt, getComboValue(band),radius);
                break;
            case WISE:
                String[] pair= getComboValue(band).split("\\.");
                wpReq= WebPlotRequest.makeWiseRequest(pt, pair[0], pair[1], radius);
                break;
        }
        return wpReq;
    }

    private static String getComboValue(String combo) { return combo.split(";")[0]; }
    private static String getComboTitle(String combo) { return combo.split(";")[1]; }

    private static String getComboPair(WebPlotRequest.ServiceType service, String key) {
        if (service.equals(WebPlotRequest.ServiceType.WISE) && key!= null) key = "3a."+key;
        for (String combo: getServiceComboArray(service)) {
            if (key!= null && key.equals(getComboValue(combo))) return combo;
        }
        return "";
    }
    private static String[] getServiceComboArray(WebPlotRequest.ServiceType key) {
        if (comboMap == null) {
            comboMap= new HashMap<WebPlotRequest.ServiceType, String[]>();
            comboMap.put(WebPlotRequest.ServiceType.DSS,dssCombo);
            comboMap.put(WebPlotRequest.ServiceType.IRIS,irisCombo);
            comboMap.put(WebPlotRequest.ServiceType.ISSA,issaCombo);
            comboMap.put(WebPlotRequest.ServiceType.MSX,msxCombo);
            comboMap.put(WebPlotRequest.ServiceType.SDSS,sDssCombo);
            comboMap.put(WebPlotRequest.ServiceType.TWOMASS,twoMassCombo);
            comboMap.put(WebPlotRequest.ServiceType.WISE,wiseCombo);
        }
        return comboMap.get(key);
    }

    private static String getServiceTitle(WebPlotRequest.ServiceType key) {
        if (serviceTitleMap == null) {
            serviceTitleMap= new HashMap<WebPlotRequest.ServiceType, String>();
            serviceTitleMap.put(WebPlotRequest.ServiceType.DSS, WebPlotRequest.ServiceType.DSS.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.IRIS, "IRAS (IRIS)");
            serviceTitleMap.put(WebPlotRequest.ServiceType.ISSA, WebPlotRequest.ServiceType.ISSA.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.MSX, WebPlotRequest.ServiceType.MSX.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.SDSS, WebPlotRequest.ServiceType.SDSS.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.TWOMASS, "2MASS");
            serviceTitleMap.put(WebPlotRequest.ServiceType.WISE, WebPlotRequest.ServiceType.WISE.toString());
            serviceTitleMap.put(WebPlotRequest.ServiceType.SDSS, WebPlotRequest.ServiceType.SDSS.toString());
        }

        return serviceTitleMap.get(key);
    }

    private static String getBandKey(WebPlotRequest.ServiceType key) {
        if (bandMap==null) {
            bandMap = new HashMap<WebPlotRequest.ServiceType, String>();
            bandMap.put(WebPlotRequest.ServiceType.DSS, "dss_bands");
            bandMap.put(WebPlotRequest.ServiceType.IRIS, "iras_bands");
            bandMap.put(WebPlotRequest.ServiceType.TWOMASS, "twomass_bands");
            bandMap.put(WebPlotRequest.ServiceType.WISE, "wise_bands");
            bandMap.put(WebPlotRequest.ServiceType.SDSS, "sdss_bands");
        }
        return bandMap.get(key);
    }

    /**
     * Finder Chart services
     * combo string format: option;title
     */
    private final static String dssCombo[]={
            "poss1_blue;DSS1 Blue",
            "poss1_red;DSS1 Red",
            "poss2ukstu_blue;DSS2 Blue",
            "poss2ukstu_red;DSS2 Red",
            "poss2ukstu_ir;DSS2 IR",
            /*"quickv;Quick-V Survey",
            "phase2_gsc2;HST Phase 2 Target Positioning(GSC 2)",
            "phase2_gsc1;HST Phase 1 Target Positioning(GSC 1)",
            "all;The best of a combined list of all plates"*/};

    private final static String twoMassCombo[] = {
            "j;J",
            "h;H",
            "k;K", };

    private final static String issaCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};

    private final static String irisCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};

    private final static String msxCombo[] = {
            "3;A (8.28 microns)",
            "4;C (12.13 microns)",
            "5;D (14.65 microns)",
            "6;E (21.3 microns)"};

    private final static String wiseCombo[]={
            "3a.1;w1",
            "3a.2;w2",
            "3a.3;w3",
            "3a.4;w4"};

    private final static String sDssCombo[]={
            "u;u","g;g","r;r","i;i","z;z"
    };

    private static HashMap<String, Integer> thumbnailSizeMap = new HashMap<String, Integer>() {
        {
            put("small",128);
            put("medium",192);
            put("large",256);
        }
    };

    public static String getAccessURL(TableServerRequest request, Double ra, Double dec, Float size, String source, String band, String type) {
        String url = ServerContext.getRequestOwner().getBaseUrl()+"servlet/api?" + MODE + "=" + FinderChartApi.GET_IMAGE;
        String thumbnailSize;

        if (type.equals("jpgurl")) {
            thumbnailSize = "large";
        } else if (type.equals("shrunkjpgurl")) {
            thumbnailSize = "small";
        } else {
            thumbnailSize = "medium";
        }
        url += "&RA="+ra;
        url += "&DEC="+dec;
        url += "&subsetsize=" + size * 60;
        url += "&thumbnail_size="+thumbnailSize;
        url += "&" + FinderChartApi.Param.survey.name() + "=" + source;

        if (request.containsParam(FinderChartApi.Param.grid.name())) {
            url += "&" + FinderChartApi.Param.grid.name() + "=" + request.getParam(FinderChartApi.Param.grid.name());
        }
        if (request.containsParam(FinderChartApi.Param.marker.name())) {
            url += "&" + FinderChartApi.Param.marker.name() + "=" + request.getParam(FinderChartApi.Param.marker.name());
        }
        if (request.containsParam(FinderChartApi.Param.orientation.name())) {
            url += "&" + FinderChartApi.Param.orientation.name() + "=" + request.getParam(FinderChartApi.Param.orientation.name());
        }
        if (request.containsParam(FinderChartApi.Param.reproject.name())) {
            url += "&" + FinderChartApi.Param.reproject.name() + "=" + request.getParam(FinderChartApi.Param.reproject.name());
        }

        if (source.equalsIgnoreCase("twomass"))
            url += "&twomass_bands="+band;
        else if (source.equals("DSS"))
            url += "&dss_bands="+band;
        else if (source.equals("WISE")) {
            if (band.startsWith("3a.")) band = band.replaceFirst("3a.", "");
            url += "&wise_bands="+band;
        }
        else if (source.equals("SDSS"))
            url += "&sdss_bands="+band;
        else if (source.equals("IRIS"))
            url += "&iras_bands="+band;
        url += "&type="+type;
        return url;
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