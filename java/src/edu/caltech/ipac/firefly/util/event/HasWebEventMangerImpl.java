package edu.caltech.ipac.firefly.util.event;
/**
 * User: roby
 * Date: 4/23/13
 * Time: 2:15 PM
 */


/**
 * @author Trey Roby
 */
public class HasWebEventMangerImpl implements HasWebEventManager {

    private final WebEventManager manager;

    public HasWebEventMangerImpl(WebEventManager manager) { this.manager = manager; }

    public WebEventManager getEventManager() { return manager; }
    public void addListener(WebEventListener l) { manager.addListener(l); }
    public void addListener(Name eventName, WebEventListener l) { manager.addListener(eventName,l); }
    public void removeListener(WebEventListener l) { manager.removeListener(l); }
    public void removeListener(Name eventName, WebEventListener l) { manager.addListener(eventName,l); }
    public void fireEvent(WebEvent ev) { manager.fireEvent(ev); }
}

