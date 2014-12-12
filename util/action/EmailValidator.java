package edu.caltech.ipac.util.action;

/**
 * A class with static method that checks the format of an e-mail address.
 * Returns true if e-mail format is valid, false if not. 
 *
 * @author Su Potts
 * @version $Id: EmailValidator.java,v 1.4 2006/04/12 20:26:35 xiuqin Exp $
 */

public class EmailValidator {

   public static boolean isValidEmail(String s) {
      boolean retval = false;
      try {
         int indexOfAt = s.indexOf("@");
         int indexOfAt2 = s.lastIndexOf("@");

         if (indexOfAt != indexOfAt2)  // there are two @
            return false;

         if (indexOfAt <= 0) { // there is not an @, or @ appears first 
            return false;
	 }
         if (s.indexOf(",") >= 0) {  // found "," in email address
            return false;
         }

         if (s.indexOf(";") >= 0) {  // found ";" in email address
            return false;
         }

         if (s.indexOf(" ") >= 0) {// found space in e-mail address
            return false;
	 }

         if (s.indexOf(".") == 0) {  // e-mail starts with a dot
            return false;
         }


         String name = s.substring(0, indexOfAt);
         String host = s.substring(indexOfAt+1);

         int indexOfDot = host.indexOf(".");
         if (indexOfDot <= 0)  // '.' no dot in host name or it is the first 
            return false;

	 if (host.indexOf("..") != -1)  // ".." in host name
	    return false;

         int len = name.length();
         for (int i=0; i<len; i++) {
            if (Character.isLetterOrDigit(name.charAt(i)))
               retval = true;
	 }

         len = host.length();
         for (int i=0; i<indexOfDot; i++) {
            if (Character.isLetterOrDigit(host.charAt(i)))
               retval = retval && true;
         }
         for (int i=len-1; i>indexOfDot; i--) {
            if (Character.isLetterOrDigit(host.charAt(i)))
               retval = retval && true;
         }
      }
      catch (NullPointerException e) {
         System.out.println("EmailValidator.isValidEmail: " + e);
      }
      return retval;
   }

}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
