package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.URLDownload.Options;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.server.network.HttpServices.BUFFER_SIZE;

public class S3Download {

    /*
     * To work with this file, IntelliJ needs to know about the following jars:
     *    s3-2.x.x.jar
     *    s3-transfer-manager-2.x.x.jar
     *    http-auth-spi-2.x.x.jar
     *    auth-2.x.x.jar
     *    aws-core-2.x.x.jar
     *    regions-2.x.x.jar
     *    sdk-core-2.x.x.jar
     *    utils-2.x.x.jar
     */

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final Region awsRegion= getAwsDefaultRegion();



    public static FileInfo getPublicData(S3Ref ref, File outfile, Options options) throws FailedRequestException {
       return getData(ref,outfile,S3Download::applyAnonymouse,options) ;
    }

    public static FileInfo getData(S3Ref ref,
                                   File outfile,
                                   Map<String, String> cookies,
                                   Map<String, String> requestHeaders,
                                   Options options) throws FailedRequestException {

                 //todo eventually we might get credentials from the cookies
        return getData(ref,outfile,S3Download::applyAnonymouse,options) ;
    }

    public static FileInfo getData(S3Ref ref,
                                   File outfile,
                                   ApplyCredentials applyCredentials,
                                   Options options) throws FailedRequestException {

        StopWatch.Tracker tracker = new StopWatch.Tracker("S3Download", null);
        tracker.starts();
        try (S3AsyncClient client = getS3AsyncClient(ref, applyCredentials);
             S3TransferManager transferManager = S3TransferManager.builder().s3Client(client).build()) {


            // --- first do a HEAD call
            S3HeaderInfo header = getHeader(ref, applyCredentials, options, outfile);
            String extName = URLDownload.getSuggestedFileName(header.contentDisposition());
            var length = header.contentLength();
            var contentType = header.contentType();
            var code = header.statusCode();
            if (code != HttpURLConnection.HTTP_OK) {
                if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    logNotModified(outfile, ref);
                    return new FileInfo(outfile, extName, code, ResponseMessage.getHttpResponseMessage(code),
                            header.contentType());
                } else {
                    tracker.stops();
                    double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
                    logFail(null, outfile, ref, code, seconds);
                    return new FileInfo(header.statusCode);
                }
            }

            // --- if success, then continue to the full download
            DownloadFileRequest dFileReq = DownloadFileRequest.builder()
                    .destination(outfile.toPath())
                    .getObjectRequest(req -> makeReq(req, ref, outfile, options))
                    .build();
            FileDownload download = transferManager.downloadFile(dFileReq);
            DownloadListener dl = options.dl();
            for (int i = 0; !download.completionFuture().isDone(); i++) {
                if (dl != null && i % 3 == 0 && length > 0) {
                    callListener(dl, download.progress().snapshot().transferredBytes(), length, false);
                }
                Thread.sleep(250);
            }
            if (length > 0 && dl!=null) callListener(dl, length, length, false);
            tracker.stops();
            if (!isSuccess(download)) {
                double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
                logFail(null, outfile, ref, code, seconds);
                return new FileInfo(getStatusCode(download));
            }

            CompletedFileDownload completedDownload = download.completionFuture().join();

            double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
            logSuccess(outfile, ref, completedDownload.response(), seconds);
            return new FileInfo(outfile, extName, 200, ResponseMessage.getHttpResponseMessage(200), contentType);
        } catch (S3Exception | InterruptedException e) {
            tracker.stops();
            double seconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
            var code= e instanceof S3Exception ? ((S3Exception )e).statusCode() : 0;
            logFail(e, outfile, ref, code, seconds);
            throw new FailedRequestException(e.getMessage(),e);
        }
    }


    private static boolean isSuccess(FileDownload download) {
        var response= download.progress().snapshot().sdkResponse();
        return response.map(sdkResponse -> sdkResponse.sdkHttpResponse().isSuccessful()).orElse(false);
    }

    private static int getStatusCode(FileDownload download) {
        var response= download.progress().snapshot().sdkResponse();
        return response.map(sdkResponse -> sdkResponse.sdkHttpResponse().statusCode()).orElse(0);
    }

    private static String getHeader(FileDownload download, String key) {
        var response= download.progress().snapshot().sdkResponse();
        var headers= response.map(sdkResponse -> sdkResponse.sdkHttpResponse().headers()).orElse(Collections.emptyMap());
        return getHeader(headers, key);
    }

    private static String getHeader(Map<String, List<String>> headers, String key) {
        return headers.containsKey(key) ? headers.get(key).getFirst() : null;
    }

    private static void makeReq(GetObjectRequest.Builder rBuild, S3Ref ref, File outfile, Options options) {
        rBuild= rBuild.bucket(ref.bucket()).key(ref.key());
        if (outfile!=null && outfile.canRead() && options.onlyIfModified()) {
            rBuild.ifModifiedSince( Instant.ofEpochMilli(outfile.lastModified()));
        }
    }

    private static void callListener(DownloadListener dl, long transferredBytes, long length, boolean complete ) {
        String msg;
        if (length==0) {
            msg= FileUtil.getSizeAsString(transferredBytes);
        }
        else {
            msg= (complete || transferredBytes==length)
                    ? FileUtil.getSizeAsString(length)
                    : String.format("%s of %s", FileUtil.getSizeAsString(transferredBytes), FileUtil.getSizeAsString(length));
        }
        var ev = new DownloadEvent(S3Download.class, complete? length : transferredBytes, length, 0, 0, "", "", msg);
        dl.dataDownloading(ev);
    }

    public static void logFail(Exception e, File outfile, S3Ref ref, int statusCode, double seconds) {
        if (e!=null) _log.error(e);
        String send= String.format( "S3 Download Failed (%d, %s, %.1f sec): %s\n",
                statusCode, ResponseMessage.getHttpResponseMessage(statusCode), seconds, ref);
        String file= "        File: "+ outfile.toPath();
        _log.info(send+file);

    }

    public static void logNotModified(File outfile, S3Ref ref) {
        String send= String.format( "S3 Download (Not Modified): %s\n", ref );
        String file= "        File: "+ outfile.toPath();
        _log.info(send+file);
    }

    public static void logSuccess(File outfile, S3Ref ref, GetObjectResponse r,  double seconds) {
        String formatedSize= FileUtil.getSizeAsString(r.contentLength());
        String send= String.format( "S3 Download (%.1f sec, %s): %s\n", seconds, formatedSize, ref );
        String stat= String.format(
                "        length: %d, contentType: %s, encoding: %s, disposition %s\n",
                r.contentLength(), r.contentType(), r.contentEncoding(), r.contentDisposition() );
        String file= "        File: "+ outfile.toPath();
        _log.info(send+stat+file);
    }



    /** keep for reference */
    private static FileInfo NOT_USED_getPublicDataNonThreaded(S3Ref ref, File outfile, Options options) throws FailedRequestException {
        return NOT_USED_getPublicDataNonThreaded(ref,outfile,S3Download::applyAnonymouse,options);
    }


    /** keep for reference */
    private static FileInfo NOT_USED_getPublicDataNonThreaded( S3Ref ref,
                                                              File outfile,
                                                              ApplyCredentials applyCredentials,
                                                              Options options) throws FailedRequestException {
        try (S3Client client= getS3Client(ref,applyCredentials)) {
            GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                    .bucket(ref.bucket())
                    .key(ref.key());
            if (outfile.canRead() && options.onlyIfModified()) {
                getObjectRequestBuilder= getObjectRequestBuilder.ifModifiedSince( Instant.ofEpochMilli(outfile.lastModified()));
            }
            GetObjectRequest getObjectRequest= getObjectRequestBuilder.build();
            String extName= null;
            try (ResponseInputStream<GetObjectResponse> s3Object = client.getObject(getObjectRequest)) {
                var hInfo= getResponseInfo(s3Object.response());
                extName= URLDownload.getSuggestedFileName(hInfo.contentDisposition());
                OutputStream out= new BufferedOutputStream(new FileOutputStream(outfile), BUFFER_SIZE);
                URLDownload.netCopy(new DataInputStream(s3Object),out,hInfo.contentLength,0, options.dl());
                return new FileInfo(outfile, extName, 200, ResponseMessage.getHttpResponseMessage(200), hInfo.contentType());
            } catch (S3Exception e) {
                int status= e.statusCode();
                String msg= ResponseMessage.getHttpResponseMessage(status);
                Logger.error(msg);
                return new FileInfo(outfile, extName, status, msg);
            } catch (Exception e) {
                throw ResponseMessage.simplifyNetworkCallException(e);
            }
        }
    }


    public static S3HeaderInfo getHeader(S3Ref ref, ApplyCredentials applyCredentials, Options options, File outfile) throws FailedRequestException {
        try (S3Client client= getS3Client(ref,applyCredentials)) {
            HeadObjectRequest.Builder rBuild= HeadObjectRequest.builder().bucket(ref.bucket()).key(ref.key());

            if (outfile!=null && outfile.canRead() && options.onlyIfModified()) {
                rBuild.ifModifiedSince( Instant.ofEpochMilli(outfile.lastModified()));
            }
            HeadObjectRequest headObjectRequest= rBuild.build();
            var h= client.headObject(headObjectRequest);
            return new S3HeaderInfo(
                    h.sdkHttpResponse().statusCode(),
                    h.contentType(), h.contentLength(), h.contentDisposition(),
                    h.contentEncoding(), h.metadata()
            );
        } catch (S3Exception e) {
            return new S3HeaderInfo(e.statusCode(), null, 0, null, null, null);
        }
    }


    public static S3HeaderInfo getPublicHeader(S3Ref ref, Options options, File outfile) throws FailedRequestException {
        return getHeader(ref, S3Download::applyAnonymouse, options, outfile);
    }
    public static S3Client getS3Client(S3Ref ref) throws FailedRequestException {
        return getS3Client(ref, S3Download::applyAnonymouse);
    }

    public static S3Client getS3Client(S3Ref ref, ApplyCredentials applyCredentials) throws FailedRequestException {
        if (ref.bucket()==null || ref.key()==null) throw new FailedRequestException("Could not find file", "some s3 parameters are null");
        var regionToUse= ref.region()!=null ? Region.of(ref.region()) : Region.AWS_GLOBAL;;
        if (regionToUse==null) throw new FailedRequestException("unrecognized region:" +ref.region());

        var builder= S3Client.builder().region(regionToUse);
        builder= (S3ClientBuilder) applyCredentials.apply(builder);
        return builder.build();
    }

    public static S3AsyncClient getS3AsyncClient(S3Ref ref, ApplyCredentials applyCredentials) throws FailedRequestException {
        if (ref.bucket()==null || ref.key()==null) throw new FailedRequestException("Could not find file", "some s3 parameters are null");
        var regionToUse= ref.region()!=null ? Region.of(ref.region()) : Region.AWS_GLOBAL;;
        if (regionToUse==null) throw new FailedRequestException("unrecognized region:" +ref.region());
        var builder= S3AsyncClient.builder().region(regionToUse);
        builder= (S3AsyncClientBuilder)applyCredentials.apply(builder);
        return builder.build();
    }

    public static AwsClientBuilder<?,?> applyAnonymouse(AwsClientBuilder<?,?> builder) {
        return builder.credentialsProvider(AnonymousCredentialsProvider.create());
    }


    public static S3HeaderInfo getResponseInfo(GetObjectResponse response) {
        return new S3HeaderInfo(
                response.sdkHttpResponse().statusCode(),
                response.contentType(), response.contentLength(), response.contentDisposition(),
                response.contentEncoding(), response.metadata()
        );
    }
    public record S3HeaderInfo(
            int statusCode, String contentType, long contentLength, String contentDisposition,
                                String contentEncoding, Map<String, String> metadata ) {}


    public interface ApplyCredentials { AwsClientBuilder<?,?> apply(AwsClientBuilder<?,?> builder); }

    public static boolean isRunningInAws() {return awsRegion!=null;}

    /**
     * get the Aws region if running in aws, otherwise return null
     * This function should just be called once. It appears to be slow
     * @return the region or null
     */
    public static Region getAwsDefaultRegion() {
          // other ways to get region: keep next two lines for reference
          // var r= System.getenv("AWS_REGION");
          // var r= System.getProperty("aws.region");
        try {
            return new DefaultAwsRegionProviderChain().getRegion();
        } catch (SdkClientException ignore) {
            return null;
        }
    }
}

