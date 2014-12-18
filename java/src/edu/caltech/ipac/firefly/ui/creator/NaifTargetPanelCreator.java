package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.NaifTargetPanel;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 25, 2010
 * Time: 5:34:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class NaifTargetPanelCreator implements FormWidgetCreator {
    public Widget create(Map<String, String> params) {

        return new NaifTargetPanel();
    }
}

