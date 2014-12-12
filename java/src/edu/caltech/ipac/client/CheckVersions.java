package edu.caltech.ipac.client;


import edu.caltech.ipac.gui.OptionPaneWrap;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.util.Assert;

import javax.swing.JEditorPane;
import java.awt.Window;
import java.util.StringTokenizer;


public class CheckVersions {


   public static void doCheck(Window w) {
      int    majorVersion= -1;
      int    minorVersion= -1;
      int    revision    = -1;
      try {
         String versionStr= System.getProperty("java.version");
         StringTokenizer st= new StringTokenizer(versionStr, ".");
         majorVersion= Integer.parseInt(st.nextToken());
         minorVersion= Integer.parseInt(st.nextToken());
         try {
           revision= Integer.parseInt(st.nextToken());
         } catch (NumberFormatException nfe) {
           revision= -1;
         }

         if (minorVersion == 0 || 
             minorVersion == 1 ||
             minorVersion == 2 || 
             minorVersion == 3) {
             showError( w, 
              "You are attemping to run the program with a unsupported " +
              "version of java- version: " + versionStr  );
             System.exit(0);
         }
         else if (minorVersion >= 4) {
               // do nothing  -- all is well
         }
         else {
             Assert.tst(false);
         }
      } catch (Exception e) {
         System.out.println("CheckVerison.doCheck: " + e);
         System.out.println("majorVersion: " + statToString(majorVersion) );
         System.out.println("minorVersion: " + statToString(minorVersion) );
         System.out.println("revision:     " + statToString(revision) );
      }

     // System.out.println("DBG: majorVersion: " + statToString(majorVersion) );
     // System.out.println("DBG: minorVersion: " + statToString(minorVersion) );
     // System.out.println("DBG: revision:     " + statToString(revision) );
   }

   private static void showError(Window w, String mess) {
      OptionPaneWrap.showError( w, mess, "Java Version Error");
   }

   private static void showInformation(Window w, String mess) {
      JEditorPane text= SwingSupport.makeInfoEditorPane(10, mess, 700);
      OptionPaneWrap.showInfo( w, text, "Information");
   }

   static String statToString(int stat) {
        return (stat > -1) ? stat + "" : "failed";
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
