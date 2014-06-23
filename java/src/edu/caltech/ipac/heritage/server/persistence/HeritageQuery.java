package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacFileQuery;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.heritage.searches.HeritageRequest;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: HeritageQuery.java,v 1.28 2012/08/21 21:17:14 tatianag Exp $
 */
abstract public class HeritageQuery extends IpacFileQuery {

    public static final String DATA_TYPE= "DataType";
    private TableServerRequest lastReq = null;
    private SqlParams lastParams = null;
    private static Map<String, String> COMMENTS_MAP = new HashMap<String,String>();
    private static Map<String, String> HEADERS_MAP = new HashMap<String,String>();
    static {
        HEADERS_MAP.put("searchTgt ",
                     "Search_Tgt");
        HEADERS_MAP.put("targetname ",
                     "Target_name");
        HEADERS_MAP.put("raj2000  ",
                     "RA(J2000)");
        HEADERS_MAP.put("decj2000  ",
                     "Dec(J2000)");
        HEADERS_MAP.put("naifid ",
                     "NAIF_ID");
        HEADERS_MAP.put("modedisplayname",
                     "Instrument/Mode");
        HEADERS_MAP.put("reqkey",
                     "AORKEY");
        HEADERS_MAP.put("reqtitle ",
                     "AOR_label");
        HEADERS_MAP.put("reqbegintime          ",
                     "Observation_start_time");
        HEADERS_MAP.put("reqendtime          ",
                     "Observation_end_time");
        HEADERS_MAP.put("progid    ",
                     "Program_ID");
        HEADERS_MAP.put("pi",
                     "PI");
        HEADERS_MAP.put("releasedate ",
                     "Release_date");
        HEADERS_MAP.put("depthofcoverage",
                     "DoC_file       ");
        HEADERS_MAP.put("reqmode     ",
                     "AOT/IER_mode");

        COMMENTS_MAP.put("aorByCampaignID","\\COMMENT Campaign ID search on %s" +
                "|SearchByCampaign.field.campaign");
        COMMENTS_MAP.put("aorByObserver","\\COMMENT Observer search on %s" +
                "|SearchByObserver.field.observer");
        COMMENTS_MAP.put("aorByPosition","\\COMMENT Position search on %s" +
                "|TargetPanel.field.targetName|UserTargetWorldPt|UploadedFilePath");
        COMMENTS_MAP.put("aorByProgramID","\\COMMENT Program ID search on %s" +
                "|SearchByProgram.field.program");
        COMMENTS_MAP.put("aorByRequestID","\\COMMENT Request ID search on %s" +
                "|SearchByRequestID.field.requestID");
    }

    public DbInstance getDbInstance() {
        return DbInstance.archive;
    }

    public String getSql(TableServerRequest request) {
        SqlParams params = doMakeSqlParams(request);
        return params == null ? null : params.getSql();
    }

    public Object[] getSqlParams(TableServerRequest request) {
        return doMakeSqlParams(request).getParams();
    }

    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
        if(getAttributes() != null) {
            for(DataGroup.Attribute atrib : getAttributes()) {
                results.getData().addAttributes(new DataGroup.Attribute(
                            atrib.getKey(), String.valueOf(atrib.getValue())));
            }
        }

