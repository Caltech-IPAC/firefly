package edu.caltech.ipac.firefly.ui.input;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author Trey Roby
 * $Id: InputFieldPanel.java,v 1.6 2010/11/19 00:24:44 schimms Exp $
 */
public class InputFieldPanel extends Composite implements HasWidgets, InputFieldContainer {

    private final FlexTable _panel= new FlexTable();
    private final int _labelWidth;
    private final Map<InputField,Loc> _labelMap= new HashMap<InputField,Loc>(7);


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public InputFieldPanel(int labelWidth) {
        _labelWidth = labelWidth;
        initWidget(_panel);

    }

//======================================================================
//----------------------- public methods -------------------------------
//======================================================================

    public Widget getWidget() {
        return this;
    }

    public void addUserField(InputField field,  HorizontalPanel.HorizontalAlignmentConstant labelAlign) {
        int row= _panel.getRowCount();
        FieldLabel label= field.getFieldLabel();
//        label.getWidget().setWidth(_labelWidth+"px");
        

        int lRow;
        int lCol;


        if (labelAlign.equals(HorizontalPanel.ALIGN_LEFT)) {
            lRow= row;
            lCol= 0;
            _panel.setWidget(row,1,field);
            _panel.getColumnFormatter().setWidth(0,_labelWidth+"px");
        } else {
            // label at the top
            lRow= row;
            lCol= 0;
            _panel.setWidget(row+1,0,field);
        }

        addLabelAt(field,lRow,lCol);

        _panel.setCellSpacing(5);

        field.setContainer(this);

//        label.getWidget().setWidth(_labelWidth+"px");
        //field.getFieldWidget().setWidth("100px");

    }

    public void setVisible(boolean visible) {
        _panel.setVisible(visible);
    }

    public void clearLabel(InputField f) {
        Loc loc= _labelMap.get(f);
        if (loc!=null) {
            _panel.setHTML(loc._row,loc._col,"");
        }
    }

    public void addLabel(InputField f) {
        Loc loc= _labelMap.get(f);
        if (loc!=null) {
            addLabelAt(f,loc._row,loc._col);
        }
    }

    private void addLabelAt(InputField field, int row, int col) {
        FieldLabel label= field.getFieldLabel();
        if (label instanceof FieldLabel.Immutable) {
            _panel.setHTML(row,col,((FieldLabel.Immutable)label).getHtml());
        }
        else if (label instanceof FieldLabel.Mutable) {
            _panel.setWidget(row,col,((FieldLabel.Mutable)label).getWidget());
        }
        else {
            _panel.setHTML(row,col,"UNKNOWN LABEL: ");
        }
        _labelMap.put(field,new Loc(row,col));
    }


    public void setPadding(int padding) {
        DOM.setStyleAttribute(_panel.getElement(), "padding", padding+"px");
    }

//====================================================================
//  implements HasWidgets
//====================================================================

    public void add(Widget w) {
        throw new UnsupportedOperationException("InputFieldPanel does not support this method.  Use addUserField() instead");
    }

    public void clear() {
        _panel.clear();
    }

    public Iterator<Widget> iterator() {
        int cnt= _panel.getRowCount();
        List<Widget> list= new ArrayList<Widget>(cnt);
        Iterator<Widget> tableWidgets = _panel.iterator();
        Widget w;
        while (tableWidgets.hasNext()) {
            w = tableWidgets.next();
            if (w instanceof InputField) {
                list.add(w);
            }
        }
        return list.iterator();
    }

    public boolean remove(Widget w) {
        return false;
    }


    public static class Loc {
        private final int _row;
        private final int _col;
        Loc(int row, int col) {
            _row= row;
            _col= col;
        }
    }

}

