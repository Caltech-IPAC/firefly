package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 9/19/14
 * Time: 12:34 PM
 */


import edu.caltech.ipac.firefly.visualize.WebPlotView;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class SubgroupVisibilityController {

    public enum SubGroupLevels {ALL, SUBGROUP, PLOT_VIEW}

    private static final String NULL_SUBGROUP= "NULL_SUBGROUP";

    private Map<String,Boolean> subgroupVisibility = null;
    private Map<WebPlotView,Boolean> pvVisibility = null;
    private boolean forceAllVisible= false;
    private final DataConnection dataConnect;
    private boolean subGroupingEnabled= false;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public SubgroupVisibilityController(DataConnection dataConnect) {
        this.dataConnect = dataConnect;
        initSubgroupVisibility();
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void enableSubgrouping() {
        if (!subGroupingEnabled) {
            subGroupingEnabled= true;
            subgroupVisibility = new HashMap<String, Boolean>(7);
            pvVisibility = new HashMap<WebPlotView, Boolean>(23);
            subgroupVisibility.put(NULL_SUBGROUP,true);
        }
    }

    public boolean isForceAllVisible() {
        return forceAllVisible;
    }

    public void setForceAllVisible(boolean forceAllVisible) {
        this.forceAllVisible = forceAllVisible;
    }

    private void initSubgroupVisibility() {
        if (isUsingSubgroupVisibility()) {
            for(String sg : dataConnect.getDefaultSubgroupList()) {
                subgroupVisibility.put(sg, true);
            }
        }
    }

    public boolean isUsingSubgroupVisibility() {
        return subGroupingEnabled;
    }

    public boolean isSubgroupVisible(String subgroup) {
        boolean retval= false;
        if (isUsingSubgroupVisibility() && subgroupVisibility.containsKey(subgroup)) {
            if (subgroup==null) subgroup= NULL_SUBGROUP;
            retval= subgroupVisibility.get(subgroup);
        }
        return retval;
    }

    public void setSubgroupVisibility(String subgroup, boolean v) {
        enableSubgrouping();
        if (subgroup==null) subgroup= NULL_SUBGROUP;
        subgroupVisibility.put(subgroup,v);
    }

    public boolean isPlotViewVisible(WebPlotView pv) {
        boolean retval= false;
        if (isUsingSubgroupVisibility() && pvVisibility.containsKey(pv)) {
            retval= pvVisibility.get(pv);
        }
        return retval;
    }

    public void setPlotViewVisibility(WebPlotView pv, boolean v) {
        enableSubgrouping();
        pvVisibility.put(pv, v);
    }

    public boolean isVisibleAtAnyLevel(WebPlotView pv) {
        boolean retval= false;
        if (isUsingSubgroupVisibility()) {
            String subgroup= pv.getDrawingSubGroup();
            retval= forceAllVisible || isSubgroupVisible(subgroup) || isPlotViewVisible(pv);
        }
        return retval;
    }

    public SubGroupLevels getVisibilityLevel(WebPlotView pv) {
        SubGroupLevels retval= SubGroupLevels.PLOT_VIEW;

        if (forceAllVisible)                                 retval= SubGroupLevels.ALL;
        else if (isSubgroupVisible(pv.getDrawingSubGroup())) retval= SubGroupLevels.SUBGROUP;
        else if (isPlotViewVisible(pv))                      retval= SubGroupLevels.PLOT_VIEW;

        return retval;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
