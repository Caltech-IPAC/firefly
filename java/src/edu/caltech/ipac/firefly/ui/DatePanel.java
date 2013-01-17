package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.DOM;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.form.DateFieldDef;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Date Panel: includes start and end date input fields
 * $Id: DatePanel.java,v 1.19 2010/05/14 19:59:41 tatianag Exp $
 */
public class DatePanel extends Component implements InputFieldGroup {


    public static final String START_DATE_KEY = "DatePanel.field.startDate";
    public static final String END_DATE_KEY = "DatePanel.field.endDate";

    long maxMillisecBetweenDates;
    InputField startDateField;
    InputField endDateField;
    private List<InputField> fields;
    DateTimeFormat format;
    VerticalPanel datePanel;
    private String intervalViolationError = "";


    public DatePanel(long maxSecBetweenDates) {
        this(maxSecBetweenDates, START_DATE_KEY, END_DATE_KEY, 
                new FormBuilder.Config(FormBuilder.Config.Direction.VERTICAL,
                                            125, 0, HorizontalPanel.ALIGN_LEFT));
    }

    public DatePanel(long maxSecBetweenDates, String startDateKey, String endDateKey, FormBuilder.Config config) {

        maxMillisecBetweenDates = maxSecBetweenDates*1000;
        startDateField = FormBuilder.createField(startDateKey);
        endDateField = FormBuilder.createField(endDateKey);
        fields = Arrays.asList(startDateField, endDateField);
        endDateField.getFocusWidget().addFocusHandler(new FocusHandler() {
            public void onFocus(FocusEvent ev) {
                if (StringUtils.isEmpty(endDateField.getValue()) &&
                        startDateField.validate()) {
                    endDateField.setValue(startDateField.getValue());
                }
            }
        });

        endDateField.getFocusWidget().addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent ev) {
                if (!StringUtils.isEmpty(startDateField.getValue())) {
                    validate();
                }
            }
        });

        Widget dateFieldsPanel = FormBuilder.createPanel(config, startDateField, endDateField);


        Label spacer0 = new Label();
        spacer0.setHeight("5px");
        Label spacer2 = new Label();
        spacer2.setHeight("5px");
        datePanel = new VerticalPanel();
        datePanel.add(spacer0);
        datePanel.add(dateFieldsPanel);
        format = ((DateFieldDef)startDateField.getFieldDef()).getDefaultDateTimeFormat();        
        if (config.getDirection().equals(FormBuilder.Config.Direction.VERTICAL)) {
            Label spacer1 = new Label();
            spacer1.setHeight("5px");
            datePanel.add(spacer1);
            DateTimeFormat aFmt = DateTimeFormat.getFormat("yyyy.MM.dd HH:mm:ss");
            HTML desc = GwtUtil.makeFaddedHelp("Enter date range to search, format example: "+format.format(aFmt.parse("2007.10.23 19:30:01"))+".");
            datePanel.add(desc);
        }
        datePanel.add(spacer2);

        initWidget(datePanel);

    }

    public Date getStartDate() {
        DateFieldDef fd = (DateFieldDef)startDateField.getFieldDef();
        return fd.getDateFromLong(startDateField.getValue());
    }

    public Date getEndDate() {
        DateFieldDef fd = (DateFieldDef)endDateField.getFieldDef();
        return fd.getDateFromLong(endDateField.getValue());
    }

    public DateTimeFormat getFormat() {
        return format;
    }

    /*
    public void focusOnStartDate() {
        new Timer() {
            public void run() {
                startDateField.grabFocus();
            }
        }.schedule(1);
    }
    */

    public void setIntervalViolationError(String error) {
        this.intervalViolationError = error+" ";
    }


    private boolean checkDates() {
        Date startDate = getStartDate();
        Date endDate = getEndDate();
        if (startDate == null || endDate == null) {
            if (endDate != null) {
                startDateField.forceInvalid("Both fields must be filled or empty");
                return false;
            }
            else if (startDate != null) {
                endDateField.forceInvalid("Both fields must be filled or empty");
                return false;
            } else {
                return true;
            }
        }
        return checkDates(startDate, endDate);
    }

    private boolean checkDates(Date startDate, Date endDate) {
        if (startDate.after(endDate)) {
            endDateField.forceInvalid("End date should be after start date");
            return false;
        } else if (endDate.getTime() - startDate.getTime() > maxMillisecBetweenDates) {
            Date maxEndDate = new Date(startDate.getTime()+maxMillisecBetweenDates);
            endDateField.forceInvalid(intervalViolationError+"End date should be on or prior to "+format.format(maxEndDate));

            return false;
        }
        return true;
    }



    public void add(Widget w) {
        datePanel.add(w);
    }

    public void clear() {
        datePanel.clear();
    }

    public Iterator<Widget> iterator() {
        return datePanel.iterator();
    }

    public boolean remove(Widget w) {
        return datePanel.remove(w);
    }


    public List<Param> getFieldValues() {
        return GwtUtil.getFieldValues(fields);
    }

    public void setFieldValues(List<Param> list) {
        GwtUtil.setFieldValues(list, fields);
    }

    public boolean validate() {
        return startDateField.validate() && endDateField.validate() && checkDates();
    }

}
