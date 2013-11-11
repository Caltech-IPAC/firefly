package edu.caltech.ipac.hydra.server.servlets;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.servlets.BaseHttpServlet;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.util.PositionParser;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.hydra.server.download.FinderChartFileGroupsProcessor;
import edu.caltech.ipac.hydra.server.query.QueryFinderChart;
import edu.caltech.ipac.hydra.server.xml.finderchart.ErrorTag;
import edu.caltech.ipac.hydra.server.xml.finderchart.FcXmlToJava;
import edu.caltech.ipac.hydra.server.xml.finderchart.FinderChartTag;
import edu.caltech.ipac.hydra.server.xml.finderchart.ImageTag;
import edu.caltech.ipac.hydra.server.xml.finderchart.InputTag;
import edu.caltech.ipac.hydra.server.xml.finderchart.ResultTag;
import edu.caltech.ipac.targetgui.net.TargetNetwork;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 /**
 * Date: 9/25/13
 *
 * Finder Chart API implementation based on specification written here:
 * http://irsa.ipac.caltech.edu/applications/FinderChart/docs/finderProgramInterface.html
 *
 * @author loi
 * @version $Id: $
 */
public class FinderChartApi extends BaseHttpServlet {

    // http://localhost:8080/applications/finderchart/servlet/sia?mode=prog&locstr=m51&subsetsize=2.25&grid=true

    public enum Param {mode, locstr, subsetsize, survey, orientation, reproject, grid, marker,  // finderchart API params
        grid_orig, grid_shrunk, markervis_orig, markervis_shrunk,  // these 4 are deprecated from previous finderchart API
        RA, DEC, POS }

