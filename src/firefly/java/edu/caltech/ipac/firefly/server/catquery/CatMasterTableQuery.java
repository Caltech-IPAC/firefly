/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.InvalidStatementException;
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.xstream.CatalogTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ProjectTag;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.dyn.DynConfigManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * Date: Jun 5, 2009
 *
 * @author Trey
 * @version $Id: CatMasterTableQuery.java,v 1.20 2012/10/30 00:41:25 loi Exp $
 */
@SearchProcessorImpl(id = "irsaCatalogMasterTable")
public class CatMasterTableQuery extends IpacTablePartProcessor {

    // debug - get file from jar (see below too)
    //private static final String TEMP_MASTER_LOC= "edu/caltech/ipac/firefly/resources/master-irsa-cat-original.tbl";

    private static final String MASTER_LOC = QueryUtil.makeUrlBase(BaseGator.DEF_HOST) + "/cgi-bin/Gator/nph-scan?mode=ascii";
    private static final File ORIGINAL_FILE = new File(ServerContext.getPermWorkDir(), "master-irsa-cat-original.tbl");
    private static final File MASTER_CAT_FILE = new File(ServerContext.getPermWorkDir(), "master-catalog.tbl");
    private static final String IRSA_HOST = AppProperties.getProperty("irsa.base.url", BaseGator.DEF_HOST);
    private static final String IRSA_BASE_URL = QueryUtil.makeUrlBase(IRSA_HOST);


    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {
        File retFile;

        StringKey key = new StringKey(CatMasterTableQuery.class.getName(), getUniqueID(request));
        Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);
        retFile = (File) cache.get(key);
        if (retFile == null) {
            retFile = getMasterCatalogFile(request);
            cache.put(key, retFile);
        }

