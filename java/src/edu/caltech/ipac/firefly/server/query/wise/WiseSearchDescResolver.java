/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query.wise;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.SearchDescResolverCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Sep 22, 2011
 *
 * @author loi
 * @version $Id: WiseSearchDescResolver.java,v 1.5 2012/02/14 21:17:54 tatianag Exp $
 */
public class WiseSearchDescResolver extends SearchDescResolver implements SearchDescResolverCreator {

    public static final String ID = "wise-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX;

    public SearchDescResolver create() {
        return this;
    }

    public String getTitle(Request req) {

        return Application.getInstance().getProperties()
                        .getProperty(req.getCmdName() + ".Title", req.getShortDesc());
    }

    public String getDesc(Request req) {
        String cmd = req.getCmdName() == null ? "" : req.getCmdName();
        if (cmd.equals("Hydra_wise_wise_1")) {
            return getPositionDesc(req);
        } else if (cmd.equals("Hydra_wise_wise_2")) {
            return getScanDesc(req);
        } else if (cmd.equals("Hydra_wise_wise_3")) {
            return getCoaddDesc(req);
        } else if (cmd.equals("Hydra_wise_wise_4")) {
            return getSourceDesc(req);
        } else if (cmd.equals("Hydra_wise_wise_5")) {
            return getSolarDesc(req);
        } else {
            return super.getDesc(req);
        }
    }

//====================================================================
//
//====================================================================

    private String getPositionDesc(Request req) {
        String source;
        if (req.getParam("filename") != null) {
            source = "Multi-Object";
        } else {
            source = req.getParam(SimpleTargetPanel.TARGET_NAME_KEY);
            if (source == null) {
                source = req.getParam(ReqConst.USER_TARGET_WORLD_PT);
            }
        }

        return source + getSearchType(req) + getSearchRadius(req) +
                getCutoutSize(req) + getDataProduct(req) + getImagesetSelection(req);
    }

    private String getScanDesc(Request req) {
        String scanId = req.getParam("scanId");
        scanId = StringUtils.isEmpty(scanId) ? "" : "ID(s)=" + scanId;

        return scanId + getImagesetSelection(req);
    }

    private String getSolarDesc(Request req) {
        String objType = req.getParam("obj_type_1");
        objType = StringUtils.isEmpty(objType) ? "" : "(" + objType + ")";
        String objName = req.getParam("obj_name");
        objName = StringUtils.isEmpty(objName) ? "" : objName;

        return objName + objType + getSearchRadius(req) + getImagesetSelection(req);
    }

    private String getCoaddDesc(Request req) {
        String coaddid = req.getParam("coaddId");
        coaddid = StringUtils.isEmpty(coaddid) ? "" : "ID(s)=" + coaddid;

        return coaddid + getDataProduct(req) + getImagesetSelection(req);
    }

    private String getSourceDesc(Request req) {
        String sourceid = req.getParam("sourceId");
        if (StringUtils.isEmpty(sourceid)) sourceid = req.getParam("refSourceId");
        sourceid = StringUtils.isEmpty(sourceid) ? "" : sourceid;

        return sourceid + getSearchType(req) + getSearchRadius(req) +
                getCutoutSize(req) + getDataProduct(req) + getImagesetSelection(req);
    }

    private String getImagesetSelection(Request req) {
        String schema = req.getParam("schema");
        return StringUtils.isEmpty(schema) ? "" : "; " + schema;
    }

    private String getBandSelection(Request req) {
        String band = req.getParam("band");
        return StringUtils.isEmpty(band) ? "" : "; Band=" + band;
    }

    private String getDataProduct(Request req) {
        String optLevel = req.getParam("optLevel");
        return StringUtils.isEmpty(optLevel) ? "" : "; Product Level=" + optLevel;
    }

    private String getSearchType(Request req) {
        String intersect = req.getParam("intersect");
        return StringUtils.isEmpty(intersect) ? "" : "; Type=" + intersect;
    }

    private String getSearchRadius(Request req) {
        String radius = req.getParam("size");
        return StringUtils.isEmpty(radius) ? "" : "; Region=" + toDegString(radius);
    }

    private String getCutoutSize(Request req) {
        String cutout = req.getParam("subsize");
        return StringUtils.isEmpty(cutout) ? "" : "; Image Size=" + toDegString(cutout);
    }

}

