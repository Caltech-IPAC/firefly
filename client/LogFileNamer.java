package edu.caltech.ipac.client;

import edu.caltech.ipac.gui.OptionPaneWrap;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.Installer;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * This class determines the name of the log file (based on the date), 
 * deletes old log files, and writes the initial information in the log file.
 *
 * @author Trey Roby
 * @version $Id: LogFileNamer.java,v 1.7 2008/08/26 23:00:31 roby Exp $
 *
 */
public class LogFileNamer {

   private static final String DEFAULT_LOG_FILE_ROOT=  "spot";
   private static final String LOG_FILE_EXT         =  ".log";
   private static final int    LOG_FILE_SAVE_MAX    =  5;
   private static final int    MILLISEC_IN_DAY      = 86400000; //1000*60*60*24
   private static String _logFileRoot= DEFAULT_LOG_FILE_ROOT;

   public static void setLogFileRoot(String root) {
       _logFileRoot= root;
   }

   /**
    * Put all standard out and error into a log file.  Name the log files so
    * we have a small log file history. The number of log files we keep is 
    * defined by the LOG_FILE_SAVE_MAX constant.  Delete all other old log
    * files.
    * @param workingDir the directory to put the log file
    * @return the full path name of the log file
    */
   static public String redirectIO(File workingDir) {
       String retpath= null;
       if (workingDir != null) {
          try {
              File out= new File(workingDir, makeLogFileName(_logFileRoot));
              PrintStream st=
                     new PrintStream(new FileOutputStream(out), true);
              System.setErr(st);
              System.setOut(st);
              retpath= out.getPath();
              initialLogging();
              cleanupOldLogFiles(_logFileRoot, workingDir);
          } catch (IOException e) {
              ClientLog.warning("Could not redirect output: " + e);
              OptionPaneWrap.showError(null,
                       "<html>Could create a log file: " +
                       e.toString() +"<br>" +
                       "You do not appear to have access to: " +
                       workingDir.getPath() + "<br>"+
                       "Logging will be disabled<br><br>"+
                       "This is a significant error that could " +
                       "affect other areas of this program<br>" +
                       "You should contact your system administrator" );
          }
       }
       else {
          ClientLog.warning("Could not redirect output: " +
                            "No log directory exist.");
       }
       return retpath;
    }

    static public PrintStream createLogFileStream(String root, File workingDir) throws IOException {
        File out= new File(workingDir, makeLogFileName(root));
        return new PrintStream(new FileOutputStream(out), true);
    }

    public static String makeLogFileName(String root) {
        Calendar cal= Calendar.getInstance();
        String month= getMonthString(  cal.get(Calendar.MONTH));
        String day  = make2DigitString(cal.get(Calendar.DATE));
        String hour=  make2DigitString(cal.get(Calendar.HOUR_OF_DAY));
        String min=   make2DigitString(cal.get(Calendar.MINUTE));
        String sec=   make2DigitString(cal.get(Calendar.SECOND));
        return root + "-" +
                   month +
                   day   + "_" +
                   hour  + "-" +
                   min   + "-" +
                   sec + LOG_FILE_EXT;
    }

   /**
    * Make a string from the passed int that is at least two characters.
    * For example- a 13 will return "13" and a 7 will return "07".
    * @param i positive integer
    * @return String a string of at least 2 characters.
    */
   static private String make2DigitString(int i) {
       Assert.tst(i>=0, "bad value for i, i=" + i );
       return (i>9) ? i + "" : "0" + i;
   }

   /**
    * remove all the old log files.  Keep the LOG_FILE_SAVE_MAX most recent.
    * @param root the name root of the log file
    * @param workingDir the directory where the log files are
    */
   static private void cleanupOldLogFiles(String root, File workingDir) {
       /*
        * first remove any pre s4 style log files
        */
       File f= new File(workingDir, root + LOG_FILE_EXT);
       if (f.exists()) {
              ClientLog.message("Deleting old log file: " + f);
              f.delete();
       }
       /*
        * now remove the oldest log files
        */
       List<File> fileList= getLogFileList(root,workingDir);
       if (fileList.size() > LOG_FILE_SAVE_MAX) {
          int numberToDel= fileList.size() - LOG_FILE_SAVE_MAX;
          Collections.sort(fileList, new CompareDate() );
          Iterator<File> i;
          long yesterday= new Date().getTime() - MILLISEC_IN_DAY;
          for(i= fileList.iterator(); (i.hasNext() && numberToDel > 0); ) {
              f= i.next();
              numberToDel--; 
              if (f.exists() && f.lastModified() < yesterday)  {
                   ClientLog.brief("Deleting old log file: " + f);
                   f.delete(); 
              }
          }
       }
   }

