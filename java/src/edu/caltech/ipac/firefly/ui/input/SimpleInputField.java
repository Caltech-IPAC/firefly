/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.Iterator;

/**
 * SimpleInputField is a widget and user field decorator that adds label
 * @author tatianag
 * $Id: SimpleInputField.java,v 1.12 2012/01/23 19:27:36 roby Exp $
 */
public final class SimpleInputField extends Composite implements HasWidgets, InputFieldContainer {

    //InputField _fieldWidget;

//    private final HorizontalPanel _panel;
    private final Grid _panel;
    private final InputField _fieldWidget;

    public SimpleInputField(InputField fieldWidget,
                            boolean supportsWarnings) {
        this(fieldWidget,new Config(), supportsWarnings);

    }

    public SimpleInputField(InputField fieldWidget,
                            Config config,
                            boolean supportsWarnings) {

        int widgetRowIdx, widgetColIdx;
        if (config.labelAlign.equals(HorizontalPanel.ALIGN_LEFT)) {
            _panel = new Grid(1,2);
            widgetRowIdx = 0;
            widgetColIdx = 1;
        } else { // ALIGN_CENTER
            _panel = new Grid(2,1);
            widgetRowIdx = 1;
            widgetColIdx = 0;
        }

        if (supportsWarnings) {
            _fieldWidget = new ValidationInputField(fieldWidget);
        } else {
            _fieldWidget = fieldWidget;
        }

        addLabel(_fieldWidget );


        _panel.setWidget(widgetRowIdx,widgetColIdx,_fieldWidget);
        _panel.setCellSpacing(5);

        if (config.labelWidth!=null) {
            _panel.getColumnFormatter().setWidth(0,config.labelWidth);
        }
        
        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add(_panel);

        initWidget(wrapper);

        fieldWidget.setContainer(this);
    }

    public Widget getWidget() {
        return this;
    }

    public void setInternalCellSpacing(int spacing) { _panel.setCellSpacing(spacing);}

    @Override
    public void setVisible(boolean visible) {
        _panel.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return _panel.isVisible();
    }

    public void clearLabel(InputField f) {
        if (f==_fieldWidget)  {
            _panel.setHTML(0,0,"");
        }
    }

    public void addLabel(InputField f) {
        if (f==_fieldWidget)  {
            FieldLabel label= _fieldWidget.getFieldLabel();
            if (label instanceof FieldLabel.Immutable) {
                _panel.setHTML(0,0,((FieldLabel.Immutable)label).getHtml());
            }
            else if (label instanceof FieldLabel.Mutable) {
                _panel.setWidget(0,0,((FieldLabel.Mutable)label).getWidget());
            }
            else {
                _panel.setHTML(0,0,"UNKNOWN LABEL: ");
            }
        }
    }

    public static SimpleInputField createByDef(FieldDef fd) {
        return createByDef(fd,new Config());
    }

    public static SimpleInputField createByDef(FieldDef fd, Config config) {
        InputField f= InputFieldCreator.createFieldWidget(fd);
        return new SimpleInputField(f, config, needsWarnings(f));
    }


    public static SimpleInputField createByDef(FieldDef fd,  Config config, boolean supportWarnings) {
        InputField f= InputFieldCreator.createFieldWidget(fd);
        return new SimpleInputField(f, config, supportWarnings);
    }




    public static SimpleInputField createByProp(String prop) { return createByProp(prop,new Config()); }

    public static SimpleInputField createByProp(String prop, Config config) {
        FieldDef fd= FieldDefCreator.makeFieldDef(prop);
        return createByDef(fd,config);
    }

    public static SimpleInputField createByProp(String prop, Config config, boolean supportWarnings) {
        FieldDef fd= FieldDefCreator.makeFieldDef(prop);
        return createByDef(fd,config,supportWarnings);
    }




    public static boolean needsWarnings(InputField inF) {
        boolean retval= false;
//        if (fd instanceof StringFieldDef && !(fd instanceof EnumFieldDef)) {
//            retval= true;
//        }
        if (inF instanceof TextBoxInputField ||
            inF instanceof SuggestBoxInputField ||
            inF instanceof DegreeInputField ||
            inF instanceof TextAreaInputField) {
            retval= true;
        }
        return retval;
    }


    public Widget getFieldWidget() {
        return this;
    }

    /**
     * Get the label for the FieldWidget
     * @return the label
     */
    public InputField getField() { return _fieldWidget; }

    public String getValue() { return _fieldWidget.getValue(); }
    public void setValue(String v) { _fieldWidget.setValue(v); }
    public void reset() { _fieldWidget.reset(); }
    public boolean validate() { return _fieldWidget.validate(); }
    public void forceInvalid(String err) { _fieldWidget.forceInvalid(err); }
    public FieldDef getFieldDef() { return _fieldWidget.getFieldDef(); }

    public void grabFocus() { _fieldWidget.getFocusWidget().setFocus(true);}

    // implementation of HasWidgets interface

    public void add(Widget w) {
        _panel.add(w);
    }

    public void clear() {
        _panel.clear();
    }

    public Iterator<Widget> iterator() {
        return _panel.iterator();
    }

    public boolean remove(Widget w) {
        return _panel.remove(w);
    }

    public static class Config {
        private String labelWidth;
        private HorizontalPanel.HorizontalAlignmentConstant labelAlign;

        public Config() {
            this(null, HorizontalPanel.ALIGN_LEFT);
        }


        public Config(String labelWidth) {
            this(labelWidth, HorizontalPanel.ALIGN_LEFT);
        }

        public Config(HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
            this(null, labelAlign);
        }

        public Config(String labelWidth, HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
            this.labelWidth = labelWidth;
            this.labelAlign = labelAlign;
        }
    }

}
