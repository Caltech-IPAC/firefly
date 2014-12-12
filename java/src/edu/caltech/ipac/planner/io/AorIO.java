package edu.caltech.ipac.planner.io;

import edu.caltech.ipac.planner.PrimaryPlugin;
import edu.caltech.ipac.util.FileReadStatusException;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class AorIO {


   public abstract void doAorSave(PrimaryPlugin plugin,
                                  File          file,
                                  boolean       saveTargetsOnly,
                                  boolean       saveAllAors,
                                  boolean       saveInOriginalOrder) 
                                              throws IOException;

   public JComponent makePreviewer(JFileChooser fc) {
       return null;
   }


   public abstract AorReadResults doAorRead(File file) throws IOException;


    /**
     * @return List a list of Target objects
     */
   public abstract TargetReadResults doTargetOnlyRead(File file) 
                                                        throws IOException;


//==============================

   public static class AorReadResults {
      private List              _requestList;    // a list of AbstractRequest
      private List              _constraintList; // a list of BaseConstraint
      private boolean           _isProposal;     // true if from a proposal file
      private FileReadStatusException _e;   // not null if problem reading file

      public AorReadResults(List              requestList, 
                            List              constraintList,
                            boolean           isProposal) {
         this(requestList, constraintList, isProposal, null);
      }
      public AorReadResults(List                    requestList, 
                            List                    constraintList,
                            boolean                 isProposal,
                            FileReadStatusException e) {
           _requestList   = requestList;
           _constraintList= constraintList;
           _isProposal    = isProposal;
           _e             = e;
      }
      public List    getRequestList()    { return _requestList;    }
      public List    getConstraintList() { return _constraintList; }
      public boolean isProposal()        { return _isProposal;     }
      public FileReadStatusException getException() { return _e;     }
   }


   public static class TargetReadResults {
      private List                    _targetList;    // a list of Target
      private FileReadStatusException _e;   // not null if problem reading file

      public TargetReadResults(List targetList) {
         this(targetList, null);
      }
      public TargetReadResults(List targetList, FileReadStatusException e) {
           _targetList= targetList;
           _e         = e;
      }
      public List    getTargetList()                { return _targetList; }
      public FileReadStatusException getException() { return _e; }
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
