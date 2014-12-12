/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;


import java.util.Hashtable;
import java.util.Vector;
import java.util.StringTokenizer;
import java.io.*;


/**
 *  A class used to read table formatted ASCII files,
 *  such as may be generated from an Excel file.
 *  <p>
 *
 *  The table is assumed to follow this format : each table row
 *  corresponds to a line in the ASCII file, and each cell is
 *  delimited by a specifiable set of delimiter characters. The default
 *  delimiter string is ";" (whitespace cannot be used as a delimiter),
 *  so an example row with 8 entries is :
 *
 *  <p><code>
 *  akey;b; c ; d ;  ;;   e;f
 *  </code><p>
 *
 *  The parser will parse this and store the information in the following way :
 *  <ul>
 *  <li> the first row entry is reated as a hashtable key
 *  <li> the remaining row entries are inserted into a <code>java.util.Vector</code>
 *  <li> the key and it's corresponding <code>java.util.Vector</code> value are stored
 *       in a <code>java.util.Hashtable</code>
 *  </ul>
 *
 *  So in the example the key would be the string <code>"akey"</code>, and the
 *  <code>java.util.Vector</code> would contain the following values :
 *  <ul>
 *  <li> element 0 is the string <code>"b"</code>
 *  <li> element 1 is the string <code>"c"</code>
 *  <li> element 2 is the string <code>"d"</code>
 *  <li> element 3 is the string <code>""</code>
 *  <li> element 4 is the string <code>""</code>
 *  <li> element 5 is the string <code>"e"</code>
 *  <li> element 6 is the string <code>"f"</code>
 *  </ul>
 *
 *  A row entry is obtained by obtaining all the characters between 2 delimiter
 *  characters, and then stripping leading and trailing whitespace (which may produce
 *  an empty string). Consecutive delimiters are assumed to sandwich empty row entries,
 *  which shows up in the <code>Vector</code> as a an empty string ("").
 *  <p>
 *  If the very first non-whitespace character is a delimiter, then the hastable key
 *  is assumed to be the empty string. This situation is almost guaranteed to indicate
 *  that the input file is not formatted correctly, however it will get through the parser.
 *  <p>
 *  This line :
 *  <p><code>
 *  ;;;
 *  </code><p>
 *  Will yield a hashtable key of <code>""</code> and the key's value would be this
 *  <code>java.util.Vector</code> :
 *  <ul>
 *  <li> element 0 is the string <code>""</code>
 *  <li> element 1 is the string <code>""</code>
 *  <li> element 2 is the string <code>""</code>
 *  </ul>
 *  since there assumed to be a blank row entry after the last occurence of the delimiter.
 *  <p>
 *  The default comment character is '#' : lines beginning
 *  with the comment character are ignored by the parser.
 *  Nested comments are not allowed, but comments are allowed after the end of all row data.
 *  This type of comment (after the end of row data) cannot contain any delimiter characters,
 *  otherwise it will be treated as data and chopped up. This allows for the occurence of the
 *  comment character inside the row entries.
 *
 *  @author G. Turek
 *  @version $Id: TableReader.java,v 1.3 2005/12/08 22:31:15 tatianag Exp $
 */
public class TableReader
{

    private char           comment = '#';
    private Hashtable      data;
    private BufferedReader file;


    /**
     * Constructor that reads a table from a <code>java.io.BufferedReader</code>.
     * <p>
     * Note that all whitespace is removed from <code>sep</code> before it is
     * used as a delimiter string for <code>java.util.StringTokenizer</code>. If
     * <code>sep</code> contains only whitespace, then the default delimiter string
     * ";" is used.
     * <p>
     * The comment character is assigned it's default value ('#').
     *
     * @param br            the input stream to parse data from
     * @param sep           separator character(s)
     */
    public TableReader(BufferedReader br, String sep)
        throws FileNotFoundException,
               IOException
    {
        file = br;
        data = new Hashtable();
        parseFile(checkDelim(sep));
    }


    /**
     * Constructor that reads a table from a <code>java.io.BufferedReader</code>.
     * <p>
     * Note that all whitespace is removed from <code>sep</code> before it is
     * used as a delimiter string for <code>java.util.StringTokenizer</code>. If
     * <code>sep</code> contains only whitespace, then the default delimiter string
     * ";" is used.
     *
     * @param br            the input stream to parse data from
     * @param sep           separator character(s)
     * @param c             the comment character (assumed to comment out one line,
     *                      if whitespace is specified the comment character is assumed
     *                      to be '#')
     */
    public TableReader(BufferedReader br, String sep, char c)
        throws FileNotFoundException,
               IOException
    {
        if(Character.isWhitespace(c)) comment = '#';
        else comment = c;
        file = br;
        data = new Hashtable();
        parseFile(checkDelim(sep));
    }