   /**
    * Return a list of files that match the pattern spot*.log
    * @param workingDir the directory to search
    * @param root the name root of the log file
    * @return List a list of file objects that look like spot*.log
    */
   static private List<File> getLogFileList(final String root,
                                            final File workingDir) {
       String files[]=workingDir.list( new FilenameFilter() {
              public boolean accept(File dir, String name) {
                 return name.startsWith(root) &&
                        name.endsWith(LOG_FILE_EXT);
              }
        });
       ArrayList<File> list= new ArrayList<File>(files.length);
       for(String file : files) {
           list.add( new File(workingDir,file)  );
       }
       return list;
   }

   /**
    * Take one of the calendar month constants and return String with 
    * that months abreviation.
    * @param month the Carlendar month constant
    * @return String a string with the month abreviation
    */
   static private String getMonthString(int month) {
       String retval= "";
       switch (month) {
         case Calendar.JANUARY   : retval= "jan"; break;
         case Calendar.FEBRUARY  : retval= "feb"; break;
         case Calendar.MARCH     : retval= "mar"; break;
         case Calendar.APRIL     : retval= "apr"; break;
         case Calendar.MAY       : retval= "may"; break;
         case Calendar.JUNE      : retval= "jun"; break;
         case Calendar.JULY      : retval= "jul"; break;
         case Calendar.AUGUST    : retval= "aug"; break;
         case Calendar.SEPTEMBER : retval= "sep"; break;
         case Calendar.OCTOBER   : retval= "oct"; break;
         case Calendar.NOVEMBER  : retval= "nov"; break;
         case Calendar.DECEMBER  : retval= "dec"; break;
         default: Assert.tst(false);              break;
       }
       return retval;
   }

   /**
    * Print out some key properties.
    */


   static public void initialLogging() {
       initialLogging(System.out);
   }


   static public void initialLogging(PrintStream out) {
       String sep= "=================================================";
       ApplicationVersion av=ApplicationVersion.getInstalledApplicationVersion();
       out.println("--------------- System Properties ---------------");
       printProp(out, "os.name");
       printProp(out, "os.arch");
       printProp(out, "os.version");
       printProp(out, "user.name");
       printProp(out, "user.home");
       printProp(out, "user.dir");
       printProp(out, "java.version");
       printProp(out, "java.vendor");
       printProp(out, "java.home");
       printProp(out, "java.class.path");
       out.println("True class path= "+ Installer.getTrueClassPath());
       if (!GraphicsEnvironment.isHeadless())  {
           out.println(sep);
           out.println("");
           out.println("");
           out.println("-------------- Platform Information -------------");
           out.println(Platform.getInstance());
       }
       out.println(sep);
       out.println("");
       out.println("");

       if(av.isVersionSet()) {
           out.println(
                          "-------------- Application Information -----------");
           out.println("Version   : "+av.getVersionString());
           out.println("Build Date: "+av.getBuildDate());
           out.println(sep);
       }
       out.println("");
   }

   /**
    * Print one system property.
    */
   static private void printProp(PrintStream out, String prop) {
     out.println( prop + "=" + System.getProperty(prop) );
   }


//=========================================================================
//-------------------------- Inner Classes --------------------------------
//=========================================================================

   /**
    * This comparator will compare to files by modified date.  It is use
    * with Collections.sort().
    * 
    * @see java.util.Collections
    * @see java.util.Comparator
    */
   private static class CompareDate implements Comparator<File> {
      public int compare(File f1, File f2) {
        int  retval= 0;
        if      (f1.lastModified() < f2.lastModified()) retval= -1;
        else if (f1.lastModified() > f2.lastModified()) retval=  1;
        return retval;
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
