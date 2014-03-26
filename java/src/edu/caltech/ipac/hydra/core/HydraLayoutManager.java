package edu.caltech.ipac.hydra.core;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.core.layout.IrsaLayoutManager;
import edu.caltech.ipac.firefly.core.layout.ResizableLayoutManager;

/**
 * Date: Jun 11, 2008
 *
 * @author loi
 * @version $Id: HydraLayoutManager.java,v 1.20 2011/12/12 17:36:36 roby Exp $
 */
public class HydraLayoutManager extends IrsaLayoutManager {

    public HydraLayoutManager() {
        super();
    }

    public void layout(String root) {
        super.layout(root);
//        BackgroundManager bMan = Application.getInstance().getBackgroundManager();
//        getDownload().setDisplay(bMan);
        RootPanel help = RootPanel.get("mission-help");

        if (help != null) {
            Anchor link = new Anchor("Spitzer Help", "javascript:top.window.ffProcessRequest('id=overviewHelp')");
            link.setWidth("98%");
            help.add(link);
            DOM.setStyleAttribute(link.getElement(), "borderBottom", "1px solid white");
            DOM.setStyleAttribute(link.getElement(), "paddingBottom", "7px");
            DOM.setStyleAttribute(link.getElement(), "marginBottom", "-2px");
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
