package edu.caltech.ipac.firefly.ui.table.filter;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: May 21, 2008
 *
 * @author loi
 * @version $Id: FilterPanel.java,v 1.5 2011/11/04 23:45:48 loi Exp $
 */
public class FilterPanel extends Composite {
    public static final String SEL_UPDATED = "SelUpdated";

    private static final int ENUM = 1;
    private static final int TEXT = 0;

    private FlexTable mainPanel;
//    private TableDataView data;
    private List<TableDataView.Column> columns;
    private ListBox colsField;
    private ListBox opsField;
    private ListBox display;
    private Button add;
    private Button remove;
    private Button removeAll;
    private Map<String, String[]> enumColValues;

    private ListBox enumInput;
    private TextBox textInput;
    private DeckPanel valueDisplay;
    private boolean sorted;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);


    public FilterPanel(List<TableDataView.Column> columns) {
        this(columns, true);
    }

    public FilterPanel(List<TableDataView.Column> columns, boolean sorted) {
        this.columns = columns;
        this.sorted = sorted;
        enumColValues = loadEnumColValues(columns);
        build();
        layout();
        initWidget(mainPanel);
        init();

    }

    private Map<String, String[]> loadEnumColValues(List<TableDataView.Column> data) {
        HashMap<String, String[]> vals = new HashMap<String, String[]>();
        for (TableDataView.Column c : data) {
            if (c.getEnums() != null && c.getEnums().length > 0) {
                vals.put(c.getName(), c.getEnums());
            }
        }
        return vals;
    }

    public List<String> getFilters() {
        ArrayList<String> vals = new ArrayList<String>();
        for (int i = 0; i < display.getItemCount(); i++) {
            vals.add(display.getValue(i));
        }
        return vals;
    }

    public void setFilters(List<String> filters) {
        display.clear();
        if (filters != null) {
            for (String s : filters) {
                String[] tokens = s.split("\\s", 3);
                String c = tokens[0];
                String op = tokens[1];
                String v = tokens[2];
                if (c.equals(DataSet.ROWID)) {
                    display.addItem(DataSet.ROWID + " " + op + " " + StringUtils.shrink(v, 15), s);
                } else {
                    TableDataView.Column col = findColumn(c);
                    if (col != null) {
                        display.addItem(col.getTitle() + " " + op + " " + v, s);
                    } else {
                        display.addItem(StringUtils.shrink(c, 15)+ " " + op + " " + v, s);
                    }
                }
            }
            pcs.firePropertyChange(SEL_UPDATED, true, true);
        }
        ensureCommands();
    }

    private TableDataView.Column findColumn(String cname) {
        for (TableDataView.Column c : columns) {
            if (c.getName().equals(cname)) {
                return c;
            }
        }
        return null;
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


    protected void layout() {

        valueDisplay.insert(textInput, TEXT);
        valueDisplay.insert(enumInput, ENUM);

        HorizontalPanel selectPanel = new HorizontalPanel();
        selectPanel.add(colsField);
        selectPanel.add(GwtUtil.getFiller(18, 0));
        selectPanel.add(opsField);
        selectPanel.add(GwtUtil.getFiller(6, 0));

        HorizontalPanel cmds = new HorizontalPanel();
        cmds.add(remove);
        cmds.add(removeAll);

        mainPanel.setCellSpacing(5);
        mainPanel.setWidget(0, 0, selectPanel);
        mainPanel.setWidget(0, 1, valueDisplay);
        mainPanel.setWidget(1, 0, display);
        mainPanel.setWidget(2, 0, add);
        mainPanel.setWidget(3, 0, cmds);
        mainPanel.getFlexCellFormatter().setRowSpan(0, 1, 2);
        mainPanel.getFlexCellFormatter().setRowSpan(1, 0, 2);
        mainPanel.getFlexCellFormatter().setVerticalAlignment(0, 1, VerticalPanel.ALIGN_TOP);
        mainPanel.getFlexCellFormatter().setVerticalAlignment(2, 0, VerticalPanel.ALIGN_TOP);
    }

    void init() {
        setupColumnField();
        ensureValueField();
        ensureCommands();
    }

    private void setupColumnField() {
        List<EnumFieldDef.Item> colItems = new ArrayList<EnumFieldDef.Item>();
        for (TableDataView.Column col : columns) {
            String cname = col.getName();
            if (col != null && col.isVisible()) {
                String desc = StringUtils.isEmpty(col.getTitle()) ? cname : col.getTitle();
                colItems.add(new EnumFieldDef.Item(cname, desc));
            }
        }

        if (sorted) {
            Collections.sort(colItems, new Comparator<EnumFieldDef.Item>() {
                public int compare(EnumFieldDef.Item o1, EnumFieldDef.Item o2) {
                    return o1.getName().toLowerCase().compareTo(
                            o2.getName().toLowerCase());
                }
            });
        }

        EnumFieldDef cols = new EnumFieldDef("Column");
        cols.setNullAllow(false);
        cols.addItems(colItems);
        cols.setErrMsg("This field is required. Select one from list");
        GwtUtil.populateComboBox(colsField, cols);
    }

    private void build() {

        mainPanel = new FlexTable();

        // create columns field
        colsField = new ListBox(false);
        colsField.setWidth("130px");
        colsField.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                ensureValueField();
            }
        });


        // create operator field
        List<EnumFieldDef.Item> opItems = new ArrayList<EnumFieldDef.Item>();
        opItems.add(new EnumFieldDef.Item("="));
        opItems.add(new EnumFieldDef.Item(">"));
        opItems.add(new EnumFieldDef.Item("<"));
        opItems.add(new EnumFieldDef.Item("!="));
        opItems.add(new EnumFieldDef.Item(">="));
        opItems.add(new EnumFieldDef.Item("<="));
        opItems.add(new EnumFieldDef.Item("IN"));
        opItems.add(new EnumFieldDef.Item("LIKE"));
        EnumFieldDef ops = new EnumFieldDef("operator");
        ops.addItems(opItems);
        ops.setNullAllow(false);
        ops.setErrMsg("This field is required. Select one from list");
        opsField = GwtUtil.createComboBox(ops);
        opsField.setWidth("50px");
        opsField.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                ensureValueField();
            }
        });


        textInput = new TextBox();
        textInput.setVisibleLength(15);
        textInput.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent ev) {
                final char keyCode = ev.getCharCode();
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        if (keyCode == KeyCodes.KEY_ENTER) {
                            addCondition();
                        }
                    }
                });
            }
        });

        valueDisplay = new DeckPanel();
        valueDisplay.setWidth("130px");
        enumInput = createEnumInput(false);

        display = new ListBox(true);
        display.setPixelSize(210, 70);

        add = GwtUtil.makeButton("<< Add", "Add this filter to the list", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                addCondition();
            }
        });

        remove = GwtUtil.makeButton("Remove", "Remove the selected filter from the list", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                for (int i = 0; i < display.getItemCount(); i++) {
                    if (display.isItemSelected(i)) {
                        display.removeItem(i);
                        pcs.firePropertyChange(SEL_UPDATED, true, true);
                    }
                }
                ensureCommands();
            }
        });

        removeAll = GwtUtil.makeButton("Remove All", "Remove all of the filters from the list", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                display.clear();
                pcs.firePropertyChange(SEL_UPDATED, true, true);
                ensureCommands();
            }
        });

        remove.setEnabled(false);
        removeAll.setEnabled(false);

