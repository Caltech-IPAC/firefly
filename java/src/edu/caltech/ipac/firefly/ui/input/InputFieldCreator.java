/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.input;

import edu.caltech.ipac.firefly.data.form.DateFieldDef;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.firefly.data.form.FileUploadFieldDef;
import edu.caltech.ipac.firefly.data.form.MultiCoordFieldDef;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.firefly.ui.FieldDefCreator;
import edu.caltech.ipac.firefly.ui.RadioGroupInputField;


/**
 * @author tatianag
 *         $Id: InputFieldCreator.java,v 1.13 2011/09/02 00:23:09 loi Exp $
 */
public class InputFieldCreator {


    public static InputField createFieldWidget(String prop) {
        FieldDef fd= FieldDefCreator.makeFieldDef(prop);
        return (fd==null) ? null : createFieldWidget(fd);
    }


    public static InputField createFieldWidget(FieldDef fd) {
        if (fd instanceof EnumFieldDef) {
            EnumFieldDef enfd = (EnumFieldDef) fd;
            if (enfd.getMask() != null && enfd.getMask().equals("[RADIO]")) {
                return createRadioField(enfd);
            } else if (enfd.getMask() != null && enfd.getMask().equals("[CHECKBOX]")) {
                return createCheckboxField(enfd);
            } else {
                return createListBox(enfd);
            }
        } else if (fd instanceof DegreeFieldDef) {
            return createDegreeField((DegreeFieldDef)fd);
        } else if (fd instanceof FileUploadFieldDef) {
            return createFileUploadField((FileUploadFieldDef)fd);
        } else if (fd instanceof DateFieldDef) {
            return createDateField((DateFieldDef)fd);
        } else if (fd instanceof MultiCoordFieldDef) {
             return createTextAreaField((MultiCoordFieldDef)fd);
        } else if (fd instanceof PositionFieldDef) {
            return createPositionStringInputField((PositionFieldDef)fd);
        } else if (fd instanceof StringFieldDef) {
            if (fd.getMask() != null && fd.getMask().equals("[HIDDEN]")) {
                return createHiddenField((StringFieldDef)fd);
            } else {
                return createTextField((StringFieldDef)fd);
            }
        } else {
            throw new IllegalArgumentException("This FieldDef type is not supported: " + (fd!=null ? fd.getClass().getName() : "null"));
        }
    }

    private static InputField createFileUploadField(FileUploadFieldDef def) {
        return new FileUploadField(def);
    }

    private static InputField createDateField(DateFieldDef dateFieldDef) {
        return new DateInputField(dateFieldDef);
    }

    private static InputField createDegreeField(DegreeFieldDef fd) {
        return  new DegreeInputField(fd);
    }

    private static InputField createTextAreaField(MultiCoordFieldDef fd){
        return new TextAreaInputField(fd);
    }

    public static InputField createTextField(final StringFieldDef fd) {
        return new TextBoxInputField(fd);
    }

    public static InputField createHiddenField(final StringFieldDef fd) {
        return new HiddenField(fd);
    }

    public static InputField createRadioField(EnumFieldDef fd) {
            return new RadioGroupInputField(fd);
    }

       public static InputField createCheckboxField(EnumFieldDef fd) {
           return new CheckBoxGroupInputField(fd);
    }

     public static InputField createListBox(EnumFieldDef fd) {
         return new ListBoxInputField(fd);
    }

    public static InputField createPositionStringInputField(PositionFieldDef fd) {
        return new PositionInputField(fd);
    }

}
