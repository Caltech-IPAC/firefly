package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.util.DataGroup;

import java.util.List;

/**
 * @author tatianag
 * $Id: AorQuery.java,v 1.21 2010/09/09 16:24:07 tatianag Exp $
 */
public abstract class AorQuery extends HeritageQuery {

    public static final String[] AOR_SUM_COLS = {"ti.targetname", "ti.raj2000", "ti.decj2000", "ti.naifid", "ri.reqmode", "ri.modedisplayname", "ri.reqtitle",
            "ri.reqbegintime","ri.reqendtime", "ri.releasedate", "ri.depthofcoverage", "ri.pi", "ri.progid","ri.reqkey"};


    public String getTemplateName() {
        return "requestinformations_dd";
    }

    public DataGroup.Attribute[] getAttributes() {
        return null;
    }

    public abstract BcdQuery getBcdQuery();

    @Override
    public void prepareTableMeta(TableMeta defaults, List<edu.caltech.ipac.util.DataType> columns, ServerRequest request) {
        super.prepareTableMeta(defaults, columns, request);
        defaults.setAttribute(DATA_TYPE, DataType.AOR.toString());
    }

    /**
     * Assumes ri for requestinformation alias
     * @param sqlParams no filter sql params
     * @param req request
     * @return sql params with filter applied
     */
    @Override
    protected SqlParams applyFilters(SqlParams sqlParams, HeritageRequest req) {
        String newSql = sqlParams.getSql();
        Object[] newParams = sqlParams.getParams();

        if (DateTimeFilter.isDefinedOn(req)) {
            DateTimeFilter filter = new DateTimeFilter(req);
            if (filter.hasStartDateFilter()) {
                newSql += " and ri.reqendtime > ?";
                newParams = addToArray(newParams, QueryUtil.convertDate(filter.getStartDate()));
            }
            if (filter.hasEndDateFilter()) {
                newSql += " and ri.reqbegintime < ?";
                newParams = addToArray(newParams, QueryUtil.convertDate(filter.getEndDate()));
            }
        }

        //String COMMON_BCD_FILTER_SQL = " and exists (select bp.reqkey from bcdproducts bp where bp.reqkey=ri.reqkey and";

        boolean bcdFilterStarted = false;

        if (InstrumentFilter.isDefinedOn(req)) {
            InstrumentFilter filter = new InstrumentFilter(req);
            if (filter.hasIracFilters() || filter.hasMipsFilters()) {
                newSql += " and "+getAotFilterSql(filter, "ri.reqmode", "ri.reqkey");
            }
            // reqmode filter was applied as part of AOT filter, if it was defined
            else if (filter.hasReqmodeFilter() || filter.hasInstrumentFilter()) {
                newSql += " and ri.reqmode in ("+ filter.getReqmodesAsString()+")";
            }
            if (filter.hasWavelenthFilter()) {
                SqlParams commonBcdFilter = getCommonBcdFilterParam(req);
                newParams = addToArray(newParams, commonBcdFilter.getParams());
                newSql += commonBcdFilter.getSql()+" wavelength in ("+filter.getWavelengthsAsString()+")";
                //newSql += COMMON_BCD_FILTER_SQL+" bp.wavelength in ("+filter.getWavelengthsAsString()+")";
                bcdFilterStarted = true;
            }
        } else if (WavelengthRangeFilter.isDefinedOn(req)){
            WavelengthRangeFilter filter = new WavelengthRangeFilter(req);
            SqlParams commonBcdFilter = getCommonBcdFilterParam(req);
            newParams = addToArray(newParams, commonBcdFilter.getParams());
            newSql += commonBcdFilter.getSql();
            //newSql += COMMON_BCD_FILTER_SQL;
            if (filter.hasMinWavelengthFilter()) {
                if (bcdFilterStarted) {
                    newSql += " and";
                } else {
                    newSql += " maxwavelength > ?";
                    newParams = addToArray(newParams, filter.getMinWavelength());
                    bcdFilterStarted = true;
                }
            }
            if (filter.hasMaxWavelengthFilter()) {
                if (bcdFilterStarted) {
                    newSql += " and";
                } else {
                    bcdFilterStarted = true;
                }
                newSql += " minwavelength < ?";
                newParams = addToArray(newParams, filter.getMaxWavelength());
            }
        }

        if (FrametimeRangeFilter.isDefinedOn(req)) {
            FrametimeRangeFilter filter = new FrametimeRangeFilter(req);
            if (filter.hasMinFrametimeFilter()) {
                if (bcdFilterStarted) {
                    newSql += " and";
                } else {
                    SqlParams commonBcdFilter = getCommonBcdFilterParam(req);
                    newParams = addToArray(newParams, commonBcdFilter.getParams());
                    newSql += commonBcdFilter.getSql();
                    //newSql += COMMON_BCD_FILTER_SQL;
                    bcdFilterStarted = true;
                }
                newSql += " exposuretime > ?";
                newParams = addToArray(newParams, filter.getMinFrametime());
            }
            if (filter.hasMaxFrametimeFilter()) {
                if (bcdFilterStarted) {
                    newSql += " and";
                } else {
                    SqlParams commonBcdFilter = getCommonBcdFilterParam(req);
                    newParams = addToArray(newParams, commonBcdFilter.getParams());
                    newSql += commonBcdFilter.getSql();
                    //newSql += COMMON_BCD_FILTER_SQL;
                    bcdFilterStarted = true;
                }
                newSql += " exposuretime < ?";
                newParams = addToArray(newParams, filter.getMaxFrametime());
            }

        }

        if (bcdFilterStarted) {
            newSql += ")";
        }
        
        return new SqlParams(newSql, newParams);
    }

    private SqlParams getCommonBcdFilterParam(HeritageRequest req) {
        SqlParams bcdSqlParams = getBcdQuery().makeSqlParams(req);
        String bcdSql = bcdSqlParams.getSql();
        int idxFrom = bcdSql.indexOf(" from ");
        if (idxFrom < 0) idxFrom = bcdSql.indexOf(" FROM ");
        assert (idxFrom > 0);
        // using bcdproducts.reqkey, because temp table (search by position) might have a column named reqkey
        String commonBcdFilterSql = " and ri.reqkey in (select bcdproducts.reqkey "+bcdSql.substring(idxFrom)+" and";
        return new SqlParams(commonBcdFilterSql, bcdSqlParams.getParams());
    }
    
}
