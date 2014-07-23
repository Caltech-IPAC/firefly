
package edu.caltech.ipac.hydra.core;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.SearchDescResolverCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.util.StringUtils;

/**
 *
 *
 * @author  wmi
 * @version
 */
public class LcogtSearchDescResolver extends SearchDescResolver implements SearchDescResolverCreator {

    public static final String ID = "lcogt-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX;

    public SearchDescResolver create() {
        return this;
    }

    public String getTitle(Request req) {
        String title = Application.getInstance().getProperties()
                        .getProperty(req.getCmdName() + ".Title", req.getShortDesc());

        return title;
    }

    public String getDesc(Request req) {
        String cmd = req.getCmdName() == null ? "" : req.getCmdName();
        if (cmd.equals("Hydra_lcogt_lcogt_l1")) {
            return getPositionDesc(req);
        } else if (cmd.equals("Hydra_lcogt_lcogt_field")) {
            return getFieldDesc(req);
        } else if (cmd.equals("Hydra_lcogt_lcogt_most")) {
            return getSolarDesc(req);
        } else if (cmd.equals("Hydra_lcogt_lcogt_view")) {
            return getSourceDesc(req);
        } else {
            return super.getDesc(req);
        }
    }

//====================================================================
//
//====================================================================

    private String getPositionDesc(Request req) {
        String source = req.getParam(SimpleTargetPanel.TARGET_NAME_KEY);
        if (source == null) {
            source = req.getParam(ReqConst.USER_TARGET_WORLD_PT);
        }
        return source + getSearchType(req) + getSearchRadius(req) +
                getCutoutSize(req) + getDataProduct(req) + getImagesetSelection(req);
    }

    private String getFieldDesc(Request req) {
        String fieldId = req.getParam("lcogtField");
        fieldId = StringUtils.isEmpty(fieldId) ? "" : "ID(s)=" + fieldId;

        return fieldId + getImagesetSelection(req);
    }

    private String getSolarDesc(Request req) {
        String objType = req.getParam("obj_type_1");
        objType = StringUtils.isEmpty(objType) ? "" : "(" + objType + ")";
        String objName = req.getParam("obj_name");
        objName = StringUtils.isEmpty(objName) ? "" : objName;

        return objName + objType + getSearchRadius(req) + getImagesetSelection(req);
    }

    private String getCcdIdDesc(Request req) {
        String ccdid = req.getParam("ccdid");
        ccdid = StringUtils.isEmpty(ccdid) ? "" : "ID(s)=" + ccdid;

        return ccdid + getDataProduct(req) + getImagesetSelection(req);
    }

    private String getSourceDesc(Request req) {
        String sourceid = req.getParam("sourceId");
        sourceid = StringUtils.isEmpty(sourceid) ? "" : sourceid;
        String from = req.getParam("sourceLevel");
        from = StringUtils.isEmpty(from) ? "" : " from " + from;


        return sourceid + from + getSearchType(req) + getSearchRadius(req) +
                getCutoutSize(req) + getDataProduct(req) + getImagesetSelection(req);
    }

    private String getImagesetSelection(Request req) {
        String schema = req.getParam("schema");
        return StringUtils.isEmpty(schema) ? "" : "; " + schema;
    }

    private String getFilterSelection(Request req) {
        String filter = req.getParam("filter");
        return StringUtils.isEmpty(filter) ? "" : "; Filter=" + filter;
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

