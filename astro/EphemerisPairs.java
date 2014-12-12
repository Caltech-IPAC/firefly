package edu.caltech.ipac.astro;

import edu.caltech.ipac.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;


/** 
 * A class to use Property to hold the (Naif-name, Naif-ID) 
 * and (Niaf-ID, Naif-name) pairs, so we can display the other
 * info when one in entered by user
 *
 * @author Xiuqin Wu
 */

public class EphemerisPairs extends Properties
{
   static private  EphemerisPairs _theInstance = null;
   private  Properties _ephID = new Properties();
   private  Properties _ephName = new Properties();
   private  Properties _ephIdNeedCorrection = new Properties();

   private BufferedReader  _reader;
   private final String EPHEMERIS_PAIR_FILE = "ClientEphemerisPairs.prop";

   /**
    the default file name to contain the ephemeris name-ID pairs
    is sysfiles/sutdata/ClientEphemerisPairs.prop
    */
   private EphemerisPairs(){
      Class objClass = getClass();
      ClassLoader cl = objClass.getClassLoader();
      //StringTokenizer st= new StringTokenizer(objClass.getName(), ".");
      //int len= st.countTokens();
      URL url;
      String line = null;

      //for(int i= 0; (i<len-1); st.nextToken(), i++);
      //String fname= "resources/" + st.nextToken() + ".prop";
      //System.out.println("resource file:" +EPHEMERIS_PAIR_FILE);
      try {
         //url= objClass.getResource(EPHEMERIS_PAIR_FILE);
         url= cl.getSystemResource(EPHEMERIS_PAIR_FILE);
         _reader = new BufferedReader(new InputStreamReader(url.openStream()));
	 line = _reader.readLine();
	 while (line != null) {
	    addProperty(line);
	    line = _reader.readLine();
	    }
	}
      catch (IOException ioe) {
         System.out.println("IO exception :" +ioe.getMessage());
	 }
      catch (Exception e) {
         System.out.println("Could not load property:" +EPHEMERIS_PAIR_FILE);
         }
   }
      
   private void addProperty(String line) {
      String delim = "=";
      String [] ids;
      String [] names;
      int  i;
      String foundIDs, newIDs, key;
      try {
	 StringTokenizer st = new StringTokenizer(line, delim);
	 String idString = st.nextToken();
	 String nameString = st.nextToken();

	 ids = StringUtil.strToStrings(idString);
	 names = StringUtil.strToStrings(nameString);


	 for (i=0; i<ids.length; i++) {
	    _ephID.setProperty(ids[i], nameString.trim());
	    //System.out.println("id: "+ids[i]);
	    //System.out.println("nameString: "+nameString);
	    }
	 for (i=0; i<names.length; i++) {
	    key = names[i].trim().toUpperCase();
	    foundIDs = _ephName.getProperty(key);
	    // if the key is in property already, merge the values with the 
	    // new ones
	    if (foundIDs != null) {
	       //newIDs = idString.trim() + " " + foundIDs;
	       newIDs = foundIDs + " " + idString.trim();
	       _ephName.remove(key);
	       _ephName.setProperty(key, newIDs);
	       }
	    else 
	       _ephName.setProperty(key, idString.trim());
	    //System.out.println("name: "+names[i]);
	    //System.out.println("idString: "+idString);
	    }
      }
      catch (Exception e) {
	 System.out.println("the offending property line: " +line);
	 }

   }

   private void addMoreIDs() {
   }
   private void addMoreNames() {
   }

   /*
   public static synchronized EphemerisPairs getInstance(boolean foreground){
      EphemerisPairs retval= null;
      if (_theInstance == null) {
         if (foreground) DialogSupport.setWaitCursor(true);
         retval= getInstance();
         if (foreground) DialogSupport.setWaitCursor(false);
      }
      else {
         retval= _theInstance;
      }
      return retval;
   }
   */

   public static boolean  isInstanceMade(){
      boolean retval = true;
      if (_theInstance == null) 
         retval =false;
      return retval;
   }
   public static synchronized EphemerisPairs getInstance(){
      if (_theInstance == null) 
         _theInstance = new EphemerisPairs();
      return _theInstance;
   }

   public String[] getName(String id) {
      String s;
      String [] strings = null;
      s = _ephID.getProperty(id);
      //System.out.println("getName: ID= " + id+"nameString="+s);
      if (s != null)
	 strings = StringUtil.strToStrings(s);
      //System.out.println("strings: "+ strings);
      return strings;

   }
   public String[] getID(String name) {
      String s;
      String [] strings = null;
      StringUtil.crunch(name);
      s = _ephName.getProperty(name.toUpperCase());
      //System.out.println("getID: name= " + name+"idString="+s);
      if (s != null)
	 strings = StringUtil.strToStrings(s);
      //System.out.println("strings: "+ strings);
      return strings;
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
