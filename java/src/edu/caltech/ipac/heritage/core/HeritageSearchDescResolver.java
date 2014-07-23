package edu.caltech.ipac.heritage.core;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.SearchDescResolverCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.heritage.commands.AbstractSearchCmd;
import edu.caltech.ipac.heritage.commands.SearchByCampaignCmd;
import edu.caltech.ipac.heritage.commands.SearchByDateCmd;
import edu.caltech.ipac.heritage.commands.SearchByNaifIDCmd;
import edu.caltech.ipac.heritage.commands.SearchByObserverCmd;
import edu.caltech.ipac.heritage.commands.SearchByPositionCmd;
import edu.caltech.ipac.heritage.commands.SearchByProgramCmd;
import edu.caltech.ipac.heritage.commands.SearchByRequestIDCmd;
import edu.caltech.ipac.heritage.commands.SearchIrsEnhancedCmd;
import edu.caltech.ipac.heritage.commands.SearchMOSCmd;
import edu.caltech.ipac.util.StringUtils;

import java.util.Date;

/**
 * Date: Sep 22, 2011
 *
 * @author loi
 * @version $Id: HeritageSearchDescResolver.java,v 1.8 2011/12/12 21:25:59 tatianag Exp $
 */
public class HeritageSearchDescResolver extends SearchDescResolver implements SearchDescResolverCreator {

    public static final String ID = Application.getInstance().getAppName() + "-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX;

    public SearchDescResolver create() {
        return this;
    }

    public String getDesc(Request req) {
        String cmd = req.getCmdName() == null ? "" : req.getCmdName();
        if (cmd.equals(SearchByPositionCmd.COMMAND_NAME)) {
            return getPositionDesc(req, SearchByPositionCmd.RADIUS_KEY);
        } else if (cmd.equals(SearchByRequestIDCmd.COMMAND_NAME)) {
            return getRequestIDDesc(req);
        } else if (cmd.equals(SearchByCampaignCmd.COMMAND_NAME)) {
            return getCampaignDesc(req);
        } else if (cmd.equals(SearchByDateCmd.COMMAND_NAME)) {
            return getDateDesc(req);
        } else if (cmd.equals(SearchByNaifIDCmd.COMMAND_NAME)) {
            return getNaifDesc(req);
        } else if (cmd.equals(SearchByObserverCmd.COMMAND_NAME)) {
            return getObserverDesc(req);
        } else if (cmd.equals(SearchByProgramCmd.COMMAND_NAME)) {
            return getProgramDesc(req);
        } else if (cmd.equals(AbstractSearchCmd.COMMAND_NAME)) {
            return getAbstractDesc(req);
        } else if (cmd.equals(SearchIrsEnhancedCmd.COMMAND_NAME)) {
            return getIrsEnhancedDesc(req);
        } else if (cmd.equals(SearchMOSCmd.COMMAND_NAME)) {
            return getMOSDesc(req);

        } else {
            return super.getDesc(req);
        }
    }

//====================================================================
//
//====================================================================

    private String getPositionDesc(Request req, String radiusKey) {
        String source;
        if (req.getParam(SearchByPositionCmd.UPLOADED_FILE_PATH) != null) {
            source = "Batch search";
        } else {
            source = req.getParam(SimpleTargetPanel.TARGET_NAME_KEY);
            if (source == null) {
                source = req.getParam(ReqConst.USER_TARGET_WORLD_PT);
            }
        }
        String radius = req.getParam(radiusKey);
        return source + " with a radius of " + toDegString(radius);
    }
    private String getRequestIDDesc(Request req) {
        String desc = req.getParam(SearchByRequestIDCmd.REQUESTID_KEY);
        if (desc.length() > 50) desc = desc.substring(0, 50)+"...";
        if (req.getBooleanParam(SearchByRequestIDCmd.INCLUDE_SAME_CONSTRAINTS_KEY)) { desc +=" (+)"; }
        return desc;
    }
    private String getCampaignDesc(Request req) {
        return req.getParam(SearchByCampaignCmd.CAMPAIGN_KEY);
    }
    private String getDateDesc(Request req) {
        return req.getParam(SearchByDateCmd.START_DATE_KEY) + ", " +
               req.getParam(SearchByDateCmd.END_DATE_KEY);
    }
    private String getNaifDesc(Request req) {
        String name = req.getParam(SearchByNaifIDCmd.TARGET_NAME_KEY);
        String retval;
        if (name == null) 
            name = "";
        String id = req.getParam(SearchByNaifIDCmd.NAIFID_KEY);
        if (name.length() ==0) {
           retval = id;
        } else {
           retval = name + " (" + id + ")";
        }
        return retval;
    }

    private String getObserverDesc(Request req) {
        return req.getParam(SearchByObserverCmd.OBSERVER_KEY);
    }
    private String getProgramDesc(Request req) {
        return req.getParam(SearchByProgramCmd.PROGRAM_KEY);
    }
    private String getIrsEnhancedDesc(Request req) {
        return req.getParam(SearchIrsEnhancedCmd.CONSTRAINTS_KEY);
    }
    private String getMOSDesc(Request req) {
        com.google.gwt.i18n.client.DateTimeFormat dateFormat = com.google.gwt.i18n.client.DateTimeFormat.getFormat("yyyy-MM-dd");
        String objType = req.getParam("obj_type_2");
        if (StringUtils.isEmpty(objType)) { objType = req.getParam("obj_type_3"); }
        objType = StringUtils.isEmpty(objType) ? "" : "(" + objType + ")";
        String objName = req.getParam("obj_name");
        objName = StringUtils.isEmpty(objName) ? "" : objName;
        String obsBegin = req.getParam("obs_begin");
        String obsEnd = req.getParam("obs_end");
        String obsPeriod = "";
        if (!StringUtils.isEmpty(obsBegin) && !StringUtils.isEmpty(obsBegin)) {
            try {
                obsPeriod = "; "+dateFormat.format(new Date(Long.parseLong(obsBegin)))+" to "+
                        dateFormat.format(new Date(Long.parseLong(obsEnd)));
            } catch (Exception e) {}
        }
        return objName + objType + obsPeriod;
    }
    private String getAbstractDesc(Request req) {
        return req.getParam(AbstractSearchCmd.SEARCH_FIELD_PROP);
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
