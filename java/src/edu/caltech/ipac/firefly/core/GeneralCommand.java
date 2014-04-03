package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PropConst;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.dd.UIAttrib;
import edu.caltech.ipac.util.dd.UIAttributes;


public abstract class GeneralCommand implements UIAttributes, com.google.gwt.user.client.Command {

    public static final String STATUS_READY = "ready";
    public static final String STATUS_LOADING = "loading";
    public static final String PROP_INIT = "init";
    public static final String PROP_ENABLED = "prop.enabled";
    public static final String PROP_ATTENTION = "prop.attention";
    public static final String PROP_HIDDEN = "prop.hidden";
    public static final String PROP_TITLE = "prop.label";
    public static final String PROP_DESC = "prop.desc";
    public static final String PROP_ICON = "prop.icon";
    public static final String ICON_PRIOPERTY = "prop.icon.property";
    public static final String PROP_HIGHLIGHT = "prop.highlight";
    public static final String PROP_ICON_ONLY_HINT = "prop.icon.only.hint";

    private UIAttributes uiAttributes;
    private boolean isEnabled;
    private boolean hidden= false;
    private boolean isHighlighted;
    private boolean isAttention= false;
    private boolean iconOnlyHint= false;
    private boolean important= false;
    transient private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean isInit = false;
    private String iconProp= null;
    private boolean hasIconStatus= false;


    /**
     * create a now command with the given command name and read properties
     * @param name the command name
     */
    public GeneralCommand(String name) {
        initUIAttributes(null,true);
        setName(name);
    }

    /**
     * create a now command with the given command name and label and read properties
     * @param name the command name
     * @param label the Label for the command (what the user sees) this overrides a property
     setEnabled();
     */
    public GeneralCommand(String name, String label) {
        this(name);
        setLabel(label);
    }

    /**
     * create a new command without reading properties
     * @param name the command name
     * @param label the Label for the command (what the user sees)
     * @param shortDesc the tool tip
     * @param enabled true if this is enabled
     */
    public GeneralCommand(String name, String label, String shortDesc, boolean enabled) {
        this(new UIAttrib(name, label, shortDesc, shortDesc, null), enabled);
    }

    /**
     * create a new command without reading properties
     * @param enabled true if this is enabled
     */
    public GeneralCommand(UIAttributes attributes, boolean enabled) {
         initUIAttributes(attributes,enabled);
    }

    private void initUIAttributes(UIAttributes attributes, boolean enabled) {
        uiAttributes = attributes == null ? new UIAttrib() : attributes;
        isEnabled = enabled;
    }





    public String getName() {
        return uiAttributes.getName();
    }

    public String getLabel() {
        return uiAttributes.getLabel();
    }

    public void setLabel(String label) {
        String oldVal = uiAttributes.getLabel();
        uiAttributes.setLabel(label);
        pcs.firePropertyChange(PROP_TITLE, oldVal, label);
    }

    public String getDesc() {
        return uiAttributes.getDesc();
    }

    public void setDesc(String desc) {
        String oldVal = uiAttributes.getDesc();
        uiAttributes.setDesc(desc);
        pcs.firePropertyChange(PROP_DESC, oldVal, desc);
    }

    public void setName(String name) {
        if (name != null) {
            String oldCommand= uiAttributes.getName();
            if (oldCommand == null || !oldCommand.equals(name)) {
                uiAttributes.setName(name);
                addProperties();
            }
        }
        newCommandNameSet();
    }

    protected void newCommandNameSet() {}

    public String getShortDesc() {
        return uiAttributes.getShortDesc();
    }

    public void setShortDesc(String shortDesc) {
        uiAttributes.setShortDesc(shortDesc);
    }

    public String getIcon() {
        return uiAttributes.getIcon();
    }

    public void setIcon(String icon) {
        String oldVal = uiAttributes.getIcon();
        uiAttributes.setIcon(icon);
        pcs.firePropertyChange(PROP_ICON, oldVal, icon);
    }

    public String getIconProperty() { return iconProp; }

    public void setIconProperty(String ip) {
        String oldIp= iconProp;
        iconProp= ip;
        pcs.firePropertyChange(ICON_PRIOPERTY, oldIp, ip);
        String icon= Application.getInstance().getProperties().getProperty(iconProp,null);
        uiAttributes.setIcon(icon);
    }

    public boolean isIE6IconBundleSafe() { return false; }

    public boolean isIconOnlyHint() {
        return iconOnlyHint;
    }

    public boolean hasIcon() { return hasIconStatus; }

    public void setIconOnlyHint(boolean iconOnly) {
        iconOnlyHint= iconOnly;
    }

    public void setImportant(boolean important) { this.important= important; }
    public boolean isImportant() { return important; }

    public Image createImage() {
//        Image retval;
//        if (!isIE6IconBundleSafe() && BrowserUtil.isIE() && BrowserUtil.getMajorVersion()<=6) {
//            retval= new Image(getIcon());
//        }
//        else {
//            retval= createCmdImage();
//        }
//        return retval;
        return createCmdImage();
    }

    protected Image createCmdImage() {
        String icon= getIcon();
        return icon==null ? null : new Image(icon);
    }

    public void setHighlighted(boolean highlight) {
        boolean oldVal = this.isEnabled;
        isHighlighted = highlight;
        pcs.firePropertyChange(PROP_HIGHLIGHT, oldVal, highlight);
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }


    public boolean isHidden() { return hidden; }
    public boolean isEnabled() { return isEnabled; }
    public boolean isAttention() { return isAttention; }

    public void setEnabled(boolean enabled) {
        boolean oldVal = this.isEnabled;
        isEnabled = enabled;
        pcs.firePropertyChange(PROP_ENABLED, oldVal, enabled);
    }

    public void setAttention(boolean attention) {
        boolean oldVal = this.isAttention;
        isAttention = attention;
        pcs.firePropertyChange(PROP_ATTENTION, oldVal, attention);
    }

    public void setHidden(boolean hidden) {
        boolean oldVal = this.hidden;
        this.hidden = hidden;
        pcs.firePropertyChange(PROP_HIDDEN, oldVal, hidden);
    }

    protected void setInit(boolean init) {
        boolean oldval = isInit;
        isInit = init;
        pcs.firePropertyChange(PROP_INIT, oldval, init);
    }

    public boolean isInit() {
        return isInit;
    }
    
    protected boolean init() {
        return true;
    }

    public void execute() {
        if (isEnabled) {
            GWT.runAsync(new GwtUtil.DefAsync() {
                public void onSuccess() {
                    if (!isInit()) {
                        setInit(init());
                    }
                    doExecute();
                }
            });
        }
    }




    protected void addProperties() {
        String value;

        String command= uiAttributes.getName();

        value=WebProp.getTitle(command);
        if(value!=null) setLabel(value);

        value=WebProp.getTip(command);
        if (value!=null) setShortDesc(value);

        value=WebProp.getTip(command);
        if (value!=null) setShortDesc(value);

        boolean hint=WebProp.getIconOnlyHint(command,iconOnlyHint);
        setIconOnlyHint(hint);
        if (hint) hasIconStatus= true;

        boolean important=WebProp.isImportant(command,false);
        setImportant(important);

        if (iconProp==null) iconProp= command + "." + PropConst.ICON;
        setIconProperty(iconProp);

    }



//====================================================================
//  PropertyChange aware
//====================================================================

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

    public void addPropertyChangeListener(String propName, PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(propName, pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}


//====================================================================
//  Abstract methods.  Must implement
//====================================================================

    protected abstract void doExecute();

}