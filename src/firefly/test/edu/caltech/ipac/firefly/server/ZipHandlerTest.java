/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.ZipHandler;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;


public class ZipHandlerTest extends ConfigTest {

    static String fileExtName = "images/5/0072/50072211/1/50072211-21/50072211.50072211-21.IRAC.%d.mosaic.fits";

    @BeforeClass
    public static void setUp() {
        // needed when dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
    }

    private List<FileGroup> getData() {
        return getData(1,1, null);
    }

    private List<FileGroup> getData(int groups, int files, String baseUrl) {
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();

        for (int g=0; g < groups; g++) {
            long fgSize = 0;
            ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
            for (int f=0; f<files; f++) {
                String url = baseUrl == null ? "https://irsa.ipac.caltech.edu/data/SPITZER/Enhanced/SEIP/"+ String.format(fileExtName, (f%4)+1) : baseUrl;
                String extName = "test-" + g+f+".fits";
                FileInfo fi = new FileInfo(url+"?"+g+f, extName, 0);
                fiArr.add(fi);
                fgSize+=fi.getSizeInBytes();
            }
            FileGroup fg = new FileGroup(fiArr, null, fgSize, "group-" + g);
            fgArr.add(fg);
        }
        return fgArr;
    }

    private ZipOutputStream getZipOut() throws Exception {
        File stagingDir = ServerContext.getStageWorkDir();
        File zipFile = new File(stagingDir, "test.zip");
        return getZipOut(zipFile);
    }

