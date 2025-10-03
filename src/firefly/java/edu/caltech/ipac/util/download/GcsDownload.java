package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 10/2/25
 */


import edu.caltech.ipac.firefly.data.FileInfo;

import java.io.File;
import java.util.Map;

/**
 * @author Trey Roby
 *
 */
public class GcsDownload {

    public static FileInfo getData(GcsRef ref,
                                   File outfile,
                                   Map<String, String> cookies,
                                   Map<String, String> requestHeaders,
                                   URLDownload.Options options) throws FailedRequestException {

        //todo implement GCS, for now use ConcurrentDownload
        return ConcurrentDownload.getData(ref.toUrl(), outfile, cookies, requestHeaders, options);
    }



    public static boolean isRunningInGcs() {return false; } // todo - implement
}
