package edu.caltech.ipac.util;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.text.NumberFormat;



/**
 * A class with static methods that do operations on Strings.
 * @author Trey Roby Xiuqin Wu
 */
public class StringUtil {

    public static final String NO_EXTENSION = "NO-EXTENSION";
   /**
    * Parse a string into an array of strings.  If the string contains quotes
    * then every things inside the quotes will be in one strings.  Works
    * much like the unix command line.  Therefore the String:
    * <ul><li><code>aaa "bbb ccc ddd" eee</code></li></ul>
    * should become:
    * <ol>
    * <li><code>aaa</code>
    * <li><code>bbb ccc ddd</code>
    * <li><code>eee</code>
    * </ol>
    * @param instr the string to be parsed
    * @return a String array of the tokens
    */
   public static String [] strToStrings(String instr) {
       String astr, delim= " ", tmpstr;
//       String [] retval;
       ArrayList<String> tokens= new ArrayList<String>(12);
       Assert.tst(instr != null);
       StringTokenizer st = new StringTokenizer(instr, delim);
       try {
           while (true) {
               astr= st.nextToken(delim);
               if (astr.charAt(0) == '"') {
                   StringBuffer adder= new StringBuffer(astr);
                   adder.deleteCharAt(0);
                   if (adder.toString().endsWith("\"")) {
                       adder.deleteCharAt(adder.length()-1);
                   }
                   else {
                       try {
                          tmpstr= st.nextToken("\"");
                          adder.append( tmpstr );
                       } catch (NoSuchElementException e) {
                          adder.append( st.nextToken(delim) );
                       }
                       st.nextToken(delim);
                   }
                   astr= adder.toString();
               } // end if
               tokens.add(astr);
           }  // end while
       } catch (NoSuchElementException e) { }
//       retval = new String[tokens.size()];
//       for(int i= 0; (i<retval.length); i++)
//                             retval[i]= (String)tokens.get(i);
       return tokens.toArray(new String[tokens.size()]);
   }


   /**
    * Return an array of Integers from a string that contains
    * space separated integers. If any of the space separated
    * tokens is not an integer then a zero is placed in that position.
    * @param instr the string to be parsed that should
                    contain space separated integers
    * @return a Integer array
    */
   public static Integer [] strToIntegers(String instr) {
       String [] strary= strToStrings(instr);
       Integer[] intary= new Integer[strary.length];
       for(int i= 0; (i<strary.length); i++ ) {
           try {
                   intary[i]= new Integer(strary[i]);
           }
           catch (NumberFormatException e) {
                   System.out.println("StringUtil.stringtoItegers: " +
                         "Could not convert " + strary[i] + " to Integer");
                   intary[i]= new Integer(0);
           }
       }
       return intary;
   }

   /**
    * Return an array of Longs from a string that contains
    * space separated longs. If any of the space separated
    * tokens is not a long then a zero is placed in that position.
    * @param instr the string to be parsed that should
                    contain space separated integers
    * @return a Long array
    */
   public static Long [] strToLongs(String instr) {
       String [] strary= strToStrings(instr);
       Long[] intary= new Long[strary.length];
       for(int i= 0; (i<strary.length); i++ ) {
           try {
                   intary[i]= new Long(strary[i]);
           }
           catch (NumberFormatException e) {
                   System.out.println("StringUtil.strToLongs: " +
                         "Could not convert " + strary[i] + " to Long");
                   intary[i]= new Long(0);
           }
       }
       return intary;
   }

   /**
    * Return an array of Floats from a string that contains
    * space separated floats. If any of the space separated
    * tokens is not a float then a zero is placed in that position.
    * @param instr the string to be parsed that should
                    contain space separated integers
    * @return a Float array
    */
   public static Float [] strToFloats(String instr) {
       String [] strary= strToStrings(instr);
       Float[] fary= new Float[strary.length];
       for(int i= 0; (i<strary.length); i++ ) {
           try {
                   fary[i]= new Float(strary[i]);
           }
           catch (NumberFormatException e) {
                   System.out.println("StringUtil.stringtoItegers: " +
                         "Could not convert " + strary[i] + " to Float");
                   fary[i]= new Float(0);
           }
       }
       return fary;
   }

   /**
    * Return an array of Doubles from a string that contains
    * space separated doubles. If any of the space separated
    * tokens is not a double then a zero is placed in that position.
    * @param instr the string to be parsed that should
                    contain space separated integers
    * @return a Double array
    */
   public static Double [] strToDoubles(String instr) {
       String [] strary= strToStrings(instr);
       Double[] fary= new Double[strary.length];
       for(int i= 0; (i<strary.length); i++ ) {
           try {
                   fary[i]= new Double(strary[i]);
           }
           catch (NumberFormatException e) {
                   System.out.println("StringUtil.stringtoItegers: " +
                         "Could not convert " + strary[i] + " to Double");
                   fary[i]= new Double(0);
           }
       }
       return fary;
   }

