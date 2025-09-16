package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 10/2/25
 */


import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;

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

        Logger.warn("Unexpected use of GCS: GCS has not been implemented yet. Using URLDownload instead.");
        //todo implement GCS
        return URLDownload.getDataToFile(ref.toUrl(), outfile, cookies, requestHeaders, options);
    }



    public static boolean isRunningInAws() {return false; } // todo - implement
}
