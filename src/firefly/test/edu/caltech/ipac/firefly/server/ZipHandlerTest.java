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
import java.util.zip.ZipOutputStream;


public class ZipHandlerTest extends ConfigTest {

    static String fileExtName = "images/5/0072/50072211/1/50072211-21/50072211.50072211-21.IRAC.4.mosaic.fits";

    @BeforeClass
    public static void setUp() {
        // needed when dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
    }

    public List<FileGroup> getData() {
        long fgSize = 0;
        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();

        FileInfo fi = new FileInfo("https://irsa.ipac.caltech.edu/data/SPITZER/Enhanced/SEIP/"+fileExtName, fileExtName, 0);
        fiArr.add(fi);
        fgSize+=fi.getSizeInBytes();

        FileGroup fg = new FileGroup(fiArr, null, fgSize, "Mock download files");
        fgArr.add(fg);

        return fgArr;
    }

    public ZipOutputStream getZipOut() throws Exception {
        File stagingDir = ServerContext.getStageWorkDir();
        File zipFile = new File(stagingDir, "test.zip");
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));
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


}
