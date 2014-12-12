package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.gui.BaseOpenAction;
import edu.caltech.ipac.gui.DialogSupport;
import edu.caltech.ipac.gui.ErrorReporter;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.util.FileReadStatusException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * This is the base class for OpenFixedSingleTarget and OpenMovingSingleTarget
 * 
 * @author Xiuqin Wu
 */

abstract public class OpenSpecialTarget extends BaseOpenAction {
    
    public static  final int MAX_NUMBER_OF_ERRORS = 20;
    public static  final String LINE_FEED = "\r\n";
    public static String FORMAT_ERROR = "Format error in the following line:" +
                                   LINE_FEED;

    private TargetList     _targets;

    private static String DIRECTORY_PROP = "sut.io.lastWorkingDirectory";


    public OpenSpecialTarget(JFrame f, TargetList targets) {
        super(f);
        _targets = targets;
    }
    
    protected FileFilter[] getFilterArray() { return null; }
    

    protected String getDirProperty() { return DIRECTORY_PROP; } 

    protected JComponent makeAccessory() {
        return new PlainTextFilePreview(getFileChooser());
    }

    /**
     * Read the file
     */
    protected void doOpen(File f) throws IOException { 
        _targets.beginBulkUpdate();
        DialogSupport.setWaitCursor(true);
        try{
            BufferedReader in = new BufferedReader(new FileReader(f));
            readTgts(in, _targets);
            in.close();
        }
        catch(IOException pe){
            //ErrorReporter errorReporter = new ErrorReporter(
            //                 getPlotFrame(),"Error(s) Found", pe.getMessage());
            //errorReporter.init();
            //errorReporter.show();
            ErrorReporter.showError( getFrame(), "Error(s) Found",
                                        pe.getMessage());
        }
        DialogSupport.setWaitCursor(false);
        _targets.endBulkUpdate();
    }
 
    
    
    protected int getNumberOfQuotes(String str){
        int counter = 0;
        String sub = str;
        while (sub.indexOf("\"") != -1 && counter<8){
            counter++;
            sub = sub.substring(sub.indexOf("\"")+1);
        }
        return counter;
    }
    
    protected String cleanUp (String line){
      System.out.println("cleanUp.line: " + line);
      if (line == null)
	 return line;

      int index1 = line.indexOf("#");
      int index2 = line.indexOf("!");
      int index;
      String clean = null;

      if (index1 == -1 && index2 == -1){
	  clean = line.trim();
      }
      else {
	  index = index2;
	  if (index1 != -1 && index2 != -1){
	      if (index1<index2){
		  index=index1;
	      }
	  }
	  else if (index1 == -1){
	      index = index2;
	  }
	  else {
	      index=index1;
	  }
	  clean = line.substring(0,index).trim();
      }
       return clean;
    }

    /**
     * parse the input line and put all elements in a array of String.
     * <p>
     * To keep this method backward compatible, it will return an array
     * of at least 8 elements.  If there is more than 8, it will return
     * an array of n elements.
     *
     * @param nextTarget the input line to parse
     * @throws FileReadStatusException
     */
    public String[] getElements(String nextTarget)
                                          throws FileReadStatusException {

      int minArySize = 8;  // set the minimum array size to return.

      if (nextTarget == null){
	throw new FileReadStatusException("String is null");
	}
      ArrayList<String> strings = new ArrayList<String>();

      StringTokenizer st = new StringTokenizer(nextTarget);
      String buffer = null;
      int stringsCounter = 0;
      try {
	 String chunk;
	 int numberOfQuotes;
	 while (st.hasMoreTokens() && stringsCounter < 8) {
	    chunk = st.nextToken();
	    if (chunk.indexOf("\"") == -1){
               strings.add(chunk);
	/*
	System.out.println("chunck: " + chunk);
	System.out.println("getStrings: " + strings[0]); 
	System.out.println("stringsCounter: " + stringsCounter);
	*/
	       continue;   // finished one element
	    }

	    numberOfQuotes = getNumberOfQuotes(chunk);
	    if (numberOfQuotes > 2){
	       throw new FileReadStatusException(FORMAT_ERROR +nextTarget);
	    }
            // no more than 2 quotes
	    if (!(chunk.startsWith("\"")) ){
		throw new FileReadStatusException(FORMAT_ERROR+nextTarget);
	    }
	    // the first character is first quote 
	    if (numberOfQuotes == 2){
	       if(!chunk.endsWith("\"")){
		   throw new FileReadStatusException(FORMAT_ERROR +nextTarget);
	       }
	       else{
                   strings.add(chunk.substring(1,chunk.length()-1));
	       }
	    }
	    else{ // one quote
	       buffer = chunk.substring(1);
	       int panic = 0;
	       boolean completedQuotedToken = false;
	       
	       while(st.hasMoreTokens() && !completedQuotedToken){
		   panic++;
		   if (panic > 40){
		       throw new FileReadStatusException(
                                                    FORMAT_ERROR+nextTarget);
		   }
		   String chunk2 = st.nextToken();
		   
		   if (chunk2.indexOf("\"") == -1){
		       buffer = buffer+" "+chunk2;
		   }
		   else {
		      if (getNumberOfQuotes(chunk2) > 1){
		         throw new FileReadStatusException(
                                                    FORMAT_ERROR+nextTarget);
		      }
		      // only one quote in chunk2
		      if(!chunk2.endsWith("\"")){
			 throw new FileReadStatusException("numberof quotes: "+
			    getNumberOfQuotes(chunk2)+",chunk="+chunk2+", "
			    +FORMAT_ERROR+nextTarget);
			}
		      // chunk2 ends with quote
		      buffer = buffer + " " +
		               chunk2.substring(0,chunk2.length()-1);
                      strings.add(buffer);
		      completedQuotedToken = true;
		   }
	       }  // while
	    } // one quote
	  }

          if (strings.size() < minArySize) {
              // if the return array size is less than the mininum, fill it with null
              for(int i = strings.size(); i < minArySize; i++) {
                  strings.add(null);
              }
          }

	  return strings.toArray(new String[0]);
      } catch(NoSuchElementException e){
	  throw new FileReadStatusException("Target data is missing in line: "+
	                               nextTarget);
      }
   }
    


    abstract public void readTgts (BufferedReader in, TargetList _targets)
                                                           throws IOException;
    
    
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
