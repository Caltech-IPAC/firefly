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
public class SubgroupVisController {

    public enum Level {ALL, SUBGROUP, PLOT_VIEW}
    public enum AllVisibility {ALL_VISIBLE, ALL_INVISIBLE, OFF}

    private static final String NULL_SUBGROUP= "NULL_SUBGROUP";

    private Map<String,Boolean> subgroupVisibility = null;
    private Map<WebPlotView,Boolean> pvVisibility = null;
    private AllVisibility forceAllVisible= AllVisibility.OFF;
    private DataConnection dataConnect;
    private boolean subGroupingEnabled= false;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public SubgroupVisController() {
        subgroupVisibility = new HashMap<String, Boolean>(7);
        pvVisibility = new HashMap<WebPlotView, Boolean>(23);
        subgroupVisibility.put(NULL_SUBGROUP, true);
    }

    public void setDataConnect(DataConnection dataConnect) {
        this.dataConnect = dataConnect;
        if (dataConnect!=null) initSubgroupVisibility();
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void enableSubgroupingIfSupported() {
        if (!subGroupingEnabled && dataConnect!=null && dataConnect.getOKForSubgroups() ) {
            subGroupingEnabled= true;
        }
    }


    private void initSubgroupVisibility() {
        if (dataConnect.getOKForSubgroups()) {
            if (dataConnect.getDefaultSubgroupList()!=null) {
                for(String sg : dataConnect.getDefaultSubgroupList()) {
                    subgroupVisibility.put(sg, true);
                }
            }
            else {
                forceAllVisible= AllVisibility.ALL_INVISIBLE;
            }
        }
    }


    public boolean isUsingSubgroupVisibility() {
        return subGroupingEnabled;
    }

    public boolean isSubgroupVisible(String subgroup) {
        boolean retval= false;
        if (subgroup==null) subgroup= NULL_SUBGROUP;
        if (subgroupVisibility.containsKey(subgroup)) {
            retval= subgroupVisibility.get(subgroup);
        }
        return retval;
    }

    public boolean containsSubgroupKey(String subgroup) {
        if (subgroup==null) subgroup= NULL_SUBGROUP;
        return subgroupVisibility.containsKey(subgroup);
    }

    public boolean isInSubgroup(String testSubGroup, WebPlotView pv) {
        String subgroup= pv.getDrawingSubGroup();
        if (subgroup==null) subgroup= NULL_SUBGROUP;
        if (testSubGroup==null) testSubGroup= NULL_SUBGROUP;
        return subgroup.equals(testSubGroup);
    }

    public void setSubgroupVisibility(String subgroup, boolean v) {
        if (subgroup==null) subgroup= NULL_SUBGROUP;
        subgroupVisibility.put(subgroup,v);
    }

    public void removeSubgroupVisibilityKey(String subgroup) {
        if (subgroup==null) subgroup= NULL_SUBGROUP;
        subgroupVisibility.remove(subgroup);
    }

    public boolean isPlotViewVisible(WebPlotView pv) {
        boolean retval= false;
        if (pvVisibility.containsKey(pv)) {
            retval= pvVisibility.get(pv);
        }
        return retval;
    }

    public void setVisibility(Level level, boolean v, WebPlotView pv) {
        if (level==Level.ALL) {
            forceAllVisible= v ? AllVisibility.ALL_VISIBLE : AllVisibility.ALL_INVISIBLE;
        }
        else {
            forceAllVisible= AllVisibility.OFF;
        }
        if (pv!=null) {
            if (level== Level.SUBGROUP) {
                setSubgroupVisibility(pv.getDrawingSubGroup(), v);
            }
            else if (level== Level.PLOT_VIEW) {
                removeSubgroupVisibilityKey(pv.getDrawingSubGroup());
                setPlotViewVisibility(pv, v);
            }
        }
    }

    public void setPlotViewVisibility(WebPlotView pv, boolean v) {
        pvVisibility.put(pv, v);
    }

    public void clearPlotView(WebPlotView pv) {
        pvVisibility.remove(pv);
    }

    public boolean containPlotView(WebPlotView pv) { return pvVisibility.containsKey(pv); }

//    public boolean isVisibilityAffected(WebPlotView pv, String subgroup, Level level) {
//        boolean retval= false;
//        if (isUsingSubgroupVisibility()) {
//            switch (level) {
//                case ALL:
//                    retval= true;
//                    break;
//                case SUBGROUP:
//                    retval= isInSubgroup(subgroup,pv);
//                    break;
//                case PLOT_VIEW:
//                    retval= true;
//                    break;
//            }
//        }
//        else {
//            retval= true;
//        }
//        return retval;
//    }



    public boolean isVisibleAtLevel(WebPlotView pv, Level level, boolean fallbackVisibility) {
        boolean retval= fallbackVisibility;
        if (isUsingSubgroupVisibility()) {
            String subgroup= pv.getDrawingSubGroup();

            if (level==Level.PLOT_VIEW) {
                retval= isPlotViewVisible(pv);
            }
            else if (level==Level.SUBGROUP) {
                retval= isSubgroupVisible(subgroup);
            }
            else if (level==Level.ALL) {
                retval= forceAllVisible==AllVisibility.ALL_VISIBLE;
            }
        }
        return retval;
    }


    public boolean isVisibleAtAnyLevel(WebPlotView pv, boolean fallbackVisibility) {
        boolean retval= fallbackVisibility;
        if (isUsingSubgroupVisibility()) {
            String subgroup= pv.getDrawingSubGroup();
            retval= forceAllVisible==AllVisibility.ALL_VISIBLE || isSubgroupVisible(subgroup) || isPlotViewVisible(pv);
        }
        return retval;
    }

    public Level getVisibilityLevel(WebPlotView pv) {
        Level retval= Level.PLOT_VIEW;

        if (forceAllVisible!=AllVisibility.OFF)                retval= Level.ALL;
        else if (containsSubgroupKey(pv.getDrawingSubGroup())) retval= Level.SUBGROUP;
        else if (isPlotViewVisible(pv))                      retval= Level.PLOT_VIEW;

        return retval;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

}

