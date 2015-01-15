/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.catalog.CatColumnInfo;
import edu.caltech.ipac.firefly.ui.catalog.CatalogQueryDialog;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CatalogPopupPanel extends Composite implements InputFieldGroup, UsesFormHub {

    private static String initialSelectedColumns = "";
    private static String selectedColumns = "";
    private static String requiredColumns = "";
    private static String selectedConstraints = "";

    private VerticalPanel mainPanel;
    private HTML selColumnsHtml;
    private HTML selConstraintsHtml;

    private List<Param> requiredParams;
    private Map<String, List<Param>> constrainedParams;

    private FormHub formHub;
    private String fieldLink;
    private String fieldValue;


    public FormHub getFormHub() {
        return formHub;
    }

    public CatalogPopupPanel(Map<String, String> paramMap) {
        requiredParams = new ArrayList<Param>();
        constrainedParams = new HashMap<String, List<Param>>();

        for (Map.Entry<String, String> e : paramMap.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();

            if (key.equalsIgnoreCase("field_link")) {
                fieldLink = value;

            } else if (key.toLowerCase().startsWith("field_value_")) {
                // key format: field_value_<value>
                String[] strArr = key.split("_");
                if (strArr.length == 3) {
                    String cpKey = strArr[2];

                    List<Param> cpValue = new ArrayList<Param>();
                    // value format: key=value,key=value,...
                    String[] valueArr = value.split(",");
                    for (String v : valueArr) {
                        String[] v2 = v.split("=");
                        if (v2.length == 2) {
                            cpValue.add(new Param(v2[0], v2[1]));
                        }
                    }

                    constrainedParams.put(cpKey, cpValue);
                }

            } else if (key.equalsIgnoreCase(CatalogRequest.SELECTED_COLUMNS)) {
                initialSelectedColumns = value.toUpperCase();
                selectedColumns = initialSelectedColumns;

            } else if (key.equalsIgnoreCase(CatalogRequest.REQUIRED_COLUMNS)) {
                requiredColumns = value;

            } else {
                requiredParams.add(new Param(key, value));
            }
        }

        init();
        initWidget(mainPanel);
    }

    public void bind(FormHub formHub) {
        this.formHub = formHub;
        if (formHub != null) {
            formHub.getEventManager().addListener(FormHub.FIELD_VALUE_CHANGE, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    Param p = (Param) ev.getData();
                    if (p.getName().equalsIgnoreCase(fieldLink)) {
                        fieldValue = p.getValue();
                    }

                    setDefaultValues();
                }
            });

            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    fieldValue = getFormHub().getValue(fieldLink);
                }
            });
        }
    }

    public List<Param> getFieldValues() {
        List<Param> paramList = new ArrayList<Param>();

        paramList.add(new Param(CatalogRequest.SELECTED_COLUMNS, selectedColumns));

        if (selectedConstraints != null) {
            paramList.add(new Param(CatalogRequest.CONSTRAINTS, selectedConstraints));
        }

        return paramList;
    }

    public void setFieldValues(List<Param> list) {
        for (Param p : list) {
            if (p.getName().equals(CatalogRequest.SELECTED_COLUMNS)) {
                selectedColumns = p.getValue();
            } else if (p.getName().equals(CatalogRequest.CONSTRAINTS)) {
                selectedConstraints = p.getValue();
            }
        }
    }

    public boolean validate() {
        boolean retval = true;
        return retval;
    }

    private void setSelColumnsHtml() {
        if (selColumnsHtml == null) {
            selColumnsHtml = new HTML();
        }

        int cnt = 0;
        if (selectedColumns.length() > 0) {
            cnt = selectedColumns.split(",").length;
        }
        selColumnsHtml.setHTML("<br><i>&nbsp;&nbsp;Number of Selected Columns: " + cnt + "</i>");
    }

    private void setSelConstraintsHtml() {
        if (selConstraintsHtml == null) {
            selConstraintsHtml = new HTML();
        }

        int cnt = 0;
        if (selectedConstraints.length() > 0) {
            cnt = selectedConstraints.split(",").length;
        }
        selConstraintsHtml.setHTML("<i>&nbsp;&nbsp;Number of Column Filters: " + cnt + "</i>");
    }

    private void init() {
        mainPanel = new VerticalPanel();

        HorizontalPanel hp = new HorizontalPanel();

        //setSelColumnsHtml();
        setSelConstraintsHtml();

        hp.add(GwtUtil.makeLinkButton("Set Column Selections and Filters", "Set Column Selections and Filters", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (fieldLink == null) {
                    PopupUtil.showInfo("Error: XML file missing 'field_link' parameter!");
                    return;
                }

                List<Param> params = new ArrayList<Param>(requiredParams);
                params.addAll(constrainedParams.get(fieldValue));

                boolean defSelect;
                if (selectedColumns.length() == 0) {
                    defSelect = true;
                } else {
                    defSelect = false;
                }

                CatalogQueryDialog.showCatalogDialog(mainPanel, new CatColumnInfo() {
                    public void setSelectedColumns(String values) {
                        selectedColumns = values;
                        //setSelColumnsHtml();
                    }

                    public void setSelectedConstraints(String values) {
                        selectedConstraints = values;
                        setSelConstraintsHtml();
                    }
                },
                        params, selectedColumns, requiredColumns, selectedConstraints, defSelect);
            }
        }));

        hp.add(new HTML("&nbsp;&nbsp;&nbsp;&nbsp;"));

        hp.add(GwtUtil.makeLinkButton("Remove Selections and Filters", "Remove Selections and Filters", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                setDefaultValues();
            }
        }));

        mainPanel.add(new HTML("&nbsp;"));
        mainPanel.add(hp);
        //mainPanel.add(selColumnsHtml);
        mainPanel.add(selConstraintsHtml);

        mainPanel.setSpacing(4);
    }

    private void setDefaultValues() {
        selectedColumns = initialSelectedColumns;
        setSelColumnsHtml();

        selectedConstraints = "";
        setSelConstraintsHtml();
    }


//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public void clear() {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public Iterator<Widget> iterator() {
        return (new ArrayList<Widget>().iterator());
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

}

