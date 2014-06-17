package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundActivation;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.MonitorItem;

/**
 *
 * @author loi
 * @version $Id: SearchActivation.java,v 1.4 2010/09/15 19:32:17 loi Exp $
 */
public class SearchActivation implements BackgroundActivation {

    public SearchActivation() {
    }

    public Widget buildActivationUI(MonitorItem monItem, int idx, boolean markAlreadyActivated) {

       String text=  "Show search results for " + monItem.getTitle();
       String tip =  "Show the search results for " + monItem.getTitle();
        return UIBackgroundUtil.buildActivationUI(text, tip, monItem, idx,
                                                  this,markAlreadyActivated);
    }

    public void activate(MonitorItem monItem, int idx, boolean byAutoActivation) {
        monItem.setActivated(0,true);
        BackgroundStatus bgStat = monItem.isComposite() ? monItem.getCompositeJob().getPartList().get(0) :
                                                          monItem.getStatus();
        if (bgStat.getBackgroundType()== BackgroundStatus.BgType.SEARCH) {
            Application.getInstance().processRequest(bgStat.getClientRequest());
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

