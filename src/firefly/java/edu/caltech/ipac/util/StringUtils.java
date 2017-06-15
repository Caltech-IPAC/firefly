/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Date: Nov 2, 2007
 *
 * Can be compile by GWT
 * @author loi
 * @version $Id: StringUtils.java,v 1.32 2012/11/09 03:01:31 tlau Exp $
 */
public class StringUtils {



    public static enum Align {LEFT, RIGHT, MIDDLE}

    public static final long BYTE          = 1;
    public static final long MEG          = 1048576;
    public static final long GIG          = 1048576 * 1024;
    public static final long MEG_TENTH    = MEG / 10;
    public static final long GIG_HUNDREDTH= GIG / 100;
    public static final long K            = 1024;
    public static String DASH_1;
    public static String DASH_2;
    public static String DASH_3;
    public static String DOUBLE_QUOTATION_MARK_1;
    public static String DOUBLE_QUOTATION_MARK_2;
    public static String DOUBLE_QUOTATION_MARK_3;
    public static String DOUBLE_QUOTATION_MARK_4;

    static {
        try { // jdk 1.1 and up supports public String(byte[] bytes, java.lang.String s)
            DASH_1= new String (new byte[]{-30,-128,-102,-61,-124,-61,-82},"UTF-8");
            DASH_2= new String (new byte[]{-30,-128,-102,-61,-124,-61,-84},"UTF-8");
            DASH_3= new String (new byte[]{-30,-128,-109},"UTF-8");
            DOUBLE_QUOTATION_MARK_1= new String (new byte[]{-30,-128,-99},"UTF-8");
            DOUBLE_QUOTATION_MARK_2= new String (new byte[]{-30,-128,-100},"UTF-8");
            DOUBLE_QUOTATION_MARK_3= new String (new byte[]{-30,-128,-102,-61,-124,-61,-71},"UTF-8");
            DOUBLE_QUOTATION_MARK_4= new String (new byte[]{-30,-128,-102,-61,-124,-61,-70},"UTF-8");
        } catch (UnsupportedEncodingException e) {
            //rare case, only happens if UTF-8 is not supported.
            DASH_1=new String (new byte[]{-30,-128,-108});
            DASH_2=new String (new byte[]{-30,-128,-109});
            DASH_3=new String (new byte[]{-48});
            DOUBLE_QUOTATION_MARK_1=new String (new byte[]{-45});
            DOUBLE_QUOTATION_MARK_2=new String (new byte[]{-46});
            DOUBLE_QUOTATION_MARK_3=new String (new byte[]{-30,-128,-99});
            DOUBLE_QUOTATION_MARK_4=new String (new byte[]{-30,-128,-100});
        }
    }

    public static String shrink(String s, int size) {
        if (s == null || s.length() <= size) return s;
        size = size <=0 ? 1 : size;
        if (size < 4) {
            char[] str = new char[size];
            Arrays.fill(str, '.');
            return String.valueOf(str);
        } else {
            String pre = s.substring(0, size/2 - 1);
            String suf = s.substring(s.length() - (size/2-2));
            return pre + "..." + suf;
        }
    }

    /**
     * Truncates the String representation of the object if its length exceed max.
     * Appends "...(more)" to indicate it has been truncated.
     * @param o
     * @param max
     * @return
     */
    public static String truncate(Object o, int max) {
        String msg = String.valueOf(o);
        msg = msg.length() < max ? msg : msg.substring(0, max) + "...(more)";
        return msg;
    }

    public static String pad(int length, String str) {
        return pad(length, str, Align.LEFT);
    }

    public static String pad(int length, String str, Align align) {
        return pad(length, str, align, ' ');
    }

    public static String pad(int length, String str, Align align, char padWith) {
        if (str == null || str.length() >= length) {
            return str;
        }
        char[] padding = new char[length-str.length()];
        for(int i=0; i<padding.length; i++) {
            padding[i] = padWith;
        }
        if (align.equals(Align.LEFT)) {
            return str + new String(padding);
        } else {
            return new String(padding) + str;
        }

    }

