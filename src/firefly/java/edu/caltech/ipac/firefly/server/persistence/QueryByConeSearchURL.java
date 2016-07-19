/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * @author tatianag
 *         $Id: $
 */
@SearchProcessorImpl(id = "ConeSearchByURL", params=
        {@ParamDoc(name="UserTargetWorldPt", desc="the target point, a serialized WorldPt object"),
         @ParamDoc(name="radius", desc="radius in degrees"),
         @ParamDoc(name="accessUrl", desc="access URL"),
         @ParamDoc(name="title", desc="catalog title"),
         @ParamDoc(name="use", desc="catalog_overlay, catalog_primary, or data_primary")
        })

public class QueryByConeSearchURL extends QueryVOTABLE {
    //private static final Logger.LoggerImpl _log = Logger.getLogger();

    public static final String RADIUS_KEY = "radius";
    public static final String ACCESS_URL = "accessUrl";


    @Override
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.CATALOGS;
    }

    @Override
    protected String getFilePrefix(TableServerRequest request) {
        return "conesearch-";
    }

    protected String getQueryString(TableServerRequest req) throws DataAccessException {
        String accessUrl = req.getSafeParam(ACCESS_URL);
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
}
