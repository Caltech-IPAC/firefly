package edu.caltech.ipac.util.dd;

import edu.caltech.ipac.util.StringUtils;

/**
 * @version $Id: PatternFieldDef.java,v 1.3 2012/05/24 22:47:07 tatianag Exp $
 */
public class PatternFieldDef extends StringFieldDef {
	private String pattern;

    public PatternFieldDef() {}

    public PatternFieldDef(String name) {
        super(name);
    }

    public String getPattern() {
        return pattern;
    }


    public boolean validate(Object aValue) {
        String value = aValue.toString();
        if (StringUtils.isEmpty(value)) {
            if (!isNullAllow()) {
                return false;
            }
        } else {
            return value.matches(pattern);
        }
        return true;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}