    public static final String PROG = "prog";
    public static final String SIAP = "siap";
    public static final String GET_IMAGE = "getImage";
    private List<String> modes = Arrays.asList(PROG, SIAP, GET_IMAGE);
    private static final String API_ONLY_PARAMS = "id|" + StringUtils.toString(Param.values(), "|");

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    PositionParser parser = new PositionParser(new PositionParser.Helper(){
                public double convertStringToLon(String s, CoordinateSys coordsys) {
                    try {
                        boolean eq= coordsys.isEquatorial();
                        return CoordUtil.sex2dd(s,false, eq);
                    } catch (CoordException e) {}
                    return Double.NaN;
                }

                public double convertStringToLat(String s, CoordinateSys coordsys) {
                    try {
                        boolean eq= coordsys.isEquatorial();
                        return CoordUtil.sex2dd(s,true, eq);
                    } catch (CoordException e) {}
                    return Double.NaN;
                }

                public WorldPt resolveName(String objName) {
                    try {
                        return TargetNetwork.getNedThenSimbad(objName);
                    } catch (FailedRequestException e) {
                        return null;
                    }
                }

                public boolean matchesIgnoreCase(String s, String regExp) {
                    return s != null && regExp != null && s.matches("(?i)" + regExp);

                }
    });


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        LOG.debug("Query string", req.getQueryString());
        Map<String, String> params = convertToStringMap(req.getParameterMap());
        try {
            String mode = params.get(Param.mode.name());

            WorldPt wp = getWorldPt(params);
            TableServerRequest searchReq = makeRequest(params, wp);

            if (mode.equals(GET_IMAGE)) {
                List<FileInfo> files = getDataFiles(searchReq);
                if (files.size()<1) {
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NO DATA: no file returned. " + searchReq);
                } else if (files.size()==1) {
                    sendSingleProduct(files.get(0), res);
                } else {
                    String fname = ("FinderChartFiles_"+req.getParameter("RA")+"+"+req.getParameter("DEC")
                            +"_"+req.getParameter("subsetsize")).replaceAll("\\+\\-","\\-");
                    sendZip(fname, files, res);
                }

            } else {
                DataGroupPart dgpart = new SearchManager().getDataGroup(searchReq);
                if (mode.equals(PROG)) {
                    sendFcXml(res, searchReq, params, wp, dgpart);
                } else if (mode.equals(SIAP)) {
//                    sendSiapXml(res, searchReq, params, wp, dgpart);
                }
            }

        } catch (Exception e) {
            LOG.error(e);
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            ErrorTag errorTag = new ErrorTag();
            errorTag.setMessage(msg);
            FcXmlToJava.toXml(errorTag, res.getWriter());
        }
    }

    private Map<String, String> convertToStringMap(Map map) {
        HashMap<String, String> params = new HashMap<String, String>();

        if (map == null) return params;
        for(Object key : map.keySet()) {
            if (key != null) {
                Object values = map.get(key);
                String val = values == null ? "" : String.valueOf(values);
                if (values != null && values.getClass().isArray()) {
                    val = StringUtils.toString((Object[])values, ",");
                }
                params.put(String.valueOf(key), val);
            }
        }

        // apply defaults
        if (!params.containsKey(Param.mode.name())) {
            params.put(Param.mode.name(), PROG);
        }
        if (!params.containsKey(Param.subsetsize.name())) {
            params.put(Param.subsetsize.name(), "5");
        }
        if (!params.containsKey(Param.survey.name())) {
            params.put(Param.survey.name(), "DSS,SDSS,2MASS,IRIS,WISE");
        }

        String v = params.get(Param.marker.name());
        v = v == null ? params.get(Param.markervis_orig.name()) : v;
        v = v == null ? params.get(Param.markervis_shrunk.name()) : v;
        if (!StringUtils.isEmpty(v)) {
            params.put(Param.marker.name(), v);
        }

        v = params.get(Param.grid.name());
        v = v == null ? params.get(Param.grid_orig.name()) : v;
        v = v == null ? params.get(Param.grid_shrunk.name()) : v;
        if (!StringUtils.isEmpty(v)) {
            params.put(Param.grid.name(), v);
        }


        return params;
    }


    private void sendFcXml(HttpServletResponse res, TableServerRequest searchReq, Map<String, String> params, WorldPt wp, DataGroupPart data) throws Exception {

        FinderChartTag fc = new FinderChartTag();

        InputTag input = new InputTag();
        fc.setInput(input);
        input.setLocstr(params.get(Param.locstr.name()));
        input.setSurveys(params.get(Param.survey.name()));
        input.setSubsetsize(params.get(Param.subsetsize.name()));
        input.setOrientation(params.get(Param.orientation.name()));
        input.setReproject(params.get(Param.reproject.name()));
        input.setGrid(params.get(Param.grid.name()));
        input.setMarker(params.get(Param.marker.name()));

        DataGroup dg = data.getData();
        ResultTag rt = new ResultTag();
        fc.setResult(rt);
        rt.setDatatag("");  // not implemented
        rt.setEquCoord(VisUtil.convert(wp, CoordinateSys.EQ_J2000).toString());
        rt.setGalCoord(VisUtil.convert(wp, CoordinateSys.GALACTIC).toString());
        rt.setEclCoord(VisUtil.convert(wp, CoordinateSys.ECL_J2000).toString());
        rt.setTotalimages(String.valueOf(dg.size()));
        rt.setHtmlfile(makeFinderChartUrl(searchReq));

        ArrayList<ImageTag> images = new ArrayList<ImageTag>(dg.size());
        rt.setImages(images);
        for (int i = 0; i < dg.size(); i++) {
            DataObject row = dg.get(i);
            String fitsUrl = String.valueOf(row.getDataElement("fitsurl"));
            String service = String.valueOf(row.getDataElement("service"));

            ImageTag image = new ImageTag();
            image.setSurveyname(String.valueOf(row.getDataElement("externalname")));
            image.setBand(String.valueOf(row.getDataElement("wavelength")));
            image.setFitsurl(fitsUrl);
            image.setJpgurl(String.valueOf(row.getDataElement("jpgurl")));
            image.setShrunkjpgurl(String.valueOf(row.getDataElement("shrunkjpgurl")));
            images.add(image);

            // go get the fits file.. then extract the obs date from it.
            if (!StringUtils.isEmpty(fitsUrl)) {
                String reqStr = fitsUrl.split("\\?", 2)[1];
                TableServerRequest req = TableServerRequest.parse(reqStr);
                Map tparams = new HashMap();
                for (edu.caltech.ipac.firefly.data.Param p : req.getParams()) {
                    tparams.put(p.getName(), p.getValue());
                }
                WorldPt twp = getWorldPt(tparams);
                req = makeRequest(tparams, twp);
                List<FileInfo> files = getDataFiles(req);
                if (files != null && files.size() > 0) {
                    String obsDate = PlotServUtils.getDateValueFromServiceFits(WebPlotRequest.ServiceType.valueOf(service), new File(files.get(0).getInternalFilename()));
                    image.setObsdate(obsDate);
                }
            }
        }
        FcXmlToJava.toXml(fc, res.getWriter());
    }

    private List<FileInfo> getDataFiles(TableServerRequest searchReq) {

        ArrayList<FileInfo> allFiles = new ArrayList<FileInfo>();
        try {
            DownloadRequest dlReq = new DownloadRequest(searchReq, "", "");
            List<FileGroup> data = new FinderChartFileGroupsProcessor().getData(dlReq);
            for(FileGroup fg : data) {
                for(FileInfo fi : fg) {
                    allFiles.add(fi);
                }
        }
        } catch (DataAccessException e) {
            LOG.error(e);
        }

        return allFiles;
    }

    private String makeFinderChartUrl(TableServerRequest req) {
        TableServerRequest sreq = new TableServerRequest("Hydra_finderchart_finder_chart", req);
        sreq.setParam("DoSearch", "true");
        sreq.removeParam("mode");
        sreq.setPageSize(0);
        if (!sreq.containsParam("sources")) {
            sreq.setParam("sources", "DSS,SDSS,twomass,WISE");
        }

        return ServerContext.getRequestOwner().getBaseUrl() + "?" + QueryUtil.encodeUrl(sreq.toString());
    }

    private TableServerRequest makeRequest(Map<String, String> paramMap, WorldPt wp) throws Exception {

        TableServerRequest searchReq = new TableServerRequest(QueryFinderChart.PROC_ID);
        searchReq.setPageSize(Integer.MAX_VALUE);


        for(String key : paramMap.keySet()) {
            String val = paramMap.get(key);
            if (!key.toString().trim().matches(API_ONLY_PARAMS)) {
                searchReq.setParam(key, val);
            }
        }
        searchReq.setParam(Param.mode.name(), paramMap.get(Param.mode.name()));
        searchReq.setParam(ReqConst.USER_TARGET_WORLD_PT, wp);

        // size is in arc minute
        float size = StringUtils.getFloat(paramMap.get(Param.subsetsize.name()));

        if (size < 1 || size > 60) {
            throw new IllegalArgumentException(Param.subsetsize.name() + " is not between .1 and 60 arcmin");
        }
        // convert to degree
        size = size/60F;
        searchReq.setParam("subsize", size + "");

        // survey
        String v = paramMap.get(Param.survey.name());
        if (!StringUtils.isEmpty(v)) {
            searchReq.setParam("sources", v);
        }

        //orientation
        v = paramMap.get(Param.orientation.name());
        if (!StringUtils.isEmpty(v)) {
            searchReq.setParam(Param.orientation.name(), v);
        }

        //reproject
        v = paramMap.get(Param.reproject.name());
        if (!StringUtils.isEmpty(v)) {
            searchReq.setParam(Param.reproject.name(), v);
        }

        //grid
        v = paramMap.get(Param.grid.name());
        if (Boolean.parseBoolean(v)) {
            paramMap.put(Param.grid.name(), v);
            searchReq.setParam(Param.grid.name(), v);
        }

        //marker
        v = paramMap.get(Param.marker.name());
        if (Boolean.parseBoolean(v)) {
            paramMap.put(Param.marker.name(), v);
            searchReq.setParam(Param.marker.name(), v);
        }

        return searchReq;
    }

