package edu.caltech.ipac.hydra.server.servlets;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorFactory;
import edu.caltech.ipac.firefly.server.servlets.BaseHttpServlet;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.util.KeyValue;
import edu.caltech.ipac.hydra.server.download.FinderChartFileGroupsProcessor;
import edu.caltech.ipac.hydra.server.query.QueryFinderChart;
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
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;

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

    public static final String PROG = "prog";
    public static final String SIAP = "siap";
    public static final String GET_IMAGE = "getImage";
    private List<String> modes = Arrays.asList(PROG, SIAP, GET_IMAGE);

    private enum Param {mode, locstr, subsetsize("subsize"), thumbnail_size, survey("sources"), orientation,
                        reproject, grid_orig, grid_shrunk, markervis_orig, markervis_shrunk;
                        private final String iname;
                        Param() { this.iname = name();}
                        Param(String iname) {this.iname = iname;}
                    }

    private static final int    MAX_RECORDS = 5000;
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        LOG.debug("Query string", req.getQueryString());
        Map params = req.getParameterMap();
        try {
            String mode = getValue(params, Param.mode.name());
            mode = StringUtils.isEmpty(mode) ? SIAP : mode;

            WorldPt wp = getWorldPt(params);
            TableServerRequest searchReq = makeRequest(params, wp);

            if (mode.equals(GET_IMAGE)) {
                DownloadRequest dlReq = new DownloadRequest(searchReq, "", "");
                List<FileGroup> data = new FinderChartFileGroupsProcessor().getData(dlReq);

                List<FileInfo> files = getAllFiles(data);
                if (files.size()<1) {
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "NO DATA: no file returned. " + dlReq);
                } else if (files.size()==1) {
                    sendSingleProduct(data.get(0).getFileInfo(0), res);
                } else {
                    String fname = ("FinderChartFiles_"+req.getParameter("RA")+"+"+req.getParameter("DEC")
                            +"_"+req.getParameter("SIZE")).replaceAll("\\+\\-","\\-");
                    sendZip(fname, data.get(0), res);
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
        }
    }

    private void sendFcXml(HttpServletResponse res, TableServerRequest searchReq, Map params, WorldPt wp, DataGroupPart data) {

        FinderChartTag fc = new FinderChartTag();

        InputTag input = new InputTag();
        fc.setInput(input);
        input.setLocstr(getValue(params, Param.locstr.name()));
        input.setSurveys(getValue(params, Param.survey.name()));
        input.setSubsetsize(getValue(params, Param.subsetsize.name()));
        input.setOrientation(getValue(params, Param.orientation.name()));
        input.setReproject(getValue(params, Param.reproject.name()));
        input.setGrid(getValue(params, Param.grid_orig.name()));
        input.setMarker(getValue(params, Param.markervis_orig.name()));
        input.setShrunkgrid(getValue(params, Param.grid_shrunk.name()));
        input.setShrunkmarker(getValue(params, Param.grid_shrunk.name()));

        DataGroup dg = data.getData();
        ResultTag rt = new ResultTag();
        fc.setResult(rt);
        rt.setDatatag("");  // not implemented
        //TODO:need to do conversion
        rt.setEquCoord("");
        rt.setGalCoord("");
        rt.setEclCoord("");
        rt.setTotalimages(String.valueOf(dg.size()));
        rt.setHtmlfile(makeFinderChartUrl(searchReq));

        ArrayList<ImageTag> images = new ArrayList<ImageTag>(dg.size());
        rt.setImages(images);
        for (int i = 0; i < dg.size(); i++) {
            DataObject row = dg.get(i);
            ImageTag image = new ImageTag();
            image.setSurveyname(String.valueOf(row.getDataElement("externalname")));
            image.setBand(String.valueOf(row.getDataElement("wavelength")));
            image.setObsdate(String.valueOf(row.getDataElement("obsdate"))); // TODO :  not implemented in FC2
            image.setFitsurl(String.valueOf(row.getDataElement("fitsurl")));
            image.setJpgurl(String.valueOf(row.getDataElement("jpgurl")));
            image.setShrunkjpgurl(String.valueOf(row.getDataElement("shrunkjpgurl")));
            images.add(image);
        }
        try {
            FcXmlToJava.toXml(fc, res.getWriter());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<FileInfo> getAllFiles(List<FileGroup> files) {
        ArrayList<FileInfo> allFiles = new ArrayList<FileInfo>();
        for(FileGroup fg : files) {
            for(FileInfo fi : fg) {
                allFiles.add(fi);
            }
        }
        return allFiles;
    }

    private String makeFinderChartUrl(TableServerRequest req) {
        TableServerRequest sreq = new TableServerRequest("Hydra_finderchart_finder_chart", req);
        req.setParam("doSearch", "true");
        return ServerContext.getRequestOwner().getBaseUrl() + "?" + sreq.toString();
    }

    private String getValue(Map paramMaps, String key) {
        if (key == null) return null;

        KeyValue<Param, String> param = getParam(key, paramMaps.get(key));
        return param == null ? null : param.getValue();
    }


    private KeyValue<Param, String> getParam(Object k, Object values) {
        if (k == null) return null;

        Param param = null;
        String val = values == null ? "" : String.valueOf(values);
        if (values != null && values.getClass().isArray()) {
            val = StringUtils.toString((Object[])values, ",");
        }
        try {
            param = Param.valueOf(k.toString().toLowerCase());
        } catch (Exception e) {}
        return new KeyValue<Param, String>(param, val);
    }

    private TableServerRequest makeRequest(Map paramMap, WorldPt wp) {

        TableServerRequest searchReq = new TableServerRequest(QueryFinderChart.PROC_ID);
        searchReq.setPageSize(Integer.MAX_VALUE);

        searchReq.setParam(ReqConst.USER_TARGET_WORLD_PT, wp);

        for(Object key : paramMap.keySet()) {
            KeyValue<Param, String> kv = getParam(key, paramMap.get(key));

            if (key.toString().trim().matches("locstr|POS|RA|DEC")) {
                // ignore these params
            } else {
                if (kv.getKey() == null) {
                    searchReq.setParam(String.valueOf(key), kv.getValue());
                } else {
                    searchReq.setParam(kv.getKey().iname, kv.getValue());
                }
            }
        }
        return searchReq;
    }

//====================================================================
//  resolving WorldPt from various types of interfaces
//====================================================================

    // pattern to match (  ra [+-]dec coord_sys  ) separated by space(s)
    private static final String POS_PATTERN = "[-+]?[0-9]*\\.?[0-9]*\\s+[-+]?[0-9]*\\.?[0-9]*.*";
    private WorldPt getWorldPt(Map params) {
        double ra = Double.NaN, dec = Double.NaN;
        String coordsys = "J2000";

        // check for RA, DEC
        if (params.containsKey("RA") && params.containsKey("DEC")) {
            ra = getDouble(getValue(params, "RA"));
            dec = getDouble(getValue(params, "DEC"));
        } else if (params.containsKey("POS")) {
            String pos = getValue(params, "POS");
            String[] parts = pos.split(",");
            ra = getDouble(parts[0].trim());
            dec = getDouble(parts[1].trim());
        } else if (params.containsKey("locstr")) {
            String locstr = getValue(params, "locstr");
            if (locstr.matches(POS_PATTERN)) {
                try {
                    locstr = URLDecoder.decode(locstr, "UTF-8");
                    String[] parts = locstr.split("\\s+");
                    if (parts.length > 1) {
                        ra = getDouble(parts[0]);
                        dec = getDouble(parts[1]);
                        if (parts.length > 2) {
                            coordsys = parts[2];
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    ra = Double.NaN;
                }
            } else {
                try {
                    ResolvedWorldPt rwp = TargetNetwork.getNedThenSimbad(locstr);
                    ra = rwp.getLon();
                    dec = rwp.getLat();
                    coordsys = rwp.getCoordSys().toString();
                } catch (FailedRequestException e) {
                    ra = Double.NaN;
                }
            }
        }

        if (ra == Double.NaN || dec == Double.NaN) {
            return null;
        } else {
            return new WorldPt(ra, dec, CoordinateSys.parse(coordsys));
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

    private void sendZip(String fname, FileGroup fiSet, HttpServletResponse resp) throws IOException{
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
