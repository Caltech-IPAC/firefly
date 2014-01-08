package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.catquery.GatorQuery;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.heritage.searches.SearchByPosition;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static edu.caltech.ipac.firefly.util.DataSetParser.*;

/**
 * @author tatianag
 *         $Id: SourceListQuery.java,v 1.4 2012/11/16 19:35:54 tatianag Exp $
 */
@SearchProcessorImpl(id ="SourceListQuery")
public class SourceListQuery  extends GatorQuery {

    private static final ClassProperties COL_META = new ClassProperties("SourceListQuery", SourceListQuery.class);


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
        meta.setAttribute(HeritageQuery.DATA_TYPE, edu.caltech.ipac.heritage.data.entity.DataType.SOURCE_LIST.toString());
        meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "IRSA");
        meta.setLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS, col);

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
    }


    private CatalogRequest getCatalogRequest(TableServerRequest request) throws IOException, DataAccessException {
        if (request instanceof CatalogRequest) {
            return (CatalogRequest)request;
        }        

        CatalogRequest gatorReq = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        gatorReq.copyFrom(request);
        gatorReq.setQueryCatName(request.getParam(CatalogRequest.CATALOG));
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
        }
        return gatorReq;
    }

}
