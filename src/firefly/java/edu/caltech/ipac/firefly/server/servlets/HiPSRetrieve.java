package edu.caltech.ipac.firefly.server.servlets;
/**
 * User: roby
 * Date: 2019-02-12
 * Time: 11:50
 */


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.LockingRetrieve;
import edu.caltech.ipac.util.FormatUtil;
import edu.caltech.ipac.util.FormatUtil.Format;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.download.UriRefParams;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static edu.caltech.ipac.util.FileUtil.K;
import static edu.caltech.ipac.util.FileUtil.isDirectoryEmpty;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

/**
 * @author Trey Roby
 */
public class HiPSRetrieve {

    private static final List<String> extList= Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<Format> formatList= Arrays.asList(Format.JPEG,Format.PNG,Format.WEBP);
    private static final long minFileLengthOnError = 2*K;
    private static final long minFitsFileLengthOnError = 5*K;
    private static final long minSizeCacheFile = K;

    private static record DeadEntry(long time, int status) {};
    private static final Map<String, DeadEntry> deadFitsUrl= Collections.synchronizedMap(new HashMap<>());
    private static final int MAX_DEAD_URL_SIZE= 10000;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static FileInfo retrieveHiPSData(String urlStr, boolean alwaysUseCached) {
        return retrieveHiPSData(urlStr,null, alwaysUseCached);

    }

    public static FileInfo retrieveHiPSData(String urlStr, String fileName, boolean alwaysUseCached) {
       URL url= URLDownload.makeURL(urlStr);
       if (url==null) return new FileInfo(404, "Invalid URL: " + urlStr);
       var params= new UriRefParams(url);
       params.setNotify(false);
       var dl= LockingRetrieve.makeDownloadProgressOrFind(params);
       return retrieveHiPSData(url, fileName, alwaysUseCached, dl);
    }

    public static FileInfo retrieveHiPSData(UriRefParams params) {
        URL url= params.getUriRef().getURL();
        var dl= LockingRetrieve.makeDownloadProgressOrFind(params);
        return retrieveHiPSData(url, null, !params.getCheckForNewer(), dl);
    }

    /**
     *
     * @param url the url
     * @param fileName add a filename to the cacheName, if null then compute a name
     s @param alwaysUseCached if true and the file is in the cache then don't make a If-Modified-Since call, just return
     * @param dl the download listener
     * @return the retrieved (or cached) file
     */
    public static FileInfo retrieveHiPSData(URL url, String fileName, boolean alwaysUseCached, DownloadListener dl) {
        if (url==null) return new FileInfo(404, "Invalid URL");

        File targetFile = getHipsCacheFile(url, fileName);
        var locationPrepared= prepareTargetLocation(targetFile);

        if (!locationPrepared) {
            return new FileInfo(HttpURLConnection.HTTP_FORBIDDEN,
                    "this hips request conflicts with the HiPS protocol, cannot cache hips file correctly");
        }
        boolean fileExistLocal= isHiPSFileCached(url.toString(),fileName);

        if (alwaysUseCached && fileExistLocal) return new FileInfo(targetFile);

        // if we already have a version of the file set the download modified only option. Also set a very small timeout,
        // so that if the server is down we don't wait long.
        try {
            URLDownload.Options options= fileExistLocal ? URLDownload.Options.modifiedAndTimeoutOp(true,3) : URLDownload.Options.def();
            options.setDl(dl);
            FileInfo fetchedFileInfo= lockUrlDownload(url,fileName,targetFile,options);
            var rCode= fetchedFileInfo.getResponseCode();
            return switch (rCode) {
                case HTTP_OK, HTTP_NOT_MODIFIED -> fetchedFileInfo;
                case HTTP_NOT_FOUND -> cleanupNoFound(targetFile,url);
                case HTTP_CLIENT_TIMEOUT, 429, HTTP_INTERNAL_ERROR, HTTP_BAD_GATEWAY,   // transient failures, use cache if valid
                     HTTP_UNAVAILABLE, HTTP_GATEWAY_TIMEOUT ->
                        isValidCached(targetFile) ? new FileInfo(targetFile) : cleanupBadRequest(targetFile,url,rCode,true);
                default -> cleanupBadRequest(targetFile,url,rCode,false);
            };
        }
        catch (FailedRequestException e) {
            if (isValidCached(targetFile)) return new FileInfo(targetFile); // if the file existed and has content, return it
            else return new FileInfo(e.getResponseCode());
        }

    }

    private static FileInfo cleanupBadRequest(File f, URL url, int rCode, boolean useDeadFits) {
        if (f != null) f.delete();
        if (useDeadFits && url.getPath().toLowerCase().endsWith(".fits")) {
            deadFitsUrl.put(url.toString(), new DeadEntry(System.currentTimeMillis() + 15 * 1000,rCode)); // 15 seconds
        }
        return new FileInfo(rCode);
    }

