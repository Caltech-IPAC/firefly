package edu.caltech.ipac.util.dd;

/**
 * $Id: PasswordFieldDef.java,v 1.1 2010/09/28 17:58:39 roby Exp $
 */
public class PasswordFieldDef extends StringFieldDef {

    public static final String PASSWORD_REGEXP = "......+";

    public PasswordFieldDef() {}

    public PasswordFieldDef(
            String name,
            String label,
            String errMsg,
            String tips,
            int maxWidth,
            int preferWidth,
            String defValue,
            boolean nullAllow) {
        super(name,label,errMsg,tips,maxWidth,preferWidth,defValue,nullAllow,null);
    }

    

    @Override
    public String getDefaultMask() { return PASSWORD_REGEXP; }
}
