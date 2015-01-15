/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;


import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.table.filter.FilterPanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RangePanel extends Composite implements InputFieldGroup, UsesFormHub {

    private final String NAME = "Name";
    private final String TITLE = "Title";
    private final String REQUIRES_QUOTES = "RequiresQuotes";

    private VerticalPanel mainPanel;

    private FormHub formHub;

    private FilterPanel fp;

    private List<TableDataView.Column> colList;

    private List<String> requiresQuotes;

    private String name;
    private String title;

    public FormHub getFormHub() {
        return formHub;
    }

    public RangePanel(Map<String, String> paramMap) {

        colList = new ArrayList<TableDataView.Column>();
        requiresQuotes = new ArrayList<String>();

        if (paramMap.containsKey(NAME)) {
            name = paramMap.get(NAME);
            paramMap.remove(NAME);
        }

        if (paramMap.containsKey(TITLE)) {
            title = paramMap.get(TITLE);
            paramMap.remove(TITLE);
        }

        if (paramMap.containsKey(REQUIRES_QUOTES)) {
            requiresQuotes = StringUtils.asList(paramMap.get(REQUIRES_QUOTES), ",");
            paramMap.remove(REQUIRES_QUOTES);
        }

        for (Map.Entry<String, String> e : paramMap.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            BaseTableColumn c = new BaseTableColumn(key);
            c.setTitle(value);
            if (requiresQuotes.contains(key)) {
                c.setRequiresQuotes(true);
            }
            colList.add(c);
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
                }
            });

            DeferredCommand.addCommand(new Command() {
                public void execute() {
                }
            });
        }
    }

    public void setFieldValues(List<Param> list) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Param> getFieldValues() {
        List<Param> pList = new ArrayList<Param>();
        List<String> filters = fp.getFilters();
        String f = StringUtils.toString(filters, ",");

        if (!StringUtils.isEmpty(f)) {
            pList.add(new Param(name, f));
        }

        return pList;
    }

    public boolean validate() {
        boolean retval = true;
        return retval;
    }

    private void init() {
        mainPanel = new VerticalPanel();
        mainPanel.add(new HTML(title));

        fp = new FilterPanel(colList, false);
        mainPanel.add(fp);
        mainPanel.setSpacing(4);
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

