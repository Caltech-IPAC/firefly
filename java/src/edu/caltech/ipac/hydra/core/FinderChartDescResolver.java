package edu.caltech.ipac.hydra.core;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.SearchDescResolverCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.util.StringUtils;


/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Apr 12, 2012
 * Time: 5:45:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class FinderChartDescResolver extends SearchDescResolver implements SearchDescResolverCreator {

    public static final String ID = "finderChart-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX;

    private static NumberFormat nf;

    static {
        try {
            nf= NumberFormat.getFormat("#.###");
        } catch (Throwable e) {
            nf = null;
        }
    }

    public SearchDescResolver create() {
        return this;
    }

    public String getTitle(Request req) {

        return Application.getInstance().getProperties()
                        .getProperty(req.getCmdName() + ".Title", req.getShortDesc());
    }

    public String getDesc(Request req) {
        String cmd = req.getCmdName() == null ? "" : req.getCmdName();
        if (cmd.equals("Hydra_finderchart_finder_chart")) {
            return getPositionDesc(req);
        } else {
            return super.getDesc(req);
        }
    }

//====================================================================
//
//====================================================================
    public static String getSourceDesc(ServerRequest req) {
        String source;
        if (req.getParam("filename") != null) {
            source = "Multi-Object";
        } else {
            String targetStr = req.getParam(SimpleTargetPanel.TARGET_NAME_KEY);
            if (targetStr == null) {
                String userWP = req.getParam(ReqConst.USER_TARGET_WORLD_PT);
                if (userWP != null) {
                    String wptStr[] = userWP.split(";");
                    if (nf != null) {
                        targetStr = nf.format(Double.parseDouble(wptStr[0])) + " " +
                                nf.format(Double.parseDouble(wptStr[1])) + " " +
                                wptStr[2];
                    } else {
                        targetStr = wptStr[0] + " " + wptStr[1] + " " + wptStr[2];
                    }
                } else {
                    targetStr = "unknown";
                }
            }
            source = "Target= " + targetStr;
        }
        return source;
    }

    private String getPositionDesc(Request req) {
        return getSourceDesc(req) + getCutoutSize(req) + getDataProduct(req);
    }

    private String getDataProduct(Request req) {
        String sources = req.getParam("sources");
        sources = sources.replaceAll("twomass","2MASS");
        return StringUtils.isEmpty(sources) ? "" : "; Sources=" + sources;
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
