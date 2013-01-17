package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.catquery.GatorQuery;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.*;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.*;

/**
 * @author tatianag
 *         $Id: IrsEnhancedQuery.java,v 1.22 2012/10/26 14:45:03 tatianag Exp $
 */
@SearchProcessorImpl(id = "IrsEnhancedQuery", params =
        {@ParamDoc(name = CatalogRequest.SEARCH_METHOD, desc = "values: Cone, Eliptical, Box, Polygon, Table, AllSky"),
                @ParamDoc(name = CatalogRequest.RADIUS, desc = "float, the radius of the code search"),
                @ParamDoc(name = CatalogRequest.SIZE, desc = "float, the length of a side for a box search"),
                @ParamDoc(name = CatalogRequest.RAD_UNITS, desc = "the units for the radius or side, must be arcsec,arcmin,degree, default arcsec"),
                @ParamDoc(name = CatalogRequest.CATALOG, desc = "the catalog name to search"),
                @ParamDoc(name = CatalogRequest.RA_DEC_J2000, desc = "the ra and dec in j2000 separated by a space"),
                @ParamDoc(name = CatalogRequest.FILE_NAME, desc = "for upload, not yet used"),
                @ParamDoc(name = CatalogRequest.PA, desc = "pa for elliptical request"),
                @ParamDoc(name = CatalogRequest.RATIO, desc = "ratio for elliptical request"),
                @ParamDoc(name = CatalogRequest.POLYGON, desc = "a set of coordinate pairs (up to 15), eg. 20.5 21.5, 20.5 20.5, 21.5 20.5"),
                @ParamDoc(name = CatalogRequest.SELECTED_COLUMNS, desc = "a comma separated list of columns to return, empty gives the default list"),
                @ParamDoc(name = CatalogRequest.CONSTRAINTS, desc = "a where fragment of the column constrains"),
                @ParamDoc(name = CatalogRequest.DATABASE, desc = "the database to search against"),
                @ParamDoc(name = CatalogRequest.SERVER, desc = "i am not sure what this one means"),
                @ParamDoc(name = CatalogRequest.DD_FILE, desc = "the dd file to use"),
                @ParamDoc(name = CatalogRequest.DD_ONLIST, desc = "search catalog that on on list, optional: default true"),
                @ParamDoc(name = CatalogRequest.XPF_FILE, desc = "the xpf file to be used on the server when SearchMethod is Table, " +
                        "not used for other SearchMethods, optional when onlist is true, " +
                        "required when onlist is false.  example xpf file: /xpf/catupd.fp_psc.xpf"),
                @ParamDoc(name = CatalogRequest.GATOR_HOST, desc = "The hostname for the gator URL. optional: almost never used"),
                @ParamDoc(name = CatalogRequest.SERVICE_ROOT, desc = "the part of the URL string that specifies the service and first params. " +
                        "optional: almost never used"),
                @ParamDoc(name = CatalogRequest.USE, desc = "how this catalog will be used on the client: value: primary or overlay; default overlay.  " +
                        "if overlay then the meta value CatalogOverlayType is set otherwise it is not")
        })
public class IrsEnhancedQuery extends GatorQuery {

    private static final ClassProperties COL_META = new ClassProperties("IrsEnhancedQuery", IrsEnhancedQuery.class);

    private static String ALL = InstrumentPanel.ALL;

