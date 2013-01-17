package edu.caltech.ipac.solrclient;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Use this class to index your document to solr server using JDBC and SolrJ.
 * The document's field is defined by the columns in the SELECT statement.
 *
 *
 *
 * Date: Jul 10, 2009
 *
 * @author loi
 * @version $Id: SolrDbIndexer.java,v 1.6 2009/10/22 00:40:47 loi Exp $
 */
public class SolrDbIndexer {

    public static final String MODE = "mode";
    public static final String SQL = "sql";
    public static final String DOC_TYPE = "doctype";
    public static final String SOLR_URL = "solr.server.url";

    public static final String DB_URL = "db.url";
    public static final String USER_NAME = "db.userId";
    public static final String USER_PASSWORD = "db.password";
    public static final String JDBC_DRIVER = "db.driver";

    private CommonsHttpSolrServer solrServer;
    private String solrServerUrl;
    private ArrayList<SolrInputDocument> docBuffer = new ArrayList<SolrInputDocument>();
    private String dbUrl;
    private String dbUser;
    private String dbPasswd;
    private String jdbcDriver;

    public SolrDbIndexer(String solrServerUrl, String dbUrl, String dbUser, String dbPasswd, String jdbcDriver) {

        if ( isEmpty(solrServerUrl, dbUrl, jdbcDriver, dbUser, dbPasswd) ) {
            throw new IllegalArgumentException("The given parameter(s) must not be blank:\n" +
                        "\nsolrServerUrl=" + solrServerUrl +
                        "\ndbUrl=" + dbUrl +
                        "\ndbUser=" + dbUser +
                        "\ndbPasswd=" + dbPasswd +
                        "\njdbcDriver=" + jdbcDriver
                    );
        }

        this.solrServerUrl = solrServerUrl;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPasswd = dbPasswd;
        this.jdbcDriver = jdbcDriver;
    }

    static boolean isEmpty(String... vals) {
        for (String s : vals) {
            if (s == null || s.trim().length() == 0) {
                return true;
            }
        }
        return false;
    }

    public int deleteByQuery(String queryString) throws IOException, SolrServerException {
        UpdateResponse rsp = getSolrServer().deleteByQuery(queryString);
        getSolrServer().commit();
        return rsp.getStatus();
    }

    public void loadData(String sql, String docType) {

        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = getDbConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setFetchSize(200);
            rs = stmt.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();

            int bufferSize = 400;
//            int bufferSize = 1000/meta.getColumnCount();
//            bufferSize = Math.min(bufferSize, 200);
//            bufferSize = Math.max(bufferSize, 20);
            int docCount = 0;

            while(rs.next()) {

                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("doctype", docType);
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String cn = meta.getColumnName(i);
                    String value = rs.getString(i);
                    value = value == null ? null : value.replaceAll("[^\\p{ASCII}]", "").replaceAll("<BR>", " ");
                    if (cn.equals("id")) {
                        doc.addField(meta.getColumnName(i), docType + "-" + value);
                    } else {
                        doc.addField(cn, value);
                    }
                }
                docBuffer.add(doc);
                if (docBuffer.size() >= bufferSize) {
                    getSolrServer().add(docBuffer);
                    UpdateResponse res = getSolrServer().commit();

                    docCount += docBuffer.size();
                    docBuffer.clear();
                    System.out.println(docCount + " " + docType + " documents added.");
                }
            }

            // flush buffer
            if (docBuffer.size() > 0) {
                getSolrServer().add(docBuffer);
                UpdateResponse res = getSolrServer().commit();
                System.out.println(res.getResponseHeader().toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SolrServerException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

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


    private Connection getDbConnection() throws SQLException, ClassNotFoundException {

        System.out.println("Getting a new database connection for " + dbUrl + " using DriverManager");
        Class.forName(jdbcDriver);
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPasswd);
        System.out.println("returned Connection:" + conn);
        return conn;
    }

//====================================================================
//
//====================================================================

    public static void main(String[] args) {

        if (args.length == 0) {
            showUsage();
            System.exit(1);
        }

        Properties params = SolrQueryExec.parseArgs(args);

        Properties defs = new Properties();

        if (params.containsKey("pfile")) {
            try {
                defs.load(new FileInputStream(new File(params.getProperty("pfile"))));
            } catch (IOException e) {
                System.err.println("Unable to load parameter file:" + params.getProperty("pfile"));
                showUsage();
            }
        }

        try {

            String solrServerUrl = params.getProperty(SOLR_URL, defs.getProperty(SOLR_URL));
            String dbUrl = params.getProperty(DB_URL, defs.getProperty(DB_URL));
            String jdbcDriver = params.getProperty(JDBC_DRIVER, defs.getProperty(JDBC_DRIVER));
            String dbUser = params.getProperty(USER_NAME, defs.getProperty(USER_NAME));
            String dbPasswd = params.getProperty(USER_PASSWORD, defs.getProperty(USER_PASSWORD));
            String sql = params.getProperty(SQL, defs.getProperty(SQL));
            String doctype = params.getProperty(DOC_TYPE, defs.getProperty(DOC_TYPE));
            String mode = params.getProperty(MODE, defs.getProperty(MODE));


            if ( isEmpty(solrServerUrl, dbUrl, jdbcDriver, dbUser, dbPasswd, sql, mode) ) {
                System.err.println("Missing required parameters!");
                System.err.println(SOLR_URL +"=" + solrServerUrl +
                                    "\n" + DB_URL + "=" + dbUrl +
                                    "\n" + JDBC_DRIVER + "=" + jdbcDriver +
                                    "\n" + USER_NAME + "=" + dbUser +
                                    "\n" + USER_PASSWORD + "=" + dbPasswd +
                                    "\n" + MODE + "=" + mode +
                                    "\n" + SQL + "=" + sql);
                showUsage();
                System.exit(1);

            }
            if (!(mode.equals("INDEX") || mode.equals("DELETE"))) {
                System.err.println("Mode has to be either 'INDEX' or 'DELETE'");
                showUsage();
                System.exit(1);
            }

            if (mode.equals("INDEX")) {
                if (isEmpty(doctype)) {
                    System.err.println("DocType is missing.");
                    showUsage();
                    System.exit(1);
                }
            }


            SolrDbIndexer solrIndexer = new SolrDbIndexer(solrServerUrl, dbUrl, dbUser, dbPasswd, jdbcDriver);
            if (mode.equals("DELETE")) {
                solrIndexer.deleteByQuery(sql);
            } else {
                solrIndexer.loadData(sql, doctype);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SolrServerException e) {
            e.printStackTrace();
        }
    }

    private static void showUsage() {
        System.err.println("Usage:  SolrDbIndexer params...");
        System.err.println("  Required Parameters:");
        System.err.println("       -mode  (INDEX | DELETE) : INDEX populates the solr server with documents");
        System.err.println("                                 DELETE removes documents from the solr server");
        System.err.println("       -solr.server.url  value : a URL pointing to the Solr server");
        System.err.println("       -sql       value : an SQL if INDEX mode or a SOLR query string if mode is DELETE");
        System.err.println("       -db.url    value : the database URL.");
        System.err.println("       -db.userId value : database user id to connect as");
        System.err.println("       -db.password value : password");
        System.out.println("       -db.driver value : JDBC driver class used to connect to database");
        System.err.println("  Optional Parameters:");
        System.err.println("       -pfile value : a file containing parameters. parameters from the command line will override these");
        System.err.println("       -doctype value : document type to store into the search engine.  Use this to store multiple doucment types");
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
