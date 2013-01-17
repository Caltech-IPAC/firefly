package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotRequestHistory;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;


/**
 * User: roby
 * Date: Feb 26, 2009
 * Time: 1:50:08 PM
 */



/**
 * @author Trey Roby
 */
public class LockOptionsPopup {

    interface PFile extends WebAppProperties.PropFile { @ClientBundle.Source("LockOptionsPopup.prop") TextResource get(); }

    public enum DialogType {LOCK_NEW, LOCK_CHANGE}
    private static final int SHOW_MAX= 4;

    private static WebClassProperties _prop= new WebClassProperties(LockOptionsPopup.class, (PFile) GWT.create(PFile.class));
    private static final String USE= _prop.getName("use");
    private final PopupPane _popup;
    private final Widget _parent;
    private final MiniPlotWidget _mpw;
    private final DialogType  _type;
    private final ImageSelectDialog.AsyncCreator _asyncCreator;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public LockOptionsPopup(MiniPlotWidget mpw,
                            Widget parent,
                            DialogType  type) {
        _mpw= mpw;
        _parent= parent;
        _type= type;
        _popup= new PopupPane(_prop.getTitle(),null, PopupType.STANDARD, false,true);

        AsyncCallback<WebPlot> dialogCallback= new AsyncCallback<WebPlot>() {
            public void onFailure(Throwable caught) { }
            public void onSuccess(WebPlot result) { _mpw.getPlotView().setLockedHint(true); }
        };


        _asyncCreator = new ImageSelectDialog.AsyncCreator(_mpw,null,true,dialogCallback);

        layout();



    }


    public static void showNewLock(MiniPlotWidget mpw) {
        new LockOptionsPopup(mpw,mpw.getPlotView(),DialogType.LOCK_NEW).setVisible(true);
    }

    public static void showAdjustLock(MiniPlotWidget mpw) {
        new LockOptionsPopup(mpw,mpw.getPlotView(),DialogType.LOCK_CHANGE).setVisible(true);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setVisible(boolean v) {
        if (v) {
            _popup.alignTo(_parent, PopupPane.Align.TOP_LEFT);
            _popup.show();
        }
        else {
            _popup.hide();
        }
    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void layout() {

        PlotRequestHistory history= PlotRequestHistory.instance();

        VerticalPanel vbox= new VerticalPanel();
        vbox.setSpacing(5);

        if (_type==DialogType.LOCK_CHANGE) {
            Widget unlock= GwtUtil.makeLinkButton(_prop.makeBase("unlock"),new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    _popup.hide();
                    _mpw.getPlotView().setLockedHint(false);

                }
            });
            vbox.add(unlock);
        }


        if (_type==DialogType.LOCK_NEW) {
            Widget lockCurrent= GwtUtil.makeLinkButton(_prop.makeBase("lockCurrent"),new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    _popup.hide();
                    _mpw.getPlotView().setLockedHint(true);
                    WebPlot p= _mpw.getPlotView().getPrimaryPlot();
                    if (p!=null) {
                        WebPlotRequest request= p.getPlotState().getWebPlotRequest(Band.NO_BAND);
                        PlotRequestHistory.instance().add(request);
                    }
                }
            });
            vbox.add(lockCurrent);
        }


        Widget selectNew= GwtUtil.makeLinkButton(_prop.makeBase("selectNew"),new ClickHandler() {
           public void onClick(ClickEvent ev) {
               _asyncCreator.show();
               _popup.hide();
            }
        });
        vbox.add(selectNew);

        if (history.size()>=1) {
            Widget lastImage= GwtUtil.makeLinkButton(_prop.makeBase("lastImage"),
                                                     new PlotLinkHandler(history.getLast()) );
            vbox.add(lastImage);
        }

        int max= history.size()>SHOW_MAX ? SHOW_MAX-1 : history.size()-1;

        WebPlotRequest request;
        String desc;
        for(int i= 0; (i<max); i++) {
            request= history.get(i);
            desc= request.getUserDesc();
            Widget image= GwtUtil.makeLinkButton(USE + desc, desc,
                               new PlotLinkHandler(request));
            vbox.add(image);
        }


        _popup.setWidget(vbox);
    }



// =====================================================================
// -------------------- Inner classes --------------------------------
// =====================================================================

    private class PlotLinkHandler implements ClickHandler {

        final WebPlotRequest _request;

        PlotLinkHandler (WebPlotRequest request) {
           _request= request;
        }

        public void onClick(ClickEvent ev) {
            _popup.hide();
            _mpw.getOps().plot(_request,true,null);
            _mpw.getPlotView().setLockedHint(true);
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