        return retFile;
    }

    private static void placeOriginal(String catalogUrl, File originalFile) throws IOException {
        try {
            URL url = new URL(catalogUrl);
            URLDownload.getDataToFile(url, originalFile);

        } catch (FailedRequestException e) {
            throw new IOException(e.toString());
        }
    }

    private static File getMasterCatalogFile(TableServerRequest request) throws IOException, DataAccessException {
        File retval;
        try {
            String colNames = "projectshort, subtitle, description, server, catname, cols, nrows, coneradius, infourl, ddlink";
            File catOutFile = MASTER_CAT_FILE;

            // if hydra, check for additional catalogs
            DataGroup dgExtra = null;
            String projectId = request.getParam("projectId");
            if (!StringUtils.isEmpty(projectId)) {
                ProjectTag pTag = DynConfigManager.getInstance().getCachedProject(projectId);
                List<CatalogTag> cList = pTag.getCatalogs();
                for (CatalogTag cTag : cList) {
                    String fullCatalogUrl = cTag.getHost() + cTag.getCatalogUrl();
                    File catInFile = new File(ServerContext.getPermWorkDir(), cTag.getOriginalFilename());
                    placeOriginal(fullCatalogUrl, catInFile);

                    catOutFile = new File(ServerContext.getPermWorkDir(), cTag.getMasterCatFilename());

                    String selectStr =
                            "select col " +
                                    colNames + " " +
                                    "from " + catInFile.getPath() + " " +
                                    "with complete_header";
                    DataGroupQueryStatement statement = DataGroupQueryStatement.parseStatement(selectStr);
                    DataGroup dg = statement.execute();

                    if (dgExtra == null) {
                        dgExtra = dg;

                    } else {
                        // concat dg to dgExtra
                        Iterator<DataObject> iter = dg.iterator();
                        while (iter.hasNext()) {
                            dgExtra.add(iter.next());
                        }
                    }
                }
            }

            // get all other catalog data
            placeOriginal(MASTER_LOC, ORIGINAL_FILE);

            String selectStr =
                    "select col " +
                            colNames + " " +
                            "from " + ORIGINAL_FILE.getPath() + " " +
                            "with complete_header";
            DataGroupQueryStatement statement = DataGroupQueryStatement.parseStatement(selectStr);
            DataGroup dg = statement.execute();

            DataGroup outputDG;
            if (dgExtra == null) {
                outputDG= dg;
//                DataGroupWriter.write(catOutFile, dg, 0);

            } else {
                // concat dg to dgExtra
                Iterator<DataObject> iter = dg.iterator();
                while (iter.hasNext()) {
                    dgExtra.add(iter.next());
                }

                outputDG= dgExtra;
//                DataGroupWriter.write(catOutFile, dgExtra, 0);
            }
            DataGroupWriter.write(catOutFile, outputDG, 0);

            DataGroup data = DataGroupReader.read(catOutFile);
            // append hostname to relative path urls.
            appendHostToUrls(data, "moreinfo", "description");
            makeIntoUrl(data, "infourl", "info");
            makeIntoUrl(data, "ddlink", "Column Def");
            addSearchProcessorCols(data);
            data.shrinkToFitData(true);
            IpacTableWriter.save(catOutFile, data);

            retval = catOutFile;

        } catch (IpacTableException e) {
            throw new DataAccessException("Error parsing catalog file", e);

        } catch (InvalidStatementException e) {
            throw new DataAccessException("Error parsing catalog file", e);
        }

        return retval;
    }
    
    private static String getValue(DataObject row, String colName) {
        try {
            return (String) row.getDataElement(colName);
        } catch (Exception e) {
            // ignore errors
        }
        return null;
    }


    private static void addSearchProcessorCols(DataGroup dg) {

        DataType catDt= new DataType("catSearchProcessor", String.class);
        DataType ddDt= new DataType("ddSearchProcessor", String.class);
        dg.addDataDefinition(catDt);
        dg.addDataDefinition(ddDt);

        for (int r = 0; r < dg.size(); r++) {
            DataObject row = dg.get(r);
            row.setDataElement(catDt, "GatorQuery");
            row.setDataElement(ddDt, "GatorDD");
        }
    }




    private static void makeIntoUrl(DataGroup dg, String colname, String linkDesc) {
        
        DataType col = dg.getDataDefintion(colname);
        for (int r = 0; r < dg.size(); r++) {
            DataObject row = dg.get(r);
            String href = getValue(row, colname);

            if (StringUtils.isEmpty(href)) continue;

            href = href.trim().replaceAll("['|\"]", "");

            // if url is relative, make it absolute
            // create <a> with linkDesc
            if (!href.toLowerCase().startsWith("http")) {
                href = href.startsWith("/") ? href : "/" + href;
                href = IRSA_BASE_URL + href;
            }
            String url = "<a href='" + href + "' target='" + linkDesc + "'>" + linkDesc + "</a>";
            row.setDataElement(col, url);
        }
    }

    
    private static void appendHostToUrls(DataGroup dg, String linkDesc, String... cols) {

        if (cols == null || cols.length ==0) return;
        for(int c = 0; c < cols.length; c++) {
            DataType col = dg.getDataDefintion(cols[c]);
            if (col == null) continue;
            
            for (int r = 0; r < dg.size(); r++) {
                DataObject row = dg.get(r);
                String val = getValue(row, col.getKeyName());
                    String modVal= modifyToFullURL(val,linkDesc);
                    if (!modVal.equals(val)) {
                        row.setDataElement(col, modVal);
                    }
                }
//            }
        }
    }

    private static String modifyToFullURL(String inStr, String  targetStr) {
        String retval= inStr;
        if (inStr.contains("href")) {
            String s= inStr.replace(" ", "");
            int start= s.indexOf("<ahref=");
            String url = null;
            if (start>-1) {
                start+=7;
                char beginChar= s.charAt(start);
                if (beginChar=='"' || beginChar=='\'') {
                    start++;
                    int end= s.indexOf(beginChar,start);
                    if (end>-1) {
                        String replaceStr= s.substring(start,end);
                        if (!replaceStr.toLowerCase().startsWith("http")) {
                            url = replaceStr.startsWith("/") ? replaceStr : "/" + replaceStr;
                            url = IRSA_BASE_URL + url;
                            retval= inStr.replace(replaceStr,url);
                        }

                    }
                }
                int endAnchor= s.indexOf(">",start);
                if (targetStr!=null && endAnchor>0 && url!=null &&
                        !s.substring(beginChar,endAnchor).contains("target=")) {
                    String rStr = beginChar + url + beginChar;
                    retval= retval.replaceFirst(rStr, rStr+ " target="+"\""+targetStr+"\" ");
                }
            }
        }


        return retval;
    }

    public static DataGroup getBaseGatorData(String originalFilename) throws IOException, DataAccessException {
        return DataGroupReader.read(new File(ServerContext.getPermWorkDir(), originalFilename));
    }

}