    /**
     * Converts a string of integers separated by the given regular expression into an array of int.
     * @param str
     * @param regExp
     * @return
     * @throws NumberFormatException
     */
    public static int[] convertToArrayInt(String str, String regExp) throws NumberFormatException {
        String[] reqkeys = str.split(regExp);
        int[] intAry = new int[reqkeys.length];
        for (int i = 0; i < reqkeys.length; i++) {
            intAry[i] = new Integer(reqkeys[i].trim());
        }
        return intAry;
    }

    public static List<Integer> convertToListInteger(String str, String regExp) throws NumberFormatException {
        if (isEmpty(str)) return null;
        String[] reqkeys = str.split(regExp);
        ArrayList<Integer> list = new ArrayList<Integer>(reqkeys.length);
        for (int i = 0; i < reqkeys.length; i++) {
            list.add( new Integer(reqkeys[i].trim()));
        }
        return list;
    }

    /**
     * split up the given string into parts based on the given regular expression.
     * the parts are trimmed and returned as a List<String>
     * @param str
     * @param regExp
     * @return
     */
    public static List<String> asList(String str, String regExp, boolean convertToLower) {
        if (isEmpty(str)) return null;
        String[] strs = str.split(regExp);
        ArrayList<String> list = new ArrayList<String>(strs.length);
        for (String str1 : strs) {
            if (convertToLower) str1= str1.toLowerCase();
            list.add(str1.trim());
        }
        return list;
    }

    /**
     * split up the given string into parts based on the given regular expression.
     * the parts are trimmed and returned as a List<String>
     * @param str
     * @param regExp
     * @return
     */
    public static List<String> asList(String str, String regExp) {
        return asList(str,regExp,false);
    }

    /**
     * convert a key=value[,key=value] string into a Map<String,String>
     * this take into consideration that value may be url encoded.
     * due to GWT limitation on the server-side, a limited charset is used.
     * should convert to true encodeUriComponent in the future.
     * @param str
     * @return
     */
    public static Map<String, String> encodedStringToMap(String str) {
        if (isEmpty(str)) return null;
        HashMap<String, String> map = new HashMap<String, String>();
        for (String entry : str.split("&")) {
            String[] kv = entry.split("=", 2);
            String value = kv.length > 1 ? kv[1].trim() : "";
            if (value.contains("%")) {
                value = value.replaceAll("%26", "&");
                value = value.replaceAll("%2C", ",");
                value = value.replaceAll("%3D", "=");
                value = value.replaceAll("%3A", ":");
                value = value.replaceAll("%2F", "/");
            }
            map.put(kv[0].trim(), value);
        }
        return map;
    }

    /**
     * write the map into its string representation.  urlencode the value.
     * @param map
     * @return
     */
    public static String mapToEncodedString(Map<String, String> map) {
        StringBuffer val = new StringBuffer();
        if (map != null && map.size() > 0) {
            for (String key : map.keySet()) {
                String v = map.get(key);
                if (val.length() > 0) {
                    val.append("&");
                }
                val.append(key).append("=");
                if (v != null) {
                    v = v.replaceAll("&", "%26");
                    v = v.replaceAll(",", "%2C");
                    v = v.replaceAll("=", "%3D");
                    v = v.replaceAll(":", "%3A");
                    v = v.replaceAll("/", "%2F");
                    val.append(v);
                }
            }
        }
        return val.toString();
    }