// setup listeners to enable/disable command buttons
        display.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent ev) {
                ensureCommands();
            }
        });
    }

    private ListBox createEnumInput(boolean isMultiSelect) {
        ListBox lb = new ListBox(isMultiSelect);
        lb.setVisibleItemCount(isMultiSelect ? 5 : 1);
        if (isMultiSelect && BrowserUtil.isIE()) {
            lb.setHeight("70px");
        }
        lb.setWidth("130px");
        return lb;
    }

    private String getCondValue() {
        if (valueDisplay.getVisibleWidget() == ENUM) {
            ArrayList<String> v = new ArrayList<String>();
            for (int i = 0; i < enumInput.getItemCount(); i++) {
                if (enumInput.isItemSelected(i)) {
                    v.add(enumInput.getValue(i));
                }
            }
            return StringUtils.toString(v);
        } else {


            return textInput.getValue();
        }
    }

    private void ensureValueField() {
        String col = colsField.getValue(colsField.getSelectedIndex());

        if (enumColValues.containsKey(col)) {
            String ops = opsField.getValue(opsField.getSelectedIndex());
            if (ops.equals("IN")) {
                if (!enumInput.isMultipleSelect()) {
                    valueDisplay.remove(ENUM);
                    enumInput = createEnumInput(true);
                    valueDisplay.insert(enumInput, ENUM);
                }
                if (BrowserUtil.isIE()) {
                    valueDisplay.setHeight("70px");
                }
            } else {
                if (enumInput.isMultipleSelect()) {
                    valueDisplay.remove(ENUM);
                    enumInput = createEnumInput(false);
                    valueDisplay.insert(enumInput, ENUM);
                }
                if (BrowserUtil.isIE()) {
                    valueDisplay.setHeight("10px");
                }
            }
            enumInput.clear();
            String[] v = enumColValues.get(col);
            for (String s : v) {
                enumInput.addItem(s);
            }
            valueDisplay.showWidget(ENUM);
        } else {
            valueDisplay.showWidget(TEXT);
        }
    }

    private void addCondition() {
//                    if (colsField.validate() & opsField.validate() &
//                            textInput.validate()) {

        String colsFieldVal = colsField.getValue(colsField.getSelectedIndex());
        boolean quotes = false;

        for (TableDataView.Column c : columns) {
            if (c instanceof BaseTableColumn) {
                BaseTableColumn c2 = (BaseTableColumn) c;

                if (c2.getName().equals(colsFieldVal) && c2.isRequiresQuotes()) {
                    quotes = true;
                    break;
                }
            }
        }

        String condVal = getCondValue();
        if (quotes && !condVal.contains("'")) {
            condVal = "'" + condVal + "'";

        }

        String val = colsField.getValue(colsField.getSelectedIndex()) +
                " " + opsField.getValue(opsField.getSelectedIndex()) + " " +
                condVal;
        String desc = colsField.getItemText(colsField.getSelectedIndex()) +
                " " + opsField.getItemText(opsField.getSelectedIndex()) +
                " " + condVal;

        if (!exists(display, val)) {
            display.addItem(desc, val);
            pcs.firePropertyChange(SEL_UPDATED, true, true);
        }
        ensureCommands();
    }

    private static boolean exists(ListBox lb, String val) {
        for (int i = 0; i < lb.getItemCount(); i++) {
            if (lb.getValue(i).equals(val)) {
                return true;
            }
        }
        return false;
    }

    private void ensureCommands() {
        if (display.getItemCount() == 0) {
            removeAll.setEnabled(false);
        } else {
            removeAll.setEnabled(true);
        }
        if (display.getSelectedIndex() >= 0) {
            remove.setEnabled(true);
        } else {
            remove.setEnabled(false);
        }
    }
}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/

