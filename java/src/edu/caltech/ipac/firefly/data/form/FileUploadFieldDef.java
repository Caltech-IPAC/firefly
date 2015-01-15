/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.StringFieldDef;

/**
 * Field definition for multi-part file upload field.
 * Date: Aug 10, 2010

 * @author loi
 * @version $Id: FileUploadFieldDef.java,v 1.3 2010/12/01 21:11:40 schimms Exp $
 */
public class FileUploadFieldDef extends StringFieldDef implements FieldDef {

    private String _size;

    public FileUploadFieldDef() {
    }

    public FileUploadFieldDef(String name) {
        super(name, name, "", "", Integer.MAX_VALUE, 100, "", true, null);
    }

    public FileUploadFieldDef(String name, String size) {
        super(name, name, "", "", Integer.MAX_VALUE, 100, "", true, null);
        _size = size;
    }

    public FileUploadFieldDef(String name, String label, String errMsg, String tips,
                              int maxWidth, int preferWidth, boolean nullAllow, String size) {
        super(name, label, errMsg, tips, maxWidth, preferWidth, null, nullAllow, null);
        _size = size;
    }

    public String getSize() {
        return _size;
    }
}