package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 10/13/11
 * Time: 10:03 AM
 */


import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class FormUtil {


    public static int getIntValue(InputField field) {
        return (field != null) ? StringUtils.getInt(field.getValue(), Integer.MIN_VALUE) :
               Integer.MIN_VALUE;
    }

    public static float getFloatValue(InputField field) {
        return (field != null) ? StringUtils.getFloat(field.getValue(), Float.NaN) :
                                 Float.NaN;
    }


    public static List<InputField> getAllChildFields(Widget widget) {
        List<InputField> fields= new ArrayList<InputField>(20);
        getAllChildFields(widget,fields);
        return fields;
    }


    public static void getAllChildFields(Widget widget, List<InputField> fields) {
        if (widget instanceof InputField) {
            fields.add((InputField) widget);
        }
        else if (widget instanceof HasInputFieldsAndGroups) {
            fields.addAll( ((HasInputFieldsAndGroups)widget).getFields()) ;
        }
        else if (widget instanceof HasWidgets) {
            for(Widget w : (HasWidgets)widget)  getAllChildFields(w, fields);
        }
    }


    public static List<InputFieldGroup> getAllChildGroups(Widget widget) {
        List<InputFieldGroup> groups= new ArrayList<InputFieldGroup>(8);
        FormUtil.getAllChildGroups(widget, groups);
        return groups;
    }


    public static void getAllChildGroups(Widget widget, List<InputFieldGroup> groups) {
        if (widget instanceof InputFieldGroup) {
            groups.add((InputFieldGroup) widget);
        }
        else if (widget instanceof HasInputFieldsAndGroups) {
            groups.addAll(((HasInputFieldsAndGroups)widget).getGroups());
        }
        else if (widget instanceof HasWidgets) {
            for(Widget w : (HasWidgets)widget)  getAllChildGroups(w, groups);
        }
    }


    public static Map<String, InputField> getUngroupedFieldsMap(Map<String, InputField> allFieldsMap,
                                                                List<InputFieldGroup> groups) {
        List<InputField> gFields = new ArrayList<InputField>(20);
        for (InputFieldGroup g : groups) {        // gFields is a list of all InputFields that are contained in a FieldGroup
            FormUtil.getAllChildFields((Widget) g, gFields);
        }
        assert(allFieldsMap.size()>=gFields.size());
        List<String> excludeKeys = new ArrayList<String>(gFields.size());
        for (InputField f : gFields) { excludeKeys.add(f.getId()); }  // excludeKeys are the IDs of all the gFields
        Map<String, InputField> ungroupedFields = new HashMap<String, InputField>(allFieldsMap.size()-gFields.size());
        List<String> fieldIDs= new ArrayList<String>(allFieldsMap.keySet());
        for (String k : fieldIDs) {
            if (!excludeKeys.contains(k)) {
                ungroupedFields.put(k,allFieldsMap.get(k));
            }
        }
        return ungroupedFields;
    }


}

