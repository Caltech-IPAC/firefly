/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*****************************************************************************
 * Copyright (C) 2008 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

import java.util.regex.*;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Represents hyperlink: URL plus description
 * Provides methods to parse hyperlinks from a string
 */
public class HREF {

    final static String DEF_DESC = "link";

    final static Pattern HREF_PATTERN = Pattern.compile("<a\\s+href\\s*=\\s*[\\\"'](.+?)[\\\"'](.*?)>(.*?)</a>", Pattern.CASE_INSENSITIVE);
    final static Pattern LINK_PATTERN = Pattern.compile("(?:\\\"|'|\\s*)((?:http://|https://|ftp://).+?)(?:\\\"|'|\\s+|$)", Pattern.CASE_INSENSITIVE);

    private URL _url;
    private String _desc;
    private String _source;

    public HREF(URL url) {
        this(url, DEF_DESC);
    }

    public HREF(URL url, String desc) {
        _url = url;
        _desc = desc;
        _source = null;
    }


    public URL getURL() {
        return _url;
    }

    public String getDesc() {
        return _desc;
    }

    /**
     * @param source from which hyperlink is parsed
     */
    public void setSource(String source) {
        _source = source;
    }

    /**
     * @return source from which hyperlink is parsed
     */
    public String getSource() {
        if (_source != null)
            return _source;
        else
            return _url.toString();
    }

    public String toString() {
         return getSource();
    }


    /**
     * This method checks whether the string represents a hyperlink.
     * If the string can not be converted to HREF object, null is returned.
     * @return HREF or null if the string is not recognized as a single hyperlink
     * @param s string to parse
     */
    public static HREF parseHREF(String s) {

        HREF href = null;
        try {
            Matcher matcher = HREF_PATTERN.matcher(s);
            if (matcher.matches()) {
                if (matcher.groupCount() == 3) {
                    href = new HREF(new URL(matcher.group(1)), matcher.group(3));
                    href.setSource(s);
                }
            } else {
                matcher = LINK_PATTERN.matcher(s);
                if (matcher.matches() && matcher.groupCount()==1) {
                    href = new HREF(new URL(matcher.group(1)));
                }
            }

        } catch (MalformedURLException e) {
            // the string appears to represent hyperlink, but the url is invalid    
        }
        return href;
    }

    /**
     * @param text to parse
     * @return array of HREF objects
     */
    public static HREF[] parseHREFs(String text) {
        ArrayList<HREF> ret = new ArrayList<HREF>();
        Matcher matcher = HREF_PATTERN.matcher(text);
        int groupCount;

        while (matcher.find()) {
            groupCount = matcher.groupCount();
            //System.out.println("Number of groups: "+ groupCount);
            //for (int i=1; i<=groupCount; i++) {
            //	System.out.println("   Group "+i+": "+matcher.group(i));
            //}
            if (groupCount == 3) {
                try {
                    HREF href = new HREF(new URL(matcher.group(1)), matcher.group(3));
                    href.setSource(matcher.group(0));
                    ret.add(href);
                    //System.out.println("Adding href");
                } catch (MalformedURLException e) {
                    System.out.println("Unable to create URL from "+matcher.group(1)+" - "+e.getMessage());
                }
            }
        }

        // if href not found, try to parse plain links
        if (ret.size() < 1) {
            matcher = LINK_PATTERN.matcher(text);
            while (matcher.find()) {
                groupCount = matcher.groupCount();
                //System.out.println("Number of groups: "+ groupCount);
                //for (int i=1; i<=groupCount; i++) {
                //    System.out.println("    Group "+i+": "+matcher.group(i));
                //}
                if (groupCount == 1) {
                    try {
                        HREF href = new HREF(new URL(matcher.group(1)));
                        ret.add(href);
                        //System.out.println("Adding url");
                    } catch (MalformedURLException e) {
                        System.out.println("Unable to create URL from "+matcher.group(1)+" - "+e.getMessage());
                    }
                }
            }
        }
        return ret.toArray(new HREF[0]);
    }

    public static boolean isHREF(String s) {
        return LINK_PATTERN.matcher(s).matches() || HREF_PATTERN.matcher(s).matches();
    }

    public static void main(String [] args) {
        if (args.length > 0) {
            HREF[] hrefs = HREF.parseHREFs(args[0]);
            System.out.println("\nParsed "+hrefs.length+" hyperlinks.");
            for (HREF href : hrefs) {
                System.out.println("_____________________________________");
                System.out.println("Source : " + href.getSource());
                System.out.println("  [URL] "+href.getURL().toString());
                System.out.println("  [DESC] "+href.getDesc());
            }
        } else {
            System.out.println("Please, provide an argument with a text which contains hyperlinks");
        }
        System.exit(0);
    }
}
