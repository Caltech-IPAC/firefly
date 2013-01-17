package edu.caltech.ipac.solrclient;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

/**
 * Date: Jul 14, 2009
 *
 * @author loi
 * @version $Id: SolrQueryExec.java,v 1.2 2009/09/01 00:36:32 loi Exp $
 */
public class SolrQueryExec {

    public static enum OutputFormat {IPAC_TABLE};
    
    private static CommonsHttpSolrServer solrServer;
    private String solrServerUrl;
    private OutputFormat format = OutputFormat.IPAC_TABLE;


    public SolrQueryExec(String solrServerUrl) {
        this.solrServerUrl = solrServerUrl;
    }

    public OutputFormat getFormat() {
        return format;
    }

    public void setFormat(OutputFormat format) {
        this.format = format;
    }

    public void query(String queryString, OutputStream outf, String... fields)
                            throws SolrServerException, IOException {
        QueryParams params = new QueryParams(queryString);
        if (fields != null && fields.length > 0) {
            params.setQueryFields(Arrays.asList(fields));
        }
        query(params, outf);
    }

    public void query(QueryParams params) throws IOException, SolrServerException {
        query(params, System.out);
    }

    public void query(QueryParams params, File outf) throws IOException, SolrServerException {
        query(params, new FileOutputStream(outf));
    }

    public void query(QueryParams params, OutputStream outf)
                            throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery();

        params.setupQuery(query);
        if (params.getQueryFields() == null) {
            query.addField("*");
        } else if (params.isHighlight() && !params.getQueryFields().contains("id")) {
            query.addField("id");
        }

        QueryResponse rsp = getSolrServer().query( query );
        Map<String, Map<String, List<String>>> hlights = rsp.getHighlighting();
        SolrDocumentList docs = rsp.getResults();

        DataGroup results = null;
        List<DataType> cols = null;
        List<Integer> colWidths = new ArrayList<Integer>();

        for (SolrDocument sdoc : docs) {
            if (cols == null) {
                cols = setupHeaderColumns(params.getQueryFields(), sdoc.getFieldNames());
                results = new DataGroup("query result", cols);
                for(DataType dt : cols) {
                    colWidths.add(Math.max(dt.getKeyName().length(), 6));
                }
            }
            DataObject row = new DataObject(results);
            for (int i = 0; i < cols.size(); i++) {
                DataType c = cols.get(i);

                String val = null;
                // if highlighted... used highlighted value
                if (params.isHighlight()) {
                    String key = sdoc.getFieldValue("id").toString();
                    if (hlights.containsKey(key)) {
                        Map<String, List<String>> hfields = hlights.get(key);
                        if (hfields.containsKey(c.getKeyName())) {
                            val = hfields.get(c.getKeyName()).get(0);
                        }
                    }
                }

                if (val == null) {
                    val = sdoc.getFieldValue(c.getKeyName()) == null ? "" :
                                 sdoc.getFieldValue(c.getKeyName()).toString();
                }

                row.setDataElement(c, val);
                if (colWidths.get(i) < val.length()) {
                    colWidths.set(i, val.length());
                }
            }
            results.add(row);
        }

