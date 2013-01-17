package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByObserver;
import edu.caltech.ipac.util.CollectionUtil;

/**
 * @author tatianag
 * $Id: QueryByObserver.java,v 1.9 2010/08/04 20:18:51 roby Exp $
 */
public class QueryByObserver {

    @SearchProcessorImpl(id ="aorByObserver")
    public static class Aor extends AorQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByObserver.Req req = QueryUtil.assureType(SearchByObserver.Req.class, request);
            String sql = "select " + CollectionUtil.toString(AOR_SUM_COLS) +
                " from requestinformation ri, targetinformation ti where ri.pi like ?" +
                " and ti.reqkey = ri.reqkey";
            return new SqlParams(sql, req.getObserver());
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }

    @SearchProcessorImpl(id ="bcdByObserver")
    public static class Bcd extends BcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByObserver.Req req = QueryUtil.assureType(SearchByObserver.Req.class, request);
            String sql = "select  * from bcdproducts where reqkey in (select reqkey from requestinformation where pi like ?)";
            return new SqlParams(sql, req.getObserver());
        }
    }

    @SearchProcessorImpl(id ="pbcdByObserver")
    public static class Pbcd extends PbcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByObserver.Req req = QueryUtil.assureType(SearchByObserver.Req.class, request);
            String sql = "select * from postbcdproducts where reqkey in (select reqkey from requestinformation where pi like ?)";
            return new SqlParams(sql, req.getObserver());
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }
}
