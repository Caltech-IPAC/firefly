package edu.caltech.ipac.firefly.server.util.multipart;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.server.util.Logger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: Jul 27, 2010
 *
 * @author loi
 * @version $Id: MultiPartPostBuilder.java,v 1.12 2012/06/21 18:23:53 loi Exp $
 */
public class MultiPartPostBuilder {

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    /** is to allow a client that is sending a request message with a request body
     *  to determine if the origin server is willing to accept the request
     * (based on the request headers) before the client sends the request body.
     * require server support HTTP/1.1 protocol.
     */
    private boolean useExpectContinueHeader = false;

    private String targetURL;
    private List<Part> parts = new ArrayList<Part>();
    private List<Param> params = new ArrayList<Param>();
    private List<Param> headers = new ArrayList<Param>();

    public MultiPartPostBuilder() {}

    public MultiPartPostBuilder(String targetURL) {
        this.targetURL = targetURL;
    }

    public void setTargetURL(String targetURL) {
        this.targetURL = targetURL;
    }

    public void addParam(String name, String value) {
        parts.add(new StringPart(name, value));
        params.add(new Param(name,value));
    }

    public void addHeader(String name, String value) {
        headers.add(new Param(name,value));
    }

    public void addFile(String name, File file) {
        try {
            parts.add(new FilePart(name, file));
        } catch (FileNotFoundException e) {
            LOG.error(e, "Unable to add to MultiPartPostBuilder.  File not found:" + file);
        }
    }


    /**
     * Post this multipart request.
     * @param responseBody  the response is written into this OutputStream.
     * @return a MultiPartRespnse which contains headers, status, and status code.
     */
    public MultiPartRespnse post(OutputStream responseBody) {
        
        if (targetURL == null || parts.size() == 0) {
            return null;
        }

        logStart();
        
        PostMethod filePost = new PostMethod(targetURL);

        filePost.getParams().setBooleanParameter(
                HttpMethodParams.USE_EXPECT_CONTINUE, useExpectContinueHeader);

        for(Param p : headers) {
            filePost.addRequestHeader(p.getName(), p.getValue());
        }

        try {

            filePost.setRequestEntity(
                    new MultipartRequestEntity(parts.toArray(new Part[parts.size()]),
                    filePost.getParams())
                    );

            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().
                    getParams().setConnectionTimeout(5000);
            client.getHttpConnectionManager().
                    getParams().setSoTimeout(0);   // this is the default.. but, setting it explicitly to be sure

            // setup authorization, if applicable
            String userId = URLDownload.getUserFromUrl(targetURL);
            if (userId != null) {
                client.getState().setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(userId, URLDownload.getPasswordFromUrl(targetURL)));
                filePost.setDoAuthentication( true );
            }

            int status = client.executeMethod(filePost);

            MultiPartRespnse resp = new MultiPartRespnse(filePost.getResponseHeaders(),
                                                status,
                                                filePost.getStatusText());
            if (responseBody != null) {
                readBody(responseBody, filePost.getResponseBodyAsStream());
            }
            return resp;

        } catch (Exception ex) {
            LOG.error(ex, "Error while posting multipart request to" + targetURL);
        } finally {
            filePost.releaseConnection();
        }
        return null;
    }

    static void readBody(OutputStream os, InputStream body) {
        BufferedInputStream bis = new BufferedInputStream(body);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        try {
            int b;
            while ((b = bis.read()) != -1) {
                bos.write(b);
            }

            bos.flush();

        } catch (IOException e) {
            LOG.error(e, "Error while reading response body");
        }
    }

    private void logStart() {
        List<String> logList= new ArrayList<String>(20);
        logList.add("Post Params");
        for(Param param : params) {
          logList.add("   " + param.getName()+ "=" + param.getValue());
        }
        String sary[]= logList.toArray(new String[logList.size()]);
        Logger.debug(sary);

    }

    public static class MultiPartRespnse {
        private Header[] headers;
        private int statusCode;
        private String statusMsg;

        public MultiPartRespnse(Header[] headers, int statusCode, String statusMsg) {
            this.headers = headers;
            this.statusCode = statusCode;
            this.statusMsg = statusMsg;
        }

        public Header[] getHeaders() {
            return headers;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMsg() {
            return statusMsg;
        }
    }


    public static void main(String[] args) {
        HttpClient client = new HttpClient();
        System.out.println("conn manager= " + client.getHttpConnectionManager().getClass().getName());
        System.out.println("def max per host= " + client.getHttpConnectionManager().getParams().getDefaultMaxConnectionsPerHost());
        System.out.println("total max = " + client.getHttpConnectionManager().getParams().getMaxTotalConnections());

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
