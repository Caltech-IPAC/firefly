package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.SolrQuery;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.AbstractSearch;
import edu.caltech.ipac.solrclient.QueryParams;

import java.util.Arrays;

/**
 * @author tatianag
 * $Id: QueryAbstractSearch.java,v 1.7 2010/08/04 20:18:51 roby Exp $
 */
@SearchProcessorImpl(id ="abstractSearch")
public class QueryAbstractSearch extends SolrQuery {

    

    protected QueryParams getQueryParams(TableServerRequest request) {
        AbstractSearch.Req req = QueryUtil.assureType(AbstractSearch.Req.class, request);
        QueryParams params = SolrQuery.makeDefQueryParams(req.getQueryString());
        params.setDoctype("program");
        params.setFieldBoostInfo(Arrays.asList("id^10", "progid^10", "progname^2.0", "progtitle^3.0", "pi^2.0", "sciencecat^0.5", "abstract^1.0"));
        params.setQueryFields(Arrays.asList("abstract", "progname", "progtitle", "pi", "sciencecat", "progid"));
        params.setHighlightFields(Arrays.asList("abstract", "progname", "progtitle", "pi", "sciencecat"));
//        params.setFragmentSize(200);
        return params;
    }
}