    private static FileInfo cleanupNoFound(File f, URL url) {
        if (f != null) f.delete();
        if (url.getPath().toLowerCase().endsWith(".fits")) {
            deadFitsUrl.put(url.toString(), new DeadEntry(System.currentTimeMillis() + 10 * 60 * 1000,HTTP_NOT_FOUND)); // 10 minutes
        }
        return imageRequest(f) ? new FileInfo(HTTP_NO_CONTENT) : new FileInfo(HTTP_NOT_FOUND); // return 204 because: the request was valid, but there is no image content to return
    }

    private static boolean prepareTargetLocation(File targetFile) {
        File dir= targetFile.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            return false;
        }

        if (!targetFile.exists()) return true;

        if (targetFile.isDirectory()) {
            if (isDirectoryEmpty(targetFile)) {
                if (!targetFile.delete()) return false;
            }
            else {
                return false;
            }
        }
        if (targetFile.length()<minSizeCacheFile) {
            if (!targetFile.delete()) return false;
        }
        return true;
    }

    private static FileInfo lockUrlDownload(URL url, String fileName, File targetFile, URLDownload.Options options) throws FailedRequestException {
        var params= new UriRefParams(url.toString());
        return LockingRetrieve.lockingRetrieve(params,
                () -> {
                    File f= getHipsCacheFile(url,fileName);
                    if (f!=null && f.canRead() && f.length()>minSizeCacheFile) return new FileInfo(targetFile);;
                    return URLDownload.getDataToFile(url,targetFile,null, null, options);
                });
    }

    public static void retrieveHiPSTileInBackground(String urlStr) {
        if (isRetrieving(urlStr)) return;
        executor.execute(() -> retrieveHiPSData(urlStr,false));
    }

    public static boolean isRetrieving(String urlStr) {
        return LockingRetrieve.isActiveRequest(urlStr);
    }

    public static File getHipsCacheFile(URL url, String fileName) {
        if (url==null) return null;
        String fPath = fileName == null ? url.getPath() : (url.getPath() + "/" + fileName);
        File dir= new File(ServerContext.getHiPSDir(),new File(url.getHost() + fPath).getParent());
        return new File(dir, new File((fileName == null ? url.getFile() : fileName)).getName());
    }

    public static boolean isHiPSFileCached(String urlStr) {
        return isHiPSFileCached(urlStr,null);
    }

    public static boolean isHiPSFileCached(String urlStr, String fileName) {
        if (isRetrieving(urlStr)) return false;
        URL url= URLDownload.makeURL(urlStr);
        if (url==null) return false;
        File f= getHipsCacheFile(url,fileName);
        if (f==null) return false;
        return f.canRead() && f.length()>minSizeCacheFile;
    }

    private static boolean imageRequest(File f) {
        if (f==null) return false;
        String lowerF= f.getAbsolutePath().toLowerCase();
        return extList.stream().anyMatch(ext -> lowerF.endsWith("." + ext));
    }

    /**
     * If we are getting errors then do some basic validation on the cached file
     * @param cachedFile the cache file on disk
     * @return true if valid
     */
    private static boolean isValidCached(File cachedFile) {
        if (cachedFile==null) return false;
        if (!cachedFile.canRead()) return false;
        if (cachedFile.length()<minFileLengthOnError) return false;
        try {
            String fLowStr= cachedFile.getAbsolutePath().toLowerCase();
            if (fLowStr.endsWith("properties") || fLowStr.endsWith("list")) {
                try (var fr= new FileReader(cachedFile)) {
                    Properties p = new Properties();
                    p.load(fr);
                    if (p.size()<2) return false;
                }
            }
            else if (imageRequest(cachedFile) ) {
                var format= Format.fromMime(FormatUtil.getMimeType(cachedFile).mime());
                return formatList.contains(format);
            }
            else if (cachedFile.getAbsolutePath().toLowerCase().endsWith("fits") ) {
                return cachedFile.length() >= minFitsFileLengthOnError;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean isDeadFitsUrl(String urlStr) {
        var entry= deadFitsUrl.get(urlStr);
        if (entry == null) return false;
        if (deadFitsUrl.size() > MAX_DEAD_URL_SIZE) cleanDeadFitsUrlCache();
        if (System.currentTimeMillis() > entry.status) {
            deadFitsUrl.remove(urlStr);
            return false;
        }
        return true;
    }

    public static int getDeadFitsUrlCode(String urlStr) {
        if (isDeadFitsUrl(urlStr)) {
            var entry= deadFitsUrl.get(urlStr);
            return entry!=null ? entry.status : 200;
        }
        return 200;
    }

    private synchronized static void cleanDeadFitsUrlCache() {
        if (deadFitsUrl.size() <= MAX_DEAD_URL_SIZE) return;
        var copyMap= new HashMap<>(deadFitsUrl);
        var cleanedUpMap= new HashMap<String, DeadEntry>();
        var cTime= System.currentTimeMillis();
        deadFitsUrl.clear();
        copyMap.forEach((key, entry) -> {
            if (cTime<entry.time) cleanedUpMap.put(key, entry);
        });
        if (cleanedUpMap.size()<MAX_DEAD_URL_SIZE*.8) deadFitsUrl.putAll(cleanedUpMap);
    }
}