   public static FormatFloat[] strToFormatFloats(String       instr,
                                                 NumberFormat nf) {
       String [] strary= strToStrings(instr);
       return strToFormatFloats(strary,nf);
   }

    public static FormatFloat[] strToFormatFloats(String       strary[],
                                                  NumberFormat nf) {
        FormatFloat[] fary= new FormatFloat[strary.length];
        for(int i= 0; (i<strary.length); i++ ) {
            try {
                fary[i]= new FormatFloat(strary[i], nf);
            }
            catch (NumberFormatException e) {
                System.out.println("StringUtil.stringtoItegers: " +
                        "Could not convert " + strary[i] + " to FormatFloat");
                fary[i]= new FormatFloat(0F,nf);
            }
        }
        return fary;
    }

   public static FormatDouble[] strToFormatDoubles(String       instr,
                                                   NumberFormat nf) {
       String [] strary= strToStrings(instr);
       return strToFormatDoubles(strary,nf);
   }

    public static FormatDouble[] strToFormatDoubles(String       strary[],
                                                    NumberFormat nf) {
        FormatDouble[] fary= new FormatDouble[strary.length];
        for(int i= 0; (i<strary.length); i++ ) {
            try {
                fary[i]= new FormatDouble(strary[i], nf);
            }
            catch (NumberFormatException e) {
                System.out.println("StringUtil.stringtoItegers: " +
                        "Could not convert " + strary[i] + " to FormatDouble");
                fary[i]= new FormatDouble(0F,nf);
            }
        }
        return fary;
    }

   /**
    * return true if there are only spaces in the string
    */
   public static boolean isSpaces(String s) {
      int     length = s.length();
      boolean retval = true;

      for (int i=0; i<length; i++) {
         if (!Character.isWhitespace(s.charAt(i))) {
            retval = false;
            break;
            }
         }
      return retval;
   }

   /**
    * return true if there are only digits in the string
    */
//   public static boolean allDigits(String s) {
//      int     length = s.length();
//      boolean retval = true;
//
//      for (int i=0; i<length; i++) {
//         if (!Character.isDigit(s.charAt(i))) {
//            retval = false;
//            break;
//            }
//         }
//      return retval;
//   }
   /**
    * removes extra spaces from a string.
    * <ul><li><code>" bbb    ccc  ddd"</code></li></ul>
    * should become:
    * <ul><li><code>aaa "bbb ccc ddd" eee</code></li></ul>
    */
    public static String crunch (String s) {
        if (s != null)
        {
            int counter = 0;
            StringTokenizer st = new StringTokenizer(s);
            StringBuffer sb = new StringBuffer();
            String token = "";
            while (st.hasMoreTokens()){
                token = st.nextToken();
                if (token.trim().length() > 0){//if there is something to write
                    if (counter > 0)
                    {
                        sb.append(" ");
                        sb.append(token);
                    }
                    else if  (counter == 0){
                        sb.append(token);
                        counter ++;
                    }
                }
            }
            s = sb.toString();
        }
        return s;
    }


   public static String getWords(String str, int howMany, boolean addElipse) {
       StringTokenizer st= new StringTokenizer(str, " ");
       String retstr= "";
       int i, index= 0;

       for(i=0; (st.hasMoreTokens() && i<=howMany); i++ ) {
              index= str.indexOf(st.nextToken() , index-1);
       }
       if (addElipse && i==howMany+1) {
             retstr= str.substring(0, index-1) + "...";
       }
       else {
             retstr= str;
       }
       return retstr;
   }


    public static String millsecToFormatStr(long milliSec,
                                            boolean userFriendly) {
        String retval;
        if (userFriendly) {
            long sec= milliSec / 1000;

            if (sec < 3300) {
                if (sec <=5)                     retval= "Less than 5 sec";
                else if (sec <=30)               retval= "Less than 30 sec";
                else if (sec <=45)               retval= "Less than a minute";
                else if (sec < 75 && sec > 45)   retval= "About a minute";
                else                     retval= "About " + sec/60 + " minutes";
            }
            else {
                float hour= sec / 3600F;
                if (hour < 1.2F && hour > .8F) {
                    retval= "About an hour";
                }
                else {
                    retval= millsecToFormatStr(milliSec);
                }
            }
        }
        else {
            retval= millsecToFormatStr(milliSec);
        }
        return retval;
    }

    public static String millsecToFormatStr(long milliSec) {
        String minStr, secStr;
        long inSec= milliSec / 1000;
        long hours= inSec/3600;
        long mins= (inSec - (hours*3600)) / 60;
        minStr=  (mins < 10) ? "0" + mins : mins + "";
        long secs= inSec - ((hours*3600) + (mins*60));
        secStr=  (secs < 10) ? "0" + secs : secs + "";
        return hours + ":" + minStr + ":" + secStr;
    }

