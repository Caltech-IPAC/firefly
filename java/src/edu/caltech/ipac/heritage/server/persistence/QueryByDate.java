package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByDate;
import edu.caltech.ipac.util.CollectionUtil;

/**
 * @author tatianag
 * $Id: QueryByDate.java,v 1.11 2010/08/04 20:18:51 roby Exp $
 *
 */
public class QueryByDate {

    @SearchProcessorImpl(id ="aorByDate")
    public static class Aor extends AorQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByDate.Req req = QueryUtil.assureType(SearchByDate.Req.class, request);
            String sql = "select " + CollectionUtil.toString(AOR_SUM_COLS) +
                " from requestinformation ri, targetinformation ti where ri.reqendtime > ? and ri.reqbegintime < ?" +
                " and ti.reqkey = ri.reqkey";
            return new SqlParams(sql, QueryUtil.convertDate(req.getStartDate()), QueryUtil.convertDate(req.getEndDate()));
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }

    }

    @SearchProcessorImpl(id ="bcdByDate")
    public static class Bcd extends BcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByDate.Req req = QueryUtil.assureType(SearchByDate.Req.class, request);
            String sql = "select  * from bcdproducts where reqkey in (select reqkey from requestinformation where reqendtime > ? and reqbegintime < ?)";
            return new SqlParams(sql, QueryUtil.convertDate(req.getStartDate()), QueryUtil.convertDate(req.getEndDate()));
        }
    }

    @SearchProcessorImpl(id ="pbcdByDate")
    public static class Pbcd extends PbcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByDate.Req req = QueryUtil.assureType(SearchByDate.Req.class, request);
            String sql = "select * from postbcdproducts where reqkey in (select reqkey from requestinformation where reqendtime > ? and reqbegintime < ?)";
            return new SqlParams(sql, QueryUtil.convertDate(req.getStartDate()), QueryUtil.convertDate(req.getEndDate()));
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }

}
