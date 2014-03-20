package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.WebClassProperties;

/**
 * @author tatianag
 *         $Id: XYPlotOptionsDialog.java,v 1.22 2012/09/20 23:36:03 xiuqin Exp $
 */
public class XYPlotOptionsDialog {
    private static WebClassProperties _prop= new WebClassProperties(XYPlotOptionsDialog.class);
    private final PopupPane _popup;

    XYPlotOptionsPanel _panel;

    XYPlotOptionsDialog(XYPlotBasicWidget widget) {
        _panel = new XYPlotOptionsPanel(widget);
        _popup= new PopupPane(_prop.getTitle(),null, PopupType.STANDARD, false, false);
        _popup.setWidget(_panel);
     }

    public void setVisible(boolean v) {
        if (v) {
            _panel.setVisible(v);
            updateDialogAlignment();
            _popup.show();
            _popup.setAutoLocate(false);
        }
        else {
            _popup.hide();
        }
    }

    public boolean isVisible() {
        return _popup.isVisible();
    }

    public boolean setupError() {
        return _panel.setupError();
    }

    void updateDialogAlignment() {
        if (_popup!=null) {
            _popup.alignTo(RootPanel.get(), PopupPane.Align.TOP_LEFT, 10, 10);
            //if (Window.getClientWidth() > 1000 && Application.getInstance().getCreator().isApplication())
            //_popup.alignTo(RootPanel.get(), PopupPane.Align.DISABLE, 130, 10);
        }
    }

}
