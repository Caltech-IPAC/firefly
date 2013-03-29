package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.ZoomUtil;


/**
 * User: roby
 * Date: Feb 26, 2009
 * Time: 1:50:08 PM
 */


/**
 * @author Trey Roby
 */
public class ZoomOptionsPopup {

    private static final float _levels[]= VisUtil.getPossibleZoomLevels();

    private static final double MAX_AREA= 15000D*15000D;

    private static WebClassProperties _prop= new WebClassProperties(ZoomOptionsPopup.class);
    private static final String _tipBase=  _prop.getTip("zoom");
    private final PopupPane _popup;
    private final Widget _parent;
    private final VerticalPanel _levelBox= new VerticalPanel();
    private Label _messageLabel= new Label();

    private static ZoomOptionsPopup _zoomOpsPopout;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ZoomOptionsPopup(String         message,
                            Widget         parent,
                            boolean        atMax) {
        _parent= parent;
        _popup= new PopupPane(_prop.getTitle(),null, PopupType.STANDARD, true,false);

        layout(message,atMax);
    }

    public static void showZoomOps() {
        showZoomOps(null,false);
    }

    public static void showZoomOps(String message, boolean atMax) {

        if (_zoomOpsPopout==null) {
            WebPlotView pv= AllPlots.getInstance().getMiniPlotWidget().getPlotView();
            _zoomOpsPopout= new ZoomOptionsPopup(message, pv,atMax);
        }
        else {
            _zoomOpsPopout.setMessage(message);
            _zoomOpsPopout.updateLevels(atMax);
        }
        _zoomOpsPopout.setVisible(true);
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void setVisible(boolean v) {
        if (v) {
            _popup.alignTo(_parent, PopupPane.Align.TOP_LEFT);
            PopupPane pp= AllPlots.getInstance().getMenuBarPopup();
            if (pp!=null) _popup.alignTo(pp.getPopupPanel(), PopupPane.Align.BOTTOM_CENTER);
            else          _popup.alignToCenter();
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

    private void setMessage(String message) {
        _messageLabel.setText(message);
        GwtUtil.setStyle(_messageLabel,"paddingBottom", message!=null ? "10px" : "0");
    }

    private void updateLevels(boolean atMax) {
        float currLevel= AllPlots.getInstance().getMiniPlotWidget().getCurrentPlot().getZoomFact();
        _levelBox.clear();
        for(float level : _levels) {
            if (atMax) {
                if (level<=currLevel) _levelBox.add(makeZoomButton(level,level==currLevel));
            }
            else if (canGo(level)) {
                _levelBox.add(makeZoomButton(level, level==currLevel));
            }
        }
    }

    private void layout(String message,boolean atMax) {

        VerticalPanel vbox= new VerticalPanel();
        vbox.setSpacing(5);

        setMessage(message);
        vbox.add(_messageLabel);

        _levelBox.setSpacing(3);
        updateLevels(atMax);

        vbox.add(GwtUtil.centerAlign(_levelBox));

        _popup.setWidget(GwtUtil.centerAlign(vbox));
    }



    private Widget makeZoomButton(final float level, boolean current) {

        String levelStr= ZoomUtil.convertZoomToString(level);
        Label w;
        if (current) {
            levelStr+= "&nbsp;&nbsp;&nbsp;<<&nbsp;Current";
            w= new Label(levelStr);
        }
        else {
            w= GwtUtil.makeLinkButton(levelStr, _tipBase+ " " + levelStr,
                                            new ClickHandler() {
                                                public void onClick(ClickEvent ev) {
                                                    _popup.hide();
                                                    ZoomUtil.zoomGroupTo(level);
                                                }
                                            });
        }
        DOM.setInnerHTML(w.getElement(),levelStr);
        return w;
    }

    public boolean canGo(float level) {
        boolean retval= true;

        WebPlot p= AllPlots.getInstance().getMiniPlotWidget().getCurrentPlot();
        if (p!=null) {
            float w= p.getImageDataWidth()*level;
            float h= p.getImageDataHeight()*level;
            retval= (w*h < MAX_AREA);
        }
        return retval;
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
