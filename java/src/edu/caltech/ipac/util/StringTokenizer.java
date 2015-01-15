/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

/**
 * Date: Apr 24, 2009
 *
 * @author loi
 * @version $Id: StringTokenizer.java,v 1.1 2009/11/06 01:24:21 loi Exp $
 */
public class StringTokenizer {
    private final String string;
    private String delimiter;
    private boolean isDelimReturned;
    private final int delimLength;
    private int current;

    public StringTokenizer(String string, String delimiter) {
        this(string, delimiter, false);
    }

    public StringTokenizer(String string, String delimiter, boolean delimReturned) {

        if (StringUtils.isEmpty(string) || delimiter == null || delimiter.length() == 0) {
            throw new IllegalArgumentException("Both string and delimiter must not be null or empty");
        }
        this.string = string;
        this.delimiter = delimiter;
        this.delimLength= delimiter.length();
        isDelimReturned = delimReturned;
    }

    public boolean hasMoreToken() {
        return current < string.length();
    }

//    public String nextToken() {
//        return nextToken(delimiter);
//    }

    public String getRestOfString() {
        String retval= string.substring(current);
        current= string.length();
        return retval;
    }

    public String nextToken() {
        return nextToken(delimiter);
    }

    public String nextToken(String delimiter) {
        this.delimiter= delimiter;
        int start = current;
        int end = indexOf(start,delimiter);
        if (end < 0) {
            end = string.length();
            current = string.length();
        } else if (end == start && isDelimReturned) {
            end = start + 1;
            current = end;
        } else {
            current = isDelimReturned ? end : movePastDelim(end);
        }
        return string.substring(start, end);
    }

    private int movePastDelim(int start) {
        int len= string.length();
        for(int i=start; (i<len); i++) {
            if (!isDelim(string.charAt(i))) {
                return i;
            }
        }
        return string.length();
    }

    private boolean isDelim(char c) { return delimiter.indexOf(c)>-1; }

    private int indexOf(int start,String delimiter) {
        if (delimLength==1) {
            return string.indexOf(delimiter, start);
        }
        else {
            int len= string.length();
            for(int i=start; (i<len); i++) {
                if (isDelim(string.charAt(i))) return i;
            }
        }
        return -1;
    }


    public static void main(String[] args) {

        System.out.println("Type enter");
//        try {
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//        String test = "this is; just a; test ; string; no need to understan;;d it.;";
        String test= "point    52.293221    31.366456 # point=circle";
        String tok= "(), ";


        System.out.println("TEST -- delim not returned");
//        StringTokenizer st = new StringTokenizer(test, ";");
        StringTokenizer st = new StringTokenizer(test, tok);
        while (st.hasMoreToken()) {
            System.out.println("\"" + st.nextToken() + "\"");
        }


        System.out.println("TEST -- return delim");
        st = new StringTokenizer(test, tok,true);
//        st = new StringTokenizer(test, ";", true);
        while (st.hasMoreToken()) {
            System.out.println("\"" + st.nextToken() + "\"");
        }


    }
}
