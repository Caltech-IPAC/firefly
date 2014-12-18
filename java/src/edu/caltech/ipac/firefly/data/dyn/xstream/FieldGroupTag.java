package edu.caltech.ipac.firefly.data.dyn.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.util.dd.UIComponent;

import java.util.ArrayList;
import java.util.List;

// custom converter used (FieldGroupConverter) - no annotations needed within class
@XStreamAlias("FieldGroup")
public class FieldGroupTag extends XidBaseTag {

    // xml attribute 'type'
    protected String type;

    // xml attribute 'typeName'
    protected String typeName;

    // xml attribute 'direction'
    protected String direction;

    // xml attribute 'labelWidth'
    protected String labelWidth;

    // xml attribute 'align'
    protected String align;

    // xml attribute 'height'
    protected String height;

    // xml attribute 'width'
    protected String width;

    // xml attribute 'spacing'
    protected String spacing;

    // xml attribute 'downloadRestriction'
    protected String downloadRestriction;


    // xml element 'Title'
    protected String title;

    // xml element 'Tooltip'
    protected String tooltip;

    // xml element 'Access'
    protected AccessTag access;

    // xml element 'FieldGroup|PreDefField|Label|String|Date|DEGREE|Double|Float|EMail|EnumString|Integer|Password|Pattern|Lat|Lon|MultiCoord)*'
    protected List<UIComponent> uiComponents;


    public String getType() {
        if (type == null) {
            return DynUtils.DEFAULT_FIELD_GROUP_TYPE;
        } else {
            return type;
        }
    }

    public void setType(String value) {
        type = value;
    }


    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String value) {
        typeName = value;
    }


    public String getDirection() {
        if (direction == null) {
            return DynUtils.DEFAULT_FIELD_GROUP_DIRECTION;
        } else {
            return direction;
        }
    }

    public void setDirection(String value) {
        direction = value;
    }


    public int getLabelWidth() {
        if (labelWidth == null) {
            return DynUtils.DEFAULT_FIELD_GROUP_LABEL_WIDTH;
        } else {
            return Integer.parseInt(labelWidth);
        }
    }

    public void setLabelWidth(String value) {
        labelWidth = value;
    }


    public String getAlign() {
        if (align == null) {
            return DynUtils.DEFAULT_FIELD_GROUP_ALIGN;
        } else {
            return align;
        }
    }

    public void setAlign(String value) {
        align = value;
    }


    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }


    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }


    public String getSpacing() {
        return spacing;
    }

    public void setSpacing(String spacing) {
        this.spacing = spacing;
    }


    public String getDownloadRestriction() {
        return downloadRestriction;
    }

    public void setDownloadRestriction(String value) {
        this.downloadRestriction = value;
    }


    public String getTitle() {
        if (title == null) {
            return DynUtils.DEFAULT_FIELD_GROUP_TITLE;
        } else {
            return title;
        }
    }

    public void setTitle(String value) {
        this.title = value;
    }


    public String getTooltip() {
        if (tooltip == null) {
            return DynUtils.DEFAULT_FIELD_GROUP_TOOLTIP;
        } else {
            return tooltip;
        }
    }

    public void setTooltip(String value) {
        tooltip = value;
    }


    public AccessTag getAccess() {
        return access;
    }

    public void setAccess(AccessTag value) {
        access = value;
    }


    public void addUIComponent(UIComponent uiComponent) {
        if (uiComponents == null) {
            uiComponents = new ArrayList<UIComponent>();
        }
        uiComponents.add(uiComponent);
    }

    public List<UIComponent> getUIComponents() {
        if (uiComponents == null) {
            uiComponents = new ArrayList<UIComponent>();
        }
        return uiComponents;
    }

}

