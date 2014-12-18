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