    /**
     * Constructor that reads a table from the specified file.
     * <p>
     * Note that all whitespace is removed from <code>sep</code> before it is
     * used as a delimiter string for <code>java.util.StringTokenizer</code>. If
     * <code>sep</code> contains only whitespace, then the default delimiter string
     * ";" is used.
     * <p>
     * The comment character is assigned it's default value ('#').
     *
     * @param fileName      the input file name
     * @param sep           separator character(s)
     */
    public TableReader(String fileName, String sep)
        throws FileNotFoundException,
               IOException
    {
        FileReader filer = null;
        try
        {
            data = new Hashtable();
            filer = new FileReader(fileName);
            file = new BufferedReader(filer);
            parseFile(checkDelim(sep));
        }
        finally
        {
            try
            {
                if(file != null) file.close();
                else if(filer != null) filer.close();
            }
            catch(Exception ignore) {}
        }
    }


    /**
     * Constructor that reads a table from the specified file.
     * <p>
     * Note that all whitespace is removed from <code>sep</code> before it is
     * used as a delimiter string for <code>java.util.StringTokenizer</code>. If
     * <code>sep</code> contains only whitespace, then the default delimiter string
     * ";" is used.
     *
     * @param fileName      the input file name
     * @param sep           separator character(s)
     * @param c             the comment character (assumed to comment out one line,
     *                      if whitespace is specified the comment character is assumed
     *                      to be '#')
     */
    public TableReader(String fileName, String sep, char c)
        throws FileNotFoundException,
               IOException
    {
        FileReader filer = null;
        try
        {
            if(Character.isWhitespace(c))
                comment = '#';
            else comment = c;
            data = new Hashtable();
            filer = new FileReader(fileName);
            file = new BufferedReader(filer);
            parseFile(checkDelim(sep));
        }
        finally
        {
            try
            {
                if(file != null) file.close();
                else if(filer != null) filer.close();
            }
            catch(Exception ignore) {}
        }
    }


    /**
     * Strip the token separator string (array of characters)
     * of all whitespace. If the token separator contains only
     * whitespace the seprator is assumed to be the ';' character.
     */
    private String checkDelim(String sep)
    {
        if((sep == null) || (sep.length() == 0))
            return ";";
        char[] sc = sep.toCharArray();
        int numw, i;
        for(i = 0, numw = 0; i < sc.length; i++)
            if(!Character.isWhitespace(sc[i])) numw++;
        if(numw == 0) return ";";
        char[] rc = new char[numw];
        for(i = 0, numw = 0; i < sc.length; i++)
            if(!Character.isWhitespace(sc[i])) rc[numw++] = sc[i];
        return new String(rc);
    }


    /**
     * Parse the table file one line at a time
     */
    private void parseFile(String sep) throws IOException
    {
        String          line;
        String          name;
        String          val;
        StringTokenizer st;
        Vector          values;
        boolean         lastTkSep;

        line = file.readLine();
        while (line != null)
        {
          lastTkSep = false;
          // trim leading and trailing whitespace in the line
          line = line.trim();
          // skip empty lines
          if(line.length() > 0)
          {
            // skip comment lines
            if (line.charAt(0) != comment)
            {
              st = new StringTokenizer(line, sep, true);
              values = new Vector();
              // trim leading and trailing whitespace from name
              name = st.nextToken();
              // if the first token is a separator, name = ""
              if(sep.indexOf(name) != -1)
              {
                name = "";
                lastTkSep = true;
              }
              else name = name.trim();
              while (st.hasMoreTokens())
              {
                val = st.nextToken();

                if(sep.indexOf(val) != -1)
                {
                  // if the previous token was a separator insert a blank
                  if(lastTkSep == true)
                  {
                    values.addElement("");
                    // if the last token in the String is a separator add a blank
                    if(!st.hasMoreTokens())
                    {
                      values.addElement("");
                      break;
                    }
                    continue;
                  }
                  else
                  {
                    // if the last token in the String is a separator add a blank
                    if(!st.hasMoreTokens())
                    {
                      values.addElement("");
                      break;
                    }
                    else
                    {
                      lastTkSep = true;
                      continue;
                    }
                  }
                }
                else lastTkSep = false;
                val = val.trim();
                // if this is not the last token in the line always
                // add it to the Vector values
                if(st.hasMoreTokens())
                  values.addElement(val);
                else if(val.length() > 0)
                {
                  // As long as the last token in the line does not start
                  // with the comment character add it to the values vector
                  if(val.charAt(0) != comment)
                    values.addElement(val);
                }
                else values.addElement(val);
              }
              data.put(name,values);
            }
          }
          line = file.readLine();
        }
    }


    /**
     *  Returns table representation in the form of
     *  a hashtable whose key is the entry in the 1st column,
     *  and the value is a Vector corresponding to entries in
     *  successive columns
     *  @return Hashtable
     */
    public Hashtable getTable() { return data; }

}

// =================================================================
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
