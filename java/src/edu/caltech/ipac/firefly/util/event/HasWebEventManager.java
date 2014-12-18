package edu.caltech.ipac.firefly.util.event;

/**
 * User: roby
 * Date: 4/23/13
 * Time: 2:12 PM
 */
public interface HasWebEventManager {

    public WebEventManager getEventManager();
    public void addListener(WebEventListener l);
    public void addListener(Name eventName, WebEventListener l);
    public void removeListener(WebEventListener l);
    public void removeListener(Name eventName, WebEventListener l);
    public void fireEvent(WebEvent ev);
}
