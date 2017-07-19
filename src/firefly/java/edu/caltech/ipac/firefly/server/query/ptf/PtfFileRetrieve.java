/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.ptf;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: wmi
 */


@SearchProcessorImpl(id = "PtfFileRetrieve")
public class PtfFileRetrieve extends URLFileInfoProcessor {
    static final String IBE_HOST = AppProperties.getProperty("ptf.ibe.host", "https://irsa.ipac.caltech.edu/ibe");

    public URL getURL(ServerRequest sr) throws MalformedURLException {
        String productLevel = sr.getSafeParam("ProductLevel");

        if (productLevel.equalsIgnoreCase("l2")) {
            PtfRefimsFileRetrieve l2FileRetrieve = new PtfRefimsFileRetrieve();

            return l2FileRetrieve.getURL(sr);
        } else if (productLevel.equalsIgnoreCase("l1")) {
            PtfProcimsFileRetrieve l1FileRetrieve = new PtfProcimsFileRetrieve();

            return l1FileRetrieve.getURL(sr);
        } else {
            Logger.warn("cannot find param: productLevel or the param returns null");
            throw new MalformedURLException("Can not find the file");
        }

    }
    public String getBaseURL(ServerRequest sr) throws MalformedURLException {
        String productLevel = sr.getSafeParam("ProductLevel");

        if (productLevel.equalsIgnoreCase("l2")) {
           return PtfRefimsFileRetrieve.getBaseURL(sr);
        } else if (productLevel.equalsIgnoreCase("l1")) {
            return PtfProcimsFileRetrieve.getBaseURL(sr);
        } else {
            Logger.warn("cannot find param: productLevel or the param returns null");
            throw new MalformedURLException("Can not find the file");
        }

    }
    @Override
    protected boolean identityAware() {
        return true;
    }
}
