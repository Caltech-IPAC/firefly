/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

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


    public static class StatsLogResolver implements QueryDescResolver {

        public String getTitle(ServerRequest req) {
            return req.getRequestId();
        }

        public String getDesc(ServerRequest req) {
            TableServerRequest treq = (TableServerRequest) req;
            // limit each params to no more than 50 chars
            List<String> params = treq.getSearchParams().stream()
                                    .map((p) -> String.valueOf(p).length() > 50 ? String.valueOf(p).substring(0,50) + " <truncated>" : String.valueOf(p))
                                    .collect(Collectors.toList());

            return StringUtils.toString(params, "; ");
        }
    }

}
