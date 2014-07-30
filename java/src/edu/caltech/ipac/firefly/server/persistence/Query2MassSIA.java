package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.draw.FixedObjectGroupUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.apache.xmlbeans.XmlOptions;
import org.usVo.xml.voTable.RESOURCEDocument;
import org.usVo.xml.voTable.VOTABLEDocument;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.DESC_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * Query 2MASS Image Inventory
 * http://irsa.ipac.caltech.edu/applications/2MASS/IM/docs/siahelp.html
 */

@SearchProcessorImpl(id = "2MassQuerySIA", params=
        {@ParamDoc(name="UserTargetWorldPt",                  desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="type", desc="at (Atlas, default) or ql (Quicklook)."),
         @ParamDoc(name="ds", desc="The 2MASS data set to search for images in."),
         @ParamDoc(name="hem", desc="The hemisphere of the 2MASS observatory where the FITS images to return were taken: n or s."),
         @ParamDoc(name="band", desc="Limits 2MASS images returned to those with the given 2MASS Band: A (all), J, H, K."),
         @ParamDoc(name="scan", desc="The nightly scan number (a positive integer) of the FITS images to return.")

        })
public class Query2MassSIA extends DynQueryProcessor {

    public static final String RADIUS_KEY = "radius";
    public static final String TYPE_KEY = "type";
    public static final String DS_KEY = "ds";
    public static final String HEM_KEY = "hem";
    public static final String BAND_KEY = "band";
    public static final String XDATE_KEY = "xdate";
    public static final String SCAN_KEY = "scan";

    private static final String TM_URL = AppProperties.getProperty("2mass.url.catquery",
                                                      "http://irsa.ipac.caltech.edu/cgi-bin/2MASS/IM/nph-im_sia?FORMAT=image/fits");
    private static final Logger.LoggerImpl _log = Logger.getLogger();


    private static final String ERR_START = "[struct ";
    private static final String ANY_ERR_STR = "error";
    private static final String HTML_ERR = "<html>";
    private static final int STAT_IDX = 0;
    private static final int MSG_IDX = 1;


