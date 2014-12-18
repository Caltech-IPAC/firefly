package edu.caltech.ipac.firefly.core.background;

import java.io.Serializable;
/**
 * User: roby
 * Date: Nov 12, 2010
 * Time: 5:20:58 PM
 */


/**
* @author Trey Roby
*/
public class DummyBackgroundPart implements Serializable, BackgroundPart {

    private BackgroundState _state;
    public DummyBackgroundPart(BackgroundState state) { _state= state;}
    public DummyBackgroundPart() {}

    public BackgroundState getState() { return _state; }
    public boolean hasFileKey() { return false; }
    public String getFileKey() { return null; }
}

