package edu.caltech.ipac.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileOutputStream;


public class MoveJre {

  public final static String PHASE_DEF= "MoveJre.phase";
  public final static String RESTART_DEF= "MoveJre.restart.prog";
  public final static String LOGFILE_DEF= "MoveJre.logfile";
  public final static String JREDIR_DEF = "MoveJre.jre.dir";
  public final static String ROOT_DEF   = "loader.root.dir";

  public final static String PHASE1= "moveJreToSave";
  public final static String PHASE2= "moveNewJreToPrimary";


  public static void main( String[] args ) {
     String phase= System.getProperty(PHASE_DEF);
     String logfile= System.getProperty(LOGFILE_DEF);
     String rootDir= System.getProperty(ROOT_DEF);
     String jreDir = System.getProperty(JREDIR_DEF);
     String restartProg = System.getProperty(RESTART_DEF);

     Assert.tst(phase   != null && logfile != null &&
                rootDir != null && jreDir  != null);

     if (logfile != null) {
         try {
            File out= new File(logfile);
            PrintStream st= 
                   new PrintStream(new FileOutputStream(out, true), true);
            System.setErr(st);
            System.setOut(st);
         } catch (IOException e) {
             System.out.println("Could not redirect output: " + e);
         }
     }
     String pDesc= null;
     if (phase.equals(PHASE1))      pDesc= "Phase1: ";
     else if (phase.equals(PHASE2)) pDesc= "Phase2: ";
     try {
         int sleepTime = 1;
         System.out.println("MoveJre: " + pDesc + "sleeping " +sleepTime+ 
                               " seconds." );
         Thread.sleep(sleepTime * 1000);  // number of seconds seconds
         System.out.println("MoveJre: " + pDesc + "awake" );
      } catch (InterruptedException e) {
      }

     if (phase.equals(PHASE1)) {
         moveJreToSaveDir(rootDir, jreDir, logfile, restartProg);
     }
     else if (phase.equals(PHASE2)) {
         moveNewJreToPrimaryAndRestartApp(rootDir, jreDir, restartProg);
     }
  }



  public MoveJre(String rootDir, 
                 String jreDir, 
                 String logfile, 
                 String restartProg) {
     System.setProperty("user.dir", rootDir);
     System.out.println("MoveJre: Starting");
     renameToUnique(jreDir + ".old" );
     String appdefs[]= makeAppDefs(PHASE1, jreDir, logfile, restartProg);
                         
     String fullJreDir= rootDir + File.separator + 
                               "update" + File.separator + jreDir;
     startAppLoader( rootDir, "edu.caltech.ipac.util.MoveJre",
                     fullJreDir, appdefs, "Phase0");
  }


  /*
   * rename jre to jre.old while runing from update/jre
   */
  private static void moveJreToSaveDir(String rootDir,
                                       String jreDir, 
                                       String logfile,
                                       String restartProg) {
     System.setProperty("user.dir", rootDir);
     System.out.println("MoveJre: Phase1: moveJreToSaveDir");

     File jreDirFile= new File(jreDir);
     File destDir= new File(jreDir+".old");
     boolean success= jreDirFile.renameTo( destDir );
     if (success) {
        System.out.println("MoveJre: Phase1: renaming: " + jreDirFile + 
                                         " to: " + destDir);
     }
     else {
        System.out.println("MoveJre: Phase1: Rename failed.");
     }

     String appdefs[]= makeAppDefs(PHASE2, jreDir, logfile, restartProg);
     String fullJreDir= rootDir + File.separator + jreDir + ".old";

     startAppLoader( rootDir, "edu.caltech.ipac.util.MoveJre", fullJreDir, 
                     appdefs, "Phase1");

  }

  /*
   * rename update/jre to jre while runing from jre.old
   */
  private static void moveNewJreToPrimaryAndRestartApp(String rootDir, 
                                                       String jreDir,
                                                       String restartProg) {
     System.setProperty("user.dir", rootDir);
     System.out.println("MoveJre: Phase2: moveNewJreToPrimaryAndRestartApp");

      //-------
      //------- Rename Jre
      //-------
     File jreDirFile= new File("update" + File.separator + jreDir);
     File destDir= new File(jreDir);
     boolean success= jreDirFile.renameTo( destDir );
     if (success) {
        System.out.println("MoveJre: Phase2: renaming: " + jreDirFile + 
                                         " to: " + destDir);
     }
     else {
        System.out.println("MoveJre: Phase2: Rename " +jreDir+ " failed.");
     }
      //-------
      //------- restart program
      //-------

     try {
        Process restart= Runtime.getRuntime().exec( restartProg);
        System.out.println( "MoveJre: Phase2: restart using: " + restartProg);
     } catch( IOException e ) {
        System.out.println( "MoveJre: Phase2: restart failed.");
     }

  }



  public void renameToUnique(String dirStr) {
      File srcDir= new File(dirStr);
      File destDir;
      boolean done= false;
      if (srcDir.exists() && srcDir.isDirectory()) {
           for(int i=0; (!done); i++) {
                 destDir= new File(dirStr + "." + i);
                 if (!destDir.exists()) {
                      srcDir.renameTo(destDir);                      
                      System.out.println("MoveJre: renaming: " + srcDir + 
                                         " to: " + destDir);
                      done= true;
                 }
           }
      }
      else {
           System.out.println("MoveJre: renameDirToUnique: " + 
                              srcDir + " not found.");
      }
  }
  
  private static void startAppLoader(String rootDir,
                                     String mainClass,
                                     String jre,
                                     String appdefs[],
                                     String phaseDesc) {

     System.out.println("MoveJre: "+ phaseDesc + ": Starting next phase with:");
     System.out.println("MoveJre: "+ phaseDesc + ":    JRE:  " + jre);
     System.out.println("MoveJre: "+ phaseDesc + ":    main: " + mainClass);
     AppLoader appLoader=  new AppLoader(rootDir,
                                         mainClass, jre,
                                         null, null, null, appdefs, 
                                         false, true, 1, null );
     String jars[] = { "loader.jar" };
     appLoader.loadApp(null, jars);
  }

  private static String [] makeAppDefs( String phase,
                                        String jreDir, 
                                        String logfile,
                                        String restartProg) {
     String appdefs[]= { "-D" + PHASE_DEF  + "=" + phase,
                         "-D" + LOGFILE_DEF+ "=" + logfile,
                         "-D" + JREDIR_DEF + "=" + jreDir,
                         "-D" + RESTART_DEF + "=" + restartProg};
     return appdefs;
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
