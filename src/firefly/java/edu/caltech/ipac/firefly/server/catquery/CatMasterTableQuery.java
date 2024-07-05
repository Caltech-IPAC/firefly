/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.network.IpacTableHandler;
import edu.caltech.ipac.firefly.server.query.EmbeddedDbProcessor;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Date: Jun 5, 2009
 *
 * @author Trey
 * @version $Id: CatMasterTableQuery.java,v 1.20 2012/10/30 00:41:25 loi Exp $
 */
@SearchProcessorImpl(id = "irsaCatalogMasterTable")
public class CatMasterTableQuery extends EmbeddedDbProcessor {

    private static final String MASTER_LOC = QueryUtil.makeUrlBase(BaseGator.DEF_HOST) + "/cgi-bin/Gator/nph-scan?mode=ascii";

    static String getIrsaBaseUrl() {
        String baseUrl = AppProperties.getProperty("irsa.base.url", "https://irsa.ipac.caltech.edu");
        return baseUrl.contains("irsasearchops") ? "https://irsa.ipac.caltech.edu" : baseUrl;
    }

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {

        IpacTableHandler handler = new IpacTableHandler();
        HttpServices.Status status = HttpServices.getData(new HttpServiceInput(MASTER_LOC), handler);
        if (status.isError()) {
            throw new DataAccessException("Unable to parse Master Table", status.getException());
        }
        DataGroup dg = handler.results;

        appendHostToUrls(dg, "moreinfo", "description");
        makeIntoUrl(dg, "infourl", "info");
        makeIntoUrl(dg, "ddlink", "Column Def");
        return dg;
    }

    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {

        String cols = Arrays.stream("projectshort,subtitle,description,server,catname,cols,nrows,coneradius,infourl,ddlink,pos"
                            .split(",")).map(s -> "\"" + s + "\"").collect(Collectors.joining(","));    // enclosed in quotes
        cols += ",'GatorQuery' as \"catSearchProcessor\",'GatorDD' as \"ddSearchProcessor\"";

        TableServerRequest tsr = (TableServerRequest) treq.cloneRequest();
        tsr.setInclColumns(cols.split(","));
        return dbAdapter.execRequestQuery(tsr, dbAdapter.getDataTable());
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
                href = getIrsaBaseUrl() + href;
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
                            url = getIrsaBaseUrl() + url;
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
}

