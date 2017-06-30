package edu.caltech.ipac.firefly.ws.client;


import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.ws.WorkspaceFactory;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.firefly.server.ws.WsResponse;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.ws.WsIrsaTest;
import edu.caltech.ipac.util.AppProperties;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.CheckinMethod;
import org.apache.jackrabbit.webdav.client.methods.CheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.client.methods.UncheckoutMethod;
import org.apache.jackrabbit.webdav.client.methods.VersionControlMethod;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;


/**
 * Created by ejoliet on 6/16/17.
 * Example of Webdav Client.
 */
public class WebDAVClient {


    private static Logger LOGGER = Logger.getLogger(WebDAVClient.class.getName());

    private HttpClient client = null;

    public WebDAVClient(String host, int port, String username, String password) {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("WebDAVConnector {host=" + host + ", port=" + port + ", username=" + username + ", password=******}");
        }

        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(host, port);

        HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxHostConnections = 20;
        params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
        connectionManager.setParams(params);

        client = new HttpClient(connectionManager);
        Credentials creds = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, creds);
        client.setHostConfiguration(hostConfig);
    }

    public WebDAVResponse listFolder(String uri) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("listFolder '" + uri + "'");
        }

        GetMethod httpMethod = new GetMethod(uri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse createFolder(String parentUri, String newFoldersName) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("createFolder '" + newFoldersName + "' into '" + parentUri + "'");
        }

        MkColMethod httpMethod = new MkColMethod(parentUri + newFoldersName);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse downloadFile(String fileUri, String outputFileFolder, String outputFileName) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("downloadFile '" + fileUri + "' and save it to '" + outputFileFolder + outputFileName + "'");
        }

        GetMethod httpMethod = new GetMethod(fileUri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, false);

        // Save output file
        if (httpMethod.getResponseContentLength() > 0) {
            InputStream inputStream = httpMethod.getResponseBodyAsStream();
            File responseFile = new File(outputFileFolder + outputFileName);
            OutputStream outputStream = new FileOutputStream(responseFile);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        }

        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse uploadFile(String destinationUri, String file, String contentType) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("uploadFile '" + file + "' with mimeType '" + contentType + "' to folder '" + destinationUri + "'");
        }

        PutMethod httpMethod = new PutMethod(destinationUri);

        FileInputStream fis = new FileInputStream(file);
        RequestEntity requestEntity = new InputStreamRequestEntity(fis, contentType);
        httpMethod.setRequestEntity(requestEntity);

        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    /**
     * To delete either a folder or a file
     *
     * @param uri
     * @throws Exception
     */
    public WebDAVResponse deleteItem(String uri) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("deleteItem '" + uri + "'");
        }

        DeleteMethod httpMethod = new DeleteMethod(uri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse checkout(String uri) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("checkout '" + uri + "'");
        }

        CheckoutMethod httpMethod = new CheckoutMethod(uri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse checkin(String uri) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("checkin '" + uri + "'");
        }

        CheckinMethod httpMethod = new CheckinMethod(uri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse cancelCheckout(String uri) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("cancelCheckout '" + uri + "'");
        }

        UncheckoutMethod httpMethod = new UncheckoutMethod(uri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse report(String uri) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("report '" + uri + "'");
        }

        ReportInfo reportInfo = new ReportInfo(ReportType.VERSION_TREE);

        ReportMethod httpMethod = new ReportMethod(uri, reportInfo);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, false);

        MultiStatus multiStatus = httpMethod.getResponseBodyAsMultiStatus();
        MultiStatusResponse responses[] = multiStatus.getResponses();

        String responseAsString = "";
        for (int i = 0; i < responses.length; i++) {
            responseAsString += responses[i].getHref() + "\n";
        }

        objResponse.setResponse(responseAsString);
        httpMethod.releaseConnection();
        return objResponse;
    }

    public WebDAVResponse versionControl(String uri) throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("versionControl '" + uri + "'");
        }

        VersionControlMethod httpMethod = new VersionControlMethod(uri);
        client.executeMethod(httpMethod);

        WebDAVResponse objResponse = processResponse(httpMethod, true);
        httpMethod.releaseConnection();
        return objResponse;
    }

    private WebDAVResponse processResponse(HttpMethod httpMethod, boolean getResponseAsString) {

        String statusCode = "-1";
        if (httpMethod.getStatusCode() > 0) {
            statusCode = String.valueOf(httpMethod.getStatusCode());
        }
        String statusText = httpMethod.getStatusText();

        String responseString = "";
        if (getResponseAsString) {
            try {
                responseString = httpMethod.getResponseBodyAsString();
                if (responseString == null) {
                    responseString = "";
                }
            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("IOException while getting responseAsString: " + e.getMessage());
                }
                e.printStackTrace();
            }
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("status CODE: " + statusCode + ", status TEXT: " + statusText + "\n response string: " + responseString);
        }

        WebDAVResponse response = new WebDAVResponse(statusCode, statusText, responseString);
        return response;
    }

    public static void main(String[] args) throws Exception {
//        WebDAVClient client = new WebDAVClient("http://test.cyberduck.ch/dav/anon/sardine/", 80, "", "");

//        WebDAVResponse webDAVResponse = client.listFolder("http://test.cyberduck.ch/dav/anon/sardine/");
//        LOGGER.info(webDAVResponse.getResponse());


        WsCredentials cred = new WsCredentials("ejoliet@ipac.caltech.edu", "ASK ME");
        WebDAVClient client = new WebDAVClient("https://irsadev.ipac.caltech.edu", 80, cred.getWsId(), cred.getPassword());

        WebDAVResponse webDAVResponse = client.listFolder("https://irsadev.ipac.caltech.edu/ssospace/ejoliet@ipac.caltech.edu");
        //LOGGER.info(webDAVResponse.getResponse());

        AppProperties.setProperty("workspace.host.url", "https://irsadev.ipac.caltech.edu");
        WorkspaceManager workspaceManager = WorkspaceFactory.getWorkspaceHandler().withCredentials(cred);
        File f = pickFile(0);
        workspaceManager.putFile("", f, null);
        WsResponse r = workspaceManager.getList("", 1);

        List<WspaceMeta> wsmeta = r.getWspaceMeta();

        for (WspaceMeta meta : wsmeta) {
            LOGGER.info(meta.getNodesAsString());
        }

    }

    private static File pickFile(int idx) throws ClassNotFoundException {
        if (idx < 0) {
            throw new IllegalArgumentException("index is negative " + idx);
        }
        File testPath = new File(FileLoader.getDataPath(WsIrsaTest.class));
        File[] allFilesButFits = testPath.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.endsWith(".fits"); // Can't use fits in pubspace because > 1Mb irsa policy storage
            }
        });
        if (idx > allFilesButFits.length) {
            idx = 0;
        }
        return allFilesButFits[idx];
    }
}


class WebDAVResponse {

    private String statusCode = "";
    private String statusText = "";
    private String response = "";

    public WebDAVResponse() {

    }

    WebDAVResponse(String code, String text, String responseString) {
        statusCode = code;
        statusText = text;
        response = responseString;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String toString() {
        String result = "status code : " + statusCode + "\n" +
                "status text : " + statusText + "\n" +
                "   response : " + response;
        return result;
    }
}
