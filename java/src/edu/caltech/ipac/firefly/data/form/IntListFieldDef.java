package edu.caltech.ipac.firefly.data.form;

import edu.caltech.ipac.util.StringUtils;

/**
 * @version $Id: IntListFieldDef.java,v 1.1 2011/03/23 20:30:32 loi Exp $
 */
public class IntListFieldDef extends StringListFieldDef {

    public IntListFieldDef() {}

    public IntListFieldDef(String name) {
        super(name);
    }

    public boolean isValidForm(Object val) {
        for(String s : getValues(val)) {
            try {
                if (!StringUtils.isEmpty(s)) {
                    Integer.parseInt(s.trim());
                }
            } catch(Exception e) {
                // not a number
                return false;
            }
        }
        return true;
    }

}