        if (cols != null) {
            // adjust column's width
            for(int i = 0; i < cols.size(); i++) {
                cols.get(i).getFormatInfo().setWidth(colWidths.get(i));
            }
            if (format == OutputFormat.IPAC_TABLE) {
                outputAsIpacTable(outf, results);
            }
        }
    }

    private void outputAsIpacTable(OutputStream outf, DataGroup results) throws IOException {
        IpacTableWriter.save(new BufferedOutputStream(outf, 128*1024), results);
    }

    private List<DataType> setupHeaderColumns(List<String> queryFields,
                                              Collection<String> docFields) {
        boolean includeExtraFields = false;
        List<String> cnames = new ArrayList<String>();
        if (queryFields == null) {
            includeExtraFields = true;
        } else {
            for (String c : queryFields) {
                if (c != null && c.length() > 0) {
                    if (c.equals("*")) {
                        includeExtraFields = true;
                        break;
                    } else {
                        cnames.add(c);
                    }
                }
            }
        }

        if (includeExtraFields) {
            for (String c : docFields) {
                if (!cnames.contains(c)) {
                    cnames.add(c);
                }
            }
        }

        List<DataType> cols = new ArrayList<DataType>();
        for(String c : cnames) {
            cols.add(new DataType(c, c, String.class, DataType.Importance.HIGH, null, true));
        }
        return cols;
    }


    protected SolrServer getSolrServer() {
        if (solrServer == null) {
            try {
                solrServer = new CommonsHttpSolrServer(solrServerUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                // this should never happen;
            }
        }
        return solrServer;
    }

    static Properties parseArgs(String[] args) {
        Properties params = new Properties();
        ListIterator<String> itr = Arrays.asList(args).listIterator();
        while (itr.hasNext()) {
            String s = itr.next();
            if (s.startsWith("-")) {
                String v = itr.next();
                if (v.startsWith("-")) {
                    params.put(s.substring(1), "true");
                    itr.previous();
                } else {
                    params.put(s.substring(1), v);
                }
            }
        }
        return params;
    }

    private static void showUsage() {
        System.err.println("Usage: SolrQueryExec params...");
        System.err.println();
        System.err.println("  Required Parameters:");
        System.err.println("       -solr.server.url  value : a URL pointing to the Solr server");
        System.err.println("       -query value     : a query string");
        System.err.println("  Optional Parameters:");
        System.err.println("       -pfile value : a file containing parameters. parameters from the command line will override these");
        System.err.println("       -hlOff       : turn highlighting off.  default to on");
        System.err.println("       -hlFragSize value : max number of characters to when highlighted. default to 0(all)");
        System.err.println("       -hlFields   list  : comma separated list of fields. default to qFields");
        System.err.println("       -qBoostInfo list  : comma separated list of field's boost. ie.. 'text^1.0,  id^5.0'");
        System.err.println("       -qFields    list  : comma separated list of fields. default to all");
        System.err.println("       -doctype    value : filter by docytype. default to not filter");
        System.err.println("       -outf       value : output file. default to system.out");
    }

    static boolean isEmpty(String... vals) {
        for (String s : vals) {
            if (s == null || s.trim().length() == 0) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            showUsage();
            System.exit(1);
        }

        Properties params = parseArgs(args);

        Properties defs = new Properties();

        if (params.containsKey("pfile")) {
            try {
                defs.load(new FileInputStream(new File(params.getProperty("pfile"))));
            } catch (IOException e) {
                e.printStackTrace();
                showUsage();
            }
        }

        String url = params.getProperty("solr.server.url", defs.getProperty("solr.url"));
//        boolean showScore = Boolean.valueOf(params.getProperty("showScore", defs.getProperty("showScore")));
        boolean hlOff = Boolean.valueOf(params.getProperty("hlOff", defs.getProperty("hlOff")));
        String hlFields = params.getProperty("hlFields", defs.getProperty("hlFields"));
        String qBoostInfo = params.getProperty("qBoostInfo", defs.getProperty("qBoostInfo"));
        String qFields = params.getProperty("qFields", defs.getProperty("qFields"));
        String query = params.getProperty("query", defs.getProperty("query"));
        String hlFragSize = params.getProperty("hlFragSize", defs.getProperty("hlFragSize"));
        String doctype = params.getProperty("doctype", defs.getProperty("doctype"));
        String outf = params.getProperty("outf", defs.getProperty("outf"));

        try {
            List<String> boosts = isEmpty(qBoostInfo) ? null : Arrays.asList(qBoostInfo.split(","));
            List<String> hlfields = isEmpty(hlFields) ? null : Arrays.asList(hlFields.split(","));
            List<String> qfields = isEmpty(qFields) ? null : Arrays.asList(qFields.split(","));
            int fragsize = isEmpty(hlFragSize) ? -1 : Integer.parseInt(hlFragSize);


            if (isEmpty(url, query)) {
                System.err.println("ERROR: required params missing!!!");
                System.err.println("solr.url=" + url);
                System.err.println("query=" + query);
                showUsage();
                System.exit(1);
            }

            SolrQueryExec solr = new SolrQueryExec(url);

            QueryParams sparams = new QueryParams(query);
            sparams.setHighlight(!hlOff);
            if (hlfields != null) {
                sparams.setHighlightFields(hlfields);
            }
            if (boosts != null) {
                sparams.setFieldBoostInfo(boosts);
            }
            if (qfields != null) {
                sparams.setQueryFields(qfields);
            }
            if (fragsize != -1) {
                sparams.setFragmentSize(fragsize);
            }
            if (!isEmpty(doctype)) {
                sparams.setDoctype(doctype);
            }
            if (isEmpty(outf)) {
                solr.query(sparams);
            } else {
                solr.query(sparams, new File(outf));
            }
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
