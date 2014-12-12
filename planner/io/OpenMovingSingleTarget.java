package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.gui.ErrorReporter;
import edu.caltech.ipac.target.StandardEphemeris;
import edu.caltech.ipac.target.TargetMovingSingle;
import edu.caltech.ipac.target.validate.InvalidTargetException;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.targetgui.net.HorizonsEphPairs;
import edu.caltech.ipac.targetgui.net.TargetNetwork;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * This class will read in list of moving target names and add each to the
 * TargetList if it can be resolved into NAIF ID
 * 
 * @author Xiuqin Wu
 */

public class OpenMovingSingleTarget extends OpenSpecialTarget{
    
    private int     _lineNumber = 0;

    private String NO_NAME  = "<No Name>";
    
    public OpenMovingSingleTarget(JFrame f, TargetList targets) {
        super(f, targets);
        setDialogTitle( "Read Moving Single Target List");
    }
    

    public void readTgts (BufferedReader in, TargetList targets) 
                                                   throws IOException {
        StringBuffer errorBuffer = new StringBuffer();
        int    errorsFound = 0;
	String nextTarget = null;

        StandardEphemeris    eph;
        String               elements[] = new String[2];
        String               name;
        String               naifName;
        int               naifId;

	 try{
	     nextTarget = cleanUp(in.readLine());
	     _lineNumber++;
	 }
	 catch(IOException e){
	 };
	 
	 // when nextTarget == null, it reaches the end of file
	 while (nextTarget != null && errorsFound<MAX_NUMBER_OF_ERRORS ){
	   if (nextTarget.length() > 2) {
	    try {
	       elements = getNameId(nextTarget);

	       if (elements[0] != null && elements[1] != null) {
		  name = elements[0];
		  naifId = Integer.parseInt(elements[1]);
		  naifName = getNaifName(Integer.toString(naifId));
		  }
	       else if (elements[0] != null) {
		  name = elements[0];
		  naifId = getNaifId(elements[0]);
		  naifName = name;
	          }
	       else  {// if (elements[1] != null) 
		  naifId = Integer.parseInt(elements[1]);
	          name = getNaifName(elements[1]);
	          naifName = name;
		  }
	       if (naifName.equals(NO_NAME)) naifName= "";
               eph = new StandardEphemeris(naifId, naifName);
               targets.addTarget(new TargetMovingSingle(name,eph));
	   } catch(NumberFormatException nfe){
	       errorBuffer.append("Line number: " + _lineNumber + LINE_FEED +
			   "Message (parser): " +
			   "Integer is expected for NAID ID." + LINE_FEED );
	       errorsFound++;
//	   } catch(ParseAORException e){
//	       errorBuffer.append("Line number: " + _lineNumber + LINE_FEED +
//			   "Message (parser): " + e.getMessage()+ LINE_FEED );
//	       errorsFound++;
	   } catch(InvalidTargetException te){
	       errorBuffer.append( "Line number: " + _lineNumber + LINE_FEED +
		  "Message (validator):" + te.getMessage() + LINE_FEED );
	       errorsFound++;
	   }
	   if (errorsFound > MAX_NUMBER_OF_ERRORS){
	      errorBuffer.append("Too many errors: " + 
		       MAX_NUMBER_OF_ERRORS +" errors reached."+LINE_FEED);
	      break;
	      }
	  }
	   try{
	      nextTarget = cleanUp(in.readLine());
	      _lineNumber++;
	   } catch(IOException e){ };
	 }
	 
	 if (errorsFound>0){
             ErrorReporter.showError( getFrame(), "Error(s) Found",
                                         errorBuffer.toString());
//	     ErrorReporter errorReporter =
//		new ErrorReporter(getPlotFrame(),"Error(s) Found",
//				 errorBuffer.toString());
//	     errorReporter.init();
//	     errorReporter.show();
	 }
        //parser.resetLineCount();
    }
    
    // string[0] is name, string[1] is id