    /**
     * return true if there are only digits in the string
     */
    public static boolean allDigits(String s) {
        int     length = s.length();
        boolean retval = true;

        for (int i=0; i<length; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                retval = false;
                break;
            }
        }
        return retval;
    }

    public static <T extends Enum<T>> T getEnum(String name, T fallback) {
        T retval= fallback;
        if (name!=null) {
            try {
                retval = Enum.valueOf(fallback.getDeclaringClass(), name);
            } catch (Exception e) {
                // ignore and use fallback
            }
        }
        return retval;
    }

    public static <T extends Enum<T>> T getEnum(Class<T> enumType, String name, T fallback) {
        T retval= fallback;
        if (name!=null) {
            try {
                retval = Enum.valueOf(enumType, name);
            } catch (Exception e) {
                // ignore and use fallback
            }
        }
        return retval;
    }



    /**
     *
     * @param s a string
     * @return  Return true if the given string contains letters.
     */
    public static boolean containsLetters(String s) {
        boolean retval = false;

        if (s==null) return retval;

        int     length = s.length();
        for (int i=0; i<length; i++) {
            if (Character.isLetter(s.charAt(i))) {
                retval = true;
                break;
            }
        }
        return retval;
    }


    /**
     * Returns true is the given string is either null, or an empty string.
     * A string of white spaces is also considered empty.
     * @param s a string
     * @return Returns true is the given string is either null, or an empty string.
     */
    public static boolean isEmpty(String s) {
        return s == null || trim(s).length() == 0;
    }

    public static boolean isAnyEmpty(String... sAry) {
        for(String s : sAry) {
            if (isEmpty(s)) return true;
        }
        return false;
    }



    public static boolean hasContent(String s) {
        return !isEmpty(s);
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

    /**
     * Returns true if two strings have the same value (null is considered to be equal to "") 
     * @param s1 first string
     * @param s2 second string
     * @return true if two strings have the same value (null is considered to be equal to "")
     */
    public static boolean areEqual(String s1, String s2) {
        boolean s1Empty = isEmpty(s1);
        boolean s2Empty = isEmpty(s2);
        if (s1Empty && s2Empty) { return true; }
        else if (s1Empty || s2Empty) { return false; }
        else {
            return s1.equals(s2);
        }
    }

    public static String toString(Object[] objs, String separatedBy) {
        if (objs != null) {
            return toString(Arrays.asList(objs), separatedBy);
        } else {
            return "";
        }
    }
    
    public static String toString(int[] objs, String separatedBy) {
        ArrayList<Integer> l = new ArrayList<Integer>();
        for(int i : objs) {
            l.add(i);
        }
        return toString(l, separatedBy);
    }

    /**
     * Same as {@link #toString(java.util.Collection ,String) toString}
     * separated by ", ".
     *
     * @param c
     * @return
     */
    public static String toString(Collection c) {
        return toString(c, ", ");
    }

    /**
     * Returns a String representation of this collection.
     * It will use String.valueOf(Object) to convert object to string.
     * If separatedby is given, the items will be separated by that string.
     *
     * @param c
     * @param separatedBy
     * @return
     */
    public static String toString(Collection c, String separatedBy) {
        if (c == null || c.size() == 0) return "";

        StringBuffer sb = new StringBuffer();
        for (Iterator itr = c.iterator(); itr.hasNext(); ) {
            Object o = itr.next();
            sb.append(String.valueOf(o));
            if (separatedBy != null && itr.hasNext()) {
                sb.append(separatedBy);
            }
        }

        return sb.toString();
    }

    public static String getMegSizeAsString(long size) {
        return getSizeAsString(size,false,MEG);
    }

    public static String getKBSizeAsString(long size) {
        return getSizeAsString(size,false,K);
    }

    public static String getSizeAsString(long size) {
        return getSizeAsString(size,false);
    }
    public static String getSizeAsString(long size, boolean verbose) {
        return getSizeAsString(size, verbose, BYTE);

    }

    private static String getSizeAsString(long size, boolean verbose, long inUnit) {


        long meg= MEG/inUnit;
        long megTenth= MEG_TENTH/inUnit;
        long gig= GIG/inUnit;
        long gigHundredth= GIG_HUNDREDTH/inUnit;


        String retval= "0";
        String kStr= "K";
        String mStr= "M";
        String gStr= "G";
        if (verbose) {
            kStr= " K";
            mStr= " MB";
            gStr= " GB";
        }

        if (size > 0 && size < (1*meg)) {
            retval= ((size / K) + 1) + kStr;
        }
        else if (size >= (1*meg) && size <  (2*gig) ) {
            long megs = size / meg;
            long remain= size % meg;
            long decimal = remain / megTenth;
            retval= megs +"."+ decimal + mStr;
        }
        else if (size >= (2*gig) ) {
            long gigs = size /gig;
            long remain= size %gig;
            long decimal = remain / gigHundredth;
            retval= gigs +"."+ decimal + gStr;
        }
        return retval;
    }

    public static String toString(Object obj) {
        return obj == null ? "null" : obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
    }

    public static String[] split(String source, String delimiter) {
        return split(source, delimiter, Integer.MAX_VALUE);
    }

    public static String[] split(String source, String delimiter, int limit) {
        ArrayList<String> rvals = new ArrayList<String>();
        StringTokenizer vals = new StringTokenizer(source, delimiter);
        while (vals.hasMoreToken()) {
            if (rvals.size() >= limit) {
                break;
            }
            rvals.add(trim(vals.nextToken()));
        }
        return rvals.toArray(new String[rvals.size()]);
    }

    /**
     * this implementation is much faster than GWt's String.trim() once
     * compiled into javascript, especially in Firefox
     * @param str
     * @return
     */
    public static String trim(String str) {
        if (str == null || str.length() == 0) return str;

        String whitespace = " \t\n\f\r";
        int i = 0; int len = str.length();
        for (; i < str.length(); i++) {
            if (whitespace.indexOf(str.charAt(i)) == -1) {
                str = str.substring(i);
                break;
            }
        }
        if (i == len) {
            // empty string...
            return "";
        }

        for (i = str.length() - 1; i >= 0; i--) {
            if (whitespace.indexOf(str.charAt(i)) == -1) {
                str = str.substring(0, i + 1);
                break;
            }
        }
        return str;
    }

    /**
     * returns the length of the given string.
     * 0 if str is null
     * @param str
     * @return
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }

    public static boolean getBoolean(String str) {
        return Boolean.parseBoolean(str);
    }

    public static boolean getBoolean(String s, boolean def) {
        boolean retval= def;
        if (s!=null)  retval= Boolean.parseBoolean(s);
        return retval;
    }

    public static int getInt(String str) {
        return getInt(str, Integer.MIN_VALUE);
    }


    public static int getInt(String s, int def) {
        int retval= def;
        if (s!=null) {
            try {
                retval= Integer.parseInt(s);
            } catch (NumberFormatException e) {
                retval= def;
            }
        }
        return retval;
    }

    public static long getLong(String str) {
        return getLong(str, Long.MIN_VALUE);
    }

    public static long getLong(String s, long def) {
        long retval= def;
        if (s!=null) {
            try {
                retval= Long.parseLong(s);
            } catch (NumberFormatException e) {
                retval= def;
            }
        }
        return retval;
    }

    public static double getDouble(String str) {
        return getDouble(str, Double.NaN);
    }

    public static double getDouble(String s, double def) {
        double retval= def;
        if (s!=null) {
            try {
                retval= Double.parseDouble(s);
            } catch (NumberFormatException e) {
                retval= def;
            }
        }
        return retval;
    }

    public static float getFloat(String str) {
        return getFloat(str,Float.NaN);
    }

    public static float getFloat(String s, float def) {
        float retval= def;
        if (s!=null) {
            try {
                retval= Float.parseFloat(s);
            } catch (NumberFormatException e) {
                retval= def;
            }
        }
        return retval;
    }

    public static double parseDouble(String s) throws NumberFormatException {
        return s.equals("NaN") ? Double.NaN : Double.parseDouble(s);
    }

    public static float parseFloat(String s) throws NumberFormatException {
        return s.equals("NaN") ? Float.NaN : Float.parseFloat(s);
    }

    public static Date getDate(String str) {
        if (str != null) {
            try {
                return new Date(Long.parseLong(str));
            } catch (Exception e) {
                return new Date(str);
            }
        }
        return null;
    }

    public static String getVal(String s, String def) {
        return (s!=null) ?  s : def;
    }


    /**
     * removes extra spaces from a string.
     * <ul><li><code>" bbb    ccc  ddd"</code></li></ul>
     * should become:
     * <ul><li><code>aaa "bbb ccc ddd" eee</code></li></ul>
     */
    public static String crunch (String s) {
        if (!isEmpty(s)) {
            s= s.replaceAll("[ \t\n\r\f]", " ");
            s= s.trim();
            int counter = 0;
            StringTokenizer st = new StringTokenizer(s, " ");
            StringBuffer sb = new StringBuffer();
            String token = "";
            while (st.hasMoreToken()){
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

    public static String polishString(String str) {
        if (str!=null && str.length()>0) {
            StringBuilder sb= new StringBuilder(200);
            sb.append(str);
            str = convertExtendedAscii(sb);
            /*
            str= str.replaceAll(DASH_1, "-");
            str= str.replaceAll(DASH_2, "-");
            str= str.replaceAll(DASH_3, "-");
            str= str.replaceAll(DOUBLE_QUOTATION_MARK_1,"\"");
            str= str.replaceAll(DOUBLE_QUOTATION_MARK_2, "\"");
            str= str.replaceAll(DOUBLE_QUOTATION_MARK_3, "\"");
            str= str.replaceAll(DOUBLE_QUOTATION_MARK_4, "\"");*/
            
        }
        return str;
    }

    public static String convertExtendedAscii(StringBuilder sbOriginal)  {
        if (null==sbOriginal) {
            return null;
        }
        int origCharAsInt;
        for (int isb = 0; isb < sbOriginal.length(); isb++)  {
            origCharAsInt = (int) sbOriginal.charAt(isb);
            switch (origCharAsInt) {
                case ((int)'\u2018'): // left single quote
                case ((int)'\u2019'): // right single quote
                case ((int)'\u201A'): // lower quotation mark
                case ((int)'\u2039'): // Single Left-Pointing Quotation Mark
                case ((int)'\u203A'): // Single right-Pointing Quotation Mark
                    sbOriginal.setCharAt(isb, '\'');
                    break;

                case ((int)'\u201C'): // left double quote
                case 210:
                case ((int)'\u201D'): // right double quote
                case 211:
                case ((int)'\u201E'): // double low quotation mark  
                    sbOriginal.setCharAt(isb, '"');
                    break;

                case ((int)'\u02DC'):
                    sbOriginal.setCharAt(isb, '~');
                    break;  // Small Tilde

                case ((int)'\u2013'): // En Dash
                case ((int)'\u2014'): // EM Dash
                case 208:
                    sbOriginal.setCharAt(isb, '-');
                    break;

                default:
                    if (origCharAsInt >= 127) {
                        sbOriginal.setCharAt(isb, '?');
                    } else if (origCharAsInt < 32) {
                        // change TAB, LF, VT, FF, CR to be space
                        if (origCharAsInt >= 9 && origCharAsInt <= 13) {
                            sbOriginal.setCharAt(isb, ' ');
                        } else {
                            sbOriginal.setCharAt(isb, '?');
                        }
                    }
                    break;
            }
        }
        return sbOriginal.toString();
    }
    public static String convertDashedToCamel(String s) {
        StringBuilder sb= new StringBuilder(s.length());
        boolean nextCap= false;
        for(char c : s.toCharArray()) {
            if (c=='-') {
                nextCap= true;
            }
            else {
                sb.append(nextCap ? Character.toUpperCase(c) : c);
                nextCap= false;
            }
        }
        return sb.toString();
    }

    public static String escapeQuotes(String s) {
        StringBuilder sb= new StringBuilder(s.length()+30);
        for(char c : s.toCharArray()) {
            if (c=='"' || c=='\'' || c=='\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static Map<String,String> createStringMap(String... sAry) {
        Map<String,String> map= new HashMap<String, String>(sAry.length+7);
        if (sAry.length>=2) {
            int max= sAry.length - (sAry.length %2);
            for(int i= 0; (i<max); i+=2) {
                map.put(sAry[i], sAry[i+1]);
            }
        }
        return map;
    }

    //=========================================================================================
    //---------- File string utilities copied from FileUtil to be used in gwt ---------------
    //=========================================================================================


    public static String getFileBase(String s) {
        String base;
        int i = s.lastIndexOf('.');
        if (i==-1 || i==0) {
            base= s;
        }
        else {
            base = s.substring(0, i);
        }
        return base;
    }

    public static String stripFilePath(String path) {
        String base;
        char slash= '/';
        int i = path.lastIndexOf(slash);
        if (i == -1 || i == 0)  base = path;
        else                    base = path.substring(i + 1, path.length());
        return base;
    }

    //=========================================================================================
    //---------- HandSerialize support methods- might break into another class  ---------------
    //=========================================================================================

    public final static String STRING_SPLIT_TOKEN= "--STR--";

    public static String checkNull(String s) {
        if (s==null)               return null;
        else if (s.equals("null")) return null;
        else                       return s;
    }

    public static String[] parseHelper(String s, int max, String splitToken) throws IllegalArgumentException {

        String sAry[]= null;
        if (!isEmpty(s)) {
            sAry= s.split(splitToken,max+1);
            if (sAry.length>max)  sAry= null;
        }
        if (sAry==null) throw new IllegalArgumentException("wrong number of tokens in String");
        return sAry;

    }
    public static String combineList(String token, List<String> parts) {
        return "[" + combine(token,parts.toArray(new String[parts.size()])) + "]";
    }

    public static String combineAry(String token, String... parts) {
        return "[" + combine(token,parts) + "]";
    }

    public static String combine(String token, String... parts) {
        int size= 0;
        for(String p : parts) size+= p!=null ? p.length() : 0;
        StringBuffer sb= new StringBuffer(size+ (token.length()*parts.length) + 50);
        for(int i=0; (i<parts.length); i++) {
            sb.append(parts[i]);
            if (i<parts.length-1) sb.append(token);
        }
        return sb.toString();
    }

    public static Map<String,String> parseStringMap(String s,String token) {
        Map<String,String> map= Collections.emptyMap();
        if (s.startsWith("[") && s.endsWith("]")) {
            s= s.substring(1,s.length()-1);
            String sAry[]= s.split(token,500);
            map= new HashMap<String,String>(sAry.length/2+ 17);
            for(int i= 0; (i<sAry.length-1); i+=2) {
                if (!StringUtils.isEmpty(sAry[i])  && !StringUtils.isEmpty(sAry[i+1])) {
                    map.put(sAry[i],sAry[i+1]);
                }
            }
        }
        return map;
    }

    public static List<String> parseStringList(String s,String token, int max) {
        List<String> list= Collections.emptyList();
        if (s.startsWith("[") && s.endsWith("]")) {
            s= s.substring(1,s.length()-1);
            String sAry[]= s.split(token,max);
            list= new ArrayList<String>(sAry.length);
            for(String entry  : sAry) {
                if (!StringUtils.isEmpty(entry)) list.add(entry);
            }
        }
        return list;
    }
    public static List<String> parseStringList(String s,String token) {
        return parseStringList(s,token,500);
    }

    public static List<String> parseStringList(String s) {
        if (!isEmpty(s)) {
            return parseStringList(s,STRING_SPLIT_TOKEN);
        }
        else {
            return Collections.emptyList();
        }
    }

    public static String combineStringList(List<String> sList) {
        return combineList(STRING_SPLIT_TOKEN,sList);
    }

    //=========================================================================================
    //---------- End HandSerialize support methods---------------------------------------------
    //=========================================================================================



 }
