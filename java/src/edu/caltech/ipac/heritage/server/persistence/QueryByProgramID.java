package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByProgramID;
import edu.caltech.ipac.util.CollectionUtil;

import java.util.regex.Pattern;

/**
 * @author tatianag
 * $Id: QueryByProgramID.java,v 1.11 2010/08/04 20:18:51 roby Exp $
 */
public class QueryByProgramID {

    public static Pattern DIGITS_PATTERN = Pattern.compile("^[0-9]+$");

    @SearchProcessorImpl(id ="aorByProgramID")
    public static class Aor extends AorQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByProgramID.Req req = QueryUtil.assureType(SearchByProgramID.Req.class, request);
            String program = req.getProgram().trim();
            String sql = "select " + CollectionUtil.toString(AOR_SUM_COLS) +
                    " from requestinformation ri, targetinformation ti where ti.reqkey = ri.reqkey and ";
            if (DIGITS_PATTERN.matcher(program).find()) {
                sql += "ri.progid= ?";
                return new SqlParams(sql, Integer.parseInt(program));
            } else {
                sql += "ri.progname = ?";
                return new SqlParams(sql, program.toUpperCase());
            }
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }

    @SearchProcessorImpl(id ="bcdByProgramID")
    public static class Bcd extends BcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByProgramID.Req req = QueryUtil.assureType(SearchByProgramID.Req.class, request);
            String sql = "select  * from bcdproducts where reqkey in (select reqkey from requestinformation where ";
            String program = req.getProgram().trim();
            if (DIGITS_PATTERN.matcher(program).find()) {
                sql += "progid = ?)";
                return new SqlParams(sql, Integer.parseInt(program));
            } else {
                sql += "progname = ?)";
                return new SqlParams(sql, program.toUpperCase());
            }
        }
    }

    @SearchProcessorImpl(id ="pbcdByProgramID")
    public static class Pbcd extends PbcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            SearchByProgramID.Req req = QueryUtil.assureType(SearchByProgramID.Req.class, request);
            String sql = "select * from postbcdproducts where reqkey in (select reqkey from requestinformation where ";
            String program = req.getProgram().trim();
            if (DIGITS_PATTERN.matcher(program).find()) {
                sql += "progid = ?)";
                return new SqlParams(sql, Integer.parseInt(program));
            } else {
                sql += "progname = ?)";
                return new SqlParams(sql, program.toUpperCase());
            }
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }
}