   public static String wrap(String s, int lineSize) {
        String retval= s;
        int length= 0;
        if (s!=null) length= s.length();
        if (s != null &&  s.length() > 0 && s.length() > lineSize ) {
              int i;
              StringBuffer buff= new StringBuffer(lineSize+50);
              StringBuffer retString= new StringBuffer(length + 30 );
              //for(i=0; (i<5); i++) buff.append(" ");
              String emptyString= buff.toString() + "\\";
              String nlString= "\\n";
              int breakCnt= 0;
              char c;
              for(i=0; (i<length); i++) {
                  c= s.charAt(i);
                  if (c != '\n') {
                      //if (c == '\"') buff.append('\\');
                      buff.append(c);
                  }
                  if ( (breakCnt >= lineSize && 
                            Character.isWhitespace(s.charAt(i)) ) 
                       || c == '\n' || i == length-1) {
                       if (i < length-1) {
                          if (buff.toString().equals(emptyString)) {
                             buff.append(' ');
                             buff.append(nlString);
                             //buff.append('\\');
                          }
                          else {
                             if (c != '\n') buff.append('\n');
                             //buff.append('\\');
                          }
                          //writer.println(buff);
                          retString.append(buff.toString());
                          buff.delete(0, buff.length());
                          if (Character.isWhitespace(s.charAt(i+1))){
                             buff.append(' ');
                          }
                          breakCnt= 0;
                       }
                       else {
                          retString.append(buff.toString());
                          //writer.println(buff);
                          //writer.println("");
                       }
                  }
                  else {
                       breakCnt++;
                  }
              } // end loop
              retval= retString.toString();
        }
        return retval;
   } 


   public static String getShortClassName(String className) {
      StringTokenizer st= new StringTokenizer(className, "."); 
      int len= st.countTokens();
      for(int i= 0; (i<len-1); st.nextToken(), i++);
      return st.nextToken();
   }

   public static String getShortClassName(Class c) { 
      return getShortClassName(c.getName());
   }

    public static String getPackageName(Class c) {
        return getPackageName(c.getName());
    }

    public static String getPackageName(String className) {
        String retval= "";
        int end= className.lastIndexOf(".");
        if (end>-1) {
            retval= className.substring(0,end);
        }
        return retval;
    }



   public static String pad(String s, int toSize) {
      int len= s.length();
      if (s.length() < toSize) {
          int diffSize= toSize-len;
          StringBuffer sb= new StringBuffer(diffSize);
          for(int i=0; (i<diffSize); i++) sb.append(' ');
          s= s+sb.toString();
      }
      return s;
   }

   public static String frontpad(String s, int toSize) {
      int len= s.length();
      if (s.length() < toSize) {
          int diffSize= toSize-len;
          StringBuffer sb= new StringBuffer(diffSize);
          for(int i=0; (i<diffSize); i++) sb.append(' ');
          s= sb.toString() + s;
      }
      return s;
   }
   public static String getInitials(String s) {
       if (s==null) return null;
       StringBuffer sb= new StringBuffer(10);
       String sAry[]= s.split(" ");
       for(int i=0; (i<sAry.length); i++) {
          if (sAry[i].length() > 0) {
             sb.append( sAry[i].charAt(0) );
          }
       }
       return sb.toString();
   }


    public static boolean matchesRegExpList(String s, String regExpArray[]) {
        return matchesRegExpList(s,regExpArray, false);

    }

    public static boolean matchesRegExpList(String s,
                                            String regExpArray[],
                                            boolean ignoreCase) {
        boolean found= false;
        String newRegExpArray[]= regExpArray;
        if (s!=null) {
            if (ignoreCase) {
                s= s.toLowerCase();
                for(int i=0; (i<regExpArray.length);i++) {
                    newRegExpArray[i]= regExpArray[i].toLowerCase();
                }
            }
            for(int i=0; (i<regExpArray.length && !found);i++) {
                found= s.matches(newRegExpArray[i]);
            }
        }
        return found;
    }

    /**
     * Returns true is the given string is either null, or an empty string.
     * A string of white spaces is also considered empty.
     * @param s a string
     * @return Returns true is the given string is either null, or an empty string.
     */
    public static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    /**
     * Returns true is the given string is either null, or an empty string.
     * A string of white spaces is also considered empty.
     * @param o a Object
     * @return Returns true is the given toString() is either null, or an empty string.
     */
    public static boolean isEmpty(Object o) {
        return o == null || isEmpty(o.toString());
    }

   public static void main(String args[]) {
       //String original= "\"asdf\" xxx";
       //String original= "\"asdf\" \"xxx\"";
       //.String original= "asdf \"qqq rrr\" xxx \"bbbb   \" ccc   " +
        //.                "\"   ffff\" \" ggg \" \" rrr sss ttt \" ";
       //String original= "\"asdf \"";
      //. System.out.println("original: "+ original);
      //. String out[]= strToStrings(original);
      //. System.out.println("out.length: "+ out.length);
      //. for (int i= 0; (i<out.length); i++) {
      //.       System.out.println("<" + out[i]+">");
      //. }
      System.out.println("java.version= " + System.getProperty("java.version"));
      Integer.parseInt(args[0]);
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