    private ZipOutputStream getZipOut(File outf) throws Exception {
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outf));
        zout.setMethod(ZipOutputStream.DEFLATED);
        zout.setLevel(ZipHandler.COMPRESSION_LEVEL);
        return zout;
    }


    //with accept-encoding: none --- Avg time is 2.5527 SECONDS.
    //with :identity --- Avg time is 2.7523 SECONDS --- through vpn: 10.7010 SECONDS (first run took 17s then reduced in all next iterations)
    //with :gzip, deflate --- Avg time is 2.5520 SECONDS. --- vpn: 10.5780 SECONDS
    @Category({TestCategory.Perf.class})
    @Test
    public void perfTest() throws Exception {
        List<FileGroup> result = getData();
        ZipOutputStream zout = getZipOut();

        Logger.setLogLevel(Level.TRACE, "edu.caltech");     // exclude numerous nom.tam warnings

        for (FileGroup fg : result) {
            ZipHandler zipHandler = new ZipHandler((fg.getBaseDir()));

            for(int i=0; i < 3; i++) {
                StopWatch.getInstance().start("zipHandler");
                for (FileInfo fi : fg) {
                    zipHandler.addZipEntry(zout, fi);
                }
                StopWatch.getInstance().printLog("zipHandler", StopWatch.Unit.SECONDS);
            }
        }

        FileUtil.silentClose(zout);
        StopWatch.getInstance().printLog("zipHandler", StopWatch.Unit.SECONDS);
    }



    // Avg time is 2.3403 SECONDS, 3.6683 SECONDS, 2.8333 SECONDS --- vpn: 12.4827 SECONDS
    @Test
    public void fetchFileHttpService() throws Exception {
        List<FileGroup> result = getData();
        File f = File.createTempFile("test", "fits");

        Logger.setLogLevel(Level.TRACE, "edu.caltech");     // exclude numerous nom.tam warnings
        for(int i=0; i < 3; i++) {
            StopWatch.getInstance().start("fetchFileHttp");

            for (FileGroup fg : result) {
                for (FileInfo fi : fg) {
                    HttpServices.getData(fi.getInternalFilename(), f);
                }
            }

            StopWatch.getInstance().stop("fetchFileHttp");
        }

        StopWatch.getInstance().printLog("fetchFileHttp", StopWatch.Unit.SECONDS);

    }



    // accept-encoding: gzip, deflate --- Avg time is 1.4053 SECONDS --- vpn: 10.8920 SECONDS
    // :identity --- Avg time is 1.6303 SECONDS --- vpn: 11.0910 SECONDS
    // :none --- Avg time is 1.4173 SECONDS.
    @Test
    public void fetchFileUrlDownload() throws Exception {
        List<FileGroup> result = getData();
        File f = File.createTempFile("test", "fits");
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "identity"); //comment it for gzip case

        Logger.setLogLevel(Level.TRACE, "edu.caltech");     // exclude numerous nom.tam warnings
        for(int i=0; i < 3; i++) {
            StopWatch.getInstance().start("fetchFileUrlDownload");

            for (FileGroup fg : result) {
                for (FileInfo fi : fg) {
                    URLDownload.getDataToFile(new URL(fi.getInternalFilename()), f, null, headers,
                            URLDownload.Options.modifiedOp(false));
                }
            }

            StopWatch.getInstance().stop("fetchFileUrlDownload");
        }

        StopWatch.getInstance().printLog("fetchFileUrlDownload", StopWatch.Unit.SECONDS);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void zipHandlerSSH() throws Exception {
        /*
        14:27:27.595 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-1 ran with elapsed time of 0.1460 SECONDS
        14:27:29.389 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-1 ran with elapsed time of 1.7880 SECONDS
        14:27:29.390 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-1 ran with elapsed time of 1.9470 SECONDS

        14:27:29.512 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-2 ran with elapsed time of 0.1220 SECONDS
        14:27:31.291 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-2 ran with elapsed time of 1.7760 SECONDS
        14:27:31.292 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-2 ran with elapsed time of 1.9010 SECONDS

        14:27:31.411 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-3 ran with elapsed time of 0.1180 SECONDS
        14:27:33.243 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-3 ran with elapsed time of 1.8300 SECONDS
        14:27:33.244 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-3 ran with elapsed time of 1.9510 SECONDS

        14:27:33.322 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-1 ran with elapsed time of 0.0770 SECONDS
        14:27:33.364 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-2 ran with elapsed time of 0.0400 SECONDS
        14:27:33.403 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-3 ran with elapsed time of 0.0370 SECONDS

        14:27:34.866 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-1 ran with elapsed time of 1.4590 SECONDS
        14:27:36.327 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-2 ran with elapsed time of 1.4600 SECONDS
        14:27:37.803 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-3 ran with elapsed time of 1.4750 SECONDS
        14:27:37.803 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-total ran with elapsed time of 4.4000 SECONDS
         */

        doZipTest(getData(1,3, "file:///Users/loi/dev/cm/stage/irsa-spitzer-supermosaics-v2.5-2/images/5/0072/50072211/1/50072211-21/50072211.50072211-21.IRAC.1.mosaic.fits"));
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void zipHandlerSSHThreaded() throws Exception {
        /*
        14:34:32.154 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-1 ran with elapsed time of 0.0640 SECONDS
        14:34:33.926 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-1 ran with elapsed time of 1.7680 SECONDS
        14:34:33.927 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-1 ran with elapsed time of 1.8410 SECONDS

        14:34:33.983 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-2 ran with elapsed time of 0.0560 SECONDS
        14:34:35.751 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-2 ran with elapsed time of 1.7670 SECONDS
        14:34:35.753 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-2 ran with elapsed time of 1.8250 SECONDS

        14:34:35.811 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-3 ran with elapsed time of 0.0560 SECONDS
        14:34:37.591 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-3 ran with elapsed time of 1.7790 SECONDS
        14:34:37.593 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-3 ran with elapsed time of 1.8390 SECONDS

        14:34:37.671 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-1 ran with elapsed time of 0.0760 SECONDS
        14:34:37.712 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-2 ran with elapsed time of 0.0410 SECONDS
        14:34:37.751 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-3 ran with elapsed time of 0.0370 SECONDS

        14:34:39.356 [pool-4-thread-2] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler ran with elapsed time of 1.5950 SECONDS
        14:34:39.356 [pool-4-thread-3] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler ran with elapsed time of 1.5950 SECONDS
        14:34:39.359 [pool-4-thread-1] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler ran with elapsed time of 1.6000 SECONDS
        14:34:39.360 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-total ran with elapsed time of 1.6080 SECONDS
         */

        doZipTest(getData(3,1, "file:///Users/loi/dev/cm/stage/irsa-spitzer-supermosaics-v2.5-2/images/5/0072/50072211/1/50072211-21/50072211.50072211-21.IRAC.1.mosaic.fits"), true);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void zipHandler() throws Exception {
        /*
        14:35:28.525 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-1 ran with elapsed time of 0.9990 SECONDS
        14:35:30.315 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-1 ran with elapsed time of 1.7850 SECONDS
        14:35:30.316 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-1 ran with elapsed time of 2.7950 SECONDS

        14:35:32.006 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-2 ran with elapsed time of 1.6890 SECONDS
        14:35:33.506 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-2 ran with elapsed time of 1.4970 SECONDS
        14:35:33.507 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-2 ran with elapsed time of 3.1900 SECONDS

        14:35:34.917 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-3 ran with elapsed time of 1.4070 SECONDS
        14:35:36.732 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-3 ran with elapsed time of 1.8150 SECONDS
        14:35:36.733 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-3 ran with elapsed time of 3.2250 SECONDS

        14:35:38.454 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-1 ran with elapsed time of 1.7190 SECONDS
        14:35:39.155 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-2 ran with elapsed time of 0.6970 SECONDS
        14:35:39.821 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-3 ran with elapsed time of 0.6630 SECONDS

        14:35:42.994 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-1 ran with elapsed time of 3.1630 SECONDS
        14:35:45.806 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-2 ran with elapsed time of 2.8110 SECONDS
        14:35:49.218 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-3 ran with elapsed time of 3.4110 SECONDS
        14:35:49.219 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-total ran with elapsed time of 9.3980 SECONDS
         */

        doZipTest(getData(1,3, null));
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void zipHandlerThreaded() throws Exception {
        /*
        14:36:31.378 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-1 ran with elapsed time of 2.1820 SECONDS
        14:36:33.144 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-1 ran with elapsed time of 1.7610 SECONDS
        14:36:33.144 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-1 ran with elapsed time of 3.9520 SECONDS

        14:36:33.831 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-2 ran with elapsed time of 0.6860 SECONDS
        14:36:35.600 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-2 ran with elapsed time of 1.7680 SECONDS
        14:36:35.602 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-2 ran with elapsed time of 2.4560 SECONDS

        14:36:37.239 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-3 ran with elapsed time of 1.6350 SECONDS
        14:36:39.008 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-3 ran with elapsed time of 1.7680 SECONDS
        14:36:39.009 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-3 ran with elapsed time of 3.4070 SECONDS

        14:36:40.547 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-1 ran with elapsed time of 1.5360 SECONDS
        14:36:41.455 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-2 ran with elapsed time of 0.9060 SECONDS
        14:36:42.234 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-3 ran with elapsed time of 0.7780 SECONDS

        14:36:44.456 [pool-4-thread-3] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler ran with elapsed time of 2.2070 SECONDS
        14:36:45.010 [pool-4-thread-2] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler ran with elapsed time of 2.7650 SECONDS
        14:36:45.311 [pool-4-thread-1] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler ran with elapsed time of 3.0650 SECONDS
        14:36:45.312 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-total ran with elapsed time of 3.0770 SECONDS
         */

        doZipTest(getData(3,1, null), true);
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void zipHandlerExternal() throws Exception {
        /*
        14:50:10.718 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-1 ran with elapsed time of 4.8010 SECONDS
        14:50:11.067 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-1 ran with elapsed time of 0.3430 SECONDS
        14:50:11.068 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-1 ran with elapsed time of 5.1590 SECONDS

        14:50:14.413 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-2 ran with elapsed time of 3.3420 SECONDS
        14:50:14.756 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-2 ran with elapsed time of 0.3410 SECONDS
        14:50:14.757 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-2 ran with elapsed time of 3.6880 SECONDS

        14:50:18.990 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdcurl-3 ran with elapsed time of 4.2290 SECONDS
        14:50:19.329 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdzip-3 ran with elapsed time of 0.3370 SECONDS
        14:50:19.329 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - cmdboth-3 ran with elapsed time of 4.5720 SECONDS

        14:50:22.791 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-1 ran with elapsed time of 3.4590 SECONDS
        14:50:26.249 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-2 ran with elapsed time of 3.4540 SECONDS
        14:50:29.290 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - urlDL-3 ran with elapsed time of 3.0390 SECONDS

        14:50:33.160 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-1 ran with elapsed time of 3.8650 SECONDS
        14:50:38.151 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-2 ran with elapsed time of 4.9890 SECONDS
        14:50:44.217 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-3 ran with elapsed time of 6.0630 SECONDS
        14:50:44.218 [main] TRACE edu.caltech.ipac.firefly.server.util.StopWatch - ziphandler-total ran with elapsed time of 14.9270 SECONDS
        */

        doZipTest(getData(1,3, "http://ipv4.download.thinkbroadband.com/10MB.zip"));
    }

    private void doZipTest(List<FileGroup> tozip) throws Exception {
        doZipTest(tozip, false);
    }
    private void doZipTest(List<FileGroup> tozip, boolean multiThreads) throws Exception {

//        Logger.setLogLevel(Level.TRACE, "edu.caltech");     // exclude numerous nom.tam warnings
        Logger.setLogLevel(Level.TRACE, "edu.caltech.ipac.firefly.server.util.StopWatch");     // prints only StopWatch logs

        // command line, curl then zip
        int tries = 1;
        for (FileGroup fg : tozip) {
            for (FileInfo fi : fg) {
                StopWatch.getInstance().start("cmdboth-" + tries);
                StopWatch.getInstance().start("cmdcurl-" + tries);
                Runtime.getRuntime().exec("curl -s -o " + fi.getExternalName() + " " + fi.getInternalFilename()).waitFor();
                StopWatch.getInstance().printLog("cmdcurl-" + tries);

                StopWatch.getInstance().start("cmdzip-" + tries);
                Runtime.getRuntime().exec("zip -r cmdline.zip " + fi.getExternalName()).waitFor();
                StopWatch.getInstance().printLog("cmdzip-" + tries);
                StopWatch.getInstance().printLog("cmdboth-" + tries++);
                System.out.println();
            }
        }


        tries = 1;
        for (FileGroup fg : tozip) {
            for (FileInfo fi : fg) {
                File f = File.createTempFile("urldownload-", ".fits", new File(".")); // put file in current work dir
                StopWatch.getInstance().start("urlDL-" + tries);
                URLDownload.getDataToFile(new URL(fi.getInternalFilename()), f, null, null, URLDownload.Options.modifiedOp(false));
                StopWatch.getInstance().printLog("urlDL-" + tries++);
            }
        }
        System.out.println();


        tries = 1;
        ExecutorService threads = Executors.newFixedThreadPool(5);
        StopWatch.getInstance().start("ziphandler-total");
        for (FileGroup fg : tozip) {
            if (multiThreads) {
                threads.submit(() -> {
                    try {
                        ZipOutputStream zout = getZipOut(new File(fg.getDesc()+".zip"));
                        ZipHandler zipHandler = new ZipHandler((fg.getBaseDir()));
                        for (FileInfo fi : fg) {
                            StopWatch.getInstance().start("ziphandler");
                            zipHandler.addZipEntry(zout, fi);
                            StopWatch.getInstance().printLog("ziphandler");
                        }
                    } catch (Exception ignored) {}
                });
            } else {
                ZipOutputStream zout = getZipOut(new File(fg.getDesc()+".zip"));
                ZipHandler zipHandler = new ZipHandler((fg.getBaseDir()));
                for (FileInfo fi : fg) {
                    StopWatch.getInstance().start("ziphandler-" + tries);
                    zipHandler.addZipEntry(zout, fi);
                    StopWatch.getInstance().printLog("ziphandler-" + tries++);
                }
            }
        }
        if (multiThreads) {
            threads.shutdown();
            threads.awaitTermination(20, TimeUnit.SECONDS);
        }
        StopWatch.getInstance().printLog("ziphandler-total");
    }

}