//====================================================================
//  resolving WorldPt from various types of interfaces
//====================================================================

    private WorldPt getWorldPt(Map<String, String> params) {
        double ra = Double.NaN, dec = Double.NaN;
        CoordinateSys coordsys = CoordinateSys.EQ_J2000;
        String input = "";

        // check for RA, DEC
        if (params.containsKey(Param.RA.name()) && params.containsKey(Param.DEC.name())) {
            input = "RA=" + params.get(Param.RA.name()) + " DEC=" + params.get(Param.DEC.name());
            ra = getDouble(params.get(Param.RA.name()));
            dec = getDouble(params.get(Param.DEC.name()));
        } else if (params.containsKey(Param.POS.name())) {
            String pos = params.get(Param.POS.name());
            input = "POS=" + params.get(Param.POS.name());
            String[] parts = pos.split(",");
            ra = getDouble(parts[0].trim());
            dec = getDouble(parts[1].trim());
        } else if (params.containsKey(Param.locstr.name())) {
            String locstr = params.get(Param.locstr.name());
            input = locstr;
            try {
                locstr = URLDecoder.decode(locstr, "UTF-8");
            } catch (UnsupportedEncodingException e) {}
            if (parser.parse(locstr)) {
                WorldPt wpt = parser.getPosition();
                if (wpt == null) {
                    throw new IllegalArgumentException("Coordinate [" + locstr + "] lookup error: Invalid object name.");
                }
                ra = wpt.getLon();
                dec = wpt.getLat();
                coordsys = wpt.getCoordSys();
            }
        }

        if (Double.isNaN(ra) || Double.isNaN(dec)) {
            throw new IllegalArgumentException("[" + input + "]: Invalid position.");
        } else {
            return new WorldPt(ra, dec, coordsys);
        }
    }

    private double getDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

