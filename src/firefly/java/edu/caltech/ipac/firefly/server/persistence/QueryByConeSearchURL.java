/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
@SearchProcessorImpl(id = "ConeSearchByURL", params=
        {@ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="accessUrl", desc="access URL"),
         @ParamDoc(name="title", desc="catalog title")
        })

public class QueryByConeSearchURL extends QueryVOTABLE {
    //private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static final String RADIUS_KEY = "radius";
    public static final String ACCESS_URL = "accessUrl";
    public static final String TITLE_KEY = "title";


    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.CATALOGS;
    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return "conesearch-";
    }

    protected String getQueryString(TableServerRequest req) throws DataAccessException {
        String accessUrl = req.getParam(ACCESS_URL);
        if (accessUrl == null) {
            throw new DataAccessException("could not find the parameter "+ACCESS_URL);
        }
        if (!(accessUrl.endsWith("?") || accessUrl.endsWith("&"))) {
            if (accessUrl.contains("?")) { accessUrl += "&"; }
            else { accessUrl += "?"; }
        }

        WorldPt wpt = req.getWorldPtParam(ServerParams.USER_TARGET_WORLD_PT);
        if (wpt == null) {
            throw new DataAccessException("could not find the paramater: " + ServerParams.USER_TARGET_WORLD_PT);
        }
        wpt = Plot.convert(wpt, CoordinateSys.EQ_J2000);

        double radVal = req.getDoubleParam(RADIUS_KEY);
        double radDeg = MathUtil.convert(MathUtil.Units.parse(req.getParam(CatalogRequest.RAD_UNITS), MathUtil.Units.DEGREE), MathUtil.Units.DEGREE, radVal);
        return accessUrl + "RA=" + wpt.getLon() + "&DEC=" +wpt.getLat() + "&SR=" + radDeg;

    }

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        super.prepareTableMeta(meta, columns, request);
        String lonCol = meta.getAttribute("POS_EQ_RA_MAIN");
        String latCol = meta.getAttribute("POS_EQ_DEC_MAIN");
        if (!StringUtils.isEmpty(lonCol) && !StringUtils.isEmpty(latCol)) {
            meta.setLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS,
                    new TableMeta.LonLatColumns(lonCol, latCol, CoordinateSys.EQ_J2000));
        }

        boolean catalogDataFound= (lonCol!=null && latCol!=null);
        if (catalogDataFound) {
            String title = request.getParam(TITLE_KEY);
            meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, title == null ? "VO Catalog" : title);
            meta.setAttribute(MetaConst.DATA_PRIMARY, "False");
        }

    }
}
