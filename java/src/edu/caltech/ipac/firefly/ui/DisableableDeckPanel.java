package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.util.StringUtils;
/**
 * User: roby
 * Date: Mar 23, 2010
 * Time: 11:03:57 AM
 */


/**
 * @author Trey Roby
 */


public class DisableableDeckPanel extends DeckPanel implements ProvidesResize, RequiresResize {
    private final MaskMessgeWidget _maskMess;
    private final DisabledPane _noDataView;
    private final int _noDataIdx;
    private final int _blankPanelIdx;
    private final String _defMessage;

    public DisableableDeckPanel(String defMessage) {

        _defMessage= defMessage;
        _maskMess = new MaskMessgeWidget(defMessage, false);
        _noDataView= new DisabledPane(_maskMess);
        AbsolutePanel _blankPanel = new AbsolutePanel();

        this.addStyleName("expand-fully");

        add(_noDataView);
        add(_blankPanel);

        _noDataIdx = getWidgetIndex(_noDataView);
        _blankPanelIdx = getWidgetIndex(_blankPanel);
    }


    @Override
    public void add(Widget w) {
        super.add(w);
//        GwtUtil.setStyle(w, "position", "absolute");
    }

    public void onResize() {
        int idx= getVisibleWidget();
        if (idx>=0) {
            Widget w= this.getWidget(idx);
            if (w instanceof RequiresResize) ((RequiresResize)w).onResize();
        }
    }


    public void showNoData(String mess) {
        if (StringUtils.isEmpty(mess)) {
            showWidget(_blankPanelIdx);
        } else {
            _maskMess.setHTML(mess);
            showWidget(_noDataIdx);
            _noDataView.update();
        }
    }

    public void showNoData() {
        showNoData(_defMessage);
    }

    public String getDefaultMsg() { return _defMessage; }

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
