/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core.background;

import java.io.Serializable;
/**
 * User: roby
 * Date: Aug 20, 2010
 * Time: 12:04:38 PM
 */


/**
 * @author Trey Roby
 */
public class DefaultBackgroundPart implements BackgroundPart, Serializable {

    private BackgroundState _state;

    public DefaultBackgroundPart() { this(BackgroundState.STARTING); }
    public DefaultBackgroundPart(BackgroundState s) { _state= s; }

    public BackgroundState getState() { return _state; }
    public void setState(BackgroundState s) {_state= s;}

    public boolean hasFileKey() { return false;}
    public String getFileKey() { return null;}

}

