package edu.caltech.ipac.heritage;

import edu.caltech.ipac.util.StringUtil;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Date: Mar 17, 2009
 *
 * @author loi
 * @version $Id: LoadTest.java,v 1.6 2009/10/08 01:17:26 loi Exp $
 */
public class LoadTest implements Runnable {
    static String hostName = "shadev1";
    static final String packageUrl = "http://%s/heritage/rpc/PackagingServices";
    static final String searchUrl = "http://%s/heritage/rpc/FireFly_SearchServices";
    static final String heritageSearchUrl = "http://%s/heritage/rpc/SearchServices";
    static final String plotUrl = "http://%s/heritage/sticky/FireFly_PlotService";
    private static int count;
    private static long waitTime = 10;
    private String name;
    private static  boolean testAll = true;
    private static  boolean testQuery;
    private static  boolean testDownload;
    private static  boolean testPlot;


    public LoadTest(String name) {
        this.name = name;
    }

    public String getSearchUrl() {
        return String.format(searchUrl, hostName);
    }

    public String getPlotUrl() {
        return String.format(plotUrl, hostName);
    }

    public String getPackageUrl() {
        return String.format(packageUrl, hostName);
    }

    public static void main(String[] params) throws InterruptedException {

        String host = System.getProperty("server.host.name");
        if (!StringUtil.isEmpty(host)) {
            hostName = host;
        }

        if (params.length < 2) {
            System.out.print("Usage:  number_of_test runs  number_of_users [wait_time] [test_options] \n" +
                        "<blank>    perform all tests \n" +
                        "-query     perform query test \n" +
                        "-download  perform download test \n" +
                        "-plot      perform plot test \n"
                        );
        }

        count = Integer.parseInt(params[0]);
        int users = Integer.parseInt(params[1]);

        if (params.length>2) {
            String wt = params[2];
            try {
                waitTime = Integer.parseInt(wt) * 1000;
            } catch(Exception e) {
                // not a waitTime param
            }
        }

        for(String p : params) {
            if (p.equals("-query")) {
                testAll = false;
                testQuery = true;
            } else if (p.equals("-download")) {
                testAll = false;
                testDownload = true;
            } else if (p.equals("-plot")) {
                testAll = false;
                testPlot = true;
            }
        }

        long startTime = System.currentTimeMillis();

        ArrayList<Thread> threads = new ArrayList<Thread>(users);
        for(int i = 0; i < users; i++) {
            System.out.println("Starting User " + i);
            Thread t = new Thread(new LoadTest("User " + i));
            t.start();
            threads.add(t);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for(Thread t : threads) {
            t.join();
        }

        System.out.println(count * users + " numbers of request submitted within " +
                (System.currentTimeMillis() - startTime)/1000 + " seconds");

    }

    private String sendRequest(HttpClient client, String url, String key, String value) throws IOException {

        // Create a method instance.
        PostMethod method = new PostMethod(url);
        method.addParameter(new NameValuePair(key, value));
        method.setRequestHeader("Content-Type",
                        "text/x-gwt-rpc; charset=utf-8");
        StringRequestEntity data = new StringRequestEntity(key + "=" + value);

        method.setRequestEntity(data);

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        try {
          // Execute the method.
          int statusCode = client.executeMethod(method);

          if (statusCode != HttpStatus.SC_OK) {
            System.err.println("Method failed: " + method.getStatusLine());
          }

          // Read the response body.
          byte[] responseBody = method.getResponseBody();

          // Deal with the response.
          // Use caution: ensure correct character encoding and is not binary data
          return new String(responseBody);

        } catch (HttpException e) {
          System.err.println("Fatal protocol violation: " + e.getMessage());
          e.printStackTrace();
        } catch (IOException e) {
          System.err.println("Fatal transport error: " + e.getMessage());
          e.printStackTrace();
        } finally {
          // Release the connection.
          method.releaseConnection();
        }
        return "";
    }


    public void run() {

        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();
        for (int i = 0; i < count; i++) {
            try {
                // query test
                if (testAll || testQuery) {
                    sendRequest(client, getSearchUrl(),
                            "5|0|16|http://" + hostName + "/heritage/|D0AB9658D29BE4A5A411AAEB4BB90861|edu.caltech.ipac.firefly.rpc.SearchServices|getRawDataSet|edu.caltech.ipac.firefly.data.ServerRequest|edu.caltech.ipac.heritage.searches.SearchByProgramID$Req/948193282|edu.caltech.ipac.heritage.data.entity.DataType/3323158111|java.util.ArrayList/3821976829|bcdByProgramID|java.util.HashMap/962170901|java.lang.String/2004016611|DoSearch|edu.caltech.ipac.firefly.data.ServerRequest$Param/268811665|true|SearchByProgram.field.programID|40020|1|2|3|4|1|5|6|7|1|8|0|9|150|10|2|11|12|13|0|12|14|11|15|13|0|15|16|0|0|",
                            ""
                            );

                    sendRequest(client, getSearchUrl(),
                            "5|0|16|http://" + hostName + "/heritage/|D0AB9658D29BE4A5A411AAEB4BB90861|edu.caltech.ipac.firefly.rpc.SearchServices|getRawDataSet|edu.caltech.ipac.firefly.data.ServerRequest|edu.caltech.ipac.heritage.searches.SearchByProgramID$Req/948193282|edu.caltech.ipac.heritage.data.entity.DataType/3323158111|java.util.ArrayList/3821976829|pbcdByProgramID|java.util.HashMap/962170901|java.lang.String/2004016611|DoSearch|edu.caltech.ipac.firefly.data.ServerRequest$Param/268811665|true|SearchByProgram.field.programID|40020|1|2|3|4|1|5|6|7|2|8|0|9|150|10|2|11|12|13|0|12|14|11|15|13|0|15|16|0|0|",
                            ""
                            );

                    sendRequest(client, String.format(heritageSearchUrl, hostName),
                            "5|0|8|http://" + hostName + "/heritage/|BB6A30D2D507DC3A26E5E5FE45DAA04E|edu.caltech.ipac.heritage.rpc.SearchServices|searchByRequestIDMap|java.lang.String|edu.caltech.ipac.firefly.data.Request|21640192|IracMap|1|2|3|4|3|5|5|6|7|8|0|",
                            ""
                            );
                }

//                // download test
//                if (testAll || testDownload) {
//                    sendRequest(client, getPackageUrl(),
//                            "5|0|28|http://" + hostName + "/heritage/|4D61C185D0BBF018B0A8E93142A8C8CC|edu.caltech.ipac.heritage.rpc.PackagingServices|packageRequest|edu.caltech.ipac.firefly.data.DownloadRequest|edu.caltech.ipac.heritage.data.entity.download.DownloadParams|edu.caltech.ipac.firefly.data.DownloadRequest/1221027267|program40020-|Program 40020: |aorByProgramID|java.util.HashMap/962170901|java.lang.String/2004016611|DoSearch|edu.caltech.ipac.firefly.data.ServerRequest$Param/268811665|true|SearchByProgram.field.programID|40020|edu.caltech.ipac.heritage.data.entity.download.DownloadParams/737027869|program40020-2-selected_AORs|java.util.ArrayList/3821976829|edu.caltech.ipac.heritage.data.entity.DataType/3323158111|edu.caltech.ipac.firefly.data.table.SelectionInfo/1890422053|java.util.HashSet/1594477813|java.lang.Integer/3438268394|Program 40020: 2 selected observation request's|java.util.Arrays$ArrayList/1243019747|[Ledu.caltech.ipac.heritage.data.entity.WaveLength;/135383007|edu.caltech.ipac.heritage.data.entity.WaveLength/2139151292|1|2|3|4|2|5|6|7|8|9|0|10|0|11|2|12|13|14|0|13|15|12|16|14|0|16|17|0|0|18|19|20|2|21|1|21|2|0|22|23|1|24|0|3|1|25|26|27|8|28|0|28|1|28|2|28|3|28|4|28|5|28|6|28|7|",
//                            ""
//                            );
//                }
//
//                // visualize test
//                if (testAll || testPlot) {
////                    sendRequest(client, getPlotUrl(),
////                            "5|0|6|http://" + hostName + "/heritage/|E27B531CE7617E50B3AA79B47F49D8EC|edu.caltech.ipac.firefly.rpc.PlotService|deletePlot|java.lang.String|PlotClientCtx-6|1|2|3|4|1|5|6|",
////                            ""
////                            );
//
//                    String s = sendRequest(client, getPlotUrl(),
//                            "5|0|11|http://shadev1/heritage/|E27B531CE7617E50B3AA79B47F49D8EC|edu.caltech.ipac.firefly.rpc.PlotService|getWebPlot|edu.caltech.ipac.firefly.visualize.WebPlotRequest|edu.caltech.ipac.firefly.visualize.WebPlotRequest/543075284|/sha/archive/shaproc/IRAC011800/req21640192/chan2/bcd/SPITZER_I2_21640192_0000_0000_1_bcd.fits|edu.caltech.ipac.firefly.visualize.WebPlotRequest$ServiceType/569673048|IRAC_Center_of_4.5&8.0umArray|edu.caltech.ipac.firefly.visualize.WebPlotRequest$RequestType/2218836859|Fits file: /sha/archive/shaproc/IRAC011800/req21640192/chan2/bcd/SPITZER_I2_21640192_0000_0000_1_bcd.fits|1|2|3|4|1|5|6|7|1|NaN|8|5|1|0|9|10|1|0|11|0|",
//                            ""
//                            );
//                }
                Thread.sleep(waitTime);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(name + " has submitted " + count + " requests");

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

