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

        WorldPt wpt = req.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        if (wpt == null) {
            throw new DataAccessException("could not find the paramater: " + ReqConst.USER_TARGET_WORLD_PT);
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

