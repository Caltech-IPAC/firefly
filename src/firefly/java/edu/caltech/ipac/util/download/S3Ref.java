package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 9/29/25
 * Time: 4:20 PM
 */


import edu.caltech.ipac.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 * @author Trey Roby
 *
 */
public record S3Ref(String region, String bucket, String key) {

    public S3Ref {
        Objects.requireNonNull(bucket, "bucket cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    private static final String AMAZON = "amazonaws.com";
    private static final String defRegion = "aws-global";
    private static final int amazonLen = AMAZON.length();

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
            try {
                URL url = new URI(s).toURL();
                var path = url.getPath();
                if (path.length() < 2) return null;
                if (!StringUtils.isEmpty(url.getQuery())) return null;
                var host = url.getHost().toLowerCase();
                if (!host.endsWith(AMAZON) || !host.contains("s3.")) return null;
                var cleanPath = path.substring(1);
                var workingHost = host.substring(0, host.length() - amazonLen - 1);

                if (workingHost.startsWith("s3.")) { // s3 path style
                    var region = workingHost.substring(3);
                    var sAry = cleanPath.split("/");
                    if (sAry.length != 2) return null;
                    return new S3Ref(region, sAry[0], sAry[1]);
                } else { // s3 host style
                    var sAry = workingHost.split("\\.s3\\.");
                    if (sAry.length != 2) return null;
                    return new S3Ref(sAry[1], sAry[0], cleanPath);
                }
            } catch (MalformedURLException | URISyntaxException e) {
                return null;
            }
        }
        return null;
    }
}
