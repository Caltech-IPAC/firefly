package edu.caltech.ipac.firefly.server.servlets;
/**
 * User: roby
 * Date: 2019-02-12
 * Time: 11:50
 */


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;

/**
 * @author Trey Roby
 */
public class HiPSRetrieve {

    public static FileInfo retrieveHiPSData(String urlStr, String pathExt, boolean alwaysUseCached) {
        try {
            URL url= new URL(urlStr);

            String fPath = pathExt == null ? url.getPath() : (url.getPath() + "/" + pathExt);
            File dir= new File(ServerContext.getHiPSDir(),new File(url.getHost() + fPath).getParent());
            if (!dir.exists()) dir.mkdirs();

            File targetFile= new File(dir, new File((pathExt == null ? url.getFile() : pathExt)).getName());
            boolean fileExistLocal= targetFile.canRead() && targetFile.length()>400;
            FileInfo preFetchFileInfo= new FileInfo(targetFile);
            if (alwaysUseCached && fileExistLocal) return preFetchFileInfo;

            // if we already have a version of the file set the download modified only option. Also set a very time timeout,
            // so that if the server is down we don't wait long.
            URLDownload.Options options= fileExistLocal ? URLDownload.Options.modifiedAndTimeoutOp(true,4) : URLDownload.Options.def();
            int rCode;
            File retFile;
            FileInfo fetchedFileInfo;
            try {
                fetchedFileInfo= URLDownload.getDataToFile(url,targetFile,null, null, options);
                rCode= fetchedFileInfo.getResponseCode();
                retFile= fetchedFileInfo.getFile();
            }
            catch (FailedRequestException e) {
                return fileExistLocal ? preFetchFileInfo : new FileInfo(e.getResponseCode());
            }

            switch (rCode) {
                case 200 -> {
                    if (isValid(retFile)) return fetchedFileInfo;
                    if (retFile!=null) retFile.delete();
                    return new FileInfo(rCode);
                }
                case HTTP_NOT_MODIFIED -> {
                    return fetchedFileInfo;
                }
                case HTTP_GATEWAY_TIMEOUT, HTTP_CLIENT_TIMEOUT -> {
                    return fileExistLocal ? preFetchFileInfo : new FileInfo(rCode);
                }
                default -> {
                    if (fileExistLocal && targetFile.length() > 400) return preFetchFileInfo; // if the file existed and it still has content, return it
                    if (retFile != null) retFile.delete();
                    if (rCode == 404 && imageRequest(retFile)) return new FileInfo(204);
                    else return new FileInfo(rCode);
                }
            }
        } catch (MalformedURLException e) {
            return new FileInfo(null, null, 404, e.toString());
        }
    }

    private static boolean imageRequest(File f) {
        if (f==null) return false;
        String fLowStr= f.getAbsolutePath().toLowerCase();
        return fLowStr.endsWith("jpg") || fLowStr.endsWith("jpeg") || fLowStr.endsWith("png");
    }

    private static boolean isValid(File f) {
        if (f==null) return false;
        try {
            String fLowStr= f.getAbsolutePath().toLowerCase();
            if (fLowStr.endsWith("properties") || fLowStr.endsWith("list")) {
                Properties p = new Properties();
                p.load(new FileReader(f));
                if (p.size()<2) return false;
            }
            else if (imageRequest(f)) {
                BufferedImage i = ImageIO.read(f);
                if (i==null) return false;
            }

        } catch (IOException e) {
            return false;
        }

        return true;
    }


}