    private String[] getNameId(String nextTarget) throws IOException {
        String elements[] = {null, null};

        if (nextTarget == null) throw new IOException("String is null");
        String parts[]= StringUtil.strToStrings(nextTarget);

        if(parts.length==1) {
            if (StringUtils.allDigits(parts[0])){
                elements[1] = parts[0];
            }
            else {
                elements[0] = parts[0];
            }
        }
        else if(parts.length==2) {
            elements[0] = parts[0];
            elements[1] = parts[1];
            if (!StringUtils.allDigits(elements[1])) {
                throw new IOException("NAIF ID can only have digits: " + nextTarget);
            }
        }
        else if(parts.length>2) {
            throw new IOException(FORMAT_ERROR+nextTarget);
        }

        return elements;
    }




    private String[] getNameId2(String nextTarget) throws IOException {
      String tmps;
      String elements[] = new String [2];
      int    numberOfQuotes;

      if (nextTarget == null){
	throw new IOException("String is null");
	}
      elements[0] = null;
      elements[1] = null;

      if (nextTarget.indexOf("\"") == -1){
	    StringTokenizer st = new StringTokenizer(nextTarget);
	    if (st.countTokens() >= 2) {
	       elements[0] = st.nextToken();
	       elements[1] = st.nextToken();
	    }
	    else if (st.hasMoreTokens()) {
	       tmps = st.nextToken();
	       if (StringUtils.allDigits(tmps)){
	          elements[0] = null;
	          elements[1] = tmps;
	       }
	       else {
	          elements[0] = tmps;
	          elements[1] = null;
	       }
	   }
	 }
      else {
	 numberOfQuotes = getNumberOfQuotes(nextTarget);
	 if (numberOfQuotes  != 2){
	    throw new IOException(FORMAT_ERROR +nextTarget);
	 }
	 else {
	    if (!(nextTarget.startsWith("\"")) ){
	      throw new IOException(FORMAT_ERROR+nextTarget);
	      }
	    // the first character is first quote 
	    int endQuote = nextTarget.indexOf("\"", 1);
	    tmps = nextTarget.substring(1, endQuote).trim();
	    if (tmps.length() == 0) tmps = null;
	    elements[0] = tmps;

	    if (endQuote == nextTarget.length()-1)
	       tmps = null;
	    else
	       tmps = nextTarget.substring(endQuote+1).trim();
	    if (tmps == null)
	       elements[1] = null;
	    else if (tmps.length() == 0)
	       elements[1] = null;
	    else if (StringUtils.allDigits(tmps))
	       elements[1] = tmps;
	    else
	      throw new IOException("NAIF ID can only have digits: " + tmps);
	 }
      }

      return elements;
   }

   private int getNaifId(String name) throws InvalidTargetException {
      int id = -1;
      //EphemerisPairs ephPairs = EphemerisPairs.getInstance();
      //String [] strs = ephPairs.getID(name);
	 try {
	  //   strs = RemoteEphemerisPairs.getID(name, getPlotFrame());
             HorizonsEphPairs.HorizonsResults[] res=
                            TargetNetwork.getEphIDInfo(name, false, getFrame());
             HorizonsEphPairs.HorizonsResults choice= res[0];
             if (res.length > 1) {
                 choice= TargetNetwork.chooseOneNaifObject(res,
                                                      getBaseComponent());
             }
             try {
                 id = Integer.parseInt(choice.getNaifID());
             } catch (NumberFormatException e) {}
	  }
	  catch (FailedRequestException e) {
             throw new InvalidTargetException(
                            "No NAIF ID associated with this name -- " + name);
         }
     return id;

   }

   private String getNaifName(String id ) {
      String name = null;
      /*
      EphemerisPairs ephPairs = EphemerisPairs.getInstance();
      String [] strs = ephPairs.getName(id);
      System.out.println("getNaifName: strs " + strs == null?"NULL":strs[0]);
      if (strs == null) {
      */
	 try {
             //strs = RemoteEphemerisPairs.getName(id, getPlotFrame());
             HorizonsEphPairs.HorizonsResults[] res=
                            TargetNetwork.getEphNameInfo(id, false, getFrame());
             name= res[0].getName();
	  }
	  catch (FailedRequestException e) {
             name = NO_NAME;
         }
      //}
     return name;

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
