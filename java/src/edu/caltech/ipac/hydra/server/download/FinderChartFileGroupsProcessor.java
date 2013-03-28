package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.FileRetrieverFactory;
import edu.caltech.ipac.firefly.server.visualize.PngRetrieve;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.hydra.server.query.QueryFinderChart;
import edu.caltech.ipac.hydra.server.query.QueryFinderChartArtifact;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.pdf.PdfUtils;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: May 4, 2012
 * Time: 4:02:23 PM
 * To change this template use File | Settings | File Templates.
 */
@SearchProcessorImpl(id = "FinderChartDownload")
public class FinderChartFileGroupsProcessor extends FileGroupsProcessor {

    enum ImageType {FITS, PNG};

    private static final Logger.LoggerImpl logger = Logger.getLogger();
    public static final String FINDERCHART_HTML_TH = AppProperties.getProperty("FinderChart.html.th");
    public static final String FINDERCHART_HTML_TD = AppProperties.getProperty("FinderChart.html.td");    
    public static final String FINDERCHART_HTML_LAYER = AppProperties.getProperty("FinderChart.html.layer");
    public static final String FINDERCHART_HTML_PAGE_BREAK= AppProperties.getProperty("FinderChart.html.page_break");
    public static final String FINDERCHART_HTML_DATE= AppProperties.getProperty("FinderChart.html.date");
    public static final String MAX_TARGETS_PER_PDF = "maxTargetsPerPdf";
    private static String DECIMAL_PRECISION = "%.6f";
    private static int _nameCnt=1;
    private int maxTargetsPerPdf = 1000;
    private Map<String,Circle> circleMap= new HashMap<String,Circle>();
    private Map<String,String> bandMap= new HashMap<String,String>();
    private String layerInfoAry[]=null;
    private boolean hasArtifactFiles = false;

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    // ---- private methods ----     
    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        double subSize= 0.;
        hasArtifactFiles = false;
        List<FileGroup> retval=null;
        ServerRequest searchR= request.getSearchRequest();
        SearchManager man= new SearchManager();
        SearchProcessor processor= man.getProcessor(searchR.getRequestId());
        if (request.containsParam(MAX_TARGETS_PER_PDF)) {
            maxTargetsPerPdf = Integer.parseInt(request.getParam(MAX_TARGETS_PER_PDF));
        }
        if (searchR instanceof TableServerRequest) {
            String scope = request.getParam("scope");
            if (scope!=null && scope.equals("all")) ((TableServerRequest)searchR).setFilters(null);
            //com.google.gwt.gen2.table.client.CachedTableModel.requestRows(CachedTableModel.java:318)
            // set pagesize to an "interesting" value.  For a quick and dirty fix, manually set pagesize to 5000.
            ((TableServerRequest) searchR).setPageSize(Integer.MAX_VALUE);
        }
        DataGroupPart primaryData= (DataGroupPart)processor.getData(searchR);
        TableMeta meta= QueryUtil.getRawDataSet(primaryData).getMeta();
        processor.prepareTableMeta(meta, Collections.unmodifiableList(primaryData.getTableDef().getCols()), searchR);
        DataGroup dataGroup= primaryData.getData();

        String fileType = request.getParam("file_type");
        boolean itemize=false;
        if (request.containsParam("itemize")) {
            itemize = request.getBooleanParam("itemize");
        }

        List<FileInfo> retList= null;
        if (fileType!=null && fileType.equals("png")) {
            retList= retrieveFiles(ImageType.PNG, itemize, dataGroup, request);
        } else if (fileType!=null && fileType.equals("fits")) {
            retList= retrieveFiles(ImageType.FITS, itemize, dataGroup, request);
        } else if (fileType!=null)
            retList= retrieveHtmlFiles(itemize, request, dataGroup, fileType.equals("pdf"));

        if (retList!=null) {
            if (!fileType.equals("pdf")) {
                createFinderChartFilesInfo(request.getSearchRequest().getParam("sources"), retList);
            }

            retval= new ArrayList<FileGroup>(1);
            retval.add(new FileGroup(retList,null,0,"Finder Chart"));
        }

