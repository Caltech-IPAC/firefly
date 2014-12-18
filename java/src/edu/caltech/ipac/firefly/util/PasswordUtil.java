package edu.caltech.ipac.firefly.util;

/**
 * @author tatianag
 * @version $Id: PasswordUtil.java,v 1.1 2009/01/22 20:45:23 tatianag Exp $
 */
public class PasswordUtil {

    /**
      * Creates MD5 (hex) hash of the password
      * Checked that md5.js implementation produces the same results
      * as edu.caltech.ipac.util.PasswordHash
      *
      * @param pass unencrypted pass
      * @return MD5 encrypted pass (hex represetation)
      */
     public static native String getMD5Hash(String pass) /*-{
       return $wnd.hex_md5(pass);
     }-*/;

}
