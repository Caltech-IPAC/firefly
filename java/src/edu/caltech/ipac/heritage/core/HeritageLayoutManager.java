package edu.caltech.ipac.heritage.core;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.ResizableLayoutManager;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.heritage.ui.image.HeritageImages;

/**
 * Date: Jun 11, 2008
 *
 * @author loi
 * @version $Id: HeritageLayoutManager.java,v 1.43 2011/10/21 00:14:03 loi Exp $
 */
public class HeritageLayoutManager extends ResizableLayoutManager {

    public HeritageLayoutManager() {
        super();
    }

    public void layout(String root) {
        super.layout(root);
        RootPanel help = RootPanel.get("mission-help");

        if (help != null) {
            Anchor link = new Anchor("Spitzer Help","javascript:top.window.ffProcessRequest('id=overviewHelp')");
            link.setWidth("98%");
            help.add(link);
            DOM.setStyleAttribute(link.getElement(), "borderBottom", "1px solid white");
            DOM.setStyleAttribute(link.getElement(), "paddingBottom", "7px");
            DOM.setStyleAttribute(link.getElement(), "marginBottom", "-2px");
        }

        Image spitzerLogo = BrowserUtil.isBrowser(Browser.IE) ?
                    new Image("images/spitzer_logo_x40.gif") :
                    HeritageImages.Creator.getInstance().getSpitzerLogoX40().createImage();
        getSmallIcon().setDisplay(spitzerLogo);

//        HtmlRegionLoader f = new HtmlRegionLoader();
//        f.load("irsa_footer.html", LayoutManager.FOOTER_REGION);
    }

//    @Override
//    protected Widget makeNorth() {
//
//        final Region download = getDownload();
//        Image spitzerLogo = BrowserUtil.isBrowser(Browser.IE) ?
//                            new Image("images/spitzer_logo_x40.gif") :
//                            HeritageImages.Creator.getInstance().getSpitzerLogoX40().createImage();
//
////        HorizontalPanel bottom= new HorizontalPanel();
//        DockLayoutPanel  bottom= new DockLayoutPanel(Style.Unit.PX);
//        bottom.addEast(download.getDisplay(), 300);
//        bottom.add(new QuickNavPanel());
//        bottom.setWidth("100%");
//        bottom.setHeight("30px");
//
//        FlowPanel flow = new FlowPanel();
//        flow.setWidth("100%");
//        flow.add(getMenu().getDisplay());
//        flow.add(bottom);
//
//        HorizontalPanel north = new HorizontalPanel();
//        north.add(spitzerLogo);
//        north.add(GwtUtil.getFiller(25, 1));
//        north.add(flow);
//        north.setCellWidth(flow,"100%");
//        north.setWidth("100%");
//
//
//        return north;
//    }

    @Override
    protected Widget makeSouth() {
        Widget s = super.makeSouth();
        HtmlRegionLoader footer= new HtmlRegionLoader();
        footer.load("irsa_footer.html",LayoutManager.FOOTER_REGION);
        return s;
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
