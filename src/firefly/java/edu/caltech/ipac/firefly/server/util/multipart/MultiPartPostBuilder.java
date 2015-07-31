/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util.multipart;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
    private String targetURL;
    private List<Part> parts = new ArrayList<Part>();
    private List<Param> params = new ArrayList<Param>();
    private List<Param> headers = new ArrayList<Param>();
    private String userId;
    private String passwd;

    public MultiPartPostBuilder() {}

    public MultiPartPostBuilder(String targetURL) {
        setTargetURL(targetURL);
    }

    public void setTargetURL(String targetURL) {
        this.targetURL = targetURL;
        try {
            URL url = new URL(targetURL);
            if (!StringUtils.isEmpty(url.getUserInfo())) {
                String[] idPass = url.getUserInfo().split(":");
                if (idPass.length == 2) {
                    userId = idPass[0];
                    passwd = idPass[1];
                }
            }
        } catch (MalformedURLException e) {
            // ignore
        }

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
            params.add(new Param(name, file.getPath()));
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

        for(Param p : headers) {
            filePost.addRequestHeader(p.getName(), p.getValue());
        }

        try {

            filePost.setRequestEntity(
                    new MultipartRequestEntity(parts.toArray(new Part[parts.size()]),
                            filePost.getParams())
            );

            HttpServices.executeMethod(filePost, userId, passwd);

            MultiPartRespnse resp = new MultiPartRespnse(filePost.getResponseHeaders(),
                                                filePost.getStatusCode(),
                                                filePost.getStatusText());
            if (responseBody != null) {
                readBody(responseBody, filePost.getResponseBodyAsStream());
            }
            return resp;

        } catch (Exception ex) {
            LOG.error(ex, "Error while posting multipart request to" + targetURL);
            return null;
        } finally {
            filePost.releaseConnection();
        }
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
        logList.add("Post URL: " + targetURL);
        logList.add("Post Params");
        for(Param param : params) {
          logList.add("   " + param.getName()+ "=" + param.getValue());
        }
        String sary[]= logList.toArray(new String[logList.size()]);
        LOG.debug(sary);

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
