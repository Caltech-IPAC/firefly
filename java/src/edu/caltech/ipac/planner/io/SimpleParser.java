package edu.caltech.ipac.planner.io;

import java.io.BufferedReader;
import java.io.IOException;

/**
 */
public class SimpleParser {

    private String _line="";
    private int _lineNumber=0;
    private BufferedReader _in;
    boolean readNextLine= true;

    public SimpleParser(BufferedReader in) {
        _in= in;
    }


    /**
     * Method will return the value of the keyword passed in.
     * If the keyword is not found in file it will throw IOException
     * If there is a different keyword found it will throw IOException.
     */
    public String GetKeywordValue(String keyword)  throws IOException {
        String nextKeywordFound="";
        boolean differentKeyword=false;

        _line=getNewLine();

        while(_line!=null && !differentKeyword) {

            int colon_location=_line.indexOf(":");
            if (_line.equals("")) {
                _line=getNewLine();
            }
            else if(colon_location==-1) {
                readNextLine= false;
                differentKeyword=true;  //this will stop reading anymore lines.
            }
            else {
                nextKeywordFound=_line.substring(0, colon_location).trim();

                if(nextKeywordFound.equalsIgnoreCase(keyword)) {
                    String value=_line.substring(colon_location+1,
                                                 _line.length());
                    return value.trim();
                }
                else {
                    readNextLine= false;
                    differentKeyword=true;  //this will stop reading anymore lines.
                }
            }
        }

        // two reasons to be here found another keyword, or reached EOF
        if(differentKeyword) {
            throw new IOException("Found (keyword) "+nextKeywordFound+
                                  ", before (expected keyword) "+keyword);
        }
        else {
            throw new IOException("Error, keyword "+keyword+
                                  ", not found in aor");
        }
    }

    public String getNewLine() throws IOException {
        String retval= _line;
        if (readNextLine) retval=cleanUp(_in.readLine());
        readNextLine= true;
        return retval;
    }

    private String cleanUp(String line) {
        if(line!=null) {
            _lineNumber++;
            if((line.indexOf("#")==-1) && (line.indexOf("!")==-1)) {
                String lineOut=line;
                return lineOut.trim();
            }
            else {
                int index1=line.indexOf("#");
                int index2=line.indexOf("!");
                int index=index2;
                if(index1!=-1 && index2!=-1) {
                    if(index1<index2) {
                        index=index1;
                    }
                }
                else if(index1==-1) {
                    index=index2;
                }
                else {
                    index=index1;
                }
                String clean=line.substring(0, index);
                return clean.trim();
            }
        }
        else {
            return line;
        }
    }

    public int getLineCount() { return _lineNumber; }

    public float getFloat(String s) throws IOException {
        Float f=null;
        try {
            f=new Float(s);
        } catch (NumberFormatException ne) {
            throw new IOException("Invalid float field value: "+s);
        }
        return f.floatValue();
    }

}
