package edu.caltech.ipac.util;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;



/**
 * A class with static methods that do operations on Strings.
 * @author Trey Roby Xiuqin Wu
 */
public class StringUtil {

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
               }
               tokens.add(astr);
           }
       }  catch (NoSuchElementException e) {
            // ignore - end of loop
       }
       return tokens.toArray(new String[tokens.size()]);
   }

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



   public static String getShortClassName(String className) {
      StringTokenizer st= new StringTokenizer(className, "."); 
      int len= st.countTokens();
      for(int i= 0; (i<len-1); st.nextToken(), i++);
      return st.nextToken();
   }

   public static String getShortClassName(Class c) { 
      return getShortClassName(c.getName());
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

}


