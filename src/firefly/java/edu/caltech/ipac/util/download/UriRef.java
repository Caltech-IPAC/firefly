package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.core.Util;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

import static edu.caltech.ipac.util.download.UriRef.ResourceType.GcsCloud;
import static edu.caltech.ipac.util.download.UriRef.ResourceType.OnPrimUrl;
import static edu.caltech.ipac.util.download.UriRef.ResourceType.S3Cloud;

/**
 * @author Trey Roby
 *
 */
public record UriRef(Object ref, Object sourceForRef) {
    public enum CloudEnvironment {AWS, GCS, ON_PRIM}
    public enum ResourceType {OnPrimUrl, S3Cloud, GcsCloud}

    public UriRef {
        Objects.requireNonNull(ref, "ref cannot be null");
        Objects.requireNonNull(sourceForRef, "sourceForRef cannot be null");
    }

    public boolean equals(UriRef uri) {
        return Objects.equals(ref, uri.ref);
        // note - we don't care about sourceForRef for equality since it is really just documentation
    }

    public String toString() {
        return getType() + ": " + ref.toString();
    }


    public ResourceType getType() {return determineType(ref);}

    public URL getURL() {
        return (ref instanceof URL url) ? url : null;
    }
    public S3Ref getS3Ref() { return (ref instanceof S3Ref s3Ref) ? s3Ref : null; }
    public GcsRef getGcsRef() { return (ref instanceof GcsRef gcsRef) ? gcsRef : null; }


    static public UriRef make(Object uriObj) {
        if (uriObj == null) return null;
        return switch (uriObj) {
            case UriRef uriRef -> uriRef;
            case S3Ref s3Ref -> makeS3(s3Ref);
            case GcsRef gcsRef -> makeGcs(gcsRef);
            case String s -> makeString(s);
            case URL url -> makeFromUrl(url);
            default -> null;
        };
    }

    public static UriRef.ResourceType determineType(Object obj)  {
        if (obj == null) return null;
        if (S3Ref.isS3Ref(obj)) return S3Cloud;
        else if (GcsRef.isGcsRef(obj)) return GcsCloud;
        else if (obj instanceof URL) return OnPrimUrl;
        else if (obj instanceof String s) return makeURLFromStr(s)!=null ? OnPrimUrl : null;
        else return null;

    }

    public static boolean isValid(Object obj) { return determineType(obj)!=null; }

    static private UriRef makeS3(S3Ref s3Ref) { return (s3Ref !=null) ? new UriRef(s3Ref, s3Ref): null; }
    static private UriRef makeGcs(GcsRef gcsRef) { return (gcsRef !=null) ? new UriRef(gcsRef, gcsRef): null; }

    static private UriRef makeFromUrl(URL url) {
        if (url == null) return null;
        var ref= S3Ref.isS3Ref(url)
                ? S3Ref.makeFromUri(url)
                : GcsRef.isGcsRef(url)
                ? GcsRef.makeFromUri(url)
                : url;
        return new UriRef(ref,url);
    }

    static private UriRef makeString(String uriStr) {
        if (uriStr == null) return null;
        uriStr= uriStr.trim();
        var resourceType= determineType(uriStr);
        if (resourceType == null) return null;
        return switch (resourceType) {
            case OnPrimUrl -> new UriRef(makeURLFromStr(uriStr), uriStr);
            case S3Cloud -> new UriRef(S3Ref.makeFromUri(uriStr), uriStr);
            case GcsCloud -> new UriRef(GcsRef.makeFromUri(uriStr), uriStr);
        };
    }

    private static URL makeURLFromStr(String urlStr) {
        if (urlStr == null) return null;
        return Util.Try.it(() -> new URI(urlStr.trim()).toURL()).getOrElse((URL)null);
    }

}
