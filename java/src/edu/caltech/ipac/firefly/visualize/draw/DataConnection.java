package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.WebPlot;

import java.util.List;
/**
 * User: roby
 * Date: Jul 16, 2010
 * Time: 12:51:58 PM
 */


/**
* @author Trey Roby
*/
public interface DataConnection {
    String getTitle();
    List<DrawObj> getData(boolean rebuild);
    List<DrawObj> getData(boolean rebuild, WebPlot plot);
    public DrawConnector getDrawConnector();
    public int size();
    public boolean isActive();
    public boolean isDataVisible();
    public void setHighlightedIdx(int... idx);
    public int[] getHighlightedIdx();
    public void showDetails(int x, int y, int index);
    public void hideDetails();
    public boolean getSupportsSelection();
    public boolean getSupportsMouse();
    public boolean getOnlyIfDataVisible();
    public boolean getHasVeryLittleData();
    public boolean getHasPerPlotData();
    public boolean isPointData();
    public WebEventManager getEventManager();
    public String getInitDefaultColor();
    public String getHelpLine();
    public AsyncDataLoader getAsyncDataLoader();
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
