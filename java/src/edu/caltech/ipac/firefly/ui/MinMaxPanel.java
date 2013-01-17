package edu.caltech.ipac.firefly.ui;

import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.util.StringUtils;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SimplePanel;

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;

/**
 * @author tatianag
 *         $Id: MinMaxPanel.java,v 1.1 2010/05/24 23:35:11 tatianag Exp $
 */
public class MinMaxPanel extends Component implements InputFieldGroup {

    InputField minField;
    InputField maxField;
    SimplePanel minMaxPanel;

    public MinMaxPanel(String minKey, String maxKey, FormBuilder.Config config) {
        minField = FormBuilder.createField(minKey);
        maxField = FormBuilder.createField(maxKey);
        Widget minmax = FormBuilder.createPanel(config, minField, maxField);
        minMaxPanel = new SimplePanel();
        minMaxPanel.add(minmax);
        minField.getFocusWidget().addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent e) {
                validate();
            }
        });
        maxField.getFocusWidget().addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent e) {
                validate();
            }
        });

        initWidget(minMaxPanel);
    }

    public InputField getMinField() { return minField; }
    public InputField getMaxField() { return maxField; }

    public List<Param> getFieldValues() {
        return GwtUtil.getFieldValues(Arrays.asList(minField, maxField));
    }

    public void setFieldValues(List<Param> list) {
        GwtUtil.setFieldValues(list, Arrays.asList(minField, maxField));
    }

    public boolean validate() {
        boolean isValid = minField.validate() && maxField.validate();
        if (isValid && !StringUtils.isEmpty(minField.getValue()) && !StringUtils.isEmpty(maxField.getValue())) {
            if (minField.getNumberValue().doubleValue() > maxField.getNumberValue().doubleValue()) {
                minField.forceInvalid("Min value may not be greater than Max value");
                isValid = false;
            }
        }
        return isValid;
    }

    public void add(Widget w) {
        minMaxPanel.add(w);
    }

    public void clear() {
        minMaxPanel.clear();
    }

    public Iterator<Widget> iterator() {
       return Arrays.asList((Widget)minField, maxField).iterator();
    }

    public boolean remove(Widget w) {
        return minMaxPanel.remove(w);
    }
}