        if (results.getData().containsKey(getReqKeyCName())) {
            // apply hasAccess info
            results.getData().addAttributes(new DataGroup.Attribute(TableMeta.HAS_ACCESS_CNAME, "hasAccess"));
            results.setHasAccessCName("hasAccess");

            for(int i = 0; i <results.getSize(); i++) {
                String reqkey = String.valueOf(results.getData().get(i).getDataElement(getReqKeyCName()));
                results.setHasAccess(i, HeritageSecurityModule.checkHasAccess(reqkey));
            }
        }
    }

    @Override
    public Map<String, String> getOutputColumnsMap() {
        return HEADERS_MAP;
    }

    @Override
    public void prepareAttributes(int rows, BufferedWriter writer, ServerRequest sr) throws IOException {
        super.prepareAttributes(rows, writer, sr);
        String id = sr.getParam("id");
        Date date = ServerContext.getRequestOwner().getStartTime();
        String user = ServerContext.getRequestOwner().getUserInfo().getLoginName();

        writeLine(writer, "\\COMMENT ----------------------------------------------------------------------");

        if (id!=null && COMMENTS_MAP.containsKey(id)) {
            String[] tokens= COMMENTS_MAP.get(id).split("\\|");
            writeLine(writer, "\\COMMENT This is the results of a Spitzer Heritage Archive (SHA) search.");
            writeLine(writer, "\\COMMENT The SHA is resident at the NASA/IPAC Infrared Science Archive (IRSA)");
            writeLine(writer, "\\COMMENT at the Infrared Processing and Analysis Center (IPAC)");
            writeLine(writer, "\\COMMENT http://irsa.ipac.caltech.edu/applications/Spitzer/SHA/");
            writeLine(writer, "\\COMMENT More information on the Spitzer Space Telescope and the SHA and more ");
            writeLine(writer, "\\COMMENT search options can be found at that site.");
            if (id.equals("aorByPosition")) {
                String answer = "";
                if (sr.getParam(tokens[3])!=null) {
                    answer= "multiple targets (see Search_Tgt column)";
                } else if (sr.getParam(tokens[1])!=null) {
                    answer= sr.getParam(tokens[1]);
                } else if (sr.getParam(tokens[2])!=null){
                    answer= sr.getParam(tokens[2]).replaceAll(";"," ");
                }
                writeLine(writer,String.format(tokens[0], answer));
            } else {
                writeLine(writer,String.format(tokens[0], sr.getParam(tokens[1])));
            }
        }

        writeLine(writer, "\\COMMENT conducted on "+date+", by "+user);
        writeLine(writer, "\\COMMENT ----------------------------------------------------------------------");
        writeLine(writer, "\\RowsRetrieved = "+rows);
        writeLine(writer, "\\ORIGIN = NASA/IPAC Infrared Science Archive (IRSA), Caltech/JPL");
        writeLine(writer, "\\fixlen = T");
        writeLine(writer, "\\primary = 0");
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(defaults, columns, request);
        TableMeta.LonLatColumns col= new TableMeta.LonLatColumns("ra", "dec", CoordinateSys.EQ_J2000);
        defaults.setCenterCoordColumns(col);

        TableMeta.LonLatColumns c1= new TableMeta.LonLatColumns("ra1", "dec1", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c2= new TableMeta.LonLatColumns("ra2", "dec2", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c3= new TableMeta.LonLatColumns("ra3", "dec3", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c4= new TableMeta.LonLatColumns("ra4", "dec4", CoordinateSys.EQ_J2000);
        defaults.setCorners(c1, c2, c3, c4);
        defaults.setAttribute("col.modedisplayname.PrefWidth", "18");
    }

    abstract protected SqlParams makeSqlParams(TableServerRequest request);
    abstract DataGroup.Attribute[] getAttributes();

    private SqlParams doMakeSqlParams(TableServerRequest request) {
        if (lastParams == null || !request.equals(lastReq)) {
            lastParams =  makeSqlParams(request);
            if (request instanceof HeritageRequest) {
                lastParams = applyFilters(lastParams, ((HeritageRequest)request));
            }
            lastReq = request;
        }
        return lastParams;
    }


    protected SqlParams applyFilters(SqlParams sqlParams, HeritageRequest req) {

        if (sqlParams == null) return null;

        String newSql = sqlParams.getSql();
        Object [] newParams = sqlParams.getParams();

        if (InstrumentFilter.isDefinedOn(req)) {
            if (InstrumentFilter.isDefinedOn(req)) {
                InstrumentFilter filter = new InstrumentFilter(req);
                if (filter.hasIracFilters() || filter.hasMipsFilters()) {
                    newSql += " and "+ getAotFilterSql(filter, "reqmode", "reqkey");
                }
                // reqmode filter was applied as part of AOT filter, if it was defined
                else if (filter.hasReqmodeFilter() || filter.hasInstrumentFilter()) {
                    newSql += " and reqmode in ("+filter.getReqmodesAsString()+")";
                }
                if (filter.hasWavelenthFilter()) {
                    newSql += " and wavelength in ("+filter.getWavelengthsAsString()+")";
                }
            } else if (WavelengthRangeFilter.isDefinedOn(req)){
                WavelengthRangeFilter filter = new WavelengthRangeFilter(req);
                if (filter.hasMinWavelengthFilter()) {
                    newSql += " and maxwavelength > "+filter.getMinWavelength();
                }
                if (filter.hasMaxWavelengthFilter()) {
                    newSql += " and minwavelength < "+filter.getMaxWavelength();
                }
            }
        }

        if (this instanceof BcdQuery) {
            if (FrametimeRangeFilter.isDefinedOn(req)) {
                FrametimeRangeFilter filter = new FrametimeRangeFilter(req);
                if (filter.hasMinFrametimeFilter()) {
                    newSql += " and exposuretime > ?";
                    newParams = addToArray(newParams, filter.getMinFrametime());
                }
                if (filter.hasMaxFrametimeFilter()) {
                    newSql += " and exposuretime < ?";
                    newParams = addToArray(newParams, filter.getMaxFrametime());
                }
            }
            if (DateTimeFilter.isDefinedOn(req)) {
                DateTimeFilter filter = new DateTimeFilter(req);
                if (filter.hasStartDateFilter()) {
                    newSql += " and scet > ?";
                    newParams = addToArray(newParams, QueryUtil.convertDate(filter.getStartDate()));
                }
                if (filter.hasEndDateFilter()) {
                    newSql +=  " and scet < ?";
                    newParams = addToArray(newParams, QueryUtil.convertDate(filter.getEndDate()));
                }
            }
        } else if (this instanceof PbcdQuery) {  // pbcdQuery
            if (FrametimeRangeFilter.isDefinedOn(req)) {
                // TODO: after max and min exposuretime are present in pbcd products table - add filtering for postbcds
                // this is workaround while exposuretime is not present in postbcdproducts table
                FrametimeRangeFilter filter = new FrametimeRangeFilter(req);
                SqlParams bcdSqlParams = ((PbcdQuery)this).getBcdQuery().makeSqlParams(req);
                String bcdSql = bcdSqlParams.getSql();
                int idxFrom = bcdSql.indexOf(" from ");
                if (idxFrom < 0) idxFrom = bcdSql.indexOf(" FROM ");
                assert (idxFrom > 0);
                newSql += " and dcesetid in (select dcesetid from dcesets where dceid in (select dceid "+bcdSql.substring(idxFrom);
                newParams = (addToArray(newParams, bcdSqlParams.getParams()));
                if (filter.hasMinFrametimeFilter()) {
                    newSql += " and exposuretime > ?";
                    newParams = addToArray(newParams, filter.getMinFrametime());
                }
                if (filter.hasMaxFrametimeFilter()) {
                    newSql += " and exposuretime < ?";
                    newParams = addToArray(newParams, filter.getMaxFrametime());
                }
                newSql += "))";
            }
            if (DateTimeFilter.isDefinedOn(req)) {
                DateTimeFilter filter = new DateTimeFilter(req);
                if (filter.hasStartDateFilter()) {
                    newSql += " and endtime > ?";
                    newParams = addToArray(newParams, QueryUtil.convertDate(filter.getStartDate()));
                }
                if (filter.hasEndDateFilter()) {
                    newSql += " and begintime < ?";
                    newParams = addToArray(newParams, QueryUtil.convertDate(filter.getEndDate()));
                }                
            }
        } else {
            assert (false);
        }

        return new SqlParams(newSql, newParams);
    }

    protected static Object[] addToArray(Object [] srcArray, Object toAdd) {
        Object [] newArray = new Object[srcArray.length+1];
        System.arraycopy(srcArray, 0, newArray, 0, srcArray.length);
        newArray[srcArray.length]=toAdd;
        return newArray;
    }

    protected static Object[] addToArray(Object [] srcArray, Object [] toAdd) {
        Object [] newArray = new Object[srcArray.length+toAdd.length];
        System.arraycopy(srcArray, 0, newArray, 0, srcArray.length);
        System.arraycopy(toAdd, 0, newArray, srcArray.length, toAdd.length);
        return newArray;
    }

    protected static String getAotFilterSql(InstrumentFilter filter, String reqmodeColName, String reqkeyColName){
        Collection<String> allReqmodes = filter.getReqmodes();
        ArrayList<String> uncheckedReqmodes = new ArrayList(allReqmodes); 


         String newSql = "";
         String preSql = "select reqkey from ";
         String postSql = " aot where ";

         boolean aotFilterStarted = false;
         if (filter.hasIracFilters()) {
             String iracSql = "";
             if (filter.hasIracFullArrayFilter()) {
                 iracSql += "readoutfull="+filter.getIracFullArray();
                 aotFilterStarted = true;
             }


             if (filter.hasIracHDRFilter()) {
                 if (aotFilterStarted) {
                     iracSql += " or ";
                 } else {
                     aotFilterStarted = true;
                 }
                 iracSql += "highdynamic=1";
             }

             // IRAC MAP PC (has no stellar mode)
             if (aotFilterStarted && allReqmodes.contains("IracMapPC")) {
                 uncheckedReqmodes.remove("IracMapPC");
                 newSql += preSql+"IracMapPC"+postSql+iracSql+
                         (allReqmodes.contains("IracMap") ? " union " : "");
             }

             if (filter.hasIracStellarModeFilter()) {
                 if (aotFilterStarted) {
                     iracSql += " or ";
                 } else {
                     aotFilterStarted = true;
                     // if only stellar requested we want exclude IracMapPC,
                     // since stellar is not supported for IracMapPC
                     uncheckedReqmodes.remove("IracMapPC");
                 }
                 iracSql += "stellarmode=1";
             }
             // IRAC MAP
             if (aotFilterStarted && allReqmodes.contains("IracMap")) {
                 uncheckedReqmodes.remove("IracMap");
                 newSql += preSql+"IracMap"+postSql+iracSql;
             }
         }

         if (filter.hasMipsPhotScaleFilter()) {
             uncheckedReqmodes.remove("MipsPhot");
             if (aotFilterStarted) {
                 newSql += " union ";
             } else {
                 aotFilterStarted = true;
             }
             newSql += preSql+"MipsPhot"+postSql+"w70_scalefine="+filter.getMipsScaleFine();
         }

         if (filter.hasMipsScanRateFilter()) {
             uncheckedReqmodes.remove("MipsScan");
             if (aotFilterStarted) {
                 newSql += " union ";
             }
             newSql += preSql+"MipsScan"+postSql+"scanrate in ("+filter.getMipsScanRatesAsString()+")";
         }

        if (uncheckedReqmodes.size() > 0) {
            newSql = "("+reqmodeColName+" in ("+InstrumentFilter.toQuotedStringList(uncheckedReqmodes)+") or "+
                    reqkeyColName+" in ("+newSql+"))";
        } else {
            newSql = reqkeyColName+" in ("+newSql+")";
        }
         return newSql;
     }



    protected String getReqKeyCName() {
        return "reqkey";
    }


//====================================================================
//
//====================================================================


    public static class SqlParams {
        private String sql;
        private Object[] params;

        public SqlParams(String sql, Object... params) {
            this.sql = sql;
            this.params = params;
        }

        public String getSql() {
            return sql;
        }

        public Object[] getParams() {
            return params;
        }
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