//====================================================================
//  method taken from deprecated BaseProductDownload servlet
//====================================================================

    private void sendSingleProduct(FileInfo fi, HttpServletResponse res) throws IOException {
        String extName = fi.getExternalName();
        String mimeType;
        String encoding;

        // check the type of the file
        encoding = null;
        boolean inline = false;
        if (extName.endsWith(".H")) {
            mimeType = "image/fits";
            encoding = "x-H";
        } else if (extName.endsWith(".fits")) {
            mimeType = "image/fits";
        } else if (extName.endsWith(".gif")) {
            mimeType = "image/gif";
            inline = true;
        } else if (extName.endsWith(".jpg")) {
            mimeType = "image/jpeg";
            inline = true;
        } else if (extName.endsWith(".png")) {
            mimeType = "image/png";
            inline = true;
        } else if (extName.endsWith(".tbl") || extName.endsWith(".txt") || extName.endsWith(".log")) {
            mimeType = "text/plain";
            inline = true;
        } else {
            mimeType = "application/octet-stream";
        }

        File f = new File(fi.getInternalFilename());

        long fSize = f.length();
        if (fSize == 0) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NO DATA: unable to get "+extName);
            return;
        }
        // now we know that at least one file is available
        // commit to HttpResponse

        BufferedOutputStream buffOS = new BufferedOutputStream(res.getOutputStream());
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DataInputStream dis  = new DataInputStream(bis);

        res.setContentType(mimeType);
        if (encoding != null) {
            res.setHeader("Content-encoding", encoding);
        }
        String fSizeS = Long.toString(fSize);
        int fSizeInt = Integer.parseInt(fSizeS);
        String baseName = (new File(extName)).getName();
        res.setContentLength(fSizeInt);
        res.setHeader("Content-disposition",
                (inline ? "inline" : "attachment")+"; filename=" +
                        baseName);



        // copy file from the input steam to servlet output stream
        getFileFromStream(dis, buffOS, fSize);
    }

    private void sendZip(String fname, List<FileInfo> fiSet, HttpServletResponse resp) throws IOException{
        ZipOutputStream zout = new ZipOutputStream(resp.getOutputStream());
        zout.setComment("Source:");
        zout.setMethod(ZipOutputStream.DEFLATED);

        // set HTTP header fields
        resp.setContentType("application/zip");
        resp.setHeader("Content-disposition", "attachment"+"; filename="+fname+".zip");

        List<FileInfo> failed = null;
        for (FileInfo fi : fiSet) {
            File f = new File(fi.getInternalFilename());
            if (f.exists() && f.canRead()) {
                try {
                    ZipEntry zipEntry = new ZipEntry(fi.getExternalName());
                    zout.putNextEntry(zipEntry);
                    FileInputStream fis = new FileInputStream(fi.getInternalFilename());
                    BufferedInputStream  bis = new BufferedInputStream(fis);
                    DataInputStream dis  = new DataInputStream (bis);
                    getFileFromStream(dis, zout, f.length());
                    zout.closeEntry();
                } catch (Exception e) {
                    log("Failed to package: "+fi.getInternalFilename(), e);
                    failed.add(fi);
                }
            } else {
                log("Can not access file "+f.getAbsolutePath());
                if (failed == null) failed = new ArrayList<FileInfo>();
                failed.add(fi);
            }
        }
        // report failures
        if (failed != null) {
            ZipEntry zipEntry = new ZipEntry("README.txt");
            zout.putNextEntry(zipEntry);
            PrintWriter pw = new PrintWriter(zout);
            pw.println("\nErrors were encountered when packaging "+failed.size()+" files: \n");
            for (FileInfo fi : failed) {
                pw.println(fi.getExternalName());
            }
            pw.flush();
            zout.closeEntry();
        }
        zout.close();
    }

    private void getFileFromStream(InputStream is, OutputStream os, long filesize)
            throws IOException {
        int inBufSize = 1024;
        byte[] data = new byte[inBufSize];
        long bytesToRead = filesize;
        int retVal;

        try {
            while (bytesToRead > 0) {
                /**
                 *  Casting of long to int here is OK. The if statement
                 *  guarantees that by the time we fall into the case where
                 *  we're doing the casting bytesToRead will be "castable"
                 */
                if (inBufSize < bytesToRead) retVal = is.read(data,0,inBufSize);
                else retVal = is.read(data,0,(int)bytesToRead);
                if (retVal < 1)
                    throw new IOException ("Unexpected EOF from network peer.");
                bytesToRead -= retVal;
                os.write(data,0,retVal);
                os.flush();
            }
        } catch (IOException e) {
            log("[ProductDownload] IOException ["+e.getMessage()+"]");
            log("[ProductDownload] Flush remaining data from peer");
            try {
                while (bytesToRead > 0) {
                    if (inBufSize < bytesToRead) retVal = is.read(data,0,inBufSize);
                    else retVal = is.read(data,0,(int)bytesToRead);
                    if (retVal < 1) break;
                    bytesToRead -= retVal;
                }
            } catch (Exception ee) {
                log("ERROR: "+ee.getMessage()+" while trying to flush remaining data from FTZ");
            }
            throw e;
        }
    }



    private void sendError(HttpServletResponse res, String error) throws IOException {
        LOG.debug("sendError", error);
        PrintWriter pw = new PrintWriter(res.getOutputStream());
        pw.println("\\ERROR = "+error);
        pw.flush();
        //res.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
    }

    private void sendOverflow(HttpServletResponse res, String error) throws IOException {
        LOG.debug("sendOverflow", error);
        PrintWriter pw = new PrintWriter(res.getOutputStream());
        pw.println("\\ERROR = [OVERFLOW] "+error);
        pw.flush();
        //res.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
    }

    private static String [] getOriginalColumns() {
        return new String[] {"ra","dec","externalname","wavelength","naxis1","naxis2","accessUrl", "accessWithAnc1Url",
                "fitsurl", "jpgurl", "shrunkjpgurl"};
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
