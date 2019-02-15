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

/**
 * @author Trey Roby
 */
public class HiPSRetrieve {

    public static FileInfo retrieveHiPSData(String urlStr, String pathExt) {
        try {
            URL url= new URL(urlStr);

            String fPath = pathExt == null ? url.getPath() : (url.getPath() + "/" + pathExt);
            File dir= new File(ServerContext.getHiPSDir(),new File(url.getHost() + fPath).getParent());
            if (!dir.exists()) dir.mkdirs();

            File targetFile= new File(dir, new File((pathExt == null ? url.getFile() : pathExt)).getName());
            FileInfo fi= URLDownload.getDataToFile(url,targetFile);
            int rCode= fi.getResponseCode();
            File retFile= fi.getFile();

            switch (rCode) {
                case 200:
                    if (isValid(retFile)) {
                        return fi;
                    }
                    else {
                        retFile.delete();
                        return new FileInfo(null, rCode);
                    }
                case 304:
                    return fi;
                default:
                    if (retFile!=null)  retFile.delete();
                    if (rCode==404 && imageRequest(retFile)) return new FileInfo(null, 204);
                    else return new FileInfo(null, rCode);
            }
        } catch (MalformedURLException | FailedRequestException e) {
            return new FileInfo(null, null, 404, e.toString());
        }
    }

    private static boolean imageRequest(File f) {
        String fLowStr= f.getAbsolutePath().toLowerCase();
        return fLowStr.endsWith("jpg") || fLowStr.endsWith("jpeg") || fLowStr.endsWith("png");
    }

    private static boolean isValid(File f) {
        try {
            String fLowStr= f.getAbsolutePath().toLowerCase();
            if (fLowStr.equals("properties") || fLowStr.equals("list")) {
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
