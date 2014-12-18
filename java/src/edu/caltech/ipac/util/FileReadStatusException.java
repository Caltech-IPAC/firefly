package edu.caltech.ipac.util;



/**
 *  This exception will be thrown when a file read has a issue.
 *  @author Trey Roby
 */

public class FileReadStatusException extends Exception {

   public static final int ERROR       = 45;
   public static final int WARNING     = 46;
   public static final int INFORMATION = 47;

   private int _severity;

   public FileReadStatusException(String message) {
      this(message, ERROR);
   }

   public FileReadStatusException(String message, int severity) {
      super(message);
      Assert.tst(severity == ERROR       ||
                 severity == WARNING     ||
                 severity == INFORMATION);
      _severity= severity;
   }

   public int getSeverity() { return _severity; }
}
