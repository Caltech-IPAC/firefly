package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 9/29/25
 * Time: 4:20 PM
 */


import edu.caltech.ipac.firefly.core.Util;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * @author Trey Roby
 *
 */
public record GcsRef(String projectId, String bucket, String objName, URL sourceUrl) {

    private static final String GOOGLE = "storage.googleapis.com";

    public GcsRef {
        Objects.requireNonNull(bucket, "bucket cannot be null");
        Objects.requireNonNull(objName, "objName cannot be null");
    }


    public String toString() {
        return String.format("%s - %s - %s", projectId, bucket, objName);
    }

    public String toUrlStr() {
        return String.format("https://%s/%s/%s", GOOGLE, bucket,objName);
    }

    public URL toUrl() {
        return sourceUrl!=null
                ? sourceUrl
                : Util.Try.it(() -> new URI(toUrlStr()).toURL()).getOrElse((URL)null);
    }

    public static GcsRef makeFromUri(Object uri) {
        return switch (uri) {
            case GcsRef gcsRef -> gcsRef;
            case URL url -> makeFromString(url.toString());
            case String s -> makeFromString(s);
            case null, default -> null;
        };
    }

    public static boolean isGcsRef(Object ref) { return makeFromUri(ref) != null; }

    private static GcsRef makeFromString(String s) {
        if (s.length() < 10) return null;
        if (s.toLowerCase().startsWith("http") && s.toLowerCase().contains(GOOGLE)) {
            URL url= Util.Try.it(() -> new URI(s).toURL()).getOrElse((URL)null);
            if (url == null) return null;
            var path = url.getPath();
            if (path.length() < 2) return null;
            var cleanPath = path.substring(1);
            if (cleanPath.length() < 2) return null;
            var host = url.getHost().toLowerCase();
            if (GOOGLE.equals(host)) {
                var sAry = cleanPath.split("/",2);
                if (sAry.length != 2) return null;
                return new GcsRef(null, sAry[0], sAry[1], url);
            }
            else if (host.endsWith(GOOGLE)) {
                var sAry = cleanPath.split("\\.",2);
                if (sAry.length != 2) return null;
                return new GcsRef(null, sAry[0], cleanPath, url);
            }
            return null;
        }
        return null;
    }
}