    @Override
    public ServerRequest inspectRequest(ServerRequest request) {
        CatalogRequest gatorReq = null;
        try {
            gatorReq = getCatalogRequest((TableServerRequest)request);
        } catch (DataAccessException e) {
            Logger.error(e);
        } catch (IOException e) {
            Logger.error(e);
        }
        return gatorReq;
    }
        
    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request)  {

        CatalogRequest gatorReq;
        try {
            gatorReq = getCatalogRequest((TableServerRequest)request);
        } catch (UnsupportedEncodingException e) {
            Logger.error(e);
            return;
        } catch (Exception e) {
            Logger.error(e);
            return;
        }

        CatalogRequest req = QueryUtil.assureType(CatalogRequest.class, gatorReq);

        TableMeta.LonLatColumns col= new TableMeta.LonLatColumns("ra", "dec", CoordinateSys.EQ_J2000);
        meta.setCenterCoordColumns(col);

        TableMeta.LonLatColumns c1= new TableMeta.LonLatColumns("ra1", "dec1", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c2= new TableMeta.LonLatColumns("ra2", "dec2", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c3= new TableMeta.LonLatColumns("ra3", "dec3", CoordinateSys.EQ_J2000);
        TableMeta.LonLatColumns c4= new TableMeta.LonLatColumns("ra4", "dec4", CoordinateSys.EQ_J2000);
        meta.setCorners(c1, c2, c3, c4);
                
        meta.setAttribute(HeritageQuery.DATA_TYPE, edu.caltech.ipac.heritage.data.entity.DataType.IRS_ENHANCED.toString());

        setColumnTips(meta, req);

        String base = "dd";
        if (COL_META.getItems(base) != null) {
            for(String s : COL_META.getItems(base)) {
                String prop = base+"."+s;
                String label = COL_META.getTitle(prop);
                String visi = COL_META.getDefault(prop);
                int width = COL_META.getIntValue(prop);

                label = label.startsWith(prop) ? s : label;
                visi = visi.startsWith(prop) ? "show" : visi;

                meta.setAttribute(makeAttribKey(LABEL_TAG, s), label);
                meta.setAttribute(makeAttribKey(VISI_TAG, s), visi);
                if (width>0) {
                    meta.setAttribute(makeAttribKey(WIDTH_TAG, s), width);
                }
            }
        }

        //super.prepareTableMeta(meta, columns, req);
    }



    private CatalogRequest getCatalogRequest(TableServerRequest request) throws IOException, DataAccessException {
        if (request instanceof CatalogRequest) {
            return (CatalogRequest) request;
        }

        CatalogRequest gatorReq = getIrsEnhancedBaseRequest(request);

        if (request instanceof SearchIrsEnhanced.Req) {
            gatorReq.setMethod(CatalogRequest.Method.ALL_SKY);
            //gatorReq.setSelectedColumns(((SearchIrsEnhanced.Req)req).getSelectedColumns());
            gatorReq.setConstraints( ((SearchIrsEnhanced.Req) request).getConstraints());
            return gatorReq;
        }

        String constraints = gatorReq.getConstraints();
        String extraConstraints = null;


        if (request instanceof SearchByPosition.SingleTargetReq || request.getParam(ReqConst.USER_TARGET_WORLD_PT) != null) {

            SearchByPosition.SingleTargetReq req = QueryUtil.assureType(SearchByPosition.SingleTargetReq.class, request);
            gatorReq.setRadius(req.getRadius());
            gatorReq.setRadUnits(CatalogRequest.RadUnits.DEGREE);
            gatorReq.setMethod(CatalogRequest.Method.CONE);
            gatorReq.setWorldPtJ2000(req.getPos());

        } else if (request instanceof SearchByPosition.MultiTargetReq || request.getParam(SearchByPosition.MultiTargetReq.UPLOAD_FILE_KEY) != null) {

            SearchByPosition.MultiTargetReq req = QueryUtil.assureType(SearchByPosition.MultiTargetReq.class, request);
            gatorReq.setRadius(req.getRadius());
            gatorReq.setRadUnits(CatalogRequest.RadUnits.DEGREE);
            gatorReq.setMethod(CatalogRequest.Method.TABLE);
            String uploadedTargetFile = req.getUploadedFilePath();
            gatorReq.setFileName(Utils.convertToGatorFormat(uploadedTargetFile));

        } else if (request instanceof SearchByRequestID.Req || request.getParam(SearchByRequestID.Req.REQIDS) != null) {

            SearchByRequestID.Req req = QueryUtil.assureType(SearchByRequestID.Req.class, request);
            gatorReq.setMethod(CatalogRequest.Method.ALL_SKY);
            int [] reqkeys = req.getReqIDs();
            String reqkeysStr = QueryByRequestID.makeIdString(req);
            if (req.includeSameConstraints()) {
                SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(DbInstance.archive);
                    try {
                        List<Integer> allReqkeys = jdbc.query("select reqkey from requestinformation where reqkey in ("+reqkeysStr+") union"+
                                   " select distinct d.reqkey from constraintinformation d, constraintinformation c where d.constrid=c.constrid and c.reqkey in ("+reqkeysStr+")",
                                new ParameterizedRowMapper<Integer>() {
                                    public Integer mapRow(ResultSet resultSet, int i) throws SQLException {
                                        return resultSet.getInt(1);  //To change body of implemented methods use File | Settings | File Templates.
                                    }
                                }, new HashMap());
                        if (allReqkeys.size()>reqkeys.length) {
                            reqkeysStr = CollectionUtil.toString(allReqkeys);
                        }
                    } catch (Exception e) {
                        Logger.error(e, "Unable to get constrained reqkeys for "+reqkeysStr+".");
                    }
            }
            extraConstraints = URLEncoder.encode("reqkey in ("+ reqkeysStr+")", "UTF-8");

        } else if (request instanceof SearchByProgramID.Req || request.getParam(SearchByProgramID.Req.PROGRAM) != null) {

            SearchByProgramID.Req req = QueryUtil.assureType(SearchByProgramID.Req.class, request);
            gatorReq.setMethod(CatalogRequest.Method.ALL_SKY);
            String program = req.getProgram();
            int progid;
            if (QueryByProgramID.DIGITS_PATTERN.matcher(program).find()) {
                // program id
                progid = Integer.parseInt(program);
            } else {
                SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(DbInstance.archive);
                try {
                    progid = jdbc.queryForInt("select first 1 progid from requestinformation where progname = ?", program.toUpperCase());
                } catch (Exception e) {
                    progid = 0;
                    Logger.error(e, "Unable to get program id for "+program+".");
                }
            }
            extraConstraints = "progid="+progid;

        } else if (request instanceof SearchByCampaignID.Req || request.getParam(SearchByCampaignID.Req.CAMPAIGN) != null) {

            SearchByCampaignID.Req req = QueryUtil.assureType(SearchByCampaignID.Req.class, request);
            gatorReq.setMethod(CatalogRequest.Method.ALL_SKY);
            String campaign = req.getCampaign();
            int campid;
            if (QueryByProgramID.DIGITS_PATTERN.matcher(campaign).find()) {
                // campaign id
                campid = Integer.parseInt(campaign);
                extraConstraints = "campid="+campid;
            } else {
                extraConstraints = "campname=\'"+campaign.toUpperCase()+"\'";
            }

        } else if (request instanceof SearchByObserver.Req || request.getParam(SearchByObserver.Req.OID) != null) {

            SearchByObserver.Req req = QueryUtil.assureType(SearchByObserver.Req.class, request);
            gatorReq.setMethod(CatalogRequest.Method.ALL_SKY);
            String observer = req.getObserver();
            // enhanced observer name is limited by 18 chars and does not include middle name
            if (observer.length()>18) observer = observer.substring(0,18);
            int lastSpaceIdx = observer.lastIndexOf(" ");
            if (observer.indexOf(" ") != lastSpaceIdx) {
                // name contains middle name, remove it
                observer = observer.substring(0, lastSpaceIdx);
            }
            extraConstraints = URLEncoder.encode("obsrvr like \'"+observer+"%\'", "UTF-8");

        } else {
            // will be used by save from SearchIrsEnhancedCmd
            SearchIrsEnhanced.Req req = QueryUtil.assureType(SearchIrsEnhanced.Req.class, request);
            gatorReq.setMethod(CatalogRequest.Method.ALL_SKY);
            //gatorReq.setSelectedColumns(req.getSelectedColumns());
            gatorReq.setConstraints( req.getConstraints());
            return gatorReq;
        }
        if (!StringUtils.isEmpty(extraConstraints)) {
            gatorReq.setConstraints(StringUtils.isEmpty(constraints) ?
                    extraConstraints : extraConstraints + " and " + constraints);

        }
        return gatorReq;
    }


    protected static CatalogRequest getIrsEnhancedBaseRequest(TableServerRequest inReq) {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        req.copyFrom(inReq);
        req.setQueryCatName(AppProperties.getProperty("irsEnhanced.gator.catname"));
        req.setDDShort(false); // long DD is requested

        // copy common optional parameters into constraints
        // IRS enhanced data contains merged wavelengths for low resolution slits
        String constraints = "";
        if (!StringUtils.isEmpty(req.getParam(InstrumentPanel.WAVE_MIN))) {
            constraints = "wave_max>="+req.getFloatParam(InstrumentPanel.WAVE_MIN);
        }
        if (!StringUtils.isEmpty(req.getParam(InstrumentPanel.WAVE_MAX))) {
            if (!StringUtils.isEmpty(constraints)) { constraints += " and "; }
            constraints += "wave_min<="+req.getFloatParam(InstrumentPanel.WAVE_MAX);
        }
        String irsSelections = req.getParam(InstrumentPanel.IRS);
        if (!StringUtils.isEmpty(irsSelections) && !irsSelections.equals(ALL)) {
            if (!irsSelections.contains("low5") || !irsSelections.contains("low7") ||
                    !irsSelections.contains("low14") || !irsSelections.contains("low20")) {
                if (!StringUtils.isEmpty(constraints)) { constraints += " and "; }
                constraints += "(";

                boolean follows = false;
                if (irsSelections.contains("low5")) {
                    constraints += "(6 between wave_min and wave_max)";
                    follows = true;
                }
                if (irsSelections.contains("low7")) {
                    if (follows) { constraints += " or "; }
                    constraints += "(8 between wave_min and wave_max)";
                    follows = true;
                }
                if (irsSelections.contains("low14")) {
                    if (follows) { constraints += " or "; }
                    constraints += "(15 between wave_min and wave_max)";
                    follows = true;
                }
                if (irsSelections.contains("low20")) {
                    if (follows) { constraints += " or "; }
                    constraints += "(21 between wave_min and wave_max)";
                    //follows = true;
                }
                constraints += ")";
            }
        }

        if (!StringUtils.isEmpty(constraints)) {
            req.setConstraints(constraints);
        }

        return req;
    }

}
