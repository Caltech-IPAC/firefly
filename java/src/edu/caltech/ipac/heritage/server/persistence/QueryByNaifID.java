package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByNaifID;
import edu.caltech.ipac.util.CollectionUtil;

/**
 * @author tatianag $Id: QueryByNaifID.java,v 1.13 2011/04/26 21:57:16 tatianag Exp $
 */
public class QueryByNaifID {

    static String makeIdString(SearchByNaifID.Req request) {
        int [] naifIDs = request.getNaifIDs();
        return CollectionUtil.toString(CollectionUtil.asList(naifIDs), ", ");
    }


    @SearchProcessorImpl(id ="aorByNaifID")
    public static class Aor extends AorQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {

            SearchByNaifID.Req req = QueryUtil.assureType(SearchByNaifID.Req.class, request);
            String sqlSelect = "select " + CollectionUtil.toString(AOR_SUM_COLS) +
                    " from requestinformation ri, targetinformation ti where ti.naifid in ("+makeIdString(req)+")" +
                    " and ti.reqkey = ri.reqkey";

            return new SqlParams(sqlSelect);
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }

    }

    @SearchProcessorImpl(id ="bcdByNaifID")
    public static class Bcd extends BcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {

            SearchByNaifID.Req req = QueryUtil.assureType(SearchByNaifID.Req.class, request);

            String sqlSelect = "select  * from bcdproducts where reqkey in (select reqkey from targetinformation where naifid in ("+makeIdString(req)+"))";

            return new SqlParams(sqlSelect);
        }
    }

    @SearchProcessorImpl(id ="pbcdByNaifID")
    public static class Pbcd extends PbcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByNaifID.Req req = QueryUtil.assureType(SearchByNaifID.Req.class, request);

            String sqlSelect = "select  * from postbcdproducts where reqkey in (select reqkey from targetinformation where naifid in ("+makeIdString(req)+"))";

            return new SqlParams(sqlSelect);
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }
}
