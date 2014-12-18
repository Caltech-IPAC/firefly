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
