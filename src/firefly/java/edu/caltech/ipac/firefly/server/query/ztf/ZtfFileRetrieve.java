package edu.caltech.ipac.firefly.server.query.ztf;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by wmi
 * on 8/16/17
 * edu.caltech.ipac.hydra.server.query
 */


@SearchProcessorImpl(id = "ZtfFileRetrieve")
public class ZtfFileRetrieve extends URLFileInfoProcessor {

    public static final boolean USE_HTTP_AUTHENTICATOR = false;
    public static final String ZTF_DATA_RETRIEVAL_TYPE = AppProperties.getProperty("ztf.data_retrieval_type");  // url or filesystem
    public static final String ZTF_FILESYSTEM_BASEPATH = AppProperties.getProperty("ztf.filesystem_basepath");


    public URL getURL(ServerRequest sr) throws MalformedURLException {
        String basePath = ZTF_FILESYSTEM_BASEPATH;
        String productLevel = sr.getSafeParam("ProductLevel");

        if (productLevel.equalsIgnoreCase("ref")) {
            ZtfRefimsFileRetrieve l2FileRetrieve = new ZtfRefimsFileRetrieve();

            return l2FileRetrieve.getURL(sr);
        } else if (productLevel.equalsIgnoreCase("sci")) {
            ZtfSciimsFileRetrieve l1FileRetrieve = new ZtfSciimsFileRetrieve();

            return l1FileRetrieve.getURL(sr);
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


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
