package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.persistence.TempTable;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByRequestID;
import edu.caltech.ipac.util.CollectionUtil;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public class QueryByRequestID {

    public static String TEMP_TABLE = "temp_reqkeys";
    public static String TEMP_ID_COL = "reqkey";

    static String makeIdString(TableServerRequest request) {
        return CollectionUtil.toString(getIds(request), ", ");
    }

    static Collection<Integer> getIds(TableServerRequest request) {
        SearchByRequestID.Req req = QueryUtil.assureType(SearchByRequestID.Req.class, request);
        return CollectionUtil.asList(req.getReqIDs());
    }

    static boolean includeByConstraints(TableServerRequest request) {
        SearchByRequestID.Req req = QueryUtil.assureType(SearchByRequestID.Req.class, request);
        return req.includeSameConstraints();
    }

    static HeritageQuery.SqlParams getAorQueryParams(Collection<Integer> ids) {
        return getAorQueryParams(ids, false);
    }


    static HeritageQuery.SqlParams getAorQueryParams(Collection<Integer> ids, boolean includeByConstraints) {
        String sql= "select " + CollectionUtil.toString(AorQuery.AOR_SUM_COLS);
        if (TempTable.useTempTable(ids)) {
            if (includeByConstraints) {
                sql += " from requestinformation ri, targetinformation ti " +
                    " where  ri.reqkey = ti.reqkey" +
                        " and ri.reqkey in (select "+ TEMP_ID_COL+" from "+TEMP_TABLE+" union" +
                        " select distinct d.reqkey from constraintinformation d, constraintinformation c, "+TEMP_TABLE+" t where t."+TEMP_ID_COL+"=c.reqkey and d.constrid=c.constrid)";
            } else {
                sql += " from requestinformation ri, targetinformation ti, " + TEMP_TABLE + " t" +
                    " where ri.reqkey = t."+ TEMP_ID_COL+" and ri.reqkey = ti.reqkey";
            }
        } else {
            if (includeByConstraints) {
                sql += " from requestinformation ri, targetinformation ti " +
                        " where ri.reqkey = ti.reqkey" +
                        " and ri.reqkey in (select distinct d.reqkey from constraintinformation d, constraintinformation c where d.constrid=c.constrid and c.reqkey in (" + CollectionUtil.toString(ids) + ")"+
                        " union select reqkey from requestinformation where reqkey in (" + CollectionUtil.toString(ids) + "))";

            } else {
                sql += " from requestinformation ri, targetinformation ti " +
                        " where ri.reqkey = ti.reqkey and ri.reqkey in" +
                        " (" + CollectionUtil.toString(ids) + ")";
            }
        }
        return new HeritageQuery.SqlParams(sql);
    }

    public static void loadTempTable(DataSource ds, Collection<Integer> ids) throws DataAccessException {
        try {
            TempTable.loadIdsIntoTempTable(ds.getConnection(), ids, TEMP_TABLE, TEMP_ID_COL);
        } catch (Exception e) {
            Logger.error(e);
            throw new DataAccessException("Unable to load "+ids.size()+" ids into temp table.");
        }
    }
//====================================================================
//
//====================================================================

    @SearchProcessorImpl(id ="aorByRequestID")
    public static class Aor extends AorQuery {

        private Collection<Integer> ids;

        @Override
        protected boolean onBeforeQuery(TableServerRequest request, DataSource datasource) throws IOException, DataAccessException {
            if (!super.onBeforeQuery(request, datasource)) { return false; }

            ids = getIds(request);
            if (TempTable.useTempTable(ids)) {
                loadTempTable(datasource, ids);
            }
            return true;
        }


        protected SqlParams makeSqlParams(TableServerRequest request) {
            return getAorQueryParams(getIds(request), includeByConstraints(request));
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }

// todo: using temp table when the 'IN CLAUS' is greater than n... remove this for now.
//        private JdbcTemplate _jdbcTemplate;
//        private Set<Integer> _requestIDs;
//
//        @Override
//        public void setDataRequest(DataRequest request) {
//            super.setDataRequest(request);
//            SearchParameters.ByRequestID reqIDsParam = (SearchParameters.ByRequestID)_request.getParams();
//            if (reqIDsParam == null) {
//                throw new IllegalArgumentException(request.getRequestId()+" : unable to find request id parameter");
//            }
//            _requestIDs = new HashSet<Integer>(CollectionUtil.asList(reqIDsParam.getRequestIDs()));
//
//           _jdbcTemplate = null;
//            if (_requestIDs.size() == 0) {
//                throw new IllegalArgumentException(request.getRequestId()+" query: req IDs may not be null");
//            }
//            if (_requestIDs.size() > 500) {
//                _jdbcTemplate = JdbcFactory.getStatefulTemplate(DbInstance.archive);
//            }
//        }
//
//
//        @Override
//        protected JdbcTemplate getJdbcTemplate() {
//            if (_jdbcTemplate == null)
//                return super.getJdbcTemplate();
//            else {
//                return _jdbcTemplate;
//            }
//        }
//
//        protected String getSql() {
//            _jdbcTemplate = null;
//            String selectSql = "select " + CollectionUtil.toString(AOR_SUM_COLS);
//
//            String sql;
//            if (_jdbcTemplate != null) {
//                SqlUtil.createTempTable(_jdbcTemplate.getDataSource(), "aorById", new ColumnDef("id", new ArrayList<Integer>(_requestIDs)));
//
//                sql = selectSql +
//                        " from requestinformation ri, targetinformation ti, aorById tmp" +
//                        " where ri.reqkey = ti.reqkey" +
//                        " and ri.reqkey = tmp.id";
//
//            } else {
//                sql = selectSql +
//                        " from requestinformation ri, targetinformation ti " +
//                        " where ri.reqkey = ti.reqkey" +
//                        " and ri.reqkey in (" + CollectionUtil.toString(_requestIDs, ", ") + ")";
//            }
//            return sql;
//        }
//
//        protected Object[] getSqlParams() {
//            return new Object[0];
//        }
    }


    @SearchProcessorImpl(id ="bcdByRequestID")
    public static class Bcd extends BcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            String sql;
            if (includeByConstraints(request)) {
                sql = "select p.* from bcdproducts p" +
                        " where p.reqkey in (select distinct d.reqkey from constraintinformation d, constraintinformation c where d.constrid=c.constrid and c.reqkey in (" + makeIdString(request) + ")"+
                        " union select reqkey from requestinformation where reqkey in (" + makeIdString(request) + "))";
            } else {
                sql = "select * from bcdproducts where reqkey in (" + makeIdString(request) + ")";
            }
            return new SqlParams(sql);
        }

    }

    @SearchProcessorImpl(id ="pbcdByRequestID")
    public static class Pbcd extends PbcdQuery {

        protected SqlParams makeSqlParams(TableServerRequest request) {
            String sql;
            if (includeByConstraints(request)) {
                sql = "select p.* from postbcdproducts p" +
                        " where p.reqkey in (select distinct d.reqkey from constraintinformation d, constraintinformation c where d.constrid=c.constrid and c.reqkey in (" + makeIdString(request) + ")"+
                        " union select reqkey from requestinformation where reqkey in (" + makeIdString(request) + "))";
            } else {
                sql = "select * from postbcdproducts where reqkey in (" + makeIdString(request) + ")";
            }
            return new SqlParams(sql);
        }

        public BcdQuery getBcdQuery() { return new Bcd(); }
    }
}
