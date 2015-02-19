/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;

import edu.caltech.ipac.util.StringUtils;

public class EmailFieldDef extends StringFieldDef {

//    private static final String EMAIL_REGEXP="^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$";

    public EmailFieldDef() {}

    public EmailFieldDef(String name,
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
    public boolean validateSoft(Object aValue) throws ValidationException {
        boolean valid= true;
        String s= (aValue==null) ? null : aValue.toString();
        if (!StringUtils.isEmpty(s)) {
            if (s.split("@",3).length>2) {
                throw new ValidationException(getErrMsg() + "- there are two @ signs");
            }
            else if (s.indexOf("@") == 0) {  // e-mail starts with a dot
                throw new ValidationException(getErrMsg() + "- an email address cannot start with dot");
            }
            else if (s.indexOf(".") == 0) {  // e-mail starts with a dot
                throw new ValidationException(getErrMsg() + "- an email address cannot start with dot");
            }
            else if (s.indexOf("") != -1) {
                throw new ValidationException(getErrMsg() + "- an email address cannot have two dots together");
            }
            else if (s.indexOf(",") != -1) {  // found "," in email address
                throw new ValidationException(getErrMsg() + "- you cannot have a comma in an email address");
            }
            else if (s.indexOf(";") != -1) {  // found ";" in email address
                throw new ValidationException(getErrMsg() + "- you cannot have a semi colon in an email address");
            }
        }
        return valid;
    }


    @Override
    public boolean validate(Object aValue) throws ValidationException {
        String s= (aValue==null) ? null : aValue.toString();
        if (StringUtils.isEmpty(s) ) {
            if (isNullAllow()) {
                return true;
            }
            else {
                throw new ValidationException("You must enter an email address");
            }
        }
        boolean valid = false;
        try {
            int indexOfAt = s.indexOf("@");
            int indexOfAt2 = s.lastIndexOf("@");

            if (indexOfAt != indexOfAt2) { // there are two @
                throw new ValidationException(getErrMsg() + "- there are two @ signs");
            }

            if (indexOfAt < 0) { // there is not an @, or @ appears first
                throw new ValidationException(getErrMsg() + "- there are is no @ sign");
            }

            if (indexOfAt == 0) { // there is not an @, or @ appears first
                throw new ValidationException(getErrMsg() + "- the @ sign cannot appear first");
            }

            if (s.indexOf(",") >= 0) {  // found "," in email address
                throw new ValidationException(getErrMsg() + "- you cannot have a comma in an email address");
            }

            if (s.indexOf(";") >= 0) {  // found ";" in email address
                throw new ValidationException(getErrMsg() + "- you cannot have a semi colon in an email address");
            }

            if (s.indexOf(" ") >= 0) {// found space in e-mail address
                throw new ValidationException(getErrMsg() + "- you cannot have a space in an email address");
            }

            if (s.indexOf(".") == 0) {  // e-mail starts with a dot
                throw new ValidationException(getErrMsg() + "- an email address cannot start with dot");
            }


            String name = s.substring(0, indexOfAt);
            String host = s.substring(indexOfAt+1);

            int indexOfDot = host.indexOf(".");
            if (indexOfDot <= 0)  {// '.' no dot in host name or it is the first
                throw new ValidationException(getErrMsg() + "- your host name is incorrect");
            }

            if (host.indexOf("") != -1)  {// ".." in host name
                throw new ValidationException(getErrMsg() + "- you cannot have two dots in the host name");
            }

            int len = name.length();
            for (int i=0; i<len; i++) {
                if (Character.isLetterOrDigit(name.charAt(i)))
                    valid = true;
            }

            len = host.length();
            for (int i=0; i<indexOfDot; i++) {
                if (Character.isLetterOrDigit(host.charAt(i)))
                    valid = valid && true;
            }
            for (int i=len-1; i>indexOfDot; i--) {
                if (Character.isLetterOrDigit(host.charAt(i)))
                    valid = valid && true;
            }
        }
        catch (NullPointerException e) {
            throw new ValidationException(getErrMsg() + "- error parsing");
        }
        if (!valid) throw new ValidationException(getErrMsg());
        return valid;
    }

    @Override
//    public String getDefaultMask() { return EMAIL_REGEXP; }
    public String getDefaultMask() { return null; }

    @Override
    public String getMask() { return null; }
}