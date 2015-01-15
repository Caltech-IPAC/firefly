/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;

/**
 * Date: July 21, 2014
 *
 * @author loi
 * @version $Id: SearchDescResolver.java,v 1.3 2011/09/28 02:18:47 loi Exp $
 */
public interface QueryDescResolver {

    public String getTitle(ServerRequest req);

    public String getDesc(ServerRequest req);


//====================================================================
//      QueryDescResolver based on SearchDescResolver
//====================================================================
    public static class DescBySearchResolver implements QueryDescResolver {
        private SearchDescResolver sdr;

        public DescBySearchResolver(SearchDescResolver sdr) {
            this.sdr = sdr;
        }

        public String getTitle(ServerRequest req) {
            return sdr.getTitle(convertToRequest(req));
        }

        public String getDesc(ServerRequest req) {
            return sdr.getDesc(convertToRequest(req));
        }

        Request convertToRequest(ServerRequest request) {
            Request req = new Request();
            req.copyFrom(request);
            return req;
        }

    }


}
