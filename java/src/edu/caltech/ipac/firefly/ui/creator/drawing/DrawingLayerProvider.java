package edu.caltech.ipac.firefly.ui.creator.drawing;
/**
 * User: roby
 * Date: 2/21/12
 * Time: 9:33 AM
 */


import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;

import java.util.Map;

/**
 * @author Trey Roby
 */
public interface DrawingLayerProvider extends EventWorker {


    void setEnabled(boolean enabled);
    boolean isEnabled();
    void setEnablingPreferenceKey(String pref);
    String getEnablingPreferenceKey();
    void activate(Object source, Map<String, String> params);


}

