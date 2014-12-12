package edu.caltech.ipac.util.dd;

import edu.caltech.ipac.util.StringUtils;

/**
 * Base Implementation.
 * Validate() returns true:  no validation
 * getDefValueAsString() returns getDefValue().toString().
 */
public class StringFieldDef implements FieldDef {
    private UIAttrib uiAttributes = new UIAttrib();
    private String id;
    private String defValue;
	private String errMsg;
	private int maxWidth;
	private int preferWidth;
	private boolean isNullAllow;
	private String mask;
    private String prefKey;
    private boolean textImmutable;
    private transient PreferenceRetrieve prefRetrieve;


    public StringFieldDef() {
    }

    public StringFieldDef(String name) {
        this(name, name, "", "", Integer.MAX_VALUE, 100, "", true, null);
    }

    public StringFieldDef(String name, String label, String errMsg, String tips, int maxWidth, int preferWidth, String defValue, boolean nullAllow, String mask) {
        uiAttributes = new UIAttrib(name, label, tips, tips, null);
        this.errMsg = errMsg;
        this.maxWidth = maxWidth;
        this.preferWidth = preferWidth;
        this.defValue = defValue;
        isNullAllow = nullAllow;
        this.mask = mask;
        this.id = name;
    }

    public String getId() {
        if (id == null) {
            id = uiAttributes.getName();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean validateSoft(Object aValue) throws ValidationException { return validate(aValue); }

    public boolean validate(Object aValue) throws ValidationException {
        String v = aValue.toString();
        if (StringUtils.isEmpty(v)) {
            if (!isNullAllow()) {
                return false;
            }
        } else {
            if (mask != null) {
                if (!v.matches(mask)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isUsingPreference() { return prefKey!=null && prefRetrieve!=null; }
    public String getPreferenceKey() { return prefKey; }

    public void setPreferenceKey(String prefKey) { this.prefKey= prefKey; }

    public void setDefaultValue(String defValue) {
        this.defValue = defValue;
    }

    public String getDefaultValueAsString() {
        String retval= isUsingPreference() ? prefRetrieve.getPref(prefKey) : defValue;
        // do we have situations when defValue is not empty, but preference value is empty?
        if (StringUtils.isEmpty(retval)) retval= defValue;
        return retval;
    }

    public String getName() {
        return uiAttributes.getName();
    }

    public void setName(String name) {
        uiAttributes.setName(name);
        if (id == null) {
            id = name;
        }
    }

    public String getLabel() {
        return uiAttributes.getLabel();
    }

    public void setLabel(String label) {
        uiAttributes.setLabel(label);
    }

    public String getDesc() {
        return uiAttributes.getDesc();
    }

    public void setDesc(String desc) {
        uiAttributes.setDesc(desc);
    }

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
        uiAttributes.setIcon(icon);
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getPreferWidth() {
        return preferWidth;
    }

    public void setPreferWidth(int preferWidth) {
        this.preferWidth = preferWidth;
    }

    public boolean isNullAllow() {
        return isNullAllow;
    }

    public void setNullAllow(boolean nullAllow) {
        isNullAllow = nullAllow;
    }

    public boolean isTextImmutable() { return textImmutable; }

    public void setTextImmutable(boolean textImmutable) {
        this.textImmutable= textImmutable;
    }


    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public String getDefaultMask() { return null; }

    public void setPreferenceRetrieve(PreferenceRetrieve pr) { prefRetrieve= pr; }

    public interface PreferenceRetrieve {
        public String getPref(String key);
    }
}