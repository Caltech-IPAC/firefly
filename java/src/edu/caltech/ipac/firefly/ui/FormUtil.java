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
        for (InputField f : gFields) { excludeKeys.add(f.getName()); }  // excludeKeys are the keys of all the gFields
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
