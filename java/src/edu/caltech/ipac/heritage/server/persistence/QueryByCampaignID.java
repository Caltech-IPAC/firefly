package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByCampaignID;
import edu.caltech.ipac.util.CollectionUtil;

import java.util.regex.Pattern;

/**
 * @author tatianag
 * $Id: QueryByCampaignID.java,v 1.11 2010/08/04 20:18:51 roby Exp $
 */
public class QueryByCampaignID {

    public static Pattern DIGIT_START_PATTERN = Pattern.compile("^[0-9]");


    @SearchProcessorImpl(id ="aorByCampaignID")
    public static class Aor extends AorQuery {

        private static final String BASE_SQL = "select " + CollectionUtil.toString(AOR_SUM_COLS) +
                " from requestinformation ri, targetinformation ti where ti.reqkey = ri.reqkey";
        private static final String BASE_SQL_CAMPNAME = BASE_SQL +
                " and ri.campid in (select campid from campaigninformation where campname = ?)";
        private static final String BASE_SQL_CAMPID = BASE_SQL + " and ri.campid= ?";

        protected SqlParams makeSqlParams(TableServerRequest request) {

            SearchByCampaignID.Req req = QueryUtil.assureType(SearchByCampaignID.Req.class, request);
            String campaign = req.getCampaign().trim();
            if (DIGIT_START_PATTERN.matcher(campaign).find()) {
                return new SqlParams(BASE_SQL_CAMPID, Integer.parseInt(campaign));
            } else {
                return new SqlParams(BASE_SQL_CAMPNAME,  campaign);
            }
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }

    }

    @SearchProcessorImpl(id ="bcdByCampaignID")
    public static class Bcd extends BcdQuery {

        private static final String BASE_SQL = "select  * from bcdproducts where campid";
        private static final String BASE_SQL_CAMPNAME = BASE_SQL + " in (select campid from campaigninformation where campname = ?)";
        private static final String BASE_SQL_CAMPID = BASE_SQL + "= ?";


        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByCampaignID.Req req = QueryUtil.assureType(SearchByCampaignID.Req.class, request);
            String campaign = req.getCampaign();
            if (DIGIT_START_PATTERN.matcher(campaign).find()) {
                return new SqlParams(BASE_SQL_CAMPID, Integer.parseInt(campaign));
            } else {
                return new SqlParams(BASE_SQL_CAMPNAME, campaign);
            }
        }

    }

    @SearchProcessorImpl(id ="pbcdByCampaignID")
    public static class Pbcd extends PbcdQuery {

        private static final String BASE_SQL = "select * from postbcdproducts where campid";
        private static final String BASE_SQL_CAMPNAME = BASE_SQL + " in (select campid from campaigninformation where campname = ?)";
        private static final String BASE_SQL_CAMPID = BASE_SQL + "= ?";

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByCampaignID.Req req = QueryUtil.assureType(SearchByCampaignID.Req.class, request);
            String campaign = req.getCampaign();
            if (DIGIT_START_PATTERN.matcher(campaign).find()) {
                return new SqlParams(BASE_SQL_CAMPID, Integer.parseInt(campaign));
            } else {
                return new SqlParams(BASE_SQL_CAMPNAME, campaign);
            }
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }
}