        return retval;
    }

    private List<FileInfo> retrieveHtmlFiles(boolean itemize, DownloadRequest request, DataGroup dataGroup, boolean exportPdf)
            throws IOException, DataAccessException {
        if (request.containsParam("LayerInfo")) layerInfoAry= request.getParam("LayerInfo").split(Constants.SPLIT_TOKEN);
        List<FileInfo> retList = retrieveFiles(ImageType.PNG, itemize, dataGroup, request);

        File outHtml;
        Map<String, Map<String, ArrayList<FileInfo>>> pngMap = null, subPngMap=null;
        String fname, survey= null, position= null;
        FileInfo fInfo = null;
        for (FileInfo fi: retList) {
            if (!fi.getExternalName().endsWith(".png")) continue;
            position= extractPositon(fi.getExternalName());
            survey= extractSurvey(fi.getExternalName());
            if (position!=null && survey!=null) {
                if (pngMap==null) pngMap= new LinkedHashMap<String, Map<String, ArrayList<FileInfo>>>();
                if (!pngMap.containsKey(position)) pngMap.put(position, new LinkedHashMap<String, ArrayList<FileInfo>>());
                if (!pngMap.get(position).containsKey(survey)) pngMap.get(position).put(survey, new ArrayList<FileInfo>());
                pngMap.get(position).get(survey).add(fi);
            } else {
                //todo: how to handle null pointer?
            }
        }

        //read html template and export png files by positions.
        if (pngMap!=null && pngMap.size()>0) {
            if (exportPdf) {
                exportPdf(pngMap, itemize, retList);
            } else {
                subPngMap = new LinkedHashMap<String, Map<String, ArrayList<FileInfo>>>();
                for (String pos: pngMap.keySet()) {
                    subPngMap.clear();
                    subPngMap.put(pos, pngMap.get(pos));
                    outHtml = createHtml(subPngMap, exportPdf);
                    if (outHtml!=null) {
                        if (itemize) {
                            fname = pos + "/";
                        } else {
                            fname = "";
                        }
                        fname += ("fc_"+pos+".html");
                        fname = fname.replaceAll(" ","_");
                        fInfo = new FileInfo(outHtml.getPath(), fname, outHtml.length());
                        retList.add(fInfo);
                    }
                }
            }
        } else {
            retList.clear();            
        }

        if (retList.size()==0) {
            throw new IOException(FinderChartFileGroupsProcessor.class.getName()+".retrieveHtmlFiles(): Unable to "+
                        "process request:\n"+request);
        }
        return retList;
    }

    private void exportPdf(Map<String, Map<String, ArrayList<FileInfo>>> pngMap, boolean itemize, List<FileInfo> retList) {
        String fname;

        if (pngMap.size()> maxTargetsPerPdf)
            fname="fc_multi_targets_1.pdf";
        else if (pngMap.size()>1)
            fname="fc_multi_targets.pdf";
        else
            fname="fc_"+pngMap.keySet().toArray()[0]+".pdf";

        retList.clear();

        Map<String, Map<String, ArrayList<FileInfo>>> currentMap =
                new LinkedHashMap<String, Map<String, ArrayList<FileInfo>>>();
        int counter = 1, idx=1;
        if (pngMap.size()==1 || itemize) {
            for (String key: pngMap.keySet()) {
                currentMap.put(key, pngMap.get(key));
                fname="fc_"+key+".pdf";
                try {
                    createPdf(currentMap, fname, retList);
                } catch (Exception e) {
                    logger.warn(e,"Could not convert PDF file.", e.getMessage());
                }
                currentMap.clear();
            }
        } else {
            for (String key: pngMap.keySet()) {
                currentMap.put(key, pngMap.get(key));
                if (counter> maxTargetsPerPdf) {
                    try {
                        createPdf(currentMap, fname, retList);
                    } catch (Exception e) {
                        logger.warn(e,"Could not convert PDF file.", e.getMessage());
                    }
                    currentMap.clear();
                    counter=1;
                    idx++;
                    fname="fc_multi_targets_"+idx+".pdf";
                }
                counter++;
            }
            if (counter>0) {
                try {
                    createPdf(currentMap, fname, retList);
                } catch (Exception e) {
                    logger.warn(e,"Could not convert PDF file.", e.getMessage());
                }
            }
        }
    }

    private void createPdf(Map<String, Map<String, ArrayList<FileInfo>>> pngMap, String fname, List<FileInfo> retList)
            throws Exception {
        FileInfo fInfo;
        File bigHTML = createHtml(pngMap, true);
        File pdf = PdfUtils.convertPDF(bigHTML);

        fname = fname.replaceAll(" ","_");
        fInfo = new FileInfo(pdf.getPath(), fname, pdf.length());
        retList.add(fInfo);
    }

    private List<FileInfo> retrieveFiles(ImageType type, boolean itemize, DataGroup dataGroup, DownloadRequest request)
            throws DataAccessException {
        List<FileInfo> retList= new ArrayList<FileInfo>();
        WebPlotRequest wpReq;
        FileInfo fi;
        String wpReqStr, filename;
        String plotStateAry[]=null, drawInfoListAry[]=null;
        int counter = 0;
        QueryFinderChartArtifact queryFinderChartArtifact = new QueryFinderChartArtifact();

        if (request.containsParam("PlotStates")) {
            plotStateAry=request.getParam("PlotStates").split("&&");
        } else {
            // todo: how to handle request from BaseProductDownload?
            plotStateAry = new String[]{""};
        }
        if (request.containsParam("DrawInfoList")) {
            drawInfoListAry=request.getParam("DrawInfoList").split("&&");
        } else {
            // todo: how to handle request from BaseProductDownload?
            drawInfoListAry = new String[]{""};
        }

        File imageFile;
        double sizeInArcSec;
        String position="", prevPosition="";
        Circle requestArea;
        for (DataObject dObj: dataGroup) {
            wpReqStr= (String) dObj.getDataElement("THUMBNAIL");
            wpReq= WebPlotRequest.parse(wpReqStr);
            try {
                if (drawInfoListAry==null) throw new DataAccessException("Unable to process DrawInfoList.");
                addArtifactFiles(itemize, dObj, queryFinderChartArtifact, wpReq,
                        drawInfoListAry[counter % drawInfoListAry.length], retList);
                imageFile=null;
                if (type.equals(ImageType.PNG)) {
                    if (plotStateAry==null) throw new DataAccessException("Unable to process PlotStates.");
                    imageFile= PngRetrieve.getFile(wpReq, plotStateAry[counter % plotStateAry.length],
                            drawInfoListAry[counter % drawInfoListAry.length], retList);
                } else if (type.equals(ImageType.FITS)) {
                    imageFile= FileRetrieverFactory.getRetriever(wpReq).getFile(wpReq).getFile();
                }
                if (imageFile==null) continue;
                sizeInArcSec = MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC,
                                Double.parseDouble(wpReq.getParam("SizeInDeg")));
                filename = getExternalFitsFilename(itemize, dObj, sizeInArcSec, FileUtil.getExtension(imageFile));
                fi= new FileInfo(imageFile.getPath(), filename, imageFile.length());
                addBandDescriptionMap(wpReq.getTitle());
                retList.add(fi);
                position= extractPositon(filename);
                if (!position.equals(prevPosition)) {
                    requestArea = new Circle(wpReq.getRequestArea().getCenter(), sizeInArcSec);
                    circleMap.put(position, requestArea);
                    prevPosition=position;
                }
            } catch (Exception e) {
                logger.warn(e,"Could not retrieve file for WebPlotRequest: " + wpReqStr);
            }
            counter++;
        }

        return retList;
    }

    private void addArtifactFiles(boolean itemize, DataObject dObj, QueryFinderChartArtifact queryFinderChartArtifact,
                                      WebPlotRequest wpReq, String drawInfoListStr, List<FileInfo> retList)
                throws Exception {

        List<String> drawInfoList = StringUtils.asList(drawInfoListStr, Constants.SPLIT_TOKEN);
        StaticDrawInfo sdi;
        String artifactStr, filename;
        FileInfo fi;
        File artifact;
        boolean isArtifact = false;
        if (drawInfoList!=null && drawInfoList.size()>0) {
            for (String dStr: drawInfoList) {
                sdi = StaticDrawInfo.parse(dStr);
                artifactStr = sdi.getLabel();
                isArtifact = false;
                for (String c: new String[]{"glint_arti_", "pers_arti_", "diff_spikes_","halos_","ghosts_","latents_"}){
                    if (artifactStr.startsWith(c)) {
                        isArtifact = true;
                        break;
                    }
                }

                if (isArtifact) {
                    //todo: find artifact file
                    artifact = queryFinderChartArtifact.getFinderChartArtifact(findArtifact(wpReq, artifactStr));
                    if (artifact==null) continue;
                    filename = getExternalArtifactFilename(itemize, dObj, artifactStr, FileUtil.getExtension(artifact));
                    fi= new FileInfo(artifact.getPath(), filename, artifact.length());
                    retList.add(fi);
                    hasArtifactFiles = true;
                }
            }
        }
    }

    private void addArtifactFiles(boolean itemize, DataObject dObj, QueryFinderChartArtifact queryFinderChartArtifact,
                                  WebPlotRequest wpReq, String ew,
                                  List<FileInfo> artifactList, List<FileInfo> retList)
            throws Exception {
        String artifactAry[], filename;
        FileInfo fi;
        File artifact;
        if (ew!=null) {
            if (ew.contains(",")) {
                artifactAry = ew.split(",");
            } else {
                artifactAry = new String[]{ew};
            }
            for (String artifactStr: artifactAry) {
                artifact = queryFinderChartArtifact.getFinderChartArtifact(findArtifact(wpReq, artifactStr));
                if (artifact==null) continue;
                filename = getExternalArtifactFilename(itemize, dObj, artifactStr, FileUtil.getExtension(artifact));
                fi= new FileInfo(artifact.getPath(), filename, artifact.length());
                if (artifactList!=null) artifactList.add(fi);
                retList.add(fi);
                hasArtifactFiles = true;
            }
        }
    }

    private File createFinderChartFilesInfo(String sources, List<FileInfo> retList) {
        File retval=null;
        InputStream in = getClass().getResourceAsStream("resources/FinderChartFilesInfo.txt");
        BufferedReader br=null;
        BufferedWriter bw= null;
        String fname= "FinderChartFilesInfo.txt";

        boolean has2Mass = sources.contains("twomass");
        boolean hasWise = sources.contains("WISE");
        boolean ok;
        if (hasArtifactFiles && (has2Mass||hasWise) ){
            retval= new File(VisContext.getVisSessionDir(), fname);
            try {
                br= new BufferedReader(new InputStreamReader(in));
                bw= new BufferedWriter(new FileWriter(retval));
                String current = br.readLine();

                while (current != null) {
                    current= current.trim();
                    ok= (current.contains("2mass") && has2Mass) ||
                        (current.contains("wise") && hasWise) ||
                        (!current.contains("2mass") && !current.contains("wise"));
                    if (ok) {
                        bw.write(current);
                        bw.newLine();
                    }
                    current= br.readLine();
                }

                retList.add(new FileInfo(retval.getPath(), retval.getName(), retval.length()));

            } catch (IOException e) {
                logger.warn(e,"Could not create Finder Chart Files Info file.", e.getMessage());
            }finally {
                if (br != null) FileUtil.silentClose(br);
                if (bw != null) FileUtil.silentClose(bw);
            }
        }

        return retval;
    }

    private TableServerRequest findArtifact(WebPlotRequest wqReq, String artifact) {
        TableServerRequest req = new TableServerRequest();
        if (artifact.startsWith("glint_") || artifact.startsWith("pers_")) {
            req.setParam("service", "2mass");
            req.setSafeParam("type", artifact.split("_")[0]);
        } else {
            req.setParam("service", "wise");
            if (artifact.startsWith("diff_spikes_")) req.setSafeParam("type", "D");
            else if (artifact.startsWith("halos_")) req.setSafeParam("type", "H");
            else if (artifact.startsWith("ghosts_")) req.setSafeParam("type", "O");
            else if (artifact.startsWith("latents_")) req.setSafeParam("type", "P");
        }
        req.setParam("band", artifact.substring(artifact.length()-1));
        req.setSafeParam("UserTargetWorldPt", wqReq.getParam("WorldPt"));
        req.setSafeParam("subsize", wqReq.getParam("SizeInDeg"));

        return req;
    }


    private File createHtml(Map<String, Map<String, ArrayList<FileInfo>>> pngMap, boolean exportPdf)
            throws IOException {
        File retval;
        InputStream in = getClass().getResourceAsStream("resources/FinderChartHeader.html");
        BufferedReader br=null;
        BufferedWriter bw= null;
        String fname;
        int counter, total;
        fname= "fc_Multi_Targets-"+_nameCnt+"-"+FileUtil.getHostname()+".html";
        retval= new File(VisContext.getVisSessionDir(), fname);
        retval= FileUtil.createUniqueFileFromFile(retval);
        try {
            br= new BufferedReader(new InputStreamReader(in));
            bw= new BufferedWriter(new FileWriter(retval));
            String current = br.readLine();

            while (current != null) {
                current= current.trim();
                if (current.contains("#TARGETS#")) {
                    counter=0;
                    for (String pos: pngMap.keySet()) {
                        if (counter>0) {
                            bw.write(FINDERCHART_HTML_PAGE_BREAK);
                            bw.newLine();
                        }
                        writeOneTarget(bw, pos, pngMap.get(pos), exportPdf);
                        counter++;
                    }
                } else {
                    bw.write(current);
                    bw.newLine();
                }
                current= br.readLine();
            }
            _nameCnt++;
        } finally {
            if (br != null) FileUtil.silentClose(br);
            if (bw != null) FileUtil.silentClose(bw);
        }
        return retval;
    }

    private void writeOneTarget(BufferedWriter bw, String position, Map<String, ArrayList<FileInfo>> surveyMap,
                                boolean exportPdf) {
        InputStream in = getClass().getResourceAsStream("resources/FinderChartOneTarget.html");
        BufferedReader br=null;
        String radius, raDec;
        WorldPt wpt;
        if (in!=null) {
            try {
                br= new BufferedReader(new InputStreamReader(in));
                String current = br.readLine();
                while (current != null) {
                    current= current.trim();
                    if (current.contains("#POSITION#")) {
                        radius= Long.toString(Math.round(circleMap.get(position).getRadius()));
                        wpt= circleMap.get(position).getCenter();
                        raDec= wpt.getLon()+"+"+wpt.getLat()+" "+wpt.getCoordSys().toString().replaceAll("_"," ");
                        raDec= raDec.replaceAll("\\+"," \\+").replaceAll("\\+\\-","\\-");

                        if (position!=null && position.startsWith(String.format(DECIMAL_PRECISION, (Double)wpt.getLon())))
                            current = current.replaceAll("#POSITION#", raDec+"; "+radius+"x"+radius+"arcsec");
                        else
                            current = current.replaceAll("#POSITION#", position+"; "+raDec+"; "+radius+"x"+radius+"arcsec");

                        current += (FINDERCHART_HTML_DATE.replaceAll("#DATE#",UTCTimeUtil.getCurrentDate().toString()));
                    }
                    if (current.trim().equals("#TABLE#")) {
                        writeTable(bw, surveyMap, exportPdf);
                    } else {
                        bw.write(current);
                        bw.newLine();
                    }
                    current= br.readLine();
                }
            } catch (Exception e) {
                e.printStackTrace(); //todo:  handle exception here
            } finally {
                if (br != null) FileUtil.silentClose(br);
            }
        }
    }

    private void writeTable(BufferedWriter writer, Map<String, ArrayList<FileInfo>> surveyMap, boolean exportPdf) throws IOException {
        String line, label;
        for (String survey: surveyMap.keySet()) {
            line = FINDERCHART_HTML_TH.replaceAll("TITLE",getSurveyTitle(survey)+" "+getLayerInfo(survey));
            writer.write(line);
            writer.newLine();

            writer.write("<tr>");
            writer.newLine();

            for (FileInfo fi: surveyMap.get(survey)) {
                label = findLabel(fi.getExternalName());
                if (bandMap.containsKey(label)) label = "<b>"+label+"</b>"+ bandMap.get(label);
                line = FINDERCHART_HTML_TD.replaceAll("LABEL", label);

                line = line.replaceAll("SRC", (new File(
                        exportPdf?fi.getInternalFilename():fi.getExternalName())).getName());
                writer.write(line);
                writer.newLine();
            }

            writer.write("</tr>");
            writer.newLine();
        }
    }

    private String getLayerInfo(String survey) throws IOException {
        String line;
        StringBuffer sb = new StringBuffer();
        String layerInfoPair[];
        for (String layer: layerInfoAry) {
            layerInfoPair = layer.split("==");
            if (layerInfoPair[0].toLowerCase().contains(survey)) {
                line = FINDERCHART_HTML_LAYER.replaceAll("#TITLE#",layerInfoPair[0])
                        .replaceAll("#COLOR#",layerInfoPair[1]).replaceAll("&amp;","&");
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void addBandDescriptionMap(String band) {
        if (band!=null) {
            String key = null, value = null;

            for (String split: new String[]{QueryFinderChart.OBS_DATE,QueryFinderChart.MID_OBS,"DATE-OBS"}) {
                if (band.contains(split)) {
                    key= band.split(split)[0].trim();
                    value= " "+split+band.split(split)[1];
                    break;
                }
            }

            if (key!=null && value!=null && !bandMap.containsKey(key)) {
                bandMap.put(key,value);
            }
        }
    }

    // ---- private static methods ----
    private static String getSurveyTitle(String survey) {
        String retval = null;
        if (survey.equals("iras")) {
            retval = "IRAS (IRIS)";
        } else {
            retval = survey.toUpperCase();
        }
        return retval;
    }

    private static String findLabel(String fname) {
        String retval="";
        if (fname!=null) {
                 if (fname.contains("sdssu")) retval="u";
            else if (fname.contains("sdssg")) retval="g";
            else if (fname.contains("sdssr")) retval="r";
            else if (fname.contains("sdssi")) retval="i";
            else if (fname.contains("sdssz")) retval="z";
            else if (fname.contains("dssdss2red")) retval="DSS2 Red";
            else if (fname.contains("dssdss2blue")) retval="DSS2 Blue";
            else if (fname.contains("dssdss2ir")) retval="DSS2 IR";
            else if (fname.contains("dssdss1red")) retval="DSS1 Red";
            else if (fname.contains("dssdss1blue")) retval="DSS1 Blue";
            else if (fname.contains("2massj")) retval="J";
            else if (fname.contains("2massh")) retval="H";
            else if (fname.contains("2massk")) retval="K";
            else if (fname.contains("iras12")) retval="12 microns";
            else if (fname.contains("iras25")) retval="25 microns";
            else if (fname.contains("iras60")) retval="60 microns";
            else if (fname.contains("iras100")) retval="100 microns";
            else if (fname.contains("wise1")) retval="w1";
            else if (fname.contains("wise2")) retval="w2";
            else if (fname.contains("wise3")) retval="w3";
            else if (fname.contains("wise4")) retval="w4";
        }
        return retval;
    }

    private static String extractPositon(String filename) {
        String retval = null;
        String[] tokens = null;
        if (filename!=null && filename.contains("_")) {
            if (filename.contains("/")) filename = filename.split("/")[1];
            tokens = filename.split("_");
            if (tokens.length>2) {
                retval = filename.substring(tokens[0].length(), filename.length()-tokens[tokens.length-1].length());
                retval = retval.replaceAll("_"," ").trim();
            }
        }
        return retval;
    }

    private static String extractSurvey(String externalFilename) {
        String retval= null;
        String[] surveys = new String[] {"sdss","2mass","dss","iras","wise"};
        for (String survey:surveys) {
            if (externalFilename.contains("_"+survey)) {
                retval = survey;
                break;
            }
        }
        return retval;
    }

    private static String getExternalFitsFilename(boolean itemize, DataObject dObj, double size, String ext) {
        String retval = "fc_";
        String imageSize= String.format("%.1f", size); //may use it in future.
        String objname = (String)dObj.getDataElement("objname");
        String survey = (String)dObj.getDataElement("GROUP");
        String band = (String)dObj.getDataElement("DESC");
        String ra= String.format(DECIMAL_PRECISION, (Double)dObj.getDataElement("ra"));
        String dec= String.format(DECIMAL_PRECISION, (Double)dObj.getDataElement("dec"));
        String target= (ra + "+" + dec).replace("+-","-");
        String path="";
        if (band!=null && band.contains(QueryFinderChart.OBS_DATE)) {
            band = band.split(QueryFinderChart.OBS_DATE)[0];
        } else if (band!=null && band.contains(QueryFinderChart.MID_OBS)) {
            band = band.split(QueryFinderChart.MID_OBS)[0];
        }

        if (objname!=null) {
            retval +=objname.replace(" ","_");
            path = objname.replace(" ","_");
        } else {
            retval +=target;
            path = target;
        }

        if (survey!=null) {
            survey= survey.toLowerCase();
            if (survey.equals("iras (iris)")) survey="iras";
        }

        if (band!=null && survey!=null) {
            if (survey.equals("wise")) {
                band = band.replace("w","").replace(" ","").trim().toLowerCase();
            } else {
                band = band.replace("/", "").replace("microns","").replace("ukstu","").replace(" ","").toLowerCase();
            }
        }
        retval = retval+"_"+survey+band+"."+ext;
        if (itemize) retval = path +"/"+ retval;
        return retval;
    }

    private static String getExternalArtifactFilename(boolean itemize, DataObject dObj, String artifact, String ext) {
        String retval = "fc_";
        String objname = (String)dObj.getDataElement("objname");
        String survey = (String)dObj.getDataElement("GROUP");
        String band = (String)dObj.getDataElement("DESC");
        String ra= String.format(DECIMAL_PRECISION, (Double)dObj.getDataElement("ra"));
        String dec= String.format(DECIMAL_PRECISION, (Double)dObj.getDataElement("dec"));
        String target= (ra + "+" + dec).replace("+-","-");
        String path = "";
        if (band!=null && band.contains(QueryFinderChart.OBS_DATE)) {
            band = band.split(QueryFinderChart.OBS_DATE)[0];
        } else if (band.contains(QueryFinderChart.MID_OBS)) {
            band = band.split(QueryFinderChart.MID_OBS)[0];
        }

        if (objname!=null) {
            retval +=objname.replace(" ","_");
            path = objname.replace(" ","_")+path;
        } else {
            retval +=target;
            path = target+path;
        }

        if (survey!=null) {
            survey= survey.toLowerCase();
        }

        if (band!=null && survey!=null) {
            if (survey.equals("wise")) {
                band = band.replace("w","").replace(" ","").trim().toLowerCase();
            } else {
                band = band.replace("/", "").replace("microns","").replace(" ","").toLowerCase();
            }
        }

        if (artifact.startsWith("glint_") || artifact.startsWith("pers_")) {
            artifact = get2MassArtifactLabel(artifact);
        } else {
            artifact = getWiseArtifactLabel(artifact);
        }

        retval = retval+"_"+survey+band+"_"+artifact+"."+ext;
        if (itemize) retval = path +"/"+ retval;
        return retval;
    }

    private static String getWiseArtifactLabel(String artifact) {
        String retval = null;

        if (artifact!=null) {
            if (artifact.startsWith("diff_spikes_3_"))  retval= "art_D";
            else if (artifact.startsWith("halos_"))     retval= "art_H";
            else if (artifact.startsWith("ghosts_"))    retval= "art_O";
            else if (artifact.startsWith("latents_"))   retval= "art_P";
        }
        return retval;
    }

    private static String get2MassArtifactLabel(String artifact) {
        String retval = null;

        if (artifact!=null) {
            if (artifact.startsWith("glint_"))      retval="art_glint";
            else if (artifact.startsWith("pers_"))  retval="art_persistence";
        }
        return retval;
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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