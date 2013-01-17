package edu.caltech.ipac.hydra.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author trey
 *         $Id: TwoMassFileRetrieve.java,v 1.4 2011/04/29 00:39:53 wmi Exp $
 */
@SearchProcessorImpl(id ="TwoMassFileRetrieve", params=
        {@ParamDoc(name="download", desc="the url taken from the 2MASS results table"),
         @ParamDoc(name="band", desc="h,j,k that will be substituted into the URL")
                    })
public class TwoMassFileRetrieve extends URLFileInfoProcessor {

    public URL getURL(ServerRequest sr) throws MalformedURLException {
        String baseurl= sr.getSafeParam("downloaddownload");
        String band= sr.getSafeParam("bandband");
        baseurl= baseurl.replaceAll("name=.", "name="+band);
        return new URL(baseurl);
    }
}