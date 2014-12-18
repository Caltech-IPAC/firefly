package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.SimpleTargetPanel;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Nov 25, 2010
 * Time: 5:34:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleTargetPanelCreator  implements FormWidgetCreator {
    public Widget create(Map<String, String> params) {

        SimpleTargetPanel panel;
        if (params.containsKey(CommonParams.RESOLVERS)) {
            List<String> rList= DataViewCreator.splitAndTrim(params.get(CommonParams.RESOLVERS));
            panel= new SimpleTargetPanel(rList.toArray(new String[rList.size()]));
        }
        else {
            panel= new SimpleTargetPanel();
        }

        return panel;
    }
}

