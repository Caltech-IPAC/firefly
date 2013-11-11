package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.ui.input.*;
import edu.caltech.ipac.firefly.ui.table.TabPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class MOSPanel extends Composite implements InputFieldGroup  {

    public static final String OBJ_TYPE_2_KEY = "obj_type_2";
    public static final String MPC_DATA_KEY = "mpc_data";
    public static final String OBJ_TYPE_3_KEY = "obj_type_3";
    public static final String BODY_DESIGNATION_KEY = "body_designation";
    public static final String EPOCH_KEY = "epoch";
    public static final String ECCENTRICITY_KEY = "eccentricity";
    public static final String SEMIMAJOR_AXIS_KEY = "semimajor_axis";
    public static final String PERIH_DIST_KEY = "perih_dist";
    public static final String INCLINATION_KEY = "inclination";
    public static final String ARH_PERIHELION_KEY = "arg_perihelion";
    public static final String ASCEND_NODE_KEY = "ascend_node";
    public static final String MEAN_ANOMALY_KEY = "mean_anomaly";
    public static final String PERIH_TIME_KEY = "perih_time";

    public static final String START_DATE_KEY = "obs_begin";
    public static final String END_DATE_KEY = "obs_end";

    private VerticalPanel mainPanel;
    private TabPane<Widget> tabPane = new TabPane<Widget>();
    private NaifTargetPanel naifTargetPanel;
    private DatePanel datePanel;
    private List<InputField> fields;
    private List<InputField> fieldsTab1;
    private List<InputField> fieldsTab2;
    private List<InputField> fieldsTab2Asteroid;
    private List<InputField> fieldsTab2Comet;

    private InputField semimajorAxis;
    private InputField perihDist;
    private InputFieldPanel  manualInputLeft;

    private InputField meanAnomaly;
    private InputField perihTime;
    private InputFieldPanel manualInputRight;

    public MOSPanel() {
        init();
        initWidget(mainPanel);

    }

    private void init() {
        naifTargetPanel = new NaifTargetPanel();
        InputField objectType2 = FormBuilder.createField(OBJ_TYPE_2_KEY);
        InputField mpcData = FormBuilder.createField(MPC_DATA_KEY);
        Widget mpcInputPanel = FormBuilder.createPanel(90, objectType2, mpcData);

        final SimpleInputField objectType3 = SimpleInputField.createByProp(OBJ_TYPE_3_KEY);

        InputField bodyDesignation = FormBuilder.createField(BODY_DESIGNATION_KEY);
        InputField epoch = FormBuilder.createField(EPOCH_KEY);
        InputField eccentricity = FormBuilder.createField(ECCENTRICITY_KEY);
        semimajorAxis= FormBuilder.createField(SEMIMAJOR_AXIS_KEY);
        perihDist = FormBuilder.createField(PERIH_DIST_KEY);

        manualInputLeft = new InputFieldPanel(140);
        manualInputLeft.addUserField(bodyDesignation, HorizontalPanel.ALIGN_LEFT);
        manualInputLeft.addUserField(epoch, HorizontalPanel.ALIGN_LEFT);
        manualInputLeft.addUserField(eccentricity, HorizontalPanel.ALIGN_LEFT);
        manualInputLeft.addUserField(semimajorAxis, HorizontalPanel.ALIGN_LEFT);
        manualInputLeft.addUserField(perihDist, HorizontalPanel.ALIGN_LEFT);

        InputField inclination = FormBuilder.createField(INCLINATION_KEY);
        InputField argPerihelion= FormBuilder.createField(ARH_PERIHELION_KEY);
        InputField ascendNode = FormBuilder.createField(ASCEND_NODE_KEY);
        meanAnomaly = FormBuilder.createField(MEAN_ANOMALY_KEY);
        perihTime = FormBuilder.createField(PERIH_TIME_KEY);

        manualInputRight = new InputFieldPanel(140);
        manualInputRight.addUserField(inclination, HorizontalPanel.ALIGN_LEFT);
        manualInputRight.addUserField(argPerihelion, HorizontalPanel.ALIGN_LEFT);
        manualInputRight.addUserField(ascendNode, HorizontalPanel.ALIGN_LEFT);
        manualInputRight.addUserField(meanAnomaly, HorizontalPanel.ALIGN_LEFT);
        manualInputRight.addUserField(perihTime, HorizontalPanel.ALIGN_LEFT);

        HorizontalPanel vp = new HorizontalPanel();
        vp.add(manualInputLeft);
        vp.add(manualInputRight);

        FlowPanel manualInputPanel = new FlowPanel();
        manualInputPanel.add(objectType3);
        manualInputPanel.add(vp);


        tabPane.addTab(naifTargetPanel, "Object Name");
        tabPane.addTab(mpcInputPanel, "MPC Input");
        tabPane.addTab(manualInputPanel, "Manual Input");
        tabPane.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> integerSelectionEvent) {
                int selected = integerSelectionEvent.getSelectedItem();
                if (selected == 0) tabPane.setSize("640px", "110px");
                else if (selected == 1) tabPane.setSize("640px", "130px");
                else if (selected == 2)  tabPane.setSize("640px", "220px");
            }
        });
        tabPane.selectTab(0);
        tabPane.setSize("640px", "110px"); // tabPane is not rendered if size is not set
        tabPane.setTabPaneName("MOST TabPane");

        FormBuilder.Config config = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                            120, 5, HorizontalPanel.ALIGN_LEFT);
        datePanel = new DatePanel((10 * 365 + 3) * 24 * 60 * 60, START_DATE_KEY, END_DATE_KEY, config);
        datePanel.setIntervalViolationError("Position searches can only cover 10-year period.");

        mainPanel = new VerticalPanel();
        mainPanel.add(new HTML("&nbsp;"));
        mainPanel.add(tabPane);
        mainPanel.add(new HTML("&nbsp;"));
        mainPanel.add(datePanel);
        mainPanel.add(new HTML("<i>&nbsp;&nbsp;&nbsp;&nbsp;Enter date range to search, format example: </i>2010-01-14 15:30:00<i>, or </i>2010-01-14<i>.</i><br><br>"));

        fields = Arrays.asList(objectType2, mpcData,
                objectType3.getField(), bodyDesignation, epoch, eccentricity, semimajorAxis, perihDist,
                inclination, argPerihelion, ascendNode, meanAnomaly, perihTime);

        fieldsTab1 = Arrays.asList(objectType2, mpcData);

        fieldsTab2Asteroid = Arrays.asList(
                                objectType3.getField(), bodyDesignation, epoch, eccentricity, semimajorAxis,
                                inclination, argPerihelion, ascendNode, meanAnomaly);
        fieldsTab2Comet = Arrays.asList(
                                objectType3.getField(), bodyDesignation, epoch, eccentricity, perihDist,
                                inclination, argPerihelion, ascendNode, perihTime);


        fieldsTab2 = objectType3.getValue().equalsIgnoreCase("asteroid") ? fieldsTab2Asteroid : fieldsTab2Comet;

        // perihDist and perihTime are only valid for comets
        // semimajorAxis and meanAnomaly are only valid for asteroids
        syncManualInputFieldVis(objectType3.getValue());

        objectType3.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                syncManualInputFieldVis(objectType3.getValue());
            }
        });

    }

    private void syncManualInputFieldVis(String objectType3Val) {
         if (objectType3Val.equalsIgnoreCase("asteroid")) {
             manualInputLeft.clearLabel(perihDist);
             manualInputRight.clearLabel(perihTime);
             manualInputLeft.addLabel(semimajorAxis);
             manualInputRight.addLabel(meanAnomaly);
             perihDist.setVisible(false);
             perihTime.setVisible(false);
             semimajorAxis.setVisible(true);
             meanAnomaly.setVisible(true);
             fieldsTab2 = fieldsTab2Asteroid;
         } else if (objectType3Val.equalsIgnoreCase("comet")) {
             manualInputLeft.clearLabel(semimajorAxis);
             manualInputRight.clearLabel(meanAnomaly);
             manualInputLeft.addLabel(perihDist);
             manualInputRight.addLabel(perihTime);
             perihDist.setVisible(true);
             perihTime.setVisible(true);
             semimajorAxis.setVisible(false);
             meanAnomaly.setVisible(false);
             fieldsTab2 = fieldsTab2Comet;
         }
    }


    public List<Param> getFieldValues() {
        List<Param> params;
        if (tabPane.getSelectedIndex()==0) {
            params = naifTargetPanel.getFieldValues();
        } else if (tabPane.getSelectedIndex() == 1) {
            params = GwtUtil.getFieldValues(fieldsTab1);
        } else if (tabPane.getSelectedIndex() == 2) {
            params = GwtUtil.getFieldValues(fieldsTab2);
        } else {
            params = new ArrayList<Param>();
        }
        params.addAll(datePanel.getFieldValues());
        return params;
    }

    public void setFieldValues(List<Param> list) {
        naifTargetPanel.setFieldValues(list);
        GwtUtil.setFieldValues(list, fields);
        datePanel.setFieldValues(list);
    }

    public boolean validate() {

        boolean validated = datePanel.validate();
        if (tabPane.getSelectedIndex()==0) {
            validated = naifTargetPanel.validate() && validated;
        } else if (tabPane.getSelectedIndex() == 1) {
            for (InputField f : fieldsTab1) {
                validated = f.validate() && validated;
            }
        } else if (tabPane.getSelectedIndex() == 2) {
            for (InputField f : fieldsTab2) {
                validated = f.validate() && validated;
            }
        }
        return validated;
    }

    public void add(Widget widget) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public void clear() {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public Iterator<Widget> iterator() {
        ArrayList<Widget> fieldWidgets = new ArrayList<Widget>(fields.size()+3);
        for (InputField f : fields) {
            if (f != null) fieldWidgets.add(f);
        }
        Iterator<Widget> iter = naifTargetPanel.iterator();
        while (iter.hasNext()) {
            fieldWidgets.add(iter.next());
        }
        iter = datePanel.iterator();
        while (iter.hasNext()) {
            fieldWidgets.add(iter.next());
        }
        return fieldWidgets.iterator();
    }

    public boolean remove(Widget widget) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

}
