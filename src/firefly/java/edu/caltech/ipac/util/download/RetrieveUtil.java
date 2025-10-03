package edu.caltech.ipac.util.download;


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static edu.caltech.ipac.util.download.UriRef.ResourceType.GcsCloud;
import static edu.caltech.ipac.util.download.UriRef.ResourceType.OnPrimUrl;
import static edu.caltech.ipac.util.download.UriRef.ResourceType.S3Cloud;

public class RetrieveUtil {

    private final static int MAX_LENGTH = 30;
    private static final List<String> EXT_LIST =
            Arrays.asList(
                    "ul", FileUtil.FITS, FileUtil.GZ, FileUtil.TAR, FileUtil.PDF, FileUtil.GZ, FileUtil.REG,
                    "votable", FileUtil.VOT, FileUtil.XML, FileUtil.TBL, FileUtil.CSV, FileUtil.TSV, FileUtil.TXT,
                    FileUtil.jpeg, FileUtil.jpg, FileUtil.png, FileUtil.bmp, FileUtil.gif, FileUtil.tiff, FileUtil.tif,
                    FileUtil.FITS+"."+FileUtil.GZ, FileUtil.FITS+"."+FileUtil.BZ2);


    /**
     * Retrieve a file from URL or S3 and cache it.  If the URL is a gz file then uncompress it and return the uncompress version.
     * @param params the configuration about the retrieve request
     * @param dl a Download listener, only used in server mode
     * @return a FileInfo of file returned from this URL.
     * @throws FailedRequestException when request fails
     */
    public static FileInfo downloadCaching(UriRefParams params, DownloadListener dl) throws FailedRequestException {
        if (params==null) throw new FailedRequestException("downloadCaching: params is null");
        FileInfo fileInfo= FileCacheHelper.getFileInfo(params);
        if (fileInfo!=null && !params.getCheckForNewer()) return fileInfo;


        try {
            File fileName= (fileInfo==null) ? FileCacheHelper.makeFile(params.getDownloadDir(), params.getUniqueString()) : fileInfo.getFile();
            var ops= URLDownload.Options.listenerOp(params.getMaxSizeToDownload(), dl);
            Map<String,String> cookies = new HashMap<>(ServerContext.getRequestOwner().getCookieMap());
            if (params.getAddtlCookies()!=null) cookies.putAll(params.getAddtlCookies());
            fileInfo= download(params.getUriRef(), fileName, cookies, params.getHeaders(), ops);
            if (fileInfo.getResponseCode()==200) FileCacheHelper.putFileInfo(params,fileInfo);
            return fileInfo;
        } catch (Exception e) {
            Logger.warn(e.toString());
            throw ResponseMessage.simplifyNetworkCallException(e);
        }
    }


    /**
     *
     * @param uri - this a string url, a URL, a string s3 ref, or a s3 ref object
     * @param outFile output file
     * @param cookies map of cookies
     * @param requestHeaders headers
     * @param ops  options
     * @throws FailedRequestException when something goes wrong
     */
    public static FileInfo download(UriRef uri,
                                    File outFile,
                                    Map<String, String> cookies,
                                    Map<String, String> requestHeaders,
                                    URLDownload.Options ops) throws FailedRequestException {
        if (uri==null) return new FileInfo(404);
        return switch (uri.ref()) {
            case S3Ref s3-> S3Download.getData(s3, outFile, cookies, requestHeaders, ops);
            case GcsRef gcs -> GcsDownload.getData(gcs, outFile, cookies, requestHeaders, ops);
            case URL url -> URLDownload.getDataToFile(url, outFile, cookies, requestHeaders, ops);
            default -> new FileInfo(404);
        };
    }


    public static String makeCacheFileString(UriRef uri, String userLoginName, String desc) {

        if (uri==null) return "no network resource";
        String loc;
        String path;
        String outLoc;
        String prefix;

        switch (uri.ref()) {
            case URL url -> {
                loc= url.getHost();
                path= url.getFile();
                outLoc = FileUtil.makeShortHostName(loc);
                prefix= "URL-";
            }
            case S3Ref s3Ref -> {
                loc= s3Ref.bucket();
                path= s3Ref.key();
                outLoc = loc;
                prefix= "S3-";
            }
            case GcsRef gcsRef -> {
                loc= gcsRef.bucket();
                path= gcsRef.objName();
                outLoc = loc;
                prefix= "GCS-";
            }
            default -> {return "no network resource";}
        }

        String fileStr= path.replace('&','-');
        String loginName= (userLoginName!=null) ? "-"+ userLoginName : "";

        // since this string is limited to MAX_LENGTH, having loginName in the baseKey is not ideal
        // loginName can be long.  it'll be used in hashcode calculation instead.
        String baseKey = fileStr;
        String addtlKeys = desc == null ? "" : desc;
        int originalHashCode = (loc + loginName + baseKey + addtlKeys).hashCode();
        baseKey= baseKey.replaceAll("[ :\\[\\]/\\\\|*?+<>]", "");
        if (baseKey.length()>MAX_LENGTH) {
            baseKey= baseKey.substring(0,MAX_LENGTH);
        }

        String retval;
        retval = prefix + outLoc + "-" + originalHashCode + baseKey;
        //note: "=","," signs causes problem in download servlet.
        retval = retval.replaceAll("[ :\\[\\]/\\\\|*?<>=,]", "-");
        String ext= FileUtil.getExtension(fileStr,true);
        var fileExt= EXT_LIST.contains(ext) ? ext : EXT_LIST.getFirst();
        return retval + "." + fileExt;
    }


    /**
     * if both parameters are non-null then we are assuming they are referencing the same piece of data
     * @param uriList  a list of UriRef that all point to the same data
     * @return the optimal way to retrieve the data
     */
    private static UriRef.ResourceType determineOptimalResourceType(List<UriRef> uriList, UriRef.CloudEnvironment env) {

        var uriS3= uriList.stream().filter( (r) -> r.getType()== S3Cloud).findFirst().orElse(null);
        var uriGcs= uriList.stream().filter( (r) -> r.getType()== GcsCloud).findFirst().orElse(null);
        var uriUrl= uriList.stream().filter( (r) -> r.getType()== OnPrimUrl).findFirst().orElse(null);

        var nonNullsTypes= Stream.of(uriS3,uriGcs,uriUrl).filter(Objects::nonNull).toList();
        if (nonNullsTypes.isEmpty()) return null;
        if (nonNullsTypes.size()==1) return nonNullsTypes.getFirst().getType();

        if (env==UriRef.CloudEnvironment.AWS && uriS3!=null) return S3Cloud;
        if (env==UriRef.CloudEnvironment.GCS && uriGcs!=null) return GcsCloud;
        if (uriUrl!=null) return OnPrimUrl;
        return nonNullsTypes.getFirst().getType(); // if I get to here then I am guessing
    }

    /**
     * Fine the optimal URI from a list
     */
    public static UriRef getOptimalUri(List<UriRef> uriList, UriRef.CloudEnvironment env) {
        if (uriList.isEmpty()) return null;
        if (uriList.size()==1) return uriList.getFirst();
        var resourceType= determineOptimalResourceType(uriList,env);
        if (resourceType==null) return uriList.getFirst();
        return uriList.stream().filter( (r) -> r.getType()==resourceType).findFirst().orElse(null);
    }

    public interface ServiceCaller {
        FileInfo retrieve(CanCallService p, File suggestedFile) throws IOException, FailedRequestException;
    }

    public interface CanCallService {}
}
