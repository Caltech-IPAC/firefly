/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.LockingRetrieve;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.GcsRef;
import edu.caltech.ipac.util.download.ResponseMessage;
import edu.caltech.ipac.util.download.RetrieveUtil;
import edu.caltech.ipac.util.download.S3Ref;
import edu.caltech.ipac.util.download.UriRef;
import edu.caltech.ipac.util.download.UriRefParams;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Stream;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static edu.caltech.ipac.util.download.UriRef.ResourceType.S3Cloud;

@FileRetrieverImpl(id ="URI")
public class URIFileRetriever implements FileRetriever {


    public FileInfo getFile(WebPlotRequest request) throws FailedRequestException, GeomException, SecurityException {
        return getFile(request,true);
    }

    public FileInfo getFile(WebPlotRequest request, boolean handleAllErrors) throws FailedRequestException, GeomException, SecurityException {

        if ((request.getURL()!=null && request.getURL().toLowerCase().startsWith("file:///"))) {
            return new LocalFileRetriever().getFileByName(request.getURL().substring(7));
        }

        var s3Ref= makeS3UriRef(request);
        var gcsRef= makeGcsUriRef(request);
        var urlStr = request.getURL();

        UriRef.ResourceType resType= UriRef.determineType(urlStr);
        UriRef url = resType==UriRef.ResourceType.OnPrimUrl ? UriRef.make(makeUrl(urlStr)) : UriRef.make(urlStr);

        var list= Stream.of(url,s3Ref,gcsRef).filter(Objects::nonNull).toList();
        if (list.isEmpty()) throw new FailedRequestException("url, s3, gcs ref are all null");
        var progressKey = makeProgressKey(request);
        UriRefParams params= new UriRefParams(list, progressKey,request.getPlotId());
        params.setOptimalUriRef(RetrieveUtil.getOptimalUri(params.getUriList(), ServerContext.getCloudEnvironment()));
        params.setExpectStaticFile(request.getExpectStaticFile());
        return doGetFile(params, request, handleAllErrors);
    }

    private static URL makeUrl(String urlStr) {
        try {
            if (UriRef.determineType(urlStr)== UriRef.ResourceType.OnPrimUrl) {
                return processUrl(urlStr);
            }
        } catch (FailedRequestException ignore) { }
        return null;
    }

    private static FileInfo doGetFile(UriRefParams params, WebPlotRequest request, boolean handleAllErrors) throws FailedRequestException, SecurityException {
        try {
            params.setCheckForNewer(request.getUrlCheckForNewer());
            params.setMaxSizeToDownload(VisContext.FITS_MAX_SIZE);
            var desc= request.getUserDesc();
            params.setDesc(desc); // set file description
            FileInfo fileInfo = LockingRetrieve.downloadWithCacheMsg(params);
            if (fileInfo.getResponseCode()>=400 && handleAllErrors) {
                throw new FailedRequestException(fileInfo.getResponseCodeMsg());
            }
            fileInfo.setDesc(desc);
            return fileInfo;
        } catch (Exception e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        }
    }

    private static URL processUrl(String urlStr) throws FailedRequestException {

        if (urlStr == null) throw new FailedRequestException("Could not find file", "request.getURL() returned null");
        if ((urlStr.toLowerCase().contains("irsa") || urlStr.toLowerCase().contains("ceres.ipac"))) { // this is a hack for IRSA images that have a plus in files names as ra+dec
            int plusIdx= urlStr.indexOf("+");
            if (plusIdx>-1 && plusIdx+1 < urlStr.length()) { // if there is a plus and is in the form of num+num such as 4+5
                try {
                    String before= urlStr.charAt(plusIdx-1)+"";
                    String after= urlStr.charAt(plusIdx+1)+"";
                    Integer.parseInt(before);
                    Integer.parseInt(after);
                    urlStr = urlStr.replaceAll("\\+", URLEncoder.encode("+", StandardCharsets.UTF_8));
                } catch (NumberFormatException ignore) {}
            }
        }
        try  {
            return new URI(urlStr).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FailedRequestException("Could not find file", "request.getURL() returned URISyntaxException", e);
        }
    }

    private static String makeProgressKey(WebPlotRequest request) {
        return !isEmpty(request.getProgressKey())
                ? request.getProgressKey()
                : request.getURL()!=null
                ? request.getURL()
                : ifNotNull(makeS3UriRef(request)).orElse("").get();
    }

    private static UriRef makeS3UriRef(WebPlotRequest request) {
        var region= request.getS3Region();
        var bucket= request.getS3Bucket();
        var key= request.getS3Key();
        var url= request.getURL();
        var s3Uri= request.getS3Uri();

        if (bucket!=null && key!=null) return UriRef.make(new S3Ref(region,bucket,key));
        if (UriRef.determineType(s3Uri)== S3Cloud) return UriRef.make(s3Uri);
        if (UriRef.determineType(url)== S3Cloud) return UriRef.make(url);
        return null;
    }

    private static UriRef makeGcsUriRef(WebPlotRequest request) {
        var bucket= request.getGcsBucket();
        var objName= request.getGcsObjName();
        if (bucket!=null && objName!=null) return UriRef.make(new GcsRef(null,bucket,objName,null));
        return null;
    }

}