    @Override
    public boolean doCache() {
        return true;
    }

    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.IMAGESET;
    }

    @Override
    public File loadDynDataFile(TableServerRequest req) throws IOException, DataAccessException {

        long start = System.currentTimeMillis();

        String fromCacheStr = "";


        StringKey key = new StringKey(Query2MassSIA.class.getName(), getUniqueID(req));
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_FILE);
        File retFile = (File) cache.get(key);
        if (retFile == null) {
            retFile = query2Mass(req);  // all the work is done here
            cache.put(key, retFile);
        } else {
            fromCacheStr = "   (from Cache)";
        }

        long elaspe = System.currentTimeMillis() - start;
        String sizeStr = FileUtil.getSizeAsString(retFile.length());
        String timeStr = UTCTimeUtil.getHMSFromMills(elaspe);

        _log.info("catalog: " + timeStr + fromCacheStr,
                "filename: " + retFile.getPath(),
                "size:     " + sizeStr);

        return retFile;
    }


    private File query2Mass(TableServerRequest req) throws IOException, DataAccessException {
        WorldPt wpt = req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        if (wpt == null)
            throw new DataAccessException("could not find the paramater: " + ReqConst.USER_TARGET_WORLD_PT);
        wpt = Plot.convert(wpt, CoordinateSys.EQ_J2000);

        String ds = req.getParam(DS_KEY);
        if (StringUtil.isEmpty(ds)) { ds = "asky"; }

        String type = req.getParam(TYPE_KEY);
        String hem = req.getParam(HEM_KEY);
        String band = req.getParam(BAND_KEY);
        Date xdate = req.getDateParam(XDATE_KEY);
        int scan = req.getIntParam(SCAN_KEY, -1);


        double radVal = req.getDoubleParam(RADIUS_KEY);
        double radDeg = MathUtil.convert(MathUtil.Units.parse(req.getParam(CatalogRequest.RAD_UNITS), MathUtil.Units.DEGREE), MathUtil.Units.DEGREE, radVal);
        String query = TM_URL + "&ds=" + ds
                + ((StringUtil.isEmpty(type) || type.equals("at")) ? "" : "&type=" + type)
                + "&SIZE=" + radDeg + "&POS=" + wpt.getLon() + "," + wpt.getLat()
                + ((StringUtil.isEmpty(hem) || hem.equals("a")) ? "" : "&hem=" + hem)
                + ((StringUtil.isEmpty(band) || band.equals("A")) ? "" : "&band=" + band)
                + (xdate == null ? "" : "&xdate="+(new SimpleDateFormat("yyMMdd")).format(xdate))
                + (scan == -1 ? "" : "&scan="+scan)
                ;


        URL url = new URL(query);
        File outFile;
        try {
            String data = URLDownload.getStringFromURL(url, null);
            evaluateData(data);

            // data comes back a a VOTable, this format is too big to us internally, we need to get ipac tables
            // convert it to a dataGroup here the save as a ipac table
            DataGroup dataGroup = voToDataGroup(data);
            if (dataGroup != null && dataGroup.getDataDefinitions().length > 0) {
                for (DataType col : dataGroup.getDataDefinitions()) {
                    dataGroup.addAttributes(new DataGroup.Attribute(makeAttribKey(DESC_TAG, col.getKeyName().toLowerCase()), col.getShortDesc().replace("\n", " ")));
                }
            }

            outFile = createFile(req);
            IpacTableWriter.save(outFile, dataGroup);


        } catch (FailedRequestException e) {
            IOException eio = new IOException("2mass Query Failed");
            eio.initCause(e);
            throw eio;
        } catch (MalformedURLException e) {
            IOException eio = new IOException("2mass Query Failed - bad url");
            eio.initCause(e);
            throw eio;
        } catch (IOException e) {
            IOException eio = new IOException("2mass Query Failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (EndUserException e) {
            DataAccessException eio = new DataAccessException("2mass Query Failed - network Error");
            eio.initCause(e);
            throw eio;
        } catch (Exception e) {
            DataAccessException eio = new DataAccessException("2mass Query Failed - " + e.toString());
            eio.initCause(e);
            throw eio;
        }
        return outFile;
    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        TableMeta.LonLatColumns llc = new TableMeta.LonLatColumns("center_ra", "center_dec", CoordinateSys.EQ_J2000);
        meta.setCenterCoordColumns(llc);

    }

    /*
    private static File makeFileName(TableServerRequest req) throws IOException {
        return File.createTempFile("2mass-", ".tbl", ServerContext.getPermWorkDir());
    }
    */

    public static DataGroup voToDataGroup(String doTable) throws Exception {
        XmlOptions xmlOptions = new XmlOptions();
        HashMap<String, String> substituteNamespaceList =
                new HashMap<String, String>();
        substituteNamespaceList.put("", "http://us-vo.org/xml/VOTable.xsd");
        substituteNamespaceList.put("http://www.ivoa.net/xml/VOTable/v1.3", "http://us-vo.org/xml/VOTable.xsd");
        xmlOptions.setLoadSubstituteNamespaces(substituteNamespaceList);
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setSavePrettyPrintIndent(4)        ;

        VOTABLEDocument voTableDoc = parseVoTable(doTable, xmlOptions);
        VOTABLEDocument.VOTABLE voTable = voTableDoc.getVOTABLE();
        FixedObjectGroupUtils.checkStatus(voTable); // check for errors from VOTABLE provider
        RESOURCEDocument.RESOURCE[] resources = voTable.getRESOURCEArray();
        if (resources.length > 0) {
            return FixedObjectGroupUtils.getDataGroup(resources[0], null, false, true);
        } else {
            throw new FailedRequestException("Invalid VO table");
        }

    }


    public static void evaluateData(String errStr) throws IOException, EndUserException {
        if (errStr.length() > 200) return;
        if (errStr.startsWith(ERR_START) && errStr.charAt(errStr.length() - 1) == ']') {
            String s = errStr.substring(ERR_START.length(), errStr.length() - 1);
            String sParts[] = s.split(", ");

            Entry stat = getEntry(sParts[STAT_IDX]);
            Entry msg = getEntry(sParts[MSG_IDX]);
            if (stat.getName().equals("stat")) {
                handleErr(
                        "IRSA search error: " + msg.getValue(),
                        "Receiving errors from Gator, stat=" + stat.getValue());
            }

        } else if (errStr.toLowerCase().contains(ANY_ERR_STR)) {
            handleErr(
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unreconized errors from Gator, Error: " + errStr);
        }
//        else if (errStr.toLowerCase().contains(HTML_ERR) &&
//                 outFile.length()>MAX_ERROR_LEN) {
//            handleErr(
//                      "IRSA search failed, Catalog is unavailable",
//                      "Receiving unreconized errors from Gator, html send by mistake");
//        }
        else if (errStr.toLowerCase().contains(HTML_ERR)) { // maybe send the html here
            handleErr(
                    "IRSA search failed, Catalog is unavailable",
                    "Receiving unreconized errors from Gator, html send by mistake");
        }
    }

    private static void handleErr(String userErr,
                                  String moreDetail) throws EndUserException {
        throw new EndUserException(userErr, moreDetail);
    }

    private static Entry getEntry(String s) {
        String sAry[] = s.split("=");
        String name = removeQuotes(sAry[0]);
        String val = removeQuotes(sAry[1]);
        return new Entry(name, val);
    }

    private static String removeQuotes(String s) {
        if (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static class Entry {
        private final String _name;
        private final String _value;

        public Entry(String name, String value) {
            _name = name;
            _value = value;
        }

        public String getName() {
            return _name;
        }

        public String getValue() {
            return _value;
        }
    }


    private static VOTABLEDocument parseVoTable(String data,
                                                XmlOptions xmlOptions)
            throws Exception {

        // The next lines are done with reflections to avoid
        // having to have weblogic.jar in the compile
        //VOTABLEDocument voTableDoc = VOTABLEDocument.Factory.parse(
        //                              data,xmlOptions);
        Class fClass = VOTABLEDocument.Factory.class;
        Method parseCall = fClass.getMethod("parse",
                String.class, XmlOptions.class);
        return (VOTABLEDocument) parseCall.invoke(fClass, data, xmlOptions);
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

