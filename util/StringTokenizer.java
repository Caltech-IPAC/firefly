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
