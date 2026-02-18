package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 9/29/25
 * Time: 4:20 PM
 */


import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.util.StringUtils;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * @author Trey Roby
 *
 */
public record S3Ref(String region, String bucket, String key, String accessKey, String signature) {

    public S3Ref(String region, String bucket, String key) {this(region,bucket,key,null,null);}

    public S3Ref {
        Objects.requireNonNull(bucket, "bucket cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    private static final String AMAZON = "amazonaws.com";
    private static final String defRegion = "aws-global";
    private static final int amazonLen = AMAZON.length();
    private static final boolean ENABLE_SIGNED= false; // we don't support signed calls yet

    public String toString() {
        return String.format("%s - %s - %s", region, bucket, key);
    }

    public String toHostBasedUrl() {
        var r= region!=null ? region : defRegion;
        return String.format("https://%s.s3.%s.%s/%s", bucket, r, AMAZON, key);
    }
    public String toPathBasedUrl() {
        var r= region!=null ? region : defRegion;
        return String.format("https://s3.%s.%s/%s/%s", r, AMAZON, bucket, key);
    }
    public String toS3Based() {
        return String.format("s3://%s/%s", region, key);
    }

    public boolean hasCredentials() {
        return !StringUtils.isEmpty(accessKey) && !StringUtils.isEmpty(signature);
    }

    public static S3Ref makeFromUri(Object uri) {
        return switch (uri) {
            case S3Ref s3 -> s3;
            case URL url -> makeFromString(url.toString());
            case String s -> makeFromString(s);
            case null, default -> null;
        };
    }

    public static boolean isS3Ref(Object ref) {
        return makeFromUri(ref) != null;
    }

    private static S3Ref makeFromString(String s) {
        if (s.length() < 10) return null;
        if (s.toLowerCase().startsWith("s3://")) {
            String path = s.substring(5);
            int idx = path.indexOf("/");
            String key = path.substring(idx + 1);
            String bucket = path.substring(0, idx);
            return new S3Ref(null, bucket, key);
        } else if (s.toLowerCase().startsWith("https")) {
            URL url= Util.Try.it(() -> new URI(s).toURL()).getOrElse((URL)null);
            if (url == null) return null;
            var path = url.getPath();
            if (path.length() < 2) return null;
            var host = url.getHost().toLowerCase();
            if (!host.endsWith(AMAZON) && !host.contains("s3.")) return null;
            var cleanPath = path.substring(1);
            var workingHost = host.substring(0, host.length() - amazonLen - 1);

            S3Ref s3Ref=null;
            if (workingHost.startsWith("s3.")) { // s3 path style
                var region = workingHost.substring(3);
                var sAry = cleanPath.split("/",2);
                if (sAry.length != 2) return null;
                s3Ref= new S3Ref(region, sAry[0], sAry[1]);
            } else if (workingHost.contains(".s3.")){ // s3 host style
                var sAry = workingHost.split("\\.s3\\.");
                if (sAry.length != 2) return null;
                s3Ref= new S3Ref(sAry[1], sAry[0], cleanPath);
            }
            if (s3Ref==null) return null;

            if (isS3SignedURL(s)) {
                var params= URLDownload.getQueryParams(url);
                if (params.size()>=2) {
                    String sig= Util.Try.it(() -> params.get("Signature").getFirst()).getOrElse("");
                    if (sig.isEmpty()) {
                        sig= Util.Try.it(() -> params.get("X-Amz-Signature").getFirst()).getOrElse("");
                    }
                    String accessKey= Util.Try.it(() -> params.get("AWSAccessKeyId").getFirst()).getOrElse("");
                    if (accessKey.isEmpty()) {
                        accessKey= Util.Try.it(() -> params.get("X-Amz-Credential").getFirst()).getOrElse("");
                    }
                    if (!sig.isEmpty() && !accessKey.isEmpty()) {
                        s3Ref= new S3Ref( s3Ref.region, s3Ref.bucket, s3Ref.key, accessKey, sig);
                    }
                }
                if (!ENABLE_SIGNED) return null;
            }
            return s3Ref;
        }
        return null;
    }


    public static boolean isS3SignedURL(String s) {
        if (s==null) return false;
        if (!s.toLowerCase().startsWith("https")) return false;
        URL url = Util.Try.it(() -> new URI(s).toURL()).getOrElse((URL) null);
        if (url == null) return false;
        var path = url.getPath();
        if (path.length() < 2) return false;
        if (StringUtils.isEmpty(url.getQuery())) return false;
        var host = url.getHost().toLowerCase();
        if (!host.endsWith(AMAZON) && !host.contains("s3.")) return false;
        var q= url.getQuery();
        if (StringUtils.isEmpty(q)) return false;
        return (q.contains("Signature") || q.contains("X-Amz-Signature")) &&
               (q.contains("AWSAccessKeyId") || q.contains("X-Amz-Credential"));
    }